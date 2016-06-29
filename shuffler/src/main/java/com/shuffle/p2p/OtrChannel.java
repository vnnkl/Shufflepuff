/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.p2p;


import com.shuffle.chan.Send;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionID;

/**
 * Created by Eugene Siegel on 5/10/16.
 */

public class OtrChannel<Address> implements Channel<Address, String> {


    /**
     * Need a global SendClient so that we can do the Key Exchange with pollReceivedMessage()
     */
    SendClient sendClient;

    // TODO
    public class SendClient {

        private final String account;
        private net.java.otr4j.session.Session session;
        private OtrPolicy policy;
        private SendConnection connection;
        private MessageProcessor processor;
        private Queue<ProcessedMessage> processedMsgs = new LinkedList<ProcessedMessage>();
        private Send<String> send;

        public SendClient(String account, Send<String> send) {
            this.account = account;
            this.send = send;
        }

        public net.java.otr4j.session.Session getSession() {
            return this.session;
        }

        public String getAccount() {
            return this.account;
        }

        public void setPolicy(OtrPolicy policy) {
            this.policy = policy;
        }

        // sends a message
        // does this create a new session with every message?
        public void send(String recipient, String s) throws OtrException, InterruptedException {
            if (session == null) {
                final SessionID sessionID = new SessionID(account, recipient, "SendProtocol");
                session = new SessionImpl(sessionID, new SendOtrEngineHost());
            }

            String[] outgoingMessage = session.transformSending(s, null);
            for (String part : outgoingMessage) {
                connection.send(recipient, part);
            }
        }

        public void exit() throws OtrException {
            this.processor.stop();
            if (session != null) {
                session.endSession();
            }
        }

        public synchronized void receive(String sender, String s) throws OtrException {
            this.processor.enqueue(sender, s);
        }

        // TODO
        public void connect() {
            this.processor = new MessageProcessor();
            new Thread(this.processor).start();
            this.connection = new SendConnection(this, this.getAccount() + ".0", this.send);
        }

        // how would this even WORK with our current design?
        public void secureSession(String recipient) throws OtrException {
            if (session == null) {
                final SessionID sessionID = new SessionID(account, recipient, "SendProtocol");
                session = new SessionImpl(sessionID, new SendOtrEngineHost());
            }

            session.startSession();
        }

        public SendConnection getConnection() {
            return connection;
        }

        public ProcessedMessage pollReceivedMessage() {
            synchronized (processedMsgs) {
                ProcessedMessage m;
                while ((m = processedMsgs.poll()) == null) {
                    try {
                        processedMsgs.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        //return null
                    }
                }

                return m;
            }
        }


        private class Message {
            public Message(String sender, String content){
                this.sender = sender;
                this.content = content;
            }

            private final String sender;
            private final String content;

            public String getSender() {
                return sender;
            }

            public String getContent() {
                return content;
            }
        }

        private class ProcessedMessage extends Message {
            final Message originalMessage;

            public ProcessedMessage(Message originalMessage, String content) {
                super(originalMessage.getSender(), content);
                this.originalMessage = originalMessage;
            }
        }


        private class MessageProcessor implements Runnable {

            private final Queue<Message> messageQueue = new LinkedList<Message>();
            private boolean stopped;

            private void process(Message m) throws OtrException {
                if (session == null) {
                    final SessionID sessionID = new SessionID(account, m.getSender(), "DummyProtocol");
                    session = new SessionImpl(sessionID, new SendOtrEngineHost());
                }

                String receivedMessage = session.transformReceiving(m.getContent());
                synchronized (processedMsgs) {
                    processedMsgs.add(new ProcessedMessage(m, receivedMessage));
                    processedMsgs.notify();
                }
            }

            public void run() {
                synchronized (messageQueue) {
                    while (true) {

                        Message m = messageQueue.poll();

                        if (m == null) {
                            try {
                                messageQueue.wait();
                            } catch (InterruptedException e) {

                            }
                        } else {
                            try {
                                process(m);
                            } catch (OtrException e) {
                                e.printStackTrace();
                            }
                        }

                        if (stopped)
                            break;
                    }
                }
            }

            public void enqueue(String sender, String s) {
                synchronized (messageQueue) {
                    messageQueue.add(new Message(sender, s));
                    messageQueue.notify();
                }
            }

            public void stop() {
                stopped = true;

                synchronized (messageQueue) {
                    messageQueue.notify();
                }
            }

        }


        private class SendOtrEngineHost implements OtrEngineHost {

            public void injectMessage(SessionID sessionID, String msg) throws OtrException {
                try {
                    connection.send(sessionID.getUserID(), msg);
                } catch (InterruptedException e) {
                    return;
                }
            }

            public void smpError(SessionID sessionID, int tlvType, boolean cheated)
                    throws OtrException {
                return;
            }

            public void smpAborted(SessionID sessionID) throws OtrException {
                return;
            }

            public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
                return;
            }

            public void finishedSessionMessage(SessionID sessionID) throws OtrException {
                return;
            }

            public void requireEncryptedMessage(SessionID sessionID, String msgText) throws OtrException {
                return;
            }

            public void unreadableMessageReceived(SessionID sessionID) throws OtrException {
                return;
            }

            public void unencryptedMessageReceived(SessionID sessionID, String msg) throws OtrException {
                return;
            }

            public void showError(SessionID sessionID, String error) throws OtrException {
                return;
            }

            public String getReplyForUnreadableMessage() {
                return "You sent me an unreadable encrypted message.";
            }

            public void sessionStatusChanged(SessionID sessionID) {
                return;
            }

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

            public OtrPolicy getSessionPolicy(SessionID ctx) {
                return policy;
            }

