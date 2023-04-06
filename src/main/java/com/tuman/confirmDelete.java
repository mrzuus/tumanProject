package com.tuman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;


public class confirmDelete {
static String currentTable = "";
    @FXML
    private Button close;

    @FXML
    private Button deleteTable;

    @FXML
    void closeConfirm(ActionEvent event) {
        Stage stage = (Stage) close.getScene().getWindow();
        stage.close();
    }

    @FXML
    void delTab(ActionEvent event) {

    }

}
