package qlvm.functions.string;

import qlvm.QLVM;
import qlvm.functions.Function;

public class FunctionSubstr extends Function {

    @Override
    public String getName() { return "substr"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String orig = par[0].substring(1, par[0].length()-1);
        // TODO exception if parsing fails
        int start = Integer.parseInt(par[1]);
        int end = Integer.parseInt(par[2]);

        return "'" +orig.substring(Math.max(start, 0), Math.min(end, orig.length())) + "'";
    }
}
