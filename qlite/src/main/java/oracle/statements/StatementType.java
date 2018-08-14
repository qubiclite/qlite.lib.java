package oracle.statements;

public enum StatementType {

    HASH_STATEMENT(Constants.HASH_STATEMENT_IAM_KEYWORD), RESULT_STATEMENT(Constants.RESULT_STATEMENT_IAM_KEYWORD);

    private final String iamKeyword;

    StatementType(String iamKeyword) {
        this.iamKeyword = iamKeyword;
    }

    public String getIAMKeyword() {
        return iamKeyword;
    }

    public static class Constants {
        public static final String RESULT_STATEMENT_IAM_KEYWORD = "RESULTS", HASH_STATEMENT_IAM_KEYWORD = "HASHES";
    }
}
