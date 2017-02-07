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
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.injection.scopes.FlowScoped;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import javax.annotation.PostConstruct;

@FlowScoped
@ViewController("shuffle_start.fxml")
public class StartWizardController {
    @FXML
    StackPane stackPane;

    public StartWizardController() {
    }

    @ActionHandler
    FlowActionHandler flowActionHandler;


    @PostConstruct
    public void initialize() {
        try {
            new Flow(ShuffleStartController.class).startInPane(stackPane);
        } catch (FlowException e) {
            e.printStackTrace();
        }
    }


}