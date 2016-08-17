/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.impl.BitcoinCrypto;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

/**
 * It is possible for the last player to equivocate during the broadcast phase by sending a
 * different list of addresses to different players. This should also be detected in the
 * equivocation check phase.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestEquivocateBroadcast extends TestShuffleMachine {

    // Run a test case for equivocation during phase 3.
    private void EquivocateOutput(int numPlayers, int[] equivocation)
            throws NoSuchAlgorithmException, ExecutionException,
            InterruptedException, BitcoinCrypto.Exception {
        String description = "case " + caseNo + "; broadcast equivocation test case.";
        check(newTestCase(description).equivocateBroadcastTestCase(numPlayers, equivocation));
    }

    @Test
    // Tests for malicious players who send different output vectors to different players.
    public void testEquivocationBroadcast()
            throws NoSuchAlgorithmException, ExecutionException,
            InterruptedException, BitcoinCrypto.Exception {

        // A player sends different output vectors to different players.
        EquivocateOutput(3, new int[]{1});
        EquivocateOutput(3, new int[]{2});
        EquivocateOutput(4, new int[]{1});
        EquivocateOutput(4, new int[]{1, 2});
        EquivocateOutput(10, new int[]{3, 5, 7});
    }
}
