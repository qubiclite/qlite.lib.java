package tangle;

import constants.TangleJSONConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * @author microhash
 *
 * IAMReader acts as interface to the TangleAPI. It allows for convenient reading
 * of authentificated (= signed) data to the tangle. Its the reading counterpart to:
 * @see IAMPublisher
 * */
public class IAMReader extends IAMStream {

    private final String id;
    private final String pubKeyString;
    private final Signer signer = new Signer();

    private int index = 0;

    /**
     * Creates the IAMReader for a specific IAMPublisher ID. Fetches the according public Key.
     * @param id the id of the IAMStream to read
     * */
    public IAMReader(String id) {
        this.id = id;
        pubKeyString = TangleAPI.getInstance().findTransactionByHash(id, false);
    }

    /**
     * Reads the JSONObject from the IAMStream at a custom index.
     * @param index the index for which the message shall be fetched
     * @return the read JSONObject, NULL if no transaction with a valid signature found.
     * */
    public JSONObject read(int index) {
        String[] txs;

        String address = buildAddress(index);
        txs = TangleAPI.getInstance().findTransactionsByAddress(address, true);

        for(String tx : txs) {
            String data = readDataInFragments(tx);
            JSONObject container;

            try {
                container = new JSONObject(data);
            } catch (JSONException e) {
                continue;
            }

            // TODO make sure that json attributes exists
            String signature = container.getString(TangleJSONConstants.TANGLE_PUBLISHER_SIGNATURE);
            JSONObject content = container.getJSONObject(TangleJSONConstants.TANGLE_PUBLISHER_CONTENT);

            if(signer.verify(pubKeyString, signature.toString(), content.toString()))
                return content;
            System.err.println("INVALID SIGNATURE: " + tx.toString());
        }

        return null;
    }

    /**
     * Reads the full message distributed across multiple transactions via a chain of linked transactions.
     * @param baseTxMsg the ascii message of any transaction in the chain
     * @return the concatenated message of all following transactions following the base transaction (including that one)
     * */
    private String readDataInFragments(String baseTxMsg) {

        // TODO abort if too long

        // end reached
        if(!baseTxMsg.endsWith(">"))
            return baseTxMsg;

        // TODO validate nextHash
        String nextHash = baseTxMsg.substring(baseTxMsg.length()-82, baseTxMsg.length()-1);
        String content = baseTxMsg.substring(0, baseTxMsg.length()-82);
        String sequel = readDataInFragments(TangleAPI.getInstance().findTransactionByHash(nextHash, true));
        return content + sequel;
    }

    public String getID() {
        return id;
    }

    public int getNextIndex() {
        return index;
    }
}
