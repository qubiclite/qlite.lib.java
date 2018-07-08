package qubic;

import constants.TangleJSONConstants;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author microhash
 *
 * The QubicManager models an automated life cycle for a Qubic. It takes care of all
 * actions that have to be taken and thus provides a simple interface to run a Qubic.
 * Use start() for asynchronous, startSynchronous() for synchronous execution.
 * */
public class QubicManager extends Thread {

    private final static int ASSEMBLY_TX_PUBLISHING_INTERVAL = 20;
    private final static int PROMOTION_INTERVAL = 20;

    private final QubicWriter writer;

    public QubicManager(QubicWriter writer) {
        this.writer = writer;
    }

    @Override
    public void run() {
        startSynchronous();
    }

    /**
     * Runs the qubic life cycle synchronously as opposted to start().
     * */
    public void startSynchronous() {

        writer.publishQubicTx();

        while(getTimeUntilAssemblyTransaction() > ASSEMBLY_TX_PUBLISHING_INTERVAL) {
            writer.promote();
            takeABreak(Math.min(PROMOTION_INTERVAL, getTimeUntilAssemblyTransaction() - ASSEMBLY_TX_PUBLISHING_INTERVAL));
        }

        handleApplications();
        writer.publishAssemblyTx();
    }

    /**
     * Autonomously checks applications and accepts applicants into the assembly.
     * publishAssemblyTx() has to be called manually. Currently accepts every applicant,
     * no oracle is filtered out. TODO actually exclude some
     * */
    public void handleApplications() {
        writer.fetchApplications();
        for(JSONObject application : writer.getApplications()) {
            String applicantID = application.getString(TangleJSONConstants.ORACLE_ID);
            if(!writer.getAssembly().contains(applicantID))
                writer.addToAssembly(applicantID);
        }
    }

    /**
     * @return time in seconds until assembly transactiomn has to be published
     * */
    private int getTimeUntilAssemblyTransaction() {
        return (int)(writer.getExecutionStart()-getUnixTimeStamp());
    }

    /**
     * lets the thread sleep
     * @param s amount of seconds to sleep
     * */
    private void takeABreak(long s) {
        if(s <= 0) return;
        try { Thread.sleep(s * 1000); } catch (InterruptedException e) { }
    }

    /**
     * @return current unix timestamp
     * */
    private long getUnixTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }
}