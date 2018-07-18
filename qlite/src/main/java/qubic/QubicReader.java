package qubic;

import constants.TangleJSONConstants;
import exceptions.InvalidQubicTransactionException;
import org.apache.commons.lang3.StringUtils;
import tangle.IAMReader;
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
    private JSONObject assemblyTx;
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
    public QubicReader(String id) {
        this.id = id;
        reader = new IAMReader(id);
        JSONObject qubicTx = reader.read(0);
        QubicTransactionVerificator.verify(qubicTx);

        // init attributes
        version              = qubicTx.getString(TangleJSONConstants.VERSION);
        code                 = qubicTx.getString(TangleJSONConstants.QUBIC_CODE);
        applicationAddress   = qubicTx.getString(TangleJSONConstants.QUBIC_APPLICATION_ADDRESS);
        executionStart       = qubicTx.getInt(TangleJSONConstants.QUBIC_EXECUTION_START);
        hashPeriodDuration   = qubicTx.getInt(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION);
        resultPeriodDuration = qubicTx.getInt(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION);
        runtimeLimit         = qubicTx.getInt(TangleJSONConstants.QUBIC_CODE);
    }

    /**
     * Lists the IAMStream identities of all oracles in the assembly transaction.
     * Fetches the assembly transaction if necessary.
     * @return ArrayList of oracle IAMStream identities, NULL if no assembly tx published
     * */
    public ArrayList<String> getAssemblyList() {

        // fetch contract if not already done so
        fetchAssemblyTx();
        if(assemblyTx == null) return null;

        // copy assembly from contract's JSONArray to new ArrayList and return
        final ArrayList<String> assembly = new ArrayList<String>();
        JSONArray assemblyArray = assemblyTx.getJSONArray("assembly");
        for(int i = 0; i < assemblyArray.length(); i++)
            assembly.add(assemblyArray.getString(i));
        return assembly;
    }

    /**
     * Fetches the assembly transaction from the IAMStream if the reader is
     * in the correct state.
     * */
    private void fetchAssemblyTx() {
        if(assemblyTx == null)
            assemblyTx = reader.read(1);
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
        String[] recentPromotions = TangleAPI.getInstance().findTransactionsByAddress(TryteTool.buildCurrentQubicPromotionAddress(), false);
        for(String recentPromotion : recentPromotions) {
            qubics.add(new QubicReader(StringUtils.rightPad(recentPromotion, 81, '9')));
        }
        return qubics;
    }

    public int lastCompletedEpoch() {

        long timeRunning = System.currentTimeMillis()/1000 - getExecutionStart();
        int epochDuration = getEpochDuration();
        return (int)Math.floor(timeRunning / epochDuration)-1;
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
                throw new InvalidQubicTransactionException(buildAttributeNotFoundErrorMessage(TangleJSONConstants.QUBIC_CODE));

        return false;
    }

    private static String buildAttributeNotFoundErrorMessage(String attributeName) {
        return "qubic transaction does not contain attribute '"+attributeName+"'";
    }
}