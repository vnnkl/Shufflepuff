package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;
import org.junit.Test;

/**
 * Created by conta on 25.07.16.
 */
public class BlockCypherDotComTest {

   @Test
   public void testGetAddressBalance() throws Exception {
      // conncect to testnet and 3 peers
      BlockCypherDotCom blockCypher = new BlockCypherDotCom(NetworkParameters.fromID(NetworkParameters.ID_TESTNET),3);
      long balance = blockCypher.getAddressBalance("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc");
      System.out.println("balance : "+balance);
   }

   @Test
   public void testGetAddressTransactions() throws Exception {

   }

   @Test
   public void testGetTransaction() throws Exception {

   }
}