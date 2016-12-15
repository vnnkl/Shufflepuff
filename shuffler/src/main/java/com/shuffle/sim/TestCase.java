/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.sim.init.Initializer;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TestCase attempts to provide a unified way of constructing test cases for different
 * implementations of CoinShuffle.
 *
 * Created by Daniel Krawisz on 3/15/16.
 */
public abstract class TestCase {

    public static final class Mismatch {
        public final SigningKey player;
        public final Matrix expected; // Can be null.
        public final Matrix result;   // Can be null.

        Mismatch(SigningKey player, Matrix expected, Matrix result) {
            if (player == null) throw new NullPointerException();

            this.player = player;
            this.expected = expected;
            this.result = result;
        }
    }

    public final long amount;
    public final long fee;

    public final Bytestring session;

    public final Initializer.Type type;

    protected TestCase(Bytestring session, Initializer.Type type, long amount, long fee) {
        this.amount = amount;
        this.fee = fee;
        this.session = session;
        this.type = type;
    }

    // Get the cryptography service for this test case (could be mock crypto or real, depending
    // on what we're testing.)
    public abstract Crypto crypto();
    protected abstract Protobuf proto();

    // An initial state containing no malicious players.
    public final InitialState successfulTestCase(final int numPlayers)
            throws BitcoinCrypto.Exception, NoSuchAlgorithmException {

        InitialState init = new InitialState(this);

        for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
        }

        return init;
    }

    // Initial state for cases in which players cannot afford to engage in the round, for
    // one reason or another.
    public final InitialState insufficientFundsTestCase(
            final int numPlayers,
            final int[] deadbeats,
            final int[] poor,
            final int[] spenders) throws BitcoinCrypto.Exception, NoSuchAlgorithmException {

        InitialState init = new InitialState(this);

        pit : for (int i = 1; i <= numPlayers; i++) {
            init.player().initialFunds(20);
            for (int deadbeat : deadbeats) {
                if (deadbeat == i) {
                    init.initialFunds(0);
                    continue pit;
                }
            }
            for (int aPoor : poor) {
                if (aPoor == i) {
                    init.initialFunds(10);
                    continue pit;
                }
            }
            for (int spender : spenders) {
                if (spender == i) {
                    init.spend(16);
                }
            }
        }

        return init;
    }

    // Initial state for players who spend their funds while the protocol is running.
    public final InitialState doubleSpendTestCase(
            final int[] views,
            final int[] spenders
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {

        final Set<Integer> doubleSpenders = new HashSet<>();

        for (int d : spenders) {
            doubleSpenders.add(d);
        }

        InitialState init = new InitialState(this);
        for (int i = 0; i < views.length; i ++) {
            init.player().initialFunds(20).networkPoint(views[i]);

            if (doubleSpenders.contains(i + 1)) {
                init.doubleSpend(13);
            }
        }
        return init;
    }

    // Initial state for malicious players who equivocate during the announcement phase.
    public final InitialState equivocateAnnouncementTestCase(
            final int numPlayers,
            final Equivocation[] equivocators
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {

        InitialState init = new InitialState(this);

        int eq = 0;
        for (int i = 1; i <= numPlayers; i ++) {
            init.player().initialFunds(20);

            while (eq < equivocators.length && equivocators[eq].equivocator < i) {
                eq++;
            }

            if (eq < equivocators.length && equivocators[eq].equivocator == i) {
                init.equivocateAnnouncement(equivocators[eq].equivocation);
            }
        }

        return init;
    }

    // Initial state for a player who equivocates during the broadcast phase.
    public final InitialState equivocateBroadcastTestCase(
            final int numPlayers,
            final int[] equivocation) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {

        InitialState init = new InitialState(this);

        // Only the last player can equivocate.
        for (int i = 1; i < numPlayers; i ++) {
            init.player().initialFunds(20);
        }

        // Add the malicious equivocator.
        init.player().initialFunds(20).equivocateOutputVector(equivocation);

        return init;
    }

    // Initial state for players who shuffle their addresses incorrectly.
    public final InitialState dropAddressTestCase(
            final int numPlayers,
            final Map<Integer, Integer> drop,
            final int[][] replaceNew,
            final int[][] replaceDuplicate) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {

        final Map<Integer, Integer> replaceNewMap = new HashMap<>();
        final Map<Integer, Integer[]> replaceDuplicateMap = new HashMap<>();

        if (replaceDuplicate != null) {
            for (int[] d : replaceDuplicate) {
                if (d.length == 2 && d[1] < d[0]) {
                    replaceDuplicateMap.put(d[0], new Integer[]{d[1], d[2]});
                }
            }
        }

        if (replaceNew != null) {
            for (int[] d : replaceNew) {
                if (d.length == 2 && d[1] < d[0]) {
                    replaceNewMap.put(d[0], d[1]);
                }
            }
        }

        InitialState init = new InitialState(this);

        for (int i = 1; i <= numPlayers; i ++) {

            init.player().initialFunds(20);

            if (drop.containsKey(i)) {
                init.drop(drop.get(i));
            } else if (replaceDuplicateMap.containsKey(i)) {
                Integer[] dup = replaceDuplicateMap.get(i);
                init.replace(dup[0], dup[1]);
            } else if (replaceNewMap.containsKey(i)) {
                init.replace(replaceNewMap.get(i));
            }
        }

        return init;
    }

    // Initial state for players who make invalid signatures.
    public final InitialState invalidSignatureTestCase(
            final int numPlayers,
            final int[] mutants
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        final Set<Integer> mutantsSet = new HashSet<>();

        for (int mutant : mutants) {
            mutantsSet.add(mutant);
        }

        InitialState init = new InitialState(this);

        for (int i = 1; i <= numPlayers; i ++) {
            init.player().initialFunds(20);

            if (mutantsSet.contains(i)) {
                init.mutateTransaction();
            }
        }

        return init;
    }

    // A class used to define certain kinds of initial states.
    public static class Equivocation {
        final int equivocator;
        final int[] equivocation;

        public Equivocation(int equivocator, int[] equivocation) {
            // Testing the case where the first player is the equivocator is too hard for now.
            // It would require basically writing a whole new version of protocolDefinition()
            // to be a test function. It is unlikely that testing case will find a bug in the code.
            if (equivocator == 1) {
                throw new IllegalArgumentException();
            }

            for (int eq : equivocation) {
                if (eq <= equivocator) {
                    throw new IllegalArgumentException();
                }
            }

            this.equivocator = equivocator;
            this.equivocation = equivocation;
        }

        @Override
        public String toString() {
            return "equivocation[" + equivocator + ", " + Arrays.toString(equivocation) + "]";
        }
    }
}
