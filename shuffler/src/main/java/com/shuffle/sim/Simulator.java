/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.MockProtobuf;
import com.shuffle.monad.Either;
import com.shuffle.monad.NaturalSummableFuture;
import com.shuffle.monad.SummableFuture;
import com.shuffle.monad.SummableFutureZero;
import com.shuffle.monad.SummableMaps;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Payload;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.sim.init.BasicInitializer;
import com.shuffle.sim.init.MarshallInitializer;
import com.shuffle.sim.init.Initializer;
import com.shuffle.sim.init.MockInitializer;
import com.shuffle.sim.init.OtrInitializer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A simulator for running integration tests on the protocol.
 *
 * Created by Daniel Krawisz on 12/6/15.
 */
public final class Simulator {
    private static final Logger log = LogManager.getLogger(Simulator.class);

    public final Initializer.Type type;
    public final Marshaller<Signed<Packet<VerificationKey, Payload>>> marshaller;

    public Simulator() {
        type = Initializer.Type.Basic;
        this.marshaller = new MockProtobuf().signedMarshaller();
    }

    public Simulator(Initializer.Type type) {
        if (type == null) throw new NullPointerException();

        if (type == Initializer.Type.OTR || type == Initializer.Type.Marshall) throw new IllegalArgumentException();

        this.type = type;
        this.marshaller = new MockProtobuf().signedMarshaller();
    }

    public Simulator(Initializer.Type type, Marshaller<Signed<Packet<VerificationKey, Payload>>> marshaller) {
        if (type == null || marshaller == null) throw new NullPointerException();

        this.type = type;
        this.marshaller = marshaller;
    }

    private Initializer<Packet<VerificationKey, Payload>> makeInitializer(Bytestring session, int numPlayers) {
        // An appropriate value for the capacity of the channels between the players.
        int capacity = 3 * (1 + numPlayers);

        switch (type) {
            case Basic:
                return new BasicInitializer<>(session, capacity);
            case Mock:
                return new MockInitializer<>(session, capacity);
            case Marshall:
                return new MarshallInitializer<>(session, capacity, marshaller);
            case OTR:
                return new OtrInitializer<>(session, capacity, marshaller);
            default:
                throw new IllegalArgumentException();
        }
    }

    public Map<SigningKey, Either<Transaction, Matrix>> run(
            InitialState init)
            throws ExecutionException, InterruptedException, IOException {

        final Initializer<Packet<VerificationKey, Payload>> initializer =
                makeInitializer(init.session(), init.size());

        final Map<SigningKey, Adversary> machines = init.getPlayers(initializer);

        Map<SigningKey, Either<Transaction, Matrix>> results = runSimulation(machines);

        initializer.clear(); // Avoid memory leak.
        return results;
    }

    private static synchronized Map<SigningKey, Either<Transaction, Matrix>> runSimulation(
            Map<SigningKey, Adversary> machines)  {

        // Create a future for the set of entries.
        SummableFuture<Map<SigningKey, Either<Transaction, Matrix>>> wait
                = new SummableFutureZero<>(
                        new SummableMaps<SigningKey, Either<Transaction, Matrix>>()
                );

        // Start the simulations.
        for (Adversary in : machines.values()) {
            wait = wait.plus(new NaturalSummableFuture<>(in.turnOn()));
        }

        try {
            return wait.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Returning null. This indicates that some player returned an exception "
                    + "and was not able to complete the protocol.");
            return null;
        }
    }
}
