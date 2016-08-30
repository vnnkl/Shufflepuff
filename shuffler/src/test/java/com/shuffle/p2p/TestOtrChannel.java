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
                } catch (OtrException e) {
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
                } catch (OtrException e) {
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
        OtrChannel.OtrPeer.OtrSession bobSession = bob.openSession(aliceSend);  // bob.openBobSession(aliceSend, tempSession);

        // Bob to Alice
        OtrChannel.OtrPeer alice = otrBob.getPeer("alice");
        System.out.println(alice.peer.identity()); // alice.peer is alice
        OtrChannel.OtrPeer.OtrSession aliceSession = alice.openReceivingSession(bobSend, tempSession);

        // OTR v2/3 initialization string
        String query = "?OTRv23?";
        // Alice sends the initialization string to Bob.
        bobSession.send(new Bytestring(query.getBytes()));
        Assert.assertEquals(query, new String(bobMessage.bytes));

        // Check that Alice and Bob's SendClients have been initialized.
        Assert.assertNotNull(otrBob.sendClient.getConnection().session);
        Assert.assertNotNull(otrAlice.sendClient.getConnection().session);

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
