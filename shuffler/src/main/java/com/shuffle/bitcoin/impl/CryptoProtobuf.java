package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.NetworkParameters;

/**
 * Created by Daniel Krawisz on 7/14/16.
 */
public class CryptoProtobuf extends Protobuf {
    NetworkParameters params;
    Bitcoin bitcoin;

    @Override
    // Unmarshall an address from its string representation.
    public Address unmarshallAdress(String str) {
        return new AddressImpl(str);
    }

    @Override
    // Unmarshall an encryption key from a string.
    public EncryptionKey unmarshallEncryptionKey(String str) {
        return new EncryptionKeyImpl(str);
    }

    @Override
    // Unmarshall a decryption key.
    public DecryptionKey unmarshallDecryptionKey(String str) {
        return new DecryptionKeyImpl(str, params);
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
}
