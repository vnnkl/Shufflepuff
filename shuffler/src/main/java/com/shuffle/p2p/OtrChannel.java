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

/**
 * Created by Eugene Siegel on 5/10/16.
 */

/**
 * The OtrChannel class allows OTR ("Off-The-Record") encrypted communications. OtrChannel is a
 * wrapper for a plaintext Channel like TcpChannel or WebsocketClientChannel / WebsocketServerChannel.
 * After an initialization phase, it ensures messages over the input Channel are encrypted
 * first via Jitsi's OTR library. OtrChannel then passes these encrypted messages to the inner
 * input Channel.
 */

public class OtrChannel<Address> implements Channel<Address, Bytestring> {

    /**
     * The SendOtrEngineHost class implements OtrEngineHost, a Jitsi interface.
     * This class processes messages sent / received by the SessionImpl Jitsi object.
     *
     * Most of the methods for SendOtrEngineHost are not filled out, but it does not matter
     * for this particular implementation.  If you need other jitsi/otr functionality,
     * feel free to change the methods.
     */

    private class SendOtrEngineHost implements OtrEngineHost {
        private final OtrPolicy policy;
        private final Session<Address, Bytestring> session;

        /**
         * @param policy -- Tells SessionImpl which version of OTR to use
         * @param session -- Tells SendOtrEngineHost where to send messages
         */

        private SendOtrEngineHost(OtrPolicy policy, Session<Address, Bytestring> session) {
            this.policy = policy;
            this.session = session;
        }

        /**
         * @param sessionID -- Identifies the SessionID of the SessionImpl we are using
         * @param msg -- Contains a message we wish to pass along to our Session object.
         */

