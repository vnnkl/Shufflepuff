package com.shuffle.p2p;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.mock.MockVerificationKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nsa on 12/23/16.
 */

/**
 * A test of MappedChannel in conjunction with TcpChannel
 */

public class TestMappedChannel {

	// Alice & Bob's keys that we use for identification
	VerificationKey aliceKey;
	VerificationKey bobKey;

	// Alice & Bob's InetSocketAddresses that are used for TCP communication
	InetSocketAddress aliceAddr;
	InetSocketAddress bobAddr;

	// Alice & Bob's Map of peers' VerificationKey to peers' InetSocketAddress
	Map<VerificationKey, InetSocketAddress> aliceMap;
	Map<VerificationKey, InetSocketAddress> bobMap;
	
	// Alice & Bob's respective TCPChannels
	Channel<InetSocketAddress, Bytestring> aliceTcp;
	Channel<InetSocketAddress, Bytestring> bobTcp;
	
	// Alice & Bob's respective MappedChannels
	MappedChannel<VerificationKey, InetSocketAddress> mappedAlice;
	MappedChannel<VerificationKey, InetSocketAddress> mappedBob;
	
	// Alice & Bob's respective MappedConnections
	Connection<VerificationKey> aliceConnection;
	Connection<VerificationKey> bobConnection;
	
	// Alice & Bob's Send objects - this is where their Listeners pass received messages
	Send<Bytestring> aliceSend;
	Send<Bytestring> bobSend;
	
	// Alice & Bob's respective Peer objects - these represent the Peer they are connected to
	Peer<VerificationKey, Bytestring> aliceToBob;
	Peer<VerificationKey, Bytestring> bobToAlice;
	
	// Alice & Bob's respective Session objects - these represent a Session object that can send messages to 
	// the connected Peer
	Session<VerificationKey, Bytestring> aliceToBobSession;
	Session<VerificationKey, Bytestring> bobToAliceSession;
	
	@Before
	public void setup() throws InterruptedException, IOException, UnknownHostException {
		
		// Alice uses port 5000 for TCPChannel
		aliceKey = new MockVerificationKey(5000);
		// Bob uses port 5001 for TCPChannel
		bobKey = new MockVerificationKey(5001);
		
		// Alice has an InetSocketAddress of 127.0.0.1:5000
		aliceAddr = new InetSocketAddress(InetAddress.getLocalHost(), 5000);
		// Bob has an InetSocketAddress of 127.0.0.1:5001
		bobAddr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);

		// Alice & Bob initialize their Map<VerificationKey, InetSocketAddress>
		// As described before, these Maps contain a peer's VerificationKey mapped to the peer's 
		// respective InetSocketAddress
		aliceMap = new HashMap<>();
		bobMap = new HashMap<>();
		
		// Alice & Bob put each other in their respective Map<VerificationKey, InetSocketAddress>
		aliceMap.put(bobKey, bobAddr);
		bobMap.put(aliceKey, aliceAddr);
		
		// Alice & Bob each initialize their own TCPChannel with their InetSocketAddress
		aliceTcp = new TcpChannel(aliceAddr);
		bobTcp = new TcpChannel(bobAddr);
		
		// Alice & Bob create their own MappedChannel
		mappedAlice = new MappedChannel<>(aliceTcp, aliceMap, aliceKey);
		mappedBob = new MappedChannel<>(bobTcp, bobMap, bobKey);

		/**
		 * Here Alice creates the Send object and the Listener object 
		 */
		aliceSend = new Send<Bytestring>() {
			@Override
			public boolean send(Bytestring bytestring) throws InterruptedException, IOException {
				System.out.println("Alice received: " + new String(bytestring.bytes));
				return true;
			}

			@Override
			public void close() {
				// TODO
				// This println is called, bobSend's close() is not
				System.out.println("aliceSend closed");
			}
		};
		
		Listener<VerificationKey, Bytestring> aliceListener = new Listener<VerificationKey, Bytestring>() {
			@Override
			public Send<Bytestring> newSession(Session<VerificationKey, Bytestring> session) throws InterruptedException {
				System.out.println("Alice's listener caught: " + session);
				return aliceSend;
			}
		};

		/**
		 * Here Bob creates the Send object and the Listener object
		 */
		bobSend = new Send<Bytestring>() {
			@Override
			public boolean send(Bytestring bytestring) throws InterruptedException, IOException {
				System.out.println("Bob received: " + new String(bytestring.bytes));
				return true;
			}

			@Override
			public void close() {
				System.out.println("bobSend closed");
			}
		};
		
		Listener<VerificationKey, Bytestring> bobListener = new Listener<VerificationKey, Bytestring>() {
			@Override
			public Send<Bytestring> newSession(Session<VerificationKey, Bytestring> session) throws InterruptedException {
				// We initialize bobToAliceSession here since Alice initiated the connection & sent the first message.
				bobToAliceSession = session;
				System.out.println("Bob's listener caught: " + session);
				return bobSend;
			}
		};
		
		// Alice & Bob open their MappedChannels with their respective listeners.
		// They are now both listening for incoming connections.
		aliceConnection = mappedAlice.open(aliceListener);
		bobConnection = mappedBob.open(bobListener);
		
	}
	
	@Test
	public void testSendMessages() throws InterruptedException, IOException {
		
		// Alice gets a peer by using Bob's VerificationKey.  
		// The returned Peer object represents Bob.
		aliceToBob = mappedAlice.getPeer(bobKey);
		
		// Bob gets a peer by using Alice's VerificationKey.  
		// The returned Peer object represents Alice.
		bobToAlice = mappedBob.getPeer(aliceKey);

		/**
		 * Alice opens up the session to Bob and a Session object is returned.
		 * This Session object allows Alice to send messages to Bob.
		 * 
		 * As a direct consequence of Alice calling openSession, Bob's newSession method is called
		 * on Bob's Listener and Bob's bobToAliceSession object is set.
		 * Bob can now send messages to Alice.
		 */
		aliceToBobSession = aliceToBob.openSession(aliceSend);
		
		// Alice sends "Bob, this is a mapped channel test!" to Bob
		aliceToBobSession.send(new Bytestring("Bob, this is a mapped channel test!".getBytes()));
		// Bob sends "Alice, testing testing testing" to Alice
		bobToAliceSession.send(new Bytestring("Alice, testing testing testing".getBytes()));
		
	}
	
	@After
	public void shutdown() {
		
		// Alice closes the session to Bob
		aliceToBobSession.close();
		// Bob closes the session to Alice
		bobToAliceSession.close();
		
		// Alice closes MappedChannel
		aliceConnection.close();
		// Bob closes MappedChannel
		bobConnection.close();

		/**
		 * We assert that the above Session & Connection objects are indeed closed.
		 */
		org.junit.Assert.assertTrue(aliceToBobSession.closed());
		org.junit.Assert.assertTrue(bobToAliceSession.closed());

		org.junit.Assert.assertTrue(aliceConnection.closed());
		org.junit.Assert.assertTrue(bobConnection.closed());
		
	}
	
}
