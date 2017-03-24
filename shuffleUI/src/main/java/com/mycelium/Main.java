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

import com.google.common.util.concurrent.Service;
import com.mycelium.controls.NotificationBarPane;
import com.mycelium.utils.GuiUtils;
import com.mycelium.utils.TextFieldValidator;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Optional;

import static com.mycelium.utils.GuiUtils.*;

public class Main extends Application {
    public static NetworkParameters params = TestNet3Params.get();
    public static final String APP_NAME = "ShufflePuff";
    private static final String WALLET_FILE_NAME = APP_NAME.replaceAll("[^a-zA-Z0-9.-]", "_") + "-"
            + params.getPaymentProtocolId();

    public static WalletAppKit bitcoin;
    public static Main instance;

    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    private StackPane uiStack;
    private Pane mainUI;
    MainController controller;
    NotificationBarPane notificationBar;
    Stage mainStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            realStart(primaryStage);
        } catch (Throwable e) {
            GuiUtils.crashAlert(e);
            throw e;
        }
    }

    private void realStart(Stage mainStage) throws IOException {
        this.mainStage = mainStage;
        instance = this;
        // Show the crash dialog for any exceptions that we don't handle and that hit the main loop.
        GuiUtils.handleCrashesOnThisThread();

        //if (System.getProperty("os.name").toLowerCase().contains("mac")) {
        // We could match the Mac Aqua style here, except that (a) Modena doesn't look that bad, and (b)
        // the date picker widget is kinda broken in AquaFx and I can't be bothered fixing it.
        // AquaFx.style();
        //}

        // Load the GUI. The MainController class will be automagically created and wired up.
        URL location = getClass().getResource("main.fxml");
        FXMLLoader loader = new FXMLLoader(location);
        mainUI = loader.load();
        controller = loader.getController();
        // Configure the window with a StackPane so we can overlay things on top of the main UI, and a
        // NotificationBarPane so we can slide messages and progress bars in from the bottom. Note that
        // ordering of the construction and connection matters here, otherwise we get (harmless) CSS error
        // spew to the logs.
        notificationBar = new NotificationBarPane(mainUI);
        mainStage.setTitle(APP_NAME);
        uiStack = new StackPane();
        Scene scene = new Scene(uiStack);
        TextFieldValidator.configureScene(scene);   // Add CSS that we need.
        scene.getStylesheets().add(getClass().getResource("wallet.css").toString());
        uiStack.getChildren().add(notificationBar);
        mainStage.setScene(scene);

        // Make log output concise.
        BriefLogFormatter.init();
        // Tell bitcoinj to run event handlers on the JavaFX UI thread. This keeps things simple and means
        // we cannot forget to switch threads when adding event handlers. Unfortunately, the DownloadListener
        // we give to the app kit is currently an exception and runs on a library thread. It'll get fixed in
        // a future version.
        Threading.USER_THREAD = Platform::runLater;
        // Create the app kit. It won't do any heavyweight initialization until after we start it.
        setupWalletKit(null);
        if (bitcoin.isChainFileLocked()) {
            informationalAlert("Already running", "This application is already running and cannot be started twice.");
            Platform.exit();
            return;
        }

        mainStage.show();

        WalletSetPasswordController.estimateKeyDerivationTimeMsec();

        bitcoin.addListener(new Service.Listener() {
            @Override
            public void failed(Service.State from, Throwable failure) {
                GuiUtils.crashAlert(failure);
            }
        }, Platform::runLater);

        bitcoin.startAsync();

        scene.getAccelerators().put(KeyCombination.valueOf("Shortcut+F"), () -> bitcoin.peerGroup().getDownloadPeer().close());
    }

    void setupWalletKit(@Nullable DeterministicSeed seed) {
        // If seed is non-null it means we are restoring from backup.
        bitcoin = new WalletAppKit(params, new File("."), WALLET_FILE_NAME) {
            @Override
            protected void onSetupCompleted() {
                // Don't make the user wait for confirmations for now, as the intention is they're sending it
                // their own money!
                bitcoin.wallet().allowSpendingUnconfirmedTransactions();
                Platform.runLater(controller::onBitcoinSetup);
            }
        };
        // Now configure and start the appkit. This will take a second or two - we could show a temporary splash screen
        // or progress widget to keep the user engaged whilst we initialise, but we don't.

        //bitcoin.connectToLocalHost();


        PeerAddress nodeAddress = getNodeAddress();


        PeerAddress[] peerAddresses = new PeerAddress[0];
        if (nodeAddress == null) {
            if (params.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
                try {
                    peerAddresses = new PeerAddress[]{new PeerAddress(Inet4Address.getByName("91.240.141.169"), 8333), new PeerAddress(Inet4Address.getByName("208.12.64.252"), 8333),
                            new PeerAddress(Inet4Address.getByName("84.246.200.122"), 8333), new PeerAddress(Inet4Address.getByName("50.3.72.129"), 8333), new PeerAddress(Inet4Address.getByName("188.65.59.69"), 8333),
                            new PeerAddress(Inet4Address.getByName("193.49.43.219"), 8333), new PeerAddress(Inet4Address.getByName("212.45.19.162"), 8333), new PeerAddress(Inet4Address.getByName("217.64.47.138"), 8333)};
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            if (params.equals(NetworkParameters.fromID(NetworkParameters.ID_TESTNET))) {
                try {
                    peerAddresses = new PeerAddress[]{
                            new PeerAddress(Inet6Address.getByName("[2a00:1098:0:80:1000:25:0:1]"), 18333), new PeerAddress(Inet4Address.getByName("160.16.109.253"), 18333),
                            new PeerAddress(Inet4Address.getByName("160.16.109.253"), 18333), new PeerAddress(Inet4Address.getByName("190.190.150.45"), 18333)};
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }

            bitcoin.setPeerNodes(peerAddresses).setDiscovery(null);
        } else bitcoin.setPeerNodes(nodeAddress).setDiscovery(null);

        //else if (params == TestNet3Params.get()) {
        // As an example!
        //bitcoin.useTor();
        //bitcoin.setDiscovery(new HttpDiscovery(params, URI.create("http://localhost:8080/peers"), ECKey.fromPublicOnly(BaseEncoding.base16().decode("02cba68cfd0679d10b186288b75a59f9132b1b3e222f6332717cb8c4eb2040f940".toUpperCase()))));
        //}
        bitcoin.setDownloadListener(controller.progressBarUpdater())
                .setBlockingStartup(false)
                .setUserAgent(APP_NAME, "1.0");
        if (seed != null)
            bitcoin.restoreWalletFromSeed(seed);
    }



    private PeerAddress getNodeAddress() {
        TextInputDialog dialog = new TextInputDialog("127.0.0.1");
        dialog.setTitle("Do you have your own Bitcoin Full Node?");
        dialog.setHeaderText("You can provide the IP to your BitcoinCore or Btcd server \nand we will connect bitcoinj with it.\nIf you do not have one or do not know what it is, \nuse cancel to use a default.");
        dialog.setContentText("Please enter Nodes IP or URL: \n");
        dialog.setGraphic(new ImageView(this.getClass().getResource("core-logo.png").toString()));

        Optional<String> result = dialog.showAndWait();

        final PeerAddress[] nodeAddress = {null};
        // The Java 8 way to get the response value (with lambda expression).
        result.ifPresent(name -> {
                    try {
                        InetAddress inetAddress = InetAddress.getByName(name);
                        nodeAddress[0] = new PeerAddress(inetAddress, params.getPort());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

        );

        if (!(nodeAddress[0] == null)) {
            getNodeLogin();
        }

        return nodeAddress[0];
    }


    private void getNodeLogin() {

        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Full Node Login");
        dialog.setHeaderText("What are the RPC login details for your Full node?");

        // Set the icon (must be included in the project).
        dialog.setGraphic(new ImageView(this.getClass().getResource("core-logo.png").toString()));

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("RPCUser");
        PasswordField password = new PasswordField();
        password.setPromptText("RPCPass");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {
            System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
            String user = usernamePassword.getKey().toString();
            String pw = usernamePassword.getValue().toString();
            if (!(user.equals(""))) {
                if (!(pw.equals(""))){
                    applicationContext.register("nodeUser", usernamePassword.getKey().toString());
                    applicationContext.register("nodePW", usernamePassword.getValue().toString());
                }
            }
        });

    }


    private Node stopClickPane = new Pane();

    public class OverlayUI<T> {
        Node ui;
        public T controller;

        public OverlayUI(Node ui, T controller) {
            this.ui = ui;
            this.controller = controller;
        }

        void show() {
            checkGuiThread();
            if (currentOverlay == null) {
                uiStack.getChildren().add(stopClickPane);
                uiStack.getChildren().add(ui);
                blurOut(mainUI);
                //darken(mainUI);
                fadeIn(ui);
                zoomIn(ui);
            } else {
                // Do a quick transition between the current overlay and the next.
                // Bug here: we don't pay attention to changes in outsideClickDismisses.
                explodeOut(currentOverlay.ui);
                fadeOutAndRemove(uiStack, currentOverlay.ui);
                uiStack.getChildren().add(ui);
                ui.setOpacity(0d);
                fadeIn(ui, 100);
                zoomIn(ui, 100);
            }
            currentOverlay = this;
        }

        //public void outsideClickDismisses() {
        //    stopClickPane.setOnMouseClicked((ev) -> done());
        //}

        public void done() {
            checkGuiThread();
            if (ui == null) {
                return;  // In the middle of being dismissed and got an extra click.
            }
            explodeOut(ui);
            fadeOutAndRemove(uiStack, ui, stopClickPane);
            blurIn(mainUI);
            //undark(mainUI);
            this.ui = null;
            this.controller = null;
            currentOverlay = null;
        }
    }

    @Nullable
    private OverlayUI currentOverlay;

    public <T> OverlayUI<T> overlayUI(Node node, T controller) {
        checkGuiThread();
        OverlayUI<T> pair = new OverlayUI<>(node, controller);
        // Auto-magically set the overlayUI member, if it's there.
        try {
            controller.getClass().getField("overlayUI").set(controller, pair);
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }
        pair.show();
        return pair;
    }

    /**
     * Loads the FXML file with the given name, blurs out the main UI and puts this one on top.
     */
    public <T> OverlayUI<T> overlayUI(String name) {
        try {
            checkGuiThread();
            // Load the UI from disk.
            URL location = GuiUtils.getResource(name);
            FXMLLoader loader = new FXMLLoader(location);
            Pane ui = loader.load();
            T controller = loader.getController();
            OverlayUI<T> pair = new OverlayUI<>(ui, controller);
            // Auto-magically set the overlayUI member, if it's there.
            try {
                if (controller != null)
                    controller.getClass().getField("overlayUI").set(controller, pair);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
                ignored.printStackTrace();
            }
            pair.show();
            return pair;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Can't happen.
        }
    }

    @Override
    public void stop() throws Exception {
        bitcoin.stopAsync();
        bitcoin.awaitTerminated();
        // Forcibly terminate the JVM because Orchid likes to spew non-daemon threads everywhere.
        Runtime.getRuntime().exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
