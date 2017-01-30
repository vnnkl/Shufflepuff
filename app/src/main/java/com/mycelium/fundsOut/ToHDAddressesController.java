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

package com.mycelium.fundsOut;

import com.google.common.collect.ImmutableList;
import com.mycelium.Main;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ViewController("shuffle_toHDAddresses.fxml")
public class ToHDAddressesController {
    public Button AddBtn;
    @FXML @BackAction public Button backBtn;
    public TextField inputPrivKEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> privKeyList;
    public ListView privKeyListView;
    public Main.OverlayUI overlayUI;
    public Button nextBtn;
    public Label extAddressLabel1;
    public Label extAddressLabel2;
    public Label extAddressLabel3;
    public Label extAddressLabel4;

    private DeterministicKey key1;
    private DeterministicKey key2;
    private DeterministicKey key3;
    private DeterministicKey key4;

    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    List<Label> labelList;
    List<String> extAddressList = new ArrayList<String>();
    // Called by FXMLLoader
    public void initialize() {
        // fetch internal wallets next unused addresses here and display them to the user
        key1 = Main.bitcoin.wallet().currentReceiveKey();
        key2 = makeNextKey(key1);
        key3 = makeNextKey(key2);
        key4 = makeNextKey(key3);
        List<DeterministicKey> keyList = new ArrayList<DeterministicKey>();
        keyList.add(key1);
        keyList.add(key2);
        keyList.add(key3);
        keyList.add(key4);

        labelList = new ArrayList<Label>();
        labelList.add(extAddressLabel1);
        labelList.add(extAddressLabel2);
        labelList.add(extAddressLabel3);
        labelList.add(extAddressLabel4);

        for (int i=0; i<labelList.size();i++) {
                labelList.get(i).setText(keyList.get(i).toAddress(Main.bitcoin.params()).toBase58());
                extAddressList.add(keyList.get(i).toAddress(Main.bitcoin.params()).toBase58());
        }

    }

    private DeterministicKey makeNextKey(DeterministicKey key){
        ImmutableList<ChildNumber> immutableList = key.getPath();
        List<ChildNumber> das = new LinkedList<ChildNumber>();
        // dasd = new ImmutableList.Builder<ChildNumber>()

        for (int i = 0; i<= immutableList.size()-2;i++){
            das.add(immutableList.get(i));
        }
        das.add(new ChildNumber(immutableList.get(immutableList.size()-1).getI()+1));
        ImmutableList<ChildNumber> newKey = new ImmutableList.Builder<ChildNumber>().addAll(das).build();
        return Main.bitcoin.wallet().getKeyByPath(newKey);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void next(ActionEvent actionEvent) {
        applicationContext.register("outAddresses",extAddressList);
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("connectOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
