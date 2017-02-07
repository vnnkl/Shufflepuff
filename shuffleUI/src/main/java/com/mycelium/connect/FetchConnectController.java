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
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;

@ViewController("shuffle_fetchIPs.fxml")
public class FetchConnectController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button backBtn;
    @FXML private TextField inputHashEdit;
    @FXML private TextField inputIndexEdit;
    ListProperty<String> listProperty = new SimpleListProperty<>();
    public ArrayList<String> inputList = new ArrayList<String>();
    @FXML private ListView inputListView;
    public Main.OverlayUI overlayUI;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    // Called by FXMLLoader
    public void initialize() {
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {
        // add Input, could be invalid still
        String newInput = inputHashEdit.getText() + ":" + inputIndexEdit.getText();
        String betterInput = newInput.replaceAll(" ", "");
        // todo: if one of fields is empty do not paste
        if (!inputList.contains(betterInput)) {
            inputList.add(betterInput);
        }
        listProperty.set(FXCollections.observableArrayList(inputList));
    }

    public void next(ActionEvent actionEvent) {
        applicationContext.register("IPs",listProperty.getValue());

    }
}
