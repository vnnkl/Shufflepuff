package com.shuffle.protocol;

import com.shuffle.cryptocoin.Address;
import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.EncryptionKey;

/**
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockEncryptionKey implements EncryptionKey {
    final int index;

    public MockEncryptionKey(int index) {
        this.index = index;
    }

    @Override
    public Address encrypt(Address m) throws CryptographyError {
        return new MockEncryptedAddress(m, this);
    }

    @Override
    public String toString() {
        return "ek[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockEncryptionKey)) {
            return false;
        }

        return index == ((MockEncryptionKey)o).index;
    }

}
