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
    final byte[] encryptionKey;
    final PrivateKey privateKey;
    final PublicKey publicKey;
    final transient NetworkParameters params;

    public DecryptionKeyImpl(org.bitcoinj.core.ECKey key, NetworkParameters params) {
        this.key = key;
        this.encryptionKey = key.getPubKey();
        this.privateKey = null;
        this.publicKey = null;
        this.params = params;
    }
    public DecryptionKeyImpl(KeyPair keyPair, NetworkParameters params) {
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        this.key = ECKey.fromPrivate(this.privateKey.getEncoded());
        this.encryptionKey = this.publicKey.getEncoded();
        this.params = params;
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
      return new EncryptionKeyImpl(publicKey);
   }


    @Override
    public Address decrypt(Address m) throws FormatException {
        Guice.createInjector(new JvmModule()).injectMembers(this);
        java.lang.String input = m.toString();
        AddressImpl returnAddress = null;
        if (BitcoinCrypto.isValidAddress(input, params)) {
            return new AddressImpl(input,false);
        } else {

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
        byte[] bytes = Hex.decode(m.toString());
        byte[] decrypted = new byte[0];
        try {
           decrypted = cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
           e.printStackTrace();
        } catch (BadPaddingException e) {
           e.printStackTrace();
           throw new RuntimeException(e);
        }
        String addrString = new String(decrypted, StandardCharsets.UTF_8);
        returnAddress = new AddressImpl(addrString,
                !BitcoinCrypto.isValidAddress(addrString, params));

        }
        return returnAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecryptionKeyImpl that = (DecryptionKeyImpl) o;

        return key.equals(that.key) && Arrays.equals(encryptionKey, that.encryptionKey)
                && privateKey.equals(that.privateKey) && publicKey.equals(that.publicKey)
                && params.equals(that.params);

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + Arrays.hashCode(encryptionKey);
        result = 31 * result + privateKey.hashCode();
        result = 31 * result + publicKey.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }
}
