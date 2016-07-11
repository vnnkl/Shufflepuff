package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.util.Arrays;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   private final ECKey ecKey;
   private final byte[] vKey;
   private final BitcoinCrypto bitcoinCrypto;

   public VerificationKeyImpl(byte[] ecKey, BitcoinCrypto bitcoinCrypto) {
      this.ecKey = ECKey.fromPublicOnly(ecKey);
      this.vKey = this.ecKey.getPubKey();
      this.bitcoinCrypto = bitcoinCrypto;
   }

   // returns PublicKey compressed, 66 chars
   public String toString() {
      return this.ecKey.getPublicKeyAsHex();
   }



   @Override
   public boolean verify(Bytestring payload, Bytestring signature) {
      ECKey.ECDSASignature ecdsaSignature;
      ecdsaSignature = ECKey.ECDSASignature.decodeFromDER(signature.bytes);
      return ECKey.verify(Sha256Hash.of(payload.bytes).getBytes(),ecdsaSignature,vKey);
   }

   @Override
   public boolean equals(Object vk) {
      if (vk.getClass() == this.getClass()) {
         VerificationKey oKey = (VerificationKey) vk;
         return this.address().equals(oKey.address());
      }
      return false;
   }

   @Override
   public Address address() {
      return new AddressImpl(ecKey.toAddress(bitcoinCrypto.getParams()));
   }

   @Override
   public int compareTo(Object o) {
      if (!(o instanceof VerificationKeyImpl && o.getClass() == this.getClass())) {
         throw new IllegalArgumentException("unable to compare with other VerificationKey");
      }
      //get netParams to create right address and check by address.
      org.bitcoinj.core.Address a = ((VerificationKeyImpl) o).ecKey.toAddress(bitcoinCrypto.getParams());
      return a.compareTo(this.ecKey.toAddress(bitcoinCrypto.getParams()));
   }

   @Override
   public int hashCode() {
      int result = ecKey.hashCode();
      result = 31 * result + Arrays.hashCode(vKey);
      result = 31 * result + bitcoinCrypto.hashCode();
      return result;
   }

}
