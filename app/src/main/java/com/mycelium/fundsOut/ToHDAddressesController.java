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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.bitcoinj.wallet.KeyChain;

import java.util.ArrayList;
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
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    // Called by FXMLLoader
    public void initialize() {
        // fetch internal wallets next unused addresses here and display them to the user
        List<Label> labelList = new ArrayList<Label>();
        labelList.add(extAddressLabel1);
        labelList.add(extAddressLabel2);
        labelList.add(extAddressLabel3);
        labelList.add(extAddressLabel4);

        for (Label label :
                labelList) {
            label.setText(Main.bitcoin.wallet().freshKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).toAddress(Main.bitcoin.params()).toString());
        }

    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void next(ActionEvent actionEvent) {
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("connectOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }
}
