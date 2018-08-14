package oracle;

import iam.exceptions.CorruptIAMStreamException;
import jota.model.Transaction;
import iam.IAMReader;
import oracle.statements.*;
import oracle.statements.hash.HashStatementReader;
import oracle.statements.result.ResultStatementReader;

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

    /**
     * Initializes TangleReaders for HashStatements and ResultStatements.
     * @param id IAM stream ID (= hash of IAM stream's root transaction)
     * */
    public OracleReader(String id) throws CorruptIAMStreamException {
        reader = new IAMReader(id);
        hashStatementReader = new HashStatementReader(reader);
        resultStatementReader = new ResultStatementReader(reader, hashStatementReader);
    }

    public Statement read(StatementIAMIndex index) {
        return read(null, index);
    }

    public Statement read(List<Transaction> preload, StatementIAMIndex index) {
        switch (index.getStatementType()) {
            case HASH_STATEMENT:
                return hashStatementReader.read(preload, index.getEpoch());
            case RESULT_STATEMENT:
                return resultStatementReader.read(preload, index.getEpoch());
            default:
                throw new IllegalStateException("unknown statement type: " + index.getStatementType().name());
        }
    }

    public HashStatementReader getHashStatementReader() {
        return hashStatementReader;
    }

    public ResultStatementReader getResultStatementReader() {
        return resultStatementReader;
    }

    public String getID() {
        return reader.getID();
    }

    public IAMReader getReader() {
        return reader;
    }
}
