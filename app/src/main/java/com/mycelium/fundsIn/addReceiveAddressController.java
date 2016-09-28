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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.glxn.qrgen.image.ImageType;

import java.io.ByteArrayInputStream;


public class addReceiveAddressController {
    @FXML private Button AddBtn;
    @FXML private Button cancelBtn;
    public Main.OverlayUI overlayUI;
    @FXML private ImageView qrReceiveCode;
    @FXML private Label qrReceiveCodeLabel;
    @FXML private ImageView qrReceiveCode1;
    @FXML private Label qrReceiveCode1Label;
    @FXML private ImageView qrReceiveCode2;
    @FXML private Label qrReceiveCode2Label;
    @FXML private ImageView qrReceiveCode3;
    @FXML private Label qrReceiveCode3Label;



    // Called by FXMLLoader
    public void initialize() {

        String address = "1BitcoinEaterAddressDontSendf59kuE";

        final byte[] imageBytes = net.glxn.qrgen.QRCode
                .from(address)
                .withSize(200, 200)
                .to(ImageType.PNG)
                .stream()
                .toByteArray();
        Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
        this.qrReceiveCode.setImage(qrImage);
        this.qrReceiveCodeLabel.setText(address);
        this.qrReceiveCode1.setImage(qrImage);
        this.qrReceiveCode1Label.setText(address);
        this.qrReceiveCode2.setImage(qrImage);
        this.qrReceiveCode2Label.setText(address);
        this.qrReceiveCode3.setImage(qrImage);
        this.qrReceiveCode3Label.setText(address);
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still





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
