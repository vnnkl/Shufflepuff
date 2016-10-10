package com.shuffle.p2p;

import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.VerificationKey;
import com.shuffle.chan.Send;
import com.shuffle.chan.packet.Marshaller;
import com.shuffle.chan.packet.Packet;
import com.shuffle.chan.packet.Signed;
import com.shuffle.mock.MockNetwork;
import com.shuffle.mock.MockProtobuf;
import com.shuffle.mock.MockSigningKey;
import com.shuffle.player.P;
import com.shuffle.player.Protobuf;
import com.shuffle.protocol.FormatException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by nsa on 10/4/16.
 */

public class TestOtrMockChannel {

    private MockNetwork<Integer, Bytestring> mock;
    private Channel<Integer, Bytestring> n;
    private Channel<Integer, Bytestring> m;
    private OtrChannel<Integer> o_n;
    private OtrChannel<Integer> o_m;
    private Channel<Integer, Signed<Packet<VerificationKey, P>>> m_n;
    private Channel<Integer, Signed<Packet<VerificationKey, P>>> m_m;
    private Send<Signed<Packet<VerificationKey, P>>> s_n;
    private Send<Signed<Packet<VerificationKey, P>>> s_m;
    private Listener<Integer,Signed<Packet<VerificationKey, P>>> l_n;
    private Listener<Integer,Signed<Packet<VerificationKey, P>>> l_m;
    private Peer<Integer,Signed<Packet<VerificationKey, P>>> p_n;
    private Peer<Integer,Signed<Packet<VerificationKey, P>>> p_m;
    private Session<Integer,Signed<Packet<VerificationKey, P>>> z_n;
    private Session<Integer,Signed<Packet<VerificationKey, P>>> z_m;

    @Before
    public void setup() throws InterruptedException, IOException {
        mock = new MockNetwork<>();
        n = mock.node(0);
        m = mock.node(1);
        o_n = new OtrChannel<>(n);
        o_m = new OtrChannel<>(m);
        m_n = new MarshallChannel<>(o_n,new MockProtobuf().signedMarshaller());
        m_m = new MarshallChannel<>(o_m,new MockProtobuf().signedMarshaller());

        s_n = new Send<Signed<Packet<VerificationKey, P>>>() {
            @Override
            public boolean send(Signed<Packet<VerificationKey, P>> packetSigned) throws InterruptedException, IOException {
                System.out.println("n: received");
                return true;
            }

            @Override
            public void close() {
                System.out.println("n: closed");
            }
        };

        l_n = new Listener<Integer, Signed<Packet<VerificationKey, P>>>() {
            @Override
            public Send<Signed<Packet<VerificationKey, P>>> newSession(Session<Integer, Signed<Packet<VerificationKey, P>>> session) throws InterruptedException {
                System.out.println("n: caught");
                return s_n;
            }
        };

        s_m = new Send<Signed<Packet<VerificationKey, P>>>() {
            @Override
            public boolean send(Signed<Packet<VerificationKey, P>> packetSigned) throws InterruptedException, IOException {
                System.out.println("m: received");
                return true;
            }

            @Override
            public void close() {
                System.out.println("m: closed");
            }
        };

        l_m = new Listener<Integer, Signed<Packet<VerificationKey, P>>>() {
            @Override
            public Send<Signed<Packet<VerificationKey, P>>> newSession(Session<Integer, Signed<Packet<VerificationKey, P>>> session) throws InterruptedException {
                System.out.println("m: caught");
                z_m = session;
                return s_m;
            }
        };

        m_n.open(l_n);
        m_m.open(l_m);

    }

    @Test
    public void test() throws InterruptedException, IOException, FormatException {

        p_n = m_n.getPeer(1);
        p_m = m_m.getPeer(0);
        z_n = p_n.openSession(s_n);

        // SigningKey
        SigningKey sk = new MockSigningKey(1);

        Bytestring message;
        Bytestring signature;
        VerificationKey vk = sk.VerificationKey();
        Marshaller<Signed<Packet<VerificationKey, P>>> x = new MockProtobuf().signedMarshaller();

        Signed<Packet<VerificationKey, P>> e = new Signed<>(null, null, null, null);



        z_n.send(e);
        z_m.send(e);

    }

    @After
    public void shutdown() {

    }

}
