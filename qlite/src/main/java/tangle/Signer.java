package tangle;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @author
 *
 * Signer creates and validates signatures and manages the key pair. Signatures
 * are needed in order to authenticate the content and origin of data transactions.
 * */
public class Signer {

    private static final int KEY_SIZE = 1024;
    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String KEY_PAIR_GENERATOR_ALGORITHM = "DSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withDSA";

    private PrivateKey privKey;
    private PublicKey pubKey;
    private Signature signature;
    private KeyFactory keyFactory;

    /**
     * Initializes the Signature object needed both for signing and verifying.
     * */
    public Signer() {
        try {
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates a key pair consisting of a private key and its associated public key.
     * This procedure has to be called before using sign().
     * */
    public void generateKeys() {
        KeyPairGenerator keyGen = null;
        SecureRandom random = null;
        try {
            keyGen = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_ALGORITHM);
            random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        keyGen.initialize(KEY_SIZE, random);
        KeyPair pair = keyGen.generateKeyPair();

        privKey = pair.getPrivate();
        pubKey = pair.getPublic();

        initSignature();
    }

    /**
     * Creates a signature for a given message.
     * @param message the message to be signed
     * @return the signature encoded in trytes (encoder: TryteTool.bytesToTrytes()).
     * */
    public String sign(String message) {
        byte[] msgBytes = message.getBytes();
        byte[] buffer = new byte[KEY_SIZE];

        try {
            // create signature
            for (byte b : msgBytes)
                signature.update(buffer, 0, b);
            byte[] signatureBytes = signature.sign();

            // byte[] -> String

            String signatureString = TryteTool.bytesToTrytes(signatureBytes);
            return signatureString;
        } catch (SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies the correctness of a signature for a certain message and public key.
     * @param pubKeyTrytes    the public key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param signatureTrytes the signature encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * @param message         the message for which the signature was created
     * @return TRUE = valid/correct signature, FALSE = invalid/incorrect signature
     * */
    public boolean verify(String pubKeyTrytes, String signatureTrytes, String message) {

        if(keyFactory == null)
            try {
                keyFactory = KeyFactory.getInstance(KEY_PAIR_GENERATOR_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return false;
            }

        byte[] pubKeyBytes;
        byte[] signatureBytes;

        pubKeyBytes = TryteTool.trytesToBytes(pubKeyTrytes);
        signatureBytes = TryteTool.trytesToBytes(signatureTrytes);

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);

        PublicKey pubKey;
        try {
            pubKey = keyFactory.generatePublic(pubKeySpec);
        } catch (InvalidKeySpecException e) {
            System.err.println("problem with generation of from publicKey: '" + pubKeyTrytes + "'");
            e.printStackTrace();
            return false;
        }

        try {
            signature.initVerify(pubKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }

        byte[] msgBytes = message.getBytes();
        byte[] buffer = new byte[KEY_SIZE];

        try {
            for (byte b : msgBytes)
                signature.update(buffer, 0, b);
            return signature.verify(signatureBytes);
        } catch (SignatureException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return the public key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * */
    public String getPublicKeyTrytes() {
        byte[] pubKeyBytes = pubKey.getEncoded();
        return TryteTool.bytesToTrytes(pubKeyBytes);
    }

    /**
     * @return the private key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * */
    public String getPrivateKeyTrytes() {
        byte[] privKeyBytes = privKey.getEncoded();
        return TryteTool.bytesToTrytes(privKeyBytes);
    }

    /**
     * Sets the key pair to the encoded parameter keys.
     * @param privKeyTrytes tryte encoded private key
     * @param pubKeyTrytes  tryte encoded public key
     * */
    public void loadKeysFromTrytes(String privKeyTrytes, String pubKeyTrytes) {

        byte[] privKeyBytes = TryteTool.trytesToBytes(privKeyTrytes);
        byte[] pubKeyBytes = TryteTool.trytesToBytes(pubKeyTrytes);

        try {
            KeyFactory kf = KeyFactory.getInstance(KEY_PAIR_GENERATOR_ALGORITHM);
            privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privKeyBytes));
            pubKey = kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        }

        initSignature();
    }

    /**
     * Initializes the signature object.
     * */
    private void initSignature() {

        try {
            signature.initSign(privKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
