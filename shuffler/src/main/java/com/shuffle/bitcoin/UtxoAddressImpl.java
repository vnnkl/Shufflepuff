package com.shuffle.bitcoin.impl;

import com.shuffle.protocol.FormatException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.TransactionOutPoint;

import java.util.ArrayList;

/**
 * Created by conta on 03.11.16.
 */

public class UtxoAddressImpl extends AddressImpl {

    ArrayList<TransactionOutPoint> transactionOutPoints = new ArrayList<TransactionOutPoint>();

    public UtxoAddressImpl(Address address, ArrayList<TransactionOutPoint> outPoints) {
        super(address);
        this.transactionOutPoints.addAll(outPoints);
    }

    public UtxoAddressImpl(String address, ArrayList<TransactionOutPoint> outPoints) throws FormatException {
        super(address);
        this.transactionOutPoints.addAll(outPoints);
    }

    public ArrayList<TransactionOutPoint> getTransactionOutPoints() {
        return transactionOutPoints;
    }
}