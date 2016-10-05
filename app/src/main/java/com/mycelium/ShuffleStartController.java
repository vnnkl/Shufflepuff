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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import java.util.ArrayList;

public class ShuffleStartController {
    @FXML private Button AddBtn;
    @FXML private Button cancelBtn;
    public Main.OverlayUI overlayUI;
    @FXML private RadioButton fundsInReceiveAddress;
    @FXML private RadioButton fundsInPrivKeyWIF;
    @FXML private RadioButton fundsInMasterPrivKey;
    @FXML private RadioButton fundsInUTXOs;
    @FXML private RadioButton fundsOutInternalHD;
    @FXML private RadioButton fundsOutExtAddresses;
    @FXML private RadioButton fundsOutXPub;
    @FXML private RadioButton connectByIP;
    @FXML private RadioButton connectByFetch;
    @FXML private ToggleGroup shuffleInOptions;
    @FXML private ToggleGroup shuffleOutOptions;
    @FXML private ToggleGroup shuffleConnectOptions;
    // Called by FXMLLoader

    ArrayList<String> fundsInList = new ArrayList<>();

    public void setFundsInList(ArrayList<String> fundsInList){
        this.fundsInList.addAll(fundsInList);
    }
    public void initialize() {
        // most is injected by fxml already
        // setUserData for button selection FundsIn
        fundsInReceiveAddress.setUserData("addReceiveAddress");
        fundsInPrivKeyWIF.setUserData("addPrivKeyInWIF");
        fundsInMasterPrivKey.setUserData("addMasterPriv");
        fundsInUTXOs.setUserData("addUTXO");

        // setUserData for button selection FundsOut
        fundsOutInternalHD.setUserData("toHDAddresses");
        fundsOutExtAddresses.setUserData("toExtAddress");
        fundsOutXPub.setUserData("toMasterPub");

        // setUserData for button selection connectOptions
        connectByIP.setUserData("connectByIP");
        connectByFetch.setUserData("connectByFetch");



    }


    public void cancel(ActionEvent event) {
        overlayUI.done();
    }


    public void next(ActionEvent actionEvent) {
        // if next is clicked and every group has a selection made
        //todo: setter method for each group
        System.out.println(shuffleInOptions.getSelectedToggle().getUserData().toString());
        String selectedToggle = new String(shuffleInOptions.getSelectedToggle().getUserData().toString());
        //Main.OverlayUI<com.mycelium.fundsIn.addReceiveAddressController> screen = Main.instance.overlayUI("fundsOut/shuffle_toMasterPub.fxml");
         Main.OverlayUI<com.mycelium.fundsIn.addReceiveAddressController> screen = Main.instance.overlayUI("fundsIn/shuffle_"+selectedToggle+".fxml");
    }
}
