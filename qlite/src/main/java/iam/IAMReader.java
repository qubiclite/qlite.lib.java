package iam;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
import exceptions.CorruptIAMStreamException;
import exceptions.IllegalIAMStreamLengthException;
import exceptions.IncompleteIAMChainException;
import iam.signing.SignatureValidator;
import jota.model.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import tangle.TangleAPI;

import java.util.List;
import java.util.Map;

/**
 * @author microhash
 *
 * IAMReader acts as interface to the TangleAPI. It allows for convenient reading
 * of authentificated (= signed) data to the tangle. Its the reading counterpart to:
 * @see IAMWriter
 * */
public class IAMReader extends IAMStream {

    private final String id;
    private final String pubKeyString;
    private static final SignatureValidator signatureValidator = new SignatureValidator();

    /**
     * Creates the IAMReader for a specific IAMWriter ID. Fetches the according public Key.
     * @param id the id of the IAMStream to read
     * */
    public IAMReader(String id) throws CorruptIAMStreamException {
        this.id = id;
        try {
            pubKeyString = TangleAPI.getInstance().readTransactionMessage(id, false);
        } catch (IncompleteIAMChainException e) {
            throw new CorruptIAMStreamException("the iam stream root message chain could not be read completely ("+e.getMessage()+")", e);
        }
    }

    public JSONObject read(int index) {
        return read(null, index);
    }

    /**
     * Reads the JSONObject from the IAMStream at a custom index.
     * @param preload resource of prefetched transactions for efficiency purposes, optional (set to null if not required)
     * @param index   the index for which the message shall be fetched
     * @return the read JSONObject, NULL if no transaction with a valid signature found.
     * */
    public JSONObject read(List<Transaction> preload, int index) {

        String address = buildAddress(index);
        Map<String, String> txMessages = TangleAPI.getInstance().readTransactionsByAddress(preload, address, true);

        for(String hash : txMessages.keySet()) {
            String data;
            JSONObject container;

            try {
                data = readDataInFragments(hash, txMessages.get(hash), 0);
            } catch (IncompleteIAMChainException e) {
                continue;
            }

            try {
                container = new JSONObject(data);
            } catch (JSONException e) {
                continue;
            }

            String signature;
            JSONObject content;

            try {
                signature = container.getString(TangleJSONConstants.TANGLE_PUBLISHER_SIGNATURE);
                content = container.getJSONObject(TangleJSONConstants.TANGLE_PUBLISHER_CONTENT);
            } catch (JSONException e) {
                continue;
            }

            if(signatureValidator.validate(pubKeyString, signature, index + "!" + content.toString()))
                return content;

            System.err.println("INVALID SIGNATURE: " + hash);
        }

        return null;
    }

    /**
     * Reads the full message distributed across multiple transactions via a chain of linked transactions.
     * @param baseTxHash the hash of the first transaction in the chain (required for IllegalIAMStreamLengthException())
     * @param lastTxMsg  the message of the last transaction in the chain
     * @param depth      depth in the current transaction chain (0 for initial call, each recursive calls adds 1),
     *                   allows to prevent maliciously long spam chains
     * @return the concatenated message of all following transactions following the base transaction (including that one)
     * TODO make fragmentation more efficient
     * */
    private String readDataInFragments(String baseTxHash, String lastTxMsg, int depth) {

        if(depth > GeneralConstants.IAM_MAX_TRANSACTION_CHAIN_LENGTH)
            throw new IllegalIAMStreamLengthException(baseTxHash);

        // end reached
        if(!lastTxMsg.endsWith(">"))
            return lastTxMsg;

        // TODO validate nextHash
        String nextTxHash = lastTxMsg.substring(lastTxMsg.length()-82, lastTxMsg.length()-1);
        String nextTxMsg = TangleAPI.getInstance().readTransactionMessage(nextTxHash, true);

        if(nextTxMsg == null)
            throw new IncompleteIAMChainException(nextTxHash);

        String lastTxContent = lastTxMsg.substring(0, lastTxMsg.length()-82);
        String sequel = readDataInFragments(baseTxHash, nextTxMsg, depth+1);
        return lastTxContent + sequel;
    }

    public String getID() {
        return id;
    }
}
