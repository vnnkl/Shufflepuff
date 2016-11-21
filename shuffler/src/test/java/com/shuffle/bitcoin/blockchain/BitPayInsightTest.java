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
public class BitPayInsightTest {

   // BitPayInsight BitpayInsightTestnetTest = new BitPayInsight(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), 3);

   //MainNet tx:  9d9cc3995326848839c6117ad9b0fc9464890b866dcdda11e249cee323eab139
   //TestNet tx: 1f9f22ae425c379e650621025483741d841c555d7f5f23f8c11b02684fe3158c

   Insight insightMainnetTest = new Insight(NetworkParameters.fromID(NetworkParameters.ID_MAINNET), 3);

   @Test
   public void testGetAddressBalance() throws Exception {
      // conncect to testnet and 3 peers
      // send some btc there, could get empty sometime
      long balanceTest = insightMainnetTest.getAddressBalance("1CL5zkcneQ1uHpK8f3zcTAnC1GbLdKo1vn");
      System.out.println("balance Test: " + balanceTest);
      // 1BitcoinEaterAddressDontSendf59kuE should always have a positive balance
      // long balanceMain = blockCypherMain.getAddressBalance("1BitcoinEaterAddressDontSendf59kuE");
      // System.out.println("balance Main: "+balanceMain);
      long balance = insightMainnetTest.getAddressBalance("1CL5zkcneQ1uHpK8f3zcTAnC1GbLdKo1vn");
      Assert.assertEquals(0, balance);
      System.out.println("balance : " + balance);
   }

   @Test
   public void testGetAddressTransactions() throws Exception {
      // for easy check n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or n2KwazAwSdE2SyPBfWQfWxSgMrmWFUu3Nx for many addresses
      List<Bitcoin.Transaction> transactionListTest = insightMainnetTest.getAddressTransactionsInner("1CL5zkcneQ1uHpK8f3zcTAnC1GbLdKo1vn");
      //List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListMain = blockCypherMain.getAddressTransactions("1BitcoinEaterAddressDontSendf59kuE");
      assert transactionListTest != null;
      for (Bitcoin.Transaction trans : transactionListTest) {
         Transaction ta = trans.bitcoinj();
         for (TransactionInput transInput : ta.getInputs()) {
            String parentHash = transInput.getOutpoint().getHash().toString();
            System.out.println("\nParent Hash: " + parentHash);
            Transaction addressList = insightMainnetTest.getTransaction(parentHash);
            for (TransactionInput parentTansaction : addressList.getInputs()) {
               System.out.println("Parent Transaction Input: " + parentTansaction.toString());
            }
         }
      }
   }

   @Test
   public void testGetAddressAssociates() throws Exception {
      // for easy check n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or n2KwazAwSdE2SyPBfWQfWxSgMrmWFUu3Nx for many addresses
      List<Bitcoin.Transaction> transactionListTest = insightMainnetTest.getAddressTransactionsInner("1CL5zkcneQ1uHpK8f3zcTAnC1GbLdKo1vn");
      //List<com.shuffle.bitcoin.blockchain.Bitcoin.Transaction> transactionListMain = blockCypherMain.getAddressTransactions("1BitcoinEaterAddressDontSendf59kuE");
      assert transactionListTest != null;
      for (Bitcoin.Transaction trans : transactionListTest) {
         Transaction ta = trans.bitcoinj();
         for (TransactionInput transInput : ta.getInputs()) {
            String parentHash = transInput.getOutpoint().getHash().toString();
            System.out.println("\nTransaction Inputs funding TransactionHash: " + parentHash);

         }
      }
   }


