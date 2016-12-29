/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.EncryptionKey;

import java.io.Serializable;

/**
 * It's a pretend decryption key for testing purposes.
 *
 * Created by Daniel Krawisz on 12/8/15.
 */
public class MockDecryptionKey implements DecryptionKey, Serializable {
    public final int index;
    public final MockEncryptionKey key;

    public MockDecryptionKey(int index) {
        this.index = index;
        key = new MockEncryptionKey(index);
    }

    public MockDecryptionKey(String str) throws NumberFormatException {
        // TODO regex
        index = Integer.parseInt(str);
        key = new MockEncryptionKey(index);
    }

    @Override
    public EncryptionKey EncryptionKey() {
        return key;
    }

    @Override
    // Intended blockchain decrypt a single element.
    public String decrypt(String m) {
        String encrypted = "~encrypt[" + index + "]";

        if (m.endsWith(encrypted)) {
            return m.substring(0, m.length() - encrypted.length());
        }

        return m + "~decrypt[" + index + "]";
    }

    @Override
    public String toString() {
        return "dk[" + index + "]";
    }
}
