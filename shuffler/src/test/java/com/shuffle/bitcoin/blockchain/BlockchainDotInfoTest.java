package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

/**
 * Created by conta on 25.07.16.
 */
public class BlockchainDotInfoTest {

   BlockchainDotInfo bci = new BlockchainDotInfo(MainNetParams.get(), 0);

   @Test
   public void testIsUtxo() throws Exception {
      // https://blockchain.info/rawtx/aa4d70d6bd8533ece17a45f5e83d382b83ef5da0c7d51ab38c39f510f7aa6d32
      // vout or n 0 currently spent, 1 is not
      Assert.assertFalse(bci.isUtxo("aa4d70d6bd8533ece17a45f5e83d382b83ef5da0c7d51ab38c39f510f7aa6d32", 0));
      Assert.assertTrue(bci.isUtxo("5a835dc498b33a60d3b41c0412bd81ee829137c560d763c1635f0697d9e5cffa", 1));

   }

   @Test
   public void testGetTransactionFromUtxoInner() throws Exception {
      HashSet<TransactionOutPoint> utxos = new HashSet<TransactionOutPoint>();
      TransactionOutPoint outPoint = new TransactionOutPoint(MainNetParams.get(), 0, Sha256Hash.wrap("aa4d70d6bd8533ece17a45f5e83d382b83ef5da0c7d51ab38c39f510f7aa6d32"));
      utxos.add(outPoint);

      // hex of tx aa4d70d6bd8533ece17a45f5e83d382b83ef5da0c7d51ab38c39f510f7aa6d32
      byte[] payload = Hex.decode("01000000014bacac492084384e1207ccffa11a16e2a204bef581bc4f22b158b1208e553082000000006a473044022026ac37434b294f3cb7a3cc26f441376478ba6b92e9c11ae352b821138c6d770102202b428e3103f7dff316f675d07cf6b9e7d2ece17fbb4a686cff89cc996cf9845f012102663fa406fc79a4834732fd72141d7ef029b7be8a80f6372063678f820b104831feffffff02a5331f06000000001976a91431532c57ceef733b7c63100ca9169297605e09e588acbc890f00000000001976a9143b34d8fee18c6e9d3a23266040cbd56c0c0af57888ac70b60600");
      Transaction testTx = new Transaction(MainNetParams.get(), payload);
      List<Bitcoin.Transaction> tList = bci.getTransactionsFromUtxosInner(utxos);
      Assert.assertTrue(tList.get(0).equals(testTx));
   }

   @Test
   public void testGetAddressBalance() throws Exception {
      // conncect to testnet and 3 peers
      // send some btc there, could get empty sometime
      long balanceTest = bci.getAddressBalance("1Q2TWHE3GMdB6BZKafqwxXtWAWgFt5Jvm3");
      System.out.println("balance Test: " + balanceTest);
      // 1Q2TWHE3GMdB6BZKafqwxXtWAWgFt5Jvm3 should have a positive balance as Hal Finney is frozen in time, so his balance is likely as well

      long balance = bci.getAddressBalance("1Q2TWHE3GMdB6BZKafqwxXtWAWgFt5Jvm3");
      Assert.assertEquals(3333700, balance);
      System.out.println("balance : " + balance);
   }

   @Test
   public void testGetTransaction() throws Exception {
      // tx a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3 from n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or 1f9f22ae425c379e650621025483741d841c555d7f5f23f8c11b02684fe3158c
      Transaction otherTransaction = new Transaction(MainNetParams.get(), Hex.decode("01000000014bacac492084384e1207ccffa11a16e2a204bef581bc4f22b158b1208e553082000000006a473044022026ac37434b294f3cb7a3cc26f441376478ba6b92e9c11ae352b821138c6d770102202b428e3103f7dff316f675d07cf6b9e7d2ece17fbb4a686cff89cc996cf9845f012102663fa406fc79a4834732fd72141d7ef029b7be8a80f6372063678f820b104831feffffff02a5331f06000000001976a91431532c57ceef733b7c63100ca9169297605e09e588acbc890f00000000001976a9143b34d8fee18c6e9d3a23266040cbd56c0c0af57888ac70b60600"));
      Transaction transactionTest = bci.getTransaction("aa4d70d6bd8533ece17a45f5e83d382b83ef5da0c7d51ab38c39f510f7aa6d32");
      Assert.assertEquals(otherTransaction.getHashAsString(), transactionTest.getHashAsString());
      // tx fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735 from 1BitcoinEaterAddressDontSendf59kuE
      //Transaction transactionMain = blockCypherMain.getTransaction("fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735");
   }

}