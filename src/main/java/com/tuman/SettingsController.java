package com.tuman;

import com.tuman.cash.AppConfig;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class SettingsController {
    @FXML private BorderPane root;
    @FXML private VBox productsPane,tablesPane;
    @FXML private TextField percent;
    @FXML private Label status;
    private AppConfig draft;

    @FXML public void initialize() throws java.io.IOException {
        draft=Main.config.copy();
        percent.setText(draft.employeePercent.toPlainString());
        productsPane.getChildren().add(productEditor());
        tablesPane.getChildren().add(tableEditor());
    }

    private VBox productEditor() {
        ListView<AppConfig.Product> list=new ListView<>(FXCollections.observableArrayList(draft.products));
        list.setId("settings-products");
        list.setCellFactory(view -> new ListCell<AppConfig.Product>() {
            @Override protected void updateItem(AppConfig.Product item,boolean empty) {
                super.updateItem(item,empty);
                setText(empty || item==null ? null : item.name+" — "+item.price+" ₽");
            }
        });
        Button add=new Button("Добавить"),edit=new Button("Изменить"),remove=new Button("Удалить");
        add.setOnAction(event -> productDialog(null).ifPresent(product -> {
            draft.products.add(product); list.getItems().setAll(draft.products); status.setText("Есть несохранённые изменения");
        }));
        edit.setOnAction(event -> {
            AppConfig.Product selected=list.getSelectionModel().getSelectedItem();
            if (selected==null) return;
            productDialog(selected).ifPresent(product -> {
                draft.products.set(draft.products.indexOf(selected),product);
                list.getItems().setAll(draft.products); status.setText("Есть несохранённые изменения");
            });
        });
        remove.setOnAction(event -> {
            AppConfig.Product selected=list.getSelectionModel().getSelectedItem();
            if (selected!=null) {
                draft.products.remove(selected); list.getItems().setAll(draft.products); status.setText("Есть несохранённые изменения");
            }
        });
        VBox content=new VBox(10,list,new HBox(10,add,edit,remove));
        VBox.setVgrow(list,Priority.ALWAYS);
        return content;
    }

    private java.util.Optional<AppConfig.Product> productDialog(AppConfig.Product selected) {
        Dialog<AppConfig.Product> dialog=new Dialog<>();
        dialog.initOwner(root.getScene().getWindow());
        dialog.setTitle(selected==null ? "Новая позиция" : "Изменение позиции");
        TextField name=new TextField(selected==null ? "" : selected.name);
        TextField price=new TextField(selected==null ? "" : String.valueOf(selected.price));
        price.setTextFormatter(new TextFormatter<String>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
        dialog.getDialogPane().setContent(new VBox(8,new Label("Название"),name,new Label("Цена в целых рублях"),price));
        ButtonType apply=new ButtonType("Применить",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(apply,ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(apply).addEventFilter(ActionEvent.ACTION,event -> {
            try {
                if (name.getText().isBlank()) throw new IllegalArgumentException("Укажите название позиции.");
                TableInput.integer(price.getText(),false);
            } catch (Exception e) { event.consume(); mainController.showError(e); }
        });
        dialog.setResultConverter(button -> {
            if (button!=apply) return null;
            AppConfig.Product product=new AppConfig.Product();
            product.id=selected==null ? "product-"+UUID.randomUUID() : selected.id;
            product.name=name.getText().trim(); product.price=TableInput.integer(price.getText(),false);
            product.recipe=selected==null ? null : selected.recipe;
            return product;
        });
        return dialog.showAndWait();
    }

    private VBox tableEditor() {
        ListView<AppConfig.Table> list=new ListView<>(FXCollections.observableArrayList(draft.tables));
        list.setId("settings-tables");
        list.setCellFactory(view -> new ListCell<AppConfig.Table>() {
            @Override protected void updateItem(AppConfig.Table item,boolean empty) {
                super.updateItem(item,empty); setText(empty || item==null ? null : item.name);
            }
        });
        Button add=new Button("Добавить стол"),rename=new Button("Переименовать"),remove=new Button("Удалить стол");
        add.setOnAction(event -> tableName(null).ifPresent(name -> {
            AppConfig.Table table=new AppConfig.Table(); table.id="table-"+UUID.randomUUID(); table.name=name;
            draft.tables.add(table); list.getItems().setAll(draft.tables); status.setText("Есть несохранённые изменения");
        }));
        rename.setOnAction(event -> {
            AppConfig.Table selected=list.getSelectionModel().getSelectedItem();
            if (selected==null) return;
            tableName(selected.name).ifPresent(name -> {
                selected.name=name; list.refresh(); status.setText("Есть несохранённые изменения");
            });
        });
        remove.setOnAction(event -> {
            AppConfig.Table selected=list.getSelectionModel().getSelectedItem();
            if (selected!=null) {
                draft.tables.remove(selected); list.getItems().setAll(draft.tables); status.setText("Есть несохранённые изменения");
            }
        });
        VBox content=new VBox(10,list,new HBox(10,add,rename,remove));
        VBox.setVgrow(list,Priority.ALWAYS);
        return content;
    }

    private java.util.Optional<String> tableName(String previous) {
        TextInputDialog dialog=new TextInputDialog(previous==null ? "" : previous);
        dialog.initOwner(root.getScene().getWindow()); dialog.setHeaderText("Название стола");
        return dialog.showAndWait().map(String::trim).filter(name -> !name.isEmpty());
    }

    @FXML public void save() {
        try {
            try { draft.employeePercent=new BigDecimal(percent.getText().trim().replace(',','.')); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Введите процент сотрудника от 0 до 100."); }
            draft.validate();
            AppConfig candidate=draft.copy();
            FxJobs.run(root,() -> Main.register.saveSettings(candidate,System.getProperty("user.name","Оператор")),saved -> {
                Main.config=saved;
                mainController.refreshRevenueLabels(); status.setText("Настройки сохранены и применены");
            });
        } catch (Exception e) { mainController.showError(e); }
    }
}
