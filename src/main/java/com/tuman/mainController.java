package com.tuman;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class mainController implements Initializable {
    @FXML
    private Button closeShift;

    @FXML
    private Button cocktailMap;

    @FXML
    private Text countOpenTables;
    @FXML
    private Text revenueCard;

    @FXML
    private Button openTables;

    @FXML
    private  Text revenue;


    @FXML
    private Text revenueCash ;


    @FXML
    private Text revenueStuff;

    @FXML
    private Text revenueTransfer;
    public static Text staticRevenueLabel;
    public static Text staticCashRevenueLabel;
    public static Text staticCardRevenueLabel;
    public static Text staticTransferRevenueLabel;
    public static Text staticStuffRevenueLabel;
public static totalCash money = new totalCash();
   public static SimpleStringProperty moneyProperty = new SimpleStringProperty();
    public static int CurrentMoneyProperty = 0;
    private void openNewScene(String window, String Title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource(window));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(Title);
        stage.initModality(Modality.NONE);
        stage.show();
        stage.setResizable(false);

    }

    public void openCockMap(ActionEvent actionEvent) {
        cocktailMap.setOnAction(event -> {
            try {
                openNewScene("coctailMenu.fxml", "Коктейльная карта");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void openTablesMenu(ActionEvent actionEvent) {
        openTables.setOnAction(event -> {
            try {
                openNewScene("tableMenu.fxml", "Столы");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void closeAndSave(ActionEvent actionEvent) {
        closeShift.setOnAction(event -> {
            try {
                openNewScene("confirmCloseShift.fxml", "Коктейльная карта");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        System.out.println(money.getCashRevenue() +   " - is cash   ");
        System.out.println(money.getCardRevenue() +   " - is card   ");
        System.out.println(money.getTransferRevenue() + " - is trasfer");
        System.out.println(money.getTotalMoney() +   " - is Total  ");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
staticRevenueLabel = revenue;
        staticCashRevenueLabel = revenueCash;
        staticCardRevenueLabel = revenueCard;
        staticTransferRevenueLabel = revenueTransfer;
        staticStuffRevenueLabel = revenueStuff;
    }
}
