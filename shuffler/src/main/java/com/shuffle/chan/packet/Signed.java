package com.shuffle.chan.packet;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.proto.Proto;
import com.shuffle.protocol.FormatException;

import java.io.IOException;
import java.io.Serializable;

/**
 * Signed is an object that contains an object and a signature. It is constructed
 * with checks which provide a high probability that it is what it claims to be; ie,
 * an object and a valid signature of the object's serialized form.
 *
 * Created by Daniel Krawisz on 5/24/16.
 */
public class Signed<X> implements Serializable {
    public final X message;
    public final Bytestring signature;

    // If you want to make the signature yourself, use this constructor.
    public Signed(X x, SigningKey key, Marshaller<X> m) throws IOException {
        if (x == null || key == null || m == null) throw new NullPointerException();

        this.message = x;
        this.signature = key.sign(m.marshall(x));
    }

    // If the signature was generated by someone else, use this one.
    public Signed(Bytestring message, Bytestring signature, VerificationKey key, Marshaller<X> m) throws FormatException {
        if (message == null || signature == null || key == null) throw new NullPointerException();

        if (!key.verify(message, signature)) {
            throw new IllegalArgumentException();
        }
        System.out.println("Verified : " + key.toString());

        this.signature = signature;
        this.message = m.unmarshall(message);
    }

    @Override
    public String toString() {
        return "Sig[" + message + ", " + signature + "]";
    }

    @Override
    public int hashCode() {
        return message.hashCode() + 17 * signature.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Signed)) return false;

        Signed x = (Signed)o;

        return message.equals(x.message) && signature.equals(x.signature);
    }
}
