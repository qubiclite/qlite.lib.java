package oracle.statements.hash;

import iam.IAMReader;
import jota.model.Transaction;
import oracle.statements.StatementReader;
import oracle.statements.StatementType;

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