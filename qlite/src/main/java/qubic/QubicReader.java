package qubic;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
import exceptions.CorruptIAMStreamException;
import exceptions.InvalidQubicTransactionException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import iam.IAMReader;
import org.json.JSONArray;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.util.ArrayList;

/**
 * @author microhash
 *
 * The QubicReader reads the public parameters defining the qubic.
 * It's the reading counterpart to:
 * @see QubicWriter
 *
 * TODO state enum
 * */

public class QubicReader {

    private final IAMReader reader;
    private ArrayList<String> assemblyList;
    private String id;

    private final String code;
    private final String version;
    private final String applicationAddress;
    private final int executionStart;
    private final int hashPeriodDuration;
    private final int resultPeriodDuration;
    private final int runtimeLimit;

    /**
     * Creates the IAMReader for the qubic stream and fetches the qubic transaction.
     * @param id IAMStream identity of qubic
     * */
    public QubicReader(String id) throws InvalidQubicTransactionException {
        this.id = id;

        try {
            reader = new IAMReader(id);
        } catch (CorruptIAMStreamException e) {
            throw new InvalidQubicTransactionException(e.getMessage(), e);
        }

        JSONObject qubicTx = reader.read(0);

        if(qubicTx == null)
            throw new InvalidQubicTransactionException("qubic transaction not found", null);

        QubicTransactionVerificator.verify(qubicTx);

        // init attributes
        try {
            version              = qubicTx.getString(TangleJSONConstants.VERSION);

            if(!version.equals(GeneralConstants.VERSION))
                throw new InvalidQubicTransactionException("version declared in qubic does not match qlri version", null);

            code                 = qubicTx.getString(TangleJSONConstants.QUBIC_CODE);
            applicationAddress   = qubicTx.getString(TangleJSONConstants.QUBIC_APPLICATION_ADDRESS);
            executionStart       = qubicTx.getInt(TangleJSONConstants.QUBIC_EXECUTION_START);
            hashPeriodDuration   = qubicTx.getInt(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION);
            resultPeriodDuration = qubicTx.getInt(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION);
            runtimeLimit         = qubicTx.getInt(TangleJSONConstants.QUBIC_RUN_TIME_LIMIT);
        } catch (JSONException e) {
            throw new InvalidQubicTransactionException("failed to parse qubic meta data: " + e.getMessage(), e);
        }
    }

    /**
     * Lists the IAMStream identities of all oracles in the assembly transaction.
     * Fetches the assembly transaction if necessary.
     * @return ArrayList of oracle IAMStream identities, NULL if no assembly tx published
     * */
    public ArrayList<String> getAssemblyList() {

        // fetch assembly if not already done so
        fetchAssemblyTx();
        return assemblyList;
    }

    /**
     * Fetches the assembly if that hasn't been done before.
     * */
    private void fetchAssemblyTx() {
        if(assemblyList == null) {
            JSONObject assemblyTx = reader.read(1);
            if(assemblyTx == null)
                return;

            assemblyList = new ArrayList<>();

            JSONArray assemblyArray;

            try {
                assemblyArray = assemblyTx.getJSONArray("assembly");
            } catch (JSONException e) {
                // if assembly transaction malformed, set assembly to empty list
                return;
            }

            for(int i = 0; i < assemblyArray.length(); i++)
                assemblyList.add(assemblyArray.getString(i));
        }
    }

    public String getApplicationAddress() {
        return applicationAddress;
    }

    /**
     * Searches the tangle for recently promoted qubics.
     * @return ArrayList of all found qubics
     * */
    public static ArrayList<QubicReader> findQubics() {
        ArrayList<QubicReader> qubics = new ArrayList<>();
        String[] recentPromotions = TangleAPI.getInstance().readTransactionsByAddress(null, TryteTool.buildCurrentQubicPromotionAddress(), false).values().toArray(new String[0]);
        for(String recentPromotion : recentPromotions) {
            qubics.add(new QubicReader(StringUtils.rightPad(recentPromotion, 81, '9')));
        }
        return qubics;
    }

    public int lastCompletedEpoch() {

        long timeRunning = System.currentTimeMillis()/1000 - getExecutionStart();
        int epochDuration = getEpochDuration();
        return Math.max((int)Math.floor(timeRunning / epochDuration)-1, -1);
    }

    public String getID() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getVersion() { return version; }

    public int getExecutionStart() {
        return executionStart;
    }

    public int getRunTimeLimit() {
        return runtimeLimit;
    }

    public int getHashPeriodDuration() {
        return hashPeriodDuration;
    }

    public int getResultPeriodDuration() {
        return resultPeriodDuration;
    }

    public int getEpochDuration() {
        return hashPeriodDuration + resultPeriodDuration;
    }
}

class QubicTransactionVerificator {

    protected static boolean verify(JSONObject qubicTx) {

        String[] attributes = {
                TangleJSONConstants.VERSION,
                TangleJSONConstants.QUBIC_CODE,
                TangleJSONConstants.QUBIC_EXECUTION_START,
                TangleJSONConstants.QUBIC_RUN_TIME_LIMIT,
                TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION,
                TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION,
                TangleJSONConstants.QUBIC_APPLICATION_ADDRESS,
        };

        for(String attribute : attributes)
            if(!qubicTx.has(attribute))
                throw new InvalidQubicTransactionException(buildAttributeNotFoundErrorMessage(TangleJSONConstants.QUBIC_CODE), null);

        return false;
    }

    private static String buildAttributeNotFoundErrorMessage(String attributeName) {
        return "qubic transaction does not contain attribute '"+attributeName+"'";
    }
}