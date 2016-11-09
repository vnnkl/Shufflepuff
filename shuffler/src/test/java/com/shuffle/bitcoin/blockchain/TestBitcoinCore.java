package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by nsa on 10/25/16.
 */
public class TestBitcoinCore {

    NetworkParameters netParams = MainNetParams.get();
    BitcoinCore testCase;

    public TestBitcoinCore() throws MalformedURLException, BitcoindException, CommunicationException {
        //testCase = new BitcoinCore(netParams, "admin", "pass");
    }

    @Test
    public void test2() throws Exception {
        // test confirmations, etc

        BtcdQueryClient client = new BtcdQueryClient("127.0.0.1", 8332, "admin", "pass");

        com.neemre.btcdcli4j.core.domain.Transaction rawTx = client.getTransaction("0b17924648f84e60c0a83fb65027ff60dc0355c347790e5d6baced3847cd24d6");

        System.out.println(rawTx);

        System.out.println(rawTx.getConfirmations());
        System.out.println(rawTx.getHex());

    }

    // REMOVE
    @Test
    public void test() {
        //SortedSet<TransactionOutPoint> utxos = new TreeSet<>();
        HashSet<TransactionOutPoint> utxos = new HashSet<>();
        utxos.add(new TransactionOutPoint(netParams, 2, Sha256Hash.wrap("4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a")));
        String jsonString = "{";
        String output = "\"output\"";
        String txhash = "\"txhash\"";
        String vout = "\"vout\"";
        for (TransactionOutPoint t : utxos) {
            jsonString = jsonString.concat(output + ":");
            jsonString = jsonString.concat("{");
            jsonString = jsonString.concat(txhash + ":" + "\"" + t.getHash().toString() + "\"");
            jsonString = jsonString.concat(",");
            jsonString = jsonString.concat(vout + ":" + String.valueOf(t.getIndex()));
            jsonString = jsonString.concat("}");
            //if (t != utxos.last()) {
            if (t != utxos.toArray()[utxos.size() - 1]) {
                jsonString = jsonString.concat(",");
            }
        }

        jsonString = jsonString.concat("}");

        System.out.println(jsonString);
    }

    @Test
    public void isUtxo() throws IOException, BitcoindException, CommunicationException {
        //boolean testUtxo = testCase.isUtxo("8acc615b65c0d0c1ff255e3a316f69706a495aab426ee448506f99d5a2629598", 6);
        boolean testUtxo = testCase.isUtxo("4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a", 0);
        Assert.assertTrue(testUtxo);
    }

    @Test
    public void testGetTransaction() throws IOException {
        Transaction testTx = testCase.getTransaction("820c1c38df7e5b87b8890fa08c9a974d50986df8d0efe296a3e0cd4f18c58284");

        HexBinaryAdapter adapter = new HexBinaryAdapter();
        String testHex = "01000000013eac7c60e861e43937079782550831cd1bd504e800e77ee3c581ae68ee0935bf000000006b483045022100e1be72792ce08cbb0f5fdb74b363c59e35cc873989b7cb901aeccaa4985ae4ba02204e88630994c4a4d10c628ba3195b0536b3832f0526753d8e37810de2f75463ae012103f0e89ee335835056fcc265d43553ba3e86f0ff81a030eb2c494e2a1934f7d2d2ffffffff02fc350000000000001976a914a5a73ae92105d02c76a846f7345cde5eb22f085788ac50c30000000000001976a9141234f920743134689a0429001406608e4981e77588ac00000000";
        byte[] bytearray = adapter.unmarshal(testHex);
        org.bitcoinj.core.Transaction transactionj = new org.bitcoinj.core.Transaction(netParams, bytearray);

        String hash1, hash2;
        hash1 = transactionj.getHashAsString();
        hash2 = testTx.getHashAsString();

        Assert.assertEquals(hash1, hash2);

    }

}
