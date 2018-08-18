package exceptions;

public class NoQubicTransactionException extends InvalidQubicTransactionException {

    public NoQubicTransactionException() {
        super("could not find qubic transaction", null);
    }
}
