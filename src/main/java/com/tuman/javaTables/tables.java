package com.tuman.javaTables;


import java.util.ArrayList;
import java.util.List;

public class tables {

    private ArrayList<String> positions = new ArrayList<>();
    private ArrayList<Integer> amount = new ArrayList<>();
    private  ArrayList <Integer>  price = new ArrayList<>();
    public void deleteAllPositions(){
        positions = new ArrayList<>();
        amount = new ArrayList<>();
        price = new ArrayList<>();
    }
    public int getSum(){
        int sum = 0;
        for (int i = 0; i< positions.size(); i++){
           sum= sum+ amount.get(i) * price.get(i);
        }
        return sum;
    }

    @Override
    public String toString() {
        return "tables{" +
                "namePos=" + positions +
                ", amount=" + amount +
                ", price=" + price +
                '}';
    }

    public tables() {
    }
public void changePositionValues(int index, String position, String am, String pr){
        positions.set(index,position);
        amount.set(index, Integer.valueOf(am));
        price.set(index,Integer.valueOf(pr));
}
public void deletePositionValues(int index){
        positions.remove(index);
        amount.remove(index);
        price.remove(index);
}

    public void addPos(String namePos, int amount, int price ){
    getPositions().add(namePos);
    getAmount().add(amount);
    getPrice().add(price);
}


    public List<String> getPositions() {
        return positions;
    }

    public void setPositions(ArrayList<String> positions) {
        this.positions = positions;
    }

    public List<Integer> getAmount() {
        return amount;
    }

    public void setAmount(ArrayList<Integer> amount) {
        this.amount = amount;
    }

    public List<Integer> getPrice() {
        return price;
    }

    public tables(ArrayList<String> positions, ArrayList<Integer> amount, ArrayList<Integer> price) {
        this.positions = positions;
        this.amount = amount;
        this.price = price;
    }

    public void setPrice(ArrayList<Integer> price) {
        this.price = price;
    }



}