            public void askForSecret(SessionID sessionID, String question) {
                return;
            }

            public void verify(SessionID sessionID, boolean approved) {
                return;
            }

            public void unverify(SessionID sessionID) {
                return;
            }

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

            public void askForSecret(SessionID sessionID, InstanceTag receiverTag, String question) {
                return;
            }

            public void verify(SessionID sessionID, String fingerprint, boolean approved) {
                return;
            }

            public void unverify(SessionID sessionID, String fingerprint) {
                return;
            }

            public String getReplyForUnreadableMessage(SessionID sessionID) {
                return null;
            }

            public String getFallbackMessage(SessionID sessionID) {
                return null;
            }

            public void messageFromAnotherInstanceReceived(SessionID sessionID) {
                return;
            }

            public void multipleInstancesDetected(SessionID sessionID) {
                return;
            }

            public String getFallbackMessage() {
                return "Off-the-Record private conversation has been requested. However, you do not have a plugin to support that.";
            }

            public FragmenterInstructions getFragmenterInstructions(SessionID sessionID) {
                return new FragmenterInstructions(FragmenterInstructions.UNLIMITED,
                        FragmenterInstructions.UNLIMITED);
            }

        }

        // TODO
        private class SendConnection implements Send<String> {

            private final SendClient client;
            private final String connectionName;
            private String sentMessage;
            private Send<String> send;
            private Session<Address, String> session;

            public SendConnection(SendClient client, String connectionName, Send<String> send) {
                this.client = client;
                this.connectionName = connectionName;
                this.send = send;
            }

            public String getSentMessage() {
                return sentMessage;
            }

            public SendClient getClient() {
                return client;
            }

            @Override
            public String toString() {
                return "PriorityConnection{" +
                        "connectionName='" + connectionName + '\'' +
                        '}';
            }

            // TODO
            // sends a message
            public void send(String recipient, String msg) throws OtrException, InterruptedException {
                this.sentMessage = msg;
                session.send(msg);
            }

            // TODO
            // receives a message
            // synchronized?
            // THIS IS NEVER USED ??
            public boolean send(String msg) throws InterruptedException {
                String sender = "doesnt matter";

                try {
                    //decrypts the message

                    this.client.receive(sender, msg);

                    // this won't work because we could receive a new message by the time we poll...
                    // @danielk does a synchronized method solve this issue?
                    // also made SendClient's receive() method synchronized
                    String pollMessage = this.client.pollReceivedMessage().originalMessage.content;
                    return this.send.send(pollMessage);
                } catch (OtrException e) {
                    return false;
                }
            }

            // TODO
            public void close() throws InterruptedException {

            }

        }

    }

    public class OtrPeer extends FundamentalPeer<Address, String> {

        Peer<Address, String> peer;

        public OtrPeer(Address identity, Peer<Address, String> peer) {
            super(identity);
            this.peer = peer;
            sendClient = new SendClient("nothing", null);
            sendClient.setPolicy(new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
                    | OtrPolicy.ERROR_START_AKE));
        }

        @Override
        public synchronized Session<Address, String> openSession(Send<String> send) {

            Session<Address, String> session;

            try {
                sendClient.send = send;
                sendClient.connect();
                session = peer.openSession(sendClient.connection);
                sendClient.connection.session = session;
            } catch (InterruptedException e) {
                return null;
            }

            OtrSession otrSession = new OtrSession(session);
            sendClient.connection.session = otrSession;
            return otrSession;
        }

        public class OtrSession implements Session<Address, String> {

            Session<Address, String> session;

            public OtrSession(Session<Address, String> session) {
                this.session = session;
            }

            @Override
            public synchronized boolean send(String message) {
                try {
                    sendClient.send("recipient",message);
                    return true;
                } catch (OtrException e) {
                    return false;
                } catch (InterruptedException er) {
                    return false;
                }
            }

            @Override
            public synchronized void close() {
                try {
                    session.close();
                } catch (InterruptedException e) {
                    return;
                }
            }

            @Override
            public synchronized boolean closed() {
                try {
                    return session.closed();
                } catch (InterruptedException e) {
                    return false;
                }
            }

            // TODO
            @Override
            public Peer<Address, String> peer() {
                return OtrPeer.this;
            }

        }

    }

    private class OtrConnection implements Connection<Address> {

        Connection<Address> connection;

        public OtrConnection(Connection<Address> connection) {
            this.connection = connection;
        }

        @Override
        public void close() {
            synchronized (lock) {
                running = false;
                try {
                    this.connection.close();
                } catch (InterruptedException e) {
                    running = true;
                }
            }
        }

        @Override
        public boolean closed() {
            try {
                return connection.closed();
            } catch (InterruptedException e) {
                return false;
            }
        }

    }

    Channel<Address, String> channel;
    private final Address me;
    private boolean running = false;
    private final Object lock = new Object();

    public OtrChannel(Channel<Address, String> channel, Address me) {
        if (me == null) {
            throw new NullPointerException();
        }

        this.channel = channel;
        this.me = me;

    }

    // TODO
    @Override
    public Connection<Address> open(Listener<Address, String> listener) {

        if (listener == null) {
            throw new NullPointerException();
        }

        synchronized (lock) {
            try {
                if (running) return null;
                running = true;
                return new OtrConnection(this.channel.open(listener));
            } catch (InterruptedException e) {
                running = false;
                return null;
            }
        }

    }

    @Override
    public Peer<Address, String> getPeer(Address you) {

        if (you.equals(me)) return null;

        //return this.channel.getPeer(you);
        return new OtrPeer(you, this.channel.getPeer(you));
    }

}