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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
@ViewController("shuffle_toMasterPub.fxml")
public class toMasterPubController {
    public Button AddBtn;
    @FXML @BackAction private Button cancelBtn;
    public TextField inputPrivKEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> privKeyList;
    public ListView privKeyListView;
    public Main.OverlayUI overlayUI;
    public Button nextBtn;
    public TextField inputMasterPubEdit;
    public ListView pubKeyListView;
    public Label pubAddressesListLabel;
    public ProgressIndicator progressIndicator;
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public ArrayList<String> outputList = new ArrayList<String>();

    // Called by FXMLLoader
    public void initialize() {
        pubKeyListView.itemsProperty().bind(listProperty);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addFromPub(ActionEvent event) {
        // get MasterPub, could be invalid still
            String newInput = inputMasterPubEdit.getText();
        // todo: find next unused addresses
        outputList.add(newInput);
        progressIndicator.visibleProperty().setValue(true);
    }

    public void next(ActionEvent actionEvent) {
        progressIndicator.setProgress(13.37);
    }
}
