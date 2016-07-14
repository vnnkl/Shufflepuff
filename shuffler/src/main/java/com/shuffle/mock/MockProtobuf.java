package com.shuffle.mock;

import com.shuffle.bitcoin.DecryptionKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Message;
import com.shuffle.player.Messages;
import com.shuffle.player.P;
import com.shuffle.player.Protobuf;
import com.shuffle.player.proto.Proto;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.blame.Blame;
import com.shuffle.protocol.blame.Reason;
import com.shuffle.protocol.message.Phase;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Daniel Krawisz on 7/14/16.
 */
public class MockProtobuf extends Protobuf {

    @Override
        public Message.Atom unmarshallAtom(Proto.Message atom) throws FormatException {

            Object o;
            // Only one field is allowed to be set in the Atom.
            if (atom.hasAddress()) {
                if (atom.hasKey() || atom.hasHash() || atom.hasSignature() || atom.hasBlame()) {
                    throw new FormatException("Atom contains more than one value.");
                }

                o = new MockAddress(atom.getAddress().getAddress());
            } else if (atom.hasKey()) {
                if (atom.hasHash() || atom.hasSignature() || atom.hasBlame()) {
                    throw new FormatException("Atom contains more than one value.");
                }

                try {
                    o = new MockEncryptionKey(atom.getKey().getKey());
                } catch (NumberFormatException e) {
                    throw new FormatException("Could not read " + atom.getKey().getKey() + " as number.");
                }
            } else if (atom.hasHash()) {
                if (atom.hasSignature() || atom.hasBlame()) {
                    throw new FormatException("Atom contains more than one value.");
                }

                o = new Message.SecureHash(new Bytestring(atom.getHash().getHash().toByteArray()));
            } else if (atom.hasSignature()) {
                if (atom.hasBlame()) throw new FormatException("Atom contains more than one value.");

                o = new Bytestring(atom.getSignature().getSignature().toByteArray());
            } else if (atom.hasBlame()) {
                o = unmarshallBlame(atom.getBlame());
            } else {
                throw new FormatException("Atom contains no values.");
            }

            if (atom.hasNext()) {
                return Message.Atom.make(o, unmarshallAtom(atom.getNext()));
            } else {
                return Message.Atom.make(o);
            }
        }

        @Override
        public Blame unmarshallBlame(Proto.Blame blame) throws FormatException {
            Reason reason;
            switch (blame.getReason()) {
                case INSUFFICIENTFUNDS: {
                    reason = Reason.InsufficientFunds;
                    break;
                }
                case NOFUNDSATALL: {
                    reason = Reason.NoFundsAtAll;
                    break;
                }
                case DOUBLESPEND: {
                    reason = Reason.DoubleSpend;
                    break;
                }
                case EQUIVOCATIONFAILURE: {
                    reason = Reason.EquivocationFailure;
                    break;
                }
                case SHUFFLEFAILURE: {
                    reason = Reason.ShuffleFailure;
                    break;
                }
                case SHUFFLEANDEQUIVOCATIONFAILURE: {
                    reason = Reason.ShuffleAndEquivocationFailure;
                    break;
                }
                case INVALIDSIGNATURE: {
                    reason = Reason.InvalidSignature;
                    break;
                }
                case MISSINGOUTPUT: {
                    reason = Reason.MissingOutput;
                    break;
                }
                case LIAR: {
                    reason = Reason.Liar;
                    break;
                }
                case INVALIDFORMAT: {
                    reason = Reason.InvalidFormat;
                    break;
                }
                default : {
                    throw new FormatException("Unknown blame reason " + blame.getReason());
                }
            }

            VerificationKey accused = null;
            if (blame.hasAccused()) {
                try {
                    accused = new MockVerificationKey(blame.getAccused().getKey());
                } catch (NumberFormatException e) {
                    throw new FormatException(e.getMessage());
                }
            }

            DecryptionKey key = null;
            if (blame.hasKey()) {
                try {
                    key = new MockDecryptionKey(blame.getKey().getKey());
                } catch (NumberFormatException e) {
                    throw new FormatException(e.getMessage());
                }
            }

            Transaction t = null;
            if (blame.hasTransaction()) {
                t = MockCoin.MockTransaction.fromBytes(
                        new Bytestring(blame.getTransaction().getTransaction().toByteArray()));
            }

            Bytestring invalid = null;
            if (blame.hasInvalid()) {
                invalid = new Bytestring(blame.getInvalid().getInvalid().toByteArray());
            }

            Queue<com.shuffle.protocol.message.Packet> packets = null;
            if (blame.hasPackets()) {
                packets = unmarshallPackets(blame.getPackets());
            }

            return new Blame(reason, accused, t, key, invalid, packets);
        }

        @Override
        public Queue<com.shuffle.protocol.message.Packet> unmarshallPackets(Proto.Packets pp) throws FormatException {
            Queue<com.shuffle.protocol.message.Packet> packets = new LinkedList<>();

            for (Proto.Signed p : pp.getPacketList()) {
                packets.add(new Messages.SignedPacket(unmarshallSignedPacket(p)));
            }

            return packets;
        }

        @Override
        public Signed<com.shuffle.chan.packet.Packet<VerificationKey, P>> unmarshallSignedPacket(Proto.Signed sp) throws FormatException {
            if (!(sp.hasSignature() && sp.hasPacket() && sp.getPacket().hasFrom())) {
                throw new FormatException("All entries in Signed must be filled:" + sp);
            }

            return new Signed<>(
                    new Bytestring(sp.getPacket().toByteArray()),
                    new Bytestring(sp.getSignature().getSignature().toByteArray()),
                    new MockVerificationKey(sp.getPacket().getFrom().getKey()),
                    packetMarshaller);
        }

        @Override
        public com.shuffle.chan.packet.Packet<VerificationKey, P> unmarshallPacket(Proto.Packet p) throws FormatException {
            if (!(p.hasFrom() && p.hasTo() && p.hasMessage())) {
                throw new FormatException("All entries in Packet must be filled: " + p);
            }

            Phase phase;
            switch(p.getPhase()) {
                case ANNOUNCEMENT: {
                    phase = Phase.Announcement;
                    break;
                }
                case SHUFFLE: {
                    phase = Phase.Shuffling;
                    break;
                }
                case BROADCAST: {
                    phase = Phase.BroadcastOutput;
                    break;
                }
                case EQUIVOCATION_CHECK: {
                    phase = Phase.EquivocationCheck;
                    break;
                }
                case VERIFICATION_AND_SUBMISSION: {
                    phase = Phase.VerificationAndSubmission;
                    break;
                }
                case BLAME: {
                    phase = Phase.Blame;
                    break;
                }
                default : {
                    throw new FormatException("Invalid phase " + p.getPhase());
                }
            };

            return new com.shuffle.chan.packet.Packet<>(
                    new Bytestring(p.getSession().toByteArray()),
                    (VerificationKey)new MockVerificationKey(p.getFrom().getKey()),
                    new MockVerificationKey(p.getTo().getKey()),
                    p.getNumber(),
                    new P(phase, new Message(unmarshallAtom(p.getMessage()), null)));
        }
}
