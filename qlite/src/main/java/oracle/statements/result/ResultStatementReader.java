package oracle.statements.result;

import iam.IAMReader;
import jota.model.Transaction;
import oracle.statements.StatementReader;
import oracle.statements.StatementType;
import oracle.statements.hash.HashStatementReader;

import java.util.List;

public class ResultStatementReader extends StatementReader {

    private HashStatementReader hashStatementReader;

    public ResultStatementReader(IAMReader generalReader, HashStatementReader hashStatementReader) {
        super(generalReader, StatementType.RESULT_STATEMENT);
        this.hashStatementReader = hashStatementReader;

        if(hashStatementReader == null)
            throw new NullPointerException("parameter 'hashStatementReader' is null");
    }

    public ResultStatement read(int epoch) {
        return read(null, epoch);
    }

    @Override
    public ResultStatement read(List<Transaction> preload, int epoch) {
        ResultStatement resultStatement = (ResultStatement)super.read(preload, epoch);
        if(resultStatement != null)
            resultStatement.setHashStatement(hashStatementReader.read(epoch));
        return resultStatement;
    }
}