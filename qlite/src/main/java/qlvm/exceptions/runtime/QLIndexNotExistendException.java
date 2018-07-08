package qlvm.exceptions.runtime;

public class QLIndexNotExistendException extends QLRunTimeException {

    public QLIndexNotExistendException(String indexable, String index) {
        super("index "+index+" is not existend in '"+indexable+"'");
    }
}
