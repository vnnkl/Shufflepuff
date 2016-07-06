package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The JavaMarshaller uses java's serializable interface to turn objects into byte arrays.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public class JavaMarshaller<X extends Serializable> implements
        Marshaller<X> {

    private static final Logger log = LogManager.getLogger(JavaMarshaller.class);

    @Override
    public Bytestring marshall(X x) throws IOException {

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(x);
        return new Bytestring(b.toByteArray());
    }

    @Override
    public X unmarshall(Bytestring string) throws FormatException {

        ByteArrayInputStream b = new ByteArrayInputStream(string.bytes);
        Object obj = null;
        try {
            ObjectInputStream o = new ObjectInputStream(b);
            obj = o.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new FormatException("Could not unmarshall " + string + " got error " + e);
        }

        try {
            return (X)obj;
        } catch (ClassCastException e) {
            throw new FormatException(e.getMessage());
        }
    }
}
