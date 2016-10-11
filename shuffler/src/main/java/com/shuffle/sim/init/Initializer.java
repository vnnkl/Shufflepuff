package com.shuffle.sim.init;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.p2p.Bytestring;

import java.io.IOException;

/**
 * A class that creates connections between players in a simulation.
 *
 * Created by Daniel Krawisz on 5/21/16.
 */
public interface Initializer<X> {

    // This function is called every time a new player is created in a simulation.
    Communication<X> connect(SigningKey sk) throws IOException, InterruptedException;

    void clear();

    public enum Type {
        Basic,
        Mock,
        OTR,
    }

    static <X> Initializer<X> make(Type type, Bytestring session, int capacity) {
        switch (type) {
            case Basic: {
                return new BasicInitializer(session, capacity);
            }
            case Mock: {
                return new ChannelInitializer(session, capacity);
            }
            case OTR: {
                throw new IllegalArgumentException();
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }
}
