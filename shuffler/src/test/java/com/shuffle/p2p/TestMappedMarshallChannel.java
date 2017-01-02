package com.shuffle.p2p;

/**
 * Created by nsa on 1/2/17.
 */

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.JavaMarshaller;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.player.Message;
import com.shuffle.player.Messages;
import com.shuffle.player.Payload;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A test of MappedChannel in conjunction with TcpChannel and MarshallChannel
 * 
 * MarshallChannel ( MappedChannel ( TcpChannel ) )
 */

public class TestMappedMarshallChannel {
	
	// Packet Marshaller for transforming Bytestring messages to Packets
	// Alice & Bob can use the same Marshaller object
	Marshaller<Packet<VerificationKey, Payload>> m;
	
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
	
	// Alice & Bob's respective MarshallChannels
	MarshallChannel<VerificationKey, Packet<VerificationKey, Payload>> aliceMarshall;
	MarshallChannel<VerificationKey, Packet<VerificationKey, Payload>> bobMarshall;
	
	// Alice & Bob's respective MarshallConnections
	Connection<VerificationKey> aliceConnection;
	Connection<VerificationKey> bobConnection;
	
	// Alice & Bob's Send objects - this is where their Listeners pass received messages
	Send<Packet<VerificationKey, Payload>> aliceSend;
	Send<Packet<VerificationKey, Payload>> bobSend;
	
	// Alice & Bob's respective Peer objects - these represent the Peer they are connected to
	Peer<VerificationKey, Packet<VerificationKey, Payload>> aliceToBob;
	Peer<VerificationKey, Packet<VerificationKey, Payload>> bobToAlice;
	
	// Alice & Bob's respective Session objects - these represent a Session object that can send messages to
	// the connected Peer
	Session<VerificationKey, Packet<VerificationKey, Payload>> aliceToBobSession;
	Session<VerificationKey, Packet<VerificationKey, Payload>> bobToAliceSession;
	
	@Before
	public void setup() throws InterruptedException, IOException {
		
		// Alice & Bob's collective Packet Marshaller
		m = new JavaMarshaller<>();
		
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
		
		// Alice & Bob create their own MarshallChannel
		aliceMarshall = new MarshallChannel<>(mappedAlice, m);
		bobMarshall = new MarshallChannel<>(mappedBob, m);

		/**
		 * Here Alice creates the Send object and the Listener object
		 */
		aliceSend = new Send<Packet<VerificationKey, Payload>>() {
			@Override
			public boolean send(Packet<VerificationKey, Payload> verificationKeyPayloadPacket) throws InterruptedException, IOException {
				System.out.println("Alice received: " + verificationKeyPayloadPacket.payload + " \n from: " + verificationKeyPayloadPacket.from);
				return true;
			}

			@Override
			public void close() {
				// TODO
				// println called?
				System.out.println("aliceSend closed");
			}
		};
		
		Listener<VerificationKey, Packet<VerificationKey, Payload>> aliceListener = new Listener<VerificationKey, Packet<VerificationKey, Payload>>() {
			@Override
			public Send<Packet<VerificationKey, Payload>> newSession(Session<VerificationKey, Packet<VerificationKey, Payload>> session) throws InterruptedException {
				System.out.println("Alice's listener caught: " + session);
				return aliceSend;
			}
		};

		/**
		 * Here Bob creates the Send object and the Listener object
		 */
		bobSend = new Send<Packet<VerificationKey, Payload>>() {
			@Override
			public boolean send(Packet<VerificationKey, Payload> verificationKeyPayloadPacket) throws InterruptedException, IOException {
				System.out.println("Bob received: " + verificationKeyPayloadPacket.payload + " \n from: " + verificationKeyPayloadPacket.from);
				return true;
			}

			@Override
			public void close() {
				System.out.println("bobSend closed");
			}
		};
		
		Listener<VerificationKey, Packet<VerificationKey, Payload>> bobListener = new Listener<VerificationKey, Packet<VerificationKey, Payload>>() {
			@Override
			public Send<Packet<VerificationKey, Payload>> newSession(Session<VerificationKey, Packet<VerificationKey, Payload>> session) throws InterruptedException {
				// We initialize bobToAliceSession here since Alice initiated the connection & sent the first message.
				bobToAliceSession = session;
				System.out.println("Bob's listener caught: " + session);
				return bobSend;
			}
		};
		
		// Alice & Bob open their MarshallChannels with their respective listeners.
		// They are now both listening for incoming connections.
		aliceConnection = aliceMarshall.open(aliceListener);
		bobConnection = bobMarshall.open(bobListener);
		
	}
	
