/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Send;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Created by Eugene Siegel on 5/10/16.
 */

public class OtrChannel<Address> implements Channel<Address, Bytestring> {

    /**
     * Most of the methods for SendOtrEngineHost are not filled out, but it does not matter
     * here.  If you need other jitsi/otr functionality, feel free to change the methods.
     */

    private class SendOtrEngineHost implements OtrEngineHost {
        private final OtrPolicy policy;
        private final Session<Address, Bytestring> session;

        private SendOtrEngineHost(OtrPolicy policy, Session<Address, Bytestring> session) {
            this.policy = policy;
            this.session = session;
        }

        @Override
        public void injectMessage(SessionID sessionID, String msg) {
            try {
                session.send(new Bytestring(msg.getBytes()));
            } catch (IOException e) {

            } catch (InterruptedException er) {

            }
        }

        @Override
        public void smpError(SessionID sessionID, int tlvType, boolean cheated)
                throws OtrException {
            return;
        }

        @Override
        public void smpAborted(SessionID sessionID) throws OtrException {
            return;
        }

        @Override
        public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
            return;
        }

        @Override
        public void requireEncryptedMessage(SessionID sessionID, String msgText) throws OtrException {
            return;
        }

        @Override
        public void unreadableMessageReceived(SessionID sessionID) throws OtrException {
            return;
        }

        @Override
        public void unencryptedMessageReceived(SessionID sessionID, String msg) throws OtrException {
            return;
        }

        @Override
        public void showError(SessionID sessionID, String error) throws OtrException {
            return;
        }

