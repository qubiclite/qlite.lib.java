package oracle;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ResultHasher {

    private static MessageDigest digest;

    static {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates hash for HashStatement.
     * @param salt salt (a random nonce to prevent rainbow tables)
     * @param result result string for subsequent ResultStatement
     **/
    public static String hash(String salt, String result) {
        return new String(Hex.encode(digest.digest((salt+result).getBytes(StandardCharsets.US_ASCII))));
    }
}