	@Test
	public void testSendMessages() throws InterruptedException, IOException, NoSuchAlgorithmException {
		
		// Alice gets a peer by using Bob's VerificationKey.
		// The returned Peer object represents Bob.
		aliceToBob = aliceMarshall.getPeer(bobKey);
		
		// Bob gets a peer by using Alice's VerificationKey.
		// The returned Peer object represents Alice.
		bobToAlice = bobMarshall.getPeer(aliceKey);

		/**
		 * Alice opens up the session to Bob and a Session object is returned.
		 * This Session object allows Alice to send messages to Bob.
		 * 
		 * As a direct consequence of Alice calling openSession, Bob's newSession method is called
		 * on Bob's Listener and Bob's bobToAliceSession object is set.
		 * Bob can now send messages to Alice.
		 */
		aliceToBobSession = aliceToBob.openSession(aliceSend);

		// TODO
		Bytestring session = new Bytestring("test mapped marshall".getBytes());

		Connect<VerificationKey, Packet<VerificationKey, Payload>> aliceConnect = 
				new Connect<>(aliceMarshall, new MockCrypto(new InsecureRandom(5000)));

		SortedSet<VerificationKey> aliceConnectTo = new TreeSet<>();
		aliceConnectTo.add(bobKey);
		
		Collector<VerificationKey, Packet<VerificationKey, Payload>> aliceCollector = 
				aliceConnect.connect(aliceConnectTo, 3);
		
		Messages aliceMessages = new Messages(session, new MockSigningKey(5000), aliceCollector.connected, aliceCollector.inbox, m);
		Message aliceMessage = new Message(aliceMessages);
		Payload alicePayload = new Payload(null, aliceMessage);
		Packet<VerificationKey, Payload> alicePacket = new Packet<>(session, aliceKey, bobKey, 0, alicePayload);
		
		Connect<VerificationKey, Packet<VerificationKey, Payload>> bobConnect =
				new Connect<>(bobMarshall, new MockCrypto(new InsecureRandom(5001)));
		
		SortedSet<VerificationKey> bobConnectTo = new TreeSet<>();
		bobConnectTo.add(aliceKey);
		
		Collector<VerificationKey, Packet<VerificationKey, Payload>> bobCollector = 
				bobConnect.connect(bobConnectTo, 3);
		
		Messages bobMessages = new Messages(session, new MockSigningKey(5001), aliceCollector.connected, aliceCollector.inbox, m);
		Message bobMessage = new Message(bobMessages);
		Payload bobPayload = new Payload(null, bobMessage);
		Packet<VerificationKey, Payload> bobPacket = new Packet<>(session, bobKey, aliceKey, 0, bobPayload);
		
		// Alice sends a packet containing the message "" to Bob
		aliceToBobSession.send(alicePacket);
		// Bob send a packet containing the message "" to Alice
		bobToAliceSession.send(bobPacket);
		
	}
	
	@After
	public void shutdown() {
		
		// Alice closes the session to Bob
		aliceToBobSession.close();
		// Bob closes the session to Alice
		bobToAliceSession.close();
		
		// Alice closes MarshallChannel
		aliceConnection.close();
		// Bob closes MarshallChannel
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
