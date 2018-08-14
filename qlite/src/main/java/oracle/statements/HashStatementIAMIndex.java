package oracle.statements;

public class HashStatementIAMIndex extends StatementIAMIndex {

    public HashStatementIAMIndex(long position) {
        super(StatementType.HASH_STATEMENT, position);
    }
}