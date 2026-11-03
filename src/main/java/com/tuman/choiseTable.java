package com.tuman;

import com.tuman.cash.AppConfig;
import com.tuman.tableControllers.TableController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class choiseTable {
    @FXML private FlowPane tables;

    @FXML public void initialize() {
        for (AppConfig.Table table : Main.config.tables) {
            Button button = new Button(table.name);
            button.setId("table-" + table.id);
            button.setMinSize(130,70);
            button.setOnAction(event -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/table.fxml"));
                    loader.setController(new TableController(table.id));
                    Parent root = loader.load();
                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setTitle(table.name);
                    stage.initOwner(button.getScene().getWindow());
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.setOnCloseRequest(e -> { if (FxJobs.isBusy()) e.consume(); });
                    stage.show();
                } catch (Exception e) { mainController.showError(e); }
            });
            tables.getChildren().add(button);
        }
    }
}
