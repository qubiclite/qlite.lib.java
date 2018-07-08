package qlvm.exceptions.runtime;

import constants.GeneralConstants;

public class QLValueMaxLengthExceeded extends QLRunTimeException {

    public QLValueMaxLengthExceeded(String val) {
        super("value exceeded maximum length of "+ GeneralConstants.QLVM_MAX_VALUE_LENGTH+" characters: " + val.substring(0, 100) + "...");
    }
}
