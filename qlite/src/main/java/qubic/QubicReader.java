package qubic;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
import exceptions.NoQubicTransactionException;
import exceptions.UnsupportedVersionException;
import iam.exceptions.CorruptIAMStreamException;
import exceptions.InvalidQubicTransactionException;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import iam.IAMReader;
import org.json.JSONArray;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    private IAMReader reader;
    private List<String> assemblyList;
    private final String id;
    private final QubicSpecification specification;

    /**
     * Creates the IAMReader for the qubic stream and fetches the qubic transaction.
     *
     * @param id IAMStream identity of qubic
     */
    public QubicReader(String id) throws InvalidQubicTransactionException, CorruptIAMStreamException {
        this.id = id;
        reader = new IAMReader(id);
        JSONObject qubicTransaction = fetchQubicTransaction();
        specification = new QubicSpecification(qubicTransaction);
    }

    private JSONObject fetchQubicTransaction() {
        JSONObject qubicTransaction = reader.read(QubicWriter.QUBIC_TRANSACTION_IAM_INDEX);
        QubicTransactionValidator.throwExceptionIfInvalid(qubicTransaction);
        return qubicTransaction;
    }

    /**
     * Lists the IAMStream identities of all oracles in the assembly transaction.
     * Fetches the assembly transaction if necessary.
     *
     * @return ArrayList of oracle IAMStream identities, NULL if no assembly tx published
     */
    public List<String> getAssemblyList() {
        if (assemblyList == null)
            assemblyList = fetchAssemblyList();
        return assemblyList;
    }

    private List<String> fetchAssemblyList() {
        JSONArray assemblyJSONArray = fetchAssemblyJSONArray();
        if (assemblyJSONArray == null)
            return null;
        return convertAssemblyJSONArrayToAssemblyList(assemblyJSONArray);
    }

    private List<String> convertAssemblyJSONArrayToAssemblyList(JSONArray assemblyJSONArray) {
        List<String> assemblyList = new LinkedList<>();
        for (int i = 0; i < assemblyJSONArray.length(); i++)
            assemblyList.add(assemblyJSONArray.getString(i));
        return assemblyList;
    }

    private JSONArray fetchAssemblyJSONArray() {
        JSONObject assemblyTransaction = reader.read(QubicWriter.ASSEMBLY_TRANSACTION_IAM_INDEX);
        if(assemblyTransaction == null) return null;
        try {
            return assemblyTransaction.getJSONArray("assembly");
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Searches the tangle for recently promoted qubics.
     * @return ArrayList of all found qubics
     */
    public static List<QubicReader> findPromotedQubics() {
        ArrayList<QubicReader> qubics = new ArrayList<>();
        String[] recentPromotions = TangleAPI.getInstance().readTransactionsByAddress(null, TryteTool.buildCurrentQubicPromotionAddress(), false).values().toArray(new String[0]);
        for (String recentPromotion : recentPromotions) {
            qubics.add(new QubicReader(StringUtils.rightPad(recentPromotion, 81, '9')));
        }
        return qubics;
    }

    public int lastCompletedEpoch() {
        long timeRunning = System.currentTimeMillis() / 1000 - specification.getExecutionStartUnix();
        int epochDuration = specification.getEpochDuration();
        return Math.max((int) Math.floor(timeRunning / epochDuration) - 1, -1);
    }

    public String getID() {
        return id;
    }

    public QubicSpecification getSpecification() {
        return specification;
    }

}

class QubicTransactionValidator {

    private static final String[] ATTRIBUTES_TO_CHECK = {
            TangleJSONConstants.QUBIC_CODE,
            TangleJSONConstants.QUBIC_EXECUTION_START,
            TangleJSONConstants.QUBIC_RUN_TIME_LIMIT,
            TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION,
            TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION,
    };

    static void throwExceptionIfInvalid(JSONObject qubicTransaction) {
        ensureNotNull(qubicTransaction);
        ensureHasVersion(qubicTransaction);
        ensureVersionIsSupported(qubicTransaction);
        validateStructure(qubicTransaction);
    }

    private static void ensureNotNull(JSONObject qubicTransaction) {
        if(qubicTransaction == null)
            throw new NoQubicTransactionException();
    }

    private static void validateStructure(JSONObject qubicTransaction) {

        for(String attribute : ATTRIBUTES_TO_CHECK) {
            if(!qubicTransaction.has(attribute))
                throw new InvalidQubicTransactionException(buildAttributeNotFoundErrorMessage(attribute), null);
        }
    }

    private static void ensureVersionIsSupported(JSONObject qubicTransaction) {
        String version = String.valueOf(qubicTransaction.get(TangleJSONConstants.VERSION));
        if(!GeneralConstants.VERSION.equals(version))
            throw new UnsupportedVersionException(version);
    }

    private static void ensureHasVersion(JSONObject qubicTransaction) {
        if(!qubicTransaction.has(TangleJSONConstants.VERSION))
            throw new InvalidQubicTransactionException(buildAttributeNotFoundErrorMessage(TangleJSONConstants.VERSION), null);
    }

    private static String buildAttributeNotFoundErrorMessage(String attributeName) {
        return "qubic transaction does not contain attribute '"+attributeName+"'";
    }
}