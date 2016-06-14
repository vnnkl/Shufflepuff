/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;

import java.io.Serializable;

/**
 * Mock Bitcoin address used for testing.
 *
 * Created by Daniel Krawisz on 12/19/15.
 */
public class MockAddress implements Address, Serializable {
    public final String addr;

    public MockAddress(String addr) {
        if (addr == null) throw new NullPointerException();
        this.addr = addr;
    }

    public MockAddress(int index) {
        this.addr = Integer.toString(index);
    }

    public MockAddress(Long l) {
        this.addr = l.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof MockAddress && addr.equals(((MockAddress) o).addr);

    }

    @Override
    public int hashCode() {
        return addr.hashCode();
    }

    @Override
    public String toString() {
        return "ad[" + addr + "]";
    }

    @Override
    public int compareTo(Address address) {
        if (address instanceof MockDecryptedAddress) {
            return -1;
        }

        if (address instanceof MockEncryptedAddress) {
            return 1;
        }

        if (!(address instanceof MockAddress)) {
            return 0;
        }

        return addr.compareTo(((MockAddress)address).addr);
    }
}
