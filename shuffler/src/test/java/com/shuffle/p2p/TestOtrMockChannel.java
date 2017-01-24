package com.shuffle.p2p;

import com.shuffle.chan.Send;
import com.shuffle.mock.MockNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by nsa on 10/4/16.
 */

public class TestOtrMockChannel {

    MockNetwork<String, Bytestring> network;

    Channel<String, Bytestring> aNode;
    Channel<String, Bytestring> bNode;

    Channel<String, Bytestring> a;
    Channel<String, Bytestring> b;

    HistoryChannel<String, Bytestring> aHist;
    HistoryChannel<String, Bytestring> bHist;

    Send<Bytestring> aliceSend;
    Send<Bytestring> bobSend;

    Peer<String, Bytestring> aliceToBob;
    Peer<String, Bytestring> bobToAlice;

    Session<String, Bytestring> aliceToBobSession;
    Session<String, Bytestring> bobToAliceSession;

    @Before
    public void setup() throws IOException, InterruptedException {
        network = new MockNetwork<>();

        aNode = network.node("a");
        bNode = network.node("b");

        a = new OtrChannel<>(aNode);
        b = new OtrChannel<>(bNode);

        aHist = new HistoryChannel<>(a);
        bHist = new HistoryChannel<>(b);

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

        aHist.open(aliceListener);
        bHist.open(bobListener);
        //a.open(aliceListener);
        //b.open(bobListener);

    }

    @Test
    public void test() throws InterruptedException, IOException {

        aliceToBob = aHist.getPeer("b");
        bobToAlice = bHist.getPeer("a");

        aliceToBobSession = aliceToBob.openSession(aliceSend);

        aliceToBobSession.send(new Bytestring("test1".getBytes()));

        System.out.println(new String(aHist.peers.get("b").history().get(0).sent().get(0).bytes));
        //System.out.println(aHist.peers.get("b").history().get(0).received().get(0));

    }

    @After
    public void shutdown() {

    }

}