        @Override
        public KeyPair getLocalKeyPair(SessionID paramSessionID) {
            KeyPairGenerator kg;
            try {
                kg = KeyPairGenerator.getInstance("DSA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
            return kg.genKeyPair();
        }

        @Override
        public OtrPolicy getSessionPolicy(SessionID ctx) {
            return policy;
        }

        @Override
        public byte[] getLocalFingerprintRaw(SessionID sessionID) {
            try {
                return new OtrCryptoEngineImpl()
                        .getFingerprintRaw(getLocalKeyPair(sessionID)
                                .getPublic());
            } catch (OtrCryptoException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void askForSecret(SessionID sessionID, InstanceTag receiverTag, String question) {
            return;
        }

        @Override
        public void verify(SessionID sessionID, String fingerprint, boolean approved) {
            return;
        }

        @Override
        public void unverify(SessionID sessionID, String fingerprint) {
            return;
        }

        @Override
        public String getReplyForUnreadableMessage(SessionID sessionID) {
            return null;
        }

        @Override
        public String getFallbackMessage(SessionID sessionID) {
            return null;
        }

        @Override
        public void messageFromAnotherInstanceReceived(SessionID sessionID) {
            throw new NullPointerException("Message from another instance received: " + sessionID.toString());
        }

        @Override
        public void multipleInstancesDetected(SessionID sessionID) {
            return;
        }

        @Override
        public FragmenterInstructions getFragmenterInstructions(SessionID sessionID) {
            return new FragmenterInstructions(FragmenterInstructions.UNLIMITED,
                    FragmenterInstructions.UNLIMITED);
        }

    }

    private class OtrSession implements Session<Address, Bytestring> {
        private final Session<Address, Bytestring> s;

        private OtrSession(Session<Address, Bytestring> s) {
            this.s = s;
        }

        @Override
        public boolean send(Bytestring message) throws InterruptedException, IOException {
            if (sessionImpl == null) {
                final SessionID sessionID = new SessionID("", "", "");
                OtrPolicy policy = new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
                        | OtrPolicy.ERROR_START_AKE); // this assumes the user wants either v2 or v3
                sessionImpl = new SessionImpl(sessionID, new SendOtrEngineHost(policy, s));
            }

            String[] outgoingMessage;
            try {
                outgoingMessage = sessionImpl.transformSending(new String(message.bytes), null);
            } catch (OtrException e) {
                return false;
            }

            for (String part : outgoingMessage) {
                s.send(new Bytestring(part.getBytes()));
            }
            return true;
        }

        @Override
        public boolean closed() {
            return s.closed();
        }

        @Override
        public void close() {
            s.close();
        }

        @Override
        public Peer<Address, Bytestring> peer() {
            return new OtrPeer(s.peer());
        }

    }

    /**
     * Filter init string / key exchange / termination messages
     * (accept passed-in session object from newSession)
     * Make this stateful.
     */

    private class OtrSendA implements Send<Bytestring> {
        private final Send<Bytestring> z;

        private OtrSendA(Send<Bytestring> z) {
            this.z = z;
        }

        @Override
        public boolean send(Bytestring message) {
            String receivedMessage;
            try {
                receivedMessage = sessionImpl.transformReceiving(new String(message.bytes));
            } catch (OtrException e) {
                return false;
            }

            if (receivedMessage == null) {
                return false;
            }

            /**
             * If uncommented, the below line will throw a NullPointerException with the
             * Jitsi Session Status ("ENCRYPTED" if all goes well), the ciphertext of the message,
             * and the deciphered plaintext message.
             */
            //if (true) throw new NullPointerException(sessionImpl.getSessionStatus() + "\n" + new String(message.bytes) + "\n" + receivedMessage);

            try {
                chan.send(new Bytestring(receivedMessage.getBytes()));
                return z.send(new Bytestring(receivedMessage.getBytes()));
            } catch (InterruptedException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public void close() {
            z.close();
        }

    }

    private class OtrSendB implements Send<Bytestring> {
        private final Listener<Address, Bytestring> l;
        private final Session<Address, Bytestring> s;

        private OtrSendB(Listener<Address, Bytestring> l, Session<Address, Bytestring> s) {
            this.l = l;
            this.s = s;
        }

        @Override
        public boolean send(Bytestring message) {
            String receivedMessage;
            try {
                receivedMessage = sessionImpl.transformReceiving(new String(message.bytes));
            } catch (OtrException e) {
                return false;
            }

            if (receivedMessage == null) {
                return false;
            }

            /**
             * What do I do when I eventually call:
             * Send<Bytestring> z = l.newSession(new OtrSession(s));
             *
             * What is the Send<> z object used for?
             */

            /**
             * We don't have a Send<> object here to pass along messages..?
             * l.newSession(session)
             */


            return false;
        }

        @Override
        public void close() {
            s.close();
        }

    }

    private class OtrListener implements Listener<Address, Bytestring> {
        private final Listener<Address, Bytestring> l;

        private OtrListener(Listener<Address, Bytestring> l) {
            this.l = l;
        }

        @Override
        public Send<Bytestring> newSession(Session<Address, Bytestring> session) throws InterruptedException {
            if (sessionImpl == null) {
                final SessionID sessionID = new SessionID("", "", "");
                OtrPolicy policy = new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
                        | OtrPolicy.ERROR_START_AKE); // this assumes the user wants either v2 or v3
                sessionImpl = new SessionImpl(sessionID, new SendOtrEngineHost(policy, session));
            }

            return new OtrSendB(this.l, session);
        }

    }

    private class OtrPeer implements Peer<Address, Bytestring> {

        Peer<Address, Bytestring> peer;

        public OtrPeer(Peer<Address, Bytestring> peer) {
            this.peer = peer;
        }

        @Override
        public synchronized OtrSession openSession(Send<Bytestring> send) throws InterruptedException, IOException {
            if (send == null) throw new NullPointerException();
            Session<Address, Bytestring> session = peer.openSession(new OtrSendA(send));
            if (session == null) throw new NullPointerException();

            // This string depends on the version / type of OTR encryption that the user wants.
            String query = "?OTRv23?";
            session.send(new Bytestring(query.getBytes()));

            /**
             * Initialization phase is happening before it returns
             */

            chan.receive();

            return new OtrSession(session);
        }

        @Override
        public Address identity() {
            return peer.identity();
        }

        @Override
        public void close() throws InterruptedException {
            peer.close();
        }

    }

    private class OtrConnection implements Connection<Address> {

        private Connection<Address> connection;

        private OtrConnection(Connection<Address> connection) {
            this.connection = connection;
        }

        @Override
        public void close() {
            running = false;
            this.connection.close();
        }

        @Override
        public boolean closed() {
            return connection.closed();
        }

    }

    private Channel<Address, Bytestring> channel;
    private boolean running = false;
    private final Address me;
    private SessionImpl sessionImpl;
    private HashMap<Integer, SessionImpl> sessionMap = new HashMap<>();
    private BasicChan<Bytestring> chan = new BasicChan<>(1);

    public OtrChannel(Channel<Address, Bytestring> channel, Address me) {
        if (me == null) {
            throw new NullPointerException();
        }

        this.channel = channel;
        this.me = me;
    }

    @Override
    public OtrConnection open(Listener<Address, Bytestring> listener) throws InterruptedException, IOException {

        if (listener == null) {
            throw new NullPointerException();
        }

        if (running) {
            return null;
        }

        running = true;
        return new OtrConnection(this.channel.open(new OtrListener(listener)));
    }

    @Override
    public Peer<Address, Bytestring> getPeer(Address you) {
        if (you.equals(me)) return null;
        Peer<Address, Bytestring> p = channel.getPeer(you);
        if (p == null) return null;
        return new OtrPeer(p);
    }

}