        @Override
        public void injectMessage(SessionID sessionID, String msg) {
            try {
                session.send(new Bytestring(msg.getBytes()));
            } catch (IOException | InterruptedException e) {
                session.close();
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

        /**
         * @param paramSessionID
         * @return -- Given a SessionID, return the local key pair
         */

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

        /**
         * @param sessionID
         * @return -- Given a SessionID, return the local fingerprint
         */

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

        /**
         * @param sessionID -- If we receive a message from another SessionImpl instance,
         *                     throw a NullPointerException.
         */

        @Override
        public void messageFromAnotherInstanceReceived(SessionID sessionID) {
            throw new NullPointerException("Message from another instance received: " + sessionID.toString());
        }

        @Override
        public void multipleInstancesDetected(SessionID sessionID) {
            return;
        }

        /**
         * @param sessionID
         * @return -- Given a SessionID, return FragmenterInstructions for UNLIMITED fragment sizes.
         *            SessionImpl will try to split up (fragment) sent and received messages into
         *            set chunks specified by the user.  This function tells the calling SessionImpl
         *            instance that it can use an unlimited number of fragments, and that each
         *            fragment has an unlimited size.  For our current implementation, this means
         *            that SessionImpl will send a message in one fragment, rather than multiple.
         */

        @Override
        public FragmenterInstructions getFragmenterInstructions(SessionID sessionID) {
            return new FragmenterInstructions(FragmenterInstructions.UNLIMITED,
                    FragmenterInstructions.UNLIMITED);
        }

    }

    /**
     * The OtrSession class is a wrapper for a plaintext Session (TcpSession / WebsocketSession).
     * OtrSession encrypts messages and then sends them via the inner Session object.
     *
     * The OtrSession object is a fully encrypted Session object.  No initialization occurs here.
     */

    private class OtrSession implements Session<Address, Bytestring> {
        private final Session<Address, Bytestring> s;
        private final SessionImpl sessionImpl;

        /**
         * @param s -- Tells OtrSession which inner Session to use.
         * @param sessionImpl -- Tells OtrSession which SessionImpl to use in the send() message.
         */

        private OtrSession(Session<Address, Bytestring> s, SessionImpl sessionImpl) {
            this.s = s;
            this.sessionImpl = sessionImpl;
        }

        /**
         * This method will take a Bytestring message and encrypt it via the member variable
         * SessionImpl's transformSending() method.  Since we are using chunks of unlimited size,
         * outgoingMessage will only contain one chunk that contains the entire encrypted message.
         * After the message is encrypted, it is then sent to the inner Session object.
         */

        @Override
        public boolean send(Bytestring message) throws InterruptedException, IOException {
            String[] outgoingMessage;
            try {
                outgoingMessage = sessionImpl.transformSending(org.bouncycastle.util.encoders.Hex.toHexString(message.bytes), null);
            } catch (OtrException e) {
                return false;
            }

            for (String part : outgoingMessage) {
                System.out.println("s " + OtrChannel.this + " " + message + " " + part);
                if (part.equals("?OTRv23?null")) throw new NullPointerException();
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
     * The OtrSendAlice class is a wrapper for a normal Send object.  Its purpose is to
     * send decrypted messages to the inner Send object "z".
     */

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

        /**
         * This method will take a Bytestring message and pass it into SessionImpl's
         * transformReceiving() method.  If we are in the initialization phase, transformReceiving()
         * will create Diffie Hellman keys and exchange them with the peer.
         *
         * In the exchange process, SessionImpl's transformReceiving() will exchange messages with
         * the peer via the member variable SendOtrEngineHost's injectMessage() method. For just the
         * exchange process, receivedMessage will be null.  Thus, send() will return true, barring
         * any errors.  When the peer replies, OtrSendAlice is notified, and transformReceiving()
         * is called again.  This ping/pong happens three times.  If the key exchange process fails
         * anywhere, an OtrException is caught, the Send<Boolean> chan and Send<> z objects are
         * closed, and send() returns false.  If the process succeeds, the Send<Boolean> chan
         * object sends "true" (this notifies openSession() to return an OtrSession object) and
         * send() returns true.
         *
         * After the exchange process, transformReceiving() will decrypt all messages passed into it.
         * It will then pass messages to the inner Send object.
         */

        @Override
        public boolean send(Bytestring message) throws InterruptedException, IOException {
            System.out.println("k " + OtrChannel.this + " " + new String(message.bytes));
            if (new String(message.bytes).equals("?OTRv23?null")) throw new NullPointerException();
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
                System.out.println("******");
                return true;
            }

            chan.close();

            if (receivedMessage.equals("?OTRv23?null")) throw new NullPointerException();
            System.out.println("r " + OtrChannel.this + " " + new Bytestring(receivedMessage.getBytes()));

            try {
                return z.send(new Bytestring(org.bouncycastle.util.encoders.Hex.decode(receivedMessage)));
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

    /**
     * This class is identical to OtrSendAlice except for 2 things:
     *
     * 1.  The send() messageCount variable must reach 3 before the Send<> "z" object is
     *     set and newSession is called with a new OtrSession as the parameter.  This is because
     *     Bob will receive 3 messages from Alice, whereas Alice receives only 2 from Bob.
     *
     * 2.  There is a Listener<> "l" member variable that is used to call newSession() when
     *     the encrypted session is fully established in send().
     */

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
            System.out.println("k " + OtrChannel.this + " " + new String(message.bytes));
            if (new String(message.bytes).equals("?OTRv23?null")) throw new NullPointerException();
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
                System.out.println("*****");
                return true;
            }

            if (receivedMessage.equals("?OTRv23?null")) throw new NullPointerException();
            System.out.println("r " + OtrChannel.this + " " + new Bytestring(receivedMessage.getBytes()));

            try {
                return z.send(new Bytestring(org.bouncycastle.util.encoders.Hex.decode(receivedMessage)));
            } catch (IOException | InterruptedException e) {
                return false;
            }

        }

        @Override
        public void close() {
            s.close();
        }

    }

    /**
     * The OtrListener class is a wrapper for a normal Listener object.  When newSession()
     * is called, we first make a new SessionImpl object with the passed in Session object
     * as a parameter.  Note: The OtrPolicy variable allows OTRv2 and OTRv3.  This can be
     * changed if the user desires different versions of OTR.  Finally, we return an OtrSendBob
     * object with our Listener, Session, and SessionImpl objects as parameters.
     */

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

    /**
     * The OtrPeer class is a wrapper for a normal Peer object.  When openSession() is called,
     * the OTR initialization process is started.  Only Alice calls openSession().
     */

    private class OtrPeer implements Peer<Address, Bytestring> {

        Peer<Address, Bytestring> peer;

        public OtrPeer(Peer<Address, Bytestring> peer) {
            this.peer = peer;
        }

        /**
         * The openSession() method starts the OTR protocol.  It creates a BasicChan object
         * that is passed into OtrSendAlice and when the key exchange is complete, this Chan
         * "chan" is notified and an OtrSession object is returned.  We also create a SessionImpl
         * object for handling messages and pass that into both our OtrSendAlice object and the
         * returned OtrSession object.  To start the actual protocol, a specific string needs to
         * be crafted.  In our implementation, we used "?OTRv23?" which means we are initializing
         * OTR version 3, with version 2 as a backup if version 3 is not available.  Other protocol
         * strings are possible.  We then call chan.receive(), and when the key exchange in
         * OtrSendAlice returns, we check to see if the key exchange was successful or not.
         * If it is, we return a new OtrSession object.  Else, we shut everything down.
         *
         * Note: The OtrPolicy variable allows OTRv2 and OTRv3.  This can be changed if the user
         * desires different versions of OTR.
         */

        @Override
        public synchronized OtrSession openSession(Send<Bytestring> send) throws InterruptedException, IOException {
            if (send == null) throw new NullPointerException();
            Chan<Boolean> chan = new BasicChan<>(1);

            OtrSendAlice alice = new OtrSendAlice(send, chan);
            Session<Address, Bytestring> session = peer.openSession(alice);
            if (session == null) return null;

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
            if (!result) {
                chan.close();
                session.close();
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

    /**
     * The OtrConnection class is a wrapper of a normal Connection object.  There is no
     * difference whatsoever in functionality... yet.
     */

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

    public OtrChannel(Channel<Address, Bytestring> channel) {
        this.channel = channel;
    }

    /**
     * Here we simply pass on a new OtrListener object to the open inner channel.
     * An OtrConnection is returned.
     */

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
        Peer<Address, Bytestring> p = channel.getPeer(you);
        if (p == null) return null;
        return new OtrPeer(p);
    }

}