package com.shuffle.p2p;

import com.shuffle.chan.Send;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 5/27/16.
 */
public class MappedChannel<Identity, Address> implements Channel<Identity, Bytestring> {
	private final Channel<Address, Bytestring> inner;
	private final Map<Identity, Address> hosts;
	private final Map<Address, Identity> inverse = new HashMap<>();

	public MappedChannel(Channel<Address, Bytestring> inner, Map<Identity, Address> hosts) {
		this.inner = inner;
		this.hosts = hosts;
	}

	private class MappedSession implements Session<Identity, Bytestring> {
		private final Session<Address, Bytestring> inner;
		private final Identity identity;

		private MappedSession(Session<Address, Bytestring> inner, Identity identity) {
			if (inner == null || identity == null) throw new NullPointerException();

			this.inner = inner;
			this.identity = identity;
		}

		@Override
		public boolean closed() {
			return inner.closed();
		}

		@Override
		public Peer<Identity, Bytestring> peer() {
			return new MappedPeer(inner.peer(), identity);
		}

		@Override
		public boolean send(Bytestring b) throws InterruptedException, IOException {
			return inner.send(b);
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
	
	private class MappedBobSend implements Send<Bytestring> {
		private final Listener<Identity, Bytestring> l;
		private final Session<Address, Bytestring> s;
		private int messageCount = 0;
		
		private MappedBobSend(Listener<Identity, Bytestring> l, Session<Address, Bytestring> s) {
			this.l = l;
			this.s = s;
		}
		
		
		
	}

	private class MappedPeer implements Peer<Identity, Bytestring> {
		private final Peer<Address, Bytestring> inner;
		private final Identity identity;

		private MappedPeer(Peer<Address, Bytestring> inner, Identity identity) {
			if (inner == null || identity == null) throw new NullPointerException();

			this.inner = inner;
			this.identity = identity;
		}

		@Override
		public Identity identity() {
			return identity;
		}

		@Override
		public Session<Identity, Bytestring> openSession(Send<Bytestring> send) throws InterruptedException, IOException {
			if (send == null) return null;
			Session<Address, Bytestring> session = inner.openSession(send);
			if (session == null) return null;
			
			MappedSession s = new MappedSession(session, identity);
			
			//session.send(s.peer().identity());
			
			return s;
		}

		@Override
		public void close() throws InterruptedException {
			inner.close();
		}
	}

	@Override
	public Peer<Identity, Bytestring> getPeer(Identity you) {
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

	private class MappedListener implements Listener<Address, Bytestring> {
		private final Listener<Identity, Bytestring> inner;

		private MappedListener(Listener<Identity, Bytestring> inner) {
			this.inner = inner;
		}

		@Override
		public Send<Bytestring> newSession(Session<Address, Bytestring> session) throws InterruptedException {
			return new MappedBobSend(inner, session);
			/*
			Identity you = inverse.get(session.peer().identity());
			if (you == null) throw new NullPointerException();
			return inner.newSession(new MappedSession(session, you));
			*/
		}
	}

	@Override
	public Connection<Identity> open(Listener<Identity, Bytestring> listener) throws InterruptedException, IOException {
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
