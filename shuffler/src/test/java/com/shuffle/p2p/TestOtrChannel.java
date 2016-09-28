package com.shuffle.p2p;

import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    /**
     * These are variables for establishing two-way encrypted communication
     * between Alice <-> Bob and Alice <-> Charlie.
     */

    MockNetwork<String, Bytestring> network;

    Channel<String, Bytestring> aliceNode;
    Channel<String, Bytestring> bobNode;
    Channel<String, Bytestring> charlieNode;

    OtrChannel<String> otrAlice;
    OtrChannel<String> otrBob;
    OtrChannel<String> otrCharlie;

    Connection<String> aliceConnection;
    Connection<String> bobConnection;
    Connection<String> charlieConnection;

    Send<Bytestring> aliceSend;
    Send<Bytestring> bobSend;
    Send<Bytestring> charlieSend;

    Peer<String, Bytestring> aliceToBob;
    Peer<String, Bytestring> bobToAlice;

    Peer<String, Bytestring> aliceToCharlie;
    Peer<String, Bytestring> charlieToAlice;

    Session<String, Bytestring> aliceToBobSession;
    Session<String, Bytestring> bobToAliceSession;

    Session<String, Bytestring> aliceToCharlieSession;
    Session<String, Bytestring> charlieToAliceSession;

    @Before
    public void setup() throws InterruptedException, IOException {
        network = new MockNetwork<>();

        /**
         * Alice, Bob, and Charlie as nodes in the MockNetwork.
         */

        aliceNode = network.node("alice");
        bobNode = network.node("bob");
        charlieNode = network.node("charlie");

        /**
         * Alice, Bob, and Charlie -- each with their own OtrChannel objects.
         */

        otrAlice = new OtrChannel<>(aliceNode, "alice");
        otrBob = new OtrChannel<>(bobNode, "bob");
        otrCharlie = new OtrChannel<>(charlieNode, "charlie");

        /**
         * Setting up Alice, Bob, and Charlie's Send<> and Listener<> objects.
         */

        aliceSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                System.out.println("Alice received: " + new String(message.bytes));
                return true;
            }

            @Override
            public void close() {
                System.out.println("aliceSend closed");
            }
        };

        Listener<String, Bytestring> aliceListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                System.out.println("Alice's listener caught: " + session);
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
                System.out.println("bobSend closed");
            }
        };

        Listener<String, Bytestring> bobListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {

                /**
                 * This Session object is an OtrSession object because of how we constructed
                 * OtrListener's newSession() method.
                 */

                bobToAliceSession = session;
                System.out.println("Bob's listener caught: " + session);
                return bobSend;
            }
        };

        charlieSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                System.out.println("Charlie received: " + new String(message.bytes));
                return true;
            }

            @Override
            public void close() {
                System.out.println("charlieSend closed");
            }
        };

        Listener<String, Bytestring> charlieListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {

                /**
                 * This Session object is an OtrSession object because of how we constructed
                 * OtrListener's newSession() method.
                 */

                charlieToAliceSession = session;
                System.out.println("Charlie's listener caught: " + session);
                return charlieSend;
            }
        };

        // Open all Channels and start listening
        aliceConnection = otrAlice.open(aliceListener);
        bobConnection = otrBob.open(bobListener);
        charlieConnection = otrCharlie.open(charlieListener);

    }

    @Test
    public void encryptedChat() throws InterruptedException, IOException {

        // Alice to Bob
        aliceToBob = otrAlice.getPeer("bob");

        // Bob to Alice
        bobToAlice = otrBob.getPeer("alice");

        // Alice to Charlie
        aliceToCharlie = otrAlice.getPeer("charlie");

        // Charlie to Alice
        charlieToAlice = otrCharlie.getPeer("alice");

        // Alice opens the session to Bob
        aliceToBobSession = aliceToBob.openSession(aliceSend);

        // bobToAliceSession is created in bobListener.newSession()

        // Alice opens the session to Charlie
        aliceToCharlieSession = aliceToCharlie.openSession(aliceSend);

        // charlieToAliceSession is created in charlieListener.newSession()

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

        // Alice sends Bob a message
        aliceToBobSession.send(new Bytestring("Bob, Do Not Buy OneCoin".getBytes()));

        // Bob sends Alice a message
        bobToAliceSession.send(new Bytestring("CryptoCurrency OneCoin is a Virus".getBytes()));

        // Alice sends Charlie a message
        aliceToCharlieSession.send(new Bytestring("CHARLIE DONT BUY ONECOIN".getBytes()));

        // Charlie sends Alice a message
        charlieToAliceSession.send(new Bytestring("THE NSA HAS OUR BACK".getBytes()));

    }

    @After
    public void shutdown() throws InterruptedException, IOException {

        aliceToBobSession.close();
        bobToAliceSession.close();

        aliceToCharlieSession.close();
        charlieToAliceSession.close();

        aliceConnection.close();
        bobConnection.close();
        charlieConnection.close();

        Assert.assertTrue(aliceToBobSession.closed());
        Assert.assertTrue(bobToAliceSession.closed());

        Assert.assertTrue(aliceToCharlieSession.closed());
        Assert.assertTrue(charlieToAliceSession.closed());

        Assert.assertTrue(aliceConnection.closed());
        Assert.assertTrue(bobConnection.closed());
        Assert.assertTrue(charlieConnection.closed());
    }

}
