package iam;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;


import static org.junit.Assert.*;

public class IAMReaderTest {

    private final IAMWriter iamWriter = new IAMWriter();
    private final IAMReader iamReader = new IAMReader(iamWriter.getID());

    @Test
    public void testPublishAndRead() {
        testPublishAndReadObject(0, null);
        testPublishAndReadObject(1, "");
        testPublishAndReadObject(2, 4);
        testPublishAndReadObject(3, new JSONArray("[{'age': 20}, 'some string']"));
        testPublishAndReadObject(4, StringUtils.repeat("ok!\"9\'$3", 200));
    }

    private void testPublishAndReadObject(int position, Object object) {
        IAMIndex index = new IAMIndex(position);
        JSONObject sent = new JSONObject();
        sent.put("object", object);
        String hash = iamWriter.publish(index, sent);
        JSONObject read = iamReader.read(index);
        assertEquals("hash of failed: " + hash, String.valueOf(sent), String.valueOf(read));
    }

    @Test
    public void multiPublish() {
        JSONObject message1 = new JSONObject();
        message1.put("planet", "mars");

        JSONObject message2 = new JSONObject();
        message2.put("planet", "venus");

        iamWriter.publish(new IAMIndex(100), message1);
        assertNotNull(iamReader.read(new IAMIndex(100)));

        iamWriter.publish(new IAMIndex(100), message1);
        assertNotNull(iamReader.read(new IAMIndex(100)));

        iamWriter.publish(new IAMIndex(100), message2);
        assertNull(iamReader.read(new IAMIndex(100)));
    }
}