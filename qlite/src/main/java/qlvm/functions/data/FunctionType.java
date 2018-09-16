package qlvm.functions.data;

import qlvm.QLVM;
import qlvm.functions.Function;

public class FunctionType extends Function {

    @Override
    public String getName() { return "type"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String o = par[0];

        if(o == null || o.equals(""))
            return "'null'";

        if(o.startsWith("'"))
            return "'string'";

        if(o.startsWith("{"))
            return "'json'";

        if(o.startsWith("["))
            return "'array'";

        if(QLVM.OBJECT_NUMBER.matcher(o).find())
            return "'number'";

        return "'unknown'";
    }
}
