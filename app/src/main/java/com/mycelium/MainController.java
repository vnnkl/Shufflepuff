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

import com.mycelium.controls.ClickableBitcoinAddress;
import com.mycelium.controls.NotificationBarPane;
import com.mycelium.utils.BitcoinUIModel;
import com.mycelium.utils.easing.EasingMode;
import com.mycelium.utils.easing.ElasticInterpolator;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.utils.MonetaryFormat;
import org.fxmisc.easybind.EasyBind;

import javax.annotation.PostConstruct;

import static com.mycelium.Main.bitcoin;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
@ViewController("main.fxml")
public class MainController {
    public HBox controlsBox;
    public Label balance;
    public Button sendMoneyOutBtn;
    public ClickableBitcoinAddress addressControl;
    public ListView<Transaction> transactionsList;

    private BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;

    // Called by FXMLLoader.
    @PostConstruct
    public void initialize() {
        addressControl.setOpacity(0.0);
    }

    public void onBitcoinSetup() {
        model.setWallet(bitcoin.wallet());
        addressControl.addressProperty().bind(model.addressProperty());
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), coin -> MonetaryFormat.BTC.noCode().format(coin).toString()));
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        TorClient torClient = Main.bitcoin.peerGroup().getTorClient();
        if (torClient != null) {
            SimpleDoubleProperty torProgress = new SimpleDoubleProperty(-1);
            String torMsg = "Initialising Tor";
            syncItem = Main.instance.notificationBar.pushItem(torMsg, torProgress);
            torClient.addInitializationListener(new TorInitializationListener() {
                @Override
                public void initializationProgress(String message, int percent) {
                    Platform.runLater(() -> {
                        syncItem.label.set(torMsg + ": " + message);
                        torProgress.set(percent / 100.0);
                    });
                }

                @Override
                public void initializationCompleted() {
                    Platform.runLater(() -> {
                        syncItem.cancel();
                        showBitcoinSyncMessage();
                    });
                }
            });
        } else {
            showBitcoinSyncMessage();
        }
        model.syncProgressProperty().addListener(x -> {
            if (model.syncProgressProperty().get() >= 1.0) {
                readyToGoAnimation();
                if (syncItem != null) {
                    syncItem.cancel();
                    syncItem = null;
                }
            } else if (syncItem == null) {
                showBitcoinSyncMessage();
            }
        });
        Bindings.bindContent(transactionsList.getItems(),model.getTransactions());
        transactionsList.setCellFactory(param -> new TextFieldListCell< >(new StringConverter<Transaction>() {
            @Override
            public String toString(Transaction tx) {
                Coin value =  tx.getValue(Main.bitcoin.wallet());
                if (value.isPositive()){
                    return "Incoming payment of "+MonetaryFormat.BTC.format(value);
                }
                if (value.isNegative()){
                    // display value and address for outgoing transactions, as we create them we can get address from script
                    Address address = tx.getOutput(0).getAddressFromP2PKHScript(Main.params);
                    return "Outgoing payment of "+MonetaryFormat.BTC.format(value)+" to "+address;
                }
                return "Payment with id " + tx.getHash();
            }

            @Override
            public Transaction fromString(String string) {
                // user can't edit list so null is fine
                return null;
            }
        }));

    }

    private void showBitcoinSyncMessage() {
        syncItem = Main.instance.notificationBar.pushItem("Synchronising with the Bitcoin network", model.syncProgressProperty());
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money.fxml");
    }

    public void settingsClicked(ActionEvent event) {
        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("wallet_settings.fxml");
        screen.controller.initialize(null);
    }

    public void restoreFromSeedAnimation() {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void readyToGoAnimation() {
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), controlsBox);
        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(1200), addressControl);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        group.setDelay(NotificationBarPane.ANIM_OUT_DURATION);
        group.setCycleCount(1);
        group.play();
    }

    public DownloadProgressTracker progressBarUpdater() {
        return model.getDownloadProgressTracker();
    }

    public void shuffleClicked(ActionEvent actionEvent) throws FlowException {

        Flow shuffleFlow = new Flow(ShuffleStartController.class);
        Scene shuffleScene = new Scene(shuffleFlow.start());
        Main.instance.mainWindow.setScene(shuffleScene);
        Main.instance.mainWindow.show();

        //new Flow(ShuffleStartController.class).startInStage(Main.instance.mainWindow);
        // Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("shuffle_start.fxml");
    }
}
