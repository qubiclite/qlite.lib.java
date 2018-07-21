package tangle;

import constants.TangleJSONConstants;
import exceptions.CorruptIAMStreamException;
import jota.model.Transaction;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.List;

/**
 * @author microhash
 *
 * IAMPublisher acts as interface to the TangleAPI. It allows for convenient publishing
 * of authentificated (= signed) data to the tangle. Its the writing counterpart to:
 * @see IAMReader
 * */
public class IAMPublisher extends IAMStream {

    private final String id;
    private final Signer signer = new Signer();

    /**
     * Creates a key pair and attaches the public key to the tangle.
     * The resulting transaction hash serves as ID for this object.
     * */
    public IAMPublisher() {
        signer.generateKeys();
        List<Transaction> txList = TangleAPI.getInstance().sendTransfer(TryteTool.generateRandom(81), signer.getPublicKeyTrytes(), false);
        id = txList.get(0).getHash();
    }

    /**
     * Loads an existent IAMPublisher whose identity has already been
     * published to the tangle.
     * @param id id of the IAMStream
     * @param privKeyTrytes the private key encoded in trytes
     * */
    public IAMPublisher(String id, String privKeyTrytes) {
        this.id = id;
        String pubKeyTrytes = TangleAPI.getInstance().findTransactionByHash(id, false);
        if(pubKeyTrytes == null)
            throw new CorruptIAMStreamException("failed loading root transaction for IAM stream: '"+id+"'", null);
        signer.loadKeysFromTrytes(privKeyTrytes, pubKeyTrytes);
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
        messageContainer.put(TangleJSONConstants.TANGLE_PUBLISHER_SIGNATURE, signer.sign(content.toString()));
        String hash = sendDataInFragments(index, messageContainer);
        return hash;
    }

    private String sendDataInFragments(int index, JSONObject data) {
        String dataString = data.toString();

        final int FRAGMENT_LENGTH = (2187)/2;

        for(int i = dataString.length(); i >= 0; i-=FRAGMENT_LENGTH) {

            boolean last = i-FRAGMENT_LENGTH <= 0;

            // publish fragment
            String subString = dataString.substring(Math.max(i-FRAGMENT_LENGTH, 0), i);
            String address = last ? buildAddress(index) : StringUtils.repeat('9', 81);
            String hash = TangleAPI.getInstance().sendTransfer(address, subString, true).get(0).getHash();

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