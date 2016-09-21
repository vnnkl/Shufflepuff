package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.P;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;

import java.io.IOException;

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
    public DecryptionKey unmarshallDecryptionKey(String privString, String pubString) {
        return new MockDecryptionKey(privString);
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

    @Override
    public Marshaller<Address> addressMarshaller() {
        return new Marshaller<Address>() {

            @Override
            public Bytestring marshall(Address address) throws IOException {
                return new Bytestring(address.toString().getBytes());
            }

            @Override
            public Address unmarshall(Bytestring string) throws FormatException {
                return unmarshallAdress(new String(string.bytes));
            }
        };
    }
}
