package oracle.statements;

public class ResultStatementIAMIndex extends StatementIAMIndex {

    public ResultStatementIAMIndex(long position) {
        super(StatementType.RESULT_STATEMENT, position);
    }
}