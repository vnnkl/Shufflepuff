package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;

import java.util.Arrays;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   private final ECKey ecKey;
   private final byte[] vKey;
   private final NetworkParameters params;
   private final Address address;

   public VerificationKeyImpl(byte[] ecKey, NetworkParameters params) {
      this.ecKey = ECKey.fromPublicOnly(ecKey);
      this.vKey = this.ecKey.getPubKey();
      this.params = params;
      this.address = new AddressImpl(this.ecKey.toAddress(params));
   }

   public VerificationKeyImpl(String string, NetworkParameters params) {
      // TODO
      throw new IllegalArgumentException();
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
      return address;
   }

   @Override
   public int compareTo(Object o) {
      if (!(o instanceof VerificationKeyImpl && o.getClass() == this.getClass())) {
         throw new IllegalArgumentException("unable to compare with other VerificationKey");
      }
      //get netParams to create right address and check by address.
      org.bitcoinj.core.Address a = ((VerificationKeyImpl) o).ecKey.toAddress(params);
      return a.compareTo(this.ecKey.toAddress(params));
   }

   @Override
   public int hashCode() {
      int result = ecKey.hashCode();
      result = 31 * result + Arrays.hashCode(vKey);
      result = 31 * result + params.hashCode();
      return result;
   }

}
