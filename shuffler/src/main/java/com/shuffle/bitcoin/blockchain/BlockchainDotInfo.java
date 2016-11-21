/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;


/**
 *
 * Created by Eugene Siegel on 3/4/16.
 *
 */

public final class BlockchainDotInfo extends Bitcoin {

    String userAgent = "Chrome/5.0";

    /**
     * The constructor takes in a NetworkParameters variable that determines whether we are
     * connecting to the Production Net or the Test Net.  It also takes in an int which
     * determines the minimum number of peers to connect to before broadcasting a transaction.
     *
     */

    public BlockchainDotInfo(NetworkParameters netParams, int minPeers) {
        super(netParams, minPeers);
        if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
            throw new IllegalArgumentException();
        }
    }


    synchronized boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException {
        // https://blockchain.info/rawtx/$tx_hash
        String url = "https://blockchain.info/rawtx/" + transactionHash;
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        org.json.JSONArray outputs = root.getJSONArray("out");
        JSONObject output = (JSONObject) outputs.get(vout);
        return !output.getBoolean("spent");
    }

    /**
     *
     * Given a wallet address, this function looks up the address' balance using Blockchain.info's
     * API. The amount returned is of type long and represents the number of satoshis.
     */

    // TODO
    // remove?
    public synchronized long getAddressBalance(String address) throws IOException {
        String url = "https://blockchain.info/rawaddr/" + address;
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        return Long.valueOf(root.get("final_balance").toString());
    }


    /**
     *
     * Given a set or TransactionOutpoints, this function looks up all transactions associated with the wallet
     * using Blockchain.info's API. These "n" transaction hashes are then returned in a String
     * array.
     *
     */
    protected final List<Transaction> getTransactionsFromUtxosInner(HashSet<TransactionOutPoint> t) throws IOException {

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

    /**
     *
     * This function takes in a transaction hash and passes it to Blockchain.info's API.
     * After some formatting, it returns a bitcoinj Transaction object using this transaction hash.
     *
     */
    public synchronized TransactionWithConfirmations getTransaction(String transactionHash) throws IOException {

        String url = "https://blockchain.info/tr/rawtx/" + transactionHash + "?format=hex";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", userAgent);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(response.toString());

        url = "https://blockchain.info/rawtx/" + transactionHash;
        obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        if (root.has("block_height")) {
            return new TransactionWithConfirmations(netParams, bytearray, true);
        }
        // bitcoinj needs this Context variable
        Context context = Context.getOrCreate(netParams);
        return new TransactionWithConfirmations(netParams, bytearray, false);

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
