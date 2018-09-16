package qlvm;

import constants.GeneralConstants;
import org.json.JSONArray;
import oracle.OracleWriter;
import org.json.JSONObject;
import qlvm.exceptions.runtime.*;
import qlvm.functions.operations.LogicOperations;
import qlvm.functions.operations.MathOperations;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author microhash
 *
 * The QLVM (Qubic Virtual Machine) interpretes the qubic source code specified in
 * the qubic transaction and by doing so calculates the result for the ResultStatement.
 * */
public class QLVM {

    private final static String VARIABLE_REGEX = "[a-zA-Z_]([a-zA-Z0-9_])*";
    private final static String S_SUBSTRUC_REGEX = "[$][s][0-9]+[$]";
    private final static String R_SUBSTRUC_REGEX = "[$][r][0-9]+[$]";
    private final static String B_SUBSTRUC_REGEX = "[$][b][0-9]+[$]";
    private final static String GENERAL_SUBSTRUC_REGEX = "[$][rsb][0-9]+[$]";
    private final static String INDEXED_EXPRESSION = VARIABLE_REGEX+"("+S_SUBSTRUC_REGEX+")+";
    private final static String VAR_OR_INDEXED = VARIABLE_REGEX+"("+S_SUBSTRUC_REGEX+")*";
    private final static String IF_REGEX = "if"+R_SUBSTRUC_REGEX+"("+B_SUBSTRUC_REGEX+"){1,2}";

    private final static Pattern STATEMENT_IF = Pattern.compile("^"+IF_REGEX+"$");
    private final static Pattern STATEMENT_WHILE = Pattern.compile("^while"+R_SUBSTRUC_REGEX+B_SUBSTRUC_REGEX);
    private final static Pattern STATEMENT_ASSIGNMENT = Pattern.compile("^"+VARIABLE_REGEX+"[=]");
    private final static Pattern STATEMENT_INDEXED_ASSIGNMENT = Pattern.compile("^"+INDEXED_EXPRESSION+"[=]");

    private final static Pattern STATEMENT_INCREMENT = Pattern.compile("^"+VAR_OR_INDEXED+"\\+\\+$");
    private final static Pattern STATEMENT_DECREMENT = Pattern.compile("^"+VAR_OR_INDEXED+"--$");
    private final static Pattern STATEMENT_ADD = Pattern.compile("^"+VAR_OR_INDEXED+"\\+=");
    private final static Pattern STATEMENT_SUB = Pattern.compile("^"+VAR_OR_INDEXED+"-=");
    private final static Pattern STATEMENT_MUL = Pattern.compile("^"+VAR_OR_INDEXED+"\\*=");
    private final static Pattern STATEMENT_DIV = Pattern.compile("^"+VAR_OR_INDEXED+"/=");

    private final static Pattern OBJECT_SUBSTRUCTURE = Pattern.compile("^"+GENERAL_SUBSTRUC_REGEX+"$");
    public final static Pattern OBJECT_NUMBER = Pattern.compile("^[-]?[0-9]+(\\.[0-9]*)?$");
    private final static Pattern OBJECT_STRING = Pattern.compile("^%[0-9]+$");
    private final static Pattern OBJECT_FUNCTION_CALL = Pattern.compile("^([a-zA-Z_]+)"+R_SUBSTRUC_REGEX+"$");
    private final static Pattern OBJECT_VARIABLE = Pattern.compile("^"+VARIABLE_REGEX+"$");
    private final static Pattern OBJECT_ARRAY = Pattern.compile("^\\[.*]$");
    private final static Pattern OBJECT_JSON = Pattern.compile("^\\{.*}$");
    private final static Pattern OBJECT_INDEXED = Pattern.compile("^"+INDEXED_EXPRESSION+"$");

    private final ArrayList<String> subStructureList = new ArrayList<>();
    private final ArrayList<String> stringTable = new ArrayList<>();
    private final HashMap<String, String> variables = new HashMap<>();
    private final OracleWriter oracleWriter;

    private boolean interrupted = false;
    private final boolean inTestMode;

