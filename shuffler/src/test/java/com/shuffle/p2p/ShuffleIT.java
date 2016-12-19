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
   Long time = (System.currentTimeMillis() / 1000L) + 180L;

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


   // new shuffle
   @Test
   public void testShuffle() {

      /** we will shuffle from the same keys
       *    privKey        pub         address
       *
       *   Player1 cTqDy2FrE1rYWHD5c7mAyKu8RDFb6xU3gv2tmnHNiAgMEKzBSzny  03ad9403ba57d610ddcf660432dd04eb47c9135fec7f1aedba5217bcecfa820434  mnstkekN1bAZXL83QCmoQkN723h7Q7q14G
       *   shuffles to n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3
       *   change to n3Cta4xp9F1zzkuCSH9nzXACAiM6dcjHTY
       *
       *   Player2 cPpU65DtcqaqUpieaooT2FkYc9i8s1Q2KnXhcSvvm3fsPbJQSuW7  02b6408cd341300a0e19cc69a5442d015524593bc6d2157fbaba42dfbe80495ec8  moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV
       *   shuffles to mpUr8en3adMSctw3vmyECid8c6zEEweFSi
       *   change to n2kvNLWvXUv5yeFrnZeqU9FUtGSM6sytim
       *
       *   Player 3 cNnPdwysBLBA3jENeFKA3Q6fZkGR19N2MvngPWiDaEAdBSQbWyBB  026f040da7316679729b3a6483d43754a608c96c798b504564edf0a71b04eb1005  n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3
       *   shuffles to mmTgeLetNQXQSip4a8EhYh6DA1VDCQGzXv
       *   change to mzxuvrNabkuTL8u5Sa4tHvnTcWPH91ZYT6
       *
       * --port 3001 --utxos [{"vout":"3","transactionHash":"0d41246259b95010bac124b3f0cc5c4d98f0449eef5d533f5af39e9b86740cb4"}]
       *
       */

      // same for all players
      String arguments = "--amount 500000 --session testnet00 --query bitcoin-core --blockchain test  --fee 8000 --rpcuser admin --rpcpass pass --time " + time.toString();

      String player1peers = null;
      String player2peers = null;
      String player3peers = null;
      // player 1 peers, other two port
      player1peers = "--peers [{\"address\":\"127.0.0.1:3002\",\"key\":\"02b6408cd341300a0e19cc69a5442d015524593bc6d2157fbaba42dfbe80495ec8\"},{\"address\":\"127.0.0.1:3003\",\"key\":\"026f040da7316679729b3a6483d43754a608c96c798b504564edf0a71b04eb1005\"}]";
      player2peers = "--peers [{\"address\":\"127.0.0.1:3001\",\"key\":\"03ad9403ba57d610ddcf660432dd04eb47c9135fec7f1aedba5217bcecfa820434\"},{\"address\":\"127.0.0.1:3003\",\"key\":\"026f040da7316679729b3a6483d43754a608c96c798b504564edf0a71b04eb1005\"}]";
      player3peers = "--peers [{\"address\":\"127.0.0.1:3001\",\"key\":\"03ad9403ba57d610ddcf660432dd04eb47c9135fec7f1aedba5217bcecfa820434\"},{\"address\":\"127.0.0.1:3002\",\"key\":\"02b6408cd341300a0e19cc69a5442d015524593bc6d2157fbaba42dfbe80495ec8\"}]";

      // make utxo argument as --utxos '[{\"vout\":\"3\",\"transactionHash\":\"0d41246259b95010bac124b3f0cc5c4d98f0449eef5d533f5af39e9b86740cb4\"}]'}
      // find here https://live.blockcypher.com/btc-testnet/address/mnstkekN1bAZXL83QCmoQkN723h7Q7q14G/
      String player1utxos = " --utxos [{\"vout\":\"1\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}]";
      String player1utxo = " --utxos [{\"vout\":\"1\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}";

      // find here https://live.blockcypher.com/btc-testnet/address/moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV/
      String player2utxos = " --utxos [{\"vout\":\"4\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}]";
      // find here https://live.blockcypher.com/btc-testnet/address/n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3/
      String player3utxos = " --utxos [{\"vout\":\"2\",\"transactionHash\":\"589e6a662325ba2e4fef9ebfb85b357ec41a49f1947189f1dcadf2d82d048970\"}]";

      //make optionSets
      OptionParser parser = ShuffleIT.getShuffleOptionsParser();
      OptionSet player1set = parser.parse((arguments + " --port 3001 --key cTqDy2FrE1rYWHD5c7mAyKu8RDFb6xU3gv2tmnHNiAgMEKzBSzny --anon n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3 --change n2kvNLWvXUv5yeFrnZeqU9FUtGSM6sytim " + player1peers + player1utxos).split(" "));
      OptionSet player2set = parser.parse((arguments + " --port 3002 --key cPpU65DtcqaqUpieaooT2FkYc9i8s1Q2KnXhcSvvm3fsPbJQSuW7 --anon mpUr8en3adMSctw3vmyECid8c6zEEweFSi --change n3Cta4xp9F1zzkuCSH9nzXACAiM6dcjHTY " + player2peers + player2utxos).split(" "));
      OptionSet player3set = parser.parse((arguments + " --port 3003 --key cNnPdwysBLBA3jENeFKA3Q6fZkGR19N2MvngPWiDaEAdBSQbWyBB --anon mmTgeLetNQXQSip4a8EhYh6DA1VDCQGzXv --change mzxuvrNabkuTL8u5Sa4tHvnTcWPH91ZYT6 " + player3peers + player3utxos).split(" "));

      Thread player1Thread = new Thread(() -> {
         try {
            Shuffle player1Shuffle = new Shuffle(player1set, System.out);
         } catch (ParseException | UnknownHostException | FormatException | NoSuchAlgorithmException | AddressFormatException | MalformedURLException | BitcoinCrypto.Exception | BitcoindException | CommunicationException e) {
            e.printStackTrace();
         }
      });
      Thread player2Thread = new Thread(() -> {
         try {
            Shuffle player1Shuffle = new Shuffle(player2set, System.out);
         } catch (ParseException | UnknownHostException | FormatException | NoSuchAlgorithmException | AddressFormatException | MalformedURLException | BitcoinCrypto.Exception | BitcoindException | CommunicationException e) {
            e.printStackTrace();
         }
      });
      Thread player3Thread = new Thread(() -> {
         try {
            Shuffle player3Shuffle = new Shuffle(player3set, System.out);
         } catch (ParseException | UnknownHostException | FormatException | NoSuchAlgorithmException | AddressFormatException | MalformedURLException | BitcoinCrypto.Exception | BitcoindException | CommunicationException e) {
            e.printStackTrace();
         }
      });
      player1Thread.start();
      player2Thread.start();
      player3Thread.start();

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
