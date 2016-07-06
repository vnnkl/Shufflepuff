/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulation of a cryptocurrency network for testing purposes.
 *
 * Created by Daniel Krawisz on 12/5/15.
 */
public class MockCoin implements com.shuffle.sim.MockCoin {
    public static class Output implements Serializable {
        public final Address address;
        public final long amountHeld;

        public Output(Address address, long amount) {
            if (address == null) throw new NullPointerException();
            if (amount == 0) throw new IllegalArgumentException();

            this.address = address;
            this.amountHeld = amount;
        }

        @Override
        public String toString() {
            return "output[" + address.toString() + ", " + amountHeld + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;

            if (!(o instanceof Output)) return false;

            Output out = (Output)o;

            return address.equals(out.address) && amountHeld == out.amountHeld;
        }

        @Override
        public int hashCode() {
            return address.hashCode() + (int)amountHeld;
        }
    }

    /**
     * Created by Daniel Krawisz on 12/8/15.
     */
    public static class MockTransaction implements Transaction, Serializable {
        public final List<Output> inputs = new LinkedList<>();
        public final List<Output> outputs = new LinkedList<>();
        // A number used to represented slight variations in a transaction which would
        // result in different signatures being produced.
        public final int z;

        private final transient MockCoin coin;

        MockTransaction(List<Output> inputs, List<Output> outputs, MockCoin coin) {
            this(inputs, outputs, 1, coin);
        }

        public static MockTransaction fromBytes(Bytestring b) throws FormatException {

            ObjectInputStream str;
            try {
                str = new ObjectInputStream(new ByteArrayInputStream(b.bytes));
                return (MockTransaction) str.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new FormatException("Cannot read " + b + " as MockTransaction.");
            }

        }

        public MockTransaction(List<Output> inputs, List<Output> outputs, int z, MockCoin coin) {
            for (Output output : inputs)
                if (output == null) throw new NullPointerException();

            for (Output output : outputs)
                if (output == null) throw new NullPointerException();

            this.z = z;
            this.inputs.addAll(inputs);
            this.outputs.addAll(outputs);
            this.coin = coin;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;

            if (!(o instanceof MockTransaction)) return false;

            MockTransaction mock = (MockTransaction)o;

            if (this == mock) return true;

            if (z != mock.z) return false;

            if (inputs.size() != mock.inputs.size()) return false;

            if (outputs.size() != mock.outputs.size()) return true;

            Iterator<Output> i1 = inputs.iterator();
            Iterator<Output> i2 = mock.inputs.iterator();

            while (i1.hasNext()) {
                Output c1 = i1.next();
                Output c2 = i2.next();

                if (c2 == null) return false;

                if (!c1.equals(c2)) return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "{" + inputs.toString() + " ==> " + outputs.toString() + "; z:" + z + "}";
        }

        @Override
        public boolean send() throws CoinNetworkException {
            if (coin == null) return false;
            coin.send(this);
            return true;
        }

        @Override
        public Bytestring serialize() {
            return new Bytestring(toString().getBytes());
        }

        public MockTransaction copy() {
            return new MockTransaction(inputs, outputs, z, coin);
        }

        public MockTransaction mutate() {
            return new MockTransaction(inputs, outputs, z + 1, coin);
        }

        @Override
        public int hashCode() {
            return inputs.hashCode() + 17 * (outputs.hashCode() + (17 * z));
        }
    }

    final ConcurrentHashMap<Address, Output> blockchain;
    // The transaction that spends an output.
    final ConcurrentHashMap<Output, MockTransaction> spend;
    // The transaction that sends to an input.
    final ConcurrentHashMap<Output, MockTransaction> sent;

    public MockCoin(Map<Address, Output> blockchain) {

        this.blockchain = new ConcurrentHashMap<>();
        spend = new ConcurrentHashMap<>();
        sent = new ConcurrentHashMap<>();

        this.blockchain.putAll(blockchain);
    }

