package com.shuffle.sim.init;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;
import com.shuffle.p2p.Connection;
import com.shuffle.p2p.HistoryChannel;
import com.shuffle.p2p.Listener;
import com.shuffle.p2p.MappedChannel;
import com.shuffle.p2p.MarshallChannel;
import com.shuffle.p2p.OtrChannel;
import com.shuffle.p2p.Session;
import com.shuffle.p2p.TcpChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * TcpInitializer creates TcpChannels for all peers during a simulation.
 *
 * Created by Daniel Krawisz on 1/15/17.
 */

public class TcpInitializer<X> implements Initializer<X> {
    // The set of incoming mailboxes for each player.
    private final Map<SigningKey, Inbox<VerificationKey, Signed<X>>> mailboxes = new HashMap<>();

    private final int capacity;
    private final Marshaller<Signed<X>> marshaller;
    private final Map<VerificationKey, InetSocketAddress> addresses;
    private final boolean otr;

    private final List<Connection<VerificationKey>> connections = new LinkedList<>();

    private final Map<SigningKey, HistoryChannel<VerificationKey, Signed<X>>> channels = new HashMap<>();

    public TcpInitializer(
            int capacity,
            Marshaller<Signed<X>> marshaller,
            Map<VerificationKey, InetSocketAddress> addresses,
            boolean otr) {

        if (capacity == 0 || marshaller == null) throw new IllegalArgumentException();

        this.capacity = capacity;
        this.marshaller = marshaller;
        this.otr = otr;
        this.addresses = addresses;
    }

    @Override
    public Communication<X> connect(SigningKey sk) throws IOException, InterruptedException {
        VerificationKey vk = sk.VerificationKey();

        // Create a new map. This will contain the channels from this mailbox to the others.
        final Map<VerificationKey, Send<Signed<X>>> inputs = new HashMap<>();

        // Create a new mailbox.
        final Inbox<VerificationKey, Signed<X>> inbox = new Inbox<>(capacity);

        // Create a new channel.
        InetSocketAddress address = addresses.get(vk);
        if (address == null) throw new IllegalArgumentException("Unrecognized identity.");
        MappedChannel<VerificationKey> m = new MappedChannel<>(new TcpChannel(address), addresses, vk);

        HistoryChannel<VerificationKey, Signed<X>> channel;
        if (otr) {
            channel = new HistoryChannel<>(new MarshallChannel<>(new OtrChannel<>(m), marshaller));
        } else {
            channel = new HistoryChannel<>(new MarshallChannel<>(m, marshaller));
        }

        channels.put(sk, channel);

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
    public Map<VerificationKey, Map<VerificationKey, List<HistoryChannel<VerificationKey, Signed<X>>.HistorySession>>> end() {
        for (Connection<VerificationKey> c : connections) {
            c.close();
        }

        Map<VerificationKey, Map<VerificationKey, List<HistoryChannel<VerificationKey, Signed<X>>.HistorySession>>> histories = new HashMap<>();
        for (Map.Entry<SigningKey, HistoryChannel<VerificationKey, Signed<X>>> entry : channels.entrySet()) {
            histories.put(entry.getKey().VerificationKey(), entry.getValue().histories());
        }

        return histories;
    }
}
