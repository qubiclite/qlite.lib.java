package iam.signing;

import tangle.TryteTool;

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

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final Signature signature;

    public Signer() throws NoSuchAlgorithmException {
        super();
        signature = Signature.getInstance(SignatureConstants.SIGNATURE_ALGORITHM);
        generateKeys();
    }

    /**
     * Generates a key pair consisting of a private key and its associated public key.
     * This procedure has to be called before using sign().
     * */
    private void generateKeys() {
        KeyPairGenerator keyGen = null;
        SecureRandom random = null;
        try {
            keyGen = KeyPairGenerator.getInstance(SignatureConstants.KEY_PAIR_GENERATOR_ALGORITHM);
            random = SecureRandom.getInstance(SignatureConstants.SECURE_RANDOM_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        keyGen.initialize(SignatureConstants.KEY_SIZE, random);
        KeyPair pair = keyGen.generateKeyPair();

        privateKey = pair.getPrivate();
        publicKey = pair.getPublic();

        initSignature();
    }

    /**
     * Creates a signature for a given message.
     * @param message the message to be signed, ASCII encoded
     * @return the signature encoded in trytes (encoder: TryteTool.bytesToTrytes()).
     * */
    public String sign(String message) {
        byte[] msgBytes = message.getBytes();
        byte[] buffer = new byte[SignatureConstants.KEY_SIZE];

        try {

            // create signature
            for (byte b : msgBytes)
                signature.update(buffer, 0, b);

            byte[] signatureBytes = signature.sign();

            // byte[] -> String

            return TryteTool.bytesToTrytes(signatureBytes);
        } catch (SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return the public key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * */
    public String getPublicKeyTrytes() {
        byte[] pubKeyBytes = publicKey.getEncoded();
        return TryteTool.bytesToTrytes(pubKeyBytes);
    }

    /**
     * @return the private key encoded in trytes (encoder: TryteTool.bytesToTrytes())
     * */
    public String getPrivateKeyTrytes() {
        byte[] privateKeyBytes = privateKey.getEncoded();
        return TryteTool.bytesToTrytes(privateKeyBytes);
    }

    /**
     * Sets the key pair to the encoded parameter keys.
     * @param privateKeyTrytes tryte encoded private key
     * @param publicKeyTrytes  tryte encoded public key
     * */
    public void loadKeysFromTrytes(String privateKeyTrytes, String publicKeyTrytes) throws InvalidKeySpecException {

        byte[] privateKeyBytes = TryteTool.trytesToBytes(privateKeyTrytes);
        byte[] publicKeyBytes = TryteTool.trytesToBytes(publicKeyTrytes);

        try {
            KeyFactory kf = KeyFactory.getInstance(SignatureConstants.KEY_PAIR_GENERATOR_ALGORITHM);
            privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            publicKey = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        initSignature();
    }

    /**
     * Initializes the signature object.
     * */
    private void initSignature() {

        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
