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
import com.shuffle.sim.init.Initializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.TestNet3Params;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.IOException;
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
    private static final boolean override = false;
    private static final long defaultAmount = 17;
    private static final long defaultFee = 1;
    static final Initializer.Type defaultType = Initializer.Type.TCP;

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
        public final Crypto crypto;

        RealTestCase(String session) throws BitcoinCrypto.Exception, NoSuchAlgorithmException {
            super(new Bytestring(("CoinShuffle Shufflepuff " + session).getBytes()),
                    defaultType, defaultAmount, defaultFee);

            crypto = new BitcoinCrypto(NetworkParameters.fromID(NetworkParameters.ID_TESTNET));
        }

        @Override
        public Crypto crypto() {
            return crypto;
        }

        @Override
        protected Protobuf proto() {
            return new CryptoProtobuf(TestNet3Params.get());
        }
    }

    public class MockTestCase extends TestCase {
        private final Crypto crypto;

        MockTestCase(String session) {
            super(new Bytestring(("CoinShuffle Shufflepuff test " + session).getBytes()),
                    defaultType, defaultAmount, defaultFee);

            crypto = new MockCrypto(new InsecureRandom(seed++));
        }

        MockTestCase(String session, Initializer.Type type, long amount, long fee) {
            super(new Bytestring(("CoinShuffle Shufflepuff test " + session).getBytes()),
                    type, amount, fee);

            crypto = new MockCrypto(new InsecureRandom(seed++));
        }

        @Override
        public Crypto crypto() {
            return crypto;
        }

        @Override
        protected Protobuf proto() {
            return new MockProtobuf();
        }
    }

    public class NoShuffleTestCase extends TestCase {
        private Crypto crypto;

        NoShuffleTestCase(String session) {
            super(new Bytestring(("CoinShuffle Shufflepuff test " + session).getBytes()),
                    defaultType, defaultAmount, defaultFee);
        }

        @Override
        public Crypto crypto() {
            if (crypto == null) {
                ++seed;
                crypto = new MockCrypto(new AlwaysZero());
            }
            return crypto;
        }

        @Override
        protected Protobuf proto() {
            return new MockProtobuf();
        }
    }

    public TestCase newTestCase(String session) {
        return new MockTestCase(session);
    }

    void check(InitialState init) throws ExecutionException, InterruptedException, IOException {
        int fail = 0;
        int success = 0;
        caseNo++;

        for (int i = 0; i < trials; i ++ ) {
            if (i % 10 == 0) {
                System.out.println("Trial " + i + " in progress. ");
            }

            Map<SigningKey, TestCase.Mismatch> mismatch = init.run();

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
