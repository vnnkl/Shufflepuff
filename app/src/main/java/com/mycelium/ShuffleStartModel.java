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

import io.datafx.controller.ViewController;
import io.datafx.controller.ViewNode;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.ActionMethod;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.injection.scopes.ApplicationScoped;
import io.datafx.controller.injection.scopes.FlowScoped;
import io.datafx.controller.util.VetoException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;


@FlowScoped
public class ShuffleStartModel {

    public ShuffleStartModel(ArrayList<String> fundsInList, ArrayList<String> fundsOutList, ArrayList<String> connectionList) {
        this.fundsInList = fundsInList;
        this.fundsOutList = fundsOutList;
        this.connectionList = connectionList;
    }

    public ShuffleStartModel() {
        this.fundsInList = new ArrayList<>();
        this.fundsOutList = new ArrayList<>();
        this.connectionList = new ArrayList<>();
    }

    private ArrayList<String> fundsInList;
    private ArrayList<String> fundsOutList;
    private ArrayList<String> connectionList;

    public void setFundsInList(ArrayList<String> fundsInList){
        this.fundsInList.addAll(fundsInList);
    }
    public void setFundsOutList(ArrayList<String> fundsOutList){
        this.fundsOutList.addAll(fundsOutList);
    }
    public void setConnectionList(ArrayList<String> connectionList){
        this.connectionList.addAll(connectionList);
    }

    public ArrayList<String> getFundsInList() {
        return fundsInList;
    }

    public ArrayList<String> getFundsOutList() {
        return fundsOutList;
    }

    public ArrayList<String> getConnectionList() {
        return connectionList;
    }
}
