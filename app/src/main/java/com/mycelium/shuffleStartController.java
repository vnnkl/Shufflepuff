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

package com.mycelium;

import javafx.event.ActionEvent;
import javafx.scene.control.*;

import java.util.ArrayList;

public class shuffleStartController {
    public Button AddBtn;
    public Button cancelBtn;
    public TextField inputHashEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> inputList;
    public ListView inputListView;
    public Main.OverlayUI overlayUI;
    public RadioButton fundsInReceiveAddress;
    final ToggleGroup shuffleInOptions = new ToggleGroup();
    // Called by FXMLLoader
    public void initialize() {
        // create Group for FundsIn


        fundsInReceiveAddress = new RadioButton("Send to ShufflePuff");
        fundsInReceiveAddress.setToggleGroup(shuffleInOptions);
        fundsInReceiveAddress.setUserData("addMasterPriv");
        fundsInReceiveAddress.setSelected(true);


        RadioButton fundsInPrivKeyWIF = new RadioButton("Private Keys (WIF)");
        fundsInPrivKeyWIF.setToggleGroup(shuffleInOptions);

        RadioButton fundsInMasterPrivKey = new RadioButton("Master PrivKey");
        fundsInMasterPrivKey.setToggleGroup(shuffleInOptions);

        RadioButton fundsInUTXOs = new RadioButton("UTXOs");
        fundsInUTXOs.setToggleGroup(shuffleInOptions);

        // create Group for FundsOut
        final ToggleGroup fundsOutGroup = new ToggleGroup();

        RadioButton fundsOutInternalHD = new RadioButton("ShufflePuffWallet (HD Seed)");
        fundsOutInternalHD.setToggleGroup(fundsOutGroup);

        RadioButton fundsOutExtAddresses = new RadioButton("external address(es)");
        fundsOutExtAddresses.setToggleGroup(fundsOutGroup);

        RadioButton fundsOutXPub = new RadioButton("xPub");
        fundsOutXPub.setToggleGroup(fundsOutGroup);
        shuffleInOptions.getSelectedToggle().toString();

        final ToggleGroup connectOptions = new ToggleGroup();


    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }


    public void next(ActionEvent actionEvent) {
        // if next is clicked

        Main.OverlayUI<com.mycelium.fundsIn.addReceiveAddressController> screen = Main.instance.overlayUI("fundsIn/shuffle_addReceiveAddress.fxml");
    }
}
