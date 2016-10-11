package com.shuffle.sim.init;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 10/11/16.
 */

public class BasicInitializer<X> implements Initializer<X> {
    // The set of incoming mailboxes for each player.
    final Map<SigningKey, Inbox<VerificationKey, Signed<X>>> mailboxes = new HashMap<>();

    // The set of channels that player use to send to other players.
    final Map<SigningKey, Map<VerificationKey, Send<Signed<X>>>> networks = new HashMap<>();

    final Bytestring session;
    final int capacity;

    public BasicInitializer(Bytestring session, int capacity) {

        if (session == null || capacity == 0) throw new IllegalArgumentException();

        this.session = session;
        this.capacity = capacity;
    }

    // This function is called every time a new player is created in a simulation.
    public Communication<X> connect(SigningKey sk)
            throws IOException, InterruptedException {

        VerificationKey vk = sk.VerificationKey();

        // Create a new map. This will contain the channels from this mailbox to the others.
        final Map<VerificationKey, Send<Signed<X>>> inputs = new HashMap<>();
        networks.put(sk, inputs);

        // Ceate a new mailbox.
        final Inbox<VerificationKey, Signed<X>> inbox = new Inbox<>(capacity);

        // Create input channels for this new mailbox that lead to all other mailboxes
        // and create input channels for all the other mailboxes for this new one.
        for (Map.Entry<SigningKey, Inbox<VerificationKey, Signed<X>>> entry : mailboxes.entrySet()) {
            SigningKey ks = entry.getKey();
            VerificationKey kv = ks.VerificationKey();
            Inbox<VerificationKey, Signed<X>> box = entry.getValue();

            // Create a session from the new mailbox to the previous one.
            inputs.put(kv, box.receivesFrom(vk));

            // And create a corresponding session the other way.
            networks.get(ks).put(vk, inbox.receivesFrom(kv));
        }

        // Put the mailbox in the set.
        mailboxes.put(sk, inbox);

        return new Communication<>(vk, inputs, inbox);
    }

    @Override
    public void clear() {
        networks.clear();
    }
}
