/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.json.JSONTokener;
import org.json.JSONObject;


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

    /**
     *
     * Given a wallet address, this function looks up the address' balance using Blockchain.info's
     * API. The amount returned is of type long and represents the number of satoshis.
     */
    public long getAddressBalance(String address) throws IOException {
        String url = "https://blockchain.info/rawaddr/" + address;
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        return Long.valueOf(root.get("final_balance").toString());
    }


    /**
     *
     * Given a wallet address, this function looks up all transactions associated with the wallet
     * using Blockchain.info's API. These "n" transaction hashes are then returned in a String
     * array.
     *
     */
    public List<Transaction> getAddressTransactions(String address) throws IOException {

        String url = "https://blockchain.info/rawaddr/" + address;
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        List<Transaction> txhashes = new LinkedList<>();
        for (int i = 0; i < root.getJSONArray("txs").length(); i++) {
            boolean confirmed;
            String blockHeight = root.getJSONArray("txs").getJSONObject(i).get("block_height").toString();
            if (blockHeight == null) {
                confirmed = false;
            } else {
                confirmed = true;
            }
            txhashes.add(new Transaction(
                    root.getJSONArray("txs").getJSONObject(i).get("hash").toString(), false, confirmed));
        }
        if (txhashes.size() == 50) {
            return null;
        }
        return txhashes;

    }

    /**
     *
     * This function takes in a transaction hash and passes it to Blockchain.info's API.
     * After some formatting, it returns a bitcoinj Transaction object using this transaction hash.
     *
     */
    public org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {

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
        // bitcoinj needs this Context variable
        Context context = Context.getOrCreate(netParams);
        return new org.bitcoinj.core.Transaction(netParams, bytearray);

    }

}
