package com.shuffle.p2p;

import com.shuffle.mock.MockNetwork;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by nsa on 10/4/16.
 */

public class TestOtrMockChannel {

    private MockNetwork<Integer, Bytestring> mock;
    private Channel<Integer, Bytestring> n;
    private Channel<Integer, Bytestring> m;
    private OtrChannel<Integer> o_n;
    private OtrChannel<Integer> o_m;

    @Before
    public void setup() {
        mock = new MockNetwork<>();
        n = mock.node(0);
        m = mock.node(1);
        o_n = new OtrChannel<>(n);

    }

    @Test
    public void test() {

    }

    @After
    public void shutdown() {

    }

}
