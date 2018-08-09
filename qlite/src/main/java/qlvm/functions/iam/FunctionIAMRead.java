package qlvm.functions.iam;

import iam.IAMIndex;
import org.json.JSONObject;
import qlvm.QLVM;
import qlvm.functions.Function;
import iam.IAMReader;

public class FunctionIAMRead extends Function {

    @Override
    public String getName() { return "iam_read"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String iamID = par[0];
        iamID = iamID.substring(1, iamID.length()-1);

        int position = parseStringToNumber(par[1]).intValue();

        JSONObject o = new IAMReader(iamID).read(new IAMIndex(position));
        return o == null ? null : o.toString();
    }
}