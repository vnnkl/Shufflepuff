package com.shuffle.bitcoin.blockchain;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.mock.MockAddress;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by Daniel Krawisz on 7/26/16.
 */
public class TestBitcoin {
    private class MockBitcoin extends Bitcoin {
        public MockBitcoin() {
            super(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), 3);
        }

        @Override
        protected List<Transaction> getAddressTransactionsInner(String address) throws IOException, CoinNetworkException, AddressFormatException {
            return null;
        }

        @Override
        org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {
            return null;
        }

        @Override
        protected List<Transaction> getAddressTransactions(String address) {
            HexBinaryAdapter adapter = new HexBinaryAdapter();
            Context context = Context.getOrCreate(this.netParams);

            List<Transaction> txList = new LinkedList<>();


            /**
             * Mining addresses -- outputs are NOT all P2PKH.
             */

            byte[] bytex2 = adapter.unmarshal("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0d03491c030155062f503253482fffffffff0100f2052a010000002321020cf3600b55a8782a2ef594d8c4b1d5cdf5d29f826ae72fb8b0ceb43a1200fabeac00000000");
            org.bitcoinj.core.Transaction btx2 = new org.bitcoinj.core.Transaction(this.netParams, bytex2);
            Transaction tx2 = new Transaction("77b291dcbf6483719ca4287045ee8faef19f03b3728fc8b0f697296b9da9978e",btx2,false,true);
            //txList.add(tx2);

            byte[] bytex3 = adapter.unmarshal("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0e03d01c0302fa21062f503253482fffffffff011019062a01000000232102a525e0acc35f97b9b742f3d881910951c4e5303fe5e295ad723d08893aa86942ac00000000");
            org.bitcoinj.core.Transaction btx3 = new org.bitcoinj.core.Transaction(this.netParams, bytex3);
            Transaction tx3 = new Transaction("f4a4a742ef159e93e1133f85079536f7f7c0ff467a7b7fbf47cdeb72d30fcbb3",btx3,false,true);
            //txList.add(tx3);

            // This transaction works for sufficientFunds because the output address is P2PKH
            byte[] bytex4 = adapter.unmarshal("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff2503ca7b04184b6e434d696e657242510cb670ab1c4b086d542ee8d32b8d01000000d4040000ffffffff0100f90295000000001976a9149e8985f82bc4e0f753d0492aa8d11cc39925774088ac00000000");
            org.bitcoinj.core.Transaction btx4 = new org.bitcoinj.core.Transaction(this.netParams, bytex4);
            Transaction tx4 = new Transaction("47d37f362f400cec3e9366b0701b2c54a8a85aba0ceda48c8d27343031e7f2ae",btx4,false,true);
            //txList.add(tx4);

            byte[] bytex5 = adapter.unmarshal("01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0d03ca9504015c062f503253482fffffffff0110fd0295000000002321030fff984dad779eb2804c73ae41038b3db2402dd33e64753f2eab5ad37784792eac00000000");
            org.bitcoinj.core.Transaction btx5 = new org.bitcoinj.core.Transaction(this.netParams, bytex5);
            Transaction tx5 = new Transaction("635635211f84e5e89bb197ccf514c531e51f5ca2ba8ce4da720250b4a29f3478",btx5,false,true);
            //txList.add(tx5);


            /**
             * Non-mining transaction -- sufficientFunds works
             */

            byte[] bytex6 = adapter.unmarshal("0100000001d7b3d434fad5961b5a1ca8083b9c83f8bc7ba283c027dc34cbea7a42fefc7ebe010000006b483045022100ee44af88e7295dfe6ab0f556c84bb722fea7271626f79dc914f5a1b4991f51a102204fd79b0a062cadad1682aed8094bf639f957f0e43d9337145f48521c8509ba92012102230d12de280ef92660c166054b194d09000e8328dbb33ef2667d1dd1ccf52369feffffff02c0fc9b01000000001976a91423e077ffac6f109795a82021dc1698bd9ce4011988ac1a52641a000000001976a9140c878fdf88351cc9f4ced15df3aea8183b604f0688acd5cb0800");
            org.bitcoinj.core.Transaction btx6 = new org.bitcoinj.core.Transaction(this.netParams, bytex6);
            Transaction tx6 = new Transaction("20b2ec5e7d8127241230104d6c776f3bb87d5a20c2ba9e9d92f4658017181201",btx6,false,true);
            txList.add(tx6);

            return txList;
        }

    }

    // TODO
    @Test
    public void testSufficientFunds() throws Exception {
        MockBitcoin mock = new MockBitcoin();

        /**
         * The first five are mining addresses
         */

        // NullPointerException
        //MockAddress addr = new MockAddress("mhsU4iQ2XoFvgZck69z6zdyMMs9PYwNzof");

        // NullPointerException
        //MockAddress addr = new MockAddress("mhFYAUik5P3BBc2KuKnhKs23DdoXCKXWUQ");

        // works? For some reason, the output address is P2PKH.
        //MockAddress addr = new MockAddress("muyDoehpBExCbRRXLtDUpw5DaTb33UZeyG");

        // NullPointerException
        //MockAddress addr = new MockAddress("mkubqAKU27qfvE5DFsr1H8ZyvoFAm666Ns");

        /**
         * Can only test non-mining addresses -- output.getAddressFromP2PKHScript only returns non-mining transactions
         * Returns null if the output script is not the form OP_DUP OP_HASH160 OP_EQUALVERIFY OP_CHECKSIG, i.e. P2PKH
         */

        // Non-mining address
        // The transaction we are using for this address contains 4.42782234 BTC or 442,782,234 Satoshis
        MockAddress addr = new MockAddress("mgfCoNNDAx32p1PuRL4puqsZ3Q3wcawYqw");

        // One satoshi below the transaction amount
        Assert.assertTrue(mock.sufficientFunds(addr, 442782233l));

        // Equal to the transaction amount
        Assert.assertTrue(mock.sufficientFunds(addr, 442782234l));

        // One satoshi above the transaction amount
        Assert.assertTrue(!mock.sufficientFunds(addr, 442782235l));

    }
}
