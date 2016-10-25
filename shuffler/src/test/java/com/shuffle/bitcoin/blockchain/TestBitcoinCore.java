package com.shuffle.bitcoin.blockchain;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by nsa on 10/25/16.
 */
public class TestBitcoinCore {

    NetworkParameters netParams = MainNetParams.get();
    BitcoinCore testCase;
    HexBinaryAdapter adapter = new HexBinaryAdapter();
    Context context = Context.getOrCreate(netParams);

    public TestBitcoinCore() throws MalformedURLException {
        testCase = new BitcoinCore(netParams, "admin", "pass");
    }

    @Test
    public void test() throws IOException {
        testCase.getTransaction("7b90096cedc1dccc8b5d674f598da8ee475a607f8f3c28dd6c6634391150ecae");
    }

}
