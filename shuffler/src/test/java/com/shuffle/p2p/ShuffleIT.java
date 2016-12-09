package com.shuffle.p2p;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by conta on 08.12.16.
 */

public class ShuffleIT {

   // really dumb integration test
   NetworkParameters netParams = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
   // simple strings as arguments.

   // protocol options needed
   // session ID, same for every player in shuffle
   List<String> sessionList = getSessionIds(3);

   //shuffle amount in satoshi = 500000
   Coin amount = Coin.valueOf(500000L);
   String amountString = amount.toString();

   // Current system unixtime in milliseconds -> seconds + 3 min, same for everyone
   long time = (System.currentTimeMillis() / 1000L) + 180L;

   //fee amount in satoshi = 50000
   Coin feeAmount = Coin.valueOf(5000L);
   String feeAmountString = amount.toString();

   // player specific options
   // port
   List<String> portStringList = getPorts(3);

   // key
   // create n privKeys, option to give privkey with funds, time for confirmation then needed?
   // better provide keys with funds and create everything else

   // anon (shuffled) address
   List<String> anonStringList = getPorts(3);


   // change address


   public ECKey getNewPrivKey() {
      ECKey privateKey = new ECKey();
      return privateKey;
   }

   public HashMap<String, String> getAnonStringList(Integer n) {
      HashMap<String, String> privPubMap = new HashMap<>();
      List<String> anonStringList = new LinkedList<>();
      for (int i = 0; i < n; i++) {
         ECKey ecKey = getNewPrivKey();
         privPubMap.put(ecKey.getPrivateKeyAsHex(), ecKey.toAddress(netParams).toString());
         anonStringList.add(i, getNewPrivKey().getPublicKeyAsHex());
      }
      return privPubMap;
   }

   public List<String> getPorts(Integer n) {
      List<String> portStringList = new LinkedList<>();
      for (int i = 1880; i < i + n; i++) {
         portStringList.add(i - 1880, String.valueOf(i));
      }
      return portStringList;
   }


   public List<String> getSessionIds(Integer n) {
      List<String> stringList = new LinkedList<>();
      for (int i = 0; i < n; i++) {
         stringList.add(i, "seshionId" + n);
      }
      return stringList;
   }

   /**
    * http://www.convert-unix-time.com/api?timestamp=now
    */


}
