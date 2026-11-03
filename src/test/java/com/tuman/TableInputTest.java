package com.tuman;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableInputTest {
    @Test void rejectsEmptyFractionalNegativeAndOversizedValues() {
        for (String text : new String[]{null, "", " ", "1.5", "1,5", "-1", "abc", "2147483648"}) {
            assertThrows(IllegalArgumentException.class, () -> TableInput.integer(text, false));
        }
        assertThrows(IllegalArgumentException.class, () -> TableInput.integer("0", true));
        assertEquals(0, TableInput.integer("0", false));
        assertEquals(12, TableInput.integer("12", true));
    }

    @Test void validatesNameAndMultiplication() {
        assertThrows(IllegalArgumentException.class, () -> TableInput.item(" ", "1", "100"));
        assertThrows(ArithmeticException.class, () -> TableInput.item("Tea", "2147483647", "2"));
        assertEquals(600, TableInput.item("Чай", "3", "200").total());
    }
}
