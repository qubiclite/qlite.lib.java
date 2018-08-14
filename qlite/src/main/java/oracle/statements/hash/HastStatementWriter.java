package oracle.statements.hash;

import iam.IAMWriter;
import oracle.statements.StatementType;
import oracle.statements.StatementWriter;

public class HastStatementWriter extends StatementWriter<HashStatement> {

    public HastStatementWriter(IAMWriter generalWriter) {
        super(generalWriter, StatementType.HASH_STATEMENT);
    }
}
