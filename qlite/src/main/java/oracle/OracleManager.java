package oracle;

import qubic.QubicReader;
import qubic.QubicSpecification;

/**
 * @author microhash
 *
 * The OracleManager models an automated life cycle for an OracleWriter. It takes
 * care of all actions that have to be taken and thus provides a simple interface to run
 * an Oracle. The OracleManager is the very core of a q-node.
 * Use start() for asynchronous, startSynchronous() for synchronous execution.
 * */
public class OracleManager {

    private final OracleWriter ow;
    private State state = State.PAUSED;

    public OracleManager(OracleWriter ow) {
        this.ow = ow;
        this.ow.setManager(this);
    }

    public void start() {
        new Thread() {
            @Override
            public void run() {
                startSynchronous();
            }
        }.start();
    }


    /**
     * Runs the oracle life cycle synchronously as opposed to start().
     * */
    public void startSynchronous() {

        state = State.PRE_EXECUTION;

        if(ow.getQubicReader().getSpecification().timeUntilExecutionStart() > 0) {
            ow.apply();
            takeABreak(ow.getQubicReader().getSpecification().timeUntilExecutionStart());
        }

        if(ow.assemble()) {
            runEpochs();
        } else {
            state = State.ABORTED;
        }
    }

    /**
     * Runs the qubic life cycle during the execution phase by publishing
     * HashStatements and ResultStatements until interrupted with terminate().
     * */
    public void runEpochs() {

        // not part of assembly
        if(!ow.isAcceptedIntoAssembly())
            return;

        state = State.RUNNING;
        while(state != State.PAUSING)
            runEpochAndCatchThrowable();

        state = State.PAUSED;
    }

    private void runEpochAndCatchThrowable() {
        try {
            tryToRunEpoch();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void tryToRunEpoch() {
        final QubicReader qubic = ow.getQubicReader();
        final QubicSpecification spec =  qubic.getSpecification();

        final int e = determineEpochToRun();
        final long epochStart = spec.getExecutionStartUnix() + e * spec.getEpochDuration();

        // run hash epoch
        takeABreak(epochStart - getUnixTimeStamp());
        ow.doHashStatement(e);

        // run result epoch
        takeABreak(epochStart - getUnixTimeStamp() + spec.getHashPeriodDuration());
        ow.doResultStatement();
    }

    /**
     * Determines the current epoch to run by timestamps. Skips the current epoch
     * if the hash period has already progressed beyond 30%
     * @return epoch to run
     * */
    private int determineEpochToRun() {

        final QubicSpecification spec = ow.getQubicReader().getSpecification();

        // TODO base decision on runtime limit
        // skip running epoch if 30% of hash period is already over
        double relOverTime = (double)(spec.ageOfExecutionPhase()%spec.getEpochDuration())/spec.getHashPeriodDuration();
        int skippedEpochs = relOverTime > 0.3 ? 1 : 0;
        return (spec.ageOfExecutionPhase() / spec.getEpochDuration()) + skippedEpochs;
    }

    /**
     * Stops runEpochs right after finishing the next epoch.
     * */
    public void terminate() {
        state = State.PAUSING;
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

    enum State {
        PRE_EXECUTION, RUNNING, PAUSED, PAUSING, ABORTED
    }

    /**
     * @return TRUE if oracle is creating epochs, FALSE otherwise
     * */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * @return TRUE if oracle has been paused and is no longer creating epochs, FALSE otherwise
     * */
    public boolean isPaused() {
        return state == State.PAUSED;
    }

    /**
     * Aborted oracles are useless and can be deleted.
     * If, for example, an oracle does not make it into the assembly, it is considered aborted.
     * @return TRUE if oracle has been aborted, FALSE otherwise
     * */
    public boolean isAborted() {
        return state == State.ABORTED;
    }

    /**
     * @return current state as String
     * */
    public String getState() {
        return state.name().toLowerCase();
    }

    /**
     * @return current unix timestamp
     * */
    private long getUnixTimeStamp() {
        return System.currentTimeMillis() / 1000;
    }
}