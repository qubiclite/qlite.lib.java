package oracle;

import qubic.QubicReader;

/**
 * @author microhash
 *
 * The OracleManager models an automated life cycle for an OracleWriter. It takes
 * care of all actions that have to be taken and thus provides a simple interface to run
 * an Oracle. The OracleManager is the very core of a q-node.
 * Use start() for asynchronous, startSynchronous() for synchronous execution.
 * */
public class OracleManager extends Thread {

    private final OracleWriter ow;
    private boolean stop = false;

    public OracleManager(OracleWriter ow) {
        this.ow = ow;
        this.ow.setManager(this);
    }

    @Override
    public void run() {
        startSynchronous();
    }

    /**
     * Runs the oracle life cycle synchronously as opposted to start().
     * */
    public void startSynchronous() {
        ow.apply();
        takeABreak(ow.getQubicReader().getExecutionStart() - getUnixTimeStamp());
        if(ow.assemble())
            runEpochs();
    }

    /**
     * Runs the qubic life cycle during the execution phase by publishing
     * HashStatements and ResultStatements until interrupted with terminate().
     * */
    public void runEpochs() {
        final QubicReader qubic = ow.getQubicReader();

        final long executionStart = qubic.getExecutionStart();
        final int hashPeriodDuration = qubic.getHashPeriodDuration();
        final int resultPeriodDuration = qubic.getResultPeriodDuration();
        final int epochDuration = hashPeriodDuration + resultPeriodDuration;

        while(!stop) {

            final int e = determineEpochToRun();
            final long epochStart = executionStart + e * epochDuration;

            // run hash epoch
            takeABreak(epochStart - getUnixTimeStamp());
            ow.doHashStatement(e);

            // run result epoch
            takeABreak(epochStart - getUnixTimeStamp() + hashPeriodDuration);
            ow.doResultStatement();
        }
    }

    /**
     * Determines the current epoch to run by timestamps. Skips the current epoch
     * if the hash period has already progressed beyond 30%
     * @return epoch to run
     * */
    private int determineEpochToRun() {

        final QubicReader qubic = ow.getQubicReader();

        long executionStart = qubic.getExecutionStart();
        int hashPeriodDuration = qubic.getHashPeriodDuration();
        int resultPeriodDuration = qubic.getResultPeriodDuration();
        int epochDuration = hashPeriodDuration + resultPeriodDuration;

        long timeRunning = getUnixTimeStamp() - executionStart;

        // skip running epoch if 30% of hash period is already over
        double relOverTime = (double)(timeRunning%epochDuration)/hashPeriodDuration;
        int skippedEpoches = relOverTime > 0.3 ? 1 : 0;

        return (int)(timeRunning / epochDuration) + skippedEpoches;
    }

    /**
     * Stops runEpochs right after finishing the next epoch.
     * */
    public void terminate() {
        stop = true;
    }

    /**
     * Pauses the thread.
     * @param s amount of seconds to pause.
     * */
    private void takeABreak(long s) {
        if(s <= 0) return;

        try {
            Thread.sleep(s * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return current unix timestamp
     * */
    private long getUnixTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }
}