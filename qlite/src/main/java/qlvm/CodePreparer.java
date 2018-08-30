package qlvm;

import org.apache.commons.lang3.StringUtils;
import qlvm.exceptions.compile.QLCompilerException;
import qlvm.exceptions.compile.QLInvalidSubStructureException;

import java.util.*;

/**
 * @author microhash
 *
 * CodePreparer provides functions to prepare the code of a qubic for its execution.
 * */
public class CodePreparer {

    private final ArrayList<String> subStructureList;
    private final ArrayList<String> stringTable;

    protected CodePreparer(ArrayList<String> subStructureList, ArrayList<String> stringTable) {
        this.subStructureList = subStructureList;
        this.stringTable = stringTable;
    }

    /**
     * Prepares the code for formal execution by moving strings to the stringTable,
     * removing whitespace and moving seperate sub structures (code blocks,
     * bracket expressions) into the subStructureList.
     * @param program the complete qubic source code
     * @return main block of program
     * */
    String prepareProgram(String program) {

        // isolateStrings
        program = isolateStrings(program);

        // avoid case sensitivity
        program = program.toLowerCase();

        // TODO ignore comments

        // ignore whitespace
        program = program.replace("\n", "");
        program = program.replace("\t", "");
        program = program.replace(" ", "");

        // seperateBlocks
        program = seperateSubStructures(program);

        // set non-intuitive ';' for if/while/else
        program = delimitAllControlStructures(program);
        for(int i = 0; i < subStructureList.size(); i++) {
            String subStructure = subStructureList.get(i);
            String subStructureReplacement = subStructure.charAt(0)+ delimitAllControlStructures(subStructure.substring(1, subStructure.length()-1))+subStructure.charAt(subStructure.length()-1);
            subStructureList.set(i, subStructureReplacement);
        }

        return program;
    }

    /**
     * Isolates strings by moving them out of the code and into the stringTable.
     * Allows easy code pattern recognition during the execution.
     * @param code qubic source code containing strings
     * @return qubic source code, strings replaced with references to the stringTable
     * */
    protected String isolateStrings(String code) {

        code = code.replace("\\'", "%ESCAPED%");
        String[] fracs = code.split("'");

        StringBuilder codeSB = new StringBuilder();
        for(int i = 0; i < fracs.length; i++) {
            if(i%2 == 0) codeSB.append(fracs[i]);
            else {
                fracs[i] = fracs[i].replace("%ESCAPED%", "'");
                stringTable.add(fracs[i]);
                codeSB.append("%").append(i/2);
            }
        }

        code = codeSB.toString().replace("%ESCAPED%", "'");
        return code;
    }

    /**
     * Filters out sub structures (code blocks, bracket expressions) and puts them into
     * the subStructureList. This way it reduces the effort necessary for pattern recognition.
     * */
    protected String seperateSubStructures(String code) {

        int codeLength = code.length();

        int sBracketsO = StringUtils.countMatches(code, '[');
        int sBracketsC = StringUtils.countMatches(code, ']');
        int rBracketsO = StringUtils.countMatches(code, '(');
        int rBracketsC = StringUtils.countMatches(code, ')');
        int bracesO    = StringUtils.countMatches(code, '{');
        int bracesC    = StringUtils.countMatches(code, '}');

        // validate bracket amounts
        if(sBracketsO != sBracketsC)
            throw new QLInvalidSubStructureException("invalid amount of square brackets: " + sBracketsO + "x '[', " + sBracketsC + "x ']'. code segment:\n\n"+code);
        if(rBracketsO != rBracketsC)
            throw new QLInvalidSubStructureException("invalid amount of round brackets: " + rBracketsO + "x '(', " + rBracketsC + "x ')'. code segment:\n\n"+code);
        if(bracesO != bracesC)
            throw new QLInvalidSubStructureException("invalid amount of braces: " + bracesO + "x '{', " + bracesC + "x '}'. code segment:\n\n"+code);

        // nothing to seperate
        if(sBracketsO+rBracketsO+bracesO == 0)
            return code;

        Stack<Integer> levelStack = new Stack<>();

        // traverse from left to find opening bracket/brace
        for(int i = 0; i < codeLength; i++) {
            char c = code.charAt(i);

            if(c == '(' || c == '[' || c == '{') {
                levelStack.push(i);
            }

            if(c == ')' || c == ']' || c == '}') {
                int s = levelStack.pop();

                // TODO check hierarchy consistency

                // add substructure to subStructureList
                String subStructure = code.substring(s, i+1);
                subStructureList.add(subStructure);

                // build new code
                String subStructurePointer = "$" + encodeBracket(c) + (subStructureList.size()-1) + "$";
                String alreadyTraversed = code.substring(0, s) + subStructurePointer;
                code = alreadyTraversed + code.substring(i+1, codeLength);

                // update state
                i = alreadyTraversed.length()-1;
                codeLength = code.length();
            }
        }

        return code;
    }

    private char encodeBracket(char c) {
        if(c == '}')
            return 'b';
        if(c == ']')
            return 's';
        if(c == ')')
            return 'r';
        throw new QLCompilerException("not a bracket/brace character: " + c);
    }

    /**
     * Code blocks are marked by braces: "{...}", but require a ';' to be set thereafter to seperate them
     * from the next command. delimitAllControlStructures() does exactly that for if/while/else code blocks.
     * @param code the input code that shall be processed in this method
     * @return the resulting code with delimited control structures
     * */
    private String delimitAllControlStructures(String code) {

        LinkedList<String> lines = new LinkedList<>(Arrays.asList(code.split(";")));

        for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if(line.startsWith("if$") || line.startsWith("while$")) {
                String[] insertLines = delimitSingleControlStructure(line);
                lines.set(lineIndex, insertLines[0]);
                lines.add(lineIndex+1, insertLines[1]);
            }
        }

        return StringUtils.join(lines, ";");
    }

    /**
     * Seperates a code block from the command following the code block
     * @param line the command line to search though, it should start with if/else
     * @return seperated commands as array without ';'
     * */
    private String[] delimitSingleControlStructure(String line) {

        String block = line.startsWith("else$") ? line.substring(4) : line.split("\\$", 3)[2];

        if(!block.startsWith("$b"))
            throw new QLCompilerException("control structures (if/while/else) require a braces block '{ ... }'");

        String afterStatement = block.split("\\$", 3)[2];

        if(line.startsWith("if$") && afterStatement.startsWith("else$")) {
            afterStatement = delimitSingleControlStructure(afterStatement)[1];
            return new String[] {line.substring(0, line.length()-afterStatement.length()).replace("else$", "$"), afterStatement};
        }

        String controlStructure = line.substring(0, line.length()-afterStatement.length());
        return new String[] {controlStructure, afterStatement};
    }
}
