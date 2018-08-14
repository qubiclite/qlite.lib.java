package oracle.statements;

import iam.IAMReader;
import jota.model.Transaction;

import java.util.List;

public class ResultStatementReader extends StatementReader {

    private HashStatementReader hashStatementReader;

    public ResultStatementReader(IAMReader generalReader, HashStatementReader hashStatementReader) {
        super(generalReader, StatementType.RESULT_STATEMENT);
        this.hashStatementReader = hashStatementReader;
    }

    public ResultStatement read(int epoch) {
        return read(null, epoch);
    }

    @Override
    public ResultStatement read(List<Transaction> preload, int epoch) {
        ResultStatement resultStatement = (ResultStatement)super.read(preload, epoch);
        resultStatement.setHashStatement(hashStatementReader.read(epoch));
        return resultStatement;
    }
}