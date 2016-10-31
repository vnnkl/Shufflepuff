package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by conta on 31.10.16.
 */

public class BitAccessInsight extends Insight {

   public BitAccessInsight(NetworkParameters networkParameters, Integer minPeers) {
      super(networkParameters, minPeers, "search.bitaccess.co", "test-insight.bitpay.com");
      if (netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         throw new IllegalArgumentException("BitAccess has no TestNet");
      }
   }
}
