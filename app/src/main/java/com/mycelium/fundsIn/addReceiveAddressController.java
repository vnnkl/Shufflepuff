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
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;

public class addReceiveAddressController {
    public Button AddBtn;
    public Button cancelBtn;
    public TextField inputPrivKEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> privKeyList;
    public ListView privKeyListView;
    public Main.OverlayUI overlayUI;


    // Called by FXMLLoader
    public void initialize() {
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
            String newInput = inputPrivKEdit.getText();
            privKeyList.add(newInput);



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

    private void askForPasswordAndRetry() {
        /**Main.OverlayUI<WalletPasswordController> pwd = Main.instance.overlayUI("wallet_password.fxml");
        final String addressStr = address.getText();
        final String amountStr = amountEdit.getText();
        pwd.controller.aesKeyProperty().addListener((observable, old, cur) -> {
            // We only get here if the user found the right password. If they don't or they cancel, we end up back on
            // the main UI screen. By now the send money screen is history so we must recreate it.
            checkGuiThread();
            Main.OverlayUI<addUTXOController> screen = Main.instance.overlayUI("send_money.fxml");
            screen.controller.aesKey = cur;
            screen.controller.address.setText(addressStr);
            screen.controller.amountEdit.setText(amountStr);
            screen.controller.send(null);
        });**/
    }


    public void next(ActionEvent actionEvent) {

    }
}
