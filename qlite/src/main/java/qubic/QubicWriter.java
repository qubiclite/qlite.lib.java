package qubic;

import constants.TangleJSONConstants;
import iam.IAMIndex;
import iam.IAMWriter;
import org.json.JSONException;
import org.json.JSONObject;
import tangle.TangleAPI;
import tangle.TryteTool;

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

    static final IAMIndex QUBIC_TRANSACTION_IAM_INDEX = new IAMIndex(0);
    static final IAMIndex ASSEMBLY_TRANSACTION_IAM_INDEX = new IAMIndex(1);

    private final IAMWriter writer;
    private final List<String> assembly = new LinkedList<>();
    private String qubicTransactionHash, assemblyTransactionHash;
    private final EditableQubicSpecification editable;

    private QubicWriterState state = QubicWriterState.PRE_ASSEMBLY_PHASE;

    public QubicWriter() {
        writer = new IAMWriter();
        editable = new EditableQubicSpecification();
    }

    /**
     * Recreates an already existing Qubic by its IAMStream identity.
     * @param writer IAMStream of the qubic
     * */
    public QubicWriter(IAMWriter writer) {
        this.writer = new IAMWriter();
        QubicReader qr = new QubicReader(writer.getID());
        editable = new EditableQubicSpecification(qr.getSpecification());
        state = determineQubicWriterStateFromQubicReader(qr);
    }

    /**
     * Publishes the qubic transaction to the IAMStream.
     * */
    public synchronized void publishQubicTransaction() {

        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("qubic transaction can only be published if qubic is in state PRE_ASSEMBLY_PHASE, but qubic is in state " + state.name());

        editable.throwExceptionIfTooLateToPublish();
        JSONObject qubicTransactionJSON = editable.generateQubicTransactionJSON();
        qubicTransactionHash = writer.publish(QUBIC_TRANSACTION_IAM_INDEX, qubicTransactionJSON);
        state = QubicWriterState.ASSEMBLY_PHASE;
    }

    /**
     * Publicly promotes the qubic transaction on the tangle to attract oracles for its assembly.
     * */
    public void promote() {
        String address = TryteTool.buildCurrentQubicPromotionAddress();
        TangleAPI.getInstance().sendTrytes(address, writer.getID());
    }

    /**
     * Publishes the assembly transaction to the IAMStream. The assembly will consist
     * of all oracles added via addOracle().
     * */
    public synchronized void publishAssemblyTransaction() {
        throwExceptionIfCannotPublishAssemblyTransaction();
        JSONObject assemblyTransaction = generateAssemblyTransaction(assembly);
        assemblyTransactionHash = writer.publish(ASSEMBLY_TRANSACTION_IAM_INDEX, assemblyTransaction);
        state = QubicWriterState.EXECUTION_PHASE;
    }

    public String getID() {
        return writer.getID();
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
        if(editable.timeUntilExecutionStart() <= 0)
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

    public String getQubicTransactionHash() {
        return qubicTransactionHash;
    }

    public String getAssemblyTransactionHash() {
        return assemblyTransactionHash;
    }

    public IAMWriter getWriter() {
        return writer;
    }

    public List<String> getAssembly() {
        return assembly;
    }

    public EditableQubicSpecification getEditable() {
        if(state != QubicWriterState.PRE_ASSEMBLY_PHASE)
            throw new IllegalStateException("the specification cannot be edited anymore because the qubic transaction has already been published");
        return editable;
    }

    public QubicSpecification getSpecification() {
        return state == QubicWriterState.PRE_ASSEMBLY_PHASE ? editable : new QubicSpecification(editable);
    }

    public String getState() {
        if(state != QubicWriterState.EXECUTION_PHASE && editable.timeUntilExecutionStart() < 0)
            state = QubicWriterState.ABORTED;
        return state.name().toLowerCase().replace('_', ' ');
    }

    enum QubicWriterState {
        PRE_ASSEMBLY_PHASE, ASSEMBLY_PHASE, EXECUTION_PHASE, ABORTED;
    }
}