package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.domain.RawTransaction;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.params.TestNet3Params;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;

//import org.bitcoinj.core.Transaction;

/**
 * Created by nsa on 10/25/16.
 */
public class TestBitcoinCore {

    NetworkParameters netParams = TestNet3Params.get();
    BitcoinCore testCase;

    public TestBitcoinCore() throws MalformedURLException, BitcoindException, CommunicationException {
        testCase = new BitcoinCore(netParams, "admin", "pass");
    }

    private static JSONArray readJSONArray(String ar) {

        try {
            JSONObject json = (JSONObject) JSONValue.parse("{\"x\":" + ar + "}");
            if (json == null) {
                throw new IllegalArgumentException("Could not parse json object " + ar + ".");
            }

            return (JSONArray) json.get("x");

        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Could not parse json object " + ar + ".");
        }
    }

    @Test
    public void test4() throws Exception {

            HashSet<Bytestring> sigs = new HashSet<>();
            sigs.add(new Bytestring("\"leol\"".getBytes()));
            sigs.add(new Bytestring("\"woakoekokefw\"".getBytes()));
            sigs.add(new Bytestring("\"wokwokwokwok\"".getBytes()));
            String jsonString = "{";
            int count = 0;
            for (Bytestring b : sigs) {
                jsonString = jsonString.concat("\"" + String.valueOf(count) + "\":" + new String(b.bytes));
                if (!b.equals(sigs.toArray()[sigs.size() - 1])) {
                    jsonString = jsonString.concat(",");
                }
                count++;
            }
            jsonString = jsonString.concat("}");

            Bytestring x  = new Bytestring(jsonString.getBytes());
            System.out.println(x);
            System.out.println(new String(x.bytes));

    }

    @Test
    public void test3() throws Exception {
        BtcdQueryClient client = new BtcdQueryClient("127.0.0.1", 18332, "admin", "pass");
        Object rawTx= client.getRawTransaction("7858d17243b58f9cb4c9ef0083c8df315c6275d9b518d42e81e12d1df4105be4", 1);
        RawTransaction x = (RawTransaction) rawTx;
        System.out.println(x.getHex());
        System.out.println(x.getConfirmations());

        String rawString = client.getRawTransaction("7858d17243b58f9cb4c9ef0083c8df315c6275d9b518d42e81e12d1df4105be4");
        System.out.println(rawString);
    }

    @Test
    public void test2() throws Exception {
        // test confirmations, etc

        BtcdQueryClient client = new BtcdQueryClient("127.0.0.1", 18332, "admin", "pass");

        RawTransaction x = (RawTransaction) client.getRawTransaction("a4f233d19fbbebde45e7296bb5e830962c4565d0aefc409a73df3143637cb0af", 1);

        System.out.println(x);

        System.out.println(x.getConfirmations());
        System.out.println(x.getTxId());

    }

    @Test
    public void test() {

        HashSet<TransactionOutPoint> utxos = new HashSet<>();
        utxos.add(new TransactionOutPoint(netParams, 2, Sha256Hash.wrap("4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a")));
        utxos.add(new TransactionOutPoint(netParams, 3, Sha256Hash.wrap("04192d801e714cd661ea0b8182bbe3c3dcfb3ab21b563b100961baa9da3db652")));
        String jsonString = "[";
        String txhash = "\"txhash\"";
        String vout = "\"vout\"";
        for (TransactionOutPoint t : utxos) {
            jsonString = jsonString.concat("{");
            jsonString = jsonString.concat(txhash + ":" + "\"" + t.getHash().toString() + "\"");
            jsonString = jsonString.concat(",");
            jsonString = jsonString.concat(vout + ":" + String.valueOf(t.getIndex()));
            jsonString = jsonString.concat("}");
            if (t != utxos.toArray()[utxos.size() - 1]) {
                jsonString = jsonString.concat(",");
            }
        }

        jsonString = jsonString.concat("]");

        System.out.println(readJSONArray(jsonString));

    }

    @Test
    public void isUtxo() throws IOException, BitcoindException, CommunicationException {
        //boolean testUtxo = testCase.isUtxo("8acc615b65c0d0c1ff255e3a316f69706a495aab426ee448506f99d5a2629598", 6);
        boolean testUtxo = testCase.isUtxo("a4f233d19fbbebde45e7296bb5e830962c4565d0aefc409a73df3143637cb0af", 0);
        Assert.assertTrue(testUtxo);
    }

    /*
    @Test
    public void testGetTransaction() throws IOException {
        com.shuffle.bitcoin.Transaction testTx = testCase.getTransaction("820c1c38df7e5b87b8890fa08c9a974d50986df8d0efe296a3e0cd4f18c58284");

        HexBinaryAdapter adapter = new HexBinaryAdapter();
        String testHex = "01000000013eac7c60e861e43937079782550831cd1bd504e800e77ee3c581ae68ee0935bf000000006b483045022100e1be72792ce08cbb0f5fdb74b363c59e35cc873989b7cb901aeccaa4985ae4ba02204e88630994c4a4d10c628ba3195b0536b3832f0526753d8e37810de2f75463ae012103f0e89ee335835056fcc265d43553ba3e86f0ff81a030eb2c494e2a1934f7d2d2ffffffff02fc350000000000001976a914a5a73ae92105d02c76a846f7345cde5eb22f085788ac50c30000000000001976a9141234f920743134689a0429001406608e4981e77588ac00000000";
        byte[] bytearray = adapter.unmarshal(testHex);
        org.bitcoinj.core.Transaction transactionj = new org.bitcoinj.core.Transaction(netParams, bytearray);

        String hash1, hash2;
        hash1 = transactionj.getHashAsString();
        hash2 = testTx.getHashAsString();

        Assert.assertEquals(hash1, hash2);

    }
    */
}
