/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.monad.Either;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.blame.Matrix;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * TestCase attempts to provide a unified way of constructing test cases for different
 * implementations of CoinShuffle.
 *
 * Created by Daniel Krawisz on 3/15/16.
 */
public abstract class TestCase {
    private static final Logger log = LogManager.getLogger(TestCase.class);

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

    // Returns a map containing the set of results which did not match expectations. An empty map
    // represents a successful test.
    public static Map<SigningKey, Mismatch> test(InitialState init)
            throws ExecutionException, InterruptedException, IOException {

        // Run the simulation.
        Map<SigningKey, Either<Transaction, Matrix>> results = Simulator.run(init);

        if (results == null) {
            return null;
        }

        // Get the expected values.
        Map<SigningKey, Matrix> expected = init.expected();

        // The result to be returned.
        Map<SigningKey, Mismatch> mismatch = new HashMap<>();

        // Check that the map of error states returned matches that which was expected.
        for (Map.Entry<SigningKey, Matrix> ex : expected.entrySet()) {
            SigningKey key = ex.getKey();
            Either<Transaction, Matrix> er = results.get(key);

            if (er == null) {
                return null;
            }

            Matrix result = er.second;
            Matrix expect = ex.getValue();

            if (!expect.match(result)) {
                log.error("  expected " + expect);
                log.error("  result   " + result);
                mismatch.put(key, new Mismatch(key, expect, result));
            }
        }

        return mismatch;
    }

    private final long amount;
    private final long fee;

    private final Bytestring session;

    protected TestCase(long amount, long fee, Bytestring session) {
        this.amount = amount;
        this.fee = fee;
        this.session = session;
    }

    // Get the cryptography service for this test case (could be mock crypto or real, depending
    // on what we're testing.)
    protected abstract Crypto crypto() throws NoSuchAlgorithmException, BitcoinCrypto.Exception;
    protected abstract Protobuf proto();

    public final InitialState successfulTestCase(final int numPlayers)
            throws BitcoinCrypto.Exception, NoSuchAlgorithmException {
        return InitialState.successful(session, amount, fee, crypto(), proto(), numPlayers);
    }

    public final InitialState insufficientFundsTestCase(
            final int numPlayers,
            final int[] deadbeats,
            final int[] poor,
            final int[] spenders) throws BitcoinCrypto.Exception, NoSuchAlgorithmException {
        return InitialState.insufficientFunds(
                session, amount, fee, crypto(), proto(), numPlayers, deadbeats, poor, spenders);
    }

    public final InitialState doubleSpendTestCase(
            final int[] views,
            final int[] spenders
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        return InitialState.doubleSpend(session, amount, fee, crypto(), proto(), views, spenders);
    }

    public final InitialState equivocateAnnouncementTestCase(
            final int numPlayers,
            final InitialState.Equivocation[] equivocators
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        return InitialState.equivocateAnnouncement(
                session, amount, fee, crypto(), proto(), numPlayers, equivocators);
    }

    public final InitialState equivocateBroadcastTestCase(
            final int numPlayers,
            final int[] equivocation) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        return InitialState.equivocateBroadcast(
                session, amount, fee, crypto(), proto(), numPlayers, equivocation);
    }

    public final InitialState dropAddressTestCase(
            final int numPlayers,
            final Map<Integer, Integer> drop,
            final int[][] replaceNew,
            final int[][] replaceDuplicate) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        return InitialState.dropAddress(
                session, amount, fee, crypto(), proto(), numPlayers, drop, replaceNew, replaceDuplicate);
    }

    public final InitialState invalidSignatureTestCase(
            final int numPlayers,
            final int[] mutants
    ) throws NoSuchAlgorithmException, BitcoinCrypto.Exception {
        return InitialState.invalidSignature(session, amount, fee, crypto(), proto(), numPlayers, mutants);
    }
}
