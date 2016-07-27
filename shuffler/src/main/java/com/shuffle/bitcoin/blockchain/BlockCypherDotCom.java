/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin.blockchain;

import com.google.common.util.concurrent.RateLimiter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;


/**
 *
 * Created by Eugene Siegel on 3/4/16.
 *
 */

public final class BlockCypherDotCom extends Bitcoin {

    /**
     * The constructor takes in a NetworkParameters variable that determines whether we are
     * connecting to the Production Net or the Test Net.  It also takes in an int which
     * determines the minimum number of peers to connect to before broadcasting a transaction.
     *
     */

    final RateLimiter rateLimiter = RateLimiter.create(3.0);

    public BlockCypherDotCom(NetworkParameters netParams, int minPeers) {
        super(netParams, minPeers);
    }

    /**
     *
     * Given a wallet address, this function looks up the address' balance using Blockchain.info's
     * API. The amount returned is of type long and represents the number of satoshis.
     */
    public long getAddressBalance(String address) throws IOException {
       rateLimiter.acquire();
       try {
            if (Address.getParametersFromAddress(address)==NetworkParameters.fromID(NetworkParameters.ID_TESTNET)) {
               rateLimiter.acquire();
                String url = "https://api.blockcypher.com/v1/btc/test3/addrs/" + address;
                URL obj = new URL(url);
               JSONTokener tokener;
               tokener = new JSONTokener(obj.openStream());
               JSONObject root = new JSONObject(tokener);
                return Long.valueOf(root.get("final_balance").toString());
            }
            //if not testnet likely mainnet
            else{
               rateLimiter.acquire();
                String url = "https://api.blockcypher.com/v1/btc/main/addrs/" + address;
                URL obj = new URL(url);
                JSONTokener tokener = new JSONTokener(obj.openStream());
                JSONObject root = new JSONObject(tokener);
                return Long.valueOf(root.get("final_balance").toString());
            }


        } catch (AddressFormatException e) {
            e.printStackTrace();
        }

        return 0;
    }


    /**
     *
     * Given a wallet address, this function looks up all transactions associated with the wallet
     * using Blockchain.info's API. These "n" transaction hashes are then returned in a String
     * array.
     *
     */
    public List<Transaction> getAddressTransactions(String address) throws IOException {
        try {
            if (Address.getParametersFromAddress(address)==NetworkParameters.fromID(NetworkParameters.ID_TESTNET)) {
               rateLimiter.acquire();
                String url = "https://api.blockcypher.com/v1/btc/test3/addrs/" + address;
                URL obj = new URL(url);
                JSONTokener tokener = new JSONTokener(obj.openStream());
                JSONObject root = new JSONObject(tokener);
                List<Transaction> txhashes = new LinkedList<>();
                for (int i = 0; i < root.getJSONArray("txrefs").length(); i++) {
                    boolean confirmed;
                    String blockHeight = root.getJSONArray("txrefs").getJSONObject(i).get("block_height").toString();
                    confirmed = blockHeight != null;
                    txhashes.add(new Transaction(
                          root.getJSONArray("txrefs").getJSONObject(i).get("tx_hash").toString(), false, confirmed));
                }
                if (txhashes.size() == 50) {
                    return null;
                }
                return txhashes;
            }
            else {
               rateLimiter.acquire();
                String url = "https://api.blockcypher.com/v1/btc/main/addrs/" + address;
                URL obj = new URL(url);
                JSONTokener tokener = new JSONTokener(obj.openStream());
                JSONObject root = new JSONObject(tokener);
                List<Transaction> txhashes = new LinkedList<>();
                for (int i = 0; i < root.getJSONArray("txs").length(); i++) {
                    boolean confirmed;
                    String blockHeight = root.getJSONArray("txs").getJSONObject(i).get("block_height").toString();
                    confirmed = blockHeight != null;
                    txhashes.add(new Transaction(
                          root.getJSONArray("txs").getJSONObject(i).get("hash").toString(), false, confirmed));
                }
                if (txhashes.size() == 50) {
                    return null;
                }
                return txhashes;
            }
        } catch (AddressFormatException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * This function takes in a transaction hash and passes it to Blockchain.info's API.
     * After some formatting, it returns a bitcoinj Transaction object using this transaction hash.
     *
     */
    public org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {
        String url;
        if (netParams==NetworkParameters.fromID(NetworkParameters.ID_TESTNET)){
            url = "https://api.blockcypher.com/v1/btc/test3/txs/"+transactionHash+"?includeHex=true";
        }else {
            url = "https://api.blockcypher.com/v1/btc/main/txs/"+transactionHash+"?includeHex=true";
        }
       rateLimiter.acquire();
        URL obj = new URL(url);
        JSONTokener tokener = new JSONTokener(obj.openStream());
        JSONObject root = new JSONObject(tokener);
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        byte[] bytearray = adapter.unmarshal(root.get("hex").toString());
        org.bitcoinj.core.Transaction transaction = new org.bitcoinj.core.Transaction(netParams,bytearray);
        return transaction;
    }

}
