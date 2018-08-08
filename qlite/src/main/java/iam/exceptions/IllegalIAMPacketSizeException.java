package iam.exceptions;

import iam.IAMStream;

public class IllegalIAMPacketSizeException extends RuntimeException {

    public IllegalIAMPacketSizeException(String baseHash) {
        super("the iam packet starting with the transaction '"+baseHash+"' exceedes the maximum length of " + IAMStream.MAX_FRAGMENTS_PER_IAM_PACKET + " transactions");
    }
}