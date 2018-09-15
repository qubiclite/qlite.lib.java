package qlvm;

import constants.GeneralConstants;
import qlvm.exceptions.runtime.QLValueMaxLengthExceeded;
import qlvm.exceptions.runtime.UnknownFunctionException;
import qlvm.functions.*;
import qlvm.functions.data.FunctionSizeOf;
import qlvm.functions.data.FunctionType;
import qlvm.functions.iam.FunctionIAMRead;
import qlvm.functions.iota.*;
import qlvm.functions.qubic.FunctionQubicConsensus;
import qlvm.functions.qubic.FunctionQubicFetch;
import qlvm.functions.string.FunctionHash;
import qlvm.functions.string.FunctionSubstr;

/**
 * @author microhash
 *
 * FunctionCall provides a way to call any function formally. It is easily extendable
 * and therefore was implemented to bundle and manage all Functions.
 * */
public final class FunctionCall {

    private static final Function[] functions = {
        new FunctionQubicFetch(),
        new FunctionQubicConsensus(),
        new FunctionIAMRead(),
        new FunctionIotaBalance(),
        new FunctionSizeOf(),
        new FunctionType(),
        new FunctionSubstr(),
        new FunctionHash(),
    };

    private FunctionCall() {}

    /**
     * Calls a function on a specific QLVM.
     * @param qlvm the QLVM in which the function was called, provides the data for the actual function
     * @param functionName the name of the function (e.g. "qubic_fetch")
     * @param par normalized function parameters
     * @return return value of the actual function or NULL if function not found
     * */
    public static String call(QLVM qlvm, String functionName, String[] par) {

        for(Function f : functions)
            if(f.getName().equals(functionName)) {
                String ret = f.call(qlvm, par);
                if(ret != null && ret.length() > GeneralConstants.QLVM_MAX_VALUE_LENGTH)
                    throw new QLValueMaxLengthExceeded(ret);
                return ret;
            }

        throw new UnknownFunctionException(functionName);
    }
}