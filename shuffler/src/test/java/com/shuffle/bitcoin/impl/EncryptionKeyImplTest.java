package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.EncryptionKey;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.Assert.assertEquals;

/**
 * Created by conta on 07.06.16.
 */
public class EncryptionKeyImplTest {
    ECKey ecKey, tkey;
    ECKey pub;
    EncryptionKey ek;
    Address address;
    PrivateKey privKey;
    PublicKey pubKey;

    @Before
    public void setUp() throws Exception {
        ecKey = new ECKey();
        pub = ECKey.fromPublicOnly(ecKey.getPubKey());
        //ek = new EncryptionKeyImpl(pub);
        address = new AddressImpl("myGgn8UojMsyqn6KGQLEbVbpYSePcKfawG");

        System.out.println("ecKey pubHex            " + ecKey.getPublicKeyAsHex());
        System.out.println("ecKey privHex           " + ecKey.getPrivateKeyAsHex());
        System.out.println("pub from ecKey          " + pub);


        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES",new BouncyCastleProvider());
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"));

        KeyPair recipientKeyPair = keyPairGenerator.generateKeyPair();
        pubKey = recipientKeyPair.getPublic();
        privKey = recipientKeyPair.getPrivate();

        ek = new EncryptionKeyImpl(pubKey);

        System.out.println("privKey                  " + privKey);

        //testkey creation
        byte[] privbytes = Hex.decodeHex("076edbacad6ba3572be68131900da4e2a3b72f273bb2184c304282bcac117838".toCharArray());
        tkey = ECKey.fromPrivate(privbytes, false);
        System.out.println("\nhardcoded testKey (tkey) " + tkey);
        // PrivateKey privateKey = BitcoinCrypto.loadPrivateKey(tkey.getPrivateKeyAsHex());
        System.out.println("testKey (tkey) priv:     " + tkey.getPrivateKeyAsHex());
        // System.out.println("testKey priv loaded:     " + privateKey.toString());
    }

    @Test
    public void testToString() throws Exception {
        System.out.println("\ntestToString:");
        System.out.println("ecKeyPubAsHex:          " + ecKey.getPublicKeyAsHex());
        System.out.println("pub from ecKey toString " + pub.toString());
        System.out.println("EncKey.toString:        " + ek.toString());

        assertEquals("toString of ecKeyPubHex same as EncryptionKeyToString ", (new EncryptionKeyImpl(pubKey)).toString(), ek.toString());
        assertEquals(ek, new EncryptionKeyImpl(ek.toString()));
    }
}