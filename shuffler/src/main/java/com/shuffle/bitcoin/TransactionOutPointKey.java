package com.shuffle.bitcoin;

import org.bitcoinj.core.TransactionOutPoint;

/**
 * Created by nsa on 11/5/16.
 */
public class TransactionOutPointKey {

    private final TransactionOutPoint t;
    private final VerificationKey vk;

    public TransactionOutPointKey(TransactionOutPoint t, VerificationKey vk) {
        this.t = t;
        this.vk = vk;
    }

    public TransactionOutPoint getOutPoint() {
        return t;
    }

    public VerificationKey getKey() {
        return vk;
    }

}
