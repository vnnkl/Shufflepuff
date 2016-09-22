package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * Tests for the tcp connection
 *
 * Created by Daniel Krawisz on 4/14/16.
 */
public class TestTcpChannel {

    // The three channels we will use to test with.
    // This is a vector.
    Channel<Integer, LinkedList<Integer>>[] channels;

    // The three connections we would like to open.
    // This is also a vector.
    Connection<Integer>[] conn;

    // The peers that represent the programs' view of one anothers
    // This is an assymmetric 2-tensor.
    Peer<Integer, LinkedList<Integer>>[][] peer;

    // The session from A to B and the session from B to A.
    // Also an assymmetric 2-tensor.
    Session<Integer, LinkedList<Integer>>[][] session;

    // Channels for receiving messages.
    // another assymmetric 2-tensor.
    Receive<LinkedList<Integer>> rec[][];

    // The connection listeners. A vector.
    TestListener listen[];

    private static class TcpTestChannel implements Channel<Integer, LinkedList<Integer>> {
        private final TcpChannel tcp;
        private final Integer me;
        private final int[] ports;

        TcpTestChannel(Integer me, int[] ports)
                throws UnknownHostException {

            this.me = me;
            this.ports = ports;
            tcp = new TcpChannel(new InetSocketAddress(InetAddress.getLocalHost(), ports[me]));

        }

        private class TcpTestPeer implements Peer<Integer, LinkedList<Integer>> {
            private final Peer<InetSocketAddress, Bytestring> peer;
            private final int identity;

            private TcpTestPeer(Peer<InetSocketAddress, Bytestring> peer, int identity) {
                this.peer = peer;
                this.identity = identity;
            }

            @Override
            public Integer identity() {
                return identity;
            }

            @Override
            public Session<Integer, LinkedList<Integer>> openSession(Send<LinkedList<Integer>> send)
                    throws InterruptedException, IOException {

                TcpTestReceiver tr = new TcpTestReceiver(send, identity);

                Session<InetSocketAddress, Bytestring> p = peer.openSession(tr);

                if (p == null) {
                    return null;
                }

                Session<Integer, LinkedList<Integer>> session =
                        new TcpTestSession(p, identity);

                // Tell them who we are.
                LinkedList<Integer> identity = new LinkedList<>();
                identity.add(me);
                session.send(identity);

                // Wait for the response.
                tr.chan.receive();

                return session;
            }

            @Override
            public void close() throws InterruptedException {
                peer.close();
            }
        }

        private class TcpTestSession implements Session<Integer, LinkedList<Integer>> {
            private final Session<InetSocketAddress, Bytestring> session;
            private final int identity;

            private TcpTestSession(Session<InetSocketAddress, Bytestring> session, int identity) {
                this.session = session;
                this.identity = identity;
            }

            @Override
            public boolean closed() {
                return session.closed();
            }

            @Override
            public Peer<Integer, LinkedList<Integer>> peer() {
                return new TcpTestPeer(session.peer(), identity);
            }

            @Override
            public boolean send(LinkedList<Integer> msg) throws InterruptedException, IOException {
                ByteBuffer buf = ByteBuffer.allocate(4 * msg.size());
                for (Integer i : msg) {
                    buf.putInt(i);
                }
                return session.send(new Bytestring(buf.array()));
            }

            @Override
            public void close() {
                session.close();
            }
        }

        private class TcpTestListener implements Listener<InetSocketAddress, Bytestring> {
            private final Listener<Integer, LinkedList<Integer>> inner;

            private TcpTestListener(
                    Listener<Integer, LinkedList<Integer>> inner) {

                this.inner = inner;
            }

            @Override
            public Send<Bytestring> newSession(Session<InetSocketAddress, Bytestring> session)
                    throws InterruptedException {

                return new TcpTestReceiver(inner, session);
            }
        }

        private class TcpTestConnection implements Connection<Integer> {
            private final Connection<InetSocketAddress> conn;

