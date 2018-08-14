package oracle.statements.hash;

import constants.TangleJSONConstants;
import exceptions.InvalidStatementException;
import oracle.statements.result.ResultStatement;
import oracle.statements.Statement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author microhash
 *
 * During each epoch the oracle publishes a HashStatement followed by a ResultStatement.
 * The HashStatement hashes the result in the subsequent ResultStatement in order to combat lazy
 * oracles from copying the result from other assembly members.
 * @see Statement
 * @see ResultStatement
 * */

public class HashStatement extends Statement {

    private static final String CONTENT_TYPE = "hash";

    private final String hash;
    private final int[] ratings;

    /**
     * Creates a new HashStatement from a JSONObject
     * @param obj the JSONObject describing the HashStatement
     * @return new HashStatement created from the JSONObject
     * */
    public static HashStatement fromJSON(JSONObject obj) throws InvalidStatementException {

        if(obj == null) return null;

        int epochIndex;
        String hash;
        JSONArray ratingsArr;

        try {
            epochIndex = obj.getInt(TangleJSONConstants.STATEMENT_EPOCH_INDEX);
            hash = obj.getString(TangleJSONConstants.HASH_STATEMENT_HASH);
            ratingsArr = obj.getJSONArray(TangleJSONConstants.HASH_STATEMENT_RATINGS);
        } catch (JSONException e) {
            throw new InvalidStatementException(e.getClass().getName() + ": " + e.getMessage(), e);
        }

        // convert JSONArray -> int array
        int[] ratings = new int[ratingsArr.length()];
        for(int i = 0; i < ratingsArr.length(); i++)
            ratings[i] = ratingsArr.getInt(i);

        return new HashStatement(epochIndex, hash, ratings);
    }

    /**
     * @param epochIndex index of epoch in which this statement occured
     * @param hash       hash for subsequent ResultEpoch
     * @param ratings    rating of each assembly participant's behaviour during
     *                   the last epoch (-1 = negative, 0 = neutral, 1 = positive)
     * */
    public HashStatement(int epochIndex, String hash, int[] ratings) {
        super(epochIndex);
        this.hash = hash;
        this.ratings = ratings;
    }

    @Override
    public String getContent() {
        return hash;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = super.toJSON();
        obj.put(TangleJSONConstants.HASH_STATEMENT_RATINGS, ratings);
        return obj;
    }
}