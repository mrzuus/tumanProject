package com.tuman;

import javafx.beans.property.SimpleStringProperty;

public class totalCash {
 private int cashRevenue= 0;
 private int cardRevenue = 0 ;
 private int transferRevenue = 0 ;
 public int getTotalMoney(){
     return getCashRevenue()+getCardRevenue()+getTransferRevenue();
 }

    public  int getCashRevenue() {
        return cashRevenue;
    }

    public void setCashRevenue(int cashRevenue) {
        this.cashRevenue = cashRevenue;
    }

    public int getCardRevenue() {
        return cardRevenue;
    }

    public void setCardRevenue(int cardRevenue) {
        this.cardRevenue = cardRevenue;
    }

    public int getTransferRevenue() {
        return transferRevenue;
    }

    public void setTransferRevenue(int transferRevenue) {
        this.transferRevenue = transferRevenue;
    }
}
