package com.tuman.cash;

public final class OrderItem {
    public String name;
    public int quantity;
    public int price;

    public OrderItem() { }

    public OrderItem(String name, int quantity, int price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        validate();
    }

    public void validate() {
        if (name == null || name.trim().isEmpty() || quantity <= 0 || price < 0) {
            throw new IllegalArgumentException("Укажите название, целое количество больше нуля и цену от нуля.");
        }
        Math.multiplyExact(quantity, price);
    }

    public int total() { return Math.multiplyExact(quantity, price); }

    public OrderItem copy() { return new OrderItem(name, quantity, price); }
}
