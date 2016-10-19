package com.shuffle.sim.init;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.MockNetwork;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Channel;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.MarshallChannel;
import com.shuffle.p2p.OtrChannel;
import com.shuffle.p2p.Session;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel on 10/18/16.
 */

public class MockInitializer<X extends Serializable> implements Initializer<X> {
    // The set of incoming mailboxes for each player.
    final Map<SigningKey, Inbox<VerificationKey, Signed<X>>> mailboxes = new HashMap<>();

    // The set of channels that player use to send to other players.
    final Map<SigningKey, Map<VerificationKey, Send<Signed<X>>>> networks = new HashMap<>();

    final Bytestring session;
    final int capacity;

    private final MockNetwork<VerificationKey, Signed<X>> mockNetwork = new MockNetwork<>();

    private final List<Connection<VerificationKey>> connections = new LinkedList<>();

    public MockInitializer(Bytestring session, int capacity) {

        if (session == null || capacity == 0) throw new IllegalArgumentException();

        this.session = session;
        this.capacity = capacity;
    }

    // This function is called every time a new player is created in a simulation.
    public Communication<X> connect(SigningKey sk) throws IOException, InterruptedException {

        VerificationKey vk = sk.VerificationKey();

        // Create a new map. This will contain the channels from this mailbox to the others.
        final Map<VerificationKey, Send<Signed<X>>> inputs = new HashMap<>();
        networks.put(sk, inputs);

        // Create a new mailbox.
        final Inbox<VerificationKey, Signed<X>> inbox = new Inbox<>(capacity);

        // Create a new channel.
        Channel<VerificationKey, Signed<X>> channel = mockNetwork.node(sk.VerificationKey());

        // Open the channel.
        connections.add(channel.open(new Listener<VerificationKey, Signed<X>>() {

            @Override
            public Send<Signed<X>> newSession(Session<VerificationKey, Signed<X>> session) throws InterruptedException {
                VerificationKey key = session.peer().identity();

                // Create a session from the new mailbox to the previous one.
                inputs.put(key, session);

                // And create a corresponding session the other way.
                return inbox.receivesFrom(key);
            }
        }));

        // Create input channels for this new mailbox that lead to all other mailboxes
        // and create input channels for all the other mailboxes for this new one.
        for (SigningKey ks : mailboxes.keySet()) {
            VerificationKey kv = ks.VerificationKey();

            // And create a corresponding session the other way.
            inputs.put(kv, channel.getPeer(kv).openSession(inbox.receivesFrom(vk)));
        }

        // Put the mailbox in the set.
        mailboxes.put(sk, inbox);

        return new Communication<>(vk, inputs, inbox);
    }

    @Override
    public void clear() {
        networks.clear();

        for (Connection<VerificationKey> c : connections) {
            c.close();
        }
    }
}
