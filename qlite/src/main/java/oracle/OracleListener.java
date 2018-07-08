package oracle;

/**
 * The OracleListener listens and reacts to events happening in the OracleWriter. This is useful
 * when the OracleWriter is being run by an OracleManager and it's hard to intervene.
 * To use, just over-write the methods and pass the OracleListener object to the OracleWriter.
 *
 * @see OracleWriter
 * @see OracleManager
 * */
public class OracleListener {

    /**
     * Is called right at the start of a new epoch (not in epoch #0) after the last epoch
     * has been fetched.
     * @param epochIndex epochIndex of last epoch
     * @param qbr        the quorum based result found for the epoch
     * */
    public void onReceiveEpochResult(int epochIndex, QuorumBasedResult qbr) {}
}