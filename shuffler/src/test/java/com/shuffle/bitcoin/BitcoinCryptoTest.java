package com.shuffle.bitcoin;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by conta on 28.06.16.
 */
public class BitcoinCryptoTest {

   BitcoinCrypto bitcoinCrypto;
   SigningKey signingKey;
   DecryptionKey decryptionKey;
   BitcoinCrypto bitcoinCryptoNoP = new BitcoinCrypto();
   BitcoinCrypto bitcoinCryptoMain = new BitcoinCrypto(NetworkParameters.fromID(NetworkParameters.ID_MAINNET));

   @Before
   public void setUp(){

      // create testnet crypto class
      bitcoinCrypto = new BitcoinCrypto(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));

      //make signing key
      signingKey = bitcoinCrypto.makeSigningKey();
      System.out.println("\n SigningKey: "+signingKey.toString()+ " Verification: "+signingKey.VerificationKey().toString()
      +" Address: "+signingKey.VerificationKey().address().toString());

      decryptionKey = bitcoinCrypto.makeDecryptionKey();
      System.out.println("\n Decryption: "+decryptionKey.toString()+ "\nEncryption: "+decryptionKey.EncryptionKey().toString());
   }

   @Test
   public void testGetParams() throws Exception {
      assertEquals("Test that default params are testnet",NetworkParameters.fromID(NetworkParameters.ID_TESTNET),bitcoinCryptoNoP.getParams());
      assertEquals("Test for mainnet params that got parsed to constructor ",NetworkParameters.fromID(NetworkParameters.ID_MAINNET),bitcoinCryptoMain.getParams());
      assertEquals(NetworkParameters.ID_TESTNET,bitcoinCrypto.getParams().getId());
   }

   @Test
   public void testValidateBitcoinAddress() throws Exception {

   }

   @Test
   public void testIsValidAddress() throws Exception {

   }

   @Test
   public void testLoadPrivateKey() throws Exception {

   }

   @Test
   public void testLoadPublicKey() throws Exception {

   }

   @Test
   public void testSavePrivateKey() throws Exception {

   }

   @Test
   public void testSavePublicKey() throws Exception {

   }

   @Test
   public void testGetCurrentPathAsString() throws Exception {

   }

   @Test
   public void testMakeDecryptionKey() throws Exception {

      // generate Keypair to compare
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES", new BouncyCastleProvider());
      KeyPair keyPair = keyPairGenerator.genKeyPair();

      // generate keypair using our class to test against


   }

   @Test
   public void testgetRandom() throws Exception {
      // get a lot of numbers and make sure they change?

      // make sure it is within our boundary
      assertTrue(bitcoinCryptoNoP.getRandom(5)<5);
      assertTrue(bitcoinCryptoNoP.getRandom(15)<15);
      assertTrue(bitcoinCryptoNoP.getRandom(25)<25);
      assertTrue(bitcoinCryptoNoP.getRandom(35)<35);
      // same with main
      assertTrue(bitcoinCryptoMain.getRandom(5)<5);
      assertTrue(bitcoinCryptoMain.getRandom(15)<15);
      assertTrue(bitcoinCryptoMain.getRandom(25)<25);
      assertTrue(bitcoinCryptoMain.getRandom(35)<35);
   }

   @Test(expected=IllegalArgumentException.class)
   public void testWrongArgs(){
      assertFalse(bitcoinCryptoNoP.getRandom(-4)<4);
   }

   @Test
   public void testMakeSigningKey() throws Exception {

      // something to compare to
      KeyChainGroup keyChainTestGroup = new KeyChainGroup(bitcoinCryptoNoP.getParams());
      KeyChainGroup keyChainMainGroup = new KeyChainGroup(bitcoinCryptoMain.getParams());

      // get one key to compare for each
      SigningKey signingKeyTest = bitcoinCryptoNoP.makeSigningKey();
      SigningKey signingKeyMain = bitcoinCryptoMain.makeSigningKey();

      // compare
      assert (signingKeyTest instanceof SigningKey);
      assert (signingKeyMain instanceof SigningKey);

   }
}