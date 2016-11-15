package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by conta on 01.04.16.
 */
public class EncryptionKeyImpl implements EncryptionKey {

    private final PublicKey publicKey;

    public EncryptionKeyImpl(PublicKey pubKey) {
        this.publicKey = pubKey;
    }

    // takes a key in hex as string
    public EncryptionKeyImpl(String hexString)
            throws InvalidKeySpecException, NoSuchAlgorithmException {

        try {
            // get base64 of passed hexstring and use BitcoinCrypto to load publickey
            this.publicKey = BitcoinCrypto.loadPublicKey(org.bouncycastle.util.encoders.Base64.toBase64String(org.spongycastle.util.encoders.Hex.decode(hexString)));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String toString() {
        return org.spongycastle.util.encoders.Hex.toHexString(this.publicKey.getEncoded());
    }

    @Override
    public String encrypt(String input) {

        // encrypts the address passed for this encryption key
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        //get cipher cipher for ECIES encryption
        Cipher cipher = null;
        try {
            try {
                cipher = Cipher.getInstance("ECIES", "BC");
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
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