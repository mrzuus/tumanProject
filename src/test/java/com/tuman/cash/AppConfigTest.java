package com.tuman.cash;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {
    @TempDir Path directory;

    @Test void loadsEditableMenuTablesAndPercentageWithoutChangingCode() throws Exception {
        AppConfig config=AppConfig.load(directory);
        config.employeePercent=new BigDecimal("12.5");
        config.products.get(0).name="Новый напиток";
        config.products.get(0).price=333;
        config.tables.get(0).name="У окна";
        new ObjectMapper().writeValue(directory.resolve("settings.json").toFile(),config);
        AppConfig loaded=AppConfig.load(directory);
        assertEquals(new BigDecimal("12.5"),loaded.employeePercent);
        assertEquals(333,loaded.products.get(0).price);
        assertEquals("У окна",loaded.tables.get(0).name);
    }

    @Test void rejectsInvalidAndDuplicateSettingsWithoutOverwritingFile() throws Exception {
        AppConfig config=AppConfig.defaults();
        config.products.get(0).price=null;
        assertThrows(IllegalArgumentException.class,config::validate);
        config=AppConfig.defaults();
        config.tables.get(1).id=config.tables.get(0).id;
        assertThrows(IllegalArgumentException.class,config::validate);
        config=AppConfig.defaults();
        config.employeePercent=new BigDecimal("101");
        assertThrows(IllegalArgumentException.class,config::validate);
        Path file=directory.resolve("settings.json");
        Files.writeString(file,"broken");
        assertThrows(java.io.IOException.class,() -> AppConfig.load(directory));
        assertEquals("broken",Files.readString(file));
    }

    @Test void fractionalPricesAreNotSilentlyRoundedDown() throws Exception {
        String json=new ObjectMapper().writeValueAsString(AppConfig.defaults());
        json=json.replaceFirst("\"price\":\\d+", "\"price\":1.5");
        Path file=directory.resolve("settings.json");
        Files.writeString(file,json);
        assertThrows(java.io.IOException.class,() -> AppConfig.load(directory));
        assertEquals(json,Files.readString(file));
    }
}
