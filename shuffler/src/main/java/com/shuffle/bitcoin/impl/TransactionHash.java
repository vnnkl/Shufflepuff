package com.shuffle.bitcoin.impl;


import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

/**
 * Created by conta on 02.11.16.
 */

public class TransactionHash extends Sha256Hash {
   private final byte[] bytes;
   private final Sha256Hash hash;

   public TransactionHash(String string) {
      super(string);
      this.hash = Sha256Hash.wrap(string);
      this.bytes = Utils.HEX.decode(string);
      if (hash.equals(Sha256Hash.ZERO_HASH)) {
         throw new RuntimeException("Created Hash of all 0s");
      }
   }

   public TransactionHash(Sha256Hash hash) {
      super(hash.toString());
      this.hash = hash;
      this.bytes = hash.getBytes();
      if (hash.equals(Sha256Hash.ZERO_HASH)) {
         throw new RuntimeException("Created Hash of all 0s");
      }
   }

   public String toString() {
      return hash.toString();
   }


}
