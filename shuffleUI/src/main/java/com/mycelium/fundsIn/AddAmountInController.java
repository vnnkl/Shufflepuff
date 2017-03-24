package com.mycelium.fundsIn;

import io.datafx.controller.ViewController;
import io.datafx.controller.context.ApplicationContext;
import io.datafx.controller.context.FXMLApplicationContext;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.action.BackAction;
import io.datafx.controller.flow.context.ActionHandler;
import io.datafx.controller.flow.context.FlowActionHandler;
import io.datafx.controller.util.VetoException;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import org.bitcoinj.core.Coin;

/**
 * Created by conta on 24.01.17.
 */
@ViewController("shuffle_amountIn.fxml")
public class AddAmountInController {
    @FXML
    @BackAction
    private Button backBtn;
    @FXML
    private Button nextBtn;
    @FXML
    ChoiceBox<String> amountInChoice;
    @ActionHandler
    FlowActionHandler flowActionHandler;
    @FXMLApplicationContext
    ApplicationContext applicationContext = ApplicationContext.getInstance();



    public void initialize(){
        amountInChoice.setItems(FXCollections.observableArrayList("0.1 BTC", "0.5 BTC", "1 BTC", "1.5 BTC", "2 BTC"));
        amountInChoice.setValue("1 BTC");
    }


    public void next(ActionEvent actionEvent) {
        // register shuffleAmount as Long in satoshis
        applicationContext.register("shuffleAmount", Coin.parseCoin(amountInChoice.getValue().split(" BTC")[0]));
        try {
            flowActionHandler.navigate((Class<?>) applicationContext.getRegisteredObject("inOption"));
        } catch (VetoException | FlowException e) {
            e.printStackTrace();
        }
    }


}
