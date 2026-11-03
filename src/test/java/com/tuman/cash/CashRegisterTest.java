package com.tuman.cash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import com.tuman.cash.CashState.PaymentMethod;

class CashRegisterTest {
    @TempDir Path directory;

    private OrderItem drink() { return new OrderItem("Мохито", 2, 250); }

    @Test void ordersEditsAndRemovalsSurviveRestart() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(drink(), new OrderItem("Чай", 1, 100)));
            register.changeItem("1", 0, new OrderItem("Мохито", 3, 250));
            register.removeItem("1", 1);
        }
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            assertEquals(750, CashState.total(register.snapshot().tables.get("1")));
            assertEquals("Мохито", register.snapshot().tables.get("1").get(0).name);
        }
    }

    @Test void allTablesAndPaymentMethodsSurviveRestartWithReceipts() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            for (String id : register.tableIds()) {
                for (PaymentMethod method : PaymentMethod.values()) {
                    register.addItems(id, List.of(drink()));
                    register.pay(id, method);
                }
            }
        }
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            assertEquals(3000, register.revenue(PaymentMethod.CASH));
            assertEquals(3000, register.revenue(PaymentMethod.CARD));
            assertEquals(3000, register.revenue(PaymentMethod.TRANSFER));
            assertEquals(9000, register.revenue(null));
            assertEquals(18, register.snapshot().receipts.size());
            assertTrue(register.snapshot().tables.values().stream().allMatch(List::isEmpty));
            assertEquals(2, register.snapshot().receipts.get(0).items.get(0).quantity);
        }
    }

    @Test void duplicatePaymentIsRejected() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(drink()));
            register.pay("1", PaymentMethod.TRANSFER);
            assertThrows(IllegalArgumentException.class, () -> register.pay("1", PaymentMethod.TRANSFER));
            assertEquals(500, register.revenue(null));
            assertEquals(1, register.snapshot().receipts.size());
        }
    }

    static class FailingStore extends StateStore {
        boolean fail;
        boolean failReport;
        FailingStore(Path path) throws IOException { super(path); }
        @Override protected void beforeCommit() throws IOException {
            if (fail) throw new IOException("Disk full");
        }
        @Override public Path writeReport(String name, String text) throws IOException {
            if (failReport) throw new IOException("Report storage unavailable");
            return super.writeReport(name, text);
        }
    }

    @Test void failedPaymentKeepsOrderAndRevenueOnDiskAndInMemory() throws Exception {
        FailingStore store = new FailingStore(directory);
        try (CashRegister register = new CashRegister(store)) {
            register.addItems("VIP", List.of(drink()));
            String before = register.snapshot().shiftId;
            store.fail = true;
            assertThrows(IOException.class, () -> register.pay("VIP", PaymentMethod.CARD));
            assertEquals(500, CashState.total(register.snapshot().tables.get("VIP")));
            assertEquals(0, register.revenue(null));
            assertTrue(register.snapshot().receipts.isEmpty());
            assertEquals(before, register.snapshot().shiftId);
        }
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            assertEquals(500, CashState.total(register.snapshot().tables.get("VIP")));
        }
    }

    @Test void failedEditAndAddAreNotApplied() throws Exception {
        FailingStore store = new FailingStore(directory);
        try (CashRegister register = new CashRegister(store)) {
            register.addItems("1", List.of(drink()));
            store.fail = true;
            assertThrows(IOException.class, () -> register.addItems("1", List.of(drink())));
            assertThrows(IOException.class, () -> register.changeItem("1", 0, new OrderItem("Чай", 1, 100)));
            assertThrows(IOException.class, () -> register.removeItem("1", 0));
            assertEquals(500, CashState.total(register.snapshot().tables.get("1")));
        }
    }

    @Test void invalidBatchAndOverflowLeaveAllPositionsUnchanged() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            OrderItem invalid = drink();
            invalid.quantity = 0;
            assertThrows(IllegalArgumentException.class, () -> register.addItems("1", List.of(drink(), invalid)));
            assertTrue(register.snapshot().tables.get("1").isEmpty());
            assertThrows(ArithmeticException.class, () -> register.addItems("1",
                    List.of(new OrderItem("A", 1, Integer.MAX_VALUE), new OrderItem("B", 1, 1))));
            assertTrue(register.snapshot().tables.get("1").isEmpty());
        }
    }

    @Test void dailyRevenueOverflowDoesNotEraseUnpaidOrder() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(new OrderItem("A", 1, Integer.MAX_VALUE)));
            register.pay("1", PaymentMethod.CASH);
            register.addItems("2", List.of(drink()));
            assertThrows(ArithmeticException.class, () -> register.pay("2", PaymentMethod.TRANSFER));
            assertEquals(500, CashState.total(register.snapshot().tables.get("2")));
            assertEquals(Integer.MAX_VALUE, register.revenue(null));
        }
    }

    @Test void damagedOrIncompleteStateIsNotSilentlyReset() throws Exception {
        Path file = directory.resolve("cash-state.json");
        for (String invalid : List.of("broken", "{}", "null", "{\"version\":1}")) {
            Files.writeString(file, invalid);
            try (StateStore store = new StateStore(directory)) {
                assertThrows(IOException.class, () -> new CashRegister(store));
            }
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test void previousStateIsBackedUpAndSecondProcessCannotOpenData() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(drink()));
            String before = register.snapshot().shiftId;
            register.pay("1", PaymentMethod.CASH);
            assertTrue(Files.exists(directory.resolve("cash.previous.sqlite")));
            assertThrows(IOException.class, () -> new StateStore(directory));
        }
        try (StateStore reopened = new StateStore(directory)) { assertNotNull(reopened.load()); }
    }

    @Test void reportsSurviveClosureAndSameDayReportsAreNotOverwritten() throws Exception {
        Path first;
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(drink()));
            register.pay("1", PaymentMethod.TRANSFER);
            first = register.closeShift(LocalDate.of(2026, 11, 3));
            String report = Files.readString(first);
            assertTrue(report.contains("Переводом: 500"));
            assertTrue(report.contains("Мохито × 2 × 250 = 500"));
            assertEquals(0, register.revenue(null));
            assertEquals(1, register.snapshot().receipts.size());
            Path second = register.closeShift(LocalDate.of(2026, 11, 3));
            assertNotEquals(first, second);
            assertEquals(report, Files.readString(first));
            assertTrue(Files.exists(second));
        }
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            assertEquals(0, register.revenue(null));
            assertEquals(1, register.snapshot().receipts.size());
            assertTrue(Files.exists(first));
        }
    }

    @Test void reportFailureDoesNotCloseShiftOrResetTotals() throws Exception {
        FailingStore store = new FailingStore(directory);
        try (CashRegister register = new CashRegister(store)) {
            register.addItems("1", List.of(drink()));
            register.pay("1", PaymentMethod.CARD);
            String shift = register.snapshot().shiftId;
            store.failReport = true;
            assertThrows(IOException.class, () -> register.closeShift(LocalDate.now()));
            assertEquals(shift, register.snapshot().shiftId);
            assertEquals(500, register.revenue(null));
        }
    }

    @Test void stateFailureAfterReportKeepsLocalReportAndOriginalShift() throws Exception {
        FailingStore store = new FailingStore(directory);
        try (CashRegister register = new CashRegister(store)) {
            register.addItems("1", List.of(drink()));
            register.pay("1", PaymentMethod.CARD);
            String shift = register.snapshot().shiftId;
            store.fail = true;
            IOException error = assertThrows(IOException.class, () -> register.closeShift(LocalDate.now()));
            assertTrue(error.getMessage().contains("смена не закрыта"));
            assertEquals(shift, register.snapshot().shiftId);
            assertEquals(500, register.revenue(null));
            try (var reports = Files.list(directory.resolve("reports"))) {
                Path report = reports.findFirst().orElseThrow();
                assertTrue(Files.readString(report).contains("Безналом: 500"));
            }
        }
    }

    @Test void openOrdersPreventClosingShift() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("4", List.of(drink()));
            assertThrows(IllegalArgumentException.class, () -> register.closeShift(LocalDate.now()));
            assertFalse(Files.exists(directory.resolve("reports")));
        }
    }

    @Test void snapshotsCannotMutateRegister() throws Exception {
        try (CashRegister register = new CashRegister(new StateStore(directory))) {
            register.addItems("1", List.of(drink()));
            register.snapshot().tables.get("1").get(0).price = 0;
            assertEquals(500, CashState.total(register.snapshot().tables.get("1")));
        }
    }
}
