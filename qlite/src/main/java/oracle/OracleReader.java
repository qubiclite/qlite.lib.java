package oracle;

import constants.TangleJSONConstants;
import iam.exceptions.CorruptIAMStreamException;
import exceptions.InvalidStatementException;
import jota.model.Transaction;
import iam.IAMReader;
import org.json.JSONObject;
import oracle.statements.HashStatement;
import oracle.statements.ResultStatement;
import oracle.statements.Statement;

import java.util.HashMap;
import java.util.List;

/**
 * @author microhash
 *
 * The OracleReader is a single (usually untrusted) q-node instance in the assembly.
 * This class fetches, stores and processes its published qnode.statements. OracleReader
 * is the reading counterpart to OracleWriter:
 * @see OracleWriter
 * */
public class OracleReader {

    private final IAMReader iamResults;
    private final IAMReader iamHashes;

    // a list of all already fetched valid statements (for efficiency purposes)
    private final HashMap<Integer, ResultStatement> resultStatements = new HashMap<>();
    private final HashMap<Integer, HashStatement> hashStatements = new HashMap<>();

    /**
     * Initializes TangleReaders for HashStatements and ResultStatements.
     * @param resultsRoot root of oracles result tangle stream
     * */
    public OracleReader(String resultsRoot) throws CorruptIAMStreamException {
        iamResults = new IAMReader(resultsRoot);

        // reads oracle specification transaction to find root of HashStatement mam channel
        JSONObject oracleSpecTxObj = iamResults.read(0);
        iamHashes = new IAMReader(oracleSpecTxObj.getString(TangleJSONConstants.ORACLE_HASH_STREAM)); // TODO check if exists
    }

    /**
     * Fetches the Statement at a certain position from the respective oracles IAM stream
     * if it exists and is well-formed. Can be used both for HashStatement and ResultStatement.
     * If the Statement has already been fetched before, it will just return the old one.
     * @param preload         resource of prefetched transactions for efficiency purposes, optional (set to null if not required)
     * @param isHashStatement fetches HashStatement if TRUE, ResultStatement if FALSE
     * @param epoch           epoch index for desired Statement
     * @return the fetched Statement, NULL if not existent or malformed
     * */
    Statement readStatement(List<Transaction> preload, boolean isHashStatement, int epoch) {

        HashMap map = (isHashStatement ? hashStatements : resultStatements);
        if(map.containsKey(epoch))
            return (Statement)map.get(epoch);

        final IAMReader reader = isHashStatement ? iamHashes : iamResults;

        // read JSONObject from tangle stream
        JSONObject statObj = reader.readFromSelection(epoch+1, preload); // +1 because address for epoch #0 is ...999A, not 9999
        if(statObj == null)
            return null;

        Statement newStat;

        try {
            newStat = isHashStatement ? HashStatement.fromJSON(statObj) : ResultStatement.fromJSON(statObj);
        } catch (InvalidStatementException e) {
            return null;
        }

        // accept HashStatement because order is correct (HashStatement needs to be received earlier than ResultStatement)
        if(!isHashStatement)
            ((ResultStatement)newStat).setHashEpoch(hashStatements.get(epoch));

        // add to HashMap for future use
        if(isHashStatement)
            hashStatements.put(epoch, (HashStatement) newStat);
        else
            resultStatements.put(epoch, (ResultStatement) newStat);
        return  newStat;
    }

    public String getID() {
        return iamResults.getID();
    }

    public IAMReader getHashIAMStream() {
        return iamHashes;
    }

    public IAMReader getResultIAMStream() {
        return iamResults;
    }
}
