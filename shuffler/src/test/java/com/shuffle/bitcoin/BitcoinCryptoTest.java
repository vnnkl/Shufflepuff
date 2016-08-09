package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.BitcoinCrypto;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by conta on 28.06.16.
 */
public class BitcoinCryptoTest {


   SigningKey signingKey;
   DecryptionKey decryptionKey;
   NetworkParameters testnet3 = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
   NetworkParameters mainnet = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
   BitcoinCrypto bitcoinCryptoNoP = getBitcoinCrypto();


   public BitcoinCrypto getBitcoinCrypto() {
      try {
         return bitcoinCryptoNoP = new BitcoinCrypto(testnet3);
      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      } catch (BitcoinCrypto.Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }

   }



   @Test
   public void testGetParams() throws Exception {
      assertEquals("Test that default params are testnet",NetworkParameters.fromID(NetworkParameters.ID_TESTNET),bitcoinCryptoNoP.getParams());
      // assertEquals("Test for mainnet params that got parsed to constructor ",NetworkParameters.fromID(NetworkParameters.ID_MAINNET),bitcoinCryptoMain.getParams());
      assertEquals(NetworkParameters.ID_TESTNET,bitcoinCryptoNoP.getParams().getId());
      System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic());
   }


   @Test
   public void testIsValidAddress() throws Exception {

      String privMain = "1NRkTbEo8z7qj9KiGAENKTuUZzs6kszZqC";
      String privTest = "my316CQbTLXFut87FXT8xTzsJLjwCUXKQH";

      assertTrue(BitcoinCrypto.isValidAddress(privTest, testnet3));
      assertTrue(BitcoinCrypto.isValidAddress(privMain, mainnet));

      assertFalse(BitcoinCrypto.isValidAddress(privMain, testnet3));
      assertFalse(BitcoinCrypto.isValidAddress(privTest, mainnet));
      System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic());
   }

   @Test
   public void testLoadPrivateKey() throws Exception {
      String privK = "MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgk4OP0krnEkP5IkAvzH3HEXalM2VVIb3EaDk8zDU1ypWgBwYFK4EEAAqhRANCAAScJ+9oHg9jufttpUDJeJuxD36qDcJzIn7X7/kjrhCjhRzArEe0dzTE/kTS02hGHsX9OtleBaxBjJxGCIAeKh0e";
      PrivateKey privateKey = BitcoinCrypto.loadPrivateKey(privK);
      System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic());
   }

   @Test(expected=GeneralSecurityException.class)
   public void testWrongArgsPrivK() throws GeneralSecurityException {
      String privK = "MIGNAgEAMBAGByKBHYwdAIBAQQgk4OP0krnEkP5IkAvzH3HEXalM2VVIb3EaDk8zDU1ypWgBwYFK4EEAAqhRANCAAScJ";
      PrivateKey privateKey = BitcoinCrypto.loadPrivateKey(privK);
   }



   @Test
   public void testLoadPublicKey() throws Exception {
      String pubK = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCfvaB4PY7n7baVAyXibsQ9+qg3CcyJ+1+/5I64Qo4UcwKxHtHc0xP5E0tNoRh7F/TrZXgWsQYycRgiAHiodHg==";
      PublicKey publicKey = BitcoinCrypto.loadPublicKey(pubK);
   }

    @Test(expected=IllegalArgumentException.class)
    public void testWrongArgsPubK() throws GeneralSecurityException {
        String pub = "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCfvaB4PY7n7baVAyXibs5I64Qo4UcwKxHtHc0xP5E0tNoRh7F/TrZXgWsQYycRgiAHiodHg==";
        PublicKey publicKey = BitcoinCrypto.loadPublicKey(pub);
    }


    @Test
    public void testMakeDecryptionKey() throws NoSuchAlgorithmException {

        // generate Keypair to compare
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES", new BouncyCastleProvider());

    }

   @Test
   public void testgetRandom() throws Exception {
      // get a lot of numbers and make sure they change?

      // make sure it is within our boundary
      assertTrue(bitcoinCryptoNoP.getRandom(5)<6);
      assertTrue(bitcoinCryptoNoP.getRandom(15)<16);
      assertTrue(bitcoinCryptoNoP.getRandom(25)<26);
      assertTrue(bitcoinCryptoNoP.getRandom(35)<36);
      // same with main
     /** assertTrue(bitcoinCryptoMain.getRandom(5)<6);
      assertTrue(bitcoinCryptoMain.getRandom(15)<16);
      assertTrue(bitcoinCryptoMain.getRandom(25)<26);
      assertTrue(bitcoinCryptoMain.getRandom(35)<36);
      assertTrue(bitcoinCryptoMain.getRandom(0)<1); **/
   }

   @Test(expected=IllegalArgumentException.class)
   public void testWrongArgs(){
      assertFalse(bitcoinCryptoNoP.getRandom(-4)<4);
   }

   @Test
   public void testMakeSigningKey() throws Exception {

      // something to compare to
      KeyChainGroup keyChainTestGroup = new KeyChainGroup(bitcoinCryptoNoP.getParams());
     // KeyChainGroup keyChainMainGroup = new KeyChainGroup(bitcoinCryptoMain.getParams());

      // get one key to compare for each
      SigningKey signingKeyTest = bitcoinCryptoNoP.makeSigningKey();
     // SigningKey signingKeyMain = bitcoinCryptoMain.makeSigningKey();

      // compare
      assert (signingKeyTest instanceof SigningKey);
      // assert (signingKeyMain instanceof SigningKey);
      System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic());
   }

   @Test
   public void testgetKeychainMnemonic() throws Exception {
      System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic().toString().replaceAll(",",""));
      for (int i = 0; i<5;i++){
      System.out.println(bitcoinCryptoNoP.makeSigningKey().VerificationKey().address().toString());
      }
   }

   @Test
   public void testSend() throws Exception {

      KeyChainGroup momChain = new KeyChainGroup(bitcoinCryptoNoP.getParams(),new DeterministicSeed("mom mom mom mom mom mom mom mom mom mom mom mom",null,"",0));
      BitcoinCrypto bitcoinCrypto = new BitcoinCrypto(bitcoinCryptoNoP.getParams(),momChain);
         // no funds? that gets you an exception!
      Wallet wallet = new Wallet(bitcoinCrypto.getParams(),momChain);
      wallet.allowSpendingUnconfirmedTransactions();

      System.out.println(wallet.currentReceiveAddress().toString()+wallet.getIssuedReceiveAddresses().toString()+wallet.getKeyChainSeed().getMnemonicCode().toString()+" "+wallet.getIssuedReceiveAddresses()+" "+wallet.getBalance().toPlainString()+bitcoinCrypto.getKeyChainMnemonic());
      org.bitcoinj.core.Transaction sentTransaction = bitcoinCrypto.send("n2ooxjPCQ19f56ivrCBq93DM6a71TA89bc",10000);
      System.out.println(sentTransaction.getHashAsString());
   }

   @Test
   public void testGetKeyChainMnemonic() throws Exception {
         System.out.println(bitcoinCryptoNoP.getKeyChainMnemonic().toString());
   }

   @Test
   public void testHexStringToByteArray() throws Exception {

   }

   @Test
   public void testGetRecommendedFee() throws Exception {
      System.out.println(bitcoinCryptoNoP.getRecommendedFee().toString());
   }
    /*@Test
    public void testWif() throws AddressFormatException {
        String key = "cRT6Vk7qHrJicYtL1cdTkR71A8YDnftjLdhV4r9tAgYqeG7ZPhYk";
        byte[] expected = new byte[]{115, 111, -51, -41, -46, 15, -68, 58, -51,
                103, -119, 46, 77, -66, 45, 40, 11, -79, -49, -26, -99, 73,
                24, 40, -73, -6, 6, 115, 124, -70, 67, 110, 1};
        byte[] decoded = BitcoinCrypto.ImportWif(key).bytes;
        Assert.assertTrue(Arrays.equals(expected, decoded));
        System.out.println(decoded);
    }*/
}