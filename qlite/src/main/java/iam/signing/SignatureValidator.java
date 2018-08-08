package iam.signing;

import tangle.TryteTool;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public enum SignatureValidator {;

    private static final KeyFactory keyFactory;

    static {
        try {
            keyFactory = KeyFactory.getInstance(SignatureConstants.KEY_PAIR_GENERATOR_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies the correctness of a signature for a certain message and public key.
     * @param publicKeyTrytes the public key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param signatureTrytes the signature encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param message         the message for which the signature was created
     * @return TRUE = valid/correct signature, FALSE = invalid/incorrect signature
     * */
    public static boolean validate(String publicKeyTrytes, String signatureTrytes, String message) {

        byte[] publicKeyBytes = TryteTool.trytesToBytes(publicKeyTrytes);
        byte[] signatureBytes = TryteTool.trytesToBytes(signatureTrytes);

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey;

        try {
            publicKey =  keyFactory.generatePublic(publicKeySpec);;
        } catch (InvalidKeySpecException e) {
            return false;
        }

        Signature signature;

        try {
            signature = Signature.getInstance(SignatureConstants.SIGNATURE_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        try {
            signature.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        byte[] msgBytes = message.getBytes();
        byte[] buffer = new byte[SignatureConstants.KEY_SIZE];

        try {
            for (byte b : msgBytes)
                signature.update(buffer, 0, b);
            return signature.verify(signatureBytes);
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }
}
