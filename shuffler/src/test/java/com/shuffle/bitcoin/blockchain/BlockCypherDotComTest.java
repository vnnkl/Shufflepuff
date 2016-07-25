package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;
<<<<<<< HEAD
import org.bitcoinj.core.Transaction;
=======
import org.junit.Assert;
>>>>>>> 480ef20c728ebb15bdd59fbdeed45df21ab24085
import org.junit.Test;

import java.util.List;

/**
 * Created by conta on 25.07.16.
 */
public class BlockCypherDotComTest {
   BlockCypherDotCom blockCypherTest = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_TESTNET),3);
   BlockCypherDotCom blockCypherMain = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_MAINNET),3);
   @Test
   public void testGetAddressBalance() throws Exception {
      // conncect to testnet and 3 peers
      // send some btc there, could get empty sometime
      long balanceTest = blockCypherTest.getAddressBalance("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      System.out.println("balance Test: "+balanceTest);
      // 1BitcoinEaterAddressDontSendf59kuE should always have a positive balance
      long balanceMain = blockCypherMain.getAddressBalance("1BitcoinEaterAddressDontSendf59kuE");
      System.out.println("balance Main: "+balanceMain);
      BlockCypherDotCom blockCypher = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_TESTNET),3);
      long balance = blockCypher.getAddressBalance("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      Assert.assertEquals(500000, balance);
      System.out.println("balance : "+balance);
   }

   @Test
   public void testGetAddressTransactions() throws Exception {
      List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListTest = blockCypherTest.getAddressTransactions("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListMain = blockCypherMain.getAddressTransactions("1BitcoinEaterAddressDontSendf59kuE");
      System.out.println(transactionListTest.toString());
      System.out.println(transactionListMain.toString());
   }

   @Test
   public void testGetTransaction() throws Exception {
      // tx a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3 from n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc
      Transaction transactionTest = blockCypherTest.getTransaction("a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3");
      // tx fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735 from 1BitcoinEaterAddressDontSendf59kuE
      Transaction transactionMain = blockCypherMain.getTransaction("fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735");
   }
}