package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by nsa on 10/24/16.
 */

/**
 *
 * 1. We cannot lookup address balances with Bitcoin Core easily, there is no address index.
 *
 * 2. So, the users know each other's Bitcoin addresses - and send each other the UTXOs they want to
 *    spend from.
 *
 */

// TODO
// Setup instructions

public class BitcoinCore extends Bitcoin {

    private BtcdQueryClient client;

    public BitcoinCore(NetworkParameters netParams, String rpcuser, String rpcpass) throws MalformedURLException, BitcoindException, CommunicationException {
        // 0 because we connect to peers via BitcoinCore
        super(netParams, 0);

        Integer rpcport;

        // regtest port?
        if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
            rpcport = 8332;
        } else if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            rpcport = 18332;
        } else {
            throw new IllegalArgumentException("Invalid network parameters passed to Bitcoin Core. ");
        }

        client = new BtcdQueryClient("127.0.0.1", rpcport, rpcuser, rpcpass);
    }

    synchronized boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException {
        return client.getTxOut(transactionHash, vout, false);
    }

    public class TransactionWithConfirmations extends org.bitcoinj.core.Transaction {

        byte[] bytes;
        boolean confirmed;

        public TransactionWithConfirmations(NetworkParameters netParams, byte[] bytes, boolean confirmed) {
            super(netParams, bytes);
            this.bytes = bytes;
            this.confirmed = confirmed;
        }

        public boolean getConfirmed() {
            return this.confirmed;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

    }

    synchronized TransactionWithConfirmations getTransaction(String transactionHash) throws IOException {

        RawTransaction rx;
        Object rawObject;

        try {
            rawObject = client.getRawTransaction(transactionHash, 1);
        } catch(BitcoindException | CommunicationException e) {
            return null;
        }

        rx = (RawTransaction) rawObject;

        // only confirmed transactions for now
        int confirmations = rx.getConfirmations();
        boolean confirmed;
        if (confirmations == 0) {
            confirmed = false;
        } else {
            confirmed = true;
        }

        String rawHex = rx.getHex();
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(rawHex);
        TransactionWithConfirmations tx = new TransactionWithConfirmations(netParams, bytearray, confirmed);

        return tx;
    }

    public synchronized List<Transaction> getTransactionsFromUtxosInner(HashSet<TransactionOutPoint> t) {

        List<Transaction> txList = new ArrayList<>();
        HashSet<Transaction> checkDuplicateTx = new HashSet<>();
        for (TransactionOutPoint tO : t) {
            TransactionWithConfirmations tx;
            try {
                tx = getTransaction(tO.getHash().toString());
                String txid = tx.getHash().toString();
                byte[] bytes = tx.getBytes();
                boolean confirmed = tx.getConfirmed();
                org.bitcoinj.core.Transaction bitTx = new org.bitcoinj.core.Transaction(netParams, bytes);
                Transaction bTx = new Transaction(txid, bitTx, false, confirmed);
                if (!checkDuplicateTx.contains(bTx)) {
                    txList.add(bTx);
                }
                checkDuplicateTx.add(bTx);
            } catch (IOException e) {
                return null;
            }
        }

        return txList;
    }


    // TODO
    // How does this get called ?
    @Override
    protected boolean send(Bitcoin.Transaction t) {
        if (!t.canSend || t.sent) {
            return false;
        }

        String hexTx = null;
        try {
            hexTx = DatatypeConverter.printHexBinary(t.bitcoinj().bitcoinSerialize());
        } catch (BlockStoreException e) {
            return false;
        } catch (IOException er) {
            return false;
        }

        String txid;
        try {
            txid = client.sendRawTransaction(hexTx);
        } catch (BitcoindException | CommunicationException e) {
            return false;
        }

        if (txid == null) return false;

        return true;
    }

    @Override
    protected synchronized List<Transaction> getTransactionsFromUtxos(HashSet<TransactionOutPoint> t) {
        return getTransactionsFromUtxosInner(t);
    }

}
