package iam;

import org.json.JSONObject;
import tangle.TryteTool;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;

public class IAMWriterTest {

    private final IAMWriter iamWriter = new IAMWriter();

    private int[][] generateTestTableForPredictAmountOfFragments() throws NoSuchFieldException, IllegalAccessException {
        int MAX_CHARS_PER_FRAGMENT;

        Field maxCharsPerFragmentField = IAMWriter.class.getDeclaredField("MAX_CHARS_PER_FRAGMENT");
        maxCharsPerFragmentField.setAccessible(true);
        MAX_CHARS_PER_FRAGMENT = maxCharsPerFragmentField.getInt(null);

        return new int[][]{
                {0, 0},
                {MAX_CHARS_PER_FRAGMENT, 1},
                {MAX_CHARS_PER_FRAGMENT+1, 2},
                {MAX_CHARS_PER_FRAGMENT*2-TryteTool.TRYTES_PER_HASH, 2},
                {MAX_CHARS_PER_FRAGMENT*2-TryteTool.TRYTES_PER_HASH+1, 3},
                {MAX_CHARS_PER_FRAGMENT*3-TryteTool.TRYTES_PER_HASH*2, 3},
                {MAX_CHARS_PER_FRAGMENT*3-TryteTool.TRYTES_PER_HASH*2+1, 4},
        };
    }

    @Test
    public void testPredictAmountOfFragments() throws InvocationTargetException, NoSuchMethodException, NoSuchFieldException, IllegalAccessException {

        int[][] testTable = generateTestTableForPredictAmountOfFragments();

        Method predictAmountOfFragments = IAMWriter.class.getDeclaredMethod("predictAmountOfFragments", Integer.TYPE);
        predictAmountOfFragments.setAccessible(true);

        for(int[] testRow : testTable) {
            int prediction = (int)predictAmountOfFragments.invoke(null, testRow[0]);
            assertEquals("content length: " + testRow[0], testRow[1], prediction);
        }
    }

    @Test(expected = InvalidParameterException.class)
    public void testNonAsciiMessage() {
        JSONObject message = new JSONObject();
        message.put("no ascii", "ä");
        iamWriter.publish(new IAMIndex(0), message);
    }
}