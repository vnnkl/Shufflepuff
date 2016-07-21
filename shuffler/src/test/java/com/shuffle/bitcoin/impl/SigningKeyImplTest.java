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

import java.security.NoSuchAlgorithmException;
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
    NetworkParameters testnet = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);

   @Before
   public void setUp() throws NoSuchAlgorithmException {

      this.ecKey = new ECKey();
      bitcoinCrypto = new BitcoinCrypto(TestNet3Params.get());
      this.signingKey = new SigningKeyImpl(ecKey, bitcoinCrypto.getParams());
   }

    @Test
    public void testToString() {
        String ks = ecKey.getPrivateKeyAsWiF(testnet);
        assertEquals(ks, signingKey.toString());

        assertEquals(signingKey.VerificationKey(),
                new VerificationKeyImpl(signingKey.VerificationKey().toString(), testnet));

        assertEquals(signingKey,
                new SigningKeyImpl(signingKey.toString(), testnet));
    }

   @Test
   public void testVerificationKey() {

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
         System.out.println("\n "+ signingKey.sign(hello) + "\n is a message signed by "
                 + verificationKey.toString());
      }

   }

   @Test
   public void testSomething() {
      String msg = "hello world";
      Sha256Hash hash = Sha256Hash.of(msg.getBytes());
      ECKey signingKey = new ECKey();
      ECKey verificationKey = ECKey.fromPublicOnly(signingKey.getPubKeyPoint());
      ECKey.ECDSASignature sig = signingKey.sign(hash);
      boolean isVerified = verificationKey.verify(hash, sig);
      assertTrue("msg " + msg + " can not be signed with " + Arrays.toString(sig.encodeToDER()), isVerified);
   }


    @Test
    public void testSigning() {
        Bytestring b = new Bytestring(new byte[]{
                10, 38, 67, 111, 105, 110, 83, 104, 117, 102, 102, 108, 101, 32, 83, 104,
                117, 102, 102, 108, 101, 112, 117, 102, 102, 32, 116, 101, 115, 116, 32,
                116, 101, 115, 116, 110, 101, 116, 48, 48, 26, 68, 10, 66, 48, 50, 99, 51,
                57, 100, 52, 49, 98, 102, 51, 51, 53, 101, 98, 56, 53, 97, 52, 51, 53, 52,
                48, 57, 55, 100, 51, 51, 102, 102, 100, 54, 52, 57, 102, 100, 55, 99, 53,
                52, 49, 50, 49, 56, 49, 100, 50, 102, 56, 55, 97, 99, 50, 48, 51, 56, 48,
                54, 102, 102, 54, 100, 50, 55, 99, 55, 34, 68, 10, 66, 48, 50, 99, 51, 57,
                100, 52, 49, 98, 102, 51, 51, 53, 101, 98, 56, 53, 97, 52, 51, 53, 52, 48,
                57, 55, 100, 51, 51, 102, 102, 100, 54, 52, 57, 102, 100, 55, 99, 53, 52, 49,
                50, 49, 56, 49, 100, 50, 102, 56, 55, 97, 99, 50, 48, 51, 56, 48, 54, 102, 102,
                54, 100, 50, 55, 99, 55, 40, 7, 50, -13, 2, 50, 70, 18, 68, 10, 66, 48, 50, 99,
                51, 57, 100, 52, 49, 98, 102, 51, 51, 53, 101, 98, 56, 53, 97, 52, 51, 53, 52,
                48, 57, 55, 100, 51, 51, 102, 102, 100, 54, 52, 57, 102, 100, 55, 99, 53, 52, 49,
                50, 49, 56, 49, 100, 50, 102, 56, 55, 97, 99, 50, 48, 51, 56, 48, 54, 102, 102, 54,
                100, 50, 55, 99, 55, 58, -88, 2, 50, 70, 18, 68, 10, 66, 48, 51, 99, 54, 54, 51, 98,
                50, 50, 100, 98, 98, 97, 100, 48, 101, 53, 55, 55, 52, 98, 50, 100, 57, 49, 99, 54,
                51, 57, 56, 56, 100, 98, 99, 99, 100, 49, 56, 102, 53, 100, 54, 48, 50, 52, 48, 49,
                97, 101, 99, 98, 55, 53, 52, 54, 102, 57, 98, 55, 100, 100, 49, 99, 50, 53, 50, 58,
                -35, 1, 50, 70, 18, 68, 10, 66, 48, 50, 48, 50, 97, 56, 99, 54, 97, 55, 100, 102,
                48, 53, 55, 99, 99, 101, 55, 54, 50, 99, 53, 101, 56, 100, 48, 100, 55, 50, 55,
                54, 56, 98, 56, 101, 100, 99, 50, 98, 52, 49, 56, 101, 102, 98, 51, 98, 53, 51,
                48, 54, 99, 99, 98, 100, 50, 53, 100, 57, 56, 48, 100, 49, 101, 50, 58, -110, 1,
                50, 70, 18, 68, 10, 66, 48, 50, 98, 51, 100, 97, 56, 99, 48, 51, 100, 97,
                99, 49, 97, 48, 57, 57, 98, 48, 101, 55, 51, 56, 50, 98, 99, 56, 99, 100, 101,
                51, 51, 54, 51, 57, 48, 100, 52, 50, 97, 102, 99, 48, 57, 52, 55, 50, 54,
                48, 97, 97, 52, 54, 57, 102, 99, 49, 97, 97, 48, 51, 48, 48, 56, 102, 58,
                72, 50, 70, 18, 68, 10, 66, 48, 50, 56, 97, 50, 97, 57, 53, 50, 55, 54,
                49, 51, 51, 101, 50, 98, 54, 97, 52, 99, 54, 100, 50, 56, 50, 54, 98, 52,
                57, 97, 52, 99, 49, 52, 102, 101, 51, 49, 55, 102, 99, 101, 50, 50, 56,
                53, 100, 101, 98, 53, 49, 98, 53, 52, 57, 52, 50, 53, 98, 98, 97, 49, 97, 53, 50});
        SigningKey key = new SigningKeyImpl(
                "cRT6Vk7qHrJicYtL1cdTkR71A8YDnftjLdhV4r9tAgYqeG7ZPhYk", testnet);

        key.sign(b);
    }
}