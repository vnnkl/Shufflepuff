package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by conta on 31.10.16.
 */

public class BitPayInsight extends Insight {

   public BitPayInsight(NetworkParameters networkParameters, Integer minPeers) {
      super(networkParameters, minPeers, "insight.bitpay.com", "test-insight.bitpay.com");
   }

}
