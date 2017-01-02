package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
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
	private final Identity me;

	public MappedChannel(Channel<Address, Bytestring> inner, Map<Identity, Address> hosts, Identity me) {
		this.inner = inner;
		this.hosts = hosts;
		this.me = me;
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
		public synchronized boolean send(Bytestring b) throws InterruptedException, IOException {
			System.out.println("\n\nMap B" + b);
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

	private class MappedAliceSend implements Send<Bytestring> {
		private final Send<Bytestring> z;
		private int messageCount = 0;
		private Send<Boolean> chan;

		private MappedAliceSend(Send<Bytestring> z, Send<Boolean> chan) {
			this.z = z;
			this.chan = chan;
		}

		@Override
		public synchronized boolean send(Bytestring message) throws InterruptedException, IOException {
			if (messageCount < 2) {
				messageCount++;
				if (new String(message.bytes).equals("received")) {
					chan.send(true);
					return true;
				}
				return true;
			}

			return this.z.send(message);
		}

		@Override
		public void close() {
			z.close();
			chan.close();
		}
	}

	private class MappedBobSend implements Send<Bytestring> {
		private final Listener<Identity, Bytestring> l;
		private final Session<Address, Bytestring> s;
		private int messageCount = 0;
		private Send<Bytestring> z;

		private MappedBobSend(Listener<Identity, Bytestring> l, Session<Address, Bytestring> s) {
			this.l = l;
			this.s = s;
		}

		@Override
		public synchronized boolean send(Bytestring message) throws InterruptedException, IOException {
			if (messageCount == 0) {
				messageCount++;
				Identity you = null;
				for (Map.Entry<Address, Identity> e : inverse.entrySet()) {
					if (e.getValue().toString().equals(new String(message.bytes))) {
						you = e.getValue();
						break;
					}
				}
				if (you == null) throw new NullPointerException();
				this.z = l.newSession(new MappedSession(s, you));
				return this.s.send(new Bytestring("received".getBytes()));
			}

			return this.z.send(message);
		}

		@Override
		public void close() {
			s.close();
		}
	}

	private class MappedPeer implements Peer<Identity,Bytestring> {
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
		public synchronized Session<Identity, Bytestring> openSession(Send<Bytestring> send) throws InterruptedException, IOException {
			if (send == null) return null;
			Chan<Boolean> chan = new BasicChan<>(1);

			MappedAliceSend alice = new MappedAliceSend(send, chan);
			Session<Address, Bytestring> session = inner.openSession(alice);
			if (session == null) return null;

			MappedSession s = new MappedSession(session, identity);

			session.send(new Bytestring(myIdentity().toString().getBytes()));

			Boolean result = chan.receive();
			if (!result) {
				chan.close();
				session.close();
				return null;
			}

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
		public synchronized Send<Bytestring> newSession(Session<Address, Bytestring> session) throws InterruptedException {
			return new MappedBobSend(inner, session);
		}
	}

	@Override
	public synchronized Connection<Identity> open(Listener<Identity, Bytestring> listener) throws InterruptedException, IOException {
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

	public Identity myIdentity() {
		return me;
	}

	@Override
	public String toString() {
		return "Mapped[" + hosts + ", " + inner +  "]";
	}
}
