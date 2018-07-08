package qlvm.exceptions.runtime;

public abstract class QLException extends RuntimeException {

    protected QLException(String msg) {
        super(msg);
    }
}
