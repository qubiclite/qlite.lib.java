package iam;

import org.json.JSONObject;

public class IAMKeywordWriter {

    private final String keyword;
    private final IAMWriter generalWriter;

    public IAMKeywordWriter(IAMWriter generalWriter, String keyword) {
        this.generalWriter = generalWriter;
        this.keyword = keyword;
    }

    public void publish(long position, JSONObject message) {
        generalWriter.write(new IAMIndex(keyword, position), message);
    }
}