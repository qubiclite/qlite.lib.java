package tangle;

import cfb.pearldiver.PearlDiverLocalPoW;
import jota.IotaAPI;
import jota.dto.response.GetBalancesResponse;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.TrytesConverter;

import java.util.LinkedList;
import java.util.List;

/**
 * @author microhash
 *
 * The TangleAPI is the interface between the QLite library and the
 * IotaAPI of the iota library. It takes care of all tangle requests.
 * */
public class TangleAPI {

    private static TangleAPI instance = new TangleAPI("https", "nodes.testnet.iota.org", "443", 9);

    private static final String TAG = "QLITE9999999999999999999999";

    private final IotaAPI api;
    private int mwm;

    public static TangleAPI getInstance() {
        return instance;
    }

    /**
     * Changes the node used.
     * @param protocol protocol of node api address (http/https)
     * @param host     host name of node api address (e.g. node.example.org)
     * @param port     port of node api (e.g. 14265 or 443)
     * @param mwm      min weight magnitude (14 for mainnet, 9 for testnet)
     * */
    public static void changeNode(String protocol, String host, String port, int mwm) {
        instance = new TangleAPI(protocol, host, port, mwm);
    }

    /**
     * Creates a new IotaAPI with local proof-of-work enabled.
     * @param protocol protocol of node api address (http/https)
     * @param host     host name of node api address (e.g. node.example.org)
     * @param port     port of node api (e.g. 14265 or 443)
     * @param mwm      min weight magnitude (14 for mainnet, 9 for testnet)
     * */
    private TangleAPI(String protocol, String host, String port, int mwm) {
        api = new IotaAPI.Builder()
                .protocol(protocol)
                .host(host)
                .port(port)
                .localPoW(new PearlDiverLocalPoW())
                .build();
        this.mwm = mwm;
    }

    /**
     * Sends a data transaction to the tangle. Keeps trying until there is no error.
     * @param address the address to which the transaction shall be attached
     * @param data    the data included in the transaction
     * @param convert indicates the encoding of parameter 'data':
     *                TRUE: data is ASCII string -> convert to trytes
     *                FALSE: data is already encoded in trytes -> publish raw
     * @return list of transaction objects returned from iota library (contains hashes etc)
     * */
    public List<Transaction> sendTransfer(String address, String data, boolean convert) {

        LinkedList<Input> inputs = new LinkedList<>();
        LinkedList<Transfer> transfers = new LinkedList<>();
        String message = convert ? TrytesConverter.toTrytes(data) : data;
        transfers.add(new Transfer(address, 0, message, TAG));

        while (true) {
            try {
                SendTransferResponse str = api.sendTransfer("", 1, 3, mwm, transfers, inputs, "", true, false);
                return str.getTransactions();
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }

            // bruh chill a sec b4 trying again
            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    /**
     * Finds all transaction published to a certain address.
     * @param address the address to check
     * @param convert convert the message trytes to ascii before returning?
     * @return transaction messages of all transactions found
     * */
    public String[] findTransactionsByAddress(String address, boolean convert) {
        String[] addresses = {address};
        List<Transaction> transactions = null;

        while (transactions == null) {
            try {
                transactions = api.findTransactionObjectsByAddresses(addresses);
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }
        }

        String[] transactionData = new String[transactions.size()];
        for(int i = 0; i < transactions.size(); i++) {
            String trytes = transactions.get(i).getSignatureFragments();
            trytes = trytes.split("99")[0];
            if(trytes.length()%2 == 1) trytes += "9";
            transactionData[i] = convert ? TrytesConverter.toString(trytes) : trytes;
        }
        return transactionData;
    }

    /**
     * Finds the transaction with a certain hash
     * @param hash    the hash of the requested transaction
     * @param convert convert the message trytes to ascii before returning?
     * @return transaction messages of the transaction found
     * */
    public String findTransactionByHash(String hash, boolean convert) {
        String[] hashes = {hash};
        List<Transaction> transactions = null;

        while (transactions == null) {
            try {
                transactions = api.findTransactionsObjectsByHashes(hashes);
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }
        }

        String trytes = transactions.get(0).getSignatureFragments();
        // remove end
        trytes = trytes.substring(0, trytes.length()-1);
        trytes = trytes.split("99")[0];
        if(trytes.length()%2 == 1) trytes += "9";

        return convert ? TrytesConverter.toString(trytes) : trytes;
    }

    /**
     * Requests the balance of a certain iota address.
     * @param address the address to check
     * @return the balance in iotas
     * */
    public long getBalance(String address) {
        LinkedList<String> addresses = new LinkedList<>();
        addresses.add(address);
        try {
            GetBalancesResponse balancesResponse = api.getBalances(1, addresses);
            return Long.parseLong(balancesResponse.getBalances()[0]);
        }
        catch (ArgumentException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getMWM() {
        return mwm;
    }
}