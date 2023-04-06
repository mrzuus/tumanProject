package com.tuman;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class checkTables implements Initializable {
    @FXML
    private Button addPos;

    @FXML
    private Button deleteTable;

    @FXML
    private TextField firstCount;

    @FXML
    private ComboBox<?> firstPos;

    @FXML
    private TextField firstPrice;

    @FXML
    private TextField firstSum;

    @FXML
    private TextField fourCount;

    @FXML
    private ComboBox<?> fourPos;

    @FXML
    private TextField fourPrice;

    @FXML
    private TextField fourSum;

    @FXML
    private Button openCheck;

    @FXML
    private Button payOff;

    @FXML
    private TextField secondCount;

    @FXML
    private ComboBox<?> secondPos;

    @FXML
    private TextField secondPrice;

    @FXML
    private TextField secondSum;

    @FXML
    private TextField thirdCount;

    @FXML
    private ComboBox<?> thirdPos;

    @FXML
    private TextField thirdPrice;

    @FXML
    private TextField thirdSum;



    @FXML
    public void ComboBoxUse(ComboBox box, TextField text) {

    }

    public void posCount1(ActionEvent actionEvent) {
    }

    public void posCount3(ActionEvent actionEvent) {
    }
@FXML
    void posCount2(ActionEvent actionEvent) {
        Pattern p = Pattern.compile("(\\d+\\.?\\d*)?");
    secondCount.textProperty().addListener((observable, oldValue, newValue) -> {
        if (!p.matcher(newValue).matches()) secondCount.setText(oldValue);
    });
    }
@FXML
    public void posCount4(ActionEvent actionEvent) {
        fourCount.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    fourCount.setText(newValue.replaceAll("[^\\d]", ""));
                }
            }
        });}


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


    }
}