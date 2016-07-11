package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.nio.charset.StandardCharsets;

/**
 * Created by conta on 10.03.16.
 */
public class SigningKeyImpl implements SigningKey {

   final ECKey signingKey;
   final BitcoinCrypto bitcoinCrypto;


   public SigningKeyImpl(org.bitcoinj.core.ECKey ecKey, BitcoinCrypto bitcoinCrypto) {
      this.signingKey = ecKey;
      this.bitcoinCrypto = bitcoinCrypto;
   }

   public SigningKeyImpl(String key, BitcoinCrypto bitcoinCrypto){
      this(ECKey.fromPrivate(key.getBytes(StandardCharsets.UTF_8)),bitcoinCrypto);
   }


   // returns Private Key in WIF Compressed 52 characters base58
   public String toString() {
      return this.signingKey.getPrivateKeyAsWiF(bitcoinCrypto.getParams()).toString();
   }

   @Override
   public VerificationKey VerificationKey() {
      return new VerificationKeyImpl(signingKey.getPubKey(), bitcoinCrypto);
   }

   @Override
   public Bytestring sign(Bytestring string) {
      ECKey.ECDSASignature ecdsaSignature = signingKey.sign(Sha256Hash.of(string.bytes));
      return new Bytestring(ecdsaSignature.encodeToDER());
   }


   @Override
   public int compareTo(Object o) {
      if (!(o instanceof SigningKeyImpl) && o.getClass() == this.getClass()) {
         throw new IllegalArgumentException("unable to compare with other SingingKey");
      }
      //get netParams to create correct address and check by address.
      org.bitcoinj.core.Address a = ((SigningKeyImpl) o).signingKey.toAddress(bitcoinCrypto.getParams());
      return a.compareTo(((org.bitcoinj.core.Address) o));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SigningKeyImpl that = (SigningKeyImpl) o;

      if (!signingKey.equals(that.signingKey)) return false;
      return bitcoinCrypto.equals(that.bitcoinCrypto);

   }

   @Override
   public int hashCode() {
      int result = signingKey.hashCode();
      result = 31 * result + bitcoinCrypto.hashCode();
      return result;
   }
}
