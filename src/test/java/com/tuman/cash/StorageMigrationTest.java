package com.tuman.cash;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StorageMigrationTest {
    @TempDir Path directory;

    private CashState legacy() throws IOException {
        CashState state=CashState.empty();
        for (String id:List.of("1","2","3","4","VIP")) state.tables.put(id,new ArrayList<>());
        state.tables.get("1").add(new OrderItem("Чай",2,100));
        CashState.Receipt receipt=new CashState.Receipt();
        receipt.id=UUID.randomUUID().toString(); receipt.shiftId=state.shiftId; receipt.tableId="2";
        receipt.paidAt=Instant.now().toString(); receipt.method=CashState.PaymentMethod.TRANSFER;
        receipt.items=List.of(new OrderItem("Мохито",2,250)); receipt.total=500;
        state.receipts.add(receipt);
        state.validate();
        return state;
    }

    @Test void missingLegacyMainWithExistingBackupNeverStartsEmpty() throws Exception {
        Path backup=directory.resolve("cash-state.previous.json");
        new ObjectMapper().writeValue(backup.toFile(),legacy());
        byte[] original=Files.readAllBytes(backup);
        try (StateStore store=new StateStore(directory)) {
            IOException error=assertThrows(IOException.class,() -> new CashRegister(store));
            assertTrue(error.getMessage().contains("резервная копия"));
        }
        assertArrayEquals(original,Files.readAllBytes(backup));
        assertFalse(Files.exists(directory.resolve("cash.sqlite")));
    }

    @Test void legacyMigrationKeepsOriginalAndTransfersOrdersPaymentsAndFifthTable() throws Exception {
        Path source=directory.resolve("cash-state.json");
        CashState old=legacy();
        new ObjectMapper().writeValue(source.toFile(),old);
        byte[] original=Files.readAllBytes(source);
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            assertEquals(200,CashState.total(register.order("1")));
            assertEquals(500,register.revenue(CashState.PaymentMethod.TRANSFER));
            assertEquals(old.receipts.get(0).id,register.snapshot().receipts.get(0).id);
            assertTrue(register.tableIds().contains("5"));
            register.addItems("5",List.of(new OrderItem("Чай",1,100)));
            register.pay("5",CashState.PaymentMethod.CARD);
        }
        assertArrayEquals(original,Files.readAllBytes(source));
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            assertEquals(600,register.revenue(null));
            assertEquals(2,register.snapshot().receipts.size());
        }
    }

    @Test void missingDatabaseNeverFallsBackToStaleLegacyOrEmptyState() throws Exception {
        new ObjectMapper().writeValue(directory.resolve("cash-state.json").toFile(),legacy());
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            register.pay("1",CashState.PaymentMethod.CASH);
        }
        Files.move(directory.resolve("cash.sqlite"),directory.resolve("cash.missing-for-test.sqlite"));
        try (StateStore store=new StateStore(directory)) {
            IOException error=assertThrows(IOException.class,() -> new CashRegister(store));
            assertTrue(error.getMessage().contains("Касса не обнулена"));
        }
        assertFalse(Files.exists(directory.resolve("cash.sqlite")));
        assertTrue(Files.exists(directory.resolve("cash.previous.sqlite")));
    }

    @Test void databaseCorruptionIsNotReplaced() throws Exception {
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            register.addItems("5",List.of(new OrderItem("Чай",1,100)));
        }
        Path db=directory.resolve("cash.sqlite");
        Files.writeString(db,"corrupted database");
        try (StateStore store=new StateStore(directory)) {
            assertThrows(IOException.class,() -> new CashRegister(store));
        }
        assertEquals("corrupted database",Files.readString(db));
    }

    @Test void changingConfiguredTablesPreservesHistoryAndRefusesUnpaidRemoval() throws Exception {
        AppConfig config=AppConfig.defaults();
        AppConfig.Table terrace=new AppConfig.Table(); terrace.id="terrace"; terrace.name="Терраса";
        config.tables.add(terrace);
        try (CashRegister register=new CashRegister(new StateStore(directory),config)) {
            register.addItems("terrace",List.of(new OrderItem("Чай",1,100)));
        }
        try (StateStore store=new StateStore(directory)) {
            assertThrows(IOException.class,() -> new CashRegister(store,AppConfig.defaults()));
        }
        try (CashRegister register=new CashRegister(new StateStore(directory),config)) {
            assertEquals(100,CashState.total(register.order("terrace")));
            register.pay("terrace",CashState.PaymentMethod.CASH);
        }
        try (CashRegister register=new CashRegister(new StateStore(directory),AppConfig.defaults())) {
            assertFalse(register.tableIds().contains("terrace"));
            assertEquals(100,register.revenue(null));
            assertEquals("terrace",register.snapshot().receipts.get(0).tableId);
        }
    }

    @Test void changingOrderDoesNotRewriteOrLoadArchivedReceipts() throws Exception {
        try (StateStore store=new StateStore(directory)) {
            CashRegister register=new CashRegister(store);
            store.transaction(c -> {
                CashState.Receipt receipt=legacy().receipts.get(0);
                receipt.shiftId=UUID.randomUUID().toString();
                for (int i=0;i<10000;i++) {
                    receipt.id=UUID.randomUUID().toString();
                    StateStore.insertReceipt(c,receipt);
                }
                StateStore.execute(c,
                        "CREATE TRIGGER no_history_update BEFORE UPDATE ON receipts BEGIN SELECT RAISE(ABORT,'History rewrite'); END",
                        "CREATE TRIGGER no_history_delete BEFORE DELETE ON receipts BEGIN SELECT RAISE(ABORT,'History delete'); END",
                        "CREATE TRIGGER no_history_item_update BEFORE UPDATE ON receipt_items BEGIN SELECT RAISE(ABORT,'History rewrite'); END",
                        "CREATE TRIGGER no_history_item_delete BEFORE DELETE ON receipt_items BEGIN SELECT RAISE(ABORT,'History delete'); END");
                return null;
            });
            register.addItems("5",List.of(new OrderItem("Чай",1,100)));
            register.changeItem("5",0,new OrderItem("Чай",2,100));
            register.pay("5",CashState.PaymentMethod.CARD);
            assertEquals(200,register.revenue(null));
            assertEquals(10001,store.read(c -> {
                try (Statement ps=c.createStatement();ResultSet rs=ps.executeQuery("SELECT COUNT(*) FROM receipts")) {
                    rs.next(); return rs.getInt(1);
                }
            }).intValue());
        }
    }
}
