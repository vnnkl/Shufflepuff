/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Crypto;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockProtobuf;
import com.shuffle.monad.Either;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.blame.Matrix;
import com.shuffle.sim.InitialState;
import com.shuffle.sim.Simulator;

import org.junit.Assert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Tests for a successful run of the protocol.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestSuccessfulRun extends TestShuffleMachine {

    // Create a test case representing a successful run.
    private void SuccessfulRun(int numPlayer)
            throws NoSuchAlgorithmException, ExecutionException,
            InterruptedException, BitcoinCrypto.Exception {

        String description = "case " + caseNo + "; successful run with " + numPlayer + " players.";
        check(newTestCase(description).successfulTestCase(numPlayer));
    }

    @Test
    // Tests for successful runs of the protocol.
    public void testSuccess()
            throws NoSuchAlgorithmException, ExecutionException,
            InterruptedException, BitcoinCrypto.Exception {

        // Tests for successful runs.
        int minPlayers = 2;
        int maxPlayers = 13;
        for (int numPlayer = minPlayers; numPlayer <= maxPlayers; numPlayer++) {
            log.info("Protocol successful run with " + numPlayer + " players.");
            SuccessfulRun(numPlayer);
        }
    }

    private static class ChangeTestInput {
        public final int amount;
        public final boolean change;

        private ChangeTestInput(int amount, boolean change) {
            this.amount = amount;
            this.change = change;
        }
    }

    private static class ChangeTestCase {
        public final int amount;
        public final int fee;
        public final ChangeTestInput[] inputs;

        private ChangeTestCase(int amount, int fee, ChangeTestInput... inputs) {
            for (ChangeTestInput input : inputs) {
                if (input == null || input.amount < amount) throw new IllegalArgumentException();
            }

            this.amount = amount;
            this.fee = fee;
            this.inputs = inputs;
        }
    }

    private static class ChangeTestExpected {
        public final Address address;
        public final int expected;

        private ChangeTestExpected(Address address, int expected) {
            this.address = address;
            this.expected = expected;
        }
    }

    @Test
    // Test that the resulting transaction has the correct outputs.
    public void testOutputs() throws ExecutionException, InterruptedException {
        ChangeTestCase[] tests = new ChangeTestCase[]{
                new ChangeTestCase(37, 1,
                        new ChangeTestInput(45, true), new ChangeTestInput(78, true)),
                new ChangeTestCase(37, 1,
                        new ChangeTestInput(45, true), new ChangeTestInput(78, false)),
                new ChangeTestCase(37, 1,
                        new ChangeTestInput(45, false),
                        new ChangeTestInput(78, true),
                        new ChangeTestInput(99, true)),
        };

        Crypto mc = new MockCrypto(new InsecureRandom(3928));

        int i = 0;
        for (ChangeTestCase test : tests) {
            i ++;
            Bytestring session = new Bytestring(("change test case " + i).getBytes());
            InitialState init = new InitialState(session, test.amount, test.fee, mc, new MockProtobuf());

            // First create the bitcoin mock network.
            List<ChangeTestExpected> expected = new LinkedList<>();
            for (ChangeTestInput input : test.inputs) {
                init.player().initialFunds(input.amount);
                if (input.change) {
                    Address addr = mc.makeSigningKey().VerificationKey().address();
                    expected.add(new ChangeTestExpected(addr, input.amount - test.amount - test.fee));

                    init.change(addr);
                }
            }

            // Run the simulation.
            Map<SigningKey, Either<Transaction, Matrix>> results = Simulator.run(init);

            Assert.assertNotNull(results);

            System.out.println("Got results " + results);

            // All the transactions should be the same, so we only need to get one.
            // If they are different, this will have resulted in errors being generated
            // in other tests.
            MockCoin.MockTransaction t = null;
            for (Either<Transaction, Matrix> r : results.values()) {
                t = (MockCoin.MockTransaction)r.first;

                Assert.assertNotNull(t);
            }

            List<MockCoin.Output> outputs = t.outputs;

            int expectedSize = test.inputs.length + expected.size();
            Assert.assertTrue("Expected " + expectedSize + " got " + outputs.size(),
                    outputs.size() == expectedSize);
            List<MockCoin.Output> anon = outputs.subList(0, test.inputs.length);
            List<MockCoin.Output> change = outputs.subList(test.inputs.length, expectedSize);

            // The first set of inputs are the anonymous inputs.
            for (MockCoin.Output output : anon ) {
                Assert.assertEquals(test.amount, output.amountHeld);
            }

            // The second set are the change outputs.
            Iterator<ChangeTestExpected> ex = expected.iterator();
            for (MockCoin.Output output : change) {
                ChangeTestExpected next = ex.next();
                Assert.assertEquals(output.address, next.address);
                Assert.assertEquals(output.amountHeld, next.expected);
            }
        }

    }
}
