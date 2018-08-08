package oracle;

import constants.TangleJSONConstants;
import iam.exceptions.CorruptIAMStreamException;
import iam.IAMWriter;
import qlvm.QLVM;
import org.json.JSONObject;
import qubic.QubicReader;
import oracle.statements.HashStatement;
import oracle.statements.ResultStatement;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author microhash
 *
 * The OracleWriter is the oracle machine client that processes a qubic and receives
 * rewards for its service. OracleWriter is the writing counterpart to OracleReader:
 * @see OracleReader
 * */
public class OracleWriter {

    private final IAMWriter resultStream;
    private final IAMWriter hashStream;

    private final Assembly assembly;
    private final QubicReader qubicReader;

    private OracleManager manager;

    // epoch at which the qnode became active. necessary to decide when to use InterQubicResultFetcher for own assembly
    private int firstEpochIndex = -1;

    private int epochIndex;
    private String epochResult;
    private String epochNonce;
    private String name = "ql-node";

    private final LinkedList<OracleListener> oracleListeners = new LinkedList<>();

    /**
     * Creates a new IAMStream identity for this oracle.
     * @param qubicReader qubic to be processed
     * */
    public OracleWriter(QubicReader qubicReader) throws CorruptIAMStreamException {

        this.qubicReader = qubicReader;
        assembly = new Assembly(qubicReader);
        resultStream = new IAMWriter();
        hashStream = new IAMWriter();

        JSONObject obj = new JSONObject();
        obj.put(TangleJSONConstants.ORACLE_RESULT_STREAM, resultStream.getID());
        obj.put(TangleJSONConstants.ORACLE_HASH_STREAM, hashStream.getID());
        resultStream.publish(0, obj);
        hashStream.publish(0, obj);
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param qubicReader       qubic to be processed
     * @param hashStatPubId     IAM stream identity of hash stream
     * @param hashPrivKeyTrytes tryte encoded private key of hash stream
     * @param resStatPubId      IAM stream identity of result stream
     * @param resPrivKeyTrytes  tryte encoded private key of result stream
     * */
    public OracleWriter(QubicReader qubicReader, String hashStatPubId, String hashPrivKeyTrytes, String resStatPubId, String resPrivKeyTrytes) throws InvalidKeySpecException {

        this.qubicReader = qubicReader;
        resultStream = new IAMWriter(resStatPubId, resPrivKeyTrytes);
        hashStream = new IAMWriter(hashStatPubId, hashPrivKeyTrytes);
        assembly = new Assembly(qubicReader);
    }

    /**
     * Lets assembly fetch ResultStatements from last epoch, then creates and publishes the HashStatement
     * for the current epoch. Calculates the result for the subsequent ResultStatement.
     * @param epochIndex index of the current epoch
     * */
    public void doHashStatement(int epochIndex) {

        if(firstEpochIndex < 0) firstEpochIndex = epochIndex;

        if(epochIndex > 0) {
            // update assembly
            assembly.fetchEpoch(false, epochIndex-1);

            // feed oracleListeners
            QuorumBasedResult qbr = assembly.determineQuorumBasedResult(epochIndex-1);
            for(OracleListener qf : oracleListeners)
                qf.onReceiveEpochResult(epochIndex-1, qbr);
        }

        this.epochIndex = epochIndex;
        this.epochNonce = genNonce();
        this.epochResult = calcResult();

        String hash = ResultHasher.hash(epochNonce, epochResult);
        int[] ratings = assembly.getRatings();
        HashStatement hashEpoch = new HashStatement(epochIndex, hash, ratings);

        hashStream.publish(epochIndex+1, hashEpoch.toJSON()); // +1 because epoch #0 has address …999A not …9999
    }


    /**
     * Lets assembly fetch HashStatements from current epoch, then creates and publishes the ResultStatement
     * for the current epoch. Result has already been calculated by doHashStatement().
     * */
    public void doResultStatement() {
        // update assembly
        assembly.fetchEpoch(true, epochIndex);

        //if(epochIndex > 0) assembly.determineQuorumBasedResult(epochIndex-1);
        ResultStatement resultEpoch = new ResultStatement(epochIndex, ""+epochResult, epochNonce);
        resultStream.publish(epochIndex+1, resultEpoch.toJSON()); // +1 because epoch #0 has address …999A not …9999
    }

    /**
     * Sends an application to the qubic's application address. The qubic owner might read
     * received applications on this address and consider adding the oracle to the assembly.
     * */
    public void apply() {

        if(qubicReader.getExecutionStart() < System.currentTimeMillis()/1000)
            throw new IllegalStateException("applying aborted: qubic has already entered execution phase");

        JSONObject application = new JSONObject();
        application.put(TangleJSONConstants.ORACLE_ID, resultStream.getID());
        application.put(TangleJSONConstants.ORACLE_NAME, name);
        TangleAPI.getInstance().sendMessage(qubicReader.getApplicationAddress(), application.toString());
    }

    /**
     * Calculates the result string for the current epoch.
     * @return result string for current epoch
     * */
    private String calcResult() {
        return QLVM.run(qubicReader.getCode(), OracleWriter.this);
    }

    /**
     * Checks the assembly and adds all oracle mam roots listed in the assembly transaction to
     * its own assembly list in case it is part of the assembly.
     * @return TRUE = successfully made it into assembly, FALSE = did not make it into assembly
     * */
    public boolean assemble() {

        ArrayList<String> oracleIDs = qubicReader.getAssemblyList();

        if(oracleIDs == null || !oracleIDs.contains(getID()))
            return false;

        if(assembly.size() == 0)
            assembly.addOracles(oracleIDs);

        return true;
    }

    /**
     * @return a random String used as nonce
     * */
    private String genNonce() {
        return TryteTool.generateRandom(30);
    }

    /**
     * Registers a OracleListener to subscribe it to future events. Counterpart to unsubscribeOracleListener()
     * @param oracleListener the OracleListener to be registered
     * */
    public void subscribeOracleListener(OracleListener oracleListener) {
        oracleListeners.add(oracleListener);
    }

    /**
     * Unregisters a OracleListener from event subscription. Counterpart to subscribeOracleListener()
     * @param oracleListener the OracleListener to be unregistered
     * */
    public void unsubscribeOracleListener(OracleListener oracleListener) {
        oracleListeners.remove(oracleListener);
    }

    public String getHashStreamID() {
        return hashStream.getID();
    }

    public String getResultStreamID() {
        return resultStream.getID();
    }

    public String getHashPrivateKeyTrytes() { return hashStream.getPrivateKeyTrytes(); }

    public String getResultPrivateKeyTrytes() { return resultStream.getPrivateKeyTrytes(); }

    public Assembly getAssembly() {
        return assembly;
    }

    public String getID() {
        return resultStream.getID();
    }

    public QubicReader getQubicReader() {
        return qubicReader;
    }

    public int getEpochIndex() {
        return epochIndex;
    }

    public void setManager(OracleManager manager) {
        this.manager = manager;
    }

    public OracleManager getManager() {
        return manager;
    }

    public void setName(String name) {
        this.name = name;
    }
}
