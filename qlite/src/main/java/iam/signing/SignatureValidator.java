package iam.signing;

import tangle.TryteTool;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class SignatureValidator {

    private KeyFactory keyFactory;

    /**
     * Verifies the correctness of a signature for a certain message and public key.
     * @param pubKeyTrytes    the public key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param signatureTrytes the signature encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param message         the message for which the signature was created
     * @return TRUE = valid/correct signature, FALSE = invalid/incorrect signature
     * */
    public boolean validate(String pubKeyTrytes, String signatureTrytes, String message) {

        if(keyFactory == null)
            try {
                keyFactory = KeyFactory.getInstance(SignatureConstants.KEY_PAIR_GENERATOR_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

        byte[] pubKeyBytes;
        byte[] signatureBytes;

        pubKeyBytes = TryteTool.trytesToBytes(pubKeyTrytes);
        signatureBytes = TryteTool.trytesToBytes(signatureTrytes);

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey pubKey;

        try {
            pubKey =  keyFactory.generatePublic(pubKeySpec);;
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
            signature.initVerify(pubKey);
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
