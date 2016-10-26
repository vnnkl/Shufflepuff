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

import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.injection.scopes.ApplicationScoped;
import io.datafx.controller.injection.scopes.FlowScoped;
import io.datafx.controller.util.VetoException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;

@FlowScoped
@ViewController("shuffle_start.fxml")
public class ShuffleStartController {
    @FXML private Button AddBtn;
    @FXML @ActionTrigger("cancel") private Button cancelBtn;
    @ViewNode
    private Button nextBtn;
    public Main.OverlayUI overlayUI;
    @FXML @ViewNode private RadioButton fundsInReceiveAddress;
    @FXML @ViewNode private RadioButton fundsInPrivKeyWIF;
    @FXML @ViewNode private RadioButton fundsInMasterPrivKey;
    @FXML @ViewNode private RadioButton fundsInUTXOs;
    @FXML @ViewNode private RadioButton fundsOutInternalHD;
    @FXML @ViewNode private RadioButton fundsOutExtAddresses;
    @FXML @ViewNode private RadioButton fundsOutXPub;
    @FXML @ViewNode private RadioButton connectByIP;
    @FXML @ViewNode private RadioButton connectByFetch;
    @FXML @ViewNode private ToggleGroup shuffleInOptions;
    @FXML @ViewNode private ToggleGroup shuffleOutOptions;
    @FXML @ViewNode private ToggleGroup shuffleConnectOptions;

    @ActionHandler
    FlowActionHandler flowActionHandler;



    @PostConstruct
    public void initialize() {
        // most is injected by fxml already
        // setUserData for button selection FundsIn
        fundsInReceiveAddress.setUserData(com.mycelium.fundsIn.addReceiveAddressController.class);
        fundsInPrivKeyWIF.setUserData(com.mycelium.fundsIn.addPrivKeyinWIFController.class);
        fundsInMasterPrivKey.setUserData(com.mycelium.fundsIn.addMasterPrivController.class);
        fundsInUTXOs.setUserData(com.mycelium.fundsIn.addUTXOController.class);

        // setUserData for button selection FundsOut
        fundsOutInternalHD.setUserData(com.mycelium.fundsOut.toHDAddressesController.class);
        fundsOutExtAddresses.setUserData(com.mycelium.fundsOut.toExtAddressController.class);
        fundsOutXPub.setUserData(com.mycelium.fundsOut.toMasterPubController.class);

        // setUserData for button selection connectOptions
        connectByIP.setUserData("connectByIP");
        connectByFetch.setUserData("connectByFetch");


    }

    @ActionMethod("cancel")
    public void cancel(ActionEvent event) {
        try {
            flowActionHandler.navigate(MainController.class);
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }

    public Class getFundsInClass(){
        return (Class) shuffleInOptions.getSelectedToggle().getUserData();
    }

    public ToggleGroup getFundsOutGroup(){
        return shuffleOutOptions;
    }


    public void goToFundsOut(){
        try {
            flowActionHandler.navigate((Class<? extends Object>) shuffleOutOptions.getSelectedToggle().getUserData());
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
    public boolean isConnectingManual(){
        if (shuffleConnectOptions.getSelectedToggle().getUserData() == "connectByIP"){
            return true;
        }
        else {
            return false;
        }
    }

    @ActionMethod("next")
    public void next(ActionEvent actionEvent) {
        // if next is clicked and every group has a selection made
        //todo: setter method for each group
        System.out.println(shuffleInOptions.getSelectedToggle().getUserData().toString());
        String selectedToggle = new String(shuffleInOptions.getSelectedToggle().getUserData().toString());


        /**try {
            flowActionHandler.navigate((Class<?>)shuffleInOptions.getSelectedToggle().getUserData());
        } catch (VetoException e) {
            e.printStackTrace();
        } catch (FlowException e) {
            e.printStackTrace();
        }**/
        //Main.OverlayUI<com.mycelium.fundsIn.addReceiveAddressController> screen = Main.instance.overlayUI("fundsOut/shuffle_toMasterPub.fxml");
        //Main.OverlayUI<com.mycelium.fundsIn.addReceiveAddressController> screen = Main.instance.overlayUI("fundsIn/shuffle_"+selectedToggle+".fxml");
        try {
            flowActionHandler.navigate((Class<? extends Object>) shuffleInOptions.getSelectedToggle().getUserData());
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
