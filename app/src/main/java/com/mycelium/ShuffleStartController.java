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


import com.mycelium.connect.FetchConnectController;
import com.mycelium.connect.ManualConnectController;
import com.mycelium.fundsIn.AddMasterPrivController;
import com.mycelium.fundsIn.AddPrivKeyinWIFController;
import com.mycelium.fundsIn.AddReceiveAddressController;
import com.mycelium.fundsOut.ToExtAddressController;
import com.mycelium.fundsOut.ToHDAddressesController;
import com.mycelium.fundsOut.ToMasterPubController;
import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.injection.scopes.FlowScoped;
import io.datafx.controller.util.VetoException;
import io.datafx.eventsystem.OnEvent;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

@FlowScoped
@ViewController("shuffle_start.fxml")
public class ShuffleStartController {
    @FXML
    private Button AddBtn;
    @FXML
    @ActionTrigger("cancel")
    private Button cancelBtn;
    @ViewNode
    private Button nextBtn;
    public Main.OverlayUI overlayUI;
    @FXML
    @ViewNode
    private RadioButton fundsInReceiveAddress;
    @FXML
    @ViewNode
    private RadioButton fundsInPrivKeyWIF;
    @FXML
    @ViewNode
    private RadioButton fundsInMasterPrivKey;
    @FXML
    @ViewNode
    private RadioButton fundsInUTXOs;
    @FXML
    @ViewNode
    private RadioButton fundsOutInternalHD;
    @FXML
    @ViewNode
    private RadioButton fundsOutExtAddresses;
    @FXML
    @ViewNode
    private RadioButton fundsOutXPub;
    @FXML
    @ViewNode
    private RadioButton connectByIP;
    @FXML
    @ViewNode
    private RadioButton connectByFetch;
    @FXML
    @ViewNode
    private ToggleGroup shuffleInOptions;
    @FXML
    @ViewNode
    private ToggleGroup shuffleOutOptions;
    @FXML
    @ViewNode
    private ToggleGroup shuffleConnectOptions;

    @FXML
    private ChoiceBox<String> nodeBox;

    public ShuffleStartController() {
    }

    public List<String> getKeyList() {
        return keyList;
    }

    public List<String> getUtxoList() {
        return utxoList;
    }

    private List<String> keyList = new LinkedList<String>();
    private List<String> utxoList = new LinkedList<String>();

    @OnEvent("setkeyList")
    public boolean setKeyList(List<String> keyList){
        this.keyList = keyList;
        if (this.keyList.equals(keyList)){
            return true;
        }
        else {
            return false;
        }
    }


    @OnEvent("setUtxoList")
    private boolean setUtxoList(List<String> utxoList){
        this.utxoList = utxoList;
        if (this.utxoList.equals(utxoList)){
            return true;
        }
        else {
            return false;
        }
    }

    @ActionHandler
    FlowActionHandler flowActionHandler;

    @FXMLApplicationContext
    private static ApplicationContext applicationContext = ApplicationContext.getInstance();

    @PostConstruct
    public void initialize() {
        nodeBox.setItems(FXCollections.observableArrayList("BTCD","Bitcoin Core"));
        nodeBox.setValue("BTCD");
        // most is injected by fxml already
        // setUserData for button selection FundsIn
        fundsInReceiveAddress.setUserData(AddReceiveAddressController.class);
        fundsInPrivKeyWIF.setUserData(AddPrivKeyinWIFController.class);
        fundsInMasterPrivKey.setUserData(AddMasterPrivController.class);
        //fundsInUTXOs.setUserData(AddUTXOController.class);

        // setUserData for button selection FundsOut
        fundsOutInternalHD.setUserData(ToHDAddressesController.class);
        fundsOutExtAddresses.setUserData(ToExtAddressController.class);
        fundsOutXPub.setUserData(ToMasterPubController.class);

        // setUserData for button selection connectOptions
        connectByIP.setUserData(ManualConnectController.class);
        connectByFetch.setUserData(FetchConnectController.class);

        setToggleContext("inOption");
        setToggleContext("outOption");
        setToggleContext("connectOption");
        if (!(applicationContext.getRegisteredObject("nodeOption")==null)){
            nodeBox.setValue(applicationContext.getRegisteredObject("nodeOption").toString());
        }

    }

    private void setToggleContext(String key){
        if (!((Class<?>)applicationContext.getRegisteredObject(key)==null)){
            switch (key){
                case "inOption":{
                    for (Toggle toggle: shuffleInOptions.getToggles()) {
                        if(toggle.getUserData().equals(applicationContext.getRegisteredObject(key))){
                            toggle.setSelected(true);
                        }
                    }
                }
                case "outOption":{
                    for (Toggle toggle: shuffleOutOptions.getToggles()) {
                        if(toggle.getUserData().equals(applicationContext.getRegisteredObject(key))){
                            toggle.setSelected(true);
                        }
                    }
                }
                case "connectOption":{
                    for (Toggle toggle: shuffleConnectOptions.getToggles()) {
                        if(toggle.getUserData().equals(applicationContext.getRegisteredObject(key))){
                            toggle.setSelected(true);
                        }
                    }
                }
            }

        }
    }

    @ActionMethod("cancel")
    public void cancel() {
        try {
            flowActionHandler.navigate(MainController.class);
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }


    @ActionMethod("next")
    public void next(ActionEvent actionEvent) {
        applicationContext.register("nodeOption",nodeBox.getValue());
        applicationContext.register("inOption",shuffleInOptions.getSelectedToggle().getUserData());
        applicationContext.register("outOption",shuffleOutOptions.getSelectedToggle().getUserData());
        applicationContext.register("connectOption",shuffleConnectOptions.getSelectedToggle().getUserData());

        // if next is clicked and every group has a selection made
        //todo: setter method for each group
        String selectedToggle = "" + shuffleInOptions.getSelectedToggle().getUserData();
        System.out.println(selectedToggle);

        //Main.OverlayUI<com.mycelium.fundsIn.AddReceiveAddressController> screen = Main.instance.overlayUI("fundsOut/shuffle_toMasterPub.fxml");
        //Main.OverlayUI<com.mycelium.fundsIn.AddReceiveAddressController> screen = Main.instance.overlayUI("fundsIn/shuffle_"+selectedToggle+".fxml");

        try {
            flowActionHandler.navigate((Class<?>) shuffleInOptions.getSelectedToggle().getUserData());
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
