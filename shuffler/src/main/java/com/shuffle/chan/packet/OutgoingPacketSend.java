package com.shuffle.chan.packet;

import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;

import java.io.IOException;
import java.io.Serializable;

/**
 * OutgoingPacketSend creates packets with the correct sequence numbers.
 *
 * Created by Daniel Krawisz on 5/24/16.
 */
public class OutgoingPacketSend<Address extends Serializable, X extends Serializable> implements Send<X> {
    private final Send<Packet<Address, X>> send;

    private final Bytestring session;
    private final Address from, to;

    int sequenceNumber = 0;
    boolean closed = false;

    public OutgoingPacketSend(Send<Packet<Address, X>> send, Bytestring session, Address from, Address to) {
        if (send == null || session == null || from == null || to == null) throw new NullPointerException();

        this.send = send;
        this.session = session;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean send(X x) throws InterruptedException, IOException {
        if (closed) return false;

        boolean sent = send.send(new Packet<Address, X>(session, from, to, sequenceNumber, x));

        if (sent) {
            sequenceNumber++;
        }

        return sent;
    }

    @Override
    public void close() {
        if (!closed) {
            send.close();
            closed = true;
        }
    }
}
