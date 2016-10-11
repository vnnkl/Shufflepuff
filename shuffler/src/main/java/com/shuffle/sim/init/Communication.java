package com.shuffle.sim.init;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Receive;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Signed;

import java.util.Map;

/**
 * Connections is used to collect together the objects necessary to create an
 * initialized Adversary.
 *
 * Created by Daniel Krawisz on 10/11/16.
 */
public class Communication<X> {
    public final VerificationKey identity;
    public final Map<VerificationKey, Send<Signed<X>>> send;
    public final Receive<Inbox.Envelope<VerificationKey, Signed<X>>> receive;

    Communication(
            VerificationKey identity,
            Map<VerificationKey, Send<Signed<X>>> send,
            Receive<Inbox.Envelope<VerificationKey, Signed<X>>> receive) {

        if (identity == null || send == null || receive == null)
            throw new NullPointerException();

        this.identity = identity;
        this.send = send;
        this.receive = receive;
    }

    public String toString() {
        return "Connection{identity:" + identity + ", send:" + send + ", receive:" + receive + "}";
    }
}
