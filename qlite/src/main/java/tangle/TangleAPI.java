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

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author microhash
 *
 * The TangleAPI is the interface between the QLite library and the
 * IotaAPI of the iota library. It takes care of all tangle requests.
 * */
public class TangleAPI {

    private static TangleAPI instance = new TangleAPI("https", "nodes.devnet.iota.org", "443", 9, true);

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
     * @param mwm      min weight magnitude (14 on mainnet, 9 on testnet)
     * @param localPow TRUE: perform proof-of-work locally, FALSE: perform pow on remote iota node
     * */
    public static void changeNode(String protocol, String host, String port, int mwm, boolean localPow) {
        instance = new TangleAPI(protocol, host, port, mwm, localPow);
    }

    /**
     * Creates a new IotaAPI with local proof-of-work enabled.
     * @param protocol protocol of node api address (http/https)
     * @param host     host name of node api address (e.g. node.example.org)
     * @param port     port of node api (e.g. 14265 or 443)
     * @param mwm      min weight magnitude (14 for mainnet, 9 for testnet)
     * @param localPow TRUE: perform pow locally, FALSE: outsource pow to remote node
     * */
    private TangleAPI(String protocol, String host, String port, int mwm, boolean localPow) {

        IotaAPI.Builder b = new IotaAPI.Builder()
                .protocol(protocol)
                .host(host)
                .port(port);

        if(localPow)
            b = b.localPoW(new PearlDiverLocalPoW());

        api = b.build();
        this.mwm = mwm;
    }

    /**
     * Sends a data transaction to the tangle. Keeps trying until there is no error.
     * @param address the address to which the transaction shall be attached
     * @param tryteMessage the transaction message (in trytes)
     * @return transaction hash of sent transaction
     * */
    public String sendTrytes(String address, String tryteMessage) {

        LinkedList<Input> inputs = new LinkedList<>();
        LinkedList<Transfer> transfers = new LinkedList<>();
        transfers.add(new Transfer(address, 0, tryteMessage, TAG));

        while (true) {
            try {
                SendTransferResponse response = api.sendTransfer("", 1, 3, mwm, transfers, inputs, "", true, false);
                return response.getTransactions().get(0).getHash();
            } catch (ArgumentException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                StackTraceElement ste = e.getStackTrace()[0];
                System.err.println("NullPointerException in file " + ste.getFileName() + " at line #" + ste.getLineNumber());
            }

            try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public String sendTrytes(String tryteMessage) {
        return sendTrytes(TryteTool.NINE_ADDRESS, tryteMessage);
    }

    public String sendMessage(String message) {
        return sendMessage(TryteTool.NINE_ADDRESS, message);
    }

    public String sendMessage(String address, String message) {
        return sendTrytes(address, TrytesConverter.toTrytes(message));
    }

    /**
     * Finds all transactions published to a certain address.
     * @param addresses the addresses to check
     * @return hashes of found transactions
     * */
    public List<Transaction> findTransactionsByAddresses(String[] addresses) {

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

        return transactions;
    }

    /**
     * Reads the messages of all transaction published to a certain address.
     * @param preload resource of pre-fetched transactions for efficiency purposes, optional (set to null if not required)
     * @param address the address to check
     * @param convert convert the message trytes to ascii before returning?
     * @return transaction messages mapped by transaction hash of all transactions found
     * */
    public Map<String, String> readTransactionsByAddress(List<Transaction> preload, String address, boolean convert) {
        List<Transaction> transactions;
        if(preload != null) {
            transactions = new LinkedList<>();
            for(Transaction t : preload)
                if(t.getAddress().equals(address))
                    transactions.add(t);
        } else {
            transactions = findTransactionsByAddresses(new String[] {address});
        }

        Map<String, String> map = new HashMap<>();

        for(Transaction tx : transactions) {
            String trytes = tx.getSignatureFragments();
            trytes = trytes.split("99")[0];
            if(trytes.length()%2 == 1) trytes += "9";
            String message = convert ? TrytesConverter.toString(trytes) : trytes;

            map.put(tx.getHash(), message);
        }
        return map;
    }

    public String readTransactionMessage(String hash) {
        return TrytesConverter.toString(readTransactionTrytes(hash));
    }

    /**
     * Finds the transaction with a certain hash.
     * @param hash    the hash of the requested transaction
     * @return transaction messages of the transaction found, NULL if not found
     * */
    public String readTransactionTrytes(String hash) {

        if(!TryteTool.isTryteSequence(hash))
            throw new InvalidParameterException("parameter hash is not a tryte sequence");
        if(hash.length() != 81)
            throw new InvalidParameterException("parameter hash is required to be exactly 81 trytes long");

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

        // transaction not found
        if(transactions.get(0).getHash().equals("999999999999999999999999999999999999999999999999999999999999999999999999999999999"))
            return null;

        String trytes = transactions.get(0).getSignatureFragments();
        // remove end
        trytes = trytes.substring(0, trytes.length()-1);
        trytes = trytes.split("99")[0];
        if(trytes.length()%2 == 1) trytes += "9";

        return trytes;
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

    public String getNodeAddress() {
        return api.getProtocol() + "://" + api.getHost() + ":" + api.getPort();
    }
}