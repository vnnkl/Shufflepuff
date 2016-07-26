package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.impl.DecryptionKeyImpl;
import com.shuffle.bitcoin.impl.SigningKeyImpl;
import com.shuffle.p2p.Bytestring;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;


public class BitcoinCrypto implements Crypto {

    private final SecureRandom sr;
    // Figure out which network we should connect to. Each one gets its own set of files.
    NetworkParameters params;
    KeyChainGroup keyChainGroup;
    private final KeyPairGenerator keyPG;


    public BitcoinCrypto(NetworkParameters networkParameters) throws NoSuchAlgorithmException {
        this.params = networkParameters;
        this.keyChainGroup = new KeyChainGroup(networkParameters);

        //this.sr = SecureRandom.getInstance("SHA1PRNG", new BouncyCastleProvider());
        this.sr = SecureRandom.getInstance("SHA1PRNG");
        this.keyPG = KeyPairGenerator.getInstance("ECIES",new BouncyCastleProvider());
    }

    public NetworkParameters getParams() {
      return params;
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
                    + Character.digit(s.charAt(i+1), 16));
        }
        return new Bytestring(data);
    }

}
