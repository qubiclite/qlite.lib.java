package iam;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import tangle.TryteTool;

public abstract class IAMStream {

    public static final int MAX_FRAGMENTS_PER_IAM_PACKET = 5;
    private static final int STREAM_HANDLE_LENGTH = 30;

    /**
     * Builds the address of a stream element at a specific index.
     * @param index the index for which the address shall be built
     * @return the address built
     * */
    public String buildAddress(long index) {
        String indexTrytes = TryteTool.positiveLongToTrytes(index);
        String streamHandle = getID().substring(0, STREAM_HANDLE_LENGTH);
        return streamHandle + StringUtils.leftPad(indexTrytes, TryteTool.TRYTES_PER_ADDRESS-STREAM_HANDLE_LENGTH, '9');
    }

    /**
     * @param index   index position to which the message is attached in the IAM stream
     * @param message the custom message of the IAM packet.
     * @return string that is required to be signed in the signature field of an IAM packet
     * */
    static String buildStringToSignForIAMPacket(int index, JSONObject message) {
        return index + "!" + String.valueOf(message);
    }

    /**
     * @return IAMStream ID = transaction hash of public key attachment
     * */
    public abstract String getID();
}
