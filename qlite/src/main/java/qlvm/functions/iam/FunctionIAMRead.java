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
        String iamID = par[0].substring(1, par[0].length()-1);
        int position = parseStringToNumber(par[1]).intValue();
        String keyword = par.length == 2 ? "" : par[2].substring(1, par[2].length()-1);
        JSONObject o = new IAMReader(iamID).read(new IAMIndex(keyword, position));
        return o == null ? null : o.toString();
    }
}