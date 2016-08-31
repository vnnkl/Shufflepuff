package com.shuffle.p2p;

import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import net.java.otr4j.OtrException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    MockNetwork<String, Bytestring> network;
    Channel<String, Bytestring> alice;
    Channel<String, Bytestring> bob;
    OtrChannel<String> otrAlice;
    OtrChannel<String> otrBob;
    Bytestring aliceMessage;
    Bytestring bobMessage;
    Send<Bytestring> aliceSend;
    Send<Bytestring> bobSend;
    Session<String, Bytestring> tempSession;

    @Before
    public void setup() throws InterruptedException, IOException {
        network = new MockNetwork<>();
        alice = network.node("alice");
        bob = network.node("bob");
        otrAlice = new OtrChannel<>(alice, "alice");
        otrBob = new OtrChannel<>(bob, "bob");

        aliceSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                try {
                    otrAlice.sendClient.receive("bob", message);
                    System.out.println("alice receive succeeded");
                } catch (OtrException e) {
                    System.out.println("alice receive failed");
                    return false;
                }
                System.out.println("Alice received from Bob");
                TestOtrChannel.this.aliceMessage = message;

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
                    otrBob.sendClient.receive("alice", message);
                    System.out.println("bob receive succeeded");
                } catch (OtrException e) {
                    System.out.println("bob receive failed");
                    return false;
                }
                System.out.println("Bob received from Alice");
                TestOtrChannel.this.bobMessage = message;

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

    @Test
    public void encryptedChat() throws InterruptedException, IOException {

        // Alice to Bob
        OtrChannel.OtrPeer bob = otrAlice.getPeer("bob");
        System.out.println(bob.peer.identity()); // bob.peer is bob
        OtrChannel.OtrPeer.OtrSession bobSession = bob.openSession(aliceSend);

        // Bob to Alice
        OtrChannel.OtrPeer alice = otrBob.getPeer("alice");
        System.out.println(alice.peer.identity()); // alice.peer is alice
        OtrChannel.OtrPeer.OtrSession aliceSession = alice.openReceivingSession(bobSend, tempSession);

        // OTR v2/3 initialization string
        String query = "?OTRv23?";
        // Alice sends the initialization string to Bob.
        bobSession.send(new Bytestring(query.getBytes()));
        Assert.assertEquals(query, new String(bobMessage.bytes));
        // if you send any other string, you don't get a "ping" back.

        // Check that Alice and Bob's SendClients have been initialized.
        Assert.assertNotNull(otrBob.sendClient.getConnection().session);
        Assert.assertNotNull(otrAlice.sendClient.getConnection().session);

        System.out.println("messageQueue size: " + otrBob.sendClient.processor.messageQueue.size());
        System.out.println("messageQueue poll: " + otrBob.sendClient.processor.messageQueue.poll());
        // processedMsgs is not being updated with the initialization string !
        System.out.println("processedMsgs size: " + otrBob.sendClient.processedMsgs.size());
        System.out.println("processedMsgs poll: " + otrBob.sendClient.processedMsgs.poll());

        OtrChannel.SendClient.ProcessedMessage m = otrBob.sendClient.pollReceivedMessage();
        System.out.println("1: " + m.getContent());
        System.out.println("1: " + m.getSender());
        if (m == null) System.out.println("m is null");
        OtrChannel.SendClient.ProcessedMessage n = otrAlice.sendClient.pollReceivedMessage();
        System.out.println("2: " + n.getContent());
        System.out.println("2: " + n.getSender());
        if (n == null) System.out.println("n is null");
        OtrChannel.SendClient.ProcessedMessage m1 = otrBob.sendClient.pollReceivedMessage();
        System.out.println("3: " + m1.getContent());
        System.out.println("3: " + m1.getSender());
        if (m1 == null) System.out.println("m1 is null");
        OtrChannel.SendClient.ProcessedMessage n1 = otrAlice.sendClient.pollReceivedMessage();
        System.out.println("4: " + n1.getContent());
        System.out.println("4: " + n1.getSender());
        if (n1 == null) System.out.println("n1 is null");
        OtrChannel.SendClient.ProcessedMessage m2 = otrBob.sendClient.pollReceivedMessage();
        System.out.println("5: " + m2.getContent());
        System.out.println("5: " + m2.getSender());
        if (m2 == null) System.out.println("m2 is null");

        bobSession.send(new Bytestring("Houston".getBytes()));
        OtrChannel.SendClient.ProcessedMessage m3 = otrBob.sendClient.pollReceivedMessage();
        System.out.println("Encrypted Message: " + m3.getContent());

        //otrAlice.sendClient.pollReceivedMessage();
        //otrBob.sendClient.pollReceivedMessage();
        //otrAlice.sendClient.pollReceivedMessage();
        //otrBob.sendClient.pollReceivedMessage();

        // Key Exchange starts here
        /*
        otrBob.sendClient.pollReceivedMessage();
        otrAlice.sendClient.pollReceivedMessage();
        otrBob.sendClient.pollReceivedMessage();
        otrAlice.sendClient.pollReceivedMessage();
        otrBob.sendClient.pollReceivedMessage();
        */
    }

    @After
    public void shutdown() {

    }

}
