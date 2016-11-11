package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.impl.TransactionHash;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nsa on 10/24/16.
 */

/**
 *
 * 1. We cannot lookup address balances with Bitcoin Core easily, there is no address index.
 *
 * 2. So, the users know each other's Bitcoin addresses - and send each other the UTXO they want to
 *    send from.
 *
 */

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

    // TODO -- VERIFY {address, vout, & don't need mempool}
    synchronized boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException {
        return client.getTxOut(transactionHash, vout, false);
    }

    @Override
    protected List<Transaction> getAddressTransactionsInner(String transactionHash, Long vout) throws IOException, CoinNetworkException, AddressFormatException {
        TransactionHash txHash = new TransactionHash(transactionHash);
        org.bitcoinj.core.Address address = getTransaction(txHash).getOutput(vout).getAddressFromP2PKHScript(netParams);
        return getAddressTransactions(address.toString());
    }

    synchronized TransactionWithConfirmations getTransaction(TransactionHash transactionHash) throws IOException {
        JSONObject txObject = null;
        try {
            txObject = new JSONObject(client.getRawTransaction(transactionHash.toString(), 1));
        } catch (BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }

        boolean confirmed;
        confirmed = txObject.has("confirmations");

        String rawHex = txObject.getString("hex");
        TransactionWithConfirmations tx = new TransactionWithConfirmations(netParams, Hex.decode(rawHex), confirmed);

        return tx;
    }

    // Don't need
    public synchronized List<Transaction> getAddressTransactionsInner(String address) {
        return new LinkedList<>();
    }

    @Override
    protected boolean send(Bitcoin.Transaction t) {
        // send transaction with Bitcoin Core
        return false;
    }

    // Don't need
    protected synchronized List<Transaction> getAddressTransactions(String address) {
        return getAddressTransactionsInner(null);
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

}
