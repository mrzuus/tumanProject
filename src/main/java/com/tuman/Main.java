package com.tuman;

import com.tuman.javaTables.tables;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    public static tables firstTable = new tables();
    public static tables secondTable = new tables();
    public static tables thirdTable = new tables();
    public static tables fourthTable = new tables();
    public static tables vipTable = new tables();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("mainUI.fxml"));
        primaryStage.setTitle("OOO VTUMANE");
        primaryStage.setScene(new Scene(root, 598, 382));
        primaryStage.show();
        primaryStage.setResizable(false);

        

    }


    public static void main(String[] args) {
        launch(args);

    }
}
