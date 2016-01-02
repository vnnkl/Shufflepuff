package com.shuffle.protocol;

import com.shuffle.cryptocoin.CryptographyError;
import com.shuffle.cryptocoin.SigningKey;
import com.shuffle.cryptocoin.VerificationKey;

import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

/**
 * Operations relating to the verification and formatting of inbox when they are sent or received.
 *
 * Created by Daniel Krawisz on 12/7/15.
 */
class NetworkOperations {
    SessionIdentifier τ; // The session identifier of this protocol.
    Network network; // The connection to the shuffle network.
    SigningKey sk;
    int N; // the number of players.
    Map<Integer, VerificationKey> players; // The current players.

    Queue<Packet> delivered = new LinkedList<>(); // A queue of messages that has been delivered that we aren't ready to look at yet.
    Queue<Packet> seen = new LinkedList<>(); // A queue of messages that have been seen, in the order looked at.
    Queue<Packet> sent = new LinkedList<>(); // All messages that have been sent.

    NetworkOperations(SessionIdentifier τ, SigningKey sk, Map<Integer, VerificationKey> players, Network network) {
        this.τ = τ;
        this.network = network;
        this.sk = sk;
        this.players = players;
        this.N = players.size();
    }

    // Get the set of players from i to N.
    public Set<VerificationKey> playerSet(int i, int n) throws CryptographyError, InvalidImplementationError {
        if (i < 1) {
            i = 1;
        }
        Set<VerificationKey> set = new HashSet<>();
        for(int j = i; j <= n; j ++) {
            if (j > N) {
                return set;
            }

            set.add(players.get(j));
        }

        return set;
    }

    public Set<VerificationKey> playerSet() throws CryptographyError, InvalidImplementationError {
        return playerSet(1, N);
    }

    public void broadcast(Message message, ShufflePhase phase, VerificationKey from) throws TimeoutError, CryptographyError, InvalidImplementationError {
        Set<VerificationKey> keys = playerSet();

        for (VerificationKey to : keys) {
            if (!to.equals(sk.VerificationKey())) {
                send(new Packet(message, τ, phase, from, to));
            }
        }
    }

    public void send(Packet packet) throws TimeoutError, CryptographyError, InvalidImplementationError {
        network.sendTo(packet.recipient, packet);
        sent.add(packet);
    }

    // This method should only be called by receiveNextPacket
    private Packet findPacket(ShufflePhase expectedPhase) throws InterruptedException, ValueException {
        // Go through the queue of received messages if any are there.
        if (delivered.size() > 0) {
            Iterator<Packet> i = delivered.iterator();
            while (i.hasNext()) {
                Packet packet = i.next();

                // Return any that matches what we're looking for.
                if (expectedPhase == packet.phase) {
                    i.remove();
                    return packet;
                }
            }
        }

        // Now we wait for the right message from the network, since we haven't already received it.
        while (true) {
            Packet packet = network.receive();
            ShufflePhase phase = packet.phase;

            // Check that this is someone in the same session of this protocol as us.
            if (!τ.equals(packet.τ)) {
                throw new ValueException(ValueException.Values.τ, τ.toString(), packet.τ.toString());
            }

            // Check that this message is intended for us.
            if (!packet.recipient.equals(sk.VerificationKey())) {
                throw new ValueException(ValueException.Values.recipient, sk.VerificationKey().toString(), packet.recipient.toString());
            }

            if (expectedPhase == phase || phase == ShufflePhase.Blame) {
                return packet;
            }

            delivered.add(packet);
        }
    }

    // Get the next message from the phase we're in. It's possible for other players to get
    // ahead under some circumstances, so we have to keep their messages to look at later.
    Packet receiveNextPacket(ShufflePhase expectedPhase)
            throws FormatException, CryptographyError,
            InterruptedException, TimeoutError, InvalidImplementationError, ValueException, BlameException {

        Packet packet = findPacket(expectedPhase);
        seen.add(packet);
        if (packet.phase == ShufflePhase.Blame) {
            throw new BlameException(packet.signer, packet);
        }
        return packet;
    }

    // Get all packets sent or received by phase. Used during blame phase.
    public List<Packet> getPacketsByPhase(ShufflePhase phase) {
        List<Packet> selection = new LinkedList<>();

        for (Packet packet : seen) {
            if (packet.phase == phase) {
                selection.add(packet);
            }
        }

        for (Packet packet : delivered) {
            if (packet.phase == phase) {
                selection.add(packet);
            }
        }

        for (Packet packet : sent) {
            if (packet.phase == phase) {
                selection.add(packet);
            }
        }

        return selection;
    }

    public Message receiveFrom(VerificationKey from, ShufflePhase expectedPhase)
            throws TimeoutError, CryptographyError, FormatException, ValueException,
            InvalidImplementationError, InterruptedException, BlameException {

        Packet packet = receiveNextPacket(expectedPhase);

        // If we receive a message, but it is not from the expected source, it might be a blame message.
        if (!from.equals(packet.signer)) {
            throw new ValueException(ValueException.Values.phase, packet.phase.toString(), expectedPhase.toString());
        }

        return packet.message;
    }

    public Map<VerificationKey, Message> receiveFromMultiple(Set<VerificationKey> from, ShufflePhase expectedPhase)
            throws TimeoutError, CryptographyError, FormatException,
            InvalidImplementationError, ValueException, InterruptedException, ProtocolException, BlameException {

        // Collect the messages in here.
        Map<VerificationKey, Message> broadcasts = new HashMap<>();

        // Don't receive a message from myself.
        from.remove(sk.VerificationKey());

        while (from.size() > 0) {
            Packet packet = receiveNextPacket(expectedPhase);
            VerificationKey sender = packet.signer;

            if(broadcasts.containsKey(sender)) {
                throw new ProtocolException();
            }
            broadcasts.put(sender, packet.message);
            from.remove(sender);
        }

        return broadcasts;
    }

    // When the blame phase it reached, there may be a lot of blame going around. This function
    // waits to receive all blame messages until a timeout exception is caught, and then returns
    // the list of blame messages, organized by player.
    public Map<VerificationKey, List<Packet>> receiveAllBlame() throws InterruptedException, FormatException, ValueException {
        Map<VerificationKey, List<Packet>> blame = new HashMap<>();
        for (VerificationKey player : players.values()) {
            blame.put(player, new LinkedList<Packet>());
        }

        while(true) {
            try {
                Packet next = receiveNextPacket(ShufflePhase.Blame);
                blame.get(next.signer).add(next);
            } catch (BlameException e) {
                // This shouldn't really happen but just in case.
                blame.get(e.packet.signer).add(e.packet);
            } catch (TimeoutError e) {
                break;
            }
        }

        // Get the blame messages we sent too. Just get everything!
        for (Packet packet : sent) {
            if (packet.phase == ShufflePhase.Blame) {
                blame.get(sk.VerificationKey()).add(packet);
            }
        }

        return blame;
    }
}
