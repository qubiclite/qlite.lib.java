package qlvm.functions.qubic;

import qlvm.InterQubicResultFetcher;
import oracle.QuorumBasedResult;
import qlvm.QLVM;
import qlvm.exceptions.runtime.QLRunTimeException;
import qlvm.exceptions.runtime.UnknownFunctionException;
import qlvm.functions.Function;

public class FunctionQubicFetch extends Function {

    @Override
    public String getName() { return "qubic_fetch"; }

    @Override
    public String call(QLVM qlvm, String[] par) {

        if(qlvm.isInTestMode())
            throw new UnknownFunctionException("qubic_fetch");
        
        String qubicRoot = par[0];
        qubicRoot = qubicRoot.substring(1, qubicRoot.length()-1);

        int epochIndex = parseStringToNumber(par[1]).intValue();

        QuorumBasedResult qbr;

        if(qubicRoot.equals(qlvm.getOracleWriter().getQubicReader().getID()))
            qbr = qlvm.getOracleWriter().getAssembly().determineQuorumBasedResult(epochIndex);
        else
            qbr = InterQubicResultFetcher.fetchResult(qubicRoot, epochIndex);

        return qbr.getResult();
    }
}
