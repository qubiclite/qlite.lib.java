package iam;

import constants.TangleJSONConstants;
import exceptions.CorruptIAMStreamException;
import iam.signing.Signer;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;

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

    /**
     * Creates a key pair and attaches the public key to the tangle.
     * The resulting transaction hash serves as ID for this object.
     * */
    public IAMWriter() {
        id = TangleAPI.getInstance().sendTransaction(signer.getPublicKeyTrytes(), false);
    }

    /**
     * Loads an existent IAMWriter whose identity has already been
     * published to the tangle.
     * @param id id of the IAMStream
     * @param privateKeyTrytes the private key encoded in trytes
     * */
    public IAMWriter(String id, String privateKeyTrytes) {
        validateID(id);
        this.id = id;
        initSignerKeys(id, privateKeyTrytes);
    }

    /**
     * Initializes the signer's keys by fetching the public key from the IAM root transaction.
     * @throws CorruptIAMStreamException if cannot find root transaction
     * */
    private void initSignerKeys(String id, String privateKeyTrytes) {
        String publicKeyTrytes = TangleAPI.getInstance().readTransactionMessage(id, false);
        if(publicKeyTrytes == null)
            throw new CorruptIAMStreamException("failed loading base transaction for IAM stream: '"+id+"'", null);
        signer.loadKeysFromTrytes(privateKeyTrytes, publicKeyTrytes);
    }

    /**
     * Throws exceptions if parameter id cannot be a transaction hash (tryte sequence of length 81).
     * @param id IAM stream ID to validate
     * @throws NullPointerException if id is null
     * @throws InvalidParameterException if id is not tryte sequence of length 81
     * */
    private static void validateID(String id) {
        if(id == null)
            throw new NullPointerException("parameter id is null");
        if(!TryteTool.isTryteSequence(id))
            throw new InvalidParameterException("parameter id is not a tryte sequence");
        if(id.length() != 81)
            throw new InvalidParameterException("parameter id is required to be exactly 81 trytes long");
    }

    private Signer createSigner() {
        try {
            return new Signer();
        } catch (NoSuchAlgorithmException e) {
            throw new CorruptIAMStreamException("failed initializing signature object", e);
        }
    }

    /**
     * Attaches a signed JSONObject to the tangle. The publishing tangle address
     * used for this is deterministically derived from the attribute 'id'.
     * @param content the jsonObject to attach
     * @return hash of sent iota transaction
     * */
    public String publish(int index, JSONObject content) {
        JSONObject messageContainer = new JSONObject();
        messageContainer.put(TangleJSONConstants.TANGLE_PUBLISHER_CONTENT,  content);
        messageContainer.put(TangleJSONConstants.TANGLE_PUBLISHER_SIGNATURE, signer.sign(index + "!" + content.toString()));
        return sendDataInFragments(index, messageContainer);
    }

    private String sendDataInFragments(int index, JSONObject data) {
        String dataString = data.toString();

        final int FRAGMENT_LENGTH = (2187)/2;

        for(int i = dataString.length(); i >= 0; i-=FRAGMENT_LENGTH) {

            boolean last = i-FRAGMENT_LENGTH <= 0;

            // publish fragment
            String subString = dataString.substring(Math.max(i-FRAGMENT_LENGTH, 0), i);
            String address = last ? buildAddress(index) : StringUtils.repeat('9', 81);
            String hash = TangleAPI.getInstance().sendTransaction(address, subString, true);

            // finish if last
            if(last) return hash;

            // append hash to next transaction
            dataString = dataString.substring(0, i-FRAGMENT_LENGTH) + hash+">";
            i += 82;
        }
        // something went wrong?
        return null;
    }

    public String getID() {
        return id;
    }

    public String getPrivateKeyTrytes() {
        return signer.getPrivateKeyTrytes();
    }
}