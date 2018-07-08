package qlvm.exceptions.runtime;

public class UnknownFunctionException extends QLRunTimeException {
    public UnknownFunctionException(String command) {
        super("unknown function: '"+command+"()'");
    }
}
