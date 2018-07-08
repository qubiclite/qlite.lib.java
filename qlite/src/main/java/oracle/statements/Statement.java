package oracle.statements;

import constants.TangleJSONConstants;
import org.json.JSONObject;

/**
 * @author microhash
 *
 * A statement is the way a single qnodes expresses itself in the assembly
 * and communicates with other oracles. Statements are the building blocks
 * for epoches which is the sequence defining a qubic.
 * @see HashStatement
 * @see ResultStatement
 * */

public abstract class Statement {

    private final int epochIndex;

    /**
     * @param epochIndex index of epoch in which this statement occured
     * */
    protected Statement(int epochIndex) {
        this.epochIndex = epochIndex;
    }

    /**
     * @return json object representing this statement.
     * */
    public JSONObject toJSON() {
        // generate json object and return its string
        JSONObject obj = new JSONObject();
        obj.put(TangleJSONConstants.STATEMENT_EPOCH_INDEX, epochIndex);
        obj.put(TangleJSONConstants.TRANSACTION_TYPE, getContentType() + " statement");
        obj.put(getContentType(), getContent());
        return obj;
    }

    /**
     * @return actual content of statement
     * */
    public abstract String getContent();

    /**
     * @return content type name, categorizes what kind of content the statement contains
     * */
    protected abstract String getContentType();

    public int getEpochIndex() {
        return epochIndex;
    }
}