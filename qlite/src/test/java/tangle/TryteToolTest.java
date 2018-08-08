package tangle;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class TryteToolTest {

    private static final String TRYTE_REGEX = "^[A-Z9]*$";

    /**
     * Encodes and decodes a random byte array. Then checks that the original
     * and the docoded bytes are equal.
     * */
    @Test
    public void testEncodeDecode() {

        byte[] originalBytes = genRandByteArray();

        String trytes = TryteTool.bytesToTrytes(originalBytes);
        byte[] decodedBytes = TryteTool.trytesToBytes(trytes);

        assertArrayEquals(originalBytes, decodedBytes);
    }

    /**
     * Tests that the tryte encoded bytes are indeed a tryte sequence.
     * */
    @Test
    public void testEncodedOnTrytes() {

        byte[] bytes = genRandByteArray();
        String trytes = TryteTool.bytesToTrytes(bytes);

        try {
            assertTrue(trytes.matches(TRYTE_REGEX));
        } catch (AssertionError e) {
            System.out.println("no tryte sequence: '"+trytes+"'");
            System.out.println("byte array:         "+Arrays.toString(bytes));
            throw e;
        }
    }

    /**
     * @return random byte array of random length (1000-1020 bytes)
     * */
    private byte[] genRandByteArray() {
        byte[] randByteArray = new byte[1000 + (int)(Math.random() * 20)];
        new Random().nextBytes(randByteArray);
        return randByteArray;
    }
}
