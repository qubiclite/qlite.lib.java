package qlvm.functions.operations;


import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class MathOperations {

    private static final String[] operatorArray = {">=", "<=", ">", "<", "+", "-", "*", "/", "%", "^"};
    private static final NumberFormat NF = NumberFormat.getInstance(Locale.US);

    public static String doOperation(String operator, String[] par) {
        // string concatenation
        if(operator.equals("+") && (par[0].charAt(0) == '\'' || par[1].charAt(0) == '\'')) {
            if(par[0].charAt(0) == '\'') par[0] = par[0].substring(1, par[0].length()-1);
            if(par[1].charAt(0) == '\'') par[1] = par[1].substring(1, par[1].length()-1);
            return '\''+par[0] + par[1]+'\'';
        }

        Number a = parse(par[0]);
        Number b = parse(par[1]);

        switch (operator) {

            // numeric return

            case "+":
                if((par[0]+par[1]).contains("."))
                    return "" + (a.doubleValue() + b.doubleValue());
                return "" + (a.intValue() + b.intValue());
            case "-":
                if((par[0]+par[1]).contains("."))
                    return "" + (a.doubleValue() - b.doubleValue());
                return "" + (a.intValue() - b.intValue());
            case "*":
                if((par[0]+par[1]).contains("."))
                    return "" + (a.doubleValue() * b.doubleValue());
                return "" + (a.intValue() * b.intValue());
            case "/":
                if((par[0]+par[1]).contains("."))
                    return "" + (a.doubleValue() / b.doubleValue());
                return "" + (a.intValue() / b.intValue());
            case "^":
                if((par[0]+par[1]).contains("."))
                    return "" + Math.pow(a.doubleValue(), b.doubleValue());
                return "" + (int)Math.pow(a.intValue(), b.intValue());
            case "%":
                if((par[0]+par[1]).contains("."))
                    return "" + (a.doubleValue() % b.doubleValue());
                return "" + (a.intValue() % b.intValue());

            // boolean return

            case ">=":
                return (a.doubleValue() >= b.doubleValue()) ? "1" : "0";
            case "<=":
                return (a.doubleValue() <= b.doubleValue()) ? "1" : "0";
            case ">":
                return (a.doubleValue() > b.doubleValue()) ? "1" : "0";
            case "<":
                return (a.doubleValue() < b.doubleValue()) ? "1" : "0";

            default:
                // TODO throw qlvm exception
                return "<< UNKNOWN OPERATION " + operator +">>";
        }
    }

    public static Number parse(String s) {
        if(s == null) return 0;
        try {
            return NF.parse(s);
        } catch (ParseException | NumberFormatException e) {
            System.err.println("failed to parse string '"+s+"'");
            e.printStackTrace();
            // TODO throw qlvm exception
            return null;
        }
    }

    public static String[] getOperatorArray() {
        return operatorArray;
    }
}
