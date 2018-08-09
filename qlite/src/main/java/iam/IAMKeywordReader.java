package iam;

import jota.model.Transaction;
import org.json.JSONObject;

import java.util.List;

public class IAMKeywordReader {

    private final String keyword;
    private final IAMReader generalReader;

    public IAMKeywordReader(IAMReader generalReader, String keyword) {
        this.generalReader = generalReader;
        this.keyword = keyword;
    }

    public JSONObject read(long position) {
        return generalReader.read(buildIndex(position));
    }

    public JSONObject readFromSelection(long position, List<Transaction> selection) {
        return generalReader.readFromSelection( buildIndex(position), selection);
    }

    public String buildAddress(long position) {
        return generalReader.buildAddress(buildIndex(position));
    }

    private IAMIndex buildIndex(long position) {
        return new IAMIndex(keyword, position);
    }
}