package qlvm.functions.iam;

import org.json.JSONObject;
import qlvm.QLVM;
import qlvm.functions.Function;
import tangle.IAMReader;

public class FunctionIAMRead extends Function {

    @Override
    public String getName() { return "iam_read"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String iamID = par[0];
        iamID = iamID.substring(1, iamID.length()-1);

        int index = parseStringToNumber(par[1]).intValue();

        JSONObject o = new IAMReader(iamID).read(index);
        return o == null ? null : o.toString();
    }
}