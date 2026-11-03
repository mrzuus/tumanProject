package com.tuman;

import com.tuman.cash.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class HistoryController {
    @FXML private BorderPane root;
    @FXML private DatePicker from,through;
    @FXML private TextField search;
    @FXML private ComboBox<CashState.PaymentMethod> method;
    @FXML private ListView<History.ReceiptRow> receipts;
    @FXML private ListView<History.AuditRow> audit;
    @FXML private Button previous,next,auditPrevious,auditNext;
    @FXML private Label page,auditPage;
    @FXML private Tab auditTab;
    private int offset,auditOffset;

    public static String paymentName(CashState.PaymentMethod method) {
        if (method==null) return "Все способы";
        return method.label;
    }

    private static String time(String value) {
        return DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.parse(value));
    }

    @FXML public void initialize() {
        from.setEditable(false); through.setEditable(false);
        from.setValue(LocalDate.now().minusDays(30)); through.setValue(LocalDate.now());
        method.getItems().add(null); method.getItems().addAll(CashState.PaymentMethod.values());
        method.setConverter(new StringConverter<CashState.PaymentMethod>() {
            @Override public String toString(CashState.PaymentMethod value) { return paymentName(value); }
            @Override public CashState.PaymentMethod fromString(String text) { return null; }
        });
        receipts.setCellFactory(view -> new ListCell<History.ReceiptRow>() {
            @Override protected void updateItem(History.ReceiptRow row,boolean empty) {
                super.updateItem(row,empty);
                setText(empty || row==null ? null : time(row.paidAt)+" | "+row.tableName+" | "+row.total+" ₽ | "
                        +paymentName(row.method)+" | "+(row.refundId==null ? "Оплачен" : "Возвращён"));
            }
        });
        audit.setCellFactory(view -> new ListCell<History.AuditRow>() {
            @Override protected void updateItem(History.AuditRow row,boolean empty) {
                super.updateItem(row,empty);
                setText(empty || row==null ? null : time(row.at)+" | "+row.actor+" | "+row.action+"\n"+row.details);
                setWrapText(true);
            }
        });
        previous.setDisable(true); next.setDisable(true); auditPrevious.setDisable(true); auditNext.setDisable(true);
        auditTab.setOnSelectionChanged(event -> { if (auditTab.isSelected()) refreshAudit(); });
        reload();
    }

    @FXML public void find() { offset=0; reload(); }
    @FXML public void allDates() { from.setValue(null); through.setValue(null); find(); }
    @FXML public void previous() { offset=Math.max(0,offset-50); reload(); }
    @FXML public void next() { offset+=50; reload(); }

    private void reload() {
        LocalDate start=from.getValue(),end=through.getValue();
        String query=search.getText();
        CashState.PaymentMethod selected=method.getValue();
        int selectedOffset=offset;
        FxJobs.run(root,() -> Main.register.history(start,end,ZoneId.systemDefault(),query,selected,selectedOffset),result -> {
            receipts.getItems().setAll(result.rows);
            previous.setDisable(offset==0); next.setDisable(!result.hasNext);
            page.setText(result.rows.isEmpty() ? "Чеки не найдены" : "Чеки "+(offset+1)+"–"+(offset+result.rows.size()));
        });
    }

    @FXML public void openReceipt() {
        History.ReceiptRow selected=receipts.getSelectionModel().getSelectedItem();
        if (selected==null) return;
        FxJobs.run(root,() -> Main.register.receipt(selected.id),this::showReceipt);
    }

    private void showReceipt(History.ReceiptDetail detail) {
        History.ReceiptRow receipt=detail.receipt;
        Stage stage=new Stage();
        stage.initOwner(root.getScene().getWindow()); stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Чек"); stage.setOnCloseRequest(event -> { if (FxJobs.isBusy()) event.consume(); });
        StringBuilder text=new StringBuilder("Номер: "+receipt.id+"\nОплачен: "+time(receipt.paidAt)
                +"\nСтол: "+receipt.tableName+"\nСпособ оплаты: "+paymentName(receipt.method)+"\n\n");
        for (OrderItem item:detail.items) text.append(item.name).append(" | ").append(item.quantity).append(" × ")
                .append(item.price).append(" = ").append(item.total()).append(" ₽\n");
        text.append("\nИтого: ").append(receipt.total).append(" ₽");
        if (receipt.refundId!=null) text.append("\n\nПолный возврат: ").append(time(receipt.refundedAt)).append("\nПричина: ").append(receipt.reason);
        TextArea content=new TextArea(text.toString()); content.setEditable(false); content.setWrapText(true);
        Button refund=new Button("Записать полный возврат — "+receipt.total+" ₽");
        refund.setDisable(receipt.refundId!=null);
        VBox form=new VBox(10,content,refund); form.setStyle("-fx-padding: 16;");
        refund.setOnAction(event -> refundReason(stage,receipt).ifPresent(reason -> FxJobs.run(form,
                () -> { Main.register.refund(receipt.id,reason,System.getProperty("user.name","Оператор")); return null; },ignored -> {
                    mainController.refreshRevenueLabels(); stage.close(); reload();
                })));
        stage.setScene(new Scene(form,650,500)); stage.show();
    }

    private java.util.Optional<String> refundReason(Stage owner,History.ReceiptRow receipt) {
        Dialog<String> dialog=new Dialog<>(); dialog.initOwner(owner); dialog.setTitle("Подтверждение возврата");
        TextArea reason=new TextArea(); reason.setPromptText("Причина возврата"); reason.setWrapText(true); reason.setPrefRowCount(4);
        reason.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().length()<=500 ? change : null));
        Label explanation=new Label("Полный возврат "+receipt.total+" ₽. Способ: "+paymentName(receipt.method)
                +". Касса фиксирует возврат; деньги гостю возвращаются отдельно. Возврат учитывается в текущей смене.");
        explanation.setWrapText(true);
        dialog.getDialogPane().setContent(new VBox(10,explanation,reason));
        ButtonType confirm=new ButtonType("Записать возврат",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(confirm).addEventFilter(ActionEvent.ACTION,event -> {
            if (reason.getText().isBlank()) { event.consume(); mainController.showError(new IllegalArgumentException("Укажите причину возврата.")); }
        });
        dialog.setResultConverter(button -> button==confirm ? reason.getText().trim() : null);
        return dialog.showAndWait();
    }

    @FXML public void refreshAudit() { auditOffset=0; loadAudit(); }
    @FXML public void auditPrevious() { auditOffset=Math.max(0,auditOffset-50); loadAudit(); }
    @FXML public void auditNext() { auditOffset+=50; loadAudit(); }
    private void loadAudit() {
        int selected=auditOffset;
        FxJobs.run(root,() -> Main.register.audit(selected),result -> {
            audit.getItems().setAll(result.rows); auditPrevious.setDisable(auditOffset==0); auditNext.setDisable(!result.hasNext);
            auditPage.setText(result.rows.isEmpty() ? "Записей нет" : "Записи "+(auditOffset+1)+"–"+(auditOffset+result.rows.size()));
        });
    }
}
