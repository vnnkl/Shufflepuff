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

	VerificationKey aliceKey;
	VerificationKey bobKey;

	InetSocketAddress aliceAddr;
	InetSocketAddress bobAddr;

	Map<VerificationKey, InetSocketAddress> aliceMap;
	Map<VerificationKey, InetSocketAddress> bobMap;
	
	Channel<InetSocketAddress, Bytestring> aliceTcp;
	Channel<InetSocketAddress, Bytestring> bobTcp;
	
	MappedChannel<VerificationKey, InetSocketAddress> mappedAlice;
	MappedChannel<VerificationKey, InetSocketAddress> mappedBob;
	
	Connection<VerificationKey> aliceConnection;
	Connection<VerificationKey> bobConnection;
	
	Send<Bytestring> aliceSend;
	Send<Bytestring> bobSend;
	
	Peer<VerificationKey, Bytestring> aliceToBob;
	Peer<VerificationKey, Bytestring> bobToAlice;
	
	Session<VerificationKey, Bytestring> aliceToBobSession;
	Session<VerificationKey, Bytestring> bobToAliceSession;
	
	@Before
	public void setup() throws InterruptedException, IOException, UnknownHostException {
		
		aliceKey = new MockVerificationKey(5000);
		bobKey = new MockVerificationKey(5001);
		
		aliceAddr = new InetSocketAddress(InetAddress.getLocalHost(), 5000);
		bobAddr = new InetSocketAddress(InetAddress.getLocalHost(), 5001);

		aliceMap = new HashMap<>();
		bobMap = new HashMap<>();
		
		aliceMap.put(bobKey, bobAddr);
		bobMap.put(aliceKey, aliceAddr);
		
		aliceTcp = new TcpChannel(aliceAddr);
		bobTcp = new TcpChannel(bobAddr);
		
		mappedAlice = new MappedChannel<>(aliceTcp, aliceMap, aliceKey);
		mappedBob = new MappedChannel<>(bobTcp, bobMap, bobKey);
		
		aliceSend = new Send<Bytestring>() {
			@Override
			public boolean send(Bytestring bytestring) throws InterruptedException, IOException {
				System.out.println("Alice received: " + new String(bytestring.bytes));
				return true;
			}

			@Override
			public void close() {
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
		
		bobSend = new Send<Bytestring>() {
			@Override
			public boolean send(Bytestring bytestring) throws InterruptedException, IOException {
				System.out.println("Bob received: " + new String(bytestring.bytes));
				return true;
			}

			@Override
			public void close() {
				// TODO
				// println never called
				System.out.println("bobSend closed");
			}
		};
		
		Listener<VerificationKey, Bytestring> bobListener = new Listener<VerificationKey, Bytestring>() {
			@Override
			public Send<Bytestring> newSession(Session<VerificationKey, Bytestring> session) throws InterruptedException {
				bobToAliceSession = session;
				System.out.println("Bob's listener caught: " + session);
				return bobSend;
			}
		};
		
		aliceConnection = mappedAlice.open(aliceListener);
		bobConnection = mappedBob.open(bobListener);
		
	}
	
	@Test
	public void testSendMessages() throws InterruptedException, IOException {
		
		aliceToBob = mappedAlice.getPeer(bobKey);
		bobToAlice = mappedBob.getPeer(aliceKey);
		
		aliceToBobSession = aliceToBob.openSession(aliceSend);
		
		aliceToBobSession.send(new Bytestring("Bob, this is a mapped channel test!".getBytes()));
		bobToAliceSession.send(new Bytestring("Alice, testing testing testing".getBytes()));
		
	}
	
	@After
	public void shutdown() {
		
		aliceToBobSession.close();
		bobToAliceSession.close();
		
		aliceConnection.close();
		bobConnection.close();

		org.junit.Assert.assertTrue(aliceToBobSession.closed());
		org.junit.Assert.assertTrue(bobToAliceSession.closed());

		org.junit.Assert.assertTrue(aliceConnection.closed());
		org.junit.Assert.assertTrue(bobConnection.closed());
		
	}
	
}
