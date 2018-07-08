package qlvm.exceptions.compile;

import qlvm.exceptions.runtime.QLException;

public class QLCompilerException extends QLException {
    public QLCompilerException(String msg) {
        super(msg);
    }
}
