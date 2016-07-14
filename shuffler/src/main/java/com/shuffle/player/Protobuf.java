package com.shuffle.player;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.MockAddress;
import com.shuffle.mock.MockCoin;
import com.shuffle.mock.MockDecryptionKey;
import com.shuffle.mock.MockEncryptionKey;
import com.shuffle.mock.MockVerificationKey;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.proto.Proto;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.Reason;
import com.shuffle.protocol.message.Phase;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 7/2/16.
 */
public abstract class Protobuf {

    public abstract Message.Atom unmarshallAtom(Proto.Message atom) throws FormatException;
    public abstract Blame unmarshallBlame(Proto.Blame blame) throws FormatException;
    public abstract Queue<com.shuffle.protocol.message.Packet> unmarshallPackets(Proto.Packets pp) throws FormatException;
    public abstract Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> unmarshallSignedPacket(Proto.Signed sp) throws FormatException;
    public abstract com.shuffle.chan.packet.Packet<VerificationKey, P> unmarshallPacket(Proto.Packet p) throws FormatException;

    public static Proto.Signed.Builder marshallSignedPacket(com.shuffle.protocol.message.Packet p) {
        if (p == null || !(p instanceof Messages.SignedPacket)) {
            throw new IllegalArgumentException("Unknown implementation of Packet.");
        }

        return marshallSignedPacket(((Messages.SignedPacket) p).packet);
    }

    public static Proto.Packet.Builder marshallPacket(com.shuffle.chan.packet.Packet<VerificationKey, P> p) {
        Proto.Phase phase;
        switch(p.payload.phase) {
            case Announcement: {
                phase = Proto.Phase.ANNOUNCEMENT;
                break;
            }
            case Shuffling: {
                phase = Proto.Phase.SHUFFLE;
                break;
            }
            case BroadcastOutput: {
                phase = Proto.Phase.BROADCAST;
                break;
            }
            case EquivocationCheck: {
                phase = Proto.Phase.EQUIVOCATION_CHECK;
                break;
            }
            case VerificationAndSubmission: {
                phase = Proto.Phase.VERIFICATION_AND_SUBMISSION;
                break;
            }
            case Blame: {
                phase = Proto.Phase.BLAME;
                break;
            }
            default : {
                throw new IllegalArgumentException("Invalid phase " + p.payload.phase);
            }
        };

        Proto.Message.Builder mb = Proto.Message.newBuilder();
        Object msg = p.payload.message;

        if (msg == null || !(msg instanceof Message)) {
           throw new IllegalArgumentException("Null or unknown Message format.");
        }

        Proto.Packet.Builder pb = Proto.Packet.newBuilder()
                .setSession(ByteString.copyFrom(p.session.bytes))
                .setTo(Proto.VerificationKey.newBuilder()
                        .setKey(p.to.toString()))
                .setFrom(Proto.VerificationKey.newBuilder()
                        .setKey(p.from.toString()))
                .setNumber(p.sequenceNumber)
                .setPhase(phase)
                .setMessage(mb);

        Message.Atom atom = ((Message)msg).atoms;
        if (atom != null) {
            pb.setMessage(marshallAtom(atom));
        }

        return pb;
    }

    public static Proto.Message.Builder marshallAtom(Message.Atom atom) {
        Proto.Message.Builder ab = Proto.Message.newBuilder();

        if (atom.addr != null) {
            ab.setAddress(Proto.Address.newBuilder().setAddress(atom.addr.toString()));
        } else if (atom.ek != null) {
            ab.setKey(Proto.EncryptionKey.newBuilder().setKey(atom.ek.toString()));
        } else if (atom.secureHash != null) {
            ab.setHash(Proto.Hash.newBuilder().setHash(
                    ByteString.copyFrom(atom.secureHash.toString().getBytes())));
        } else if (atom.sig != null) {
            ab.setSignature(Proto.Signature.newBuilder().setSignature(
                    ByteString.copyFrom(atom.sig.bytes)));
        } else if (atom.blame != null) {
            ab.setBlame(marshallBlame(atom.blame));
        } else {
            throw new IllegalArgumentException("Atom cannot be empty.");
        }

        if (atom.next != null) {
            ab.setNext(marshallAtom(atom.next));
        }

        return ab;
    }

