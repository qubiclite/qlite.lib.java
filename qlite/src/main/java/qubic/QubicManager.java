package qubic;

import constants.TangleJSONConstants;
import org.json.JSONObject;

/**
 * @author microhash
 *
 * The QubicManager models an automated life cycle for a Qubic. It takes care of all
 * actions that have to be taken and thus provides a simple interface to run a Qubic.
 * Use start() for asynchronous, startSynchronous() for synchronous execution.
 *
 * TODO rewrite
 * */
@Deprecated
public class QubicManager extends Thread {

    private final static int ASSEMBLY_TX_PUBLISHING_INTERVAL = 20;
    private final static int PROMOTION_INTERVAL = 20;

    private final QubicWriter qubicWriter;

    public QubicManager(QubicWriter writer) {
        this.qubicWriter = writer;
    }

    @Override
    public void run() {
        startSynchronous();
    }

    /**
     * Runs the qubic life cycle synchronously as opposted to start().
     * */
    public void startSynchronous() {

        qubicWriter.publishQubicTransaction();

        while(qubicWriter.getSpecification().timeUntilExecutionStart() > ASSEMBLY_TX_PUBLISHING_INTERVAL) {
            qubicWriter.promote();
            takeABreak(Math.min(PROMOTION_INTERVAL, qubicWriter.getSpecification().timeUntilExecutionStart() - ASSEMBLY_TX_PUBLISHING_INTERVAL));
        }

        handleApplications();
        qubicWriter.publishAssemblyTransaction();
    }

    /**
     * Autonomously checks applications and accepts applicants into the assembly.
     * publishAssemblyTransaction() has to be called manually. Currently accepts every applicant,
     * no oracle is filtered out. TODO actually exclude some
     * */
    public void handleApplications() {
        for(JSONObject application : qubicWriter.fetchApplications()) {
            String applicantID = application.getString(TangleJSONConstants.ORACLE_ID);
            if(!qubicWriter.getAssembly().contains(applicantID))
                qubicWriter.getAssembly().add(applicantID);
        }
    }

    /**
     * lets the thread sleep
     * @param s amount of seconds to sleep
     * */
    private void takeABreak(long s) {
        if(s <= 0) return;
        try { Thread.sleep(s * 1000); } catch (InterruptedException e) { }
    }
}