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
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.player.Shuffle;
import com.shuffle.protocol.FormatException;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;
import joptsimple.OptionSet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.concurrent.*;

@ViewController("shuffle_console.fxml")
public class ShuffleConsoleController {
    @FXML
    private Button AddBtn;
    @FXML
    @BackAction
    private Button backBtn;
    public Main.OverlayUI overlayUI;
    @FXML
    TextArea textArea;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    private PrintStream printStream;

    OptionSet optionSet;

    ExecutorService executorService;
    Callable<Void> shuffleCallable;
    Future<Void> shuffleFuture;

    // Called by FXMLLoader
    public void initialize() {
        this.optionSet = (OptionSet) applicationContext.getRegisteredObject("optionSet");

        printStream = new PrintStream(new Console(textArea));
        System.setOut(printStream);
        System.setErr(printStream);
        System.out.println("Press Next to begin shuffle");

        executorService = Executors.newSingleThreadExecutor();

        shuffleCallable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                new Shuffle(optionSet, printStream);
                return null;
            }
        };

    }

    public class Console extends OutputStream {
        private TextArea console;

        public Console(TextArea console) {
            this.console = console;
        }

        public void appendText(String valueOf) {
            Platform.runLater(() -> console.appendText(valueOf));
        }

        public void write(int b) throws IOException {
            Platform.runLater(() -> appendText(String.valueOf((char) b)));
        }
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    public void next(ActionEvent actionEvent) {
        shuffleFuture = executorService.submit(shuffleCallable);
    }
}
