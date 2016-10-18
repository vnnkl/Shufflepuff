/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.protocol;

import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.sim.TestCase;

import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

/**
 * During the announcement phase, a player could send different encryption keys to other players.
 * This should be detected during the equivocation check phase.
 *
 * Created by Daniel Krawisz on 3/17/16.
 */
public class TestEquivocateAnnouncement extends TestShuffleMachine{

    // Run a test case for equivocation during phase 1.
    private void EquivocateAnnouncement(
            int numPlayers,
            TestCase.Equivocation[] equivocators
    ) throws NoSuchAlgorithmException, ExecutionException, InterruptedException, BitcoinCrypto.Exception, IOException {
        String description = "case " + caseNo + "; announcement equivocation test case.";
        check(newTestCase(description).equivocateAnnouncementTestCase(
                numPlayers, equivocators
        ));
    }

    @Test
    public void testEquivocationAnnounce()
            throws NoSuchAlgorithmException, ExecutionException, InterruptedException, BitcoinCrypto.Exception, IOException {

        // A player sends different encryption keys to different players.
        EquivocateAnnouncement(3,
                new TestCase.Equivocation[]{
                        new TestCase.Equivocation(2, new int[]{3})});
        EquivocateAnnouncement(5,
                new TestCase.Equivocation[]{
                        new TestCase.Equivocation(2, new int[]{4, 5})});
        EquivocateAnnouncement(10,
                new TestCase.Equivocation[]{
                        new TestCase.Equivocation(2, new int[]{4, 10}),
                        new TestCase.Equivocation(5, new int[]{7, 8})});
        EquivocateAnnouncement(10,
                new TestCase.Equivocation[]{
                        new TestCase.Equivocation(2, new int[]{3}),
                        new TestCase.Equivocation(4, new int[]{5, 6}),
                        new TestCase.Equivocation(8, new int[]{9})});
    }
}
