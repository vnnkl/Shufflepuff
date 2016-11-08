package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by conta on 31.03.16.
 */
public class VerificationKeyImpl implements VerificationKey {

   public final Address address;
   private final ECKey ecKey;
   private final byte[] vKey;
   private final NetworkParameters params;
   public ArrayList<TransactionOutPoint> utxoList = new ArrayList<>();

   public VerificationKeyImpl(byte[] ecKey, NetworkParameters params) {
      this.ecKey = ECKey.fromPublicOnly(ecKey);
      this.vKey = this.ecKey.getPubKey();
      this.params = params;
      this.address = new AddressImpl(this.ecKey.toAddress(params));
   }

   public VerificationKeyImpl(String string, NetworkParameters params) {
      // TODO
      byte[] bytes = Hex.decode(string);
      this.ecKey = ECKey.fromPublicOnly(bytes);
      this.vKey = this.ecKey.getPubKey();
      this.params = params;
      this.address = new AddressImpl(this.ecKey.toAddress(params));
   }

   public ArrayList<TransactionOutPoint> getUtxoList() {
      return utxoList;
   }

   public void setUtxoList(ArrayList<TransactionOutPoint> utxoList) {
      this.utxoList = utxoList;
   }

   public boolean addOutpoutToUtxoList(TransactionOutPoint outPoint) {
      return this.utxoList.add(outPoint);
   }

   public boolean removeOutpoutFromUtxoList(TransactionOutPoint outPoint) {
      return this.utxoList.remove(outPoint);
   }

   public boolean removeOutpoutFromUtxoList(Integer index) {
      return this.utxoList.remove(index);
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
      return vk != null
              && vk instanceof VerificationKeyImpl
              && address.equals(((VerificationKeyImpl) vk).address);

   }

   @Override
   public Address address() {
      return address;
   }

   @Override
   public int compareTo(Object o) {
      if (!(o instanceof VerificationKeyImpl)) {
         throw new IllegalArgumentException("unable to compare with other VerificationKey");
      }
      //get netParams to create right address and check by address.
      return address.toString().compareTo((((VerificationKeyImpl) o).address).toString());
   }

   @Override
   public int hashCode() {
      int result = ecKey.hashCode();
      result = 31 * result + Arrays.hashCode(vKey);
      result = 31 * result + params.hashCode();
      return result;
   }

}
