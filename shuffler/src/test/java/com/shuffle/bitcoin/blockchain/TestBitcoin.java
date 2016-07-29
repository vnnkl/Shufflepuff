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
            //txList.add(tx6);




            // Daniel's transactions
            byte[] bytex7 = adapter.unmarshal("0100000001d38c06624d53f0ed48d05046d485d739f3076ce701655bb9ce2157fe412cf118000000006a47304402202c0e654149820ca5e95bfda24a9ef26cc3956560f0569c7157707d0c3f784c0402204337fcd8c6f86c23416bbecb2509ad897f8576e15c47b6456645d03bb4c6af7a012102bfa469ffb4404523987c018d7ba52d3929c75fc41e7a6f635640f506d715ed84ffffffff0210339041000000001976a9140b353d33f64efb36576cd4fcf0261b1aeb5ba96688ac0046c323000000001976a9143567de06aa57a188a8e9c47b3bc9778151f1a6e888ac00000000");
            org.bitcoinj.core.Transaction btx7 = new org.bitcoinj.core.Transaction(this.netParams, bytex7);
            Transaction tx7 = new Transaction("a8d1cd860df2c345fd83764413f7809fe42344a8715b03a597751f22b7a1b33d",btx7,false,true);
            //txList.add(tx7);

            byte[] bytex8 = adapter.unmarshal("01000000013db3a1b7221f7597a5035b71a84423e49f80f713447683fd45c3f20d86cdd1a8000000006a4730440220382af068eaae9d39861ca811a7d57cbe5291f3704adad248df683a7756446d7302200dc487a00944b36b8a77bb55297813da3f0982bfe2584cf24d0197286ecc8819012102b42731c4b6ec382d6889878072cd342067976eafa49e32465e44dabea3c1865affffffff020046c323000000001976a91480f6016d03ca2198d2bc74a6c887f0d1501ca8d588ac95e3cc1d000000001976a914bfb1f67bcaaa7b49979377e027ea1e693b4cac0088ac00000000");
            org.bitcoinj.core.Transaction btx8 = new org.bitcoinj.core.Transaction(this.netParams, bytex8);
            Transaction tx8 = new Transaction("a5b003f1edd4444d8de9fb86097c4790158c545c4ffcaad9a8d92fad3ff88161",btx8,false,true);
            //txList.add(tx8);

            byte[] bytex9 = adapter.unmarshal("0100000002dc9055a01974444eb293434d159f5ee321514f58b22336c8410b0789f4edc702000000006a473044022029653ab637be76a7673c561843d4686a3c9c2a6d11dbc2934a7264cbe7364d5a022025c34e05a78fc47efeef1f2f5e3390abf3133ed9910a16cbebf4f682f69acbb7012103eefd47852fe88d1f3391f2bbc1cf62d61327971e560819ea053c4c16b19680c3ffffffffcc38cfe25972f0be05da68581dba57c1f143b0069bdee2cf02a849ae47de861c000000006a47304402201d114868eeffa5f09bc6b1c6efe0c7c02516ad1186a54d330ce43564048a11ec02202bf19a775d1fa3b915fc085463a56bdbcfda48efece795eee35df0141d52b853012102da00dc5013cfe328b5c31a6e0c7c71bafbea4f8d7c10b2018808e0ca00e41e4cffffffff02fedcbe1d000000001976a914baf01e398ef7a41bb5a05733615db0ebbd1f0cdd88ac0046c323000000001976a91480e7a71a6b7c31d5be86b65b23a9e14b0658824d88ac00000000");
            org.bitcoinj.core.Transaction btx9 = new org.bitcoinj.core.Transaction(this.netParams, bytex9);
            Transaction tx9 = new Transaction("2ec2c1dec0359f25d4f25c222aa7a048686d8807934fd23bdff144827e9e73b8",btx9,false,true);
            //txList.add(tx9);

            byte[] bytex10 = adapter.unmarshal("010000000168caa4f1414da05b586b3ef8f199603d462cd80db3cbab9cb38c459259040e13000000006a4730440220151a6c5d948e67b63752cdf159c3b5f8f8317706edb1f8209463243f87e866570220340440275d0d2816d322af64fff8d71194655e241e1a54c8fdcb8770851131b70121036ef17d98ac6e3bd92a030c61bdd3becad4544b91e322edc3edf61d04d6138e88ffffffff028b825365000000001976a9144ce851eb52d6fbb76c2601c0c2f977b3bcb2178588ac0046c323000000001976a914e34284a919e5d649cf2e7e2d9f7516d7b408eda688ac00000000");
            org.bitcoinj.core.Transaction btx10 = new org.bitcoinj.core.Transaction(this.netParams, bytex10);
            Transaction tx10 = new Transaction("18f12c41fe5721ceb95b6501e76c07f339d785d44650d048edf0534d62068cd3",btx10,false,true);
            //txList.add(tx10);

            byte[] bytex11 = adapter.unmarshal("0100000001fe7b407130ea292e02e592b362b2e614c8415222f78669f5851090e78d359764000000006b483045022100d82bcfab85f3e6a10368ad01314e4866c8090f88b1cd8e7c98056e9611a9bc6802207d7eca055d0ffaf5c2922118f4e45d644faa685d1f7c5094403973758b325961012103a88556dd5c5cf2e1dadd3de21d9ce1a31f0cc2038f20e0e4eda17620171d9185ffffffff02f117ea05000000001976a914bfc3c9c13ef4f75468ee4871ed1fd38d20b2809088ac0065cd1d000000001976a91404324ba8ff598bc38498db83649985e5f33bd53088ac00000000");
            org.bitcoinj.core.Transaction btx11 = new org.bitcoinj.core.Transaction(this.netParams, bytex11);
            Transaction tx11 = new Transaction("78dbb096a1e1652888789737f3ed6efc383cbb7e9109b5b4aa392f77d54320ed",btx11,false,true);
            //txList.add(tx11);

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

        /*
        // Non-mining address
        // The transaction we are using for this address contains 4.42782234 BTC or 442,782,234 Satoshis
        MockAddress addr = new MockAddress("mgfCoNNDAx32p1PuRL4puqsZ3Q3wcawYqw");

        // One satoshi below the transaction amount
        Assert.assertTrue(mock.sufficientFunds(addr, 442782233l));

        // Equal to the transaction amount
        Assert.assertTrue(mock.sufficientFunds(addr, 442782234l));

        // One satoshi above the transaction amount
        Assert.assertTrue(!mock.sufficientFunds(addr, 442782235l));
        */


        /*
        // Daniel's addresses
        MockAddress addr = new MockAddress("mkPLY3fiDgynpt5XfMLjd4LXtQT9sCM5Sr");
        Assert.assertTrue(mock.sufficientFunds(addr, 600000000l));

        MockAddress addr = new MockAddress("msGqVNhbS5GjafQhmN49gvwLN1hsfik47T");
        Assert.assertTrue(mock.sufficientFunds(addr, 600000000l));

        MockAddress addr = new MockAddress("msGYJ87B6bxjmsUW6uYrsfTHq7Am1qesot");
        Assert.assertTrue(mock.sufficientFunds(addr, 600000000l));

        MockAddress addr = new MockAddress("n2EbNN9e8nG6pATMURT998tu6AF74t88qh");
        Assert.assertTrue(mock.sufficientFunds(addr, 600000000l));

        MockAddress addr = new MockAddress("mfu9FR9rugkYZLDGMh12ACpmicdaFuYkf1");
        Assert.assertTrue(mock.sufficientFunds(addr, 500000001l));
        */

    }
}
