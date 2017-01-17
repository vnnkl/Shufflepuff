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
import io.datafx.controller.flow.action.BackAction;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bitcoinj.core.Address;

import java.util.ArrayList;
@ViewController("shuffle_toExtAddress.fxml")
public class ToExtAddressController {
    public Button AddBtn;
    @FXML @BackAction Button cancelBtn;
    public TextField inputPrivKEdit;
    public ArrayList<String> extAddressList = new ArrayList<String>();
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public Main.OverlayUI overlayUI;
    public Label titleLabel;
    public TextField inputAddressEdit;
    public ListView addressListView;
    public Button nextBtn;


    // Called by FXMLLoader
    public void initialize() {
        addressListView.itemsProperty().bind(listProperty);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addOutput(ActionEvent event) {
        // add Output, could be invalid still
        // todo: check input for being valid address

        String newInput = inputAddressEdit.getText();
        Address address = Address.fromBase58(Main.params,newInput);
            if (!extAddressList.contains(newInput)) {
                extAddressList.add(newInput);
            }

        listProperty.set(FXCollections.observableArrayList(extAddressList));
    }

    public void next(ActionEvent actionEvent) {

    }
}
