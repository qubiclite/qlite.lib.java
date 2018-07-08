package qlvm.functions.iota;

import qlvm.QLVM;
import qlvm.functions.Function;
import tangle.TangleAPI;

public class FunctionIotaBalance extends Function {

    @Override
    public String getName() { return "iota_balance"; }

    @Override
    public String call(QLVM qlvm, String[] par) {
        String address = qlvm.unescapeString(par[0]);
        return ""+ TangleAPI.getInstance().getBalance(address);
    }
}



