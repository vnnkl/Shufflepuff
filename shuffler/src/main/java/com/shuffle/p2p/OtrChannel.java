/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;

import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
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
            } catch (IOException | InterruptedException e) {

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
        private final SessionImpl sessionImpl;

        private OtrSession(Session<Address, Bytestring> s, SessionImpl sessionImpl) {
            this.s = s;
            this.sessionImpl = sessionImpl;
        }

        @Override
        public boolean send(Bytestring message) throws InterruptedException, IOException {
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

    private class OtrSendAlice implements Send<Bytestring> {
        private final Send<Bytestring> z;
        private boolean encrypted = false;
        private int messageCount = 0;
        private Send<Boolean> chan;
        private SessionImpl sessionImpl;

        private OtrSendAlice(Send<Bytestring> z, Send<Boolean> chan) {
            this.z = z;
            this.chan = chan;
        }

        @Override
        public boolean send(Bytestring message) throws InterruptedException, IOException {
            String receivedMessage;
            try {
                receivedMessage = sessionImpl.transformReceiving(new String(message.bytes));
                if (!encrypted) {
                    messageCount++;
                    if (messageCount == 2) {
                        chan.send(true);
                        encrypted = true;
                    }
                }
            } catch (OtrException e) {
                chan.send(false);
                this.close();
                return false;
            }

            if (receivedMessage == null) {
                return false;
            }

            /**
             * If uncommented, the below line will throw a NullPointerException with the
             * Jitsi Session Status ("ENCRYPTED" if all goes well), the ciphertext of the message,
             * and the deciphered plaintext message.
             *
             * if (true) throw new NullPointerException(sessionImpl.getSessionStatus() + "\n" + new String(message.bytes) + "\n" + receivedMessage);
             *
             */

            chan.close();

            try {
                return z.send(new Bytestring(receivedMessage.getBytes()));
            } catch (IOException e) {
                this.close();
                return false;
            }
        }

        @Override
        public void close() {
            z.close();
            chan.close();
        }

    }

    private class OtrSendBob implements Send<Bytestring> {
        private final Listener<Address, Bytestring> l;
        private final Session<Address, Bytestring> s;
        private boolean encrypted = false;
        private int messageCount = 0;
        private Send<Bytestring> z;
        private final SessionImpl sessionImpl;

        private OtrSendBob(Listener<Address, Bytestring> l, Session<Address, Bytestring> s, SessionImpl sessionImpl) {
            this.l = l;
            this.s = s;
            this.sessionImpl = sessionImpl;
        }

        @Override
        public boolean send(Bytestring message) {
            String receivedMessage;
            try {
                receivedMessage = sessionImpl.transformReceiving(new String(message.bytes));
                if (!encrypted) {
                    messageCount++;
                    if (messageCount == 3) {
                        this.z = l.newSession(new OtrSession(s, sessionImpl));
                        encrypted = true;
                    }
                }
            } catch (OtrException | InterruptedException e) {
                return false;
            }

            if (receivedMessage == null) {
                return false;
            }

            try {
                return z.send(new Bytestring(receivedMessage.getBytes()));
            } catch (IOException | InterruptedException e) {
                return false;
            }

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
            final SessionID sessionID = new SessionID("", "", "");
            OtrPolicy policy = new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
                    | OtrPolicy.ERROR_START_AKE); // this assumes the user wants either v2 or v3
            SessionImpl sessionImpl = new SessionImpl(sessionID, new SendOtrEngineHost(policy, session));

            return new OtrSendBob(this.l, session, sessionImpl);
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
            Chan<Boolean> chan = new BasicChan<>(1);

            OtrSendAlice alice = new OtrSendAlice(send, chan);
            Session<Address, Bytestring> session = peer.openSession(alice);

            final SessionID sessionID = new SessionID("", "", "");
            OtrPolicy policy = new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
                    | OtrPolicy.ERROR_START_AKE); // this assumes the user wants either v2 or v3
            SessionImpl sessionImpl = new SessionImpl(sessionID, new SendOtrEngineHost(policy, session));
            alice.sessionImpl = sessionImpl;

            // This string depends on the version / type of OTR encryption that the user wants.
            String query = "?OTRv23?";
            session.send(new Bytestring(query.getBytes()));

            /**
             * Waiting for the final OTR initialization message from Bob.
             */

            Boolean result = chan.receive();
            if (result) {

            } else {
                chan.close();
                session.close();
                //sessionImpl = null;
                return null;
            }

            return new OtrSession(session, sessionImpl);
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
    private HashMap<Integer, SessionImpl> sessionMap = new HashMap<>();

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