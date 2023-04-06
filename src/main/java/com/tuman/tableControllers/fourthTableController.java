package com.tuman.tableControllers;

import com.tuman.Main;
import com.tuman.cocts.*;
import com.tuman.mainController;
import com.tuman.tableModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class fourthTableController extends mainController implements Initializable {
    @FXML
    private Button addPos;

    @FXML
    private AnchorPane anchor;

    @FXML
    private Button deleteTable;

    @FXML
    private TextField firstCount;

    @FXML
    private ComboBox<?> firstPos;

    @FXML
    private TextField firstPrice;

    @FXML
    private TextField firstSum;

    @FXML
    private TextField fourCount;

    @FXML
    private ComboBox<?> fourPos;

    @FXML
    private TextField fourPrice;

    @FXML
    private TextField fourSum;

    @FXML
    private Button openCheck;

    @FXML
    private Button payOff;

    @FXML
    private TextField secondCount;

    @FXML
    private ComboBox<?> secondPos;

    @FXML
    private TextField secondPrice;

    @FXML
    private TextField secondSum;

    @FXML
    private TextField thirdCount;

    @FXML
    private ComboBox<?> thirdPos;

    @FXML
    private TextField thirdPrice;

    @FXML
    private TextField thirdSum;

    @FXML
    private Text totalSum;

    @FXML
    void CountUp(ActionEvent event) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 300,50);
        Stage stage = new Stage();
        stage.setResizable(false);
        stage.setScene(scene);
        Button cashPay = new Button("Наличные");
        Button transferPay = new Button("Перевод");
        Button cardPay = new Button("Безнал");
        cardPay.setMinSize(90,40);
        cashPay.setMinSize(90,40);
        transferPay.setMinSize(90,40);
        root.setAlignment(cardPay, Pos.TOP_LEFT);
        root.setAlignment(cashPay, Pos.TOP_CENTER);
        root.setAlignment(transferPay, Pos.TOP_RIGHT);
        root.getChildren().add(cardPay);
        root.getChildren().add(cashPay);
        root.getChildren().add(transferPay);

        cashPay.setOnAction(e-> {
            // implementation close Table
            money.setCashRevenue( money.getCashRevenue() + Main.fourthTable.getSum());
            staticRevenueLabel.setText(String.valueOf(money.getTotalMoney()));
            staticCashRevenueLabel.setText(String.valueOf(money.getCashRevenue()));
            staticStuffRevenueLabel.setText(String.valueOf(money.getTotalMoney() * 0.1));
            Main.fourthTable.deleteAllPositions();

            stage.close();
        });
        cardPay.setOnAction(e-> {
            money.setCardRevenue( money.getCardRevenue() + Main.fourthTable.getSum());
            staticRevenueLabel.setText(String.valueOf(money.getTotalMoney()));
            staticCardRevenueLabel.setText(String.valueOf(money.getCardRevenue()));
            staticStuffRevenueLabel.setText(String.valueOf(money.getTotalMoney() * 0.1));
            Main.fourthTable.deleteAllPositions();
            stage.close();
        });
        transferPay.setOnAction(e -> {
            money.setTransferRevenue( money.getTransferRevenue() + Main.fourthTable.getSum());
            staticTransferRevenueLabel.setText(String.valueOf(money.getCashRevenue()));
            staticStuffRevenueLabel.setText(String.valueOf(money.getTotalMoney() * 0.1));
            Main.fourthTable.deleteAllPositions();
            stage.close();

        });
        stage.showAndWait();
    }
    public void clearAllPos(){
        firstPos.getSelectionModel().select(0);
        firstPrice.setText("");
        firstCount.setText("");
        secondPos.getSelectionModel().select(0);
        secondPrice.setText("");
        secondCount.setText("");
        thirdPos.getSelectionModel().select(0);
        thirdPrice.setText("");
        thirdPrice.setText("");
        fourPos.getSelectionModel().select(0);
        fourPrice.setText("");
        fourCount.setText("");

    }
    @FXML
    void addPosition(ActionEvent event) {
        if (posIsNotNull(firstPos) ) {
            Main.fourthTable.addPos( firstPos.getValue().toString(), Integer.parseInt(firstCount.getText()), Integer.parseInt(firstPrice.getText()));
        }
        if (posIsNotNull(secondPos)) {
            Main.fourthTable.addPos( secondPos.getValue().toString(), Integer.parseInt(secondCount.getText()), Integer.parseInt(secondPrice.getText()) );
        }
        if (posIsNotNull(thirdPos)) {
            Main.fourthTable.addPos( thirdPos.getValue().toString(), Integer.parseInt(thirdCount.getText()), Integer.parseInt(thirdPrice.getText()));
        }
        if (posIsNotNull(fourPos)) {
            Main.fourthTable.addPos( fourPos.getValue().toString(), Integer.parseInt(fourCount.getText()), Integer.parseInt(fourPrice.getText()));
        }
        clearAllPos();
    }

    @FXML
    void delete(ActionEvent event) {

    }

    @FXML
    void openTableCheck(ActionEvent event) {
        ObservableList<tableModel> lis = FXCollections.observableArrayList();
        if (!Main.fourthTable.getPositions().isEmpty()){
            for (int i = 0; i< (Main.fourthTable.getPositions().size()); i++){
                lis.add(new tableModel(Main.fourthTable.getPositions().get(i),String.valueOf(Main.fourthTable.getAmount().get(i)),String.valueOf(Main.fourthTable.getPrice().get(i)),
                        String.valueOf(Main.fourthTable.getAmount().get(i)*Main.fourthTable.getPrice().get(i)) ));
            }}
        TableView<tableModel> table = new TableView<>();
        TableColumn<tableModel, String> position = new TableColumn<>("Позиции");
        TableColumn<tableModel, Integer> amount = new TableColumn<>("Количество");
        TableColumn<tableModel, Integer> price = new TableColumn<>("Цена");
        TableColumn<tableModel, Integer> sum = new TableColumn<>("Сумма");
        position.setCellValueFactory(new PropertyValueFactory<>("position"));
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        price.setCellValueFactory(new PropertyValueFactory<>("price"));
        sum.setCellValueFactory(new PropertyValueFactory<>("sum"));
        table.getColumns().addAll(position,price,amount,sum);


        StackPane root = new StackPane();
        Scene scene = new Scene(root, 400,600);
        Stage stage = new Stage();
        stage.setScene(scene);

        Button changeButton = new Button("Изменить");
        table.setEditable(true);
        Label totalLabel = new Label("Итого:" + Main.fourthTable.getSum());
        totalLabel.setFont(new Font("Arial", 17));;

        root.setAlignment(totalLabel, Pos.BOTTOM_RIGHT);
        root.setAlignment(table, Pos.TOP_LEFT);
        root.setAlignment(changeButton, Pos.BOTTOM_CENTER);

        table.setMaxHeight(500);
        stage.setTitle("Чек первого стола ");
        table.setItems(lis);
        root.getChildren().add(table);
        root.getChildren().add(totalLabel);
        root.getChildren().add(changeButton);
        stage.initModality(Modality.WINDOW_MODAL);
        changeButton.setOnAction(e->
        {

            if (!table.getSelectionModel().isEmpty()) {
                StackPane rt = new StackPane();
                Scene sc = new Scene(rt, 350, 150);
                Stage st = new Stage();
                st.setScene(sc);
                st.initModality(Modality.WINDOW_MODAL);
                st.setTitle("Изменение позиций");
                TextField fieldPosition = new TextField(table.getSelectionModel().getSelectedItem().getPosition());
                TextField fieldAmount = new TextField(table.getSelectionModel().getSelectedItem().getAmount());
                TextField fieldPrice = new TextField(table.getSelectionModel().getSelectedItem().getPrice());
                Button confirm = new Button("Подтвердить");
                Button deletePosition = new Button("Удалить ");
                rt.getChildren().add(fieldPosition);
                rt.getChildren().add(fieldAmount);
                rt.getChildren().add(fieldPrice);
                rt.getChildren().add(confirm);
                rt.getChildren().add(deletePosition);
                fieldPosition.setMaxWidth(150);
                fieldAmount.setMaxWidth(100);
                onlyNumbers(firstCount);
                onlyNumbers(fieldPrice);
                fieldPrice.setMaxWidth(100);
                rt.setAlignment(fieldPosition, Pos.TOP_LEFT);
                rt.setAlignment(fieldAmount, Pos.TOP_CENTER);
                rt.setAlignment(fieldPrice, Pos.TOP_RIGHT);
                rt.setAlignment(confirm, Pos.BOTTOM_LEFT);
                rt.setAlignment(deletePosition, Pos.BOTTOM_RIGHT);
                st.show();
                confirm.setOnAction(o-> {
                    Main.fourthTable.changePositionValues(table.getSelectionModel().getFocusedIndex(), fieldPosition.getText(), fieldAmount.getText(), fieldPrice.getText());
                    st.close();
                    stage.close();
                });
                deletePosition.setOnAction(p -> {
                    Main.fourthTable.deletePositionValues(table.getSelectionModel().getFocusedIndex());
                    st.close();
                    stage.close();
                });

            }
        });
        stage.showAndWait();



    }
    void ComboBoxUse(ComboBox box, TextField text, TextField count) {
        switch (box.getValue().toString()) {
            case "Виски кола":
                text.setText(whiskeyCola.count);
                break;
            case "Кричащий оргазм":
                text.setText(String.valueOf(orgasm.count));
                break;
            case "Стиратель памяти":
                text.setText(String.valueOf(memory.count));
                break;
            case "Голубая лагуна":
                text.setText(String.valueOf(blueLagoon.count));
                break;
            case "Черный русский":
                text.setText(String.valueOf(blackRussian.count));
                break;
            case "Благодарный мертвец":
                text.setText(String.valueOf((deadman.count)));
                break;
            case "Негрони":
                text.setText(String.valueOf((negroni.count)));
                break;
            case "Отвертка":
                text.setText(String.valueOf((otvertka.count)));
                break;
            case "Джин тоник":
                text.setText(String.valueOf((ginTonic.count)));
                break;
            case "Полет на луну":
                text.setText(String.valueOf((flyToMoon.count)));
                break;
            case "Зеленая фея":
                text.setText(String.valueOf((greenSpirit.count)));
                break;
            case "Текила бум":
                text.setText(String.valueOf((tequilaBoom.count)));
                break;
            case "Лонг Айленд":
                text.setText(String.valueOf((longIsland.count)));
                break;
            case "Маргарита":
                text.setText(String.valueOf((margarita.count)));
                break;
            case "Маргарита клубничная":
                text.setText(String.valueOf((strawberryMargarita.count)));
                break;
            case "Дайкири":
                text.setText(String.valueOf((daiquiri.count)));
                break;
            case "Пина колада":
                text.setText(String.valueOf((pinaColada.count)));
                break;
            case "День без мужа":
                text.setText(String.valueOf((husband.count)));
                break;
            case "Свидание":
                text.setText(String.valueOf((meeting.count)));
                break;
            case "Вишневая любовь":
                text.setText(String.valueOf(cherryLove.count));
                break;
            case "Текила Санрайз":
                text.setText(String.valueOf((tequilaSunrise.count)));
                break;
            case "Мохито":
                text.setText(String.valueOf((mohito.count)));
                break;
            case "Куба либре":
                text.setText(String.valueOf((cubaLibra.count)));
                break;
            case "Выстрел в голову":
                text.setText(String.valueOf((headshot.count)));
                break;
            case "Хиросима":
                text.setText(String.valueOf((hiroshima.count)));
                break;
            case "Медуза":
                text.setText(String.valueOf((medusa.count)));
                break;
            case "Боярский":
                text.setText(String.valueOf((boyarsky.count)));
                break;
            case "Б52":
                text.setText(String.valueOf((b52.count)));
                break;
            case "Рафаэлло":
                text.setText(String.valueOf((rafaello.count)));
                break;
            case "Таракан":
                text.setText(String.valueOf((beetle.count)));
                break;
            case "бомж":
                text.setText(String.valueOf((bomj.count)));
                break;
            default:
                clearFullPos(box, text, count);
                break;

        }
        count.setText("1");
        if (box.getSelectionModel().getSelectedIndex() == 0) {
            count.setText("");
        }

    }
    public void clearFullPos(ComboBox pos, TextField count, TextField price) {
        pos.getSelectionModel().select(0);
        count.setText("");
        price.setText("");

    }
    boolean posIsNotNull (ComboBox pos){
        if (pos.equals(firstPos)){
            if(!firstPos.getSelectionModel().isEmpty() && !firstPos.getValue().toString().equals("Выбрать") && firstCount.getText() != "" && firstPrice.getText() != ""){
                return true;
            }
        }
        if (pos.equals(secondPos)){
            if( (!secondPos.getSelectionModel().isEmpty() &&  !secondPos.getValue().toString().intern().equals("Выбрать") && secondCount.getText() != null && secondPrice!= null)){
                return true;
            }
        }
        if (pos.equals(thirdPos)){
            if(!thirdPos.getSelectionModel().isEmpty() && !thirdPos.getValue().toString().intern().equals("Выбрать") && thirdCount.getText() != null && thirdPrice!= null){
                return true;
            }
        }
        if (pos.equals(fourPos)){
            return !fourPos.getSelectionModel().isEmpty() && !fourPos.getValue().toString().intern().equals("Выбрать") && fourCount.getText() != null && fourPrice != null;
        }
        return false;
    }
    public void setSum (TextField count, TextField price, TextField Sum){
        int calc = 0;
        int calc2 = 0;
        if (count.getText().length() > 0 && price.getText().length() > 0) {
            calc = Integer.parseInt(count.getText());
            calc2 = Integer.parseInt(price.getText());
        }
        Sum.setText(String.valueOf(calc * calc2));
    }
    public String totalAmount () {
        int sumAm = 0;
        if (firstSum.getText().length() > 0 && secondSum.getText().length() > 0 && thirdSum.getText().length() > 0 && fourSum.getText().length() > 0) {
            sumAm = (Integer.parseInt(firstSum.textProperty().getValue()) + (Integer.parseInt(secondSum.textProperty().getValue()) + Integer.parseInt(thirdSum.textProperty().getValue()) + Integer.parseInt(fourSum.textProperty().getValue())));
        }
        return String.valueOf(sumAm);
    }
    public int totalPosAmount (TextField count, TextField price){
        int calc = 0;
        int calc2 = 0;
        if (count.getText().length() > 0 && price.getText().length() > 0) {
            calc = Integer.parseInt(count.getText());
            calc2 = Integer.parseInt(price.getText());
        }
        return calc != 0 && calc2 != 0 ? calc * calc2 : 0;
    }
    void onlyNumbers (TextField field) {
        Pattern p = Pattern.compile("(\\d+\\.?\\d*)?");
        field.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (!p.matcher(newValue).matches()) field.setText(oldValue);
        });
    }
    void openNewScene(String window, String Title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(window));
        Stage stage = new Stage();
        stage.setScene(new Scene(root));
        stage.setTitle(Title);
        stage.initModality(Modality.NONE);
        stage.showAndWait();
        stage.setResizable(false);

    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        System.out.println(money.getCashRevenue());

        Pattern p = Pattern.compile("(\\d+\\.?\\d*)?");
        firstSum.textProperty().addListener((observableValue) -> totalSum.setText(String.valueOf(totalAmount())));
        firstPrice.textProperty().addListener((observableValue) -> firstSum.setText(String.valueOf(totalPosAmount(firstCount, firstPrice))));
        secondSum.textProperty().addListener((observableValue) -> totalSum.setText(String.valueOf(totalAmount())));
        thirdSum.textProperty().addListener((observableValue) -> totalSum.setText(totalAmount()));
        fourSum.textProperty().addListener((observableValue) -> totalSum.setText(totalAmount()));
        secondPrice.textProperty().addListener((observableValue) -> secondSum.setText(String.valueOf(totalPosAmount(secondCount, secondPrice))));
        thirdPrice.textProperty().addListener(observable -> thirdSum.setText(String.valueOf(totalPosAmount(thirdCount, thirdPrice))));
        firstSum.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!p.matcher(newValue).matches()) thirdCount.setText(oldValue); });
        // Set sum
        thirdCount.textProperty().addListener(observable -> {
            if (!thirdCount.getText().equals("")) setSum(thirdCount, thirdPrice, thirdSum); });
        firstCount.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!firstCount.getText().equals("")) setSum(firstCount, firstPrice, firstSum); });
        secondCount.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!secondCount.getText().equals("")) setSum(secondCount, secondPrice, secondSum); });
        thirdCount.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!thirdCount.getText().equals("")) setSum(thirdCount, thirdPrice, thirdSum); });
        fourCount.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!fourCount.getText().equals("")) setSum(fourCount, fourPrice, fourSum); });
        firstPrice.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!firstCount.getText().equals("")) setSum(firstCount, firstPrice, firstSum); });
        secondPrice.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!secondCount.getText().equals("")) setSum(secondCount, secondPrice, secondSum); });
        thirdPrice.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!thirdCount.getText().equals("")) setSum(thirdCount, thirdPrice, thirdSum); });
        fourPrice.textProperty().addListener((observableValue, oldValue, NewValue) -> {
            if (!fourCount.getText().equals("")) setSum(fourCount, fourPrice, fourSum); });
        // Set price from ComboBox
        firstPos.setOnAction(e -> ComboBoxUse(firstPos, firstPrice, firstCount));
        secondPos.setOnAction(e -> ComboBoxUse(secondPos, secondPrice, secondCount));
        thirdPos.setOnAction(e -> ComboBoxUse(thirdPos, thirdPrice, thirdCount));
        fourPos.setOnAction(e -> ComboBoxUse(fourPos, fourPrice, fourCount));
        onlyNumbers(thirdCount);
        onlyNumbers(thirdPrice);
        onlyNumbers(firstPrice);
        onlyNumbers(firstCount);
        onlyNumbers(fourCount);
        onlyNumbers(secondPrice);
        onlyNumbers(secondCount);
        onlyNumbers(fourPrice);
    }

}

