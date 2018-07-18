package exceptions;

public class InvalidQubicTransactionException extends RuntimeException {

    public InvalidQubicTransactionException(String error) {
        super(error);
    }
}
