package com.shuffle.p2p;

/**
 * Created by nsa on 1/2/17.
 */

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.JavaMarshaller;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.InsecureRandom;
import com.shuffle.mock.MockCrypto;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.player.JavaShuffleMarshaller;
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
	
	// The object that creates the Signed Packet Marshaller
	Messages.ShuffleMarshaller sm;
	
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
	MarshallChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceMarshall;
	MarshallChannel<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobMarshall;
	
	// Alice & Bob's respective MarshallConnections
	Connection<VerificationKey> aliceConnection;
	Connection<VerificationKey> bobConnection;
	
	// Alice & Bob's Send objects - this is where their Listeners pass received messages
	Send<Signed<Packet<VerificationKey, Payload>>> aliceSend;
	Send<Signed<Packet<VerificationKey, Payload>>> bobSend;
	
	// Alice & Bob's respective Peer objects - these represent the Peer they are connected to
	Peer<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceToBob;
	Peer<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobToAlice;
	
	// Alice & Bob's respective Session objects - these represent a Session object that can send messages to
	// the connected Peer
	Session<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceToBobSession;
	Session<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobToAliceSession;
	
	@Before
	public void setup() throws InterruptedException, IOException {
		
		sm = new JavaShuffleMarshaller();
		
		// Alice & Bob's collective Packet Marshaller
		m = sm.packetMarshaller();
		
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
		aliceMarshall = new MarshallChannel<>(mappedAlice, sm.signedMarshaller());
		bobMarshall = new MarshallChannel<>(mappedBob, sm.signedMarshaller());

		/**
		 * Here Alice creates the Send object and the Listener object
		 */
		aliceSend = new Send<Signed<Packet<VerificationKey, Payload>>>() {
			@Override
			public boolean send(Signed<Packet<VerificationKey, Payload>> verificationKeyPayloadPacket) throws InterruptedException, IOException {
				System.out.println("Alice received: " + verificationKeyPayloadPacket.message.payload + " \n from: " + verificationKeyPayloadPacket.message.from);
				return true;
			}

			@Override
			public void close() {
				// TODO
				// println called?
				System.out.println("aliceSend closed");
			}
		};
		
		Listener<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceListener = new Listener<VerificationKey, Signed<Packet<VerificationKey, Payload>>>() {
			@Override
			public Send<Signed<Packet<VerificationKey, Payload>>> newSession(Session<VerificationKey, Signed<Packet<VerificationKey, Payload>>> session) throws InterruptedException {
				System.out.println("Alice's listener caught: " + session);
				return aliceSend;
			}
		};

		/**
		 * Here Bob creates the Send object and the Listener object
		 */
		bobSend = new Send<Signed<Packet<VerificationKey, Payload>>>() {
			@Override
			public boolean send(Signed<Packet<VerificationKey, Payload>> verificationKeyPayloadPacket) throws InterruptedException, IOException {
				System.out.println("Bob received: " + verificationKeyPayloadPacket.message.payload + " \n from: " + verificationKeyPayloadPacket.message.from);
				return true;
			}

			@Override
			public void close() {
				System.out.println("bobSend closed");
			}
		};
		
		Listener<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobListener = new Listener<VerificationKey, Signed<Packet<VerificationKey, Payload>>>() {
			@Override
			public Send<Signed<Packet<VerificationKey, Payload>>> newSession(Session<VerificationKey, Signed<Packet<VerificationKey, Payload>>> session) throws InterruptedException {
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

		Connect<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceConnect = 
				new Connect<>(aliceMarshall, new MockCrypto(new InsecureRandom(5000)));

		SortedSet<VerificationKey> aliceConnectTo = new TreeSet<>();
		aliceConnectTo.add(bobKey);
		
		Collector<VerificationKey, Signed<Packet<VerificationKey, Payload>>> aliceCollector = 
				aliceConnect.connect(aliceConnectTo, 3);

		SigningKey aliceSk = new MockSigningKey(5000);
		
		Messages aliceMessages = new Messages(session, aliceSk, aliceCollector.connected, aliceCollector.inbox, sm);
		Message aliceMessage = new Message(aliceMessages);
		aliceMessage.attach("Hello Bob!");
		Payload alicePayload = new Payload(null, aliceMessage);
		Signed<Packet<VerificationKey, Payload>> alicePacket = new Signed<>(
				new Packet<>(session, aliceKey, bobKey, 0, alicePayload), aliceSk, m);
		
		Connect<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobConnect =
				new Connect<>(bobMarshall, new MockCrypto(new InsecureRandom(5001)));
		
		SortedSet<VerificationKey> bobConnectTo = new TreeSet<>();
		bobConnectTo.add(aliceKey);
		
		Collector<VerificationKey, Signed<Packet<VerificationKey, Payload>>> bobCollector = 
				bobConnect.connect(bobConnectTo, 3);
		
		SigningKey bobSk = new MockSigningKey(5001);
		
		Messages bobMessages = new Messages(session, bobSk, aliceCollector.connected, aliceCollector.inbox, sm);
		Message bobMessage = new Message(bobMessages);
		bobMessage.attach("Alice response");
		Payload bobPayload = new Payload(null, bobMessage);
		Signed<Packet<VerificationKey, Payload>> bobPacket = new Signed<>(
				new Packet<>(session, bobKey, aliceKey, 0, bobPayload), bobSk, m);
		
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
