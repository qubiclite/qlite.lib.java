package tangle;

import exceptions.IotaAPICallFailedException;
import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;

import java.util.LinkedList;
import java.util.List;

public class WrappedIotaAPI extends IotaAPI {

    private final int throwableTolerance;

    WrappedIotaAPI(IotaAPI.Builder builder, int throwableTolerance) {
        super(builder);
        this.throwableTolerance = throwableTolerance;
    }

    @Override
    public List<Transaction> findTransactionsObjectsByHashes(String[] hashes) {
        List<Transaction> transactionObjects = null;
        for(int i = 0; i < throwableTolerance && transactionObjects == null; i++)
            transactionObjects = tryToFindTransactionsObjectsByHashes(hashes, i == throwableTolerance-1);
        return transactionObjects;
    }

    private List<Transaction> tryToFindTransactionsObjectsByHashes(String[] hashes, boolean isLastTry) {
        try {
            return super.findTransactionsObjectsByHashes(hashes);
        } catch (Throwable t) {
            if(isLastTry)
                logThrowable(t);
            return new LinkedList<>();
        }
    }

    @Override
    public List<Transaction> findTransactionObjectsByAddresses(String[] addresses) {
        List<Transaction> transactionObjects = null;
        for(int i = 0; i < throwableTolerance && transactionObjects == null; i++)
            transactionObjects = tryToFindTransactionsObjectsByAddresses(addresses,i == throwableTolerance-1);
        return transactionObjects;
    }

    private List<Transaction> tryToFindTransactionsObjectsByAddresses(String[] addresses, boolean isLastTry) {
        try {
            return super.findTransactionObjectsByAddresses(addresses);
        } catch (Throwable t) {
            if(isLastTry)
                logThrowable(t);
            return new LinkedList<>();
        }
    }

    @Override
    public SendTransferResponse sendTransfer(String seed, int security, int depth, int minWeightMagnitude, List<Transfer> transfers, List<Input> inputs, String remainderAddress, boolean validateInputs, boolean validateInputAddresses) throws ArgumentException {
        SendTransferResponse sendTransferResponse = null;
        for(int i = 0; i < throwableTolerance && sendTransferResponse == null; i++)
            sendTransferResponse = tryToSendTransfer(seed, security, depth, minWeightMagnitude, transfers, inputs, remainderAddress, validateInputs, validateInputAddresses, i == throwableTolerance-1);
        return sendTransferResponse;
    }

    private SendTransferResponse tryToSendTransfer(String seed, int security, int depth, int minWeightMagnitude, List<Transfer> transfers, List<Input> inputs, String remainderAddress, boolean validateInputs, boolean validateInputAddresses, boolean isLastTry) {
        try {
            return super.sendTransfer(seed, security, depth, minWeightMagnitude, transfers, inputs, remainderAddress, validateInputs, validateInputAddresses);
        } catch (Throwable t) {
            if(isLastTry) {
                logThrowable(t);
                throw new IotaAPICallFailedException(t);
            }
            return null;
        }
    }

    private void logThrowable(Throwable t) {

    }
}
