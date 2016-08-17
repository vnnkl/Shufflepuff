/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.sim;

import com.shuffle.bitcoin.Address;
import com.shuffle.bitcoin.Coin;
import com.shuffle.bitcoin.CoinNetworkException;
import com.shuffle.bitcoin.SigningKey;
import com.shuffle.bitcoin.Transaction;
import com.shuffle.protocol.FormatException;

/**
 * Represents a fake Bitcoin network that can be manipulated for testing purposes.
 *
 * Created by Daniel Krawisz on 2/8/16.
 */
public interface MockCoin extends Coin {
    Coin mutated(); // Returns a new mock coin that produces mutated transactions.

    void put(Address addr, long value);

    // Make a transaction that spends the coins from a given address.
    Transaction makeSpendingTransaction(SigningKey from, Address to, long amount) throws FormatException, CoinNetworkException;
    
    // Whether the given transaction spends the funds in the given address.
    Transaction getSpendingTransaction(Address addr, long amount);

    MockCoin copy();
}
