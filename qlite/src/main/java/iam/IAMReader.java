package iam;

import iam.exceptions.CorruptIAMStreamException;
import exceptions.IncompleteIAMChainException;
import iam.signing.SignatureValidator;
import jota.model.Transaction;
import org.json.JSONObject;
import tangle.TangleAPI;

import java.util.List;

/**
 * @author microhash
 *
 * IAMReader acts as interface to the TangleAPI. It allows for convenient reading
 * of authentificated (= signed) data to the tangle. Its the reading counterpart to:
 * @see IAMWriter
 * */
public class IAMReader extends IAMStream {

    private final String id;
    private final String publicKeyTrytes;

    /**
     * Creates the IAMReader for a specific IAMWriter ID. Fetches the according public Key.
     * @param id the id of the IAMStream to read
     * */
    public IAMReader(String id) throws CorruptIAMStreamException {
        this.id = id;
        try {
            publicKeyTrytes = TangleAPI.getInstance().readTransactionTrytes(id);
        } catch (IncompleteIAMChainException e) {
            throw new CorruptIAMStreamException("the iam stream root message chain could not be read completely ("+e.getMessage()+")", e);
        }
    }

    public JSONObject read(IAMIndex index) {
        return readFromSelection(index, null);
    }

    /**
     * Reads the JSONObject from the IAMStream at a custom index.
     * @param selection resource of pre-fetched transactions for efficiency purposes, optional (set to null if not required)
     * @param index     the index for which the message shall be fetched
     * @return the read JSONObject, NULL if no transaction with a valid signature found.
     * */
    public JSONObject readFromSelection(IAMIndex index, List<Transaction> selection) {

        IAMPacketFilter iamPacketFilter = new IAMPacketFilter(this, index);
        iamPacketFilter.setSelection(selection);
        List<IAMPacket> allValidIAMPackets = iamPacketFilter.findAllValidIAMPackets();
        return findConsensusMessageAmongIAMPackets(allValidIAMPackets);
    }

    public String getID() {
        return id;
    }

    private JSONObject findConsensusMessageAmongIAMPackets(List<IAMPacket> validIAMPackets) {
        IAMPacket consensusPacket = validIAMPackets.size() > 0 ? validIAMPackets.get(0) : null;
        for(IAMPacket iamPacket : validIAMPackets)
            if(!iamPacket.equals(consensusPacket))
                consensusPacket = null;
        return consensusPacket != null ? consensusPacket.getMessage() : null;
    }

    boolean isValidIAMPacket(IAMIndex index, IAMPacket iamPacket) {
        if(iamPacket == null)
            return false;
        return SignatureValidator.validate(publicKeyTrytes, iamPacket.getSignature(), buildStringToSignForIAMPacket(index, iamPacket.getMessage()));
    }
}
