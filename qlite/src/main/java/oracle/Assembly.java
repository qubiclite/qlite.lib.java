package oracle;

import oracle.statements.ResultStatement;
import qubic.QubicReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * @author microhash
 *
 * The Assembly models all oracleReaders for a specific qubic. It is the class reponsible
 * to determine the quorum based results.
 *
 * TODO implement probabilistic result determination for large assemblies
 * */
public class Assembly {

    private final double QUORUM_MIN = 2D/3D;

    private final LinkedList<OracleReader> oracleReaders = new LinkedList<>();
    private final HashMap<Integer, QuorumBasedResult> alreadyDeterminedQuorumBasedResults = new HashMap<>();
    private int[] ratings;

    private final QubicReader qr;

    public Assembly(QubicReader qr) {
        this.qr = qr;
    }

    /**
     * Adds oracleReaders to the assembly.
     * @param assemblyRoots the oracle ids of each respective oracle to add
     * */
    public void addOracles(ArrayList<String> assemblyRoots) {
        for(String assemblyRoot : assemblyRoots)
            oracleReaders.add(new OracleReader(assemblyRoot));
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult determineQuorumBasedResult(int epochIndex) {

        // if epoch is ongoing or hasn't even started yet
        if(epochIndex < 0 || epochIndex > qr.lastCompletedEpoch())
            return new QuorumBasedResult(0, oracleReaders.size(),null);

        // return result from history if already determined -> increases efficiency
        if(alreadyDeterminedQuorumBasedResults.keySet().contains(epochIndex))
            return alreadyDeterminedQuorumBasedResults.get(epochIndex);

        // determine result
        QuorumBasedResult quorumBasedResult = findConsensus(gatherWeightedResults(epochIndex));

        // add result to list of already known results
        alreadyDeterminedQuorumBasedResults.put(epochIndex, quorumBasedResult);

        return quorumBasedResult;
    }

    /**
     * Ensures that every oracle in the assembly has its own qnode.statements from a certain epoch available.
     * @param forHashStatements fetches HashStatements if TRUE, ResultStatements if FALSE
     * @param epoch             index of the epoch to be fetched
     * TODO optimize fetching by putting all findTransaction() requests into a single API call
     * */
    public void fetchEpoch(boolean forHashStatements, int epoch) {
        for (OracleReader o : oracleReaders)
            o.readStatement(forHashStatements, epoch);
    }

    /**
     * Weights all results of a certain epoch by the voting power of the respective oracles.
     * This is the basis to determine the quorum. Assumes that the oracles have already been
     * updated with fetchEpoch().
     * @param epochIndex index of the requested epoch
     * @return HashMap mapping every result to its accumulated voting power
     * */
    private HashMap<String, Double> gatherWeightedResults(int epochIndex) {

        HashMap<String, Double> weightedResults = new HashMap<>();

        for(OracleReader oracleReader : oracleReaders) {

            // determine oracles result, ignore result if not found or hash statement invalid
            oracleReader.readStatement(true, epochIndex);
            ResultStatement resStat = (ResultStatement)oracleReader.readStatement(false, epochIndex);

            if(resStat == null || !resStat.isHashStatementValid(oracleReader.getID())) continue;
            String result = resStat.getContent();

            // add vote to HashMap
            double count = weightedResults.containsKey(result) ? weightedResults.get(result) : 0;
            weightedResults.put(resStat.getContent(), count+1);
        }

        return weightedResults;
    }

    /**
     * Determines the QuorumBasedResult from the weighted ResultStatements of an epoch.
     * @param weightedResults a map mapping every result of an epoch to its respective voting power
     * @return the quorum based result
     * */
    private QuorumBasedResult findConsensus(HashMap<String, Double> weightedResults) {

        // init score variables
        String highScoreResult = null;
        double totalScore = oracleReaders.size();
        double highScore = 0;

        // search for result with highest voting score
        for(String result : weightedResults.keySet()) {
            double score = weightedResults.get(result);

            // new high score result found?
            if(score > highScore) {
                highScoreResult = result;
                highScore = score;
            }
        }

        // return result with highest score or NULL if it doesn't have at least 2/3 of votes
        highScoreResult = highScore >= totalScore * QUORUM_MIN ? highScoreResult : null;

        return new QuorumBasedResult(highScore, oracleReaders.size(), highScoreResult);
    }

    /**
     * Rates every oracle in the assembly by its contribution during the last epoch.
     * The ratings are published in the subseqeuent epoches HashStatement. This allows
     * a future reconstruction to prevent oracles from publishing results retrospectively.
     *
     * ratings: -1 = negative, 0 = neutral, 1 = positive
     *
     * @param epochIndex   index of epoch that shall be rated
     * @param quorumResult quorum based result (determines which nodes are correct)
     *
     * TODO actually call from somewhere
     * */
    protected void rate(int epochIndex, String quorumResult) {
        // reset ratings
        ratings = new int[oracleReaders.size()];

        // rate every oracle
        for(int i = 0; i < oracleReaders.size(); i++) {
            OracleReader oracleReader = oracleReaders.get(i);

            // neutral rating for qnode.statements that were ignored in the last epoch
            // due to not being existent or following an invalid hash statement
            ResultStatement resultEpoch = (ResultStatement)oracleReader.readStatement(false, epochIndex);
            if(resultEpoch == null || !resultEpoch.isHashStatementValid(oracleReader.getID())) {
                ratings[i] = 0;
                continue;
            }

            // otherwise rate based on correctness of published result
            ratings[i] = quorumResult.equals(resultEpoch.getContent()) ? 1 : -1;
        }

    }

    /**
     * @return amount of oracles in the assembly
     * */
    public int size() {
        return oracleReaders.size();
    }

    protected int[] getRatings() {
        return ratings == null ? new int[oracleReaders.size()] : ratings;
    }
}
