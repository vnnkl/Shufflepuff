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
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;

public class shuffleStartController {
    public Button AddBtn;
    public Button cancelBtn;
    public TextField inputHashEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> inputList;
    public ListView inputListView;
    public Main.OverlayUI overlayUI;

    // Called by FXMLLoader
    public void initialize() {
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
            String newInput = inputHashEdit.getText()+":"+inputIndexEdit.getText();
            inputList.add(newInput);



            /**SendRequest req;
            if (amount.equals(Main.bitcoin.wallet().getBalance()))
                req = SendRequest.emptyWallet(destination);

            else
                req = SendRequest.to(destination, amount);
            req.aesKey = aesKey;
            sendResult = Main.bitcoin.wallet().sendCoins(req);
            Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
                @Override
                public void onSuccess(@Nullable Transaction result) {
                    checkGuiThread();
                    overlayUI.done();
                }

                @Override
                public void onFailure(Throwable t) {
                    // We died trying to empty the wallet.
                    crashAlert(t);
                }
            });
            sendResult.tx.getConfidence().addEventListener((tx, reason) -> {
                if (reason == TransactionConfidence.Listener.ChangeReason.SEEN_PEERS)
                    updateTitleForBroadcast();
            });
            AddBtn.setDisable(true);
            address.setDisable(true);
            ((HBox)amountEdit.getParent()).getChildren().remove(amountEdit);
            ((HBox)btcLabel.getParent()).getChildren().remove(btcLabel);
            updateTitleForBroadcast();
             **/

    }


    public void next(ActionEvent actionEvent) {

        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("fundsOut/shuffle_toMasterPub.fxml");
    }
}
