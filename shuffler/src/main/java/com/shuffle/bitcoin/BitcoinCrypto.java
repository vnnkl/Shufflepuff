package com.shuffle.bitcoin;

import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.HDUtils;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;



public class BitcoinCrypto implements Crypto {


   private final SecureRandom sr;
   // Figure out which network we should connect to. Each one gets its own set of files.
   NetworkParameters params;
   KeyChainGroup keyChainGroup;

   public BitcoinCrypto(){
      this(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
   }


   public BitcoinCrypto(NetworkParameters networkParameters){
      this.params = networkParameters;
      this.keyChainGroup = new KeyChainGroup(networkParameters);
      try {
         //this.sr = SecureRandom.getInstance("SHA1PRNG", new BouncyCastleProvider());
         this.sr = SecureRandom.getInstance("SHA1PRNG");
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("Error DRGB", e);
      }
   }



   //Alphabet defining valid characters used in address
   private final static String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";


   //Generate Keypair using HMAC
   KeyPairGenerator keyPG = getKeyPGen();
   KeyPair keys = getKeyPair();
   KeyPair keyPair = keyPG.generateKeyPair();
   PrivateKey privKey = keyPair.getPrivate();
   PublicKey pubKey = keyPair.getPublic();

   //DeterministicSeed seed = kit.wallet().getKeyChainSeed();
   // if we generate new keys no need for mnemonic, apparently we don't
   //List<String> mnemonicCode = seed.getMnemonicCode();




   public NetworkParameters getParams() {
      return params;
   }

   // create derivation path for shuffle keys
   String path = HDUtils.formatPath(HDUtils.parsePath("5H/"));
   int decKeyCounter = 0;




   public boolean isValidAddress(String address) {
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
      KeyFactory fact = KeyFactory.getInstance("ECDSA",new BouncyCastleProvider());
      PrivateKey priv = fact.generatePrivate(keySpec);
      Arrays.fill(clear, (byte) 0);
      return priv;
   }


   public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
      byte[] data = Base64.decode(stored);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
      KeyFactory fact = KeyFactory.getInstance("ECDSA",new BouncyCastleProvider());
      try {
         return fact.generatePublic(spec);
      } catch (InvalidKeySpecException e){
         throw new IllegalArgumentException(e);
      }
   }

   public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
      KeyFactory fact = KeyFactory.getInstance("ECDSA",new BouncyCastleProvider());
      PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
            PKCS8EncodedKeySpec.class);
      byte[] packed = spec.getEncoded();
      String key64 = Base64.toBase64String(packed);
      Arrays.fill(packed, (byte) 0);
      return key64;
   }


   public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
      KeyFactory fact = KeyFactory.getInstance("ECDSA",new BouncyCastleProvider());
      X509EncodedKeySpec spec = fact.getKeySpec(publ,
            X509EncodedKeySpec.class);
      return Base64.toBase64String(spec.getEncoded());
   }


   private KeyPairGenerator getKeyPGen() {
      if (keyPG == null) {
         try {
            keyPG = KeyPairGenerator.getInstance("ECIES",new BouncyCastleProvider());
         } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
         }
         final int keylength = 256;
         keyPG.initialize(keylength, sr);
         return keyPG;
      }
      return keyPG;
   }

   private KeyPair getKeyPair() {
      if (keys == null || keyPG == null) {
         if (keyPG == null) {
            keyPG = getKeyPGen();
         }

         keys = keyPG.generateKeyPair();
         return keys;
      }
      return keys;
   }


    @Override
    public DecryptionKey makeDecryptionKey() {
       // String ppath = getCurrentPathAsString();
       // System.out.println("Current path used by decryption key genereated: " + ppath);
       // ECKey newDecKey = keyChainGroup.getActiveKeyChain().getKeyByPath(HDUtils.parsePath(ppath),true);
       // decKeyCounter++;
       // return ECIES KeyPair
       KeyPair keyPair = getKeyPair();
       return new DecryptionKeyImpl(keyPair);

    }

    @Override
    public SigningKey makeSigningKey() {
       ECKey newSignKey = keyChainGroup.freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
       return new SigningKeyImpl(newSignKey,this);
    }

    @Override
    public int getRandom(int n) {
         return sr.nextInt(n + 1);
    }

}
