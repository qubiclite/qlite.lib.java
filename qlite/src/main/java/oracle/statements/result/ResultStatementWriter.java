package oracle.statements.result;

import iam.IAMWriter;
import oracle.statements.StatementType;
import oracle.statements.StatementWriter;

public class ResultStatementWriter extends StatementWriter<ResultStatement> {

    public ResultStatementWriter(IAMWriter generalWriter) {
        super(generalWriter, StatementType.RESULT_STATEMENT);
    }
}
