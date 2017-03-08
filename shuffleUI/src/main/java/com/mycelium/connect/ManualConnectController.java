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

package com.mycelium.connect;


import com.mycelium.Main;
import com.mycelium.utils.GuiUtils;
import com.shuffle.player.Shuffle;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
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
import javafx.scene.control.TextFormatter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.json.JSONTokener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@ViewController("shuffle_addIPs.fxml")
public class ManualConnectController {
    @FXML
    private Button AddBtn;
    @FXML
    @BackAction
    private Button backBtn;
    @FXML
    private TextField inputHashEdit;
    @FXML
    private TextField inputIndexEdit;
    @FXML
    private TextField inputEKedit;
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public ArrayList<String> inputList = new ArrayList<String>();
    @FXML
    private ListView inputListView;
    public Main.OverlayUI overlayUI;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();
    List<InetAddress> ipList = new LinkedList<InetAddress>();

    InetAddress inetAddress;

    // Called by FXMLLoader
    public void initialize() {
        inputListView.itemsProperty().bind(listProperty);

        DecimalFormat format = new DecimalFormat("#");
        inputIndexEdit.setTextFormatter(new TextFormatter<>(c ->
        {
            if (c.getControlNewText().isEmpty()) {
                return c;
            }

            ParsePosition parsePosition = new ParsePosition(0);
            Object object = format.parse(c.getControlNewText(), parsePosition);

            if (object == null || parsePosition.getIndex() < c.getControlNewText().length()) {
                return null;
            } else {
                return c;
            }
        }));

        inputIndexEdit.setText("6996");


    }

    private String makeShuffleArguments() {
        // get inputList
        List<String> inputs = inputList;

        String arguments, shuffleAmount, sessionName, blockchain, query, nodeOption, nodeUser, nodePW, timeout, time, utxos;

        StringBuilder stringBuilder = new StringBuilder();

        shuffleAmount = ((Coin) applicationContext.getRegisteredObject("shuffleAmount")).toString();
        stringBuilder.append(" --amount " + (Coin) applicationContext.getRegisteredObject("shuffleAmount"));

        sessionName = "shuffle" + shuffleAmount;
        stringBuilder.append(" --session shuffle" + shuffleAmount);

        query = (String) applicationContext.getRegisteredObject("nodeOption");

        if (query.equals("BTCD")) {
            stringBuilder.append(" --query btcd");
        } else {
            stringBuilder.append(" --query bitcoin-core");
        }

        if (Main.params.equals(NetworkParameters.fromID(NetworkParameters.ID_MAINNET))) {
            stringBuilder.append(" --blockchain main");
        } else {
            stringBuilder.append(" --blockchain test");
        }

        // is per byte, we assume 1 in/1out/1change -> 228 bytes
        stringBuilder.append(" --fee " + getRecommendedFee().multiply(228L).toString());

        if (applicationContext.getRegisteredObject("nodeUser") == (null)) {
            stringBuilder.append(" --rpcuser admin");
        } else {
            stringBuilder.append(" --rpcuser " + (String) applicationContext.getRegisteredObject("nodeUser"));
        }
        if (applicationContext.getRegisteredObject("nodePW") == (null)) {
            stringBuilder.append(" --rpcpass pass");
        } else {
            stringBuilder.append(" --rpcpass " + (String) applicationContext.getRegisteredObject("nodePW"));
        }
        if (applicationContext.getRegisteredObject("timeOutTime") == null) {
            timeout = "10000";
            stringBuilder.append(" --timeout 10000");
        } else {
            timeout = (String) applicationContext.getRegisteredObject("timeOutTime");
            stringBuilder.append(" --timeout " + (String) applicationContext.getRegisteredObject("timeOutTime"));
        }

        if (applicationContext.getRegisteredObject("port") == null) {
            stringBuilder.append(" --port 6996");
        } else {
            stringBuilder.append(" --port " + (String) applicationContext.getRegisteredObject("port"));
        }

        if (applicationContext.getRegisteredObject("WIFKeys") == (null)) {
        } else {
            stringBuilder.append(" --key " + ((List<String>) applicationContext.getRegisteredObject("WIFKeys")).toString().replace("]", "").replace("[", ""));
        }

        stringBuilder.append(" --anon " + (String) applicationContext.getRegisteredObject("outAddresses").toString().replace("]", "").replace("[", ""));

        if (applicationContext.getRegisteredObject("changeAddress") == (null)) {

        } else {
            if (!(((List<String>) applicationContext.getRegisteredObject("changeAddress")).isEmpty())) {
                stringBuilder.append(" --change " + ((List<String>) applicationContext.getRegisteredObject("changeAddress")).get(0));
            }
        }

        if (applicationContext.getRegisteredObject("UTXOs") == (null)) {
        } else {
            // --utxos '[{"vout":"2","transactionHash":"caa0472df7635f09599c925886d53794285dd6399003df9892678b29d3900d70"}]'
            stringBuilder.append(" --utxos "+ getUtxoArgument());
        }

        stringBuilder.append(" --peers " + getPeerArgument());


        LocalDateTime localTime = LocalDateTime.now();
        int minutesSinceMidnight = localTime.toLocalTime().toSecondOfDay() / 60;
        double minutesInHour = minutesSinceMidnight % 60;
        double ceilHour = Math.ceil(minutesInHour / 10);
        int shuffleTimeMinute = (int) (ceilHour * 10);
        Instant instant = Instant.ofEpochSecond(System.currentTimeMillis() / 1000);
        ZonedDateTime atZone = instant.atZone(ZoneId.systemDefault());
        long unixShuffleTime = atZone.plusMinutes(shuffleTimeMinute - atZone.getMinute()).minusSeconds(atZone.getSecond()).toEpochSecond();//shuffleDateTime.toEpochSecond(ZoneOffset.of(ZoneOffset.systemDefault().getId()));

        stringBuilder.append(" --time " + unixShuffleTime*1000L);

        System.out.println("Content of Stringbuilder: " + stringBuilder.toString());

        return stringBuilder.toString();
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
        String newInput = inputHashEdit.getText();
        String betterInput = newInput.replaceAll(" ", "");
        String portInput = inputIndexEdit.getText();
        // todo: if one of fields is empty do not paste
        if (!(betterInput.isEmpty())) {
        try {
            inetAddress = InetAddress.getByName(betterInput);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            GuiUtils.informationalAlert("Unknown Host?", "Are you sure this was a correct IP-Address?");
            inputHashEdit.setText("");
        }
        ipList.add(inetAddress);
        String encKey = inputEKedit.getText();
        String ip;
        StringBuilder builder = new StringBuilder();

        if (encKey.isEmpty()) {
            // EK is necessary
            GuiUtils.informationalAlert("Missing EK for IP?", "Did you forget to provide a EncryptionKey for an IP?");
        } else {
            if (portInput.isEmpty()) {
                // no port provided = default port?
                builder.append(inetAddress.getHostAddress());
            } else {
                builder.append(inetAddress.getHostAddress() + ":" + portInput);
            }
            builder.append(";" + encKey);


        ip = builder.toString();

            if (!inputList.contains(ip)) {
                inputList.add(ip);
            }
            listProperty.set(FXCollections.observableArrayList(inputList));
        }
        }
    }

