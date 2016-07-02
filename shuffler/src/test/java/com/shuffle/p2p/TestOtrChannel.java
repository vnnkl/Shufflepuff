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

import java.util.Stack;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    MockNetwork<String, Bytestring> network;
    Channel<String, Bytestring> client;
    Channel<String, Bytestring> server;
    OtrChannel<String> otrClient;
    OtrChannel<String> otrServer;
    Bytestring clientMessage;
    Bytestring serverMessage;
    Send<Bytestring> clientSend;
    Send<Bytestring> serverSend;

    @Before
    public void setup() {
        network = new MockNetwork<>();
        client = network.node("client");
        server = network.node("server");
        otrClient = new OtrChannel<>(client, "client");
        otrServer = new OtrChannel<>(server, "server");

        clientSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
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

        Listener<String, Bytestring> clientListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                return clientSend; //?
            }
        };

        serverSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring message) throws InterruptedException {
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

        Listener<String, Bytestring> serverListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                return serverSend;
            }
        };

        otrClient.open(clientListener);
        otrServer.open(serverListener);

    }

    @Test
    public void encryptedChat() throws InterruptedException {
        OtrChannel.OtrPeer clientPeer = otrClient.getPeer("server");
        OtrChannel.OtrPeer.OtrSession clientSession = clientPeer.openSession(clientSend);

        OtrChannel.OtrPeer serverPeer = otrServer.getPeer("client");
        OtrChannel.OtrPeer.OtrSession serverSession = serverPeer.openSession(serverSend);

        /*
        Simply sending a test message, without the initialization string -- this works.

        String message = "shufflepuff";
        Bytestring bytestring = new Bytestring(message.getBytes());
        Boolean clientSent = clientSession.send(bytestring);
        Assert.assertTrue(clientSent);
        Assert.assertEquals(serverMessage, message);
        */

        /*
        Sending with initialization string "query"

        String query = "<p>?OTRv23?\n" +
                "<span style=\"font-weight: bold;\">Bob@Wonderland/</span> has requested an <a href=\"http://otr.cypherpunks.ca/\">Off-the-Record private conversation</a>. However, you do not have a plugin to support that.\n" +
                "See <a href=\"http://otr.cypherpunks.ca/\">http://otr.cypherpunks.ca/</a> for more information.</p>";
        */

        String query = "?OTRv23?";
        //clientSession.session.send(query); Executes in a different thread..?
        clientSession.send(new Bytestring(query.getBytes()));

        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();

        String message = "hey, encryption test";

        serverSession.send(new Bytestring(message.getBytes()));

        Assert.assertEquals(message, clientMessage);
    }

    @After
    public void shutdown() {

    }

}
