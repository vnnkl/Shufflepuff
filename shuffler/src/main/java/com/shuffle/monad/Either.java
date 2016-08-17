/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.monad;

/**
 * Represents a type that is either X or Y, not exclusive. 
 *
 * Created by Daniel Krawisz on 3/18/16.
 */
public class Either<X, Y> {
    public final X first;
    public final Y second;

    public Either(X first, Y second) {
        // Both cannot be null.
        if (first == null && second == null) {
            throw new NullPointerException();
        }

        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        if (first != null) {
            return first.toString();
        }

        return second.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Either<?, ?> either = (Either<?, ?>) o;

        return first != null ? first.equals(either.first) : either.first == null
                && (second != null ? second.equals(either.second) : either.second == null);

    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
