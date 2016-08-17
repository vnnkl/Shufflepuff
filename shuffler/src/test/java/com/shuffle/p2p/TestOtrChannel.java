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
    Channel<String, Bytestring> client;
    Channel<String, Bytestring> server;
    OtrChannel<String> otrClient;
    OtrChannel<String> otrServer;
    Bytestring clientMessage;
    Bytestring serverMessage;
    Send<Bytestring> clientSend;
    Send<Bytestring> serverSend;

    @Before
    public void setup() throws InterruptedException, IOException {
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
    public void encryptedChat() throws InterruptedException, IOException {
        // client
        OtrChannel.OtrPeer clientPeer = otrServer.getPeer("client");
        // client receiving from server
        OtrChannel.OtrPeer.OtrSession clientSession = clientPeer.openSession(clientSend);

        // server
        OtrChannel.OtrPeer serverPeer = otrClient.getPeer("server");
        // server receiving from client
        OtrChannel.OtrPeer.OtrSession serverSession = serverPeer.openSession(serverSend);

        String query = "?OTRv23?";
        clientSession.send(new Bytestring(query.getBytes()));

        // the server's session is set
        Assert.assertNotNull(otrServer.sendClient.getConnection().session);
        // the client's session is not set
        Assert.assertNotNull(otrClient.sendClient.getConnection().session);

        // Key Exchange starts here
        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();
        otrClient.sendClient.pollReceivedMessage();
        otrServer.sendClient.pollReceivedMessage();

        // This should be encrypted
        String message = "hey, encryption test";

        serverSession.send(new Bytestring(message.getBytes()));

        Assert.assertEquals(message, clientMessage);
    }

    @After
    public void shutdown() {

    }

}