    /**
     * Runs code in the context of a specific OracleWriter.
     * @param code the code to run
     * @param oracleWriter the oracleWriter to use as context
     * */
    public static String run(String code, OracleWriter oracleWriter, int epochIndex) {

        final ObjectContainer oc = new ObjectContainer(null);
        final QLVM qlvm = new QLVM(oracleWriter, epochIndex);

        Thread t = new Thread() {
            @Override
            public void run() {
                oc.o = qlvm.executeProgram(code);
                synchronized (oc) { oc.notify(); }
            }
        };

        t.start();

        try {
            synchronized (oc) { oc.wait(oracleWriter.getQubicReader().getSpecification().getRuntimeLimit()*1000); }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        qlvm.interrupt();
        try{ Thread.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }

        if(oc.o == null) {
            return throwableToJSON(new QLRunTimeLimitExceededException()).toString();
        }

        return (String)oc.o;
    }

    private static JSONObject throwableToJSON(Throwable t) {

        JSONObject o = new JSONObject();
        o.put("error_type", t.getClass().getName());
        o.put("error_message", t.getMessage());

        StringBuilder stackTrace = new StringBuilder();
        for(int i = 0; i < t.getStackTrace().length; i++)
            stackTrace.append(t.getStackTrace()[i].getFileName() + ": #" + t.getStackTrace()[i].getLineNumber() + "\n");
        o.put("error_origin", stackTrace.toString());
        return o;
    }

    public static String testRun(String code, int epoch) {
        QLVM qlvm = new QLVM(epoch);
        return qlvm.executeProgram(code);
    }

    private QLVM(OracleWriter oracleWriter, int epochIndex) {
        this.oracleWriter = oracleWriter;
        variables.put("epoch", ""+epochIndex);
        variables.put("qubic", "'"+oracleWriter.getQubicReader().getID()+"'");
        inTestMode = false;
    }

    /**
     * Just for local testing purposes.
     * */
    private QLVM(int epoch) {
        this.oracleWriter = null;
        variables.put("epoch", ""+epoch);
        variables.put("qubic", null);
        inTestMode = true;
    }

    private void interrupt() {
        interrupted = true;
    }

    /**
     * Executes the whole qubic program.
     * @param program qubic program source code to be processed
     * @return calculated qubic result for the respective epoch
     * */
    private String executeProgram(String program) {

        try {
            // execute program
            String mainBlock = new CodePreparer(subStructureList, stringTable).prepareProgram(program);
            executeBlock(mainBlock);
            throw new NoReturnThrowable();
        } catch (ReturnResultThrowable e) {
            return e.result;
        } catch (Throwable t) {
            return throwableToJSON(t).toString();
        }
    }

    /**
     * Executes a single code block.
     * @param block code block to be processed
     * */
    private void executeBlock(String block) {

        if(interrupted) throw new QLRunTimeLimitExceededException();

        while(OBJECT_SUBSTRUCTURE.matcher(block).find()) {
            block = getSubstructureContent(block.split("[$]")[1]);
        }

        String[] commands = block.split(";");

        for(String command : commands)
            executeCommand(command);
    }

    /**
     * Executes a single command.
     * @param command command to be processed
     * */
    private void executeCommand(String command) {

        if(command.equals("")) return;

        if(interrupted) throw new QLRunTimeLimitExceededException();

        if(STATEMENT_ASSIGNMENT.matcher(command).find()) {
            String varName = command.split("=")[0];
            String valueExpression = command.substring(varName.length()+1);
            putVariable(varName, normalizeValueExpression(valueExpression));
            return;
        }

        if(STATEMENT_INDEXED_ASSIGNMENT.matcher(command).find()) {
            String indexedExpression = command.split("=")[0];
            String valueExpression = command.substring(indexedExpression.length()+1);
            String value = normalizeValueExpression(valueExpression);
            if(value != null && value.length() > GeneralConstants.QLVM_MAX_VALUE_LENGTH) throw new QLValueMaxLengthExceeded(value);
            assignToIndexable(indexedExpression, value);
            return;
        }

        boolean isIf = STATEMENT_IF.matcher(command).find();
        boolean isWhile = !isIf && STATEMENT_WHILE.matcher(command).find();

        if(isIf || isWhile) {
            String[] splits = command.split("[$]");

            final String conditionString =  getSubstructureContent(splits[1]);
            String condition = normalizeValueExpression(conditionString);

            if(isIf) { // simulate if/else
                if(LogicOperations.stringConditionToBoolean(condition)) {
                    String ifBlock = getSubstructureContent(splits[3]);
                    executeBlock(ifBlock);
                } else if(splits.length >= 6) {
                    String elseBlock = getSubstructureContent(splits[5]);
                    executeBlock(elseBlock);
                }
            } else if(isWhile) { // simulate while loop
                while(LogicOperations.stringConditionToBoolean(condition)) {
                    if(interrupted) throw new QLRunTimeLimitExceededException();
                    String block = getSubstructureContent(splits[3]);
                    executeBlock(block);
                    condition = normalizeValueExpression(conditionString);
                }
            }
            return;
        }

        if(STATEMENT_INCREMENT.matcher(command).find()) {
            String varName = command.split("\\+\\+")[0];
            executeCommand(varName+"="+varName+"+"+1);
            return;
        }

        if(STATEMENT_DECREMENT.matcher(command).find()) {
            String varName = command.split("--")[0];
            executeCommand(varName+"="+varName+"-"+1);
            return;
        }

        if(STATEMENT_ADD.matcher(command).find()) {
            String[] par = command.split("\\+=");
            String varName = par[0];
            par[0] = normalizeValueExpression(par[0]);
            par[1] = normalizeValueExpression(par[1]);
            executeCommand(varName+"="+par[0]+"+"+par[1]);
            return;
        }

        if(STATEMENT_SUB.matcher(command).find()) {
            String[] par = command.split("-=");
            String varName = par[0];
            par[0] = normalizeValueExpression(par[0]);
            par[1] = normalizeValueExpression(par[1]);
            executeCommand(varName+"="+par[0]+"-"+par[1]);
            return;
        }

        if(STATEMENT_MUL.matcher(command).find()) {
            String[] par = command.split("\\*=");
            String varName = par[0];
            par[0] = normalizeValueExpression(par[0]);
            par[1] = normalizeValueExpression(par[1]);
            executeCommand(varName+"="+par[0]+"*"+par[1]);
            return;
        }

        if(STATEMENT_DIV.matcher(command).find()) {
            String[] par = command.split("/=");
            String varName = par[0];
            par[0] = normalizeValueExpression(par[0]);
            par[1] = normalizeValueExpression(par[1]);
            executeCommand(varName+"="+par[0]+"/"+par[1]);
            return;
        }

        if(command.startsWith("return$r")) {
            String ret = getSubstructureContent(command.split("\\$")[1]);
            throw new ReturnResultThrowable(normalizeValueExpression(ret));
        }

        throw new UnknownCommandException(command);
    }

    private void putVariable(String varName, String value) {
        if(value != null && value.length() > GeneralConstants.QLVM_MAX_VALUE_LENGTH) throw new QLValueMaxLengthExceeded(value);
        variables.put(varName, value);
    }
    /**
     * Maps a value expression to a concrete value (= normalization) to be used by the context.
     * @param valueExpression the expression to be normalized
     * @return the normalized expression / a concrete value
     * */
    private String normalizeValueExpression(String valueExpression) {

        if(interrupted) throw new QLRunTimeLimitExceededException();

        if(valueExpression.length() == 0)
            return "";

        // remove round brackets because they do not add any meaning
        while(valueExpression.charAt(0) == '(')
            valueExpression = valueExpression.substring(1, valueExpression.length()-1);

        if(OBJECT_SUBSTRUCTURE.matcher(valueExpression).matches()) {
            int index = Integer.parseInt(valueExpression.substring(2, valueExpression.length()-1));
            String subStructure = subStructureList.get(index);
            return normalizeValueExpression(subStructure);
        }

        if(OBJECT_VARIABLE.matcher(valueExpression).matches())
            return variables.get(valueExpression);

        if(OBJECT_NUMBER.matcher(valueExpression).matches())
            return valueExpression;

        if(OBJECT_STRING.matcher(valueExpression).matches())
            return "'"+escapeString(stringTable.get(Integer.parseInt(valueExpression.substring(1))))+"'";

        if(OBJECT_ARRAY.matcher(valueExpression).matches())
            return normalizeArray(valueExpression);

        if(OBJECT_JSON.matcher(valueExpression).matches())
            return normalizeJSON(valueExpression);

        if(OBJECT_INDEXED.matcher(valueExpression).matches())
            return normalizeIndexed(valueExpression);

        if(OBJECT_FUNCTION_CALL.matcher(valueExpression).matches()) {
            String functionName = valueExpression.split("[$]")[0];
            String parSubStrucIndex = valueExpression.substring(functionName.length()+1, valueExpression.length()-1);
            String parameterString = getSubstructureContent(parSubStrucIndex);

            return FunctionCall.call(this, functionName, normalizeListExpression(parameterString));
        }

        // logic operations
        String[] logicOperators = LogicOperations.getOperatorArray();
        for(String operator : logicOperators)
            if(valueExpression.contains(operator)) {
                String[] par = valueExpression.split(Pattern.quote(operator), 2);
                for (int i = 0; i < par.length; i++)
                    par[i] = normalizeValueExpression(par[i]);
                return LogicOperations.doOperation(operator, par);
            }

        // math operations
        String[] mathOperators = MathOperations.getOperatorArray();
        for(String operator : mathOperators)
            if(valueExpression.contains(operator)) {
                String[] par = valueExpression.split(Pattern.quote(operator), 2);
                for (int i = 0; i < par.length; i++)
                    par[i] = normalizeValueExpression(par[i]);
                return MathOperations.doOperation(operator, par);
            }

        return "?" + valueExpression;
    }

    /**
     * Convenient access to the content of a sub structure (without the brackets/braces)
     * @param key sub structure key consisting of bracket/brace encoding + index ('e.g. b13')
     * @return sub structure content (= without the brackets/braces)
     * */
    private String getSubstructureContent(String key) {
        char c = key.charAt(0); // TODO validate correct bracket/brace
        int index = Integer.parseInt(key.substring(1, key.length()));
        String structure = subStructureList.get(index);
        return structure.substring(1, structure.length()-1);
    }

    /**
     * Normalizes a list expression (expressions seperates by ',').
     * @param listExpression the list expression, e.g. "1+1, a, $b4$"
     * @return array of the normalized list elements, e.g. "[2, 3, 'hello']"
     * */
    private String[] normalizeListExpression(String listExpression) {

        String[] elements = listExpression.split(",");
        for(int i = 0; i < elements.length; i++)
            elements[i] = normalizeValueExpression(elements[i]);
        return elements;
    }

    /**
     * Escapes a string by escaping apostrophes and doubling backslashes.
     * @param s any string to be escaped (e.g. "print '\n';")
     * @return the escaped string (e.g. "print \'\\n\';")
     * */
    public static String escapeString(String s) {
        s = s.replace("\\", "\\\\");
        s = s.replace("'", "\\'");
        return s;
    }

    /**
     * Unescapes a string, counterpart to escapeString().
     * @param s any string to be unescaped (e.g. "print \'\\n\';")
     * @return the unescaped string (e.g. "print '\n';")
     * */
    public static String unescapeString(String s) {
        s = s.replace("\\'", "\\%ESCAPED%");
        s = s.replace("'", "");
        s = s.replace("\\\\", "\\");
        s = s.replace("%ESCAPED%", "'");
        return s;
    }

    /**
     * Normalizes an array expression by normalizing every element.
     * @param s string of the array to be normalized (e.g. "[a, 3+1]")
     * @return the normalized array (e.g. "['hello', 4]")
     * */
    private String normalizeArray(String s) {

        String parameterString = s.substring(1, s.length()-1);
        String[] elements = normalizeListExpression(parameterString);
        Object[] objects = new Object[elements.length];
        for(int i = 0; i < elements.length; i++)
            objects[i] = convertToRepresentedObject(elements[i]);
        return new JSONArray("[" + String.join(",", elements) + "]").toString();
    }

    /**
     * Normalizes a json expression.
     * @param s string of the json to be normalized (e.g. "{product: 2*16}")
     * @return the normalized json (e.g. "{'apple': 32}")
     * */
    private String normalizeJSON(String s) {

        if(s.equals("{}")) return s;

        // remove braces
        String parameterString = s.substring(1, s.length()-1);

        // process attribute list
        String[] elements = parameterString.split(",");
        for(int i = 0; i < elements.length; i++) {
            String name = normalizeValueExpression(elements[i].split(":")[0]);
            String value = normalizeValueExpression(elements[i].split(":")[1]);
            elements[i] = name + ": " + value;
        }

        // convert back to json string
        return "{" + String.join(",", elements) + "}";
    }

    /**
     * Normalizes an indexed expression e.g.: var[4]['name']).
     * @param indexedExpression string of the indexed expression to be normalized (e.g. "var[4]['name']")
     * @return the value written at the respective index (e.g. "'anton'")
     * */
    private String normalizeIndexed(String indexedExpression) {

        String[] splits = indexedExpression.split("\\$");
        Object parent = convertToRepresentedObject(variables.get(splits[0]));
        for(int i = 1; i < splits.length; i+=2) {
            String index = normalizeValueExpression(getSubstructureContent(splits[i]));
            if(index.startsWith("\'")) {
                JSONObject o = (JSONObject)(parent);
                if(o == null || !o.has(unescapeString(index))) return null;
                parent = o.get(unescapeString(index));
            } else {
                int indexInt = Integer.parseInt(index);
                JSONArray a = (JSONArray)parent;
                if(indexInt < 0 || indexInt >= a.length())
                    throw new QLIndexNotExistendException(a.toString(), index);
                parent = a.get(indexInt);
            }
        }

        if(parent instanceof String)
            return "'"+parent+"'";

        return parent.toString();
    }

    /**
     * Assigns a value to a certain index of a JSON object or list/array.
     * @param indexedExpression indexed expression which shall be assigned to (e.g. "var[4]['name']")
     * @param assignmentValue   value which shall be assigned
     * */
    private void assignToIndexable(String indexedExpression, String assignmentValue) {

        // traverse to build hierarchy
        String[] splits = indexedExpression.split("\\$");
        String s = variables.get(splits[0]);
        Object o = s.charAt(0) == '{' ? new JSONObject(s) : new JSONArray(s);
        Object prevO = null;
        Object mainObject = o;
        Object assignmentObject = convertToRepresentedObject(assignmentValue);

        for(int i = 1; i < splits.length; i+=2) {
            String index = normalizeValueExpression(getSubstructureContent(splits[i]));

            if(index.startsWith("\'")) {
                if(i == splits.length-1)
                    ((JSONObject)o).put(unescapeString(index), assignmentObject);
                else {
                    JSONObject p = (JSONObject) o;
                    if(!p.has(unescapeString(index))) throw new QLIndexNotExistendException(p.toString(), index);
                    o = p.get(unescapeString(index));
                }
            } else {

                if(i == splits.length-1)
                    ((JSONArray)o).put(Integer.parseInt(index), assignmentObject);
                else {
                    JSONArray p = (JSONArray) o;
                    if(p.length() <= Integer.parseInt(index)) throw new QLIndexNotExistendException(p.toString(), index);
                    o = p.get(Integer.parseInt(index));
                }

            }
        }
        variables.put(splits[0], mainObject.toString());
    }

    private Object convertToRepresentedObject(String s) {

        if(s == null) return null;
        if(s.startsWith("\'"))
            return s.substring(1, s.length()-1);
        else if(s.startsWith("{"))
            return new JSONObject(s);
        else if(s.startsWith("["))
            return new JSONArray(s);
        else if(OBJECT_NUMBER.matcher(s).find()) {
            Number n = MathOperations.parse(s);
            return s.contains(".") ? n.doubleValue() : n.intValue();
        }
        return null;
    }

    public OracleWriter getOracleWriter() {
        return oracleWriter;
    }

    public boolean isInTestMode() {
        return inTestMode;
    }
}

class ObjectContainer {

    Object o;

    ObjectContainer(Object o) {
        this.o = o;
    }
}