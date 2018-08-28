package oracle;

import oracle.statements.result.ResultStatement;
import org.junit.Test;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;

import static org.junit.Assert.*;

public class OracleReaderTest {

    @Test
    public void testResultStatement() {

        final int position = 7;
        final String code = "return({'epoch': epoch});", expected = "{'epoch': "+position+"}";

        QubicWriter qubicWriter = createQubicWriterWithPublishedQubicTransaction(code);
        OracleWriter oracleWriter = createOracleAndPublishResultStatement(qubicWriter, position);
        ResultStatement resultStatement = readResultStatement(oracleWriter.getID(), position);

        String assetMessage = "qubic ID: " + qubicWriter.getID() + ", oracle ID" + oracleWriter.getID();
        assertEquals(assetMessage, expected, resultStatement.getContent());
    }

    private static QubicWriter createQubicWriterWithPublishedQubicTransaction(String code) {
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification eqs =  qubicWriter.getEditable();
        eqs.setCode(code);
        qubicWriter.publishQubicTransaction();
        return qubicWriter;
    }

    private static OracleWriter createOracleAndPublishResultStatement(QubicWriter qubicWriter, int position) {
        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
        OracleWriter oracleWriter = new OracleWriter(qubicReader);
        qubicWriter.getAssembly().add(oracleWriter.getID());
        qubicWriter.publishAssemblyTransaction();
        oracleWriter.doHashStatement(position);
        oracleWriter.doResultStatement();
        return oracleWriter;
    }

    private static ResultStatement readResultStatement(String oracleID, int position) {
        OracleReader oracleReader = new OracleReader(oracleID);
        oracleReader.getHashStatementReader().read(position);
        return oracleReader.getResultStatementReader().read(position);
    }
}