package oracle;

import oracle.statements.*;
import org.junit.Test;
import qubic.EditableQubicSpecification;
import qubic.QubicReader;
import qubic.QubicWriter;

import static org.junit.Assert.*;

public class OracleReaderTest {

    @Test
    public void readStatement() {
        QubicWriter qubicWriter = new QubicWriter();
        EditableQubicSpecification eqs =  qubicWriter.getEditable();
        eqs.setCode("return({'epoch': epoch});");
        qubicWriter.publishQubicTransaction();

        QubicReader qubicReader = new QubicReader(qubicWriter.getID());
        OracleWriter oracleWriter = new OracleWriter(qubicReader);

        qubicWriter.getAssembly().add(oracleWriter.getID());
        qubicWriter.publishAssemblyTransaction();

        oracleWriter.doHashStatement(3);
        oracleWriter.doResultStatement();

        OracleReader oracleReader = new OracleReader(oracleWriter.getID());
        oracleReader.readStatement(null, new HashStatementIAMIndex(3));
        ResultStatement resultStatement = (ResultStatement)oracleReader.readStatement(null, new ResultStatementIAMIndex(3));

        String assetMessage = "qubic ID: " + qubicWriter.getID() + ", oracle ID" + oracleWriter.getID();

        assertEquals(assetMessage, "{'epoch': 3}", resultStatement.getContent());
    }
}