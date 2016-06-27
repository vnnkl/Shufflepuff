package com.shuffle.bitcoin.impl;

import com.google.inject.Guice;
import com.shuffle.JvmModule;
import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.BitcoinCrypto;
import com.shuffle.bitcoin.DecryptionKey;
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
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;

import static org.junit.Assert.assertEquals;

/**
 * Created by conta on 02.06.16.
 */
public class DecryptionKeyImplTest {
    ECKey ecKey;
    BitcoinCrypto bitcoinCrypto;
    DecryptionKey decryptionKey;
    SecureRandom secureRandom;
    EncryptionKey encryptionKey;
    PrivateKey privateTestKey;
    PublicKey publicTestKey;
    KeyPair testKeys;
    @Before
    public void setUp() throws Exception {
        // The module also initializes the BouncyCastle crypto
        Guice.createInjector(new JvmModule()).injectMembers(this);
        this.bitcoinCrypto = new BitcoinCrypto();
        this.secureRandom = new SecureRandom();
        this.ecKey = new ECKey(secureRandom);
        this.privateTestKey = BitcoinCrypto.loadPrivateKey("MIGNAgEAMBAGByqGSM49AgEGBSuBBAAKBHYwdAIBAQQgk4OP0krnEkP5IkAvzH3HEXalM2VVIb3EaDk8zDU1ypWgBwYFK4EEAAqhRANCAAScJ+9oHg9jufttpUDJeJuxD36qDcJzIn7X7/kjrhCjhRzArEe0dzTE/kTS02hGHsX9OtleBaxBjJxGCIAeKh0e");
        this.publicTestKey = BitcoinCrypto.loadPublicKey("MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEnCfvaB4PY7n7baVAyXibsQ9+qg3CcyJ+1+/5I64Qo4UcwKxHtHc0xP5E0tNoRh7F/TrZXgWsQYycRgiAHiodHg==");
        this.testKeys = new KeyPair(publicTestKey,privateTestKey);
        this.encryptionKey = new EncryptionKeyImpl(testKeys.getPublic());
        this.decryptionKey = new DecryptionKeyImpl(testKeys);
    }

    @Test
    public void testECIESAvailability() throws Exception {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES",new BouncyCastleProvider());
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256k1"));

        KeyPair recipientKeyPair = keyPairGenerator.generateKeyPair();
        PublicKey pubKey = recipientKeyPair.getPublic();
        PrivateKey privKey = recipientKeyPair.getPrivate();


        // init the encryption cipher
        Cipher iesCipher = Cipher.getInstance("ECIES");
        iesCipher.init(Cipher.ENCRYPT_MODE, pubKey);

        // use the cipher
        String message = "Hello ECIES World!";
        byte[] encryptedMessage = iesCipher.doFinal(message.getBytes());

        // init the decryption cipher
        iesCipher.init(Cipher.DECRYPT_MODE, privKey);
        String decryptedMessage = new String(iesCipher.doFinal(encryptedMessage));

        System.out.println(message);
        System.out.println(pubKey);
        System.out.println(privKey);
        System.out.println(Arrays.toString(privKey.getEncoded()));
        System.out.println(" -> " + Hex.encodeHexString(encryptedMessage));
        System.out.println(" -> " + decryptedMessage);
    }

    @Test
    public void testToString() throws Exception {
        System.out.println("\nBegin Test toString:");
        String string = this.ecKey.getPrivateKeyAsWiF(bitcoinCrypto.getParams());
        System.out.println("ECKey: " + this.ecKey);
        System.out.println("String ECKey WIF: " + string);
        System.out.println("String DecryptionKey: " + this.decryptionKey.toString());
        assertEquals("toString Method: ", new DecryptionKeyImpl(testKeys).toString(), this.decryptionKey.toString());

    }

    @Test
    public void testEncryptionKey() throws Exception {

        System.out.println("\nBegin Test encryptionKey:");
        byte[] pub = ECKey.publicKeyFromPrivate(ecKey.getPrivKey(), ecKey.isCompressed());
        EncryptionKey encryptionKey1 = new EncryptionKeyImpl(ECKey.fromPublicOnly(ecKey.getPubKey()));
//
//      PublicKey publicKey = BitcoinCrypto.loadPublicKey(Base64.getEncoder().encodeToString(ecKey.getPubKey()));

        System.out.println("ecKey: " + ecKey.toString());
        System.out.println("ecKey priv: " + ecKey.getPrivateKeyAsHex());
        System.out.println("secureRandom: " + secureRandom.toString());
        System.out.println("decryptionKey: " + decryptionKey.toString());
        System.out.println("ASN.1  " + Arrays.toString(ecKey.toASN1()));

        EncryptionKeyImpl encTest = new EncryptionKeyImpl(ecKey.getPubKey());
        System.out.println("\nencTest: " + encTest);
        System.out.println("encryptionKey: " + encryptionKey1);
        System.out.println("EncKey.toString from ECKeys Pub: " + encTest.toString());
        System.out.println("EncKey from DecKey to string: " + decryptionKey.EncryptionKey().toString());

        encryptionKey = new EncryptionKeyImpl(publicTestKey);
        assertEquals(encryptionKey.toString(), decryptionKey.EncryptionKey().toString());

    }

    @Test
    public void testDecrypt() throws Exception {
        System.out.println(privateTestKey);
        Address testAddress = new AddressImpl("myGgn8UojMsyqn6KGQLEbVbpYSePcKfawG",false);
        System.out.println("Address myGgn8UojMsyqn6KGQLEbVbpYSePcKfawG encrypted to PublicKey :"+ encryptionKey.toString());
        Address encAddress = encryptionKey.encrypt(testAddress);
        System.out.println("Address myGgn8UojMsyqn6KGQLEbVbpYSePcKfawG encrypted \n to "+ encryptionKey.toString() +" :\n" + encAddress);
        Address decAddress = decryptionKey.decrypt(testAddress);
        System.out.println("and then decrypted \n back to :\n" + decAddress);
    }
}
