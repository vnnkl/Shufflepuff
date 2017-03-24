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

import com.msopentech.thali.java.toronionproxy.JavaOnionProxyContext;
import com.msopentech.thali.java.toronionproxy.JavaOnionProxyManager;
import com.msopentech.thali.toronionproxy.OnionProxyManager;
import com.msopentech.thali.toronionproxy.Utilities;
import com.mycelium.Main;
import com.mycelium.utils.GuiUtils;
import com.shuffle.bitcoin.blockchain.Bitcoin;
import com.shuffle.bitcoin.blockchain.Btcd;
import com.shuffle.bitcoin.impl.AddressImpl;
import com.shuffle.protocol.FormatException;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.*;
import org.bitcoinj.store.BlockStoreException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

@ViewController("shuffle_addPrivKeyInWIF.fxml")
public class AddPrivKeyinWIFController {
    @FXML
    private Button AddBtn;
    @FXML
    @BackAction
    private Button backBtn;
    @FXML private Button rmvBtn;
    @FXML
    private Button nextBtn;
    @FXML
    private TextField inputPrivKEdit;
    public ArrayList<String> privKeyList = new ArrayList<String>();
    public ArrayList<String> addressList = new ArrayList<String>();
    ListProperty<String> keyListProperty = new SimpleListProperty<>();
    ListProperty<String> addressListProperty = new SimpleListProperty<>();
    @FXML
    private ListView privKeyListView;
    @FXML
    private ListView addressListView;
    public Main.OverlayUI overlayUI;
    Logger logger = LogManager.getLogger();

    @ActionHandler
    FlowActionHandler flowActionHandler;

    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    // Called by FXMLLoader
    public void initialize() {
        privKeyListView.itemsProperty().bind(keyListProperty);
        addressListView.itemsProperty().bind(addressListProperty);
        if (!((List<String>) applicationContext.getRegisteredObject("WIFKeys") == null)) {
            if (!(((List<String>) applicationContext.getRegisteredObject("WIFKeys")).get(0).equals(Main.bitcoin.wallet().currentReceiveKey().getPrivateKeyAsWiF(Main.params)))) {

                List<String> keyList = (List<String>) applicationContext.getRegisteredObject("WIFKeys");
                privKeyList.addAll(keyList);
                keyListProperty.set(FXCollections.observableArrayList(privKeyList));
                for (String privkey : keyListProperty.get()) {
                    addressList.add(DumpedPrivateKey.fromBase58(Main.bitcoin.params(), privkey).getKey().toAddress(Main.bitcoin.params()).toBase58());
                }
                addressListProperty.set(FXCollections.observableArrayList(addressList));
            }
        }
    }

