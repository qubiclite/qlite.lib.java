package oracle.statements.result;

import oracle.statements.StatementIAMIndex;
import oracle.statements.StatementType;

public class ResultStatementIAMIndex extends StatementIAMIndex {

    public ResultStatementIAMIndex(long position) {
        super(StatementType.RESULT_STATEMENT, position);
    }
}