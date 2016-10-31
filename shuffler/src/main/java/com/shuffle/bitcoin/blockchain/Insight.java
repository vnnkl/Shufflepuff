/**
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;


/**
 * Created by vnnkl on 10/28/16.
 */

public final class Insight extends Bitcoin {

   private String insightMainnetURL = "insight.bitpay.com";
   private String insightTestnetURL = "test-insight.bitpay.com";

   /**
    * The constructor takes in a NetworkParameters variable that determines whether we are
    * connecting to the Production Net or the Test Net.  It also takes in an int which
    * determines the minimum number of peers to connect to before broadcasting a transaction.
    */

   public Insight(NetworkParameters netParams, int minPeers) {
      super(netParams, minPeers);
      if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
         if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            throw new IllegalArgumentException();
         }
      }

   }

   public Insight(NetworkParameters netParams, int minPeers, String MainNetUrl, String TestNetUrl) {
      super(netParams, minPeers);
      if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
         if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            throw new IllegalArgumentException();
         }
      }
      this.insightMainnetURL = MainNetUrl;
      this.insightTestnetURL = TestNetUrl;
   }

   /**
    * Given a wallet address, this function looks up the address' balance using Insights
    * API. The amount returned is of type long and represents the number of satoshis.
    */
   @Override
   public synchronized long getAddressBalance(String address) throws IOException {
      String url = "https://" + insightMainnetURL + "/api/addr/" + address;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/addr/" + address;
      }
      URL obj = new URL(url);
      JSONTokener tokener = new JSONTokener(obj.openStream());
      JSONObject root = new JSONObject(tokener);
      return Long.valueOf(root.get("balance").toString());
   }


   /**
    * Given a wallet address, this function looks up all transactions associated with the wallet
    * using Insights API. These "n" transaction hashes are then returned in a String
    * array.
    */
   protected final List<Transaction> getAddressTransactionsInner(String address) throws IOException {
      String url = "https://" + insightMainnetURL + "/api/addr/" + address;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/addr/" + address;
      }
      URL obj = new URL(url);
      JSONTokener tokener = new JSONTokener(obj.openStream());
      JSONObject root = new JSONObject(tokener);
      List<Transaction> txhashes = new LinkedList<>();
      for (int i = 0; i < root.getJSONArray("transactions").length(); i++) {
         txhashes.add(new Transaction(
               root.getJSONArray("transactions").get(i).toString(), false));
      }
      return txhashes;

   }

   /**
    * This function takes in a transaction hash and passes it to Blockchain.info's API.
    * After some formatting, it returns a bitcoinj Transaction object using this transaction hash.
    */
   public synchronized org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {

      String url = "https://" + insightMainnetURL + "/api/rawtx/" + transactionHash;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/rawtx/" + transactionHash;
      }
      URL obj = new URL(url);
      JSONTokener tokener = new JSONTokener(obj.openStream());
      JSONObject object = new JSONObject(tokener);
      HexBinaryAdapter adapter = new HexBinaryAdapter();
      byte[] bytearray = adapter.unmarshal(object.getString("rawtx"));
      return new org.bitcoinj.core.Transaction(netParams, bytearray);

   }

}
