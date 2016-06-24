package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;

import org.bitcoinj.core.AddressFormatException;

/**
 * Created by conta on 10.03.16.
 */
public class AddressImpl implements Address {

   org.bitcoinj.core.Address address;
   String encrypted;
   boolean isEncrypted;

   public AddressImpl(org.bitcoinj.core.Address address) {
      BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();
      org.bitcoinj.core.Address address1 = null;
      try {
         address1 = new org.bitcoinj.core.Address(bitcoinCrypto.getParams(), address.toString());
      } catch (AddressFormatException e) {
         // if it fails we store in encrypted
         this.encrypted = address1.toString();
         this.isEncrypted = false;
         e.printStackTrace();
      }
      this.address = address1;
      this.isEncrypted = false;
   }

   public AddressImpl(String address, boolean encrypted) {
      if (encrypted){
            this.encrypted = address;
            this.isEncrypted = true;
      }
      else {
         BitcoinCrypto bitcoinCrypto = new BitcoinCrypto();
         org.bitcoinj.core.Address address1 = null;
         try {
            address1 = new org.bitcoinj.core.Address(bitcoinCrypto.getParams(), address);
         } catch (AddressFormatException e) {
            e.printStackTrace();
         }
         this.address = address1;
         this.isEncrypted = false;
      }
   }

   public String toString() {
      if (this.isEncrypted){
       return encrypted;
      }
      return this.address.toString();
   }

   public boolean isEncrypted(){
      return isEncrypted;
   }

   @Override
   public int compareTo(Address o) {
      if (!(o instanceof AddressImpl)) {
         throw new IllegalArgumentException("unable to compare with other address");
      }
      return address.compareTo((new AddressImpl(o.toString(),false)).address);
   }
}
