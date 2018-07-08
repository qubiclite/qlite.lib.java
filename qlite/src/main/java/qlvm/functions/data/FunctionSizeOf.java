package qlvm.functions.data;

import org.json.JSONArray;
import qlvm.QLVM;
import qlvm.functions.Function;

public class FunctionSizeOf extends Function {

    @Override
    public String getName() { return "size_of"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String o = par[0];

        if(o.startsWith("'"))
            return (o.length()-2)+"";

        if(o.startsWith("["))
            return (new JSONArray(o).length())+"";

        return "-1";
    }
}
