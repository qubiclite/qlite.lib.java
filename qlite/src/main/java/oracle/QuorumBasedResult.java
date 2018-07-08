package oracle;

/**
 * QuorumBasedResults acts as data container for a (result, quorum) pair.
 * 'result'    ... represents the result of a qubic for a specific epoch
 * 'quorum'    ... is the associated amount of oracles coming to this very result
 * 'quorumMax' ... is the maximum quorum that could be reached if all oracles agreed
 * */

public class QuorumBasedResult {

    private final double quorum;
    private final double quorumMax;
    private final String result;

    /**
     * @param quorum    voting power for param result
     * @param quorumMax maximum possible voting power (defined by assembly)
     * @param result    result with highest voting power, NULL if quorum not reached
     */
    public QuorumBasedResult(double quorum, double quorumMax, String result) {
        this.quorum = quorum;
        this.quorumMax = quorumMax;
        this.result = result;
    }

    public double getQuorum() {
        return quorum;
    }

    public double getQuorumMax() {
        return quorumMax;
    }

    public String getResult() {
        return result;
    }
}