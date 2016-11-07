package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.impl.TransactionHash;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

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
    public void isUtxo() throws IOException, BitcoindException, CommunicationException {
        //boolean testUtxo = testCase.isUtxo("8acc615b65c0d0c1ff255e3a316f69706a495aab426ee448506f99d5a2629598", 6);
        boolean testUtxo = testCase.isUtxo("4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a", 0);
        Assert.assertTrue(testUtxo);
    }

    @Test
    public void testGetTransaction() throws IOException {
        TransactionHash txHash = new TransactionHash("820c1c38df7e5b87b8890fa08c9a974d50986df8d0efe296a3e0cd4f18c58284");
        Transaction testTx = testCase.getTransaction(txHash);

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
