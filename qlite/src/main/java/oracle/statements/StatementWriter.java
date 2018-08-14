package oracle.statements;

import iam.IAMKeywordWriter;
import iam.IAMWriter;

public class StatementWriter<T extends Statement> {

    private final IAMKeywordWriter writer;

    public StatementWriter(IAMWriter generalWriter, StatementType statementType) {
        writer = new IAMKeywordWriter(generalWriter, statementType.getIAMKeyword());
    }

    public void write(T statement) {
        writer.publish(statement.getEpochIndex(), statement.toJSON());
    }
}