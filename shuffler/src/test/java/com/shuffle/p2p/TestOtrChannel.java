package com.shuffle.p2p;

import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import net.java.otr4j.OtrException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    MockNetwork<String, Bytestring> network;

    Channel<String, Bytestring> aliceNode;
    Channel<String, Bytestring> bobNode;

    OtrChannel<String> otrAlice;
    OtrChannel<String> otrBob;

    Send<Bytestring> aliceSend;
    Send<Bytestring> bobSend;

    Peer<String, Bytestring> aliceToBob;
    Peer<String, Bytestring> bobToAlice;

    Session<String, Bytestring> aliceToBobSession;
    Session<String, Bytestring> bobToAliceSession;

    @Before
    public void setup() throws InterruptedException, IOException {
        network = new MockNetwork<>();
        aliceNode = network.node("alice");
        bobNode = network.node("bob");
        otrAlice = new OtrChannel<>(aliceNode, "alice");
        otrBob = new OtrChannel<>(bobNode, "bob");

        aliceSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                System.out.println("Alice received: " + new String(message.bytes));
                return true;
            }

            @Override
            public void close() {

            }
        };

        Listener<String, Bytestring> aliceListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                System.out.println("alice listener caught: " + session);
                return aliceSend;
            }
        };

        bobSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                System.out.println("Bob received: " + new String(message.bytes));
                return true;
            }

            @Override
            public void close() {

            }
        };

        Listener<String, Bytestring> bobListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {

                /**
                 * This Session object is an OtrSession object because of how we constructed
                 * OtrListener's newSession method.
                 */
                bobToAliceSession = session;
                System.out.println("bob listener caught: " + session);
                return bobSend;
            }
        };

        // Open both Channels and start listening
        otrAlice.open(aliceListener);
        otrBob.open(bobListener);

    }

    @Test
    public void encryptedChat() throws InterruptedException, IOException {

        /**
         * close aliceToBobSession, see if closing aliceToBob closes aliceToBobSession
         */

        // Alice to Bob
        aliceToBob = otrAlice.getPeer("bob");

        // Bob to Alice
        bobToAlice = otrBob.getPeer("alice");

        // Alice opens the session
        aliceToBobSession = aliceToBob.openSession(aliceSend);

        // ASCII because why not.
        System.out.println("");
        System.out.println("         *~------------------------------------------~* ");
        System.out.println("         ||     _______     _________   ______       ||  ");
        System.out.println("         ||    //      \\\\   !_______!  !!  .. \\      ||   ");
        System.out.println("         ||    !|  **  !|      !.!      !! **  |     ||    ");
        System.out.println("         ||    !| **** !|      !.!      |!`  //      ||     ");
        System.out.println("         ||    !|  **  !|      !.!      !|!  \\\\      ||    ");
        System.out.println("         ||    \\\\_____//       !.!      |!!   \\\\     ||   ");
        System.out.println("         ||                                          ||  ");
        System.out.println("         *~------------------------------------------~* ");
        System.out.println("");

        aliceToBobSession.send(new Bytestring("Bob, Do Not Buy OneCoin".getBytes()));
        bobToAliceSession.send(new Bytestring("CryptoCurrency OneCoin is a Virus".getBytes()));

    }

    // TODO
    @After
    public void shutdown() {

    }

}
