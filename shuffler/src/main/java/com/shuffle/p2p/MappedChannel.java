package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Send;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 5/27/16.
 */
public class MappedChannel<Identity, Address, X extends Serializable> implements Channel<Identity, X> {
    private final Channel<Address, X> inner;
    private final Map<Identity, Address> hosts;
    private final Map<Address, Identity> inverse = new HashMap<>();

    public MappedChannel(Channel<Address, X> inner, Map<Identity, Address> hosts) {
        this.inner = inner;
        this.hosts = hosts;
    }

    private class MappedSession implements Session<Identity, X> {
        private final Session<Address, X> inner;
        private final Identity identity;

        private MappedSession(Session<Address, X> inner, Identity identity) {
            if (inner == null || identity == null) throw new NullPointerException();

            this.inner = inner;
            this.identity = identity;
        }

        @Override
        public boolean closed() {
            return inner.closed();
        }

        @Override
        public Peer<Identity, X> peer() {
            return new MappedPeer(inner.peer(), identity);
        }

        @Override
        public boolean send(X x) throws InterruptedException, IOException {
            return inner.send(x);
        }

        @Override
        public void close() {
            inner.close();
        }

        @Override
        public String toString() {
            return "MappedSession[" + inner + "]";
        }
    }
    
    private class MappedAliceSend implements Send<X> {
		private final Send<X> z;
		private int messageCount = 0;
		private final Send<Boolean> chan;
		
		private MappedAliceSend(Send<X> z, Send<Boolean> chan) {
			this.z = z;
			this.chan = chan;
		}
		
		@Override
		public boolean send(X x) throws InterruptedException, IOException {
			if (messageCount == 0) {
				// handle identifying message
				chan.send(true);
				messageCount++;
			}
			return x != null && z.send(x);
		}
		
		@Override
		public void close() {
			z.close();
		}
	}
	
	private class MappedBobSend implements Send<X> {
		private final Listener<Identity, X> l;
		private final Session<Address, X> s;
		private int messageCount = 0;
		private Send<X> z;
		
		private MappedBobSend(Listener<Identity, X> l, Session<Address, X> s) {
			this.l = l;
			this.s = s;
		}
		
		// TODO
		@Override
		public boolean send(X x) throws InterruptedException, IOException {
			if (messageCount == 0) {
				// z = 
				messageCount++;
			}
			
			// return z.send()
			return false;
		}
		
		@Override
		public void close() {
			s.close();
		}
		
	}

    private class MappedPeer implements Peer<Identity,X> {
        private final Peer<Address, X> inner;
        private final Identity identity;

        private MappedPeer(Peer<Address, X> inner, Identity identity) {
            if (inner == null || identity == null) throw new NullPointerException();

            this.inner = inner;
            this.identity = identity;
        }

        @Override
        public Identity identity() {
            return identity;
        }

		// TODO
        @Override
        public Session<Identity, X> openSession(Send<X> send) throws InterruptedException, IOException {
			if (send == null) return null;
			Chan<Boolean> chan = new BasicChan<>(1);
			
			MappedAliceSend alice = new MappedAliceSend(send, chan);
			Session<Address, X> session = inner.openSession(alice);
			if (session == null) return null;
			
			Boolean result = chan.receive();
			if (result) {
				
			} else {
				chan.close();
				session.close();
				return null;
			}
			
			return new MappedSession(session, identity);
        }

        @Override
        public void close() throws InterruptedException {
            inner.close();
        }
    }

    @Override
    public Peer<Identity, X> getPeer(Identity you) {
        Address addr = hosts.get(you);
        if (addr == null) return null;

        return new MappedPeer(inner.getPeer(addr), you);
    }

    public class MappedConnection implements Connection<Identity> {
        private final Connection<Address> connection;

        public MappedConnection(Connection<Address> connection) {
            this.connection = connection;
        }

        @Override
        public void close() {
            if (connection != null) {
                connection.close();
            }
        }

        @Override
        public boolean closed() {
            return connection.closed();
        }
    }

    private class MappedListener implements Listener<Address, X> {
        private final Listener<Identity, X> inner;

        private MappedListener(Listener<Identity, X> inner) {
            this.inner = inner;
        }

		// TODO
        @Override
        public Send<X> newSession(Session<Address, X> session) throws InterruptedException {
			return new MappedBobSend(inner, session);
			
			/*
            Identity you = inverse.get(session.peer().identity());
            if (you == null) throw new NullPointerException();
            return inner.newSession(new MappedSession(session, you));
            */
        }
    }

    @Override
    public Connection<Identity> open(Listener<Identity, X> listener) throws InterruptedException, IOException {
        Connection<Address> c = inner.open(new MappedListener(listener));
        if (c == null) return null;
		
        for (Map.Entry<Identity, Address> e : hosts.entrySet()) {
            if (inverse.containsKey(e.getValue())) {
                hosts.remove(e.getKey());
            }

            inverse.put(e.getValue(), e.getKey());
        }
		
        return new MappedConnection(c);
    }

    @Override
    public String toString() {
        return "Mapped[" + hosts + ", " + inner +  "]";
    }
}
