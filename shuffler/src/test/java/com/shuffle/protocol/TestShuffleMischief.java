/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.google.common.collect.ImmutableMap;
import com.shuffle.bitcoin.impl.BitcoinCrypto;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * During the shuffle phase, a player can do various things wrong. Elaborate checks are required
 * to detect and identify the mischief-maker.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestShuffleMischief extends TestShuffleMachine {

    public TestShuffleMischief() {
        super(99, 1);
    }

    // Run a test case for a player who drops an address in phase 2.
    private void DropAddress(
            int numPlayers,
            Map<Integer, Integer> drop,
            int[][] replaceNew,
            int[][] replaceDuplicate
    ) throws NoSuchAlgorithmException, ExecutionException, InterruptedException, BitcoinCrypto.Exception {
        if (drop == null) {
            drop = ImmutableMap.of();
        }

        String description = "case " + caseNo + "; shuffle phase mischief test case.";
        check(newTestCase(description).dropAddressTestCase(
                numPlayers, drop, replaceNew, replaceDuplicate
        ));
    }

    @Test
    public void testDropAddress() throws NoSuchAlgorithmException,
            ExecutionException, InterruptedException, BitcoinCrypto.Exception {
        DropAddress(2, ImmutableMap.of(1, 1), null, null);
        DropAddress(2, ImmutableMap.of(2, 1), null, null);
        DropAddress(2, ImmutableMap.of(2, 2), null, null);
        DropAddress(3, ImmutableMap.of(2, 1), null, null);
        DropAddress(3, ImmutableMap.of(3, 2), null, null);
        DropAddress(3, ImmutableMap.of(3, 3), null, null);
        DropAddress(3, ImmutableMap.of(3, 1), null, null);
        DropAddress(4, ImmutableMap.of(3, 2), null, null);
        DropAddress(4, ImmutableMap.of(4, 1), null, null);
        DropAddress(4, ImmutableMap.of(4, 2), null, null);
    }

    @Test
    public void testDropAddressReplaceNew() throws NoSuchAlgorithmException,
            InterruptedException, ExecutionException, BitcoinCrypto.Exception {
        DropAddress(3, null, new int[][]{new int[]{2, 1}}, null);
        DropAddress(3, null, new int[][]{new int[]{3, 2}}, null);
        DropAddress(4, null, new int[][]{new int[]{3, 2}}, null);
    }

    @Test
    public void testDropAddressDuplicate() throws NoSuchAlgorithmException,
            InterruptedException, ExecutionException, BitcoinCrypto.Exception {

        // A player drops an address and adds a duplicate in phase 2.
        DropAddress(4, null, null, new int[][]{new int[]{3, 1, 2}});
        DropAddress(4, null, null, new int[][]{new int[]{4, 3, 2}});
        DropAddress(5, null, null, new int[][]{new int[]{4, 3, 2}});

    }
}