    public void getAddressTransactions(AddressImpl address) {
        final int TOTAL_SECONDS_PER_TOR_STARTUP = 4 * 60;
        final int TOTAL_TRIES_PER_TOR_STARTUP = 5;
        final int WAIT_FOR_HIDDEN_SERVICE_MINUTES = 3;


        int localport = 6996, hiddenServicePort = 6997;
        OnionProxyManager proxyManager = new JavaOnionProxyManager(new JavaOnionProxyContext(new File("tor-files")));
        OnionProxyManager clientManager = new JavaOnionProxyManager(new JavaOnionProxyContext(new File("tor-clientfiles")));
        try {
            proxyManager.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP);
            clientManager.startWithRepeat(TOTAL_SECONDS_PER_TOR_STARTUP, TOTAL_TRIES_PER_TOR_STARTUP);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String onionAddress;
        if (proxyManager.isBootstrapped()) {
            System.out.println("Ist bootstrapped");
            try {
                onionAddress = runHiddenServiceTest(proxyManager, clientManager);

                System.out.println("onion sent message: " + onionAddress);


            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public enum ServerState {SUCCESS, TIMEDOUT, OTHERERROR}


    private String runHiddenServiceTest(OnionProxyManager hiddenServiceManager, OnionProxyManager clientManager)
            throws IOException, InterruptedException {
        int localPort = 9343;
        int hiddenServicePort = 9344;
        String onionAddress = hiddenServiceManager.publishHiddenService(hiddenServicePort, localPort);
        System.out.println("onionAddress for test hidden service is: " + onionAddress);

        byte[] testBytes = new byte[]{0x01, 0x02, 0x03, 0x05};

        long timeToExit = Calendar.getInstance().getTimeInMillis() + 1 * 60 * 1000;
        while (Calendar.getInstance().getTimeInMillis() < timeToExit) {
            SynchronousQueue<ServerState> serverQueue = new SynchronousQueue<ServerState>();
            Thread serverThread = receiveExpectedBytes(testBytes, localPort, serverQueue);

            Socket clientSocket =
                    getClientSocket(onionAddress, hiddenServicePort, clientManager.getIPv4LocalHostSocksPort());

            DataOutputStream clientOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientOutputStream.write(testBytes);
            clientOutputStream.flush();
            ServerState serverState = serverQueue.poll(1, TimeUnit.MINUTES);
            if (serverState == ServerState.SUCCESS) {
                return onionAddress;
            } else {
                long timeForThreadToExit = Calendar.getInstance().getTimeInMillis() + 1000;
                while (Calendar.getInstance().getTimeInMillis() < timeForThreadToExit &&
                        serverThread.getState() != Thread.State.TERMINATED) {
                    Thread.sleep(1000, 0);
                }
                if (serverThread.getState() != Thread.State.TERMINATED) {
                    throw new RuntimeException("Server thread doesn't want to terminate and free up our port!");
                }
            }
        }
        throw new RuntimeException("Test timed out!");
    }

    private Thread receiveExpectedBytes(final byte[] expectedBytes, int localPort,
                                        final SynchronousQueue<ServerState> serverQueue) throws IOException {
        final ServerSocket serverSocket = new ServerSocket(localPort);

        Thread thread = new Thread(new Runnable() {
            public void run() {
                Socket receivedSocket = null;
                try {
                    receivedSocket = serverSocket.accept();
                    // Yes, setTcpNoDelay is useless because we are just reading but I'm being paranoid
                    receivedSocket.setTcpNoDelay(true);
                    receivedSocket.setSoTimeout(10 * 1000);
                    System.out.println("Received incoming connection" + receivedSocket.getInetAddress() + ":" + receivedSocket.getPort());
                    DataInputStream dataInputStream = new DataInputStream(receivedSocket.getInputStream());
                    for (byte nextByte : expectedBytes) {
                        byte receivedByte = dataInputStream.readByte();
                        if (nextByte != receivedByte) {
                            System.out.println("Received " + receivedByte + ", but expected " + nextByte);
                            serverQueue.put(ServerState.OTHERERROR);
                            return;
                        } else {
                            System.out.println("Received " + receivedByte);
                        }
                    }
                    System.out.println("All Bytes Successfully Received!");
                    serverQueue.put(ServerState.SUCCESS);
                } catch (IOException e) {
                    logger.warn("We got an io exception waiting for the server bytes, this really shouldn't happen, but does.", e);
                    try {
                        serverQueue.put(ServerState.TIMEDOUT);
                    } catch (InterruptedException e1) {
                        logger.error("We couldn't send notice that we had a server time out! EEEK!");
                    }
                } catch (InterruptedException e) {
                    logger.error("Test Failed", e);
                    try {
                        serverQueue.put(ServerState.OTHERERROR);
                    } catch (InterruptedException e1) {
                        logger.error("We got an InterruptedException and couldn't tell the server queue about it!", e1);
                    }
                } finally {
                    // I suddenly am getting IncompatibleClassChangeError: interface no implemented when
                    // calling these functions. I saw a random Internet claim (therefore it must be true!)
                    // that closeable is only supported on sockets in API 19 but I'm compiling with 19 (although
                    // targeting 18). To make things even weirder, this test passed before!!! I am soooo confused.
                    try {
                        if (receivedSocket != null) {
                            receivedSocket.close();
                            System.out.println("Close of receiveExpectedBytes worked");
                        }
                        serverSocket.close();
                        System.out.println("Server socket is closed");
                    } catch (IOException e) {
                        logger.error("Close failed!", e);
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    /**
     * It can take awhile for a new hidden service to get registered
     *
     * @param onionAddress      DNS style hidden service address of from x.onion
     * @param hiddenServicePort Hidden service's port
     * @param socksPort         Socks port that Tor OP is listening on
     * @return A socket via Tor that is connected to the hidden service
     */
    private Socket getClientSocket(String onionAddress, int hiddenServicePort, int socksPort)
            throws InterruptedException {
        long timeToExit = Calendar.getInstance().getTimeInMillis() + 3 * 60 * 1000;
        Socket clientSocket = null;
        while (Calendar.getInstance().getTimeInMillis() < timeToExit && clientSocket == null) {
            try {
                clientSocket = Utilities.socks4aSocketConnection(onionAddress, hiddenServicePort, "127.0.0.1", socksPort);
                clientSocket.setTcpNoDelay(true);
                logger.info("We connected via the clientSocket to try and talk to the hidden service.");
            } catch (IOException e) {
                logger.error("attempt to set clientSocket failed, will retry", e);
                Thread.sleep(5000, 0);
            }
        }

        if (clientSocket == null) {
            throw new RuntimeException("Could not set clientSocket");
        }

        return clientSocket;
    }


    public void getKeyUTXOs(){
        if (applicationContext.getRegisteredObject("nodeOption")=="Bitcoin Core"){

        }else {
            Btcd btcd = null;

            if (applicationContext.getRegisteredObject("nodeUser") == (null)) {
                String nodeIP = (String) applicationContext.getRegisteredObject("nodeIP");
                String nodeUser = (String) applicationContext.getRegisteredObject("nodeUser");
                String nodePw = (String) applicationContext.getRegisteredObject("nodePW");
                try {
                    btcd = new Btcd(Main.params,nodeIP,nodeUser,nodePw);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    btcd = new Btcd(Main.params,"admin","pass");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }

            List<String> addressList = this.addressList;
            List<Bitcoin.Transaction> txList = new LinkedList<>();

            for (String address : addressList) {
                try {
                    txList.addAll(btcd.getAddressTransactions(address));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            List<String> utxoList = new LinkedList<String >();
            // go through all txs and find the vouts that sent to address we have
            for (Bitcoin.Transaction tx : txList) {
                try {
                    //go through all outputs
                    for (TransactionOutput output: tx.bitcoinj().getOutputs()){
                        if (this.addressList.contains(output.getAddressFromP2PKHScript(Main.params).toString())){
                            if (btcd.isUtxo(tx.bitcoinj().getHashAsString(),output.getIndex())){
                                utxoList.add(tx.bitcoinj().getHashAsString()+":" + output.getIndex());
                            }
                        }
                    }
                } catch (BlockStoreException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            applicationContext.register("UTXOs",utxoList);
        }

    }

    public void removeLastPrivKey(ActionEvent event){
        if (!(privKeyList.isEmpty())) {
            privKeyList.remove(privKeyList.size()-1);
            addressList.remove(addressList.size() - 1);
            addressListProperty.setValue(FXCollections.observableArrayList(addressList));
            keyListProperty.setValue(FXCollections.observableArrayList(privKeyList));
        }
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
        // todo: check input for being valid privKey in WIF

        try {
            String newInput = inputPrivKEdit.getText();
            if (testPrivKey()) {
                if (!privKeyList.contains(newInput)) {
                    privKeyList.add(newInput);
                    String address = null;

                    address = DumpedPrivateKey.fromBase58(Main.bitcoin.params(), newInput).getKey().toAddress(Main.bitcoin.params()).toBase58();

                    if (!addressList.contains(address)) {
                        addressList.add(address);
                    }
                }
                keyListProperty.set(FXCollections.observableArrayList(privKeyList));
                addressListProperty.set(FXCollections.observableArrayList(addressList));
            }
        } catch (WrongNetworkException e) {
            GuiUtils.informationalAlert("wrong network", "Address has been of wrong Network, mixing Mainnet and Testnet up?");
            inputPrivKEdit.setText("");
        }
    }

    private boolean testPrivKey() {
        try {
            if (inputPrivKEdit.getText().isEmpty()) {
                return false;
            }
            System.out.println(ECKey.fromPrivate(Base58.decodeChecked(inputPrivKEdit.getText()), true));
        } catch (AddressFormatException e) {
            return false;
        }
        return true;
    }

    private List<String> getKeys() {
        return keyListProperty.get();
    }

    @ActionMethod("next")
    public void next(ActionEvent actionEvent) {
        if (!(privKeyList.isEmpty())) {
            applicationContext.register("WIFKeys", getKeys());
            applicationContext.register("changeAddress", addressList);
            try {
                getAddressTransactions(new AddressImpl(addressList.get(0).toString()));
            } catch (FormatException e) {
                e.printStackTrace();
            }
            getKeyUTXOs();
            try {
                if (applicationContext.getRegisteredObject("nodeOption").equals("Bitcoin Core")) {
                    flowActionHandler.navigate(AddUTXOController.class);
                } else {
                    flowActionHandler.navigate((Class<? extends Object>) applicationContext.getRegisteredObject("outOption"));
                }
            } catch (VetoException | FlowException e) {
                e.printStackTrace();
            }

        }
    }
}
