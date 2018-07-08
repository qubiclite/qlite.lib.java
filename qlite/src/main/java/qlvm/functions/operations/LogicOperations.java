package qlvm.functions.operations;

public class LogicOperations {

    private static final String[] operatorArray = {"&&", "||", "!=", "==", "!"};

    public static String doOperation(String operator, String[] par) {

        switch (operator) {

            // equality checks
            case "==":
                return par[0].equals(par[1]) ? "1" : "0";
            case "!=":
                return par[0].equals(par[1]) ? "0" : "1";

            // logical operations
            case "!":
                return stringConditionToBoolean(par[1]) ? par[0] + "0" : par[0] + "1";
            case "&&":
                return stringConditionToBoolean(par[0]) && stringConditionToBoolean(par[1]) ? "1" : "0";
            case "||":
                return stringConditionToBoolean(par[0]) && stringConditionToBoolean(par[1]) ? "0" : "1";
            default:
                return "<< UNKNOWN OPERATION " + operator +">>";
        }
    }

    /**
     * Maps normalized values to booleans. Thus allowing them to be used as expression in if/while statements
     * in the QLVM class and for logical operations in this class.
     * @param condition any string that shall be used as a boolean condition
     * @return TRUE = condition is true, FALSE = condition is false
     * */
    public static boolean stringConditionToBoolean(String condition) {
        return condition != null && !condition.equals("") && !condition.equals("0") && !condition.equals("0.0");
    }

    public static String[] getOperatorArray() {
        return operatorArray;
    }
}