            private TcpTestConnection(Connection<InetSocketAddress> conn) {
                this.conn = conn;
            }

            @Override
            public void close() {
                conn.close();
            }

            @Override
            public boolean closed() {
                return conn.closed();
            }
        }

        @Override
        public Peer<Integer, LinkedList<Integer>> getPeer(Integer you) {
            try {
                int port = ports[you];

                Peer<InetSocketAddress, Bytestring> p =
                        tcp.getPeer(new InetSocketAddress(InetAddress.getLocalHost(), port));

                if (p == null) return null;
                return new TcpTestPeer(p, you);

            } catch (UnknownHostException e) {
                return null;
            }
        }

        @Override
        public Connection<Integer> open(Listener<Integer, LinkedList<Integer>> listener) throws IOException {
            return new TcpTestConnection(tcp.open(new TcpTestListener(listener)));
        }

        private class TcpTestReceiver implements Send<Bytestring> {
            private Send<LinkedList<Integer>> inner = null;
            private Listener<Integer, LinkedList<Integer>> listener = null;
            private Session<InetSocketAddress, Bytestring> session = null;
            private Chan<Void> chan = new BasicChan<>();
            int last = 0;
            int i = 0;
            boolean closed = false;
            boolean firstMessage = true;
            int you = -1;

            // Constructor for Alice.
            private TcpTestReceiver(Send<LinkedList<Integer>> inner, int you) {
                this.inner = inner;
                this.you = you;
            }

            // Constructor for Bob.
            TcpTestReceiver(Listener<Integer, LinkedList<Integer>> listener,
                            Session<InetSocketAddress, Bytestring> session) {
                this.listener = listener;
                this.session = session;
            }

            @Override
            public boolean send(Bytestring bytestring) throws InterruptedException, IOException {
                if (closed) return false;

                byte[] bytes = bytestring.bytes;
                LinkedList<Integer> msg = new LinkedList<>();

                for (byte b : bytes) {
                    last = (last << 8) + b;
                    i ++;
                    if (i == 4) {
                        i = 0;
                        msg.add(last);
                        last = 0;
                    }
                }

                // The first message informs us of the other person's identity.
                if (firstMessage) {
                    firstMessage = false;

                    if (msg.size() != 1) {
                        closed = true;
                        chan.close();
                        return false;
                    }

                    // If we are Bob, then we have to send a response.
                    if (inner == null) {
                        you = msg.getFirst();
                        TcpTestSession tcpSession = new TcpTestSession(session, you);

                        // Now tell the other person who we are.
                        LinkedList<Integer> init = new LinkedList<>();
                        init.add(me);
                        tcpSession.send(init);

                        inner = listener.newSession(tcpSession);
                    } else if(you != msg.getFirst()) {
                        System.out.println("    " + me + "; " + you + " != " + msg.getFirst());
                        closed = true;
                        chan.close();
                        return false;
                    }

                    chan.close();

                } else if (!inner.send(msg)) {
                    closed = true;
                    return false;
                }

                return true;
            }

            @Override
            public void close() {
                inner.close();
                closed = true;
            }
        }
    }

    private static class TestListener implements Listener<Integer, LinkedList<Integer>> {
        private final Send<LinkedList<Integer>>[] senders;
        private final Chan<Session<Integer, LinkedList<Integer>>> chan;

        private TestListener(Send<LinkedList<Integer>>[] senders,
                             Chan<Session<Integer, LinkedList<Integer>>> chan) {

            this.senders = senders;
            this.chan = chan;
        }

        @Override
        public synchronized Send<LinkedList<Integer>> newSession(
                Session<Integer, LinkedList<Integer>> session) throws InterruptedException {

            try {
                chan.send(session);
            } catch (IOException e) {
                // Ignore.
            }

            return senders[session.peer().identity()];
        }
    }

