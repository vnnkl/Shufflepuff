package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;

import java.nio.charset.StandardCharsets;

/**
 * Created by conta on 10.03.16.
 */
public class SigningKeyImpl implements SigningKey {

   final ECKey signingKey;
   final NetworkParameters params;
   private final VerificationKey vk;

   public SigningKeyImpl(org.bitcoinj.core.ECKey ecKey, NetworkParameters params) {
      signingKey = ecKey;
      this.params = params;
      vk = new VerificationKeyImpl(signingKey.getPubKey(), params);
   }

   public SigningKeyImpl(String key, NetworkParameters params) throws FormatException {
      this(ECKey.fromPrivate(key.getBytes(StandardCharsets.UTF_8)), params);
   }


   // returns Private Key in WIF Compressed 52 characters base58
   public String toString() {
      return this.signingKey.getPrivateKeyAsWiF(params);
   }

   @Override
   public VerificationKey VerificationKey() {
      return vk;
   }

   @Override
   public Bytestring sign(Bytestring string) {
      ECKey.ECDSASignature ecdsaSignature = signingKey.sign(Sha256Hash.of(string.bytes));
      return new Bytestring(ecdsaSignature.encodeToDER());
   }


    @Override
    public int compareTo(Object o) {
        if (!(o instanceof SigningKeyImpl)) {
            throw new IllegalArgumentException("unable to compare with other SingingKey");
        }
        //get netParams to create correct address and check by address.
        Address a = ((SigningKeyImpl) o).signingKey.toAddress(params);
        return a.compareTo(a);
    }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      SigningKeyImpl that = (SigningKeyImpl) o;

      if (!signingKey.equals(that.signingKey)) return false;
      return params.equals(that.params);

   }

   @Override
   public int hashCode() {
      int result = signingKey.hashCode();
      result = 31 * result + params.hashCode();
      return result;
   }
}
