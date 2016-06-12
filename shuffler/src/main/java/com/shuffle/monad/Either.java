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
}
