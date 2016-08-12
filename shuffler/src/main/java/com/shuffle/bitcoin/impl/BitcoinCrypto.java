package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.p2p.Bytestring;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.store.UnreadableWalletException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.crypto.Cipher;


public class BitcoinCrypto implements Crypto {

   private final SecureRandom sr;
   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params;
   KeyChainGroup keyChainGroup;
   private final KeyPairGenerator keyPG;
   Wallet wallet;
   WalletAppKit kit;
   String fileprefix = "shufflepuff";

   public static class Exception extends java.lang.Exception {
      public Exception(String message) {
         super(message);
      }
   }

   private static void crashIfJCEMissing() throws NoSuchAlgorithmException, Exception {
      int size = Cipher.getMaxAllowedKeyLength("AES");
      Integer expected = Integer.MAX_VALUE;
      if (size < expected) {
         String msg = "Max key size is " + size + ", but expected " + expected +
               ". Unfortunately, you have a security policy that limits your encryption " +
               "strength. Please either use OpenJDK or allow yourself to use strong crypto\n" +
               "by installing the according JCE files:\n" +
               "http://stackoverflow.com/questions/6481627/java-security-illegal-key-size-or-default-parameters";
         throw new Exception(msg);
      }
   }

   public BitcoinCrypto(NetworkParameters networkParameters) throws NoSuchAlgorithmException, Exception {
      this.params = NetworkParameters.fromID(networkParameters.getId());
      this.keyChainGroup = new KeyChainGroup(networkParameters);
      Security.insertProviderAt(new BouncyCastleProvider(), 1);
      crashIfJCEMissing();
      //this.sr = SecureRandom.getInstance("SHA1PRNG", new BouncyCastleProvider());
      this.sr = SecureRandom.getInstance("SHA1PRNG");
      this.keyPG = KeyPairGenerator.getInstance("ECIES", new BouncyCastleProvider());
      this.kit = getKit();
      this.wallet = getKit().wallet();

   }

   public BitcoinCrypto(NetworkParameters networkParameters, KeyChainGroup keyChainGroup) throws NoSuchAlgorithmException, Exception {
      this(networkParameters);
      this.keyChainGroup = keyChainGroup;
      this.kit = getKit().restoreWalletFromSeed(keyChainGroup.getActiveKeyChain().getSeed());
      this.wallet = kit.wallet();
   }

   public BitcoinCrypto(NetworkParameters networkParameters, DeterministicSeed seed) throws Exception, NoSuchAlgorithmException {
      this.params = NetworkParameters.fromID(networkParameters.getId());
      this.keyChainGroup = new KeyChainGroup(networkParameters, seed);
      Security.insertProviderAt(new BouncyCastleProvider(), 1);
      crashIfJCEMissing();
      //this.sr = SecureRandom.getInstance("SHA1PRNG", new BouncyCastleProvider());
      this.sr = SecureRandom.getInstance("SHA1PRNG");
      this.keyPG = KeyPairGenerator.getInstance("ECIES", new BouncyCastleProvider());
      this.kit = initKit(seed);
      this.wallet = kit.wallet();
   }

   public NetworkParameters getParams() {
      return params;
   }

   private void restoreFromSeed(String mnemonic) {
      kit.stopAsync().awaitTerminated();
      try {
         this.kit = initKit(new DeterministicSeed(mnemonic, null, "", 1230768000));
      } catch (UnreadableWalletException e) {
         e.printStackTrace();
      }

      kit.startAsync().awaitRunning();

   }


