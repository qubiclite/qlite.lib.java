package qlvm.exceptions.runtime;

public class ReturnResultThrowable extends RuntimeException {

    public final String result;

    public ReturnResultThrowable(String result) {
        this.result = result;
    }
}
