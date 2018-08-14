package qubic;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
import exceptions.InvalidQubicTransactionException;
import exceptions.UnsupportedVersionException;
import org.json.JSONException;
import org.json.JSONObject;

public class QubicSpecification {

    int executionStartUnix, hashPeriodDuration, resultPeriodDuration, runtimeLimit;
    String code;
    private final String version;

    QubicSpecification() {
        version = GeneralConstants.VERSION;
    }

    public QubicSpecification(JSONObject qubicTransaction) throws InvalidQubicTransactionException, UnsupportedVersionException {

        try {
            version = qubicTransaction.getString(TangleJSONConstants.VERSION);
            if(!version.equals(GeneralConstants.VERSION))
                throw new UnsupportedVersionException(version);

            executionStartUnix = qubicTransaction.getInt(TangleJSONConstants.QUBIC_EXECUTION_START);
            hashPeriodDuration = qubicTransaction.getInt(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION);
            resultPeriodDuration = qubicTransaction.getInt(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION);
            runtimeLimit = qubicTransaction.getInt(TangleJSONConstants.QUBIC_RUN_TIME_LIMIT);
            code = qubicTransaction.getString(TangleJSONConstants.QUBIC_CODE);
        } catch (JSONException e) {
            throw new InvalidQubicTransactionException("qubic transaction could not be parsed", e);
        }
    }

    public QubicSpecification(QubicSpecification origin) {
        code = origin.getCode();
        executionStartUnix = origin.getExecutionStartUnix();
        hashPeriodDuration = origin.getHashPeriodDuration();
        resultPeriodDuration = origin.getResultPeriodDuration();
        runtimeLimit = origin.getRuntimeLimit();
        version = origin.getVersion();
    }

    public int getEpochDuration() {
        return hashPeriodDuration + resultPeriodDuration;
    }

    public int getExecutionStartUnix() {
        return executionStartUnix;
    }

    public int getHashPeriodDuration() {
        return hashPeriodDuration;
    }

    public int getResultPeriodDuration() {
        return resultPeriodDuration;
    }

    public int getRuntimeLimit() {
        return runtimeLimit;
    }

    public String getCode() {
        return code;
    }

    public String getVersion() {
        return version;
    }

    public JSONObject generateQubicTransactionJSON() {
        JSONObject qubicTx = new JSONObject();
        qubicTx.put(TangleJSONConstants.TRANSACTION_TYPE, "qubic transaction");
        qubicTx.put(TangleJSONConstants.VERSION, GeneralConstants.VERSION);
        qubicTx.put(TangleJSONConstants.QUBIC_CODE, getCode());
        qubicTx.put(TangleJSONConstants.QUBIC_EXECUTION_START, getExecutionStartUnix());
        qubicTx.put(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION, getHashPeriodDuration());
        qubicTx.put(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION, getResultPeriodDuration());
        qubicTx.put(TangleJSONConstants.QUBIC_RUN_TIME_LIMIT, getRuntimeLimit());
        return qubicTx;
    }

    public void throwExceptionIfTooLateToPublish() {
        if(getExecutionStartUnix() < System.currentTimeMillis()/1000)
            throw new IllegalArgumentException("parameter 'executionStart' is smaller than current timestamp, indicating the execution would have already started");
    }

    public int timeUntilExecutionStart() {
        return getExecutionStartUnix()-currentUnixTimestamp();
    }

    public int ageOfExecutionPhase() {
        return currentUnixTimestamp()-getExecutionStartUnix();
    }

    int currentUnixTimestamp() {
        return (int)(System.currentTimeMillis()/1000);
    }
}
