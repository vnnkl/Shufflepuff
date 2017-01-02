package com.shuffle.chan.packet;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.chan.Send;
import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import java.io.IOException;

/**
 * A Send object that adds a signature to
 *
 * Created by Daniel Krawisz on 4/13/16.
 */
public class SigningSend<X> implements Send<X> {

    // TODO replace this with Send<Bytestring>.
    private final Send<Signed<X>> session;
    private final Marshaller<X> marshaller;
    private final SigningKey key;

    public SigningSend(
            Send<Signed<X>> session,
            Marshaller<X> marshaller,
            SigningKey key) {

        if (session == null || marshaller == null || key == null) throw new NullPointerException();

        this.session = session;
        this.marshaller = marshaller;
        this.key = key;
    }

    @Override
    public boolean send(X x) throws InterruptedException, IOException {
        System.out.println("X \n" + x);
        Signed<X> signed;
        try {
            signed = new Signed<X>(x, key, marshaller);
        } catch (NullPointerException e) {
            return false;
        }
		System.out.println(signed);
        return session.send(signed);

    }

    @Override
    public void close() {
        session.close();
    }
}
