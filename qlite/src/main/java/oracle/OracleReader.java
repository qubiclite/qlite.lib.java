package oracle;

import iam.IAMIndex;
import iam.IAMKeywordReader;
import iam.exceptions.CorruptIAMStreamException;
import exceptions.InvalidStatementException;
import jota.model.Transaction;
import iam.IAMReader;
import oracle.statements.*;
import org.json.JSONObject;

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

    private final HashStatementReader hashStatementReader;
    private final ResultStatementReader resultStatementReader;

    // a list of all already fetched valid statements (for efficiency purposes)
    private final HashMap<Integer, ResultStatement> resultStatements = new HashMap<>();

    /**
     * Initializes TangleReaders for HashStatements and ResultStatements.
     * @param id IAM stream ID (= hash of IAM stream's root transaction)
     * */
    public OracleReader(String id) throws CorruptIAMStreamException {
        reader = new IAMReader(id);
        hashStatementReader = new HashStatementReader(reader);
        resultStatementReader = new ResultStatementReader(reader, hashStatementReader);
    }

    public ResultStatement readResultStatement(int epoch) {
        return readResultStatement(null, epoch);
    }

    public ResultStatement readResultStatement(List<Transaction> preload, int epoch) {
        return resultStatementReader.read(preload, epoch);
    }

    public HashStatement readHashStatement(int epoch) {
        return readHashStatement(null, epoch);
    }

    public HashStatement readHashStatement(List<Transaction> preload, int epoch) {
        return hashStatementReader.read(preload, epoch);
    }

    public Statement readStatement(StatementIAMIndex index) {
        return readStatement(null, index);
    }

    public Statement readStatement(List<Transaction> preload, StatementIAMIndex index) {
        switch (index.getStatementType()) {
            case HASH_STATEMENT:
                return readHashStatement(preload, index.getEpoch());
            case RESULT_STATEMENT:
                return readResultStatement(preload, index.getEpoch());
            default:
                throw new IllegalStateException("unknown statement type: " + index.getStatementType().name());
        }
    }

    public String getID() {
        return reader.getID();
    }

    public IAMReader getReader() {
        return reader;
    }
}
