package com.shuffle.player;

import com.shuffle.protocol.message.Phase;

import java.io.Serializable;

/**
 * Created by Daniel Krawisz on 5/26/16.
 */
public class Payload implements Serializable {

    public final Message message;
    public final Phase phase;

    public Payload(
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

        if (!(o instanceof Payload)) {
            return false;
        }

        Payload p = ((Payload) o);

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
