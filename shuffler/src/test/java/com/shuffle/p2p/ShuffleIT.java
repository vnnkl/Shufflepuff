package com.shuffle.p2p;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.player.Shuffle;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

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
   HashMap anonStringList = getAnonStringList(3);

   // change address
   HashMap changeStringList = getChangeStringList(3);


   // prepare optionSets for shuffle
   public static OptionParser getShuffleOptionsParser() {
      OptionParser parser = Shuffle.getShuffleOptionsParser();
      return parser;
   }

   // OptionSet options = parser.parse( "-a", "-B", "-?" );

   OptionSet testOptions;

   @Test
   public void testOptionParser() {
      String arguments = "--amount 5000000 --session testnet00 --query bitcoin-core --blockchain test  --key cPeH8CS8TGKK29yKAFB4K4boDBXK3wV4fZTwMh15HDhiYdYJ9TGK --anon n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3 --change moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV --port 3001 --utxos [{\"vout\":\"3\",\"transactionHash\":\"0d41246259b95010bac124b3f0cc5c4d98f0449eef5d533f5af39e9b86740cb4\"}] --fee 8000 --time " + time;
      String[] splitted = arguments.split(" ");
      OptionSet options = getShuffleOptionsParser().parse(splitted);
      Assert.assertTrue(options.has("amount"));
      Assert.assertTrue(options.has("session"));
      Assert.assertTrue(options.valueOf("session").toString().equals("testnet00"));
      Assert.assertTrue(options.valueOf("query").toString().equals("bitcoin-core"));
      Assert.assertTrue(options.valueOf("blockchain").toString().equals("test"));
      Assert.assertTrue(options.valueOf("key").toString().equals("cPeH8CS8TGKK29yKAFB4K4boDBXK3wV4fZTwMh15HDhiYdYJ9TGK"));
      Assert.assertTrue(options.valueOf("anon").toString().equals("n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3"));
      Assert.assertTrue(options.valueOf("change").toString().equals("moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV"));
      Assert.assertTrue(options.valueOf("fee").toString().equals("8000"));

      // print utxo
      JSONArray array = new JSONArray(options.valueOf("u").toString());
      System.out.print(array.toString());
      testOptions = getShuffleOptionsParser().parse(splitted);
   }
   // new shuffle
   @Test
   public void testShuffle() {
      testOptionParser();
      try {
         Shuffle testShuffle = new Shuffle(testOptions, System.out);
      } catch (ParseException | UnknownHostException | FormatException e) {
         e.printStackTrace();
      } catch (NoSuchAlgorithmException e) {
         e.printStackTrace();
      } catch (AddressFormatException e) {
         e.printStackTrace();
      } catch (MalformedURLException e) {
         e.printStackTrace();
      } catch (BitcoinCrypto.Exception e) {
         e.printStackTrace();
      } catch (BitcoindException e) {
         e.printStackTrace();
      } catch (CommunicationException e) {
         e.printStackTrace();
      }
   }




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

   public HashMap<String, String> getChangeStringList(Integer n) {
      HashMap<String, String> privPubMap = new HashMap<>();
      List<String> changeStringList = new LinkedList<>();
      for (int i = 0; i < n; i++) {
         ECKey ecKey = getNewPrivKey();
         privPubMap.put(ecKey.getPrivateKeyAsHex(), ecKey.toAddress(netParams).toString());
         changeStringList.add(i, getNewPrivKey().getPublicKeyAsHex());
      }
      return privPubMap;
   }

   public List<String> getPorts(Integer n) {
      List<String> portStringList = new LinkedList<>();
      for (int i = 1880; i < (1880 + n); i++) {
         portStringList.add(i - 1880, String.valueOf(i));
      }
      return portStringList;
   }


   public List<String> getSessionIds(Integer n) {
      List<String> stringList = new LinkedList<>();
      for (int i = 0; i < n; i++) {
         stringList.add(i, "sessionId" + n);
      }
      return stringList;
   }


}
