package oracle;

import oracle.statements.result.ResultStatement;
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
     **/
    public static String hash(ResultStatement resultStatement) {
        String nonced = resultStatement.getNonce()+resultStatement.getContent();
        byte[] noncedBytes = nonced.getBytes(StandardCharsets.US_ASCII);
        return new String(Hex.encode(digest.digest(noncedBytes)));
    }
}