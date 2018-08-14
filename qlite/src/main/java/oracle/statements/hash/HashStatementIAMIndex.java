package oracle.statements.hash;

import oracle.statements.StatementIAMIndex;
import oracle.statements.StatementType;

public class HashStatementIAMIndex extends StatementIAMIndex {

    public HashStatementIAMIndex(long position) {
        super(StatementType.HASH_STATEMENT, position);
    }
}