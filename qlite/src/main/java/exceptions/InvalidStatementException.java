package exceptions;

public class InvalidStatementException extends RuntimeException {

    public InvalidStatementException(String error, Throwable cause) {
        super(error, cause);
    }
}
