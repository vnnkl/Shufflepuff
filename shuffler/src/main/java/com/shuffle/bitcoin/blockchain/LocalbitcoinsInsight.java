package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by conta on 31.10.16.
 */

public class LocalbitcoinsInsight extends Insight {

   public LocalbitcoinsInsight(NetworkParameters networkParameters, Integer minPeers) {
      super(networkParameters, minPeers, "www.localbitcoinschain.com", "test-insight.bitpay.com");
      if (!netParams.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
         throw new IllegalArgumentException("LocalBitcoins has no TestNet");
      }

   }
}
