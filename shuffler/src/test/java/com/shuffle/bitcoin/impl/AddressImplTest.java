package com.shuffle.bitcoin.impl;

import com.google.inject.Guice;
import com.shuffle.JvmModule;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

/**
 * Created by conta on 31.03.16.
 */
public class AddressImplTest {
   NetworkParameters pnpar = org.bitcoinj.params.MainNetParams.get();
   NetworkParameters tnpar = org.bitcoinj.params.TestNet3Params.get();

   SecureRandom sr = new SecureRandom();
   SecureRandom sr2 = new SecureRandom();
   ECKey ecKey = new ECKey(sr);
   ECKey ecKey2 = new ECKey(sr2);
   org.bitcoinj.core.Address address = new org.bitcoinj.core.Address(tnpar, ecKey.getPubKeyHash());
   org.bitcoinj.core.Address address2 = new org.bitcoinj.core.Address(tnpar, ecKey2.getPubKeyHash());
   Address addressi = new AddressImpl(address.toString(),false);
   Address addressi2 = new AddressImpl(address2);

   BitcoinCrypto bitcoinCrypto;
   PrivateKey privateTestKey;
   PublicKey publicTestKey;
   KeyPair testKeys;
   EncryptionKey encryptionKey;
   DecryptionKey decryptionKey;

   @Before
   public void setUp() throws Exception {
      // The module also initializes the BouncyCastle crypto
      Guice.createInjector(new JvmModule()).injectMembers(this);
      this.privateTestKey = BitcoinCrypto.loadPrivateKey("MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgk4OP0krnEkP5IkAvzH3HEXalM2VVIb3EaDk8zDU1ypWgBwYFK4EEAAqhRANCAAScJ+9oHg9jufttpUDJeJuxD36qDcJzIn7X7/kjrhCjhRzArEe0dzTE/kTS02hGHsX9OtleBaxBjJxGCIAeKh0e");
      this.publicTestKey = BitcoinCrypto.loadPublicKey("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCfvaB4PY7n7baVAyXibsQ9+qg3CcyJ+1+/5I64Qo4UcwKxHtHc0xP5E0tNoRh7F/TrZXgWsQYycRgiAHiodHg==");
      this.testKeys = new KeyPair(publicTestKey,privateTestKey);
      this.encryptionKey = new EncryptionKeyImpl(testKeys.getPublic());
      this.decryptionKey = new DecryptionKeyImpl(testKeys,
              NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
   }

   @Test
   public void testCompareTo() {
      System.out.println("Network Parameters" + tnpar.toString());
      System.out.println("address: " + address.toString());
      System.out.println("address2: " + address2.toString());
      System.out.println("addressi: " + addressi.toString());
      System.out.println("addressi2: " + addressi2.toString());
      Assert.assertEquals(0, addressi.compareTo(new AddressImpl(address.toString(),false)));

   }

   @Test
   public void multipleAddressEncryption() throws FormatException {

      bitcoinCrypto = new BitcoinCrypto(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
      Address encAddress1 = encryptionKey.encrypt(addressi);
      Address encAddress2 = encryptionKey.encrypt(encAddress1);
      Address decAddress2 = decryptionKey.decrypt(encAddress2);
      Address decAddress1 = decryptionKey.decrypt(decAddress2);
      Assert.assertEquals(addressi.toString(),decAddress1.toString());
   }

   @Test
   public void testCompareTo2() {

      Assert.assertEquals(0, addressi2.compareTo(addressi2));
   }
}