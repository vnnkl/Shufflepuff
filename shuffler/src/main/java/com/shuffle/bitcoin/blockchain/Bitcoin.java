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
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
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
import java.util.SortedSet;
import java.util.concurrent.ExecutionException;

public abstract class Bitcoin implements Coin {
    static long cach_expire = 10000; // Ten seconds.

    final NetworkParameters netParams;
    final PeerGroup peerGroup;
    final int minPeers;
    final Context context;
    protected Map<String, Cached> cache = new HashMap<>();

    /**
     *
     * The constructor takes in a NetworkParameters variable that determines whether we
     * are connecting to the Production Net or the Test Net.  It also takes in an int
     * which determines the minimum number of peers to connect to before broadcasting a transaction.
     *
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

    public PeerGroup getPeerGroup(){
        return peerGroup;
    }

    public com.shuffle.bitcoin.Transaction fromBytes(byte[] bytes) {
        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(this.netParams, bytes);
        return new Transaction(tx.getHashAsString(), tx, false);
    }

    public NetworkParameters getNetParams(){
        return netParams;
    }

    /**
     *
     * The shuffleTransaction method returns a Bitcoin.Transaction object that contains a bitcoinj
     * Transaction member which sends "amount" satoshis from the addresses listed in the "from"
     * variable to addresses listed in the "to" variable, in their respective orders.  The bitcoinj
     * Transaction member also sends change from the addresses listed in the "from" variable to
     * addresses listed in the "changeAddresses" variable, in their respective order.
     *
     * To calculate the amount in change to send to the "changeAddresses", we first lookup the
     * transaction associated with an address. We only allow one transaction per address that wants
     * to shuffle their coins.  We then find the transaction output associated with our address,
     * and see how much value was sent to our address.  We then subtract the "amount" from this
     * value and this is the amount to send to the changeAddress.
     *
     * Note: We allow no past transactions to addresses in the "to" variable.
     *
     */

    @Override
    public Bitcoin.Transaction shuffleTransaction(long amount,
                                                  long fee,
                                                  List<VerificationKey> from,
                                                  Map<VerificationKey, HashSet<TransactionOutPoint>> peerUtxos,
                                                  Queue<Address> to,
                                                  Map<VerificationKey, Address> changeAddresses)
            throws CoinNetworkException, AddressFormatException {

        // this section adds inputs to the transaction and adds outputs to the change addresses.
        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams);

