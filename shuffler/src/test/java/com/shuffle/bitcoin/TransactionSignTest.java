package com.shuffle.bitcoin;

import com.shuffle.bitcoin.blockchain.Bitcoin;

import org.bitcoinj.core.*;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * Created by nsa on 8/7/16.
 */
public class TransactionSignTest {

    @Test
    public void testSigning() throws AddressFormatException {
        NetworkParameters netParams = TestNet3Params.get();
        HexBinaryAdapter adapter = new HexBinaryAdapter();
        Context context = Context.getOrCreate(netParams);
        byte[] bx = adapter.unmarshal("0100000001bd5ee90ffe5eedd67c09c3bb348dd7dc1308800eb221b1c92dda651010519ba3010000006a4730440220467868c0b2ed001a915cca5269b928698bee8aba4fe454e1d775070d9e4041cb02205d1c979dbc75e5dc656c4e9d5969d716a383797bd5ad5df79a13d0d6e3f51ccb012102403adb7674f25212bc8cf4a97797154a4980c60e9f328c90300b71a8a04389c7ffffffff024088db60000000001976a914990628d3670f439a5f9e0dfa6492b8bbf3b3fa1b88ac76cf6edd050000001976a914b679378d01ee7203a454bca2ad25698ef23a056388ac00000000");
        org.bitcoinj.core.Transaction testbx = new org.bitcoinj.core.Transaction(netParams, bx);
        org.bitcoinj.core.Transaction tx = new org.bitcoinj.core.Transaction(netParams);
        tx.addOutput(org.bitcoinj.core.Coin.SATOSHI.multiply(testbx.getOutput(0).getValue().value - 50000l), new Address(netParams, "mobDb19geJ66kkQnsSYvN9PNEKNDiNBHEp"));
        System.out.println(testbx.getOutput(0));
        tx.addInput(testbx.getOutput(0));

        String seckey = "3EC95EBFEDCF77373BABA0DE345A0962E51344CD2D0C8DBDF93AEFD0B66BE240";
        byte[] privkey = Hex.decode(seckey);
        ECKey ecPriv = ECKey.fromPrivate(privkey);
        Sha256Hash hash2 = tx.hashForSignature(0, testbx.getOutput(0).getScriptPubKey().getProgram(), Transaction.SigHash.ALL, false);
        ECKey.ECDSASignature ecSig = ecPriv.sign(hash2);
        TransactionSignature txSig = new TransactionSignature(ecSig, Transaction.SigHash.ALL, false);
        Script inputScript = ScriptBuilder.createInputScript(txSig, ECKey.fromPublicOnly(ecPriv.getPubKey()));
        tx.getInput(0).setScriptSig(inputScript);
        String hexBin = DatatypeConverter.printHexBinary(tx.bitcoinSerialize());
        System.out.println(hexBin);
        tx.getInput(0).verify(testbx.getOutput(0));
        // SUCCESSFULLY BROADCAST WOO!

    }

}
