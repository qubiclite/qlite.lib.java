package exceptions;

import constants.GeneralConstants;

public class IllegalIAMStreamLengthException extends RuntimeException {

    public IllegalIAMStreamLengthException(String baseHash) {
        super("the iam stream message '"+baseHash+"' exceedes the maximum chain length of " + GeneralConstants.IAM_MAX_TRANSACTION_CHAIN_LENGTH + " transactions");
    }
}
