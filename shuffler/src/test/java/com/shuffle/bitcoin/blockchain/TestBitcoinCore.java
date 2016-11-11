package com.shuffle.bitcoin.blockchain;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.impl.TransactionHash;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by nsa on 10/25/16.
 */
public class TestBitcoinCore {

    NetworkParameters netParams = TestNet3Params.get();
    BitcoinCore testCase;

    public TestBitcoinCore() throws MalformedURLException, BitcoindException, CommunicationException {
        testCase = new BitcoinCore(netParams, "admin", "pass");
    }

    @Test
    public void isUtxo() throws IOException, BitcoindException, CommunicationException {

        // TestNetTX: 088e057abb28fa4cba570ee3ce9e2e28de390136e2b6139dc4109810c0b0ad6d 0
        // MainNetTX: 4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a 0
        //boolean testUtxo = testCase.isUtxo("8acc615b65c0d0c1ff255e3a316f69706a495aab426ee448506f99d5a2629598", 6);

        String utxo;
        if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            utxo = "088e057abb28fa4cba570ee3ce9e2e28de390136e2b6139dc4109810c0b0ad6d";
        } else
            utxo = "4b08898741a647183c878bd4018daa998f3019b42515cf23a70e0c89aefbab3a";
        boolean testUtxo = testCase.isUtxo(utxo, 0);
        Assert.assertTrue(testUtxo);
    }

    @Test
    public void testGetTransaction() throws IOException {
        TransactionHash txHash;
        if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            txHash = new TransactionHash("088e057abb28fa4cba570ee3ce9e2e28de390136e2b6139dc4109810c0b0ad6d");
        } else
            txHash = new TransactionHash("820c1c38df7e5b87b8890fa08c9a974d50986df8d0efe296a3e0cd4f18c58284");

        Transaction testTx = testCase.getTransaction(txHash);

        HexBinaryAdapter adapter = new HexBinaryAdapter();
        String testHex;
        if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
            testHex = "01000000013fc21f3c97c2c0c82c095a9eb41834479aaff2ab3a4d8e87713c3f6ba7714669190000006a47304402200819c3236dde8f40478ed68b59e9e48ae2cf4046d80d4b4e1e317c3b9f0a512e022024fa6a767a7b1b0c2109aea4eda834bc6552b823149fedea81514a6cef0bcc5a01210218b9413e7a3197d63dd25058dd60fa8afdf2f7b34555b1fe136143b0e2fb6d0bffffffff02902f5009000000001976a91454cd0f03dc510317643ce7048e04904486169db588acd02dba4a020000001976a914816f01f08285041ec0a9f74d45c0cd1bcad5dfb688ac00000000";
        } else
            testHex = "01000000013eac7c60e861e43937079782550831cd1bd504e800e77ee3c581ae68ee0935bf000000006b483045022100e1be72792ce08cbb0f5fdb74b363c59e35cc873989b7cb901aeccaa4985ae4ba02204e88630994c4a4d10c628ba3195b0536b3832f0526753d8e37810de2f75463ae012103f0e89ee335835056fcc265d43553ba3e86f0ff81a030eb2c494e2a1934f7d2d2ffffffff02fc350000000000001976a914a5a73ae92105d02c76a846f7345cde5eb22f085788ac50c30000000000001976a9141234f920743134689a0429001406608e4981e77588ac00000000";


        byte[] bytearray = adapter.unmarshal(testHex);
        org.bitcoinj.core.Transaction transactionj = new org.bitcoinj.core.Transaction(netParams, bytearray);

        String hash1, hash2;
        hash1 = transactionj.getHashAsString();
        hash2 = testTx.getHashAsString();

        Assert.assertEquals(hash1, hash2);

    }

}
