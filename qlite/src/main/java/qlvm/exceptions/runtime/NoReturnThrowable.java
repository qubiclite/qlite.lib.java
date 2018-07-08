package qlvm.exceptions.runtime;

public class NoReturnThrowable extends RuntimeException {

    public NoReturnThrowable() {
        super("code was completely executed but no return() function was called");
    }
}
