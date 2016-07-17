package com.shuffle.bitcoin.impl;

import com.google.inject.Guice;
import com.shuffle.JvmModule;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


/**
 * A private key used for decryption.
 */

public class DecryptionKeyImpl implements DecryptionKey {

    final ECKey key;
    final PrivateKey privateKey;
    private final EncryptionKey ek;

    public DecryptionKeyImpl(KeyPair keyPair) {
        this.privateKey = keyPair.getPrivate();
        this.key = ECKey.fromPrivate(this.privateKey.getEncoded());
        ek = new EncryptionKeyImpl(keyPair.getPublic());
    }

    public DecryptionKeyImpl(String string) {
        // TODO
        throw new IllegalArgumentException();
    }

    // returns encoded private key in hex format
    public java.lang.String toString() {
       return org.bouncycastle.util.encoders.Hex.toHexString(privateKey.getEncoded());
    }

    public ECKey getKey() {
      return key;
   }

    @Override
    public EncryptionKey EncryptionKey() {
      return ek;
   }


    @Override
    public String decrypt(String input) {
        Guice.createInjector(new JvmModule()).injectMembers(this);

        //encrypt cipher
        Cipher cipher = null;
        try {
           cipher = Cipher.getInstance("ECIES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
           e.printStackTrace();
           throw new RuntimeException(e);
        }
        try {
           cipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (InvalidKeyException e) {
           e.printStackTrace();
           throw new RuntimeException(e);
        }
        byte[] bytes = Hex.decode(input);
        byte[] decrypted = new byte[0];
        try {
           decrypted = cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
           e.printStackTrace();
        } catch (BadPaddingException e) {
           e.printStackTrace();
           throw new RuntimeException(e);
        }
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecryptionKeyImpl that = (DecryptionKeyImpl) o;

        return key.equals(that.key)
                && privateKey.equals(that.privateKey);

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + privateKey.hashCode();
        return result;
    }
}
