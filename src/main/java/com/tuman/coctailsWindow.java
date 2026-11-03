package com.tuman;

import com.tuman.cash.AppConfig;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class coctailsWindow {
    @FXML private VBox products;

    @FXML public void initialize() {
        for (AppConfig.Product product : Main.config.products) {
            if (product.recipe == null) {
                products.getChildren().add(new Label(product.name + " — " + product.price + " ₽"));
                continue;
            }
            Button button = new Button(product.name + " — " + product.price + " ₽");
            button.setOnAction(event -> {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/cocktailsInfo/" + product.recipe));
                    Stage stage = new Stage();
                    stage.setScene(new Scene(root));
                    stage.setTitle(product.name);
                    stage.initOwner(button.getScene().getWindow());
                    stage.initModality(Modality.WINDOW_MODAL);
                    stage.show();
                } catch (Exception e) { mainController.showError(e); }
            });
            products.getChildren().add(button);
        }
    }
}
