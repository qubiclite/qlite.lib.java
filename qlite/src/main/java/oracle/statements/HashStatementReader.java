package oracle.statements;

import iam.IAMReader;
import jota.model.Transaction;

import java.util.List;

public class HashStatementReader extends StatementReader {

    public HashStatementReader(IAMReader generalReader) {
        super(generalReader, StatementType.HASH_STATEMENT);
    }

    public HashStatement read(int epoch) {
        return read(null, epoch);
    }

    @Override
    public HashStatement read(List<Transaction> preload, int epoch) {
        return (HashStatement) super.read(preload, epoch);
    }
}