    public MockCoin() {

        blockchain = new ConcurrentHashMap<>();
        spend = new ConcurrentHashMap<>();
        sent = new ConcurrentHashMap<>();
    }

    private MockCoin(
            ConcurrentHashMap<Address, Output> blockchain,
            ConcurrentHashMap<Output, MockTransaction> spend,
            ConcurrentHashMap<Output, MockTransaction> sent
    ) {
        this.blockchain = blockchain;
        this.spend = spend;
        this.sent = sent;
    }

    @Override
    public Coin mutated() {
        return new TransactionMutator(this);
    }

    @Override
    public synchronized void put(Address addr, long value) {
        Output entry = new Output(addr, value);
        blockchain.put(addr, entry);
    }

    @Override
    public synchronized Transaction makeSpendingTransaction(Address from, Address to, long amount) {
        Output output = blockchain.get(from);

        if (output == null) return null;

        if (amount > valueHeld(from)) return null;

        List<Output> in = new LinkedList<>();
        List<Output> out = new LinkedList<>();
        in.add(output);
        out.add(new Output(to, amount));

        return new MockTransaction(in, out, this);
    }

    public synchronized void send(MockTransaction mt) throws CoinNetworkException {
        if (mt == null) throw new NullPointerException();

        // First check that the transaction doesn't send more than it spends.
        long available = 0;
        for (Output input : mt.inputs) available += input.amountHeld;

        for (Output output : mt.outputs) available -= output.amountHeld;

        if (available < 0) throw new CoinNetworkException(mt);

        // Does the transaction spend from valid outputs?
        for (Output input : mt.inputs)
            if (!blockchain.get(input.address).equals(input))
                throw new CoinNetworkException();

        for (Output input : mt.inputs) {
            Transaction nt = spend.get(input);

            if (nt == null) continue;

            if (mt.equals(nt)) return;
            else throw new CoinNetworkException(nt);
        }

        // Register the transaction.
        for (Output input : mt.inputs) spend.put(input, mt);

        for (Output output : mt.outputs) {
            blockchain.put(output.address, output);
            sent.put(output, mt);
        }
    }

    @Override
    public synchronized long valueHeld(Address addr) {
        Output entry = blockchain.get(addr);
        if (entry == null) return 0;

        if (spend.get(entry) != null) return 0;

        return entry.amountHeld;
    }

    @Override
    // TODO transaction fees.
    public Transaction shuffleTransaction(
            final long amount,
            List<VerificationKey> from,
            Queue<Address> to,
            Map<VerificationKey, Address> changeAddresses) {

        if (amount == 0) throw new IllegalArgumentException();

        List<Output> inputs = new LinkedList<>();
        List<Output> changes = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        // Are there inputs big enough to make this transaction?
        for (VerificationKey key : from) {
            final Address address = key.address();
            final long value = valueHeld(address);

            Output input = blockchain.get(address);

            if (input == null) return null;

            inputs.add(input);

            // If a change address has been provided, add that.
            Address change = changeAddresses.get(key);
            if (change != null) changes.add(new Output(change, value - amount));
        }

        for (Address address : to) outputs.add(new Output(address, amount));

        outputs.addAll(changes);

        return new MockTransaction(inputs, outputs, 1, this);
    }

    @Override
    public Transaction getConflictingTransaction(Address addr, long amount) {
        if (valueHeld(addr) >= amount) return null;

        Output output = blockchain.get(addr);

        if (output == null) return null;

        Transaction t = spend.get(output);

        if (t != null) return t;

        return sent.get(output);
    }

    @Override
    public Transaction getSpendingTransaction(Address addr, long amount) {
        Output output = blockchain.get(addr);

        if (output == null) return null;

        return spend.get(output);
    }

    @Override
    public String toString() {
        return "{" + blockchain.values().toString() + ", " + spend.toString() + "}";
    }

    @Override
    public com.shuffle.sim.MockCoin copy() {
        MockCoin newCoin = new MockCoin();

        newCoin.blockchain.putAll(blockchain);
        newCoin.spend.putAll(spend);
        newCoin.sent.putAll(sent);

        return newCoin;
    }

    public String JSON() {
        JSONObject json = new JSONObject();

        JSONArray outputs = new JSONArray();
        int i = 1;
        Map<Output, Integer> blockchain = new HashMap<>();

        for (Output o : this.blockchain.values()) {
            blockchain.put(o, i);

            JSONObject output = new JSONObject();
            output.put("address", o.address);
            output.put("amount", o.amountHeld);

            outputs.add(output);

            i ++;
        }

        json.put("outputs", outputs);

        JSONArray transactions = new JSONArray();

        for (MockTransaction t : sent.values()) {
            JSONArray in = new JSONArray();
            JSONArray out = new JSONArray();

            for (Output o : t.inputs) {
                in.add(blockchain.get(o));
            }

            for (Output o : t.outputs) {
                out.add(blockchain.get(o));
            }

            JSONObject jt = new JSONObject();
            jt.put("inputs", in);
            jt.put("outputs", out);
            if (t.z != 1) jt.put("z", t.z);
            transactions.add(jt);
        }

        json.put("transactions", transactions);

        return json.toString();
    }

    public static MockCoin fromJSON(Reader jsonObject) throws IllegalArgumentException {

        JSONObject json = null;
        JSONArray outputs = null;
        JSONArray transactions = null;
        try {
            json = (JSONObject) JSONValue.parse(jsonObject);
            if (json == null) {
                throw new IllegalArgumentException("could not parse json object.");
            }

            outputs = (JSONArray) json.get("outputs");
            if (outputs == null) {
                throw new IllegalArgumentException("Missing field \"outputs\".");
            }

            transactions = (JSONArray) json.get("transactions");
            if (transactions == null) {
                throw new IllegalArgumentException("Missing field \"transactions\".");
            }
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("could not parse json object.");
        }

        MockCoin mock = new MockCoin();

        Map<Long, Output> outList = new HashMap<>();
        for (int i = 1; i <= outputs.size(); i ++) {
            JSONObject o = null;
            try {
                o = (JSONObject) outputs.get(i - 1);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read "
                        + outputs.get(i - 1) + " as json object.");
            }

            Object address = o.get("address");
            Long amount = (Long) o.get("amount");
            if (address == null) {
                throw new IllegalArgumentException("Output missing field \"address\".");
            }
            if (amount == null) {
                throw new IllegalArgumentException("Output missing field \"amount\".");
            }

            Output bo = null;
            if (address instanceof String) {
                bo = new Output(new MockAddress((String)address),amount);
            } else if (address instanceof Long) {
                bo = new Output(new MockAddress((Long)address),amount);
            } else {
                throw new IllegalArgumentException("Could not read " + address + " as address");
            }

            outList.put((long)i, bo);
            mock.blockchain.put(bo.address, bo);
        }

        for (Object t : transactions) {

            JSONObject trans = (JSONObject)t;
            JSONArray in = (JSONArray) trans.get("inputs");
            JSONArray out = (JSONArray) trans.get("outputs");
            List<Output> tout = new LinkedList<>();
            List<Output> tin = new LinkedList<>();

            for (Object i : in) {
                Output o = outList.get(i);
                if (o == null) {
                    throw new IllegalArgumentException("Missing output " + o);
                }
                tin.add(outList.get(i));
            }

            for (Object i : out) {
                Output o = outList.get(i);
                if (o == null) {
                    throw new IllegalArgumentException("Missing output " + o);
                }
                tout.add(outList.get(i));
            }

            MockTransaction tr;
            Long z = null;
            Object zz = trans.get("z");
            try {

                if (zz == null) {
                    System.out.println("About to try to create transation " + tin + ", " + tout);
                    tr = new MockTransaction(tin, tout, mock);
                } else {
                    z = (Long) zz;
                    tr = new MockTransaction(tin, tout, z.intValue(), mock);
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Could not read z value " + zz + " as long.");
            }
        }

        return mock;
    }
}
