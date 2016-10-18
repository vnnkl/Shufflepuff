package com.shuffle.mock;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.BasicChan;
import com.shuffle.chan.Chan;
import com.shuffle.chan.Inbox;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.p2p.Bytestring;
import com.shuffle.player.Messages;
import com.shuffle.player.Payload;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;
import com.shuffle.protocol.message.Phase;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Daniel Krawisz on 10/17/16.
 */

public class TestMarshall {

    @Test
    public void testMarshall() throws IOException, FormatException, NoSuchAlgorithmException, InterruptedException {

        Bytestring session = new Bytestring("s".getBytes());
        SigningKey me = new MockSigningKey(100);
        VerificationKey you = new MockVerificationKey(2);
        Protobuf proto = new MockProtobuf();
        Chan<Bytestring> chan = new BasicChan<>(3);
        Map<VerificationKey,
                Send<Signed<Packet<VerificationKey, Payload>>>> sendTo = new HashMap<>();

        sendTo.put(you, new Send<Signed<Packet<VerificationKey, Payload>>>(){

            @Override
            public boolean send(Signed<Packet<VerificationKey, Payload>> sp) throws InterruptedException, IOException {
                return chan.send(proto.signedMarshaller.marshall(sp));
            }

            @Override
            public void close() {
                chan.close();
            }
        });

        Messages messages = new Messages(session, me, sendTo, new Inbox<>(10), proto);

        List<com.shuffle.protocol.message.Packet> testCases = new LinkedList<>();

        testCases.add(messages.make().attach(new MockEncryptionKey(1))
                .attach(new MockEncryptionKey(2))
                .attach(new MockAddress(2))
                .attach(new MockAddress(4)).hashed().send(Phase.Announcement, new MockVerificationKey(2)));

        for (com.shuffle.protocol.message.Packet p : testCases) {

            Assert.assertTrue(p.equals(new Messages.SignedPacket(proto.signedMarshaller.unmarshall(chan.receive()))));
        }
    }
}
