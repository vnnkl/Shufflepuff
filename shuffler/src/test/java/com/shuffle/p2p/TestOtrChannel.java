package com.shuffle.p2p;

import com.shuffle.bitcoin.Address;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

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

    Channel<String, Bytestring> charlieNode;
    OtrChannel<String> otrCharlie;
    Connection<String> charlieConnection;
    Send<Bytestring> charlieSend;
    Peer<String, Bytestring> charlieToAlice;
    Peer<String, Bytestring> aliceToCharlie;
    Session<String, Bytestring> charlieToAliceSession;
    Session<String, Bytestring> aliceToCharlieSession;

    OtrChannel<String> otrAlice;
    OtrChannel<String> otrBob;

    Connection<String> aliceConnection;
    Connection<String> bobConnection;

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
        aliceConnection = otrAlice.open(aliceListener);
        bobConnection = otrBob.open(bobListener);


        charlieNode = network.node("charlie");
        otrCharlie = new OtrChannel<>(charlieNode, "charlie");
        charlieSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                System.out.println("Charlie received: " + new String(message.bytes));
                return true;
            }

            @Override
            public void close() {

            }
        };

        Listener<String, Bytestring> charlieListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {

                /**
                 * This Session object is an OtrSession object because of how we constructed
                 * OtrListener's newSession method.
                 */
                charlieToAliceSession = session;
                System.out.println("charlie listener caught: " + session);
                return bobSend;
            }
        };
        otrCharlie.open(charlieListener);

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


        System.out.println("\n \n \n \n");
        aliceToCharlie = otrAlice.getPeer("charlie");
        charlieToAlice = otrCharlie.getPeer("alice");
        aliceToCharlieSession = aliceToCharlie.openSession(aliceSend);
        aliceToCharlieSession.send(new Bytestring("CHARLIE DONT BUY ONECOIN".getBytes()));

    }

    // TODO
    @After
    public void shutdown() {

    }

}
