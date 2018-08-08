package qubic;

import static org.junit.Assert.*;

import org.junit.Test;

public class QubicWriterTest {

    @Test(expected = IllegalStateException.class)
    public void pretendDoubleQubicTransaction() {
        QubicWriter qwriter = new QubicWriter();
        try {
            qwriter.publishQubicTransaction();
        } catch (IllegalStateException e) {
            fail("this first qubic transaction should have worked");
        }
        qwriter.publishQubicTransaction();
    }
}