package qlvm.functions;

import qlvm.QLVM;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public abstract class Function {

    private static final NumberFormat NF = NumberFormat.getNumberInstance(Locale.US);

    public abstract String getName();

    public abstract String call(QLVM qlvm, String[] par);

    protected static Number parseStringToNumber(String s) {
        try {
            return NF.parse(s);
        } catch (ParseException e) {
            return 0;
        }
    }
}