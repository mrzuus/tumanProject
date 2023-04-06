package com.tuman;

import javafx.beans.property.SimpleStringProperty;

public class tableModel {
    private SimpleStringProperty position;
    private SimpleStringProperty amount;
    private SimpleStringProperty sum;

    public tableModel() {

    }

    public String getPosition() {
        return position.get();
    }

    public SimpleStringProperty positionProperty() {
        return position;
    }

    public void setPosition(String position) {
        this.position.set(position);
    }

    public String getAmount() {
        return amount.get();
    }

    public SimpleStringProperty amountProperty() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount.set(amount);
    }

    public String getSum() {
        return sum.get();
    }

    public SimpleStringProperty sumProperty() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum.set(sum);
    }

    public String getPrice() {
        return price.get();
    }

    public SimpleStringProperty priceProperty() {
        return price;
    }

    public void setPrice(String price) {
        this.price.set(price);
    }

    private SimpleStringProperty price;


    public tableModel(String position, String amount, String price, String sum) {
        this.price = new SimpleStringProperty(price);
        this.position = new SimpleStringProperty(position);
        this.amount = new SimpleStringProperty(amount);
        this.sum = new SimpleStringProperty(sum);
    }







}
