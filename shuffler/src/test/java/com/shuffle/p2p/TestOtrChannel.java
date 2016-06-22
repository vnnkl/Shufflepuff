package com.shuffle.p2p;

import com.shuffle.bitcoin.Address;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Eugene Siegel on 6/20/16.
 */

public class TestOtrChannel {

    MockNetwork<String, Bytestring> network;
    Channel<String, Bytestring> client;
    Channel<String, Bytestring> server;
    OtrChannel<String> otrClient;
    OtrChannel<String> otrServer;
    String clientMessage;
    String serverMessage;
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
            public boolean send(Bytestring bytestring) throws InterruptedException {
                TestOtrChannel.this.clientMessage = new String(bytestring.bytes);
                return true;
            }

            @Override
            public void close() throws InterruptedException {

            }
        };

        Listener<String, Bytestring> clientListener = new Listener<String, Bytestring>() {
            @Override
            public Send<Bytestring> newSession(Session<String, Bytestring> session) throws InterruptedException {
                return clientSend;
            }
        };

        serverSend = new Send<Bytestring>() {
            @Override
            public boolean send(Bytestring bytestring) throws InterruptedException {
                TestOtrChannel.this.serverMessage = new String(bytestring.bytes);
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
        Peer<String, Bytestring> clientPeer = otrClient.getPeer("server");
        Session<String, Bytestring> clientSession = clientPeer.openSession(clientSend);

        String message = "shufflepuff";
        Bytestring bytestring = new Bytestring(message.getBytes());
        Boolean clientSent = clientSession.send(bytestring);
        Assert.assertTrue(clientSent);
        Assert.assertEquals(serverMessage, message);
    }

    @After
    public void shutdown() {

    }

}
