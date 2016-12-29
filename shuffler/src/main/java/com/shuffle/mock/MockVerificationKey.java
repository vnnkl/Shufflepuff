/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * A mock implementation of a VerificationKey.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
public class MockVerificationKey implements VerificationKey, Serializable {
    public final int index;
    private static Pattern pattern = Pattern.compile("^vk\\[[0-9]+\\]$");

    public MockVerificationKey(int index) {
        this.index = index;
    }

    public MockVerificationKey(String str) throws NumberFormatException {

        if (!pattern.matcher(str).matches()) {
            throw new IllegalArgumentException();
        }

        index = Integer.parseInt(str.substring(3, str.length() - 1));

    }

    @Override
    public boolean verify(Bytestring payload, Bytestring signature) {
        return Arrays.equals(signature.bytes, new MockSigningKey(index).sign(payload).bytes);
    }

    @Override
    public boolean equals(Object vk) {
        return vk != null
                && vk instanceof MockVerificationKey && index == ((MockVerificationKey) vk).index;

    }

    @Override
    public Address address() {
        return new MockAddress(Integer.toString(index));
    }

    public String toString() {
        return "vk[" + index + "]";
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof MockVerificationKey)) {
            return -1;
        }

        MockVerificationKey key = ((MockVerificationKey)o);

        if (index == key.index) {
            return 0;
        }

        if (index < key.index) {
            return -1;
        }

        return 1;
    }
}
