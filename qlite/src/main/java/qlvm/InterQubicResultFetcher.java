package qlvm;

import constants.GeneralConstants;
import oracle.Assembly;
import oracle.OracleReader;
import oracle.QuorumBasedResult;
import qubic.QubicReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author microhash
 *
 * This class allows to fetch results from other qubics not watched by the OracleWriter.
 * */
public class InterQubicResultFetcher {

    private static final HashMap<String, Assembly> assemblies = new HashMap<>();

    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicId     iam stream id of qubic
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResult(String qubicId, int epochIndex) {

        Assembly assembly = getAssembly(qubicId);
        List<OracleReader> selection = assembly.selectRandomOracleReaders(GeneralConstants.QUORUM_MAX_ORACLE_SELECTION_SIZE);

        assembly.fetchEpoch(selection,true, epochIndex);
        assembly.fetchEpoch(selection, false, epochIndex);

        return assembly.determineQuorumBasedResult(selection, epochIndex);
    }
    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicReader QubicReader for qubic to fetch from
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResult(QubicReader qubicReader, int epochIndex) {

        Assembly assembly = getAssembly(qubicReader);

        assembly.fetchEpoch(true, epochIndex);
        assembly.fetchEpoch(false, epochIndex);

        return assembly.determineQuorumBasedResult(epochIndex);
    }

    private static Assembly getAssembly(String qubicID) {

        Assembly assembly;

        if(assemblies.containsKey(qubicID)) {
            assembly = assemblies.get(qubicID);
        } else {
            QubicReader qr = new QubicReader(qubicID);
            List<String> assemblyList = qr.getAssemblyList();
            assembly = new Assembly(qr);
            assembly.addOracles(assemblyList);
            assemblies.put(qr.getID(), assembly);
        }

        return assembly;
    }

    private static Assembly getAssembly(QubicReader qr) {

        Assembly assembly;

        if(assemblies.containsKey(qr.getID())) {
            assembly = assemblies.get(qr.getID());
        } else {
            List<String> assemblyList = qr.getAssemblyList();
            assembly = new Assembly(qr);
            assembly.addOracles(assemblyList);
            assemblies.put(qr.getID(), assembly);
        }

        return assembly;
    }
}