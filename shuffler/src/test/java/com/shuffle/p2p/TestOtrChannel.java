package com.shuffle.p2p;

import com.shuffle.bitcoin.Address;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    MockNetwork<String, String> network;
    Channel<String, String> client;
    Channel<String, String> server;
    OtrChannel<String> otrClient;
    OtrChannel<String> otrServer;
    String clientMessage;
    String serverMessage;
    Send<String> clientSend;
    Send<String> serverSend;
    Session<String, String> serverSession;

    @Before
    public void setup() {
        network = new MockNetwork<>();
        client = network.node("client");
        server = network.node("server");
        otrClient = new OtrChannel<>(client, "client");
        otrServer = new OtrChannel<>(server, "server");

        clientSend = new Send<String>() {
            @Override
            public boolean send(String message) throws InterruptedException {
                TestOtrChannel.this.clientMessage = message;
                try {
                    otrClient.sendClient.receive("server", message);
                } catch (OtrException e) {
                    return false;
                }
                return true;
            }

            @Override
            public void close() throws InterruptedException {

            }
        };

        Listener<String, String> clientListener = new Listener<String, String>() {
            @Override
            public Send<String> newSession(Session<String, String> session) throws InterruptedException {
                return clientSend;
            }
        };

        serverSend = new Send<String>() {
            @Override
            public boolean send(String message) throws InterruptedException {
                TestOtrChannel.this.serverMessage = message;
                try {
                    otrServer.sendClient.receive("client", message);
                } catch (OtrException e) {
                    return false;
                }
                return true;
            }

            @Override
            public void close() throws InterruptedException {

            }
        };

        Listener<String, String> serverListener = new Listener<String, String>() {
            @Override
            public Send<String> newSession(Session<String, String> session) throws InterruptedException {
                return serverSend;
            }
        };

        otrClient.open(clientListener);
        otrServer.open(serverListener);

    }

    @Test
    public void encryptedChat() throws InterruptedException {
        Peer<String, String> clientPeer = otrClient.getPeer("server");
        Session<String, String> clientSession = clientPeer.openSession(clientSend);

        Peer<String, String> serverPeer = otrServer.getPeer("client");
        Session<String, String> serverSession = serverPeer.openSession(serverSend);

        /*
        //Simply sending a test message, without the initialization string -- this works.
        String message = "shufflepuff";
        Bytestring bytestring = new Bytestring(message.getBytes());
        Boolean clientSent = clientSession.send(bytestring);
        Assert.assertTrue(clientSent);
        Assert.assertEquals(serverMessage, message);
         */


        // Sending with initialization string "query"
        /*String query = "<p>?OTRv23?\n" +
                "<span style=\"font-weight: bold;\">Bob@Wonderland/</span> has requested an <a href=\"http://otr.cypherpunks.ca/\">Off-the-Record private conversation</a>. However, you do not have a plugin to support that.\n" +
                "See <a href=\"http://otr.cypherpunks.ca/\">http://otr.cypherpunks.ca/</a> for more information.</p>";
                */
        String query = "?OTRv23?";
        clientSession.send(query);


        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();

        String message = "hey, encryption test";

        serverSession.send(message);

        Assert.assertEquals(message, clientMessage);
    }

    @After
    public void shutdown() {

    }

}
