package com.shuffle.p2p;

import com.shuffle.player.Shuffle;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

   // Current system unixtime in milliseconds -> seconds + 30 sec, same for everyone
   Long time = (System.currentTimeMillis()) + 15000L;

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

   /**
    * we will shuffle from the same keys
    * privKey        pub         address
    * <p>
    * Player1 cTqDy2FrE1rYWHD5c7mAyKu8RDFb6xU3gv2tmnHNiAgMEKzBSzny  03ad9403ba57d610ddcf660432dd04eb47c9135fec7f1aedba5217bcecfa820434  mnstkekN1bAZXL83QCmoQkN723h7Q7q14G
    * shuffles to n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3
    * change to n3Cta4xp9F1zzkuCSH9nzXACAiM6dcjHTY
    * <p>
    * Player2 cPpU65DtcqaqUpieaooT2FkYc9i8s1Q2KnXhcSvvm3fsPbJQSuW7  02b6408cd341300a0e19cc69a5442d015524593bc6d2157fbaba42dfbe80495ec8  moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV
    * shuffles to mpUr8en3adMSctw3vmyECid8c6zEEweFSi
    * change to n2kvNLWvXUv5yeFrnZeqU9FUtGSM6sytim
    * <p>
    * Player 3 cNnPdwysBLBA3jENeFKA3Q6fZkGR19N2MvngPWiDaEAdBSQbWyBB  026f040da7316679729b3a6483d43754a608c96c798b504564edf0a71b04eb1005  n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3
    * shuffles to mmTgeLetNQXQSip4a8EhYh6DA1VDCQGzXv
    * change to mzxuvrNabkuTL8u5Sa4tHvnTcWPH91ZYT6
    * <p>
    * --port 3001 --utxos [{"vout":"3","transactionHash":"0d41246259b95010bac124b3f0cc5c4d98f0449eef5d533f5af39e9b86740cb4"}]
    */

   @Test
   public void testShuffle() {


      // same for all players, min amount 100000
      String arguments = "--amount 30000000 --session testnet00 --query btcd --blockchain test --fee 8000 --rpcuser admin --rpcpass pass --timeout 100000 --time " + time.toString();

      String player1peers;
      String player2peers;
      String player3peers;
      String player4peers;
      String player5peers;

      // player 1 peers, other two port
      player1peers = "--peers [{\"address\":\"127.0.0.1:2002\",\"key\":\"03230916791a66e8fcd45a698930dfebdc87f103c4235eb85455b5cd86e711572b\"},{\"address\":\"127.0.0.1:2003\",\"key\":\"03fffa17a60c1b37407838168d27f67822c3276569eb75cb48439c877c9790f8e7\"},{\"address\":\"127.0.0.1:2004\",\"key\":\"02df9e69f6de59192ada468b7c3ac8df20e8b0d0f16747f11918baffa0a4b3609b\"},{\"address\":\"127.0.0.1:2005\",\"key\":\"03b71f43a6a19e47ffe846154d2f0fbdbfa47fc247ea1d80246bdc26c2491f0d04\"}]";
      player2peers = "--peers [{\"address\":\"127.0.0.1:2001\",\"key\":\"03155a06c771bb9020bedcc5c8b12a05154bc1cd6e7410dcd5e1651dd7f9e650b3\"},{\"address\":\"127.0.0.1:3003\",\"key\":\"03fffa17a60c1b37407838168d27f67822c3276569eb75cb48439c877c9790f8e7\"},{\"address\":\"127.0.0.1:2004\",\"key\":\"02df9e69f6de59192ada468b7c3ac8df20e8b0d0f16747f11918baffa0a4b3609b\"},{\"address\":\"127.0.0.1:2005\",\"key\":\"03b71f43a6a19e47ffe846154d2f0fbdbfa47fc247ea1d80246bdc26c2491f0d04\"}]";
      player3peers = "--peers [{\"address\":\"127.0.0.1:2001\",\"key\":\"03155a06c771bb9020bedcc5c8b12a05154bc1cd6e7410dcd5e1651dd7f9e650b3\"},{\"address\":\"127.0.0.1:3002\",\"key\":\"03230916791a66e8fcd45a698930dfebdc87f103c4235eb85455b5cd86e711572b\"},{\"address\":\"127.0.0.1:2004\",\"key\":\"02df9e69f6de59192ada468b7c3ac8df20e8b0d0f16747f11918baffa0a4b3609b\"},{\"address\":\"127.0.0.1:2005\",\"key\":\"03b71f43a6a19e47ffe846154d2f0fbdbfa47fc247ea1d80246bdc26c2491f0d04\"}]";
      player4peers = "--peers [{\"address\":\"127.0.0.1:2001\",\"key\":\"03155a06c771bb9020bedcc5c8b12a05154bc1cd6e7410dcd5e1651dd7f9e650b3\"},{\"address\":\"127.0.0.1:3002\",\"key\":\"03230916791a66e8fcd45a698930dfebdc87f103c4235eb85455b5cd86e711572b\"},{\"address\":\"127.0.0.1:2003\",\"key\":\"03fffa17a60c1b37407838168d27f67822c3276569eb75cb48439c877c9790f8e7\"},{\"address\":\"127.0.0.1:2005\",\"key\":\"03b71f43a6a19e47ffe846154d2f0fbdbfa47fc247ea1d80246bdc26c2491f0d04\"}]";
      player5peers = "--peers [{\"address\":\"127.0.0.1:2001\",\"key\":\"03155a06c771bb9020bedcc5c8b12a05154bc1cd6e7410dcd5e1651dd7f9e650b3\"},{\"address\":\"127.0.0.1:3002\",\"key\":\"03230916791a66e8fcd45a698930dfebdc87f103c4235eb85455b5cd86e711572b\"},{\"address\":\"127.0.0.1:2003\",\"key\":\"03fffa17a60c1b37407838168d27f67822c3276569eb75cb48439c877c9790f8e7\"},{\"address\":\"127.0.0.1:2004\",\"key\":\"02df9e69f6de59192ada468b7c3ac8df20e8b0d0f16747f11918baffa0a4b3609b\"}]";

      // make utxo argument as --utxos '[{\"vout\":\"3\",\"transactionHash\":\"0d41246259b95010bac124b3f0cc5c4d98f0449eef5d533f5af39e9b86740cb4\"}]'}
      // find here https://live.blockcypher.com/btc-testnet/address/mnstkekN1bAZXL83QCmoQkN723h7Q7q14G/
      //String player1utxos = " --utxos [{\"vout\":\"1\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}]";
      //String player1utxo = " --utxos [{\"vout\":\"1\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}";

      // find here https://live.blockcypher.com/btc-testnet/address/moFLhtjSQjiTxCYetVcJpdEZoKob4vWCNV/
      // String player2utxos = " --utxos [{\"vout\":\"4\",\"transactionHash\":\"ca91761b89b08d27732a07df2c5816ad215f7d9e3a01890ec389bbcb5ef2726e\"}]";
      // find here https://live.blockcypher.com/btc-testnet/address/n3izGxHXfgafbhtQ8Ls8CMsqFNbYcMncM3/
      // String player3utxos = " --utxos [{\"vout\":\"2\",\"transactionHash\":\"589e6a662325ba2e4fef9ebfb85b357ec41a49f1947189f1dcadf2d82d048970\"}]";

      //make optionSets
      OptionParser parser = ShuffleIT.getShuffleOptionsParser();
      String[] player1StringSet = (arguments + " --port 2001 --key cVhj3bTpbYU1yJgcBPm6kS4e9kyYk3Dic77am6sUabDmziHJkgZA --anon mvxaDwr7B7QREHyeLAmMew5B1z9tuAB4Nx --change mgjoRrW5PH7UFTfPBRXtKHr8xijbhgYpjt " + player1peers).split(" ");
      String[] player2StringSet = (arguments + " --port 2002 --key cRSMn7P5gWE7e9mVS7dsYLDMPe2S9DKssr4USGaH5dUmjx2WggtD --anon mnq75QqGyhooFAD6f86QnjU9dMJLkoj6dW --change mqEiGj8txWk5kPTMtfrwkuGnMVnz5hwM1Z " + player2peers).split(" ");
      String[] player3StringSet = (arguments + " --port 2003 --key cRoZDALyEbzbVs81VmsSfr6qN3VhGg8Ss2Bg116PbLCJM7x8nhkP --anon n2zetcfsB5AXkvmfec5xh3fuDdbh5TsGQt --change muC4jXuL9cWQwm8EFi5H5bwZ3b4ddH4DJP " + player3peers).split(" ");
      String[] player4StringSet = (arguments + " --port 2004 --key cNoa3hrocLRcYBkAjhYxTcCuUF4Da1Tmpj2Kn5iSbDeku4ZiM4qi --anon mqqcxvpb94KThzsdg9H5bceTK2uWpdHJkC --change mqneddYs9BSRFu7ymwqSAVUWyF7oq93AzV " + player4peers).split(" ");
      String[] player5StringSet = (arguments + " --port 2005 --key cMivKeAm17vgoNHXHBKM7ZXoyyJSx5hTCmypcpdjdDLnXT5Lzw2t --anon mpxk7EfQAdTysmNn3mVtT5XCSKumyZnaU5 --change msWVKDzUDBtJ4VTZVTsuF1DUvTKHFccCs6 " + player5peers).split(" ");


      OptionSet player1set = parser.parse(player1StringSet);
      OptionSet player2set = parser.parse(player2StringSet);
      OptionSet player3set = parser.parse(player3StringSet);
      OptionSet player4set = parser.parse(player4StringSet);
      OptionSet player5set = parser.parse(player5StringSet);

      // make ThreadGroup
      ThreadGroup threadGroup = new ThreadGroup("5x Shuffle ThreadGroup");

      // check http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/

      ExecutorService executorService = Executors.newFixedThreadPool(5);

      // not needed as we will find txid in printstream
      ExecutorCompletionService<String> completionService = new ExecutorCompletionService<String>(executorService);

      ArrayList<String[]> optionStringArrayList = new ArrayList<String[]>();
      optionStringArrayList.add(0, player1StringSet);
      optionStringArrayList.add(1, player2StringSet);
      optionStringArrayList.add(2, player3StringSet);
      optionStringArrayList.add(3, player4StringSet);
      optionStringArrayList.add(4, player5StringSet);


      ArrayList<OptionSet> optionSetArrayList = new ArrayList<OptionSet>();
      optionSetArrayList.add(0, player1set);
      optionSetArrayList.add(1, player2set);
      optionSetArrayList.add(2, player3set);
      optionSetArrayList.add(3, player4set);
      optionSetArrayList.add(4, player5set);

      List<Callable<Void>> callableList = new ArrayList<Callable<Void>>();
      for (int i = 0; i < 5; i++) {
         int finalI = i;
         Callable<Void> shuffleCallable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               Shuffle.main(optionStringArrayList.get(finalI));
               //return new Shuffle(optionSetArrayList.get(finalI), System.out).main(optionStringArrayList.get(finalI));
               return null;
            }
         };
         callableList.add(shuffleCallable);
      }
      List<Future<Void>> futureList;
      try {
         futureList = executorService.invokeAll(callableList);
      } catch (InterruptedException e) {
         e.printStackTrace();
         throw new RuntimeException("Interruption in executor Service thread", e);
      }

      for (Future<Void> future : futureList) {
         if (future.isCancelled()) {
            try {
               System.out.println(future.get().toString());
            } catch (InterruptedException e) {
               e.printStackTrace();
            } catch (ExecutionException e) {
               e.printStackTrace();
            }
         }
         if (future.isDone()) {
            try {
               System.out.println("Done " + future.get());
            } catch (InterruptedException e) {
               e.printStackTrace();
            } catch (ExecutionException e) {
               e.printStackTrace();
            }
         }
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