    private String getPeerArgument() {
        JSONArray jsonArray = new JSONArray();
        List<JSONObject> peersJsonList = new LinkedList<JSONObject>();
        for (String s : inputList) {
            JSONObject peers = new JSONObject();
            // split at ; so you have ip+port at index 0 and enckey at index 1
            String[] splitted = s.split(";");
            peers.put("key", splitted[1]);
            peers.put("address", splitted[0]);
            jsonArray.add(peers);
        }

        return jsonArray.toJSONString();
    }

    private String getUtxoArgument() {
        // In form of: --utxos '[{"vout":"2","transactionHash":"caa0472df7635f09599c925886d53794285dd6399003df9892678b29d3900d70"}]'
        JSONArray jsonArray = new JSONArray();
        List<String> utxoList = ((List<String>) applicationContext.getRegisteredObject("UTXOs"));
        for (String utxo : utxoList) {
            JSONObject utxos = new JSONObject();
            // split at ; so you have ip+port at index 0 and enckey at index 1
            String[] splitted = utxo.split(":");
            utxos.put("vout", splitted[1]);
            utxos.put("transactionHash", splitted[0]);
            jsonArray.add(utxos);

        }
        return jsonArray.toJSONString();
    }

    public void next(ActionEvent actionEvent) {
        // getAddress


        StringBuilder builder = new StringBuilder();
        /*builder.append("'[");
        for (JSONObject json :
                peersJsonList) {
            builder.append(","+json.toJSONString());
        }
        builder.append("]'");
        System.out.println(builder.toString().replaceFirst(",",""));
        */

        System.out.println(makeShuffleArguments());

        OptionParser parser = Shuffle.getShuffleOptionsParser();
        OptionSet optionSet = parser.parse(makeShuffleArguments().split(" "));

        applicationContext.register("shuffleArguments",makeShuffleArguments().split(" "));
        try {
            flowActionHandler.navigate(ShuffleConsoleController.class);
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }

    }

    public Coin getRecommendedFee() {
        String url = "https://bitcoinfees.21.co/api/v1/fees/recommended";
        URL obj;
        try {
            obj = new URL(url);
            JSONTokener tokener;
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            //set user header to prevent 403
            con.setRequestProperty("User-Agent", "Chrome/5.0");
            tokener = new JSONTokener(con.getInputStream());
            org.json.JSONObject root = new org.json.JSONObject(tokener);
            return Coin.valueOf(Long.valueOf(root.get("fastestFee").toString()));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
