package com.tuman;

import com.tuman.cash.CashState.PaymentMethod;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;
import java.math.BigDecimal;

public class mainController implements Initializable {
    @FXML private Button closeShift;
    @FXML private Text revenue, revenueCash, revenueCard, revenueTransfer, revenueStuff;
    private static mainController active;

    public static void showError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Не удалось выполнить действие");
        alert.setHeaderText("Действие не выполнено");
        alert.setContentText(e instanceof ArithmeticException ? "Сумма слишком большая. Уменьшите количество или цену."
                : e.getMessage() == null ? "Не удалось сохранить данные кассы." : e.getMessage());
        alert.showAndWait();
    }

    public static void refreshRevenueLabels() {
        if (active == null || Main.register == null) return;
        try {
        active.revenue.setText(String.valueOf(Main.register.revenue(null)));
        active.revenueCash.setText(String.valueOf(Main.register.revenue(PaymentMethod.CASH)));
        active.revenueCard.setText(String.valueOf(Main.register.revenue(PaymentMethod.CARD)));
        active.revenueTransfer.setText(String.valueOf(Main.register.revenue(PaymentMethod.TRANSFER)));
        active.revenueStuff.setText(BigDecimal.valueOf(Main.register.revenue(null))
                .multiply(Main.config.employeePercent).movePointLeft(2).stripTrailingZeros().toPlainString());
        } catch (java.io.IOException e) { showError(e); }
    }

    private void openNewScene(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource(resource));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.initOwner(closeShift.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setOnCloseRequest(event -> { if (FxJobs.isBusy()) event.consume(); });
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) { showError(e); }
    }

    public void openCockMap(ActionEvent event) { openNewScene("coctailMenu.fxml", "Коктейльная карта"); }
    public void openTablesMenu(ActionEvent event) { openNewScene("tableMenu.fxml", "Столы"); }
    public void closeAndSave(ActionEvent event) { openNewScene("confirmCloseShift.fxml", "Закрытие смены"); }
    public void openSettings(ActionEvent event) { openNewScene("settings.fxml", "Настройки кассы"); }
    public void openHistory(ActionEvent event) { openNewScene("history.fxml", "История чеков и возвраты"); }

    @Override public void initialize(URL url, ResourceBundle resources) {
        active = this;
        refreshRevenueLabels();
    }
}
