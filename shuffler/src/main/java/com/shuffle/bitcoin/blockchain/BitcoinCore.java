package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.client.BtcdClientImpl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.bitcoinj.core.NetworkParameters;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

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

    private BtcdClient client;

    public BitcoinCore(NetworkParameters netParams, String rpcuser, String rpcpass) throws MalformedURLException {
        super(netParams, 0);
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        Properties nodeConfig = new Properties();
        try {
            InputStream is = new BufferedInputStream(new FileInputStream("config/node_config.properties"));
            nodeConfig.load(is);
            is.close();
            if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
                nodeConfig.setProperty("node.bitcoind.rpc.port", "18332");
            }
            nodeConfig.setProperty("node.bitcoind.rpc.user", rpcuser);
            nodeConfig.setProperty("node.bitcoind.rpc.password", rpcpass);
            client = new BtcdClientImpl(httpClient, nodeConfig);
        } catch (IOException | BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }
    }

    synchronized boolean isUtxo(String transactionHash) throws IOException {
        String requestBody = "{}";
        return false;
    }

    // TODO UTXO Set (check address and vout ?)
    synchronized org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {
        com.neemre.btcdcli4j.core.domain.Transaction transaction = null;
        try {
            transaction = client.getTransaction(transactionHash);
        } catch (BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }
        return null;
    }

    synchronized com.neemre.btcdcli4j.core.domain.Transaction getTransactionViaClient(String transactionHash) throws IOException {
        com.neemre.btcdcli4j.core.domain.Transaction transaction = new com.neemre.btcdcli4j.core.domain.Transaction();
        try {
            System.out.println(client.getTransaction(transactionHash).getBlockHash());
            transaction.setHex(client.getTransaction(transactionHash).getHex());
            return transaction;
        } catch (BitcoindException | CommunicationException e) {
            e.printStackTrace();
        }
        return transaction;
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
