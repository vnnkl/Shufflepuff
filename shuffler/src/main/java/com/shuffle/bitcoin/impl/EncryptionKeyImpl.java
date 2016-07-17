package com.shuffle.bitcoin.impl;

import com.google.inject.Guice;
import com.shuffle.JvmModule;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

    private final PublicKey publicKey;

    public EncryptionKeyImpl(byte[] ecPubKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeyFactory keyFactory = KeyFactory.getInstance("ECIES");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(ecPubKey);
        this.publicKey = keyFactory.generatePublic(publicKeySpec);

    }

    public EncryptionKeyImpl(PublicKey pubKey) {

        this.publicKey = pubKey;
    }

    public EncryptionKeyImpl(String string)
            throws InvalidKeySpecException, NoSuchAlgorithmException {

        this(BitcoinCrypto.hexStringToByteArray(string));
    }

    public String toString() {
        return org.spongycastle.util.encoders.Hex.toHexString(this.publicKey.getEncoded());
    }

    @Override
    public String encrypt(String input) {

        // encrypts the address passed for this encryption key
        Guice.createInjector(new JvmModule()).injectMembers(this);

        //get cipher cipher for ECIES encryption
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("ECIES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //init cipher with with our encryption key
        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        //get bytes of address passed
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        //encrypt
        byte[] encrypted = new byte[0];
        try {
            encrypted = cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //create new address with
        return Hex.encodeHexString(encrypted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EncryptionKeyImpl that = (EncryptionKeyImpl) o;

        return publicKey.equals(that.publicKey);

    }

    @Override
    public int hashCode() {
        return publicKey.hashCode();
    }
}