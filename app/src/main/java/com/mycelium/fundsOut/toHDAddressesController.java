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

package com.mycelium.fundsOut;

import com.mycelium.Main;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.ArrayList;

public class toHDAddressesController {
    public Button AddBtn;
    public Button cancelBtn;
    public TextField inputPrivKEdit;
    public TextField inputIndexEdit;
    public ArrayList<String> privKeyList;
    public ListView privKeyListView;
    public Main.OverlayUI overlayUI;
    public Button nextBtn;
    public Label extAddressLabel1;
    public Label extAddressLabel2;
    public Label extAddressLabel3;
    public Label extAddressLabel4;


    // Called by FXMLLoader
    public void initialize() {
        // fetch internal wallets next unused addresses here and display them to the user
        this.extAddressLabel2.setText("This is the 2nd Address");
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void addInput(ActionEvent event) {

    }

    public void next(ActionEvent actionEvent) {

    }
}