    @Before
    public void setup() throws InterruptedException, IOException {
        int[] numbers = new int[]{0, 1, 2};
        int[] port = new int[]{9997, 9998, 9999};
        InetSocketAddress[] addresses = new InetSocketAddress[3];

        for (int i : numbers) {
            addresses[i] = new InetSocketAddress(InetAddress.getLocalHost(), port[i]);
        }

        channels = new TcpTestChannel[3];
        listen = new TestListener[3];
        conn = (Connection<Integer>[]) new Connection[3];
        peer = (Peer<Integer, LinkedList<Integer>>[][]) new Peer[3][3];
        session = (Session<Integer, LinkedList<Integer>>[][]) new Session[3][3];
        rec = (Receive<LinkedList<Integer>>[][]) new Receive[3][3];

        // Channels for receiving messages [from][to]
        Chan<LinkedList<Integer>> chan[][] = (Chan<LinkedList<Integer>>[][]) new Chan[3][3];

        // Channel for sending new sessions made my remote peers. A vector.
        Chan<Session<Integer, LinkedList<Integer>>>[] sch = new Chan[3];

        // Create objects.
        for (int i : numbers) {

            // create channels.
            for (int j : numbers) {

                // A channel cannot make a connection to itself, so we leave the
                // diagonal null.
                if (i != j) {
                    chan[i][j] = new BasicChan<>(2);
                    rec[i][j] = chan[i][j];
                } else {
                    chan[i][j] = null;
                    rec[i][j] = null;
                }
            }

            sch[i] = new BasicChan<>();

            // create listeners.
            listen[i] = new TestListener(chan[i], sch[i]);

            channels[i] = new TcpTestChannel(i, port);
        }

        // create peers. This is really cool because you can pretty much
        // use the shape of the tensor to check the correctness of the program.
        for (int j : numbers) {
            for (int k : numbers) {

                peer[j][k] = channels[j].getPeer(k);

                if (j != k) {
                    Assert.assertNotNull(peer[j][k]);
                } else {
                    Assert.assertNull(peer[j][k]);
                }
            }
        }

        // Open channels objects.
        for (int i : numbers) {

            // three threads down.
            conn[i] = channels[i].open(listen[i]);
        }

        // Let's sleep for three seconds!
        Thread.sleep(3000);

        // create sessions
        for (int j : numbers) {
            int k = (j + 1)%3;

            session[j][k] = peer[j][k].openSession(chan[j][k]);

            Assert.assertNotNull(session[j][k]);
            Assert.assertTrue(!session[j][k].closed());

            session[k][j] = sch[k].receive();
            Assert.assertNotNull(session[k][j]);
            Assert.assertTrue(!session[k][j].closed());
        }
    }

    @After
    public void shutdown() throws InterruptedException {
        int[] numbers = new int[]{0, 1, 2};

        // Close sessions.
        for (int j : numbers) {
            for (int k : numbers) {
                if (j != k) {
                    session[j][k].close();
                    Assert.assertTrue(session[j][k].closed());
                }
            }
        }

        // Close connections.
        for (int i : numbers) {
            conn[i].close();
        }
    }

    @Test
    // Send messages back and forth from 1 to 2.
    public void testSendMessages() throws IOException, InterruptedException {

        LinkedList<Integer> msgA = new LinkedList<>();
        msgA.add(2032);

        LinkedList<Integer> msgB = new LinkedList<>();
        msgB.add(98);
        msgB.add(888);
        msgB.add(6057);

        // Send the first message.
        session[1][2].send(msgA);

        // Now check the message.
        LinkedList<Integer> recA = rec[2][1].receive();
        Assert.assertEquals(recA.size(), msgA.size());

        // Send a reply.
        session[2][1].send(msgB);

        // Check for the reply.
        LinkedList<Integer> recB = rec[1][2].receive();
        Assert.assertEquals(recB.size(), msgB.size());

        // Try sending an empty message.
        session[1][2].send(new LinkedList<>());

        // Now check the message.
        LinkedList<Integer> recC = rec[2][1].receive();
        Assert.assertEquals(recC.size(), 0);
    }
}
