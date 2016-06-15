/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.player;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 12/6/15.
 */
public class SessionIdentifier implements com.shuffle.chan.packet.SessionIdentifier, Serializable {
    public final String version;
    public final String id;

    private SessionIdentifier(String version, String id) {
        this.version = version;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof SessionIdentifier)) {
            return false;
        }

        SessionIdentifier s = (SessionIdentifier) o;

        return this == s || id.equals(s.id);
    }

    @Override
    public String toString() {
        return "session[" + id + "]";
    }

    @Override
    public String protocol() {
        return "test protocol";
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String id() {
        return id;
    }

    public static SessionIdentifier TestSession(String id) {
        return new SessionIdentifier(
                "Shufflepuff Coinshuffle test", id);
    }

    public static SessionIdentifier Session(String id) {
        return new SessionIdentifier(
                "Shufflepuff CoinShuffle alpha", id);
    }
}