        for (VerificationKey key : from) {
            try {
                if (peerUtxos.containsKey(key)) {
                    HashSet<TransactionOutPoint> utxos = peerUtxos.get(key);
                    // incorrect
                    // TODO
                    List<Bitcoin.Transaction> transactions = getAddressTransactions(utxos);
                    for (Bitcoin.Transaction t : transactions) {
                        org.bitcoinj.core.Transaction tx2 = getTransaction(t.hash);
                        for (TransactionOutput output : tx2.getOutputs()) {
                            TransactionOutPoint tO = new TransactionOutPoint(netParams, output.getIndex(), Sha256Hash.wrap(t.hash));
                            if (utxos.contains(tO)) {
                                tx.addInput(output);
                                // TODO
                                // Shouldn't be !changeAddresses.containsKey(key) -- error !
                                if (!changeAddresses.containsKey(key) | changeAddresses.get(key) != null) {
                                    try {
                                        tx.addOutput(output.getValue().subtract(
                                                org.bitcoinj.core.Coin.SATOSHI.multiply(amount + fee)),
                                                new org.bitcoinj.core.Address(
                                                        netParams, changeAddresses.get(key).toString()));
                                    } catch (AddressFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (IOException e) {
                throw new CoinNetworkException("Could not generate shuffle tx: " + e.getMessage());
            }
        }

        for (Address sendto : to) {
            /**
             *
             * How can we determine if getAddressTransactions.size() == 0 for Queue<Address>to
             * Leave it up to the user ? Warning on GUI ?
             *
             * String address = sendto.toString();
             * try {
             *      List<Bitcoin.Transaction> transactions = getAddressTransactions(address);
             *      if (transactions.size() > 0) return null;
             * } catch (IOException e) {
             *      throw new CoinNetworkException("Could not generate shuffle tx: " + e.getMessage());
             * }
             *
             */

            String address = sendto.toString();

            try {
                tx.addOutput(org.bitcoinj.core.Coin.SATOSHI.multiply(amount),
                        new org.bitcoinj.core.Address(netParams, address));
            } catch (AddressFormatException e) {
                e.printStackTrace();
            }

        }

        return new Transaction(tx.getHashAsString(), tx, true);

    }

    /**
     *
     * The valueHeld method takes in an Address variable and returns the balance held using
     * satoshis as the unit.
     *
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
     *
     * The sumUnspentTxOutputs takes in a list of transactions, sums the UTXOs pertaining to address,
     * and returns a long value.  This long value represents the balance of a Bitcoin address in Satoshis.
     *
     */

    protected synchronized long getUtxoBalance(TransactionOutPoint t) throws IOException, CoinNetworkException, AddressFormatException, BitcoindException, CommunicationException {

        System.out.println("\n" +  t.getHash() + "\n");
        TransactionOutput tx = getTransaction(t.getHash().toString()).getOutput(t.getIndex());
        if (isUtxo(t.getHash().toString(), (int) t.getIndex())) {
            return tx.getValue().getValue();
        } else {
            return 0;
        }

    }

    @Override
    public final boolean sufficientFunds(HashSet<TransactionOutPoint> utxos, long amount) throws CoinNetworkException, AddressFormatException, IOException {

        List<Bitcoin.Transaction> transactions = getAddressTransactions(utxos);

        long sum = 0;

        for (Bitcoin.Transaction tx : transactions) {
            if (!tx.confirmed) {
                return false;
            }

            if (tx.bitcoinj == null) {
                try {
                    tx.bitcoinj = getTransaction(tx.hash);
                } catch (IOException e) {
                    return false;
                }
            }

            for (TransactionOutput output : tx.bitcoinj.getOutputs()) {

                /**
                 * Every address in the outputs should be of type pay to public key hash, not pay to script hash
                 */

                TransactionOutPoint tO = new TransactionOutPoint(netParams, output.getIndex(), Sha256Hash.wrap(tx.hash));
                if (utxos.contains(tO)) {
                    sum += output.getValue().getValue();
                }
            }
        }

        return sum >= amount;

    }

    // TODO
    @Override
    public synchronized com.shuffle.bitcoin.Transaction getConflictingTransaction(
            com.shuffle.bitcoin.Transaction t, Address addr, long amount) throws CoinNetworkException, AddressFormatException {

        /*
        if (!(t instanceof Transaction)) throw new IllegalArgumentException();
        Transaction transaction = (Transaction)t;

        String address = addr.toString();

        List<Bitcoin.Transaction> transactions = null;
        try {
            transactions = getAddressTransactions(address);
        } catch (IOException e) {
            // Can we return null here?
            return null;
        }

        // Ensures that all transactions have the bitcoinj field set
        for (Bitcoin.Transaction tx : transactions) {
            if (tx.bitcoinj == null) {
                try {
                    tx.bitcoinj = getTransaction(tx.hash);
                } catch (IOException e) {
                    // We should not have an IOException
                    return null;
                }
            }
        }

        for (Bitcoin.Transaction tx : transactions) {
            for (TransactionInput input : tx.bitcoinj.getInputs()) {
                // Can be multiple inputs for transaction parameter.
                for (TransactionInput txInput : transaction.bitcoinj.getInputs()) {
                    if (input.equals(txInput)) {
                        return tx;
                    }
                }
            }
        }*/

        return null;
    }

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

    /**
     * Takes in a transaction and a private key and returns a signature (if possible)
     * as a Bytestring object.
     */
    public Bytestring getSignature(org.bitcoinj.core.Transaction signTx, ECKey privKey) {

        org.bitcoinj.core.Transaction copyTx = signTx;

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
                return new Bytestring(inputScript.getProgram());
            } catch (VerificationException e) {
                input.setScriptSig(this.bytestringToInputScript(new Bytestring(originalScript)));
            }
        }

        return null;
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
    protected synchronized List<Bitcoin.Transaction> getAddressTransactions(HashSet<TransactionOutPoint> t)
            throws IOException, CoinNetworkException, AddressFormatException {

        /*
        long now = System.currentTimeMillis();
        Cached cached = cache.get(address);
        if (cached != null) {
            if (now - cached.last < cach_expire) {
                return cached.txList;
            }
        }*/

        List<Bitcoin.Transaction> txList = getAddressTransactionsInner(t);

        // ?
        //cache.put(address, new Cached(address, txList, System.currentTimeMillis()));

        return txList;
    }

    protected boolean send(Bitcoin.Transaction t) throws ExecutionException, InterruptedException, CoinNetworkException {
        if (!t.canSend || t.sent) {
            return false;
        }

        //checks to see if transaction was broadcast
        if (peerGroup == null) {
            return false;
        }
        peerGroup.broadcastTransaction(t.bitcoinj).future().get();
        t.sent = true;
        return true;
    }

    // synchronized?
    abstract boolean isUtxo(String transactionHash, int vout) throws IOException, BitcoindException, CommunicationException;

    // Should NOT be synchronized.
    abstract protected List<Bitcoin.Transaction> getAddressTransactionsInner(HashSet<TransactionOutPoint> t)
            throws IOException, CoinNetworkException, AddressFormatException;

    // Should be synchronized.
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
