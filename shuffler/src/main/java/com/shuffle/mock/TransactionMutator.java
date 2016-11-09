/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.mock;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.bitcoin.VerificationKey;

import org.bitcoinj.core.TransactionOutPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;

/**
 * Created by Daniel Krawisz on 3/9/16.
 */
public class TransactionMutator implements Coin {
    private final MockCoin coin;

    public TransactionMutator(MockCoin coin) {
        this.coin = coin;
    }


    @Override
    // TODO
    public Transaction shuffleTransaction(
            long amount,
            long fee,
            List<VerificationKey> from,
            Map<VerificationKey, SortedSet<TransactionOutPoint>> peerUtxos,
            Queue<Address> to,
            Map<VerificationKey, Address> changeAddresses
    ) throws CoinNetworkException {
        return null;
        /*
        MockCoin.MockTransaction tr = (MockCoin.MockTransaction) coin.shuffleTransaction(
                        amount, fee, from, to, changeAddresses);

        return new MockCoin.MockTransaction(tr.inputs, tr.outputs, tr.z + 1, coin,
                new HashMap<MockCoin.Output, VerificationKey>());*/
    }

    @Override
    //public long valueHeld(Address addr) throws CoinNetworkException {
    public long valueHeld(SortedSet<TransactionOutPoint> utxos) {
        //return coin.valueHeld(addr);
        return 0;
    }

    @Override
    // TODO
    //public boolean sufficientFunds(Address addr, long amount) {
    public boolean sufficientFunds(SortedSet<TransactionOutPoint> utxos, long amount) {
        //return coin.sufficientFunds(addr, amount);
        return false;
    }

    @Override
    public Transaction getConflictingTransaction(Transaction t, Address addr, long amount) {
        return coin.getConflictingTransaction(t, addr, amount);
    }
}
