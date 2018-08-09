package oracle;

import iam.IAMIndex;
import iam.IAMKeywordReader;
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

    private final IAMReader reader;
    private final IAMKeywordReader resultReader, hashReader;

    // a list of all already fetched valid statements (for efficiency purposes)
    private final HashMap<Integer, ResultStatement> resultStatements = new HashMap<>();
    private final HashMap<Integer, HashStatement> hashStatements = new HashMap<>();

    /**
     * Initializes TangleReaders for HashStatements and ResultStatements.
     * @param id IAM stream ID (= hash of IAM stream's root transaction)
     * */
    public OracleReader(String id) throws CorruptIAMStreamException {
        reader = new IAMReader(id);
        resultReader = new IAMKeywordReader(reader, OracleWriter.ORACLE_RESULT_STREAM_KEYWORD);
        hashReader = new IAMKeywordReader(reader, OracleWriter.ORACLE_HASH_STREAM_KEYWORD);
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

        final IAMKeywordReader reader = isHashStatement ? hashReader : resultReader;

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
        return reader.getID();
    }

    public IAMKeywordReader getResultReader() {
        return resultReader;
    }

    public IAMKeywordReader getHashReader() {
        return hashReader;
    }
}
