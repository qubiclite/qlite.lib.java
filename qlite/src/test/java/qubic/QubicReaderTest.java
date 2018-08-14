package qubic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QubicReaderTest {

    @Test
    public void testReadQubicTransaction() {

        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification eqs = qubicWriter.getEditable();

        final int runtimeLimit = 9;
        final int hashPeriodDuration = 17;
        final String code = "return(33);";

        eqs.setRuntimeLimit(runtimeLimit);
        eqs.setHashPeriodDuration(hashPeriodDuration);
        eqs.setCode(code);

        qubicWriter.publishQubicTransaction();
        String assertMessage = "qubic transaction hash: " + qubicWriter.getQubicTransactionHash();
        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
        QubicSpecification read_qs = qubicReader.getSpecification();

        assertEquals(assertMessage, runtimeLimit, read_qs.getRuntimeLimit());
        assertEquals(assertMessage, hashPeriodDuration, read_qs.getHashPeriodDuration());
        assertEquals(assertMessage, code, read_qs.getCode());
    }
}