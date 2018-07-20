package oracle.statements;

import constants.TangleJSONConstants;
import exceptions.InvalidStatementException;
import oracle.OracleWriter;
import org.json.JSONException;
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

    private final String nonce;
    private final String result;
    private HashStatement hashEpoch;

    /**
     * Creates a new ResultStatement from a JSONObject
     * @param obj the JSONObject describing the ResultStatement
     * @return new ResultStatement created from the JSONObject
     * */
    public static ResultStatement fromJSON(JSONObject obj) throws InvalidStatementException {

        if(obj == null) return null;

        int epochIndex;
        String result, nonce;

        try {
            epochIndex = obj.getInt(TangleJSONConstants.STATEMENT_EPOCH_INDEX);
            result = obj.getString(TangleJSONConstants.RESULT_STATEMENT_RESULT);
            nonce = obj.getString(TangleJSONConstants.RESULT_STATEMENT_NONCE);
        } catch (JSONException e) {
            throw new InvalidStatementException(e.getClass().getName() + ": " + e.getMessage(), e);
        }

        return new ResultStatement(epochIndex, result, nonce);
    }

    /**
     * @param epochIndex index of epoch in which this statement occured
     * @param result     result the oracle calculated for this particular epoch
     * @param nonce      random value used as salt during the hash creation of the HashStatement.
     * */
    public ResultStatement(int epochIndex, String result, String nonce) {
        super(epochIndex);
        this.result = result;
        this.nonce = nonce;
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
     * */
    public boolean isHashStatementValid() {
        return hashEpoch != null && hashEpoch.getContent().equals(ResultHasher.hash(nonce, result));
    }

    @Override
    public JSONObject toJSON() {
        JSONObject o = super.toJSON();
        o.put(TangleJSONConstants.RESULT_STATEMENT_NONCE, nonce);
        return o;
    }
}