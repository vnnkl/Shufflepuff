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

import com.mycelium.Main;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;

import java.util.ArrayList;
import java.util.List;

@ViewController("shuffle_toExtAddress.fxml")
public class ToExtAddressController {
    public Button AddBtn;
    @FXML @BackAction Button backBtn;
    public TextField inputPrivKEdit;
    public ArrayList<String> extAddressList = new ArrayList<String>();
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public Main.OverlayUI overlayUI;
    public Label titleLabel;
    public TextField inputAddressEdit;
    public ListView addressListView;
    public Button nextBtn;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    // Called by FXMLLoader
    public void initialize() {
        if (!((List<String>) applicationContext.getRegisteredObject("OutAddresses") == null)) {
            listProperty.setValue((ObservableList<String>) applicationContext.getRegisteredObject("outAddresses"));
        }
        addressListView.itemsProperty().bind(listProperty);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addOutput(ActionEvent event) {
        // add Output, could be invalid still
        // todo: check input for being valid address

        String newInput = inputAddressEdit.getText();
        //test address provided
        if (testAddr(newInput)) {
                if (!extAddressList.contains(newInput)) {
                    extAddressList.add(newInput);
                }
                listProperty.set(FXCollections.observableArrayList(extAddressList));
            }
    }

    private boolean testAddr(String text) {
        try {
            Address.fromBase58(Main.bitcoin.params(), text);
            return true;
        } catch (AddressFormatException e) {
            return false;
        }
    }

    public void next(ActionEvent actionEvent) {
        applicationContext.register("outAddresses",extAddressList);
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("connectOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed at navigating to connectOptions ",e);
        }
    }
}
