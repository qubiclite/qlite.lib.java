package qlvm.exceptions.runtime;

public class UnknownCommandException extends QLRunTimeException {
    public UnknownCommandException(String command) {
        super("unknown command: "+command+"");
    }
}
