package com.tuman.cash;

import java.util.*;

public final class CashState {
    public int version;
    public String shiftId;
    public Map<String, List<OrderItem>> tables;
    public List<Receipt> receipts;

    public static CashState empty() {
        CashState state = new CashState();
        state.version = 1;
        state.shiftId = UUID.randomUUID().toString();
        state.tables = new LinkedHashMap<>();
        state.receipts = new ArrayList<>();
        return state;
    }

    public void validate() {
        if (version != 1 || shiftId == null || tables == null || receipts == null
                || tables.keySet().stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("Неверный формат сохранённых данных кассы.");
        }
        UUID.fromString(shiftId);
        tables.values().forEach(CashState::total);
        Set<String> ids = new HashSet<>();
        for (Receipt receipt : receipts) {
            if (receipt == null || receipt.id == null || !ids.add(receipt.id)
                    || receipt.shiftId == null || receipt.paidAt == null
                    || receipt.tableId == null || receipt.tableId.isBlank() || receipt.method == null
                    || receipt.items == null || receipt.items.isEmpty()) {
                throw new IllegalArgumentException("Повреждена история оплат.");
            }
            UUID.fromString(receipt.id);
            UUID.fromString(receipt.shiftId);
            java.time.Instant.parse(receipt.paidAt);
            if (total(receipt.items) != receipt.total) {
                throw new IllegalArgumentException("Сумма чека не совпадает с его позициями.");
            }
        }
    }

    public static int total(List<OrderItem> items) {
        if (items == null) throw new IllegalArgumentException("Нет списка позиций.");
        int sum = 0;
        for (OrderItem item : items) {
            if (item == null) throw new IllegalArgumentException("Пустая позиция.");
            item.validate();
            sum = Math.addExact(sum, item.total());
        }
        return sum;
    }

    public CashState copy() {
        CashState copy = new CashState();
        copy.version = version;
        copy.shiftId = shiftId;
        copy.tables = new LinkedHashMap<>();
        tables.forEach((id, items) -> {
            List<OrderItem> values = new ArrayList<>();
            items.forEach(item -> values.add(item.copy()));
            copy.tables.put(id, values);
        });
        copy.receipts = new ArrayList<>();
        receipts.forEach(receipt -> copy.receipts.add(receipt.copy()));
        return copy;
    }

    public enum PaymentMethod {
        CASH("Наличные"), CARD("Безнал"), TRANSFER("Перевод");
        public final String label;
        PaymentMethod(String label) { this.label=label; }
    }

    public static final class Receipt {
        public String id;
        public String shiftId;
        public String tableId;
        public String paidAt;
        public PaymentMethod method;
        public int total;
        public List<OrderItem> items;

        public Receipt copy() {
            Receipt copy = new Receipt();
            copy.id = id;
            copy.shiftId = shiftId;
            copy.tableId = tableId;
            copy.paidAt = paidAt;
            copy.method = method;
            copy.total = total;
            copy.items = new ArrayList<>();
            items.forEach(item -> copy.items.add(item.copy()));
            return copy;
        }
    }
}
