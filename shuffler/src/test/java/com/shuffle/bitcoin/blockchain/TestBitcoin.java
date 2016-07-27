package com.shuffle.bitcoin.blockchain;

import com.shuffle.bitcoin.CoinNetworkException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by Daniel Krawisz on 7/26/16.
 */
public class TestBitcoin {
    private class MockBitcoin extends Bitcoin {
        public MockBitcoin() {
            super(NetworkParameters.fromID(NetworkParameters.ID_TESTNET), 3);
        }

        @Override
        List<Transaction> getAddressTransactionsInner(String address) throws IOException, CoinNetworkException, AddressFormatException {
            return null;
        }

        @Override
        org.bitcoinj.core.Transaction getTransaction(String transactionHash) throws IOException {
            return null;
        }


    }

    // TODO
    @Test
    public void testSufficientFunds() {

    }
}
