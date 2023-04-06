package com.tuman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class choiseTable implements Initializable {

    @FXML
    private Button Fifth;

    @FXML
    private Button firstTable;

    @FXML
    private Button fourth;

    @FXML
    private Button secondTable;

    @FXML
    private Button thirdTable;

    @FXML
    private Button vipTable;

    @FXML
    private Button cancel;


    public static String currentTabel = "";
public String getCurrentTabel(){
    return currentTabel;
}
public void setCurrentTabel (String str){
    currentTabel = str;
}

    private void openNewScene(String window, String Title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(window));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(Title);
        stage.initModality(Modality.NONE);
        stage.show();
        stage.setResizable(false);

    }
@FXML
void openVip (ActionEvent actionEvent) {
    currentTabel = "VIP";
        vipTable.setOnAction(event -> {
            try {
                openNewScene("/sceneTables/vipTable.fxml","Вип");
            } catch (IOException e) {
                e.printStackTrace();
            }
//            tableCheckInfo.clearTable();
        });


}

    @FXML
    void openFifth (ActionEvent actionEvent) {
        currentTabel = "5";
Fifth.setOnAction(event -> {
    try {
        openNewScene("/sceneTables/fifthTable.fxml","Пятый стол");
    } catch (IOException e) {
        e.printStackTrace();
    }
//    tableCheckInfo.clearTable();
});

    }

    @FXML
    void openFirst(ActionEvent actionEvent) {
        currentTabel = "1";
firstTable.setOnAction(event -> {
    Parent root;
    try {
        root = FXMLLoader.load(getClass().getResource("/sceneTables/firstTable.fxml"));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    Stage stage = new Stage();
    stage.setScene(new Scene(root));
    stage.setTitle("Первый стол");
    stage.initModality(Modality.NONE);
    stage.show();
    stage.setResizable(false);
});
//        tableCheckInfo.clearTable();
    }

    @FXML
    void openFourth(ActionEvent actionEvent) {
        currentTabel = "4";
        fourth.setOnAction(event -> {
            try {
                openNewScene("/sceneTables/fourthTable.fxml", "Четвёртый стол");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
//        tableCheckInfo.clearTable();

    }

    @FXML
    void openSecond(ActionEvent actionEvent) {
        currentTabel = "2";
        secondTable.setOnAction(event -> {
            try {
                openNewScene("/sceneTables/secondTable.fxml","Второй стол");
            } catch (IOException e) {
                e.printStackTrace();
            } });
//        tableCheckInfo.clearTable();

    }

    @FXML
    void openThird(ActionEvent actionEvent) {
        currentTabel = "3";
thirdTable.setOnAction(event -> {
    try {
        openNewScene("/sceneTables/thirdTable.fxml","Третий стол");
    } catch (IOException e) {
        e.printStackTrace();
    }
});
//        tableCheckInfo.clearTable();


    }
    public void close(ActionEvent actionEvent) {
        Stage stage = (Stage) cancel.getScene().getWindow();
        stage.close();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


    }
}
