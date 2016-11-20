/**
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.shuffle.bitcoin.blockchain;


import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Signatures;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

public abstract class Bitcoin implements Coin {
   static long cache_expire = 10000; // Ten seconds.

   final NetworkParameters netParams;
   final PeerGroup peerGroup;
   final int minPeers;
   final Context context;
   protected Map<String, Cached> cache = new HashMap<>();

   /**
    * The constructor takes in a NetworkParameters variable that determines whether we
    * are connecting to the Production Net or the Test Net.  It also takes in an int
    * which determines the minimum number of peers to connect to before broadcasting a transaction.
    */

   public Bitcoin(NetworkParameters netParams, int minPeers) {
      this.netParams = netParams;
      this.minPeers = minPeers;
      if (minPeers == 0) {
         peerGroup = null;
      } else {
         peerGroup = new PeerGroup(netParams);
         peerGroup.setMinBroadcastConnections(minPeers);
         peerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
         peerGroup.start();
      }
      this.context = Context.getOrCreate(this.netParams);
   }

   public PeerGroup getPeerGroup() {
      return peerGroup;
   }

   public com.shuffle.bitcoin.Transaction fromBytes(byte[] bytes) {
      org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(this.netParams, bytes);
      return new Transaction(tx.getHashAsString(), tx, false);
   }

   public NetworkParameters getNetParams() {
      return netParams;
   }

   /**
    * The shuffleTransaction method returns a Bitcoin.Transaction object that contains a bitcoinj
    * Transaction member which sends "amount" satoshis from the addresses listed in the "from"
    * variable to addresses listed in the "to" variable, in their respective orders.  The bitcoinj
    * Transaction member also sends change from the addresses listed in the "from" variable to
    * addresses listed in the "changeAddresses" variable, in their respective order.
    *
    * To calculate the amount in change to send to the "changeAddresses", we get the summed balance of
    * UTXOs in the UTXO List.  We then subtract the "amount" and "fee" from this value - this amount
    * is then sent to the changeAddress.
    */

   @Override
   public Bitcoin.Transaction shuffleTransaction(long amount,
                                                 long fee,
                                                 List<VerificationKey> from,
                                                 Map<VerificationKey, HashSet<TransactionOutPoint>> peerUtxos,
                                                 Queue<Address> to,
                                                 Map<VerificationKey, Address> changeAddresses)
         throws CoinNetworkException, AddressFormatException {

      org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams);
      HashMap<org.bitcoinj.core.Address, org.bitcoinj.core.Coin> map = new HashMap<>();

      // this is where we add the shuffled outputs
      for (Address sendto : to) {

         String address = sendto.toString();

         try {
            tx.addOutput(org.bitcoinj.core.Coin.SATOSHI.multiply(amount),
                  new org.bitcoinj.core.Address(netParams, address));
         } catch (AddressFormatException e) {
            e.printStackTrace();
             throw new RuntimeException(e);
         }

      }

      // this is where we add the change addresses
      for (VerificationKey key : from) {
         try {
            // get all transactions belonging to VKey
            HashSet<TransactionOutPoint> utxos = peerUtxos.get(key);
            // get total amount of utxos held by Vkey
            long keyAmount = valueHeld(utxos);
            if (keyAmount >= amount + fee) {
               List<Bitcoin.Transaction> transactions = getTransactionsFromUtxos(utxos);
               for (Bitcoin.Transaction t : transactions) {
                  for (TransactionOutput output : t.bitcoinj.getOutputs()) {
                     //if output is part of utxo set add it to transaction as input
                     if (utxos.contains(output.getOutPointFor())) {
                        tx.addInput(output);
                     }
                  }
               }

               if (changeAddresses.get(key) != null) {
                  Address current = changeAddresses.get(key);
                  try {
                     // change amount is amount of ((all utxos of key) - (amount+fee)), in satoshis
                     org.bitcoinj.core.Coin coin = org.bitcoinj.core.Coin.valueOf(keyAmount).subtract(org.bitcoinj.core.Coin.SATOSHI.multiply(amount + fee));
                     org.bitcoinj.core.Address addr = new org.bitcoinj.core.Address(netParams, current.toString());
                     // this map ensures we don't have duplicate change addresses if a player enters multiple utxos
                     if (map.containsKey(addr)) {
                        org.bitcoinj.core.Coin c = map.get(addr);
                        map.put(addr, coin.add(c));
                     } else {
                        map.put(addr, coin);
                     }
                  } catch (AddressFormatException e) {
                     e.printStackTrace();
                     throw new RuntimeException(e);
                  }
               }
            } else {
               throw new RuntimeException("UTXOs do not satisfy amount + fee ");
            }

         } catch (IOException e) {
            throw new CoinNetworkException("Could not generate shuffle tx: " + e.getMessage());
         }
      }

      for (org.bitcoinj.core.Address a : map.keySet()) {
         tx.addOutput(map.get(a), a);
      }

      return new Transaction(tx.getHashAsString(), tx, true);

   }

   /**
    * The valueHeld method takes in a list of UTXOs and returns the summed balance held using
    * satoshis as the unit.
    */

   @Override
   public long valueHeld(HashSet<TransactionOutPoint> utxos) throws CoinNetworkException, AddressFormatException {

      long sum = 0;
      for (TransactionOutPoint t : utxos) {
         try {
            sum += getUtxoBalance(t);
         } catch (IOException | BitcoindException | CommunicationException e) {
            throw new CoinNetworkException("Could not look up balance: " + e.getMessage());
         }
      }

      return sum;
   }

   /**
    * The getUtxoBalance method takes one UTXO and returns the amount of spendable Bitcoin that it holds.
    * This amount is a long value represented as the number of satoshis.
    */

   protected synchronized long getUtxoBalance(TransactionOutPoint t) throws IOException, CoinNetworkException, AddressFormatException, BitcoindException, CommunicationException {

      if (isUtxo(t.getHash().toString(), (int) t.getIndex())) {
         TransactionOutput tx = getTransaction(t.getHash().toString()).getOutput(t.getIndex());
         return tx.getValue().getValue();
      } else {
         return 0;
      }

   }

   @Override
   public final boolean sufficientFunds(HashSet<TransactionOutPoint> utxos, long amount) throws CoinNetworkException, AddressFormatException {
      return valueHeld(utxos) > amount;
   }

   @Override
   public synchronized com.shuffle.bitcoin.Transaction getConflictingTransaction(
         com.shuffle.bitcoin.Transaction t, HashSet<TransactionOutPoint> utxos, long amount)
           throws CoinNetworkException, AddressFormatException, BlockStoreException, IOException {

       return getConflictingTransactionInner(t, utxos, amount);

   }

   //TODO
   /**
    * Optimize ?
    */
   public org.bitcoinj.core.Transaction signTransaction(org.bitcoinj.core.Transaction signTx, List<Bytestring> programSignatures) {

      List<Script> inputScripts = new LinkedList<>();
      for (Bytestring programs : programSignatures) {
         if (!inputScripts.add(bytestringToInputScript(programs))) {
            return null;
         }
      }

      for (Script inScript : inputScripts) {
         for (int i = 0; i < signTx.getInputs().size(); i++) {
            TransactionInput input = signTx.getInput(i);
            TransactionOutput connectedOutput = input.getConnectedOutput();
            byte[] originalScript = input.getScriptBytes().clone();
            input.setScriptSig(inScript);
            try {
               input.verify(connectedOutput);
               break;
            } catch (VerificationException e) {
               input.setScriptSig(this.bytestringToInputScript(new Bytestring(originalScript)));
               if (i == signTx.getInputs().size() - 1) {
                  return null;
               }
            }
         }
      }

      return signTx;
   }

    // TODO
   /**
    * Takes in a transaction and a private key and returns a signature (if possible)
    * as a Bytestring object.
    */
   public Bytestring getSignature(org.bitcoinj.core.Transaction signTx, ECKey privKey) {

      org.bitcoinj.core.Transaction copyTx = signTx;

      HashSet<Bytestring> sigs = new HashSet<>();

      for (int i = 0; i < copyTx.getInputs().size(); i++) {
         TransactionInput input = copyTx.getInput(i);
         TransactionOutput connectedOutput = input.getConnectedOutput();
         Sha256Hash hash = copyTx.hashForSignature(i, connectedOutput.getScriptPubKey(), org.bitcoinj.core.Transaction.SigHash.ALL, false);
         ECKey.ECDSASignature ecSig = privKey.sign(hash);
         TransactionSignature txSig = new TransactionSignature(ecSig, org.bitcoinj.core.Transaction.SigHash.ALL, false);
         byte[] originalScript = input.getScriptBytes().clone();
         Script inputScript = ScriptBuilder.createInputScript(txSig, ECKey.fromPublicOnly(privKey.getPubKey()));
         input.setScriptSig(inputScript);
         try {
            input.verify(connectedOutput);
            sigs.add(new Bytestring(inputScript.getProgram()));
         } catch (VerificationException e) {
            input.setScriptSig(this.bytestringToInputScript(new Bytestring(originalScript)));
         }
      }

      Signatures s = new Signatures("null".getBytes(), sigs);

      return s;

   }

   // Converts a Bytestring object to a Script object.
   public Script bytestringToInputScript(Bytestring program) {
      return new Script(program.bytes);
   }

   // Since we rely on 3rd party services to query the blockchain, by
   // default we cache the result.

   // TODO
   // Block explorers need to be updated
   // Duplicates due to HashSet<TransactionOutPoint> ?
   protected synchronized List<Bitcoin.Transaction> getTransactionsFromUtxos(HashSet<TransactionOutPoint> t)
         throws IOException, CoinNetworkException, AddressFormatException {

        /*
        long now = System.currentTimeMillis();
        Cached cached = cache.get(address);
        if (cached != null) {
            if (now - cached.last < cach_expire) {
                return cached.txList;
            }
        }
        */

      List<Bitcoin.Transaction> txList = getTransactionsFromUtxosInner(t);

      // ?
      // cache.put(address, new Cached(address, txList, System.currentTimeMillis()));

      return txList;
   }

   protected boolean send(Bitcoin.Transaction t) throws ExecutionException, InterruptedException, CoinNetworkException {
      if (!t.canSend || t.sent) {
         return false;
      }

      // checks to see if transaction was broadcast
      if (peerGroup == null) {
         return false;
      }

      peerGroup.broadcastTransaction(t.bitcoinj).future().get();
      t.sent = true;
      return true;
   }

   abstract com.shuffle.bitcoin.Transaction getConflictingTransactionInner(
           com.shuffle.bitcoin.Transaction t, HashSet<TransactionOutPoint> utxos, long amount)
           throws CoinNetworkException, AddressFormatException, BlockStoreException, IOException;

   abstract boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException;

   abstract protected List<Bitcoin.Transaction> getTransactionsFromUtxosInner(HashSet<TransactionOutPoint> t)
         throws IOException, CoinNetworkException, AddressFormatException;

   abstract org.bitcoinj.core.Transaction getTransaction(String transactionHash)
         throws IOException;

   public class Transaction implements com.shuffle.bitcoin.Transaction {
      final String hash;
      final boolean canSend;
      boolean confirmed;
      boolean sent = false;
      private org.bitcoinj.core.Transaction bitcoinj;

      public Transaction(String hash, boolean canSend) {
         this.hash = hash;
         this.canSend = canSend;
      }

      public Transaction(String hash, org.bitcoinj.core.Transaction bitcoinj, boolean canSend) {
         this.hash = hash;
         this.bitcoinj = bitcoinj;
         this.canSend = canSend;
      }

      public Transaction(String hash, boolean canSend, boolean confirmed) {
         this.hash = hash;
         this.canSend = canSend;
         this.confirmed = confirmed;
      }

      public Transaction(String hash, org.bitcoinj.core.Transaction bitcoinj, boolean canSend, boolean confirmed) {
         this.hash = hash;
         this.bitcoinj = bitcoinj;
         this.canSend = canSend;
         this.confirmed = confirmed;
      }

      // Get the underlying bitcoinj representation of this transaction.
      public org.bitcoinj.core.Transaction bitcoinj() throws BlockStoreException, IOException {
         if (bitcoinj == null) {
            bitcoinj = getTransaction(hash);
         }

         return bitcoinj;
      }

      /**
       * The send() method broadcasts a transaction into the Bitcoin network.  The canSend boolean
       * variable tells us if the transaction was created by us, or taken from the blockchain.
       * If we created the transaction, then we are able to broadcast it.  Otherwise, we cannot.
       */

      @Override
      public synchronized void send()
            throws ExecutionException, InterruptedException, CoinNetworkException {

         if (!Bitcoin.this.send(this)) {
            throw new CoinNetworkException("Could not send transaction.");
         }
      }

      @Override
      public Bytestring serialize() {
         return new Bytestring(bitcoinj.bitcoinSerialize());
      }

      @Override
      public Bytestring sign(SigningKey sk) {
         if (!(sk instanceof SigningKeyImpl)) {
            return null;
         }
         return Bitcoin.this.getSignature(this.bitcoinj, ((SigningKeyImpl) sk).signingKey);
      }

      @Override
      public boolean addInputScript(Bytestring b) throws FormatException {
         List<Bytestring> programSignatures = new LinkedList<>();
         programSignatures.add(b);
         return Bitcoin.this.signTransaction(this.bitcoinj, programSignatures) != null;
      }

      @Override
      public boolean isValid() {
         for (TransactionInput input : this.bitcoinj.getInputs()) {
            TransactionOutput output = input.getConnectedOutput();
            if (input.getScriptSig() == null) {
               return false;
            }
            try {
               input.verify(output);
            } catch (VerificationException e) {
               return false;
            }
         }
         return true;
      }

      @Override
      public String toString() {
         return hash;
      }
   }

   protected class Cached {
      public final String address;
      public final List<Bitcoin.Transaction> txList;
      public final long last;

      private Cached(String address, List<Bitcoin.Transaction> txList, long last) {
         this.address = address;
         this.txList = txList;
         this.last = last;
      }
   }
}
