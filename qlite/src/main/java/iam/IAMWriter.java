package iam;

import constants.TangleJSONConstants;
import exceptions.IotaAPICallFailedException;
import iam.exceptions.CorruptIAMStreamException;
import iam.exceptions.IAMPacketSizeLimitExceeded;
import iam.signing.Signer;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author microhash
 *
 * IAMWriter acts as interface to the TangleAPI. It allows for convenient publishing
 * of authentificated (= signed) data to the tangle. Its the writing counterpart to:
 * @see IAMReader
 * */
public class IAMWriter extends IAMStream {

    private final String id;
    private final Signer signer = createSigner();

    private static final int MAX_CHARS_PER_FRAGMENT = TryteTool.TRYTES_PER_TRANSACTION_MESSAGE / TryteTool.TRYTES_PER_BYTE; // = BYTES PER TRANSACTION

    /**
     * Creates a new key pair and attaches the public key to the tangle.
     * The resulting transaction hash serves as ID for the IAM Stream.
     * */
    public IAMWriter() {
        String publicKeyTrytes = signer.getPublicKeyTrytes();
        id = TangleAPI.getInstance().sendTrytes(publicKeyTrytes);
    }

    /**
     * Loads an existent IAMWriter whose identity has already been published to the tangle.
     * @param id               IAM stream ID (transaction hash of the root transaction containing the public key)
     * @param privateKeyTrytes the private key encoded to trytes
     * @throws NullPointerException      if id is null
     * @throws InvalidParameterException if id is not tryte sequence of length 81
     * @throws CorruptIAMStreamException if cannot find root transaction (transaction whose hash is the id)
     * @throws InvalidKeySpecException   if key specification is invalid
     * */
    public IAMWriter(String id, String privateKeyTrytes) throws InvalidKeySpecException {
        validateID(id = id.toUpperCase());
        this.id = id;
        initSignerKeys(id, privateKeyTrytes);
    }

    /**
     * Signs and attaches a JSONObject to the tangle. The tangle address is deterministically derived from the attribute 'id'.
     * The derivation method is documented in the Qubic Lite whitepaper (http://qubiclite.org/whitepaper.pdf).
     * @param index   the index at which the message shall be attached in the IAM stream
     * @param message the jsonObject to attach, .toString() has to return an ASCII encoded string
     * @return hash of sent iota transaction
     * @throws InvalidParameterException if index is negative
     * */
    public String write(IAMIndex index, JSONObject message) throws IotaAPICallFailedException {
        if(!TryteTool.isAsciiString(message.toString()))
            throw new InvalidParameterException("parameter message contains non-ascii characters");
        String signature = generateIAMPacketSignature(index, message);
        JSONObject iamPacket = buildIAMPacket(signature, message);
        return publishIAMPacketInFragments(iamPacket.toString(), buildAddress(index));
    }

    public String getID() {
        return id;
    }

    public String getPrivateKeyTrytes() {
        return signer.getPrivateKeyTrytes();
    }

    private void initSignerKeys(String id, String privateKeyTrytes) throws InvalidKeySpecException {
        String publicKeyTrytes = TangleAPI.getInstance().readTransactionTrytes(id);
        if(publicKeyTrytes == null)
            throw new CorruptIAMStreamException("failed loading base transaction for IAM stream: '"+id+"'", null);
        signer.loadKeysFromTrytes(privateKeyTrytes, publicKeyTrytes);
    }

    private static void validateID(String id) {
        if(id == null)
            throw new NullPointerException("parameter id is null");
        if(!TryteTool.isTryteSequence(id))
            throw new InvalidParameterException("parameter id is not a tryte sequence");
        if(id.length() != 81)
            throw new InvalidParameterException("parameter id is required to be exactly 81 trytes long");
    }

    private static Signer createSigner() {
        try {
            return new Signer();
        } catch (NoSuchAlgorithmException e) {
            throw new CorruptIAMStreamException("failed initializing signature object", e);
        }
    }

    private String generateIAMPacketSignature(IAMIndex index, JSONObject message) {
        String stringToSign = buildStringToSignForIAMPacket(index, message);
        return signer.sign(stringToSign);
    }

    private JSONObject buildIAMPacket(String signature, JSONObject message) {
        JSONObject iamPacket = new JSONObject();
        iamPacket.put(TangleJSONConstants.IAM_PACKET_MESSAGE,  message);
        iamPacket.put(TangleJSONConstants.IAM_PACKET_SIGNATURE, signature);
        return iamPacket;
    }

    private static String publishIAMPacketInFragments(String iamPacketString, String address) {

        String[] fragments = fragmentIAMPacket(iamPacketString);
        StringBuilder hashBlock = new StringBuilder();

        for(int i = fragments.length-1; i >= 1; i--) {
            String hash = TangleAPI.getInstance().sendMessage(fragments[i]);
            hashBlock.insert(0, hash);
        }

        fragments[0] = hashBlock + fragments[0];
        return TangleAPI.getInstance().sendMessage(address, fragments[0]);
    }

    private static String[] fragmentIAMPacket(String iamPacketString) {

        int amountOfFragments = predictAmountOfFragments(iamPacketString.length());

        if(amountOfFragments > MAX_FRAGMENTS_PER_IAM_PACKET)
            throw new IAMPacketSizeLimitExceeded();

        int hashBlockLength = TryteTool.TRYTES_PER_HASH * (amountOfFragments-1);
        String[] fragments = new String[amountOfFragments];

        fragments[0] = iamPacketString.substring(0, Math.min(iamPacketString.length(), MAX_CHARS_PER_FRAGMENT-hashBlockLength));
        for(int fragmentIndex = 1; fragmentIndex < fragments.length; fragmentIndex++) {
            int cutStart = fragmentIndex * MAX_CHARS_PER_FRAGMENT - hashBlockLength;
            int cutEnd = Math.min(iamPacketString.length(), cutStart + MAX_CHARS_PER_FRAGMENT);
            fragments[fragmentIndex] = iamPacketString.substring(cutStart, cutEnd);
        }

        return fragments;
    }

    private static int predictAmountOfFragments(int contentLength) {
        int oldEstimate = 0, newEstimate = 1;
        while (oldEstimate != newEstimate) {
            oldEstimate = newEstimate;
            int hashBlockLengthEstimate = TryteTool.TRYTES_PER_HASH * (oldEstimate-1);
            newEstimate = (int)Math.ceil((double)(contentLength + hashBlockLengthEstimate)/MAX_CHARS_PER_FRAGMENT);
        }
        return newEstimate;
    }
}