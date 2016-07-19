package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by conta on 07.06.16.
 */
public class SigningKeyImplTest {

   ECKey ecKey;
   SigningKey signingKey;
   VerificationKey verificationKey;
   BitcoinCrypto bitcoinCrypto;

   @Before
   public void setUp() throws Exception {

      this.ecKey = new ECKey();
      bitcoinCrypto = new BitcoinCrypto(TestNet3Params.get());
      this.signingKey = new SigningKeyImpl(ecKey, bitcoinCrypto.getParams());
   }

   @Test
   public void testToString() throws Exception {
      String ks = ecKey.getPrivateKeyAsWiF(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
      assertEquals(ks, signingKey.toString());

      assertEquals(signingKey.VerificationKey(),
              new VerificationKeyImpl(signingKey.VerificationKey().toString(),
                      NetworkParameters.fromID(NetworkParameters.ID_TESTNET)));
   }

   @Test
   public void testVerificationKey() throws Exception {

      verificationKey = new VerificationKeyImpl(this.ecKey.getPubKey(), bitcoinCrypto.getParams());
      assertEquals(verificationKey.toString(), signingKey.VerificationKey().toString());
      System.out.println("Address:    "+signingKey.VerificationKey().address().toString());
      System.out.println("SigningKey: "+signingKey.toString());
      System.out.println("VerificationKey: "+signingKey.VerificationKey().toString());
      Bytestring hello = new Bytestring("Hello World".getBytes());
      System.out.println("Bytestring :"+ hello);
      System.out.println("Bytestring signed: "+ signingKey.sign(hello));
      byte[] bytes = signingKey.sign(hello).bytes;
      // deterministic should bring same result
      byte[] bytes2 = signingKey.sign(hello).bytes;
      byte[] bytes3 = signingKey.sign(hello).bytes;
      System.out.println("toHexString bytes:  "+Hex.toHexString(bytes));
      System.out.println("toHexString bytes2: "+Hex.toHexString(bytes));
      System.out.println("toHexString bytes3: "+Hex.toHexString(bytes));
      System.out.println(verificationKey.verify(hello,signingKey.sign(hello)));
      if(verificationKey.verify(hello,signingKey.sign(hello))){
         System.out.println("\n "+ signingKey.sign(hello) + "\n is a message signed by "+ verificationKey.toString());
      }

   }

   @Test
   public void testSomething() throws Exception {
      String msg = "hello world";
      Sha256Hash hash = Sha256Hash.of(msg.getBytes());
      ECKey signingKey = new ECKey();
      ECKey verificationKey = ECKey.fromPublicOnly(signingKey.getPubKeyPoint());
      ECKey.ECDSASignature sig = signingKey.sign(hash);
      boolean isVerified = verificationKey.verify(hash, sig);
      assertTrue("msg " + msg + " can not be signed with " + Arrays.toString(sig.encodeToDER()), isVerified);
   }


   @Test
   public void testCompareTo() throws Exception {

   }
}