   @Test
   public void testGetTransaction() throws Exception {
      // tx a49aa30eb850966db6c1aba5c8c725cb375d9c741c597b462388dccee16408c3 from n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc or 1f9f22ae425c379e650621025483741d841c555d7f5f23f8c11b02684fe3158c
      byte[] bytes = Hex.decode("01000000074c7064fded89b6d36336e2b43c07810d5e44977465bd48b6cabd9d90982fb919010000006a47304402204c3294dfbda958e3ba65c67bc567bbf5c244923a4b9ab10be7243f15dcee181d0220250a1395bb6981d3dade1a2bfde49d28f915b617bcf5039c61bb4831f10df4ec0121029be8675ca236d6246401c455ca3f88f3ed1bd1ad6d050abdec82b44e66b0e47effffffff3ad6c4544e6810e353494f4699fdb5c54803be0a8e026595887cdd2c08ce5b1e000000006a47304402206a4856ba012b40ea771e06c58e472d0b8b25aaec08336f4cda7ac0c7a007cdf30220121e7ad49a54221725706d434549d7a971e5a68ca7cbe0117b8109766e92755f0121030f842fe2fdd4c5ea4a137a12cbd928aad46115679750d58fd8450ef265e17ee8ffffffff40069498cbfba625cfdeb96170e17daef7e55de5b7281a8a905bb84df02b0e73010000006a47304402207f75377e6e27dc91f4c099d23051ca31eaee5b11f25613aca1d157cc5b794be402200685bdd6489cdacbae27d564d2d9b051a3f959649d77f23d87ffdf1062647b90012102a59c88ca5ff6db4876f3712351db557634ea07ea384c30c3fc953f248a6e5aeaffffffff4f0733cabf3ac6d0a132bcba21a737dab26032158b04d33f8547c3503ad413b1010000006b48304502210086d52b8bbf68ece91789be3ff5fd3abfbc16308ebd1b736f2a3ec5b14ac8fcf402206cc97da583834051c2e986c2c5c1fdca3073e7125a6f9f7478e879446712d3f60121029be8675ca236d6246401c455ca3f88f3ed1bd1ad6d050abdec82b44e66b0e47effffffff2f27996542d267afd329f08e16e33376b30e677104209af2589d9ad4f0da13ce010000006b4830450221009999c593ebf04a1964dae5690247e70dccd4e4031aa524c1f01daeb9c785206e02207714ae74bc35bf7f429457963f5d6f59068a5db45630ae9d73f7e9bfa243dff3012102f349641819a219d11a2b117e8c6ec4db8bb54e57207c433a69c01ebbb0e9732fffffffff9bc6d4505735d633c14824924b3965ccf0f14ac7a0a57bb736607b8c266715e8010000006a47304402206408062fccdf05d9b64f2bce5738166fadaebcc72f2253d0091f8b8648b9cd7f022054c3b52d08db53cb348f5d4456700e77cda7c0f44b8ef76fdc79ebbc8e3eef010121021bae302bd15d4a29ac0a65291d39fde6398d9d32af95880c799cf58df841e1e8ffffffffa04d77e70363ad7b0a359ff718ed383001d0335a19d21d3555236732024590ea010000006a47304402201940851154ffb3b598587fd09b358003a3bd09bc28e81dc9fe18325626507ae002206873d408a842cdd79fa6a641ec92e98f2cee70dd2536dbd2696de3af9eba4f4b012102a59c88ca5ff6db4876f3712351db557634ea07ea384c30c3fc953f248a6e5aeaffffffff02b9539a02000000001976a914283d8ebe710228ce312eba71bcbe6d887751885f88ac80ac6e37000000001976a914ed6e1ed86b73739dfb7f01e4adefa47eac183af488ac00000000");
      Transaction otherTransaction = new Transaction(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), bytes);
      //BitPayInsight.TransactionWithConfirmations withConfirmations = new BitPayInsight.TransactionWithConfirmations(TestNet3Params.get(),bytes,true);
      Transaction transactionTest = insightMainnetTest.getTransaction("9d9cc3995326848839c6117ad9b0fc9464890b866dcdda11e249cee323eab139");
      Assert.assertEquals(otherTransaction, transactionTest);
      // tx fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735 from 1BitcoinEaterAddressDontSendf59kuE
      //Transaction transactionMain = blockCypherMain.getTransaction("fdb0959aed119a9cfd6696cf41716854a0d5cce5d7b19fd2417f8f148cf0b735");
   }
}