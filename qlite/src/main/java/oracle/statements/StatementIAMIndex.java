package oracle.statements;

import iam.IAMIndex;

public class StatementIAMIndex extends IAMIndex {

    private final StatementType statementType;

    public StatementIAMIndex(StatementType statementType, long position) {
        super(statementType.getIAMKeyword(), position);
        this.statementType = statementType;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public int getEpoch() {
        return (int)getPosition();
    }
}