   public Transaction send(String destinationAddress, long amountSatoshis) throws InsufficientMoneyException {
      Address addressj;
      try {
         addressj = new Address(params, destinationAddress);
      } catch (AddressFormatException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
      Coin amount = Coin.valueOf(amountSatoshis);
      // create a tx to destinationAddress of amount and broadcast by wallets peergroup
      Wallet.SendResult sendResult = getKit().wallet().sendCoins(kit.peerGroup(), addressj, amount);
      try {
         return sendResult.broadcastComplete.get();
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   public String sendOffline(String destinationAddress, long amountSatoshis) throws InsufficientMoneyException {
      Address addressj;
      try {
         addressj = new Address(params, destinationAddress);
      } catch (AddressFormatException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
      Coin amount = Coin.valueOf(amountSatoshis);
      // create a SendRequest of amount to destinationAddress
      Wallet.SendRequest sendRequest = Wallet.SendRequest.to(addressj, amount);
      // set dynamic fee
      sendRequest.feePerKb = getRecommendedFee();
      // complete & sign tx
      wallet.completeTx(sendRequest);
      wallet.signTransaction(sendRequest);
      // return tx bytes as hex encoded String
      return Hex.encodeHexString(sendRequest.tx.bitcoinSerialize());
   }

   private WalletAppKit initKit(@Nullable DeterministicSeed seed) {
      //initialize files and stuff here, add our address to the watched ones
      DeterministicSeed deterministicSeed = this.keyChainGroup.getActiveKeyChain().getSeed();
      WalletAppKit kit = new WalletAppKit(params, new File("./spv"), fileprefix);
      kit.setAutoSave(true);
      //kit.useTor();
      kit.setDiscovery(new DnsDiscovery(params));
      // fresh restore if seed provided
      if (seed != null) {
         kit.restoreWalletFromSeed(seed);
      }
      // startUp WalletAppKit
      kit.startAsync();
      // wait for kit to sync and startup, print out every 3 sec
      while (!kit.isRunning()) {
         System.out.println("Waiting for kit to start up ...");
         try {
            kit.awaitRunning(3, TimeUnit.SECONDS);
         } catch (TimeoutException ignored) {
         }
      }
      return kit;
   }

   public WalletAppKit getKit() {
      if (kit == null) {
         initKit(null);
      }
      return kit;
   }

   public Coin getRecommendedFee() {
      String url = "https://bitcoinfees.21.co/api/v1/fees/recommended";
      URL obj;
      try {
         obj = new URL(url);
         JSONTokener tokener;
         HttpURLConnection con = (HttpURLConnection) obj.openConnection();
         con.setRequestMethod("GET");
         //set user header to prevent 403
         con.setRequestProperty("User-Agent", "Chrome/5.0");
         tokener = new JSONTokener(con.getInputStream());
         JSONObject root = new JSONObject(tokener);
         return Coin.valueOf(Long.valueOf(root.get("fastestFee").toString()));
      } catch (IOException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }


   public static boolean isValidAddress(String address, NetworkParameters params) {
      try {
         new Address(params, address);
         return true;
      } catch (AddressFormatException e) {
         return false;
      }
   }

   public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
      byte[] clear = Base64.decode(key64);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
      KeyFactory fact = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
      PrivateKey priv = fact.generatePrivate(keySpec);
      Arrays.fill(clear, (byte) 0);
      return priv;
   }


   public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
      byte[] data = Base64.decode(stored);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
      KeyFactory fact = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
      try {
         return fact.generatePublic(spec);
      } catch (InvalidKeySpecException e) {
         throw new IllegalArgumentException(e);
      }
   }

   @Override
   public synchronized DecryptionKey makeDecryptionKey() {
      // String ppath = getCurrentPathAsString();
      // System.out.println("Current path used by decryption key genereated: " + ppath);
      // ECKey newDecKey = keyChainGroup.getActiveKeyChain().getKeyByPath(HDUtils.parsePath(ppath),true);
      // decKeyCounter++;
      // return ECIES KeyPair
      keyPG.initialize(256, sr);
      return new DecryptionKeyImpl(keyPG.generateKeyPair());

   }

   public List<String> getKeyChainMnemonic() {
      return keyChainGroup.getActiveKeyChain().getSeed().getMnemonicCode();
   }

   @Override
   public SigningKey makeSigningKey() {
      ECKey newSignKey = keyChainGroup.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
      return new SigningKeyImpl(newSignKey, this.getParams());
   }

   @Override
   public int getRandom(int n) {
      return sr.nextInt(n + 1);
   }

   public static Bytestring hexStringToByteArray(String s) {
      int len = s.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
         data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
               + Character.digit(s.charAt(i + 1), 16));
      }
      return new Bytestring(data);
   }

}
