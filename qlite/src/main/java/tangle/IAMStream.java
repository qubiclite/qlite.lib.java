package tangle;

import org.apache.commons.lang3.StringUtils;

public abstract class IAMStream {

    private static final int TRYTES_PER_ADDRESS = 81;
    private static final int STREAM_HANDLE_LENGTH = 30;

    /**
     * Builds the address of a stream element at a specific index.
     * @param index the index for which the address shall be built
     * @return the address built
     * */
    public String buildAddress(long index) {
        String indexTrytes = TryteTool.positiveLongToTrytes(index);
        String streamHandle = getID().substring(0, STREAM_HANDLE_LENGTH);
        return streamHandle + StringUtils.leftPad(indexTrytes, TRYTES_PER_ADDRESS-STREAM_HANDLE_LENGTH, '9');
    }

    /**
     * @return IAMStream ID = transaction hash of public key attachment
     * */
    public abstract String getID();
}
