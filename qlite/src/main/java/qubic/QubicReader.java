package qubic;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
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
    private final JSONObject qubicTx;
    private JSONObject assemblyTx;
    private String id;

    /**
     * Creates the IAMReader for the qubic stream and fetches the qubic transaction.
     * @param id IAMStream identity of qubic
     * */
    public QubicReader(String id) {
        this.id = id;
        reader = new IAMReader(id);
        qubicTx = reader.read(0);
    }

    /**
     * Lists the IAMStream identities of all oracles in the assembly transaction.
     * Fetches the assembly transaction if necessary.
     * @return ArrayList of oracle IAMStream identities, NULL if no assembly tx published
     * */
    public ArrayList<String> getAssemblyList() {

        // fetch contract if not already done so
        if(assemblyTx == null) fetchAssemblyTx();
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
        return qubicTx.getString("application_address");
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
        return qubicTx.getString(TangleJSONConstants.QUBIC_CODE);
    }

    public String getVersion() { return qubicTx.getString(TangleJSONConstants.VERSION); }

    public int getExecutionStart() {
        return qubicTx.getInt(TangleJSONConstants.QUBIC_EXECUTION_START);
    }

    public int getRunTimeLimit() {
        return qubicTx.getInt(TangleJSONConstants.QUBIC_RUN_TIME_LIMIT);
    }

    public int getEpochDuration() {
        return getHashPeriodDuration() + getResultPeriodDuration();
    }

    public int getHashPeriodDuration() {
        return qubicTx.getInt(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION);
    }

    public int getResultPeriodDuration() {
        return qubicTx.getInt(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION);
    }
}
