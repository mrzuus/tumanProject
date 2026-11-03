package com.tuman.tableControllers;

import com.tuman.*;
import com.tuman.cash.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.*;

/** One data-driven view for any configured dining table and any number of draft rows. */
public final class TableController {
    private final String tableId;
    private final List<DraftRow> drafts = new ArrayList<>();
    @FXML private BorderPane root;
    @FXML private VBox rows;
    @FXML private Label totalSum;

    public TableController(String tableId) { this.tableId = tableId; }

    private void integersOnly(TextField field) {
        field.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("[0-9]*") ? change : null));
    }

    private final class DraftRow {
        final ComboBox<AppConfig.Product> product = new ComboBox<>();
        final TextField quantity = new TextField();
        final TextField price = new TextField();
        final Label sum = new Label();
        final HBox controls;

        DraftRow() {
            product.getItems().setAll(Main.config.products);
            product.setPromptText("Позиция");
            product.setPrefWidth(260);
            quantity.setPromptText("Кол-во"); quantity.setPrefWidth(80);
            price.setPromptText("Цена, ₽"); price.setPrefWidth(100);
            integersOnly(quantity); integersOnly(price);
            Button remove = new Button("Убрать");
            controls = new HBox(8, product, quantity, price, sum, remove);
            quantity.textProperty().addListener((o,a,b) -> updateSums());
            price.textProperty().addListener((o,a,b) -> updateSums());
            product.setOnAction(event -> {
                if (product.getValue() == null) { quantity.clear(); price.clear(); }
                else { price.setText(String.valueOf(product.getValue().price)); quantity.setText("1"); }
                updateSums();
            });
            remove.setOnAction(event -> {
                drafts.remove(this); rows.getChildren().remove(controls);
                if (drafts.isEmpty()) addRow();
                updateSums();
            });
        }

        OrderItem item() {
            return TableInput.item(product.getValue().name, quantity.getText(), price.getText());
        }
    }

    @FXML public void initialize() { addRow(); }

    @FXML public void addRow() {
        DraftRow row = new DraftRow();
        drafts.add(row); rows.getChildren().add(row.controls);
        updateSums();
    }

    private void updateSums() {
        int total=0;
        boolean valid=true;
        for (DraftRow row:drafts) {
            if (row.product.getValue()==null) { row.sum.setText(""); continue; }
            try {
                int value=row.item().total();
                row.sum.setText(String.valueOf(value));
                total=Math.addExact(total,value);
            } catch (IllegalArgumentException | ArithmeticException e) { row.sum.setText("—"); valid=false; }
        }
        totalSum.setText(valid ? "К добавлению: " + total + " ₽" : "Проверьте количество и цену");
    }

    @FXML public void addPosition() {
        try {
            List<OrderItem> items=new ArrayList<>();
            for (DraftRow row:drafts) if (row.product.getValue()!=null) items.add(row.item());
            if (items.isEmpty()) throw new IllegalArgumentException("Выберите хотя бы одну позицию.");
            FxJobs.run(root, () -> { Main.register.addItems(tableId,items); return null; }, ignored -> {
                drafts.clear(); rows.getChildren().clear(); addRow();
            });
        } catch (Exception e) { mainController.showError(e); }
    }

    @FXML public void pay() {
        if (drafts.stream().anyMatch(row -> row.product.getValue()!=null)) {
            mainController.showError(new IllegalArgumentException("Сначала добавьте выбранные позиции в заказ или уберите их."));
            return;
        }
        Stage stage=new Stage();
        stage.setTitle("Оплата");
        stage.initOwner(root.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnCloseRequest(event -> { if (FxJobs.isBusy()) event.consume(); });
        HBox buttons=new HBox(10);
        buttons.setPadding(new Insets(15));
        for (CashState.PaymentMethod method:CashState.PaymentMethod.values()) {
            Button button=new Button(method.label);
            button.setOnAction(event -> FxJobs.run(buttons,
                    () -> { Main.register.pay(tableId,method); return null; }, ignored -> {
                        mainController.refreshRevenueLabels(); stage.close();
                    }));
            buttons.getChildren().add(button);
        }
        stage.setScene(new Scene(buttons)); stage.show();
    }

    private Stage modal(Stage owner,String title) {
        Stage stage=new Stage();
        stage.initOwner(owner); stage.initModality(Modality.WINDOW_MODAL); stage.setTitle(title);
        stage.setOnCloseRequest(event -> { if (FxJobs.isBusy()) event.consume(); });
        return stage;
    }

    @FXML public void openTableCheck() {
        Stage stage=modal((Stage)root.getScene().getWindow(),"Чек");
        ListView<OrderItem> list=new ListView<>();
        list.setCellFactory(view -> new ListCell<OrderItem>() {
            @Override protected void updateItem(OrderItem item,boolean empty) {
                super.updateItem(item,empty);
                setText(empty || item==null ? null : item.name+" | "+item.quantity+" × "+item.price+" = "+item.total());
            }
        });
        Label total=new Label();
        Runnable refresh=() -> {
            try {
                List<OrderItem> items=Main.register.order(tableId);
                list.getItems().setAll(items); total.setText("Итого: "+CashState.total(items));
            } catch (Exception e) { mainController.showError(e); }
        };
        refresh.run();
        Button edit=new Button("Изменить"), delete=new Button("Удалить позицию");
        VBox content=new VBox(10,list,total,new HBox(10,edit,delete));
        content.setPadding(new Insets(15));
        edit.setOnAction(event -> {
            int index=list.getSelectionModel().getSelectedIndex();
            if (index<0) return;
            OrderItem item=list.getSelectionModel().getSelectedItem();
            Stage editor=modal(stage,"Изменение позиции");
            TextField name=new TextField(item.name), quantity=new TextField(String.valueOf(item.quantity)),
                    price=new TextField(String.valueOf(item.price));
            integersOnly(quantity); integersOnly(price);
            Button save=new Button("Сохранить");
            VBox form=new VBox(8,new Label("Название"),name,new Label("Количество"),quantity,
                    new Label("Цена, целые рубли"),price,save);
            form.setPadding(new Insets(15));
            save.setOnAction(action -> {
                try {
                    OrderItem changed=TableInput.item(name.getText(),quantity.getText(),price.getText());
                    FxJobs.run(form, () -> { Main.register.changeItem(tableId,index,changed); return null; }, ignored -> {
                        refresh.run(); editor.close();
                    });
                } catch (Exception e) { mainController.showError(e); }
            });
            editor.setScene(new Scene(form,320,270)); editor.show();
        });
        delete.setOnAction(event -> {
            int index=list.getSelectionModel().getSelectedIndex();
            if (index<0) return;
            FxJobs.run(content, () -> { Main.register.removeItem(tableId,index); return null; }, ignored -> refresh.run());
        });
        stage.setScene(new Scene(content,550,500)); stage.show();
    }
}
