package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by conta on 25.07.16.
 */
public class BlockCypherDotComTest {
   BlockCypherDotCom blockCypherTest = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), 3);

   //BlockCypherDotCom blockCypherMain = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_MAINNET),3);
   @Test
   public void testGetAddressBalance() throws Exception {
      // conncect to testnet and 3 peers
      // send some btc there, could get empty sometime
      long balanceTest = blockCypherTest.getAddressBalance("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      System.out.println("balance Test: " + balanceTest);
      // 1BitcoinEaterAddressDontSendf59kuE should always have a positive balance
      // long balanceMain = blockCypherMain.getAddressBalance("1BitcoinEaterAddressDontSendf59kuE");
      // System.out.println("balance Main: "+balanceMain);
      BlockCypherDotCom blockCypher = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), 3);
      long balance = blockCypher.getAddressBalance("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      Assert.assertEquals(500000, balance);
      System.out.println("balance : " + balance);
   }

   @Test
   public void testGetAddressTransactions() throws Exception {
      // for easy check n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or n2KwazAwSdE2SyPBfWQfWxSgMrmWFUu3Nx for many addresses
      List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListTest = blockCypherTest.getAddressTransactions("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      //List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListMain = blockCypherMain.getAddressTransactions("1BitcoinEaterAddressDontSendf59kuE");
      assert transactionListTest != null;
      for (Bitcoin.Transaction trans : transactionListTest) {
         Transaction ta = trans.bitcoinj();
         for (TransactionInput transInput : ta.getInputs()) {
            String parentHash = transInput.getOutpoint().getHash().toString();
            System.out.println("\nParent Hash: " + parentHash);
            Transaction addressList = blockCypherTest.getTransaction(parentHash);
            for (TransactionInput parentTansaction : addressList.getInputs()) {
               System.out.println("Parent Transaction Input: " + parentTansaction.toString());
            }
         }
      }
   }

   @Test
   public void testGetAddressAssociates() throws Exception {
      // for easy check n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or n2KwazAwSdE2SyPBfWQfWxSgMrmWFUu3Nx for many addresses
      List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListTest = blockCypherTest.getAddressTransactions("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      //List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListMain = blockCypherMain.getAddressTransactions("1BitcoinEaterAddressDontSendf59kuE");
      assert transactionListTest != null;
      for (Bitcoin.Transaction trans : transactionListTest) {
         Transaction ta = trans.bitcoinj();
         for (TransactionInput transInput : ta.getInputs()) {
            String parentHash = transInput.getOutpoint().getHash().toString();
            System.out.println("\nTransaction Inputs funding TransactionHash: " + parentHash);
            List<org.bitcoinj.core.Address> addressList = blockCypherTest.getTransactionAssociates(parentHash);
            for (org.bitcoinj.core.Address parentTansaction : addressList) {
               System.out.println("Parents Inputs Associated Addresses: " + parentTansaction.toString());
            }
         }
      }
   }


   @Test
   public void testGetTransaction() throws Exception {
      // tx a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3 from n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or 1f9f22ae425c379e650621025483741d841c555d7f5f23f8c11b02684fe3158c
      Transaction otherTransaction = new Transaction(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), Hex.decode("0100000001eb66389ecd512e19a26d74074e9be7151cbc65c7b211182693e23eaac83238c1000000006a473044022060549963655fb8d32a6f69e604db291dd40d8f35b502772bc3914b2c01c0689202207b6d83ef6f2c7805bdcbc7824be5b1a53f655115716d9413f12168079b985f1901210392567ed6709a9596778a06990ccf8dc6bf7ae6803a048e9ace5be13e26bcfb81ffffffff0220a10700000000001976a914e98ac1b3ccb0c70886575c3c20946df535c1190088aca3cd7a03000000001976a91425baeb25cf6174b4f93be0b78415146a942dc8c288ac00000000"));
      Transaction transactionTest = blockCypherTest.getTransaction("a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3");
      Assert.assertEquals(otherTransaction, transactionTest);
      // tx fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735 from 1BitcoinEaterAddressDontSendf59kuE
      //Transaction transactionMain = blockCypherMain.getTransaction("fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735");
   }
}