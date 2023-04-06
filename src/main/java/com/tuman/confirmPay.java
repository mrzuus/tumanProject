package com.tuman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class confirmPay {

@FXML
private Button card;

@FXML
private Button cash;

@FXML
private Button close;

@FXML
private Button transfer;

@FXML
    void cardButton(ActionEvent event) {


}

@FXML
    void cashButton(ActionEvent event) {
 }

@FXML
    void closeWindow(ActionEvent event) {
    Stage stage = (Stage) close.getScene().getWindow();
    stage.close();
}

@FXML
    void transferButton(ActionEvent event) {
transfer.setOnAction(e -> System.out.println((";")));
            }
    private void openNewScene(String window, String Title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(window));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(Title);
        stage.initModality(Modality.NONE);
        stage.showAndWait();
        stage.setResizable(false);
    }
            }
