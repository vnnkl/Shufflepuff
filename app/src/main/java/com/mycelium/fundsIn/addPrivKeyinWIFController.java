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
import io.datafx.controller.flow.action.BackAction;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;

@ViewController("shuffle_addPrivKeyInWIF.fxml")
public class addPrivKeyinWIFController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button cancelBtn;
    @FXML private TextField inputPrivKEdit;
    public ArrayList<String> privKeyList = new ArrayList<String>();
    ListProperty<String> listProperty = new SimpleListProperty<>();
    @FXML private ListView privKeyListView;
    public Main.OverlayUI overlayUI;


    // Called by FXMLLoader
    public void initialize() {
        privKeyListView.itemsProperty().bind(listProperty);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
        // todo: check input for being valid privKey in WIF
            String newInput = inputPrivKEdit.getText();
            if (!privKeyList.contains(newInput)){
                privKeyList.add(newInput);
            }
            listProperty.set(FXCollections.observableArrayList(privKeyList));
    }

    public void next(ActionEvent actionEvent) {

    }
}
