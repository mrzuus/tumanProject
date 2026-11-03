package com.tuman.cash;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

public final class AppConfig {
    public int version;
    public BigDecimal employeePercent;
    public List<Product> products;
    public List<Table> tables;

    private static ObjectMapper mapper() {
        return new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
    }

    public String toJson() throws IOException { validate(); return mapper().writeValueAsString(this); }

    public static AppConfig fromJson(String json) throws IOException {
        AppConfig config=mapper().readValue(json,AppConfig.class);
        if (config==null) throw new IOException("Нет настроек кассы.");
        config.validate();
        return config;
    }

    public AppConfig copy() throws IOException { return fromJson(toJson()); }

    public static AppConfig defaults() throws IOException {
        try (InputStream input = AppConfig.class.getResourceAsStream("/default-config.json")) {
            if (input == null) throw new IOException("Не найдены начальные настройки.");
            AppConfig config = mapper().readValue(input, AppConfig.class);
            config.validate();
            return config;
        }
    }

    public static AppConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        Path path = directory.resolve("settings.json");
        if (Files.notExists(path)) {
            AppConfig defaults = defaults();
            try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
                mapper().writerWithDefaultPrettyPrinter().writeValue(output, defaults);
            }
        }
        try {
            AppConfig config = mapper().readValue(path.toFile(), AppConfig.class);
            config.validate();
            return config;
        } catch (RuntimeException | IOException e) {
            throw new IOException("Проверьте настройки в " + path + ": " + e.getMessage(), e);
        }
    }

    public void validate() {
        if (version != 1 || employeePercent == null || employeePercent.signum() < 0
                || employeePercent.compareTo(BigDecimal.valueOf(100)) > 0
                || products == null || products.isEmpty() || tables == null || tables.isEmpty()) {
            throw new IllegalArgumentException("Нужны меню, столы и процент сотрудника от 0 до 100.");
        }
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (Product product : products) {
            if (product == null || !validId(product.id) || !ids.add(product.id)
                    || product.name == null || product.name.isBlank() || !names.add(product.name)
                    || product.price == null || product.price < 0
                    || (product.recipe != null && !product.recipe.matches("[A-Za-z0-9]+\\.fxml"))) {
                throw new IllegalArgumentException("Неверная или повторяющаяся позиция меню.");
            }
        }
        ids.clear();
        for (Table table : tables) {
            if (table == null || !validId(table.id) || !ids.add(table.id)
                    || table.name == null || table.name.isBlank()) {
                throw new IllegalArgumentException("Неверный или повторяющийся стол.");
            }
        }
    }

    private static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9_-]{1,64}"); }

    public static final class Product {
        public String id;
        public String name;
        public Integer price;
        public String recipe;
        @Override public String toString() { return name; }
    }

    public static final class Table {
        public String id;
        public String name;
    }
}
