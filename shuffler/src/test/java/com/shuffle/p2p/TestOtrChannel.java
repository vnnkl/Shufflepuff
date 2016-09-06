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
    Session<String, Bytestring> tempSession;
    OtrChannel.OtrPeer aliceToBob;
    OtrChannel.OtrPeer bobToAlice;
    OtrChannel.OtrPeer.OtrSession aliceToBobSession;
    OtrChannel.OtrPeer.OtrSession bobToAliceSession;

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
                System.out.println("Alice received from Bob");
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
                tempSession = session;
                return aliceSend;
            }
        };

        bobSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                try {
                    bobToAlice.sendClient.receive(message);
                } catch (OtrException e) {
                    System.out.println("bob receive failed");
                    return false;
                }
                System.out.println("Bob received from Alice");
                return true;
            }

            @Override
            public void close() {

            }
        };

        Listener<String, Bytestring> bobListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                System.out.println("bob listener caught: " + session);
                tempSession = session;
                return bobSend;
            }
        };

        otrAlice.open(aliceListener);
        otrBob.open(bobListener);

    }

    public class runnableSessions implements Runnable {

        public OtrChannel.OtrPeer peer;
        public Send<Bytestring> send;
        public OtrChannel.OtrPeer.OtrSession session;

        public runnableSessions(OtrChannel.OtrPeer peer, Send<Bytestring> send) {
            this.peer = peer;
            this.send = send;
        }

        public void run()  {
            try {
                this.session = peer.openSession(send);
            } catch (IOException e) {

            } catch (InterruptedException er) {

            }
        }

    }

    @Test
    public void encryptedChat() throws InterruptedException, IOException {

        /**
         * close aliceToBobSession, see if closing aliceToBob closes aliceToBobSession
         */

        // Alice to Bob
        aliceToBob = otrAlice.getPeer("bob");
        //aliceToBobSession = aliceToBob.openSession(aliceSend);

        // Bob to Alice
        bobToAlice = otrBob.getPeer("alice");
        //bobToAliceSession = bobToAlice.openReceivingSession(bobSend, tempSession);

        new Thread(new runnableSessions(aliceToBob, aliceSend)).start();
        Thread.sleep(3000);
        bobToAlice.openReceivingSession(bobSend, tempSession);

        /*
        // OTR v2/3 query initialization string
        String query = "?OTRv23?";
        // Alice sends the initialization string to Bob.
        aliceToBobSession.send(new Bytestring(query.getBytes()));

        // Key Exchange
        bobToAlice.sendClient.pollReceivedMessage();
        aliceToBob.sendClient.pollReceivedMessage();
        bobToAlice.sendClient.pollReceivedMessage();
        aliceToBob.sendClient.pollReceivedMessage();
        bobToAlice.sendClient.pollReceivedMessage();

        //Alice sends encrypted message to Bob
        aliceToBobSession.send(new Bytestring("Houston".getBytes()));
        OtrChannel.SendClient.ProcessedMessage messageForBob = bobToAlice.sendClient.pollReceivedMessage();
        System.out.println("Encrypted Message (message for bob) : " + messageForBob.getContent());

        //Bob sends encrypted message to Alice
        bobToAliceSession.send(new Bytestring("Weston".getBytes()));
        OtrChannel.SendClient.ProcessedMessage messageForAlice = aliceToBob.sendClient.pollReceivedMessage();
        System.out.println("Encrypted Message (message for alice) : " + messageForAlice.getContent());*/


    }

    // TODO
    @After
    public void shutdown() {

    }

}
