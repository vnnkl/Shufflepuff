/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.impl.CryptoProtobuf;
import com.shuffle.mock.AlwaysZero;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockProtobuf;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Protobuf;
import com.shuffle.sim.InitialState;
import com.shuffle.sim.TestCase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.NetworkParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Integration tests for the protocol.
 *
 * Created by Daniel Krawisz on 12/10/15.
 */
public class TestShuffleMachine {
    static final Logger log = LogManager.getLogger(TestShuffleMachine.class);
    private static final int defaultSeed = 99;
    private static final int defaultTrials = 1;
    private static final boolean override = true;

    private int seed = 99;

    int caseNo = 0;

    private int trials = 1;

    public TestShuffleMachine() {
        seed = defaultSeed;
        trials = defaultTrials;
    }

    public TestShuffleMachine(int seed, int trials) {
        if (override) {
            this.seed = defaultSeed;
            this.trials = defaultTrials;
        } else {
            this.seed = seed;
            this.trials = trials;
        }
    }

    public class Report {
        final int trials;
        final int fail;
        final int success;

        public Report(int trials, int fail, int success) {
            this.trials = trials;
            this.fail = fail;
            this.success = success;
        }
    }

    private final List<Report> reports = new LinkedList<>();

    public class RealTestCase extends TestCase {

        RealTestCase(String session) {
            super(17, new Bytestring(("CoinShuffle Shufflepuff " + session).getBytes()));
        }

        @Override
        protected Crypto crypto() throws NoSuchAlgorithmException {
            return new BitcoinCrypto(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
        }

        @Override
        protected Protobuf proto() {
            return new CryptoProtobuf();
        }
    }

    public class MockTestCase extends TestCase {

        MockTestCase(String session) {
            super(17, new Bytestring(("CoinShuffle Shufflepuff test " + session).getBytes()));
        }

        @Override
        protected Crypto crypto() {
            return new MockCrypto(new InsecureRandom(seed++));
        }

        @Override
        protected Protobuf proto() {
            return new MockProtobuf();
        }
    }

    public class NoShuffleTestCase extends TestCase {

        NoShuffleTestCase(String session) {
            super(17, new Bytestring(("CoinShuffle Shufflepuff test " + session).getBytes()));
        }

        @Override
        protected Crypto crypto() {
            ++seed;
            return new MockCrypto(new AlwaysZero());
        }

        @Override
        protected Protobuf proto() {
            return new MockProtobuf();
        }
    }

    void check(InitialState init) throws ExecutionException, InterruptedException {
        int fail = 0;
        int success = 0;
        caseNo++;

        for (int i = 0; i < trials; i ++ ) {
            if (i % 10 == 0) {
                System.out.println("Trial " + i + " in progress. ");
            }

            Map<SigningKey, TestCase.Mismatch> mismatch
                    = com.shuffle.sim.TestCase.test(init);

            if (mismatch != null && mismatch.isEmpty() ) {
                success ++;
            } else {
                fail ++;
                //break;
            }
        }

        System.out.println("of " + trials + " trials, "
                + success + " successes and " + fail + " failures. ");

        reports.add(new Report(trials, fail, success));
    }

    @Before
    public void resetCaseNumber() {
        caseNo = 0;
    }

    @After
    public void printReport() {

        int i = 1;
        boolean success = true;
        for (Report report : reports) {
            System.out.println("Result for test " + i);
            if (report.fail > 0) success = false;
            System.out.println("   Trials: " + report.trials
                    + "; success: " + report.success + "; fail: " + report.fail);
            i ++;
        }

            Assert.assertTrue(success);
    }

    /*@Test
    public void testLies() {

        // A player lies about the equivocation check.
        // A player claims something went wrong in phase 2 when it didn't.
        Assert.fail();
    }

    @Test
    // Players disconnect at different points during the protocol.
    // TODO must include cases in which a malicious player disconnects after sending a malicious message!!
    public void testDisconnect() {
        Assert.fail();
    }*/
}
