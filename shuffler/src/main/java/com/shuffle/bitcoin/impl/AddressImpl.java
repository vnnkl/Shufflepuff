package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;

import org.bitcoinj.core.AddressFormatException;

/**
 * Created by conta on 10.03.16.
 */
public class AddressImpl implements Address {

   org.bitcoinj.core.Address address;
   String encrypted;
   boolean isEncrypted;

   public AddressImpl(org.bitcoinj.core.Address address) {
      org.bitcoinj.core.Address address1 = null;
      try {
         address1 = new org.bitcoinj.core.Address(org.bitcoinj.core.Address.getParametersFromAddress(address.toString()), address.toString());
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
      } else {
         org.bitcoinj.core.Address address1 = null;
         try {
            address1 = new org.bitcoinj.core.Address(org.bitcoinj.core.Address.getParametersFromAddress(address), address);
         } catch (AddressFormatException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
         }
         this.address = address1;
         this.isEncrypted = false;
      }
   }

   @Override
   public int hashCode() {
      int result = address != null ? address.hashCode() : 0;
      result = 31 * result + (encrypted != null ? encrypted.hashCode() : 0);
      result = 31 * result + (isEncrypted ? 1 : 0);
      return result;
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

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof AddressImpl)) {
         return false;
      }

      return address.equals(((AddressImpl)obj).address);
   }
}
