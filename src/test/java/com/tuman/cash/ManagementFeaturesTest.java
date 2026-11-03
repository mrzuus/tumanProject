package com.tuman.cash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ManagementFeaturesTest {
    @TempDir Path directory;

    private String paid(CashRegister register,int price,CashState.PaymentMethod method) throws IOException {
        register.addItems("1",List.of(new OrderItem("Мохито",1,price)));
        register.pay("1",method);
        return register.snapshot().receipts.get(register.snapshot().receipts.size()-1).id;
    }

    @Test void settingsSaveIsPersistentAndDoesNotRepriceExistingOrdersOrReceipts() throws Exception {
        String receipt;
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            receipt=paid(register,250,CashState.PaymentMethod.CARD);
            register.addItems("5",List.of(new OrderItem("Мохито",1,250)));
            AppConfig config=register.settings();
            config.products.stream().filter(product -> product.name.equals("Мохито")).findFirst().orElseThrow().price=999;
            config.employeePercent=new java.math.BigDecimal("15");
            AppConfig.Table extra=new AppConfig.Table(); extra.id="terrace"; extra.name="Терраса"; config.tables.add(extra);
            register.saveSettings(config,"Кассир");
            assertEquals(250,register.order("5").get(0).price);
            assertEquals(250,register.receipt(receipt).items.get(0).price);
            assertTrue(register.tableIds().contains("terrace"));
            assertEquals("Настройки",register.audit(0).rows.get(0).action);
            assertTrue(register.audit(0).rows.get(0).details.contains("999"));
        }
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            assertEquals(999,register.settings().products.stream().filter(p -> p.name.equals("Мохито")).findFirst().orElseThrow().price);
            assertEquals(new java.math.BigDecimal("15"),register.settings().employeePercent);
            assertTrue(register.tableIds().contains("terrace"));
        }
    }

    @Test void unpaidTableRemovalAndFailedSettingsWriteRollBackEverything() throws Exception {
        CashRegisterTest.FailingStore store=new CashRegisterTest.FailingStore(directory);
        try (CashRegister register=new CashRegister(store)) {
            register.addItems("5",List.of(new OrderItem("Чай",1,100)));
            AppConfig config=register.settings(); config.tables.removeIf(table -> table.id.equals("5"));
            int auditCount=register.audit(0).rows.size();
            assertThrows(IOException.class,() -> register.saveSettings(config,"Кассир"));
            assertTrue(register.tableIds().contains("5"));
            assertEquals(auditCount,register.audit(0).rows.size());
            AppConfig modified=register.settings(); modified.employeePercent=new java.math.BigDecimal("20");
            store.fail=true;
            assertThrows(IOException.class,() -> register.saveSettings(modified,"Кассир"));
            assertEquals(new java.math.BigDecimal("10"),register.settings().employeePercent);
            assertEquals(auditCount,register.audit(0).rows.size());
        }
    }

    @Test void fullRefundKeepsOriginalAndSurvivesRestartWithoutDuplicate() throws Exception {
        String id;
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            id=paid(register,500,CashState.PaymentMethod.TRANSFER);
            CashState.Receipt original=register.snapshot().receipts.get(0).copy();
            assertThrows(IllegalArgumentException.class,() -> register.refund(id," ","Кассир"));
            register.refund(id,"Ошибка оплаты","Кассир");
            assertEquals(0,register.revenue(null));
            assertEquals(0,register.revenue(CashState.PaymentMethod.TRANSFER));
            assertEquals(original.total,register.snapshot().receipts.get(0).total);
            assertEquals(original.items.get(0).price,register.snapshot().receipts.get(0).items.get(0).price);
            assertEquals(original.paidAt,register.snapshot().receipts.get(0).paidAt);
            assertNotNull(register.receipt(id).receipt.refundId);
            assertEquals("Ошибка оплаты",register.receipt(id).receipt.reason);
            assertEquals("Возврат",register.audit(0).rows.get(0).action);
            assertThrows(IllegalArgumentException.class,() -> register.refund(id,"Ещё раз","Кассир"));
        }
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            assertEquals(0,register.revenue(null));
            assertNotNull(register.receipt(id).receipt.refundId);
            assertThrows(IllegalArgumentException.class,() -> register.refund(id,"Повтор","Кассир"));
        }
    }

    @Test void allPaymentMethodsAndFailedRefundAreHandledAtomically() throws Exception {
        CashRegisterTest.FailingStore store=new CashRegisterTest.FailingStore(directory);
        try (CashRegister register=new CashRegister(store)) {
            for (CashState.PaymentMethod method:CashState.PaymentMethod.values()) {
                String id=paid(register,500,method);
                int events=register.audit(0).rows.size();
                store.fail=true;
                assertThrows(IOException.class,() -> register.refund(id,"Ошибка","Кассир"));
                assertEquals(500,register.revenue(method));
                assertNull(register.receipt(id).receipt.refundId);
                assertEquals(events,register.audit(0).rows.size());
                store.fail=false;
                register.refund(id,"Ошибка","Кассир");
                assertEquals(0,register.revenue(method));
            }
            assertEquals(0,register.revenue(null));
        }
    }

    @Test void oldShiftRefundAffectsCurrentShiftAndDoesNotRewriteOldReport() throws Exception {
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            String id=paid(register,500,CashState.PaymentMethod.CASH);
            Path oldReport=register.closeShift(LocalDate.of(2026,11,3));
            byte[] original=Files.readAllBytes(oldReport);
            register.refund(id,"Возврат гостю","Кассир");
            assertEquals(-500,register.revenue(null));
            Path newReport=register.closeShift(LocalDate.of(2026,11,4));
            assertArrayEquals(original,Files.readAllBytes(oldReport));
            String text=Files.readString(newReport);
            assertTrue(text.contains("Всего: -500"));
            assertTrue(text.contains("Возврат гостю"));
            assertTrue(text.contains(id));
            assertEquals(0,register.revenue(null));
        }
    }

    @Test void refundThenNewSaleUsesNetRevenueWithoutGrossOverflow() throws Exception {
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            String id=paid(register,Integer.MAX_VALUE,CashState.PaymentMethod.CARD);
            register.refund(id,"Отмена","Кассир");
            paid(register,Integer.MAX_VALUE,CashState.PaymentMethod.CARD);
            assertEquals(Integer.MAX_VALUE,register.revenue(null));
            assertEquals(2,register.snapshot().receipts.size());
        }
    }

    private void seed(StateStore store,String name,String time,CashState.PaymentMethod method) throws IOException {
        store.transaction(c -> {
            CashState.Receipt receipt=new CashState.Receipt(); receipt.id=UUID.randomUUID().toString();
            receipt.shiftId=StateStore.shiftId(c); receipt.tableId="1"; receipt.paidAt=time; receipt.method=method;
            receipt.total=100; receipt.items=List.of(new OrderItem(name,1,100)); StateStore.insertReceipt(c,receipt);
            return null;
        });
    }

    @Test void historyHandlesLocalDateBoundariesUnicodeSearchLiteralWildcardsAndPaging() throws Exception {
        try (StateStore store=new StateStore(directory)) {
            CashRegister register=new CashRegister(store);
            seed(store,"Мохито_100%","2026-11-02T20:59:59.999Z",CashState.PaymentMethod.CARD);
            seed(store,"Мохито_100%","2026-11-02T21:00:00Z",CashState.PaymentMethod.CARD);
            seed(store,"Мохито_100%","2026-11-02T21:00:00.001Z",CashState.PaymentMethod.TRANSFER);
            seed(store,"Мохито_100%","2026-11-03T20:59:59.999999999Z",CashState.PaymentMethod.CARD);
            seed(store,"Другой","2026-11-03T21:00:00Z",CashState.PaymentMethod.CASH);
            History.Page page=register.history(LocalDate.of(2026,11,3),LocalDate.of(2026,11,3),ZoneId.of("Europe/Moscow"),"мохито_100%",null,0);
            assertEquals(3,page.rows.size());
            assertEquals("2026-11-03T20:59:59.999999999Z",page.rows.get(0).paidAt);
            assertEquals(2,register.history(LocalDate.of(2026,11,3),LocalDate.of(2026,11,3),ZoneId.of("Europe/Moscow"),"",CashState.PaymentMethod.CARD,0).rows.size());
            assertEquals(4,register.history(null,null,ZoneOffset.UTC,"_",null,0).rows.size());
            assertEquals(4,register.history(null,null,ZoneOffset.UTC,"%",null,0).rows.size());
            for (int i=0;i<60;i++) seed(store,"Чай",Instant.parse("2026-11-05T00:00:00Z").plusSeconds(i).toString(),CashState.PaymentMethod.CASH);
            History.Page first=register.history(null,null,ZoneOffset.UTC,"ЧАЙ",null,0);
            History.Page second=register.history(null,null,ZoneOffset.UTC,"ЧАЙ",null,50);
            assertEquals(50,first.rows.size()); assertTrue(first.hasNext);
            assertEquals(10,second.rows.size()); assertFalse(second.hasNext);
            Set<String> ids=new HashSet<>(); first.rows.forEach(row -> ids.add(row.id));
            assertTrue(second.rows.stream().noneMatch(row -> ids.contains(row.id)));
        }
    }

    @Test void schemaTwoDatabaseUpgradesWithBackupAndKeepsExistingSales() throws Exception {
        String id;
        try (CashRegister register=new CashRegister(new StateStore(directory))) { id=paid(register,250,CashState.PaymentMethod.CARD); }
        try (Connection c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("cash.sqlite"))) {
            StateStore.execute(c,"DROP TABLE refunds","DROP TABLE audit_log","DROP INDEX receipt_history_time","ALTER TABLE receipts DROP COLUMN paid_at_ms","DELETE FROM metadata WHERE key='settings_json'","PRAGMA user_version=2");
        }
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            assertEquals(250,register.revenue(null));
            assertEquals(250,register.receipt(id).receipt.total);
            register.refund(id,"Проверка переноса","Кассир");
            assertEquals(0,register.revenue(null));
        }
        try (Connection c=DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("cash.previous.sqlite"));
             Statement ps=c.createStatement();ResultSet rs=ps.executeQuery("PRAGMA user_version")) {
            rs.next(); assertEquals(2,rs.getInt(1));
        }
    }
}
