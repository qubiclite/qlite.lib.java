package tangle;

import iam.IAMStream;
import org.apache.commons.lang3.StringUtils;

/**
 * @author microhash
 *
 * TryteTool is a collection of a few basic tryte functions which are needed frequently.
 * */
public class TryteTool {

    private static final char[] chars = "9ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * Generates a random tryte sequence.
     * @param length sequence length
     * @return tryte sequence
     * */
    public static String generateRandom(int length) {
        String trytes = "";
        for(int i = 0; i < length; i++)
            trytes += chars[(int)(Math.random()*chars.length)];
        return trytes;
    }

    /**
     * Builds the global qubic promotion address for the current point in time.
     * @return current global iota address for qubic promotions
     * */
    public static String buildCurrentQubicPromotionAddress() {
        long timestamp = System.currentTimeMillis()/1000;
        timestamp -= timestamp%60;
        String unpaddedAddress = "QLITE99999" + positiveLongToTrytes(timestamp);
        return StringUtils.rightPad(unpaddedAddress, 81, '9');
    }

    /**
     * Maps each long to a unique tryte sequence.
     * @param n the long to convert
     * @return the resulting tryte sequence
     * @see IAMStream
     * */
    public static String positiveLongToTrytes(long n) {
        assert n >= 0;
        String trytes = "";
        for(; n > 0; n /= 26)
            trytes = chars[(int)(n%26)] + trytes;
        return StringUtils.leftPad(trytes, 14, '9');
    }

    /**
     * Encodes a byte array to a tryte sequence. Counterpart to trytesToBytes().
     * @param bytes the byte array to convert
     * @return the resulting tryte sequence
     * */
    public static String bytesToTrytes(byte[] bytes) {
        char[] trytes = new char[bytes.length*2];

        for(int i = 0; i < bytes.length; i++) {
            int b = bytes[i]+128; // +1 to exclude reserved sequence '99' (indicating undefined)
            trytes[2*i] = chars[b/26+1];
            trytes[2*i+1] = chars[b%26+1];
        }

        return new String(trytes);
    }

    /**
     * Decodes a tryte sequence back into a byte array. Counterpart to bytesToTrytes().
     * @param tryteString the tryte sequence to convert
     * @return the decoded byte array
     * */
    public static byte[] trytesToBytes(String tryteString) {

        tryteString = tryteString.split("9")[0];
        char[] trytes = tryteString.toCharArray();

        byte[] bytes = new byte[trytes.length/2];

        for(int j = 0; j < bytes.length; j++) {
            bytes[j] = (byte)(tryteToInt(trytes[2*j])*26 + tryteToInt(trytes[2*j+1])-128); // -1 to undo +1 in bytesToTrytes()
        }

        return bytes;
    }

    public static boolean isTryteSequence(String string) {
        return string.matches("^[A-Z9]*$");
    }

    /**
     * Helper function. Maps a single tryte into its integer position in the tryte alphabet.
     * @param c the tryte to convert
     * @return the integer position of the tryte in the tryte alphabet
     * */
    private static int tryteToInt(char c) {
        return c-'A';
    }
}