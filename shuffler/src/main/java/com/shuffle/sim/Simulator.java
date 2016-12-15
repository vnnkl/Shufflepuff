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
import com.shuffle.p2p.HistoryChannel;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        // Check that all messages sent and received are the same.
        Map<VerificationKey, Map<VerificationKey, List<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession>>> histories = initializer.end();
        if (histories == null) return results;

        Set<VerificationKey> used = new HashSet<>();
        for (Map.Entry<VerificationKey, Map<VerificationKey, List<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession>>> entryFrom : histories.entrySet()) {
            VerificationKey fromKey = entryFrom.getKey();
            used.add(fromKey);

            for (Map.Entry<VerificationKey, List<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession>> entryTo : entryFrom.getValue().entrySet()) {
                VerificationKey toKey = entryTo.getKey();
                if (used.contains(toKey)) continue; // Don't check the same set of messages twice.

                // These should be identical, since they are histories in both directions
                // from a pair of players.
                List<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession> historyFrom = entryTo.getValue();
                List<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession> historyTo = histories.get(entryTo.getKey()).get(fromKey);

                // These should be the same size.
                if (historyFrom.size() != historyTo.size()) {
                    System.out.println("  Unequal session sizes: " + historyFrom.size() + " to " + historyTo.size());
                    return null;
                }

                Iterator<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession> isf = historyFrom.iterator();
                Iterator<HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession> ist = historyTo.iterator();

                while (isf.hasNext()) {
                    HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession sessionFrom = isf.next();
                    HistoryChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>>.HistorySession sessionTo = ist.next();

                    List<Signed<Packet<VerificationKey, Payload>>> fromReceived = sessionFrom.received();
                    List<Signed<Packet<VerificationKey, Payload>>> toReceived = sessionTo.received();

                    List<Signed<Packet<VerificationKey, Payload>>> fromSent = sessionFrom.sent();
                    List<Signed<Packet<VerificationKey, Payload>>> toSent = sessionTo.sent();

                    if (fromSent.size() != toReceived.size()) {
                        System.out.println("    Unequal session sent sizes: " + toReceived.size() + " to " + fromSent.size());
                        return null;
                    }

                    if (fromReceived.size() != toSent.size()) {
                        System.out.println("    Unequal session received sizes: " + fromReceived.size() + " to " + toSent.size());
                        return null;
                    }

                    Iterator<Signed<Packet<VerificationKey, Payload>>> ipfr = fromReceived.iterator();
                    Iterator<Signed<Packet<VerificationKey, Payload>>> iptr = toReceived.iterator();
                    Iterator<Signed<Packet<VerificationKey, Payload>>> ipfs = fromSent.iterator();
                    Iterator<Signed<Packet<VerificationKey, Payload>>> ipts = toSent.iterator();

                    while (ipfr.hasNext()) {
                        Signed<Packet<VerificationKey, Payload>> received = ipfr.next();
                        Signed<Packet<VerificationKey, Payload>> sent = ipts.next();

                        if (!received.equals(sent)) {
                            System.out.println("Message from " + toKey + " to " + fromKey + " sent as " + sent + " but received as " + received);

                            return null;
                        }
                    }

                    while (ipfs.hasNext()) {
                        Signed<Packet<VerificationKey, Payload>> received = iptr.next();
                        Signed<Packet<VerificationKey, Payload>> sent = ipfs.next();

                        if (!received.equals(sent)) {
                            System.out.println("Message from " + fromKey + " to " + toKey + " sent as " + sent + " but received as " + received);

                            return null;
                        }
                    }
                }
            }
        }

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
