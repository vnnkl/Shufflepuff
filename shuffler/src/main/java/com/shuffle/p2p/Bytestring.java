/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * A simple wrapper for []byte.
 *
 * Created by Daniel Krawisz on 1/19/16.
 */

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A wrapper for []byte.
 *
 * Created by Daniel Krawisz on 12/19/15.
 */
public class Bytestring implements Serializable {
    public final byte[] bytes;

    public Bytestring(byte[] bytes) {
        this.bytes = bytes;
    }

    public Bytestring prepend(Bytestring pre) {
        ByteBuffer target = ByteBuffer.wrap(new byte[bytes.length + pre.bytes.length]);
        target.put(pre.bytes);
        target.put(bytes);
        return new Bytestring(target.array());
    }

    public Bytestring append(Bytestring post) {
        return post.prepend(this);
    }

    private Bytestring cp9(int last, int next) {
        return new Bytestring(Arrays.copyOfRange(bytes, last, next));
    }

    public Bytestring[] chop(int[] where) {

        List<Bytestring> l = new LinkedList<>();
        int last = 0;
        int i = 0;

        for (int next : where) {
            if (next <= last) {
                throw new IllegalArgumentException("chop locations have to be positive and increasing");
            }

            if (next >= bytes.length) {
                throw new IllegalArgumentException("trying to chop beyond the end?");
            }

            Bytestring section = cp9(last, next);
            l.add(section);
            i += section.bytes.length;

            last = next;
        }

        if (i < bytes.length) {
            l.add(cp9(last, bytes.length));
        }

        return l.toArray(new Bytestring[l.size()]);
    }

    public Bytestring xor(Bytestring b) {
        if (bytes.length != b.bytes.length) {
            throw new IllegalArgumentException("different lengths not allowed");
        }

        byte[] newBytes = new byte[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            newBytes[i] = (byte)(bytes[i] ^ b.bytes[i]);
        }

        return new Bytestring(newBytes);
    }

    public Bytestring drop(int a) {
        if (a < 0) {
            return take(0, bytes.length + a);
        } else {
            return take(a, bytes.length);
        }
    }

    public Bytestring take(int a, int b) {
        int from, to;
        if (a < 0) {
            from = bytes.length - a;
        } else {
            from = a;
        }

        if (b < 0) {
            to = bytes.length - b;
        } else {
            to = b;
        }

        if (to <= from || to > bytes.length || from > bytes.length) {
            throw new IllegalArgumentException();
        }

        return new Bytestring(Arrays.copyOfRange(bytes, from, to));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bytestring && Arrays.equals(bytes, ((Bytestring) o).bytes);

    }

    @Override
    public int hashCode() {
        int code = 0;
        for (int i : bytes) {
            code += i;
        }

        return code;
    }

    @Override
    public String toString() {
        return "Bytestring" + Arrays.toString(bytes);
    }
}
