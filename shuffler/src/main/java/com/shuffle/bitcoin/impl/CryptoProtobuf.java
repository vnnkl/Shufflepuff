package com.shuffle.bitcoin.impl;

import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Signed;
import com.shuffle.player.Message;
import com.shuffle.player.P;
import com.shuffle.player.Protobuf;
import com.shuffle.player.proto.Proto;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.message.Packet;

import java.util.Queue;

/**
 * Created by Daniel Krawisz on 7/14/16.
 */
public class CryptoProtobuf extends Protobuf {
    @Override
    public Message.Atom unmarshallAtom(Proto.Message atom) throws FormatException {
        return null;
    }

    @Override
    public Blame unmarshallBlame(Proto.Blame blame) throws FormatException {
        return null;
    }

    @Override
    public Queue<com.shuffle.protocol.message.Packet> unmarshallPackets(Proto.Packets pp) throws FormatException {
        return null;
    }

    @Override
    public Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> unmarshallSignedPacket(Proto.Signed sp) throws FormatException {
        return null;
    }

    @Override
    public com.shuffle.chan.packet.Packet<VerificationKey, P> unmarshallPacket(Proto.Packet p) throws FormatException {
        return null;
    }
}
