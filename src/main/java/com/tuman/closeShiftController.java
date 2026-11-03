package com.tuman;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ResourceBundle;

public class closeShiftController implements Initializable {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    @FXML private TextField fieldData;
    @FXML private Button cancel;

    @FXML public void close(ActionEvent event) { ((Stage) cancel.getScene().getWindow()).close(); }

    @FXML public void confirmClose(ActionEvent event) {
        try {
            LocalDate date;
            try { date = LocalDate.parse(fieldData.getText().trim(), DATE); }
            catch (RuntimeException e) { throw new IllegalArgumentException("Введите дату в формате ДД.ММ.ГГГГ."); }
            LocalDate selectedDate = date;
            FxJobs.run(cancel.getScene().getRoot(), () -> Main.register.closeShift(selectedDate), report -> {
            mainController.refreshRevenueLabels();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Смена закрыта. Отчёт сохранён на компьютере.");
            alert.setContentText(report.toString());
            alert.showAndWait();
            close(event);
            });
        } catch (Exception e) { mainController.showError(e); }
    }

    @Override public void initialize(URL url, ResourceBundle resources) {
        fieldData.setText(LocalDate.now().format(DATE));
    }
}
