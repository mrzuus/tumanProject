package com.tuman;

import com.tuman.cash.CashRegister;
import com.tuman.cash.StateStore;
import com.tuman.cash.AppConfig;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    public static CashRegister register;
    public static AppConfig config;

    @Override
    public void start(Stage primaryStage) throws Exception{
        StateStore store = null;
        try {
            Path data = Path.of(System.getProperty("tuman.dataDir",
                    Path.of(System.getProperty("user.home"), ".tuman").toString()));
            store = new StateStore(data);
            register = new CashRegister(store);
            config = register.settings();
        } catch (Exception e) {
            if (store != null) store.close();
            mainController.showError(e);
            Platform.exit();
            return;
        }
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("mainUI.fxml"));
        primaryStage.setTitle("OOO VTUMANE");
        primaryStage.setScene(new Scene(root, 598, 382));
        primaryStage.show();
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(event -> { if (FxJobs.isBusy()) event.consume(); });

        

    }

    @Override public void stop() throws Exception {
        FxJobs.stop();
        if (register != null) register.close();
    }


    public static void main(String[] args) {
        launch(args);

    }
}
