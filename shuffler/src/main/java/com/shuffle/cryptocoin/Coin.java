package com.shuffle.cryptocoin;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 *
 * This interface provides service to the Bitcoin (or other) network. This includes queries to the block
 * chain as well as to the p2p network. If these services cannot be provided while the protocol
 * is running, then the protocol must not be run.
 *
 * Created by Daniel Krawisz on 12/5/15.
 *
 */
public interface Coin {
    Transaction shuffleTransaction(long ν, List<Address> inputs, Queue<Address> shuffledOutputs, Map<VerificationKey, Address> changeOutputs);
    void send(Transaction t) throws CoinNetworkError;

    long valueHeld(Address addr) throws BlockchainError, MempoolError;

    // Returns either a transaction that sent from the given address that caused it to have .
    // insufficient funds or a transaction that sent to a given address that caused it to have
    // insufficient funds.
    Transaction getOffendingTransaction(Address addr, long ν);

    // Whether the given transaction spends the funds in the given address.
    boolean isOffendingTransaction(Address addr, Transaction t);
}
