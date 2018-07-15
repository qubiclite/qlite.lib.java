package qlvm;

import oracle.Assembly;
import oracle.QuorumBasedResult;
import qubic.QubicReader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author microhash
 *
 * This class allows to fetch results from other qubics not watched by the OracleWriter.
 * */
public class InterQubicResultFetcher {

    private static final HashMap<String, Assembly> assemblies = new HashMap<>();

    /**
     * Fetches the QuorumBasedResult from any qubic.
     * @param qubicId tangle stream id of qubic
     * @param epochIndex  index of the epoch of which the result shall be determined
     * @return the fetched QuorumBasedResult
     * */
    public static QuorumBasedResult fetchResult(String qubicId, int epochIndex) {

        Assembly assembly;

        if(assemblies.containsKey(qubicId)) {
            assembly = assemblies.get(qubicId);
        } else {
            QubicReader qubicReader = new QubicReader(qubicId);
            ArrayList<String> assemblyList = qubicReader.getAssemblyList();

            // no assembly transaction
            if(assemblyList == null)
                return new QuorumBasedResult(0, 0, null);

            assembly = new Assembly(qubicReader);
            assembly.addOracles(assemblyList);
            assemblies.put(qubicId, assembly);
        }

        assembly.fetchEpoch(true, epochIndex);
        assembly.fetchEpoch(false, epochIndex);

        return assembly.determineQuorumBasedResult(epochIndex);
    }
}