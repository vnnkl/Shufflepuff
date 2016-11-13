package com.shuffle.bitcoin;

import com.shuffle.p2p.Bytestring;

import java.util.HashSet;

/**
 * Created by nsa on 11/13/16.
 */
public class Signatures extends Bytestring {

    public HashSet<Bytestring> sigs;

    public Signatures(byte[] b, HashSet<Bytestring> sigs) {
        super(b);
        this.sigs = sigs;
    }

}