    public static Proto.Signed.Builder marshallSignedPacket(Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> p) {
        return Proto.Signed.newBuilder().setPacket(marshallPacket(p.message)).setSignature(
                Proto.Signature.newBuilder().setSignature(ByteString.copyFrom(p.signature.bytes)));
    }

    public static Proto.Blame.Builder marshallBlame(Blame b) {
        Proto.Reason reason;

        if (b.reason == Reason.InsufficientFunds) {
            reason = Proto.Reason.INSUFFICIENTFUNDS;
        } else if (b.reason == Reason.NoFundsAtAll) {
            reason = Proto.Reason.NOFUNDSATALL;
        } else if (b.reason == Reason.DoubleSpend) {
            reason = Proto.Reason.DOUBLESPEND;
        } else if (b.reason == Reason.EquivocationFailure) {
            reason = Proto.Reason.EQUIVOCATIONFAILURE;
        } else if (b.reason == Reason.ShuffleFailure) {
            reason = Proto.Reason.SHUFFLEFAILURE;
        } else if (b.reason == Reason.ShuffleAndEquivocationFailure) {
            reason = Proto.Reason.SHUFFLEANDEQUIVOCATIONFAILURE;
        } else if (b.reason == Reason.InvalidSignature) {
            reason = Proto.Reason.INVALIDSIGNATURE;
        } else if (b.reason == Reason.MissingOutput) {
            reason = Proto.Reason.MISSINGOUTPUT;
        } else if (b.reason == Reason.Liar) {
            reason = Proto.Reason.LIAR;
        } else if (b.reason == Reason.InvalidFormat) {
            reason = Proto.Reason.INVALIDFORMAT;
        } else {
            throw new IllegalArgumentException("Invalid blame reason " + b.reason);
        }

        Proto.Blame.Builder bb = Proto.Blame.newBuilder().setReason(reason);

        if (b.accused != null) {
            bb.setAccused(Proto.VerificationKey.newBuilder().setKey(b.accused.toString()));
        }

        if (b.privateKey != null) {
            bb.setKey(Proto.DecryptionKey.newBuilder().setKey(b.privateKey.toString()));
        }

        if (b.t != null) {
            bb.setTransaction(Proto.Transaction.newBuilder().setTransaction(ByteString.copyFrom(b.t.serialize().bytes)));
        }

        if (b.invalid != null) {
            bb.setInvalid(Proto.Invalid.newBuilder().setInvalid(ByteString.copyFrom(b.invalid.bytes)));
        }

        if (b.packets != null) {
            for (com.shuffle.protocol.message.Packet p : b.packets) {
                bb.setPackets(Proto.Packets.newBuilder().addPacket(marshallSignedPacket(p)));
            }
        }

        return bb;
    }

    public final Packet packetMarshaller;
    public final Atom atomMarshaller;

    public Protobuf() {
        packetMarshaller = new Packet();
        atomMarshaller = new Atom();

    }

    public class Atom implements Marshaller<Message.Atom> {

        @Override
        public Bytestring marshall(Message.Atom atom) {
            return new Bytestring(marshallAtom(atom).build().toByteArray());
        }

        @Override
        public Message.Atom unmarshall(Bytestring string) throws FormatException {

            Proto.Message atom;
            try {
                atom = Proto.Message.parseFrom(string.bytes);
            } catch (InvalidProtocolBufferException e) {
                throw new FormatException("Could not read " + Arrays.toString(string.bytes));
            }

            return unmarshallAtom(atom);
        }
    }

    public class Packet implements Marshaller<com.shuffle.chan.packet.Packet<VerificationKey, P>> {

        @Override
        public Bytestring marshall(com.shuffle.chan.packet.Packet<VerificationKey, P> p) throws IOException {
            return new Bytestring(marshallPacket(p).build().toByteArray());
        }

        @Override
        public com.shuffle.chan.packet.Packet<VerificationKey, P> unmarshall(Bytestring string) throws FormatException {
            try {
                return unmarshallPacket(Proto.Packet.parseFrom(string.bytes));
            } catch (InvalidProtocolBufferException e) {
                throw new FormatException("Could not read " + string + " as Packet.");
            }
        }
    }
}
