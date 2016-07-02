package com.shuffle.chan.packet;

import com.shuffle.p2p.Bytestring;
import com.shuffle.protocol.FormatException;

import java.io.IOException;

/**
 * Represents a way of serializing a class.
 *
 * Created by Daniel Krawisz on 1/31/16.
 */
public interface Marshaller<X> {
    // Should never throw an exception because it should have been built properly.
    // If this throws an exception, that indicates a bug in the program itself.
    Bytestring marshall(X x) throws IOException;

    // FormatException is thrown here to indicate an invalid message from an
    // adversary.
    X unmarshall(Bytestring string) throws FormatException;
}
