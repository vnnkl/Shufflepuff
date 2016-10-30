package com.shuffle.p2p;

import com.shuffle.chan.HistorySend;
import com.shuffle.chan.Send;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The HistoryChannel can be put on top of another channel to collect all messages
 * sent over the channel.
 *
 * Created by Daniel Krawisz on 10/18/16.
 */

public class HistoryChannel<Q, X extends Serializable> implements Channel<Q, X> {
    private final Channel<Q, X> channel;

    // HistoryPeer is a peer with additional functions to grab the
    // list of sessions that had been opened between the remote peer.
    public class HistoryPeer implements Peer<Q, X> {
        Queue<HistorySession> sessions = new ConcurrentLinkedQueue<>();

        private final Peer<Q, X> peer;

        private HistoryPeer(Peer<Q, X> peer) {
            this.peer = peer;
        }

        @Override
        public Q identity() {
            return peer.identity();
        }

        @Override
        public synchronized Session<Q, X> openSession(Send<X> send) throws InterruptedException, IOException {
            HistorySend<X> history = new HistorySend<>(send);
            Session<Q, X> session = peer.openSession(history);

            if (session == null) return null;

            HistorySession hs = new HistorySession(session, history);

            sessions.add(hs);

            return hs;
        }

        @Override
        public synchronized void close() throws InterruptedException {
            peer.close();
        }

        public synchronized List<Session<Q, X>> history() {

            List<Session<Q, X>> h = new LinkedList<>();
            h.addAll(sessions);
            return h;
        }
    }

    // The set of peers. Contains the history of all messages sent between the peers.
    public Map<Q, HistoryPeer> peers = new ConcurrentHashMap<>();

    public HistoryChannel(Channel<Q, X> channel) {
        this.channel = channel;
    }

    private HistoryPeer getHistoryPeer(Q you) {
        HistoryPeer peer = peers.get(you);

        if (peer == null) {
            Peer<Q, X> innerPeer = channel.getPeer(you);
            if (innerPeer == null) return null;
            peer = new HistoryPeer(innerPeer);
            peers.put(you, peer);
        }

        return peer;
    }

    @Override
    public synchronized Peer<Q, X> getPeer(Q you) {
        return getHistoryPeer(you);
    }

    // HistorySession is a session with additional functions enabling one to
    // grab the history of messages sent and received.
    public class HistorySession implements Session<Q, X> {
        private final List<X> sent = new LinkedList<>();
        public final HistorySend<X> received;

        private final Session<Q, X> session;

        private HistorySession(Session<Q, X> session, HistorySend<X> received) {
            this.session = session;
            this.received = received;
        }

        @Override
        public boolean closed() {
            return session.closed();
        }

        @Override
        public Peer<Q, X> peer() {
            return peers.get(session.peer());
        }

        @Override
        public synchronized boolean send(X x) throws InterruptedException, IOException {
            if (session.send(x)) {
                sent.add(x);
                return true;
            }

            return false;
        }

        @Override
        public void close() {
            session.close();
            peers.clear();
        }

        // Get the list of sent messages.
        public synchronized List<X> sent() {
            List<X> h = new LinkedList<X>();
            h.addAll(sent);
            return h;
        }

        // Get the list of received messages.
        public synchronized List<X> received() {
            return received.history();
        }
    }

    @Override
    public Connection<Q> open(Listener<Q, X> listener) throws InterruptedException, IOException {
        return channel.open(new Listener<Q, X>() {

            @Override
            public Send<X> newSession(Session<Q, X> session) throws InterruptedException {
                Send<X> s = listener.newSession(session);

                HistorySend<X> h = new HistorySend<>(s);

                HistorySession hs = new HistorySession(session, h);

                HistoryPeer p = getHistoryPeer(session.peer().identity());
                if (p == null) return null;

                p.sessions.add(hs);

                return h;
            }
        });
    }
}
