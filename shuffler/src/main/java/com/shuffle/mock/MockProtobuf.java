package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;

/**
 * Created by Daniel Krawisz on 7/14/16.
 */
public class MockProtobuf extends Protobuf {

    @Override
    // Unmarshall an address from its string representation.
    public Address unmarshallAdress(String str) {
        return new MockAddress(str);
    }

    @Override
    // Unmarshall an encryption key from a string.
    public EncryptionKey unmarshallEncryptionKey(String str) {
        return new MockEncryptionKey(str);
    }

    @Override
    // Unmarshall a decryption key.
    public DecryptionKey unmarshallDecryptionKey(String str) {
        return new MockDecryptionKey(str);
    }

    @Override
    // Unmarshall a verification key.
    public VerificationKey unmarshallVerificationKey(String str) {
        return new MockVerificationKey(str);
    }

    @Override
    // Unmarshall a Transaction
    public Transaction unmarshallTransaction(byte[] bytes) throws FormatException {
        return MockCoin.MockTransaction.fromBytes(new Bytestring(bytes));
    }
}
