package com.shuffle.player;

import com.shuffle.protocol.message.Phase;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 5/26/16.
 */
public class P implements Serializable {

    public final Message message;
    public final Phase phase;

    P(
            Phase phase,
            Message message
    ) {

        this.phase = phase;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof P)) {
            return false;
        }

        P p = ((P) o);

        return phase == p.phase
                && message.equals(p.message);
    }

    @Override
    public int hashCode() {
        int hash = message.hashCode() + 17 * phase.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "{" + message.toString() + ", " + phase.toString() + "}";
    }
}
