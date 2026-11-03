package com.tuman;

import com.tuman.cash.OrderItem;

/** The original menu uses whole rubles and whole portions. */
public final class TableInput {
    private TableInput() { }

    public static int integer(String text, boolean positive) {
        if (text == null || !text.matches("[0-9]+")) {
            throw new IllegalArgumentException("Количество и цена должны быть целыми числами.");
        }
        try {
            int value = Integer.parseInt(text);
            if (positive && value == 0) throw new IllegalArgumentException("Количество должно быть больше нуля.");
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Введено слишком большое число.", e);
        }
    }

    public static OrderItem item(String name, String quantity, String price) {
        return new OrderItem(name, integer(quantity, true), integer(price, false));
    }
}
