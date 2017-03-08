/*
 * Copyright by the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.fundsIn;

import com.mycelium.Main;
import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.bitcoin.blockchain.BitcoinCore;
import com.shuffle.bitcoin.blockchain.Btcd;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import org.bitcoinj.core.*;
import org.bitcoinj.store.BlockStoreException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ViewController("shuffle_addUTXO.fxml")
public class AddUTXOController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button backBtn;
    @FXML private TextField inputHashEdit;
    @FXML private TextField inputIndexEdit;
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public ArrayList<String> inputList = new ArrayList<String>();
    @FXML private ListView inputListView;
    public Main.OverlayUI overlayUI;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();
    //Btcd btcd;

    // Called by FXMLLoader
    public void initialize() {
        // btcd  = new Btcd(Main.params,"admin","pass");
        if (!((List<String>) applicationContext.getRegisteredObject("UTXOs") == null)) {
            List<String> utxos = (List<String>) applicationContext.getRegisteredObject("UTXOs");
            inputList.addAll(utxos);
            listProperty.setValue((ObservableList<String>) applicationContext.getRegisteredObject("UTXOs"));
        }
        inputListView.itemsProperty().bind(listProperty);
        //allow index to have up to 3 numbers
        DecimalFormat format = new DecimalFormat("#");
        inputIndexEdit.setTextFormatter(new TextFormatter<>(c ->
        {
            if (c.getControlNewText().isEmpty()) {
                return c;
            }

            ParsePosition parsePosition = new ParsePosition(0);
            Object object = format.parse(c.getControlNewText(), parsePosition);

            if (object == null || parsePosition.getIndex() < c.getControlNewText().length()) {
                return null;
            } else {
                return c;
            }
        }));
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        BitcoinCore bitcoinCore = null;
        try {
            bitcoinCore = new BitcoinCore(Main.params,"admin","pass");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (BitcoindException e) {
            e.printStackTrace();
        } catch (CommunicationException e) {
            e.printStackTrace();
        }
        BitcoinCore.TransactionWithConfirmations transactionWithConfirmations;
        String hash = inputHashEdit.getText().toString().replaceAll(" ","");
        int vout = Integer.valueOf(inputIndexEdit.getText());
        try {
            transactionWithConfirmations = bitcoinCore.getTransaction(hash);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (bitcoinCore.isUtxo(hash,vout)){
                System.out.println("is UTXO √");

                TransactionOutPoint outPoint = new TransactionOutPoint(Main.params,Long.valueOf(vout), Sha256Hash.wrap(hash));
                TransactionOutput output = bitcoinCore.getTransaction(outPoint.getHash().toString()).getOutputs().get(vout);
                    Address address = output.getAddressFromP2PKHScript(Main.params);
                    for (String privKey : (List<String>) applicationContext.getRegisteredObject("WIFKeys")){
                        if (address.equals(DumpedPrivateKey.fromBase58(Main.params,privKey).getKey().toAddress(Main.params))){
                            System.out.println("Is our utxo √");
                            String newInput = hash+ ":" + vout;
                            String betterInput = newInput.replaceAll(" ", "");
                            // todo: if one of fields is empty do not paste
                            if (!inputList.contains(betterInput)) {
                                inputList.add(betterInput);
                            }
                            listProperty.set(FXCollections.observableArrayList(inputList));
                        }
                    }


            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BitcoindException e) {
            e.printStackTrace();
        } catch (CommunicationException e) {
            e.printStackTrace();
        }



    }


    public void getKeyFunds(){
        if (applicationContext.getRegisteredObject("nodeOption")=="Bitcoin Core"){
            BitcoinCore bitcoinCore = null;
            try {
                bitcoinCore = new BitcoinCore(Main.params,"admin","pass");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (BitcoindException e) {
                e.printStackTrace();
            } catch (CommunicationException e) {
                e.printStackTrace();
            }

        }else {
            Btcd btcd = null;
            try {
                btcd = new Btcd(Main.params,"admin","pass");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            List<String> keyList = null;

            List<Bitcoin.Transaction> txList = new LinkedList<>();

            for (String address : keyList) {
                try {
                    txList.addAll(btcd.getAddressTransactions(address));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            List<String> utxoList = new LinkedList<String >();
            // go through all txs and find the vouts that sent to address we have
            for (Bitcoin.Transaction tx : txList) {
                try {
                    //go through all outputs
                    for (TransactionOutput output: tx.bitcoinj().getOutputs()){
                        // todo: fix, needs isUTXO

                    }
                } catch (BlockStoreException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(utxoList);
        }

    }
    public void next(ActionEvent actionEvent) {
        applicationContext.register("UTXOs",listProperty.getValue());
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("outOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
