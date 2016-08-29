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
                /* sendClient only initiated when openSession() called
                try {
                    otrAlice.sendClient.receive("bob", message);
                } catch (OtrException e) {
                    return false;
                }*/
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
                System.out.println("heyyyy" + session);
                tempSession = session;
                return aliceSend; //?
            }
        };

        bobSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
                /*
                try {
                    otrBob.sendClient.receive("alice", message);
                } catch (OtrException e) {
                    return false;
                }*/
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
                System.out.println("message received from Alice!!!");
                return bobSend;
            }
        };

        otrAlice.open(aliceListener);
        otrBob.open(bobListener);

    }

    @Test
    public void encryptedChat() throws InterruptedException, IOException {

        OtrChannel.OtrPeer alice = otrBob.getPeer("alice");
        System.out.println(alice.peer.identity()); // alice.peer is alice
        OtrChannel.OtrPeer.OtrSession aliceSession = alice.openSession(aliceSend);

        String query = "?OTRv23?";
        // NullPointerException here
        aliceSession.send(new Bytestring(query.getBytes()));
        Assert.assertEquals(query, new String(aliceMessage.bytes));
        // throws error --> bobSession.send(new Bytestring(query.getBytes()));

        OtrChannel.OtrPeer bob = otrAlice.getPeer("bob");
        System.out.println(bob.peer.identity());
        OtrChannel.OtrPeer.OtrSession bobSession = bob.openBobSession(bobSend, tempSession);
        //OtrChannel.OtrPeer.OtrSession bobSession = bob.new OtrSession(tempSession);
        bobSession.send(new Bytestring(query.getBytes()));


        Assert.assertNotNull(otrBob.sendClient.getConnection().session);

        // SendClient is null ---- session is null
        Assert.assertNotNull(otrAlice.sendClient.getConnection().session);

        // Key Exchange starts here
        otrBob.sendClient.pollReceivedMessage();
        otrAlice.sendClient.pollReceivedMessage();
        otrBob.sendClient.pollReceivedMessage();
        otrAlice.sendClient.pollReceivedMessage();
        otrBob.sendClient.pollReceivedMessage();

        // This should be encrypted
        String message = "hey, encryption test";

        //bobSession.send(new Bytestring(message.getBytes()));

        Assert.assertEquals(message, aliceMessage);
    }

    @After
    public void shutdown() {

    }

}
