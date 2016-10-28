package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;
import com.neemre.btcdcli4j.core.domain.Transaction;

import org.bitcoinj.core.NetworkParameters;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

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

    public BitcoinCore(NetworkParameters netParams, String rpcuser, String rpcpass) throws MalformedURLException {
        super(netParams, 0);
        try {
            Integer rpcport = 8332;
            if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
                rpcport = 18332;
            }
            client = new BtcdQueryClient("127.0.0.1", rpcport, rpcuser, rpcpass);
        } catch (BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }
    }

    synchronized void utxo() throws IOException, BitcoindException, CommunicationException {
        client.getTxOut();
    }
    //

    synchronized boolean getUtxo(String transactionHash) throws IOException, BitcoindException, CommunicationException {
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(client.getRawTransaction(transactionHash));
        org.bitcoinj.core.Transaction transactionj = new org.bitcoinj.core.Transaction(netParams, bytearray);
        System.out.println(transactionj.isAnyOutputSpent());
        System.out.println(transactionj.getOutput(0).getSpentBy());
        return false;
    }
    //

    synchronized boolean isUtxo(String transactionHash) throws IOException {
        org.bitcoinj.core.Transaction transactionj = getTransaction(transactionHash);
        return transactionj.isAnyOutputSpent();
    }

    // TODO UTXO Set (check address and vout ?)
    synchronized org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {
        com.neemre.btcdcli4j.core.domain.Transaction transaction = null;
        try {
            transaction = client.getTransaction(transactionHash);
            System.out.println(transaction.getAmount());
        } catch (BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(transaction.getHex());
        org.bitcoinj.core.Transaction transactionj = new org.bitcoinj.core.Transaction(netParams, bytearray);
        return transactionj;
    }

    // TODO ?
    public synchronized List<Transaction> getAddressTransactionsInner(String address) {
        //get address

        //get all txs of address

        //check all tx if confirmed

        return null;

    }

    @Override
    protected boolean send(Bitcoin.Transaction t) {
        return false;
    }

    @Override
    protected synchronized List<Transaction> getAddressTransactions(String address) {
        return getAddressTransactionsInner(address);
    }

}
