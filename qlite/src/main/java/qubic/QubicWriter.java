package qubic;

import constants.GeneralConstants;
import constants.TangleJSONConstants;
import iam.IAMWriter;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.util.ArrayList;

/**
 * @author microhash
 *
 * The QubicWriter allows to easily create and manage a new qubic
 * from the qubic author's perspective. It's the writing counterpart to:
 * @see QubicReader
 * */

public class QubicWriter {

    private final IAMWriter publisher;
    private final String applicationAddress;
    private final int executionStart;
    private final int hashPeriodDuration, resultPeriodDuration, runTimeLimit;
    private final ArrayList<String> assembly = new ArrayList<>();
    private JSONObject[] applications;
    private String qubicTxHash, assemblyTxHash;
    private String code;

    private QubicWriterState state;

    /**
     * Creates the qubic's IAMStream identity.
     * @param executionStart       unix timestamp of assembly phase end / execution phase start
     * @param hashPeriodDuration   duration of each hash period in seconds
     * @param resultPeriodDuration duration of each result period in seconds
     * @param runTimeLimit         maximum time in ms before the execution of an epoch is aborted
     * */
    public QubicWriter(int executionStart, int hashPeriodDuration, int resultPeriodDuration, int runTimeLimit) {

        if(executionStart < System.currentTimeMillis()/1000)
            throw new IllegalArgumentException("parameter 'executionStart' is smaller than current timestamp, indicating the execution would have already started");

        publisher = new IAMWriter();

        this.executionStart = executionStart;
        this.hashPeriodDuration = hashPeriodDuration;
        this.resultPeriodDuration = resultPeriodDuration;
        this.runTimeLimit = runTimeLimit;
        this.applicationAddress = TryteTool.generateRandom(81);

        state = QubicWriterState.PRE_ASSEMBLY_PHASE;
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param id            IAMStream identity of the qubic
     * @param privKeyTrytes tryte encoded private key
     * */
    public QubicWriter(String id, String privKeyTrytes) {

        publisher = new IAMWriter(id, privKeyTrytes);

        QubicReader qr = new QubicReader(id);
        executionStart = qr.getExecutionStart();
        hashPeriodDuration = qr.getHashPeriodDuration();
        resultPeriodDuration = qr.getResultPeriodDuration();
        runTimeLimit = qr.getRunTimeLimit();
        applicationAddress = qr.getApplicationAddress();

        state = determineState(qr);
    }

    /**
     * Adds an oracle to the future assembly as preparation for the assembly transaction.
     * @param oracleID IAMStream identity of the oracle
     * */
    public void addToAssembly(String oracleID) {
        assembly.add(oracleID);
    }

    /**
     * Publishes the qubic transaction to the IAMStream.
     * */
    public void publishQubicTx() {

        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("qubic transaction can only be published if qubic is in state PRE_ASSEMBLY_PHASE, but qubic is in state " + state.name());

        // generate json specification of qubic request
        JSONObject qubicTx = new JSONObject();
        qubicTx.put(TangleJSONConstants.TRANSACTION_TYPE, "qubic transaction");
        qubicTx.put(TangleJSONConstants.VERSION, GeneralConstants.VERSION);
        qubicTx.put(TangleJSONConstants.QUBIC_CODE, code);
        qubicTx.put(TangleJSONConstants.QUBIC_APPLICATION_ADDRESS, applicationAddress);

        qubicTx.put(TangleJSONConstants.QUBIC_EXECUTION_START, executionStart);
        qubicTx.put(TangleJSONConstants.QUBIC_HASH_PERIOD_DURATION, hashPeriodDuration);
        qubicTx.put(TangleJSONConstants.QUBIC_RESULT_PERIOD_DURATION, resultPeriodDuration);
        qubicTx.put(TangleJSONConstants.QUBIC_RUN_TIME_LIMIT, runTimeLimit);

        // publish json to IAMStream
        qubicTxHash = publisher.publish(0, qubicTx);

        state = QubicWriterState.ASSEMBLY_PHASE;
    }

    /**
     * Publicly promotes the qubic transaction on the tangle to attract oracles for its assembly.
     * */
    public void promote() {
        String address = TryteTool.buildCurrentQubicPromotionAddress();
        TangleAPI.getInstance().sendTransaction(address, publisher.getID(), false);
    }

    /**
     * Publishes the assembly transaction to the IAMStream. The assembly will consist
     * of all oracles added via addOracle().
     * */
    public void publishAssemblyTx() {

        if(state != QubicWriterState.ASSEMBLY_PHASE)
            throw new IllegalStateException("assembly transaction can only be published if qubic is in state ASSEMBLY_PHASE, but qubic is in state " + state.name());

        if(System.currentTimeMillis()/1000 >= executionStart)
            throw new IllegalStateException("assembly tx aborted: the execution phase would have already started, it is too late to publish the assembly tx now");

        // generate json specification of qubic contract
        JSONObject assemblyTx = new JSONObject();
        assemblyTx.put(TangleJSONConstants.TRANSACTION_TYPE, "assembly transaction");
        assemblyTx.put(TangleJSONConstants.QUBIC_ASSEMBLY, assembly);

        // publish json to IAMStream
        assemblyTxHash = publisher.publish(1, assemblyTx);

        state = QubicWriterState.EXECUTION_PHASE;
    }

    /**
     * @return IAMStream identity of qubic
     * */
    public String getID() {
        return publisher.getID();
    }

    /**
     * Fetches all applications for this qubic and stores them in
     * private field, useable by handleApplications()
     * */
    public void fetchApplications() {
        String[] applicationTransactions = TangleAPI.getInstance().readTransactionsByAddress(null, applicationAddress, true).values().toArray(new String[0]);
        applications = new JSONObject[applicationTransactions.length];
        for(int i = 0; i < applicationTransactions.length; i++) {
            applications[i] = new JSONObject(applicationTransactions[i]); // TODO check if valid JSONObject
        }
    }

    /**
     * Determines the QubicWriterState by checking whether assembly
     * transaction has already been published. Cannot determine
     * PRE_ASSEMBLY_PHASE.
     * @param qr QubicReader of Qubic in question
     * @return determined QubicWriterState
     * */
    private static QubicWriterState determineState(QubicReader qr) {
        if(qr.getAssemblyList() == null) {
            return (qr.getExecutionStart() < System.currentTimeMillis()/1000) ? QubicWriterState.ABORTED : QubicWriterState.ASSEMBLY_PHASE;
        }
        return QubicWriterState.EXECUTION_PHASE;
    }

    public JSONObject[] getApplications() {
        return applications;
    }

    public int getTimeUntilExecutionStart() { return (int)(getExecutionStart()-System.currentTimeMillis()/1000); }

    public void setCode(String code) {
        this.code = code;
    }

    public long getExecutionStart() {
        return executionStart;
    }

    public int getHashPeriodDuration() {
        return hashPeriodDuration;
    }

    public int getResultPeriodDuration() {
        return resultPeriodDuration;
    }

    public String getQubicTxHash() {
        return qubicTxHash;
    }

    public String getAssemblyTxHash() {
        return assemblyTxHash;
    }

    public String getPrivateKeyTrytes() { return publisher.getPrivateKeyTrytes(); }

    public ArrayList<String> getAssembly() {
        return assembly;
    }

    public String getState() {
        if(state != QubicWriterState.EXECUTION_PHASE && getTimeUntilExecutionStart() < 0)
            state = QubicWriterState.ABORTED;
        return state.name().toLowerCase().replace('_', ' ');
    }

    enum QubicWriterState {
        PRE_ASSEMBLY_PHASE, ASSEMBLY_PHASE, EXECUTION_PHASE, ABORTED;
    }
}