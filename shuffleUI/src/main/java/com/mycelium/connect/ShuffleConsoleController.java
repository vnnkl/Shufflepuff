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
import com.mycelium.utils.TextAreaAppender;
import com.shuffle.bitcoin.impl.BitcoinCrypto;
import com.shuffle.player.Shuffle;
import com.shuffle.protocol.FormatException;
import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.core.concurrent.Async;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import joptsimple.OptionSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.filter.BurstFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@ViewController("shuffle_console.fxml")
public class ShuffleConsoleController {
    @FXML private Button AddBtn;
    @FXML @BackAction private Button backBtn;
    public Main.OverlayUI overlayUI;
    @FXML TextArea textArea;

    TextAreaAppender textAreaAppender;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();

    private PrintStream printStream;

    OptionSet optionSet;

    ExecutorService executorService;
    Callable<Void> shuffleCallable;

    // Called by FXMLLoader
    public void initialize() {
        this.optionSet = (OptionSet) applicationContext.getRegisteredObject("optionSet");

        textAreaAppender = TextAreaAppender.createAppender("textAreaAppender", null, BurstFilter.newBuilder().setLevel(Level.ALL).build());
        TextAreaAppender.setTextArea(textArea);



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
            appendText(String.valueOf((char)b));
        }
    }

    public void cancel(ActionEvent event) {
        overlayUI.done();
    }

    @Async
    public void next(ActionEvent actionEvent) {
        try {
            new Shuffle(optionSet);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (BitcoinCrypto.Exception e) {
            e.printStackTrace();
        }
    }
}
