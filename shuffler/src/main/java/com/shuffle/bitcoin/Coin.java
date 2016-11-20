/**
 *
 * Copyright © 2016 Mycelium.
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 *
 */

package com.shuffle.bitcoin;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * This interface provides service to the Bitcoin (or other) network. This includes queries to the
 * blockchain as well as to the p2p network. If these services cannot be provided while the protocol
 * is running, then the protocol must not be run.
 *
 * Created by Daniel Krawisz on 12/5/15.
 *
 */
public interface Coin {
    Transaction shuffleTransaction(
            long amount,
            long fee,
            List<VerificationKey> from,
            Map<VerificationKey, HashSet<TransactionOutPoint>> peerUtxos,
            Queue<Address> to,
            Map<VerificationKey, Address> changeAddresses) throws CoinNetworkException, AddressFormatException;

    long valueHeld(HashSet<TransactionOutPoint> utxos) throws CoinNetworkException, AddressFormatException;

    // Returns true if the utxo list has sufficient funds, false otherwise
    boolean sufficientFunds(HashSet<TransactionOutPoint> utxos, long amount) throws CoinNetworkException, AddressFormatException, IOException;

    // If there is a conflicting transaction in the mempool or blockchain, this function
    // returns that transaction.
    Transaction getConflictingTransaction(Transaction transaction, HashSet<TransactionOutPoint> utxos, long amount)
            throws CoinNetworkException, AddressFormatException, BlockStoreException, IOException;

}
