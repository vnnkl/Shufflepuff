/**
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.CoinNetworkException;
import com.subgraph.orchid.encoders.Hex;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.TransactionOutPoint;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by vnnkl on 10/28/16.
 */

public class Insight extends Bitcoin {

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

   @Override
   boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException {
      // https://blockchain.info/rawtx/$tx_hash
      String url = "https://" + insightMainnetURL + "/api/tx/" + transactionHash;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/tx/" + transactionHash;
      }
      URL obj = new URL(url);
      JSONTokener tokener = new JSONTokener(obj.openStream());
      JSONObject object = new JSONObject(tokener);
      JSONArray outs = object.getJSONArray("vout");
      JSONObject voutObj = (JSONObject) outs.get(vout);
      return voutObj.getString("spentTxId") == "null";
   }


   protected List<Transaction> getTransactionsFromUtxosInner(HashSet<TransactionOutPoint> t) throws IOException, CoinNetworkException, AddressFormatException {
      List<Transaction> txList = new ArrayList<>();
      HashSet<Transaction> checkDuplicateTx = new HashSet<>();
      for (TransactionOutPoint tO : t) {
         Insight.TransactionWithConfirmations tx;
         try {
            tx = getTransaction(tO.getHash().toString());
            byte[] bytes = tx.getBytes();
            boolean confirmed = tx.getConfirmed();
            org.bitcoinj.core.Transaction bitTx = new org.bitcoinj.core.Transaction(netParams, bytes);
            Transaction bTx = new Transaction(tx.getHashAsString(), bitTx, false, confirmed);
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
    * This function takes in a transaction hash and passes it to insights API.
    * After some formatting, it returns a bitcoinj Transaction object using this transaction hash.
    */
   public synchronized Insight.TransactionWithConfirmations getTransaction(String transactionHash) throws IOException {
      String url = "https://" + insightMainnetURL + "/api/rawtx/" + transactionHash;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/rawtx/" + transactionHash;
      }
      URL obj = new URL(url);
      JSONTokener tokener = new JSONTokener(obj.openStream());
      JSONObject root = new JSONObject(tokener);
      byte[] bytes = Hex.decode(root.getString("rawtx"));

      url = "https://" + insightMainnetURL + "/api/tx/" + transactionHash;
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         url = "https://" + insightTestnetURL + "/api/tx/" + transactionHash;
      }
      obj = new URL(url);
      tokener = new JSONTokener(obj.openStream());
      root = new JSONObject(tokener);
      if (root.getInt("blockheight") == -1) {
         return new Insight.TransactionWithConfirmations(netParams, bytes, false);
      }
      // bitcoinj needs this Context variable
      Context context = Context.getOrCreate(netParams);
      return new Insight.TransactionWithConfirmations(netParams, bytes, true);

   }

}
