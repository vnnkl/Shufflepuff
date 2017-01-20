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
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;
import java.util.List;

@ViewController("shuffle_addPrivKeyInWIF.fxml")
public class AddPrivKeyinWIFController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button backBtn;
    @FXML private Button nextBtn;
    @FXML private TextField inputPrivKEdit;
    public ArrayList<String> privKeyList = new ArrayList<String>();
    public ArrayList<String> addressList = new ArrayList<String>();
    ListProperty<String> keyListProperty = new SimpleListProperty<>();
    ListProperty<String> addressListProperty = new SimpleListProperty<>();
    @FXML private ListView privKeyListView;
    @FXML private ListView addressListView;
    public Main.OverlayUI overlayUI;


    @ActionHandler
    FlowActionHandler flowActionHandler;

    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    // Called by FXMLLoader
    public void initialize() {
        privKeyListView.itemsProperty().bind(keyListProperty);
        addressListView.itemsProperty().bind(addressListProperty);
        if (!((List<String>)applicationContext.getRegisteredObject("WIFKeys")==null)){
            keyListProperty.set(FXCollections.observableArrayList((List<String>)applicationContext.getRegisteredObject("WIFKeys")));
            for (String privkey: keyListProperty.get()) {
                addressList.add(DumpedPrivateKey.fromBase58(Main.bitcoin.params(),privkey).getKey().toAddress(Main.bitcoin.params()).toBase58());
            }
            addressListProperty.set(FXCollections.observableArrayList(addressList));
        }
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
        // todo: check input for being valid privKey in WIF
            String newInput = inputPrivKEdit.getText();

            if (testPrivKey()){
                if (!privKeyList.contains(newInput)){
                    privKeyList.add(newInput);
                    String address = DumpedPrivateKey.fromBase58(Main.bitcoin.params(),newInput).getKey().toAddress(Main.bitcoin.params()).toBase58();
                    if (!addressList.contains(address)){
                        addressList.add(address);
                    }
                }
                keyListProperty.set(FXCollections.observableArrayList(privKeyList));
                addressListProperty.set(FXCollections.observableArrayList(addressList));
            }

    }

   private boolean testPrivKey(){
            try {
                if (inputPrivKEdit.getText().isEmpty()){
                    return false;
                }
                System.out.println(ECKey.fromPrivate(Base58.decodeChecked(inputPrivKEdit.getText()),true));
            }
            catch (AddressFormatException e){
                return false;
            }
        return true;
   }

    private List<String> getKeys(){
        return keyListProperty.get();
    }

    @ActionMethod("next")
    public void next(ActionEvent actionEvent) {
        applicationContext.register("WIFKeys",getKeys());
        try {
            flowActionHandler.navigate(AddUTXOController.class);
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
