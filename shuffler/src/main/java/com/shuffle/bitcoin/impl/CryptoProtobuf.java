package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.P;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.NetworkParameters;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Created by Daniel Krawisz on 7/14/16.
 */
public class CryptoProtobuf extends Protobuf {
    NetworkParameters params;
    Bitcoin bitcoin;

    @Override
    // Unmarshall an address from its string representation.
    public Address unmarshallAdress(String str) throws FormatException {
        return new AddressImpl(str);
    }

    @Override
    // Unmarshall an encryption key from a string.
    public EncryptionKey unmarshallEncryptionKey(String str)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {
            return new EncryptionKeyImpl(BitcoinCrypto.loadPublicKey(org.bouncycastle.util.encoders.Base64.toBase64String(org.spongycastle.util.encoders.Hex.decode(str))));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    // Unmarshall a decryption key.
    public DecryptionKey unmarshallDecryptionKey(String privString, String pubString) {
        return new DecryptionKeyImpl(privString,pubString);
    }

    @Override
    // Unmarshall a verification key.
    public VerificationKey unmarshallVerificationKey(String str) {
        return new VerificationKeyImpl(str, params);
    }

    @Override
    // Unmarshall a Transaction
    public Transaction unmarshallTransaction(byte[] bytes) throws FormatException {
        return bitcoin.fromBytes(bytes);
    }

    @Override
    public Marshaller<Address> addressMarshaller() {
        return new Marshaller<Address>() {

            @Override
            public Bytestring marshall(Address address) throws IOException {
                return new Bytestring(address.toString().getBytes());
            }

            @Override
            public Address unmarshall(Bytestring string) throws FormatException {
                return new AddressImpl(new String(string.bytes));
            }
        };
    }
}
