package com.shuffle.sim.init;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.HistoryChannel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A class that creates connections between players in a simulation.
 *
 * Created by Daniel Krawisz on 5/21/16.
 */
public interface Initializer<X> {

    // This function is called every time a new player is created in a simulation.
    Communication<X> connect(SigningKey sk) throws IOException, InterruptedException;

    Map<VerificationKey, Map<VerificationKey, List<HistoryChannel<VerificationKey, Signed<X>>.HistorySession>>> end();

    enum Type {
        Basic,
        Mock,
        Marshall,
        OTR,
        TCP,
    }
}
