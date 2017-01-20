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

import com.google.common.collect.ImmutableList;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import net.glxn.qrgen.image.ImageType;
import org.bitcoinj.core.Address;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.Wallet;

import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.List;

@ViewController("shuffle_addReceiveAddress.fxml")
public class AddReceiveAddressController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button backBtn;
    public Main.OverlayUI overlayUI;
    @FXML private ImageView qrReceiveCode;
    @FXML private Label qrReceiveCodeLabel;
    @FXML private ImageView qrReceiveCode1;
    @FXML private Label qrReceiveCode1Label;
    @FXML private ImageView qrReceiveCode2;
    @FXML private Label qrReceiveCode2Label;
    @FXML private ImageView qrReceiveCode3;
    @FXML private Label qrReceiveCode3Label;
    @FXML private Text explanation1;
    @FXML private Text explanation2;

    Wallet wallet = Main.bitcoin.wallet();

    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    public Image stringToQR(String address){
        final byte[] imageBytes = net.glxn.qrgen.QRCode
                .from(address)
                .withSize(200, 200)
                .to(ImageType.PNG)
                .stream()
                .toByteArray();
        Image qrImage = new Image(new ByteArrayInputStream(imageBytes));
        return qrImage;
    }

    // Called by FXMLLoader
    public void initialize() {

        DeterministicKey currentReceiveKey = wallet.currentReceiveKey();
        DeterministicKey firstKeyAfter = makeNextKey(currentReceiveKey);
        DeterministicKey secondKeyAfter = makeNextKey(firstKeyAfter);
        DeterministicKey thirdKeyAfter  = makeNextKey(secondKeyAfter);

        Address currentAddress = currentReceiveKey.toAddress(wallet.getParams());
        ImmutableList<ChildNumber> childNumber =  currentReceiveKey.getPath();

        System.out.println(currentReceiveKey.getPathAsString());
        System.out.println(firstKeyAfter.getPathAsString());


        //ImmutableList<ChildNumber> dsas = new ImmutableList.Builder<ChildNumber>().add()


        this.qrReceiveCode.setImage(stringToQR(currentAddress.toBase58()));
        this.qrReceiveCodeLabel.setText(currentAddress.toBase58());
        this.qrReceiveCode1.setImage(stringToQR(firstKeyAfter.toAddress(wallet.getParams()).toBase58()));
        this.qrReceiveCode1Label.setText(firstKeyAfter.toAddress(wallet.getParams()).toBase58());
        this.qrReceiveCode2.setImage(stringToQR(firstKeyAfter.toAddress(wallet.getParams()).toBase58()));
        this.qrReceiveCode2Label.setText(secondKeyAfter.toAddress(wallet.getParams()).toBase58());
        this.qrReceiveCode3.setImage(stringToQR(thirdKeyAfter.toAddress(wallet.getParams()).toBase58()));
        this.qrReceiveCode3Label.setText(thirdKeyAfter.toAddress(wallet.getParams()).toBase58());

        explanation2.setText(explanation2.getText() + wallet.currentKey(KeyChain.KeyPurpose.RECEIVE_FUNDS).getPath());

    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    private DeterministicKey makeNextKey(DeterministicKey key){
        ImmutableList<ChildNumber> immutableList = key.getPath();
        List<ChildNumber> das = new LinkedList<ChildNumber>();
        // dasd = new ImmutableList.Builder<ChildNumber>()

        for (int i = 0; i<= immutableList.size()-2;i++){
            das.add(immutableList.get(i));
        }
        das.add(new ChildNumber(immutableList.get(immutableList.size()-1).getI()+1));
        ImmutableList<ChildNumber> newKey = new ImmutableList.Builder<ChildNumber>().addAll(das).build();
        return wallet.getKeyByPath(newKey);
    }

    public void next(ActionEvent actionEvent) {
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("outOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }

}
