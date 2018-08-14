package oracle;

import constants.GeneralConstants;
import oracle.statements.result.ResultStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsensusBuilder {

    private final double QUORUM_MIN = 2D/3D;
    private final Assembly assembly;

    private final Map<Integer, QuorumBasedResult> alreadyDeterminedQuorumBasedResults = new HashMap<>();

    public ConsensusBuilder(Assembly assembly) {
        this.assembly = assembly;
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult buildConsensus(int epochIndex) {
        return buildConsensus(null, epochIndex);
    }

    /**
     * Determines the quorum based result for a specific epoch.
     * @param selection  a selection of the whole assembly based on which the quorum will be determined probabilisticly
     * @param epochIndex index of the epoch for which the result shall be determined
     * @return quorum based result
     * */
    public QuorumBasedResult buildConsensus(List<OracleReader> selection, int epochIndex) {

        if(alreadyDeterminedQuorumBasedResults.keySet().contains(epochIndex))
            return alreadyDeterminedQuorumBasedResults.get(epochIndex);

        if(selection == null)
            selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE);

        // empty assembly
        if(selection.size() == 0)
            return new QuorumBasedResult(0, 0, null);

        // if epoch is ongoing or hasn't even started yet
        if(epochIndex < 0 || epochIndex > assembly.getQubicReader().lastCompletedEpoch())
            return new QuorumBasedResult(0, selection.size(),null);

        // TODO decide whether this is okay when using probabilistic quorum
        // return result from history if already determined -> increases efficiency

        // determine result
        QuorumBasedResult quorumBasedResult = findVotingQuorum(accumulateEpochVotings(selection, epochIndex), selection.size());

        // add result to list of already known results
        alreadyDeterminedQuorumBasedResults.put(epochIndex, quorumBasedResult);

        return quorumBasedResult;
    }

    private Map<String, Double> accumulateEpochVotings(List<OracleReader> voters, int epochIndex) {
        Map<String, Double> quorumVoting = new HashMap<>();
        for(OracleReader oracleReader : voters)
            addOraclesVoteToVoting(oracleReader, epochIndex, quorumVoting);
        return quorumVoting;
    }

    private static void addOraclesVoteToVoting(OracleReader oracleReader, int epochIndex, Map<String, Double> voting) {
        oracleReader.getHashStatementReader().read(epochIndex);
        ResultStatement resultStatement = oracleReader.getResultStatementReader().read(epochIndex);
        if(resultStatement != null && resultStatement.isHashStatementValid())
            addResultStatementToQuorumVoting(voting, resultStatement);
    }

    private static void addResultStatementToQuorumVoting(Map<String, Double> quorumVoting, ResultStatement resultStatement) {
        String result = resultStatement.getContent();
        double count = quorumVoting.containsKey(result) ? quorumVoting.get(result) : 0;
        quorumVoting.put(resultStatement.getContent(), count+1); // TODO allow individual oracle voting weights
    }

    private QuorumBasedResult findVotingQuorum(Map<String, Double> voting, double totalVotesAllowed) {

        // init score variables
        String highScoreResult = null;
        double highScore = 0;

        // search for result with highest voting score
        for(String result : voting.keySet()) {
            double score = voting.get(result);

            // new high score result found?
            if(score > highScore) {
                highScoreResult = result;
                highScore = score;
            }
        }

        // return result with highest score or NULL if it doesn't have at least 2/3 of votes
        highScoreResult = highScore >= totalVotesAllowed * QUORUM_MIN ? highScoreResult : null;

        return new QuorumBasedResult(highScore, totalVotesAllowed, highScoreResult);
    }

}
