package qlvm;

import constants.GeneralConstants;
import iam.IAMIndex;
import oracle.Assembly;
import oracle.OracleReader;
import oracle.QuorumBasedResult;
import oracle.statements.hash.HashStatementIAMIndex;
import oracle.statements.result.ResultStatementIAMIndex;
import qubic.QubicReader;

import java.util.HashMap;
import java.util.List;

/**
 * @author microhash
 *
 * This class allows to fetch results from other qubics not watched by the OracleWriter.
 * */
public class InterQubicResultFetcher {

    private static final HashMap<String, Assembly> knownAssemblies = new HashMap<>();

    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicId     iam stream id of qubic
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResult(String qubicId, int epochIndex) {
        Assembly assembly = getAssembly(qubicId);
        return findConsensus(assembly, epochIndex);
    }

    public static QuorumBasedResult fetchQubicConsensus(String qubicId, IAMIndex index) {
        Assembly assembly = getAssembly(qubicId);
        return assembly.getConsensusBuilder().buildIAMConsensus(index);
    }
    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicReader QubicReader for qubic to fetch from
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResult(QubicReader qubicReader, int epochIndex) {
        Assembly assembly = getAssembly(qubicReader);
        return findConsensus(assembly, epochIndex);
    }

    private static QuorumBasedResult findConsensus(Assembly assembly, int epochIndex) {
        List<OracleReader> selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE);
        if(!assembly.getConsensusBuilder().hasAlreadyDeterminedQuorumBasedResult(epochIndex)) {
            assembly.fetchStatements(selection, new HashStatementIAMIndex(epochIndex));
            assembly.fetchStatements(selection, new ResultStatementIAMIndex(epochIndex));
        }
        return assembly.getConsensusBuilder().buildConsensus(selection, epochIndex);
    }

    private static Assembly getAssembly(String qubicID) {
        return knownAssemblies.containsKey(qubicID)
            ? knownAssemblies.get(qubicID)
            : createAssembly(new QubicReader(qubicID));
    }

    private static Assembly getAssembly(QubicReader qubicReader) {
        return knownAssemblies.containsKey(qubicReader.getID())
                ? knownAssemblies.get(qubicReader.getID())
                : createAssembly(qubicReader);
    }

    private static Assembly createAssembly(QubicReader qr) {
        List<String> assemblyList = qr.getAssemblyList();
        Assembly assembly = new Assembly(qr);
        assembly.addOracles(assemblyList);
        knownAssemblies.put(qr.getID(), assembly);
        return assembly;
    }
}