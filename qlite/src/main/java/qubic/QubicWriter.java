package qubic;

import constants.TangleJSONConstants;
import iam.IAMWriter;
import org.json.JSONException;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author microhash
 *
 * The QubicWriter allows to easily create and manage a new qubic
 * from the qubic author's perspective. It's the writing counterpart to:
 * @see QubicReader
 * */

public class QubicWriter {

    private IAMWriter publisher;
    private final ArrayList<String> assembly = new ArrayList<>();
    private String qubicTxHash, assemblyTxHash;
    private final EditableQubicSpecification specification;

    private QubicWriterState state = QubicWriterState.PRE_ASSEMBLY_PHASE;

    public QubicWriter() {
        publisher = new IAMWriter();
        specification = new EditableQubicSpecification();
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param id               IAMStream identity of the qubic
     * @param privateKeyTrytes tryte encoded private key
     * */
    public QubicWriter(String id, String privateKeyTrytes) throws InvalidKeySpecException {
        publisher = new IAMWriter(id, privateKeyTrytes);
        QubicReader qr = new QubicReader(id);
        specification = new EditableQubicSpecification(qr.getSpecification());
        state = determineQubicWriterStateFromQubicReader(qr);
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
    public synchronized void publishQubicTransaction() {

        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("qubic transaction can only be published if qubic is in state PRE_ASSEMBLY_PHASE, but qubic is in state " + state.name());

        specification.throwExceptionIfInvalid();
        JSONObject qubicTransactionJSON = specification.generateQubicTransactionJSON();
        qubicTxHash = publisher.publish(0, qubicTransactionJSON);
        state = QubicWriterState.ASSEMBLY_PHASE;
    }

    /**
     * Publicly promotes the qubic transaction on the tangle to attract oracles for its assembly.
     * */
    public void promote() {
        String address = TryteTool.buildCurrentQubicPromotionAddress();
        TangleAPI.getInstance().sendTrytes(address, publisher.getID());
    }

    /**
     * Publishes the assembly transaction to the IAMStream. The assembly will consist
     * of all oracles added via addOracle().
     * */
    public synchronized void publishAssemblyTransaction() {
        throwExceptionIfCannotPublishAssemblyTransaction();
        JSONObject assemblyTransaction = generateAssemblyTransaction(assembly);
        assemblyTxHash = publisher.publish(1, assemblyTransaction);
        state = QubicWriterState.EXECUTION_PHASE;
    }

    public String getID() {
        return publisher.getID();
    }

    /**
     * Fetches all applications for this qubic
     * @return the fetched applications
     * */
    public List<JSONObject> fetchApplications() {
        Collection<String> transactionMessagesOnApplicationAddress = TangleAPI.getInstance().readTransactionsByAddress(null, getID(), true).values();
        return filterValidApplicationsFromTransactionMessages(transactionMessagesOnApplicationAddress);
    }

    private List<JSONObject> filterValidApplicationsFromTransactionMessages(Iterable<String> uncheckedTransactionMessages) {
        List<JSONObject> applications = new LinkedList<>();
        for(String transactionMessage : uncheckedTransactionMessages) {
            try {
                applications.add(new JSONObject(transactionMessage));
            } catch (JSONException e) {  }
        }
        return applications;
    }

    private void throwExceptionIfCannotPublishAssemblyTransaction() {
        if(state != QubicWriterState.ASSEMBLY_PHASE)
            throw new IllegalStateException("assembly transaction can only be published if qubic is in state ASSEMBLY_PHASE, but qubic is in state " + state.name());
        if(specification.timeUntilExecutionStart() <= 0)
            throw new IllegalStateException("the execution phase would have already started, it is too late to publish the assembly transaction now");
    }

    private JSONObject generateAssemblyTransaction(List<String> assembly) {

        JSONObject assemblyTransaction = new JSONObject();
        assemblyTransaction.put(TangleJSONConstants.TRANSACTION_TYPE, "assembly transaction");
        assemblyTransaction.put(TangleJSONConstants.QUBIC_ASSEMBLY, assembly);
        return assemblyTransaction;
    }

    private static QubicWriterState determineQubicWriterStateFromQubicReader(QubicReader qr) {
        if(qr.getAssemblyList() != null)
            return QubicWriterState.EXECUTION_PHASE;
        return (qr.getSpecification().getExecutionStartUnix() < System.currentTimeMillis()/1000) ? QubicWriterState.ABORTED : QubicWriterState.ASSEMBLY_PHASE;
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
        if(state != QubicWriterState.EXECUTION_PHASE && specification.timeUntilExecutionStart() < 0)
            state = QubicWriterState.ABORTED;
        return state.name().toLowerCase().replace('_', ' ');
    }

    enum QubicWriterState {
        PRE_ASSEMBLY_PHASE, ASSEMBLY_PHASE, EXECUTION_PHASE, ABORTED;
    }

    public EditableQubicSpecification getEditableSpecification() {
        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("the specification cannot be edited anymore because the qubic transaction has already been published");
        return specification;
    }

    public QubicSpecification getSpecification() {
        return state == QubicWriterState.PRE_ASSEMBLY_PHASE ? specification : new QubicSpecification(specification);
    }
}