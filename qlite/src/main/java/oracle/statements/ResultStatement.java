package oracle.statements;

import constants.TangleJSONConstants;
import oracle.OracleWriter;
import org.json.JSONObject;
import oracle.ResultHasher;

/**
 * @author microhash
 *
 * During each epoch the oracle publishes a HashStatement followed by a ResultStatement.
 * The ResultStatements contains the result calculated by the oracle for the particular epoch.
 * @see Statement
 * @see HashStatement
 * */
public class ResultStatement extends Statement {

    private static final String CONTENT_TYPE = "result";

    private final String result;
    private HashStatement hashEpoch;

    /**
     * Creates a new ResultStatement from a JSONObject
     * @param obj the JSONObject describing the ResultStatement
     * @return new ResultStatement created from the JSONObject
     * */
    public static ResultStatement fromJSON(JSONObject obj) {

        if(obj == null) return null;

        // TODO check if keys exist
        // determine statement attributes
        int epochIndex = obj.getInt(TangleJSONConstants.STATEMENT_EPOCH_INDEX);
        String result = obj.getString(TangleJSONConstants.RESULT_STATEMENT_RESULT);

        return new ResultStatement(epochIndex, result);
    }

    /**
     * @param epochIndex index of epoch in which this statement occured
     * @param result     result the oracle calculated for this particular epoch
     * */
    public ResultStatement(int epochIndex, String result) {
        super(epochIndex);
        this.result = result;
    }

    @Override
    public String getContent() {
        return result;
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE;
    }

    public void setHashEpoch(HashStatement hashEpoch) {
        this.hashEpoch = hashEpoch;
    }

    /**
     * Checks whether the associated HashStatement has been set (this requires it to be
     * published in time) and contains the correct hash.
     * @param oracleID id of oracle that published this statement
     * */
    public boolean isHashStatementValid(String oracleID) {
        return hashEpoch != null && hashEpoch.getContent().equals(ResultHasher.hash(oracleID, result));
    }
}