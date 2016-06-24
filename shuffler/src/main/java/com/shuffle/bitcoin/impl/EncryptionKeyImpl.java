package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

   ECKey encryptionKey;
   PublicKey publicKey;

   static {
      Security.addProvider(new BouncyCastleProvider());
   }

   public EncryptionKeyImpl(byte[] ecPubKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
      // this.encryptionKey = ECKey.fromPublicOnly(ecPubKey);

      KeyFactory keyFactory = KeyFactory.getInstance("ECIES");
      EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(ecPubKey);
      this.publicKey = keyFactory.generatePublic(publicKeySpec);
      this.encryptionKey = ECKey.fromPublicOnly(this.publicKey.getEncoded());

   }

   public EncryptionKeyImpl(PublicKey pubKey) {
      this.publicKey = pubKey;
   }


   public EncryptionKeyImpl(ECKey ecPubKey) {
      if (ecPubKey.hasPrivKey()) {
         this.encryptionKey = ECKey.fromPublicOnly(ecPubKey.getPubKey());
      } else {
         this.encryptionKey = ecPubKey;
      }
   }

   public String toString() {
      // return this.encryptionKey.getPublicKeyAsHex();
      return org.spongycastle.util.encoders.Hex.toHexString(publicKey.getEncoded());
   }

   @Override
   public Address encrypt(Address m) {
      AddressImpl add = null;
      try {

         //cast will fail, maybe
         //X509EncodedKeySpec spec = kf.getKeySpec(encryptionKey,X509EncodedKeySpec.class);
         //PublicKey pubKey = kf.generatePublic(kf.getKeySpec(((Key) encryptionKey), KeySpec.class));
         //PublicKey publicKey = BitcoinCrypto.loadPublicKey(org.spongycastle.util.encoders.Base64.toBase64String(encryptionKey.getPubKey()));
         // byte[] publicKey2 = ECKey.publicKeyFromPrivate(encryptionKey.getPrivKey(), encryptionKey.isCompressed());

         //encrypt cipher
         Cipher cipher = Cipher.getInstance("ECIES");
         cipher.init(Cipher.ENCRYPT_MODE, publicKey);
         byte[] bytes = m.toString().getBytes(StandardCharsets.UTF_8);
         byte[] encrypted = cipher.doFinal(bytes);
         add = new AddressImpl(Hex.encodeHexString(encrypted),true);
      } catch (Exception e) {
         e.printStackTrace();

      }
      return add;
   }
   

}