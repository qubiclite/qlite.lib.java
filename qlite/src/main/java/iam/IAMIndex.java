package iam;

import org.apache.commons.lang3.StringUtils;
import tangle.TryteTool;

import java.security.InvalidParameterException;

public class IAMIndex {

    public static final int STREAM_HANDLE_LENGTH = 30;
    public static final int MAX_KEYWORD_LENGTH = 30;
    private static final int  MAX_POSITION_LENGTH = TryteTool.TRYTES_PER_ADDRESS - STREAM_HANDLE_LENGTH - MAX_KEYWORD_LENGTH;

    private final long position;
    private final String keyword;

    public IAMIndex(long position) {
        this.position = position;
        this.keyword = "";
        throwExceptionIfInvalid();
    }

    public IAMIndex(String keyword, long position) {
        this.position = position;
        this.keyword = keyword != null ? keyword : "";
        throwExceptionIfInvalid();
    }

    private void throwExceptionIfInvalid() {
        if(position < 0)
            throw new InvalidParameterException("parameter position cannot be negative");
        if(!TryteTool.isTryteSequence(keyword))
            throw new InvalidParameterException("parameter keyword is required to be a tryte sequence");
        if(keyword.length() > MAX_KEYWORD_LENGTH)
            throw new InvalidParameterException("parameter keyword cannot be longer than " + MAX_KEYWORD_LENGTH + " trytes");
    }

    public long getPosition() {
        return position;
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public String toString() {
        return padKeyword() + padPosition();
    }

    private String padPosition() {
        return StringUtils.leftPad(TryteTool.positiveLongToTrytes(position), MAX_POSITION_LENGTH, '9');
    }

    private String padKeyword() {
        return StringUtils.rightPad(keyword, MAX_KEYWORD_LENGTH, '9');
    }
}
