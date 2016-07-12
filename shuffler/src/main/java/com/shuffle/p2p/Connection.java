/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

/**
 * Represents an open connection. The Connection is produced from the Channel's open() method.
 *
 * It is used to close the channel.
 *
 * Created by Daniel Krawisz on 2/22/16.
 */
public interface Connection<Address> {

    // Once closed, a Connection cannot be reopened. Instead, a new Connection is created
    // by the Channel's open method.
    void close();

    // Whether the channel has been closed. After this method returns true, it can only return true
    // after that.
    boolean closed();
}
