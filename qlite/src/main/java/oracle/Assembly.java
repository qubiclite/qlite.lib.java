package oracle;

import constants.GeneralConstants;
import oracle.statements.ResultStatement;
import qubic.QubicReader;

import java.util.*;

/**
 * @author microhash
 *
 * The Assembly models all oracleReaders for a specific qubic. It is the class reponsible
 * to determine the quorum based results.
 * */
public class Assembly {

    private final double QUORUM_MIN = 2D/3D;

    private final List<OracleReader> oracleReaders = new LinkedList<>();
    private final Map<Integer, QuorumBasedResult> alreadyDeterminedQuorumBasedResults = new HashMap<>();
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
        if(assemblyRoots != null)
            for(String assemblyRoot : assemblyRoots)
                oracleReaders.add(new OracleReader(assemblyRoot));
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult determineQuorumBasedResult(int epochIndex) {
        return determineQuorumBasedResult(selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE), epochIndex);
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param selection  a selection of the whole assembly based on which the quorum will be determined probabilisticly
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult determineQuorumBasedResult(List<OracleReader> selection, int epochIndex) {

        // empty assembly
        if(selection.size() == 0)
            return new QuorumBasedResult(0, 0, null);

        // if epoch is ongoing or hasn't even started yet
        if(epochIndex < 0 || epochIndex > qr.lastCompletedEpoch())
            return new QuorumBasedResult(0, selection.size(),null);

        // TODO decide whether this is okay when using probabilistic quorum
        // return result from history if already determined -> increases efficiency
        if(alreadyDeterminedQuorumBasedResults.keySet().contains(epochIndex))
            return alreadyDeterminedQuorumBasedResults.get(epochIndex);

        // determine result
        QuorumBasedResult quorumBasedResult = findConsensus(selection.size(), gatherWeightedResults(selection, epochIndex));

        // add result to list of already known results
        alreadyDeterminedQuorumBasedResults.put(epochIndex, quorumBasedResult);

        return quorumBasedResult;
    }

    /**
     * Ensures that every oracle in the assembly has its Statement for a certain epoch available.
     * @param selection         a selection of the whole assembly (allows probabilisticly determined quorum)
     * @param forHashStatements fetches HashStatements if TRUE, ResultStatements if FALSE
     * @param epoch             index of the epoch to be fetched
     * TODO optimize fetching by putting all findTransaction() requests into a single API call
     * */
    public void fetchEpoch(List<OracleReader> selection, boolean forHashStatements, int epoch) {
        for (OracleReader o : selection)
            o.readStatement(forHashStatements, epoch);
    }

    /**
     * Ensures that every oracle in the assembly has its Statement for a certain epoch available.
     * @param forHashStatements fetches HashStatements if TRUE, ResultStatements if FALSE
     * @param epoch             index of the epoch to be fetched
     * */
    public void fetchEpoch(boolean forHashStatements, int epoch) {
        fetchEpoch(oracleReaders, forHashStatements, epoch);
    }

    /**
     * Weights all results of a certain epoch by the voting power of the respective oracles.
     * This is the basis to determine the quorum. Assumes that the oracles have already been
     * updated with fetchEpoch().
     * @param selection  a selection of the whole assembly based on which the quorum will be determined probabilisticly
     * @param epochIndex index of the requested epoch
     * @return HashMap mapping every result to its accumulated voting power
     * */
    private HashMap<String, Double> gatherWeightedResults(List<OracleReader> selection, int epochIndex) {

        HashMap<String, Double> weightedResults = new HashMap<>();

        for(OracleReader oracleReader : selection) {

            // determine oracles result, ignore result if not found or hash statement invalid
            oracleReader.readStatement(true, epochIndex);
            ResultStatement resStat = (ResultStatement)oracleReader.readStatement(false, epochIndex);

            if(resStat == null || !resStat.isHashStatementValid()) continue;
            String result = resStat.getContent();

            // add vote to HashMap
            double count = weightedResults.containsKey(result) ? weightedResults.get(result) : 0;
            weightedResults.put(resStat.getContent(), count+1); // TODO allow individual oracle voting weights
        }

        return weightedResults;
    }

    /**
     * Determines the QuorumBasedResult from the weighted ResultStatements of an epoch.
     * @param maxScore        maximum possible score (= amount of oracles examined)
     * @param weightedResults a map mapping every result of an epoch to its respective voting power
     * @return the quorum based result
     * */
    private QuorumBasedResult findConsensus(double maxScore, Map<String, Double> weightedResults) {

        // init score variables
        String highScoreResult = null;
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
        highScoreResult = highScore >= maxScore * QUORUM_MIN ? highScoreResult : null;

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
            if(resultEpoch == null || !resultEpoch.isHashStatementValid()) {
                ratings[i] = 0;
                continue;
            }

            // otherwise rate based on correctness of published result
            ratings[i] = quorumResult.equals(resultEpoch.getContent()) ? 1 : -1;
        }

    }

    /**
     * Filters out random oracles. Used to determine quorum deterministically.
     * @param amount amount of oracles to select
     * @return random selection of oracleReaders from the assembly (no double entries)
     * */
    public List<OracleReader> selectRandomOracleReaders(int amount) {

        if(amount < 0)
            throw new IllegalArgumentException("parameter amount cannot be negative");

        amount = Math.min(amount, oracleReaders.size());

        boolean[] selected = new boolean[oracleReaders.size()];
        List<OracleReader> selection = new LinkedList<>();
        while (amount > 0) {
            int randomIndex = (int)(Math.random() * selected.length);
            if(selected[randomIndex])
                continue;
            selection.add(oracleReaders.get(randomIndex));
            amount--;
        }
        return selection;
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
