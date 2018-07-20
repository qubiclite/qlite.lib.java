package exceptions;

public class IncompleteIAMChainException extends RuntimeException {

    public IncompleteIAMChainException(String txHash) {
        super("failed reading iam chain because unable to find chain element '"+txHash+"'");
    }
}
