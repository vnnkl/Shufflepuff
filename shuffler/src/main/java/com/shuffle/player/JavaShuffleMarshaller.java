package com.shuffle.player;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.JavaMarshaller;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;

/**
 * Created by Daniel Krawisz on 7/16/16.
 */
public class JavaShuffleMarshaller implements Messages.ShuffleMarshaller {
    @Override
    public Marshaller<Message.Atom> atomMarshaller() {
        return new JavaMarshaller<>();
    }

    @Override
    public Marshaller<Address> addressMarshaller() {
        return new JavaMarshaller<>();
    }

    @Override
    public Marshaller<Packet<VerificationKey, P>> packetMarshaller() {
        return new JavaMarshaller<>();
    }

    @Override
    public Marshaller<Signed<Packet<VerificationKey, P>>> signedMarshaller() {
        return new JavaMarshaller<>();
    }
}
