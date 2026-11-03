package com.tuman;

import com.tuman.cash.*;
import com.tuman.tableControllers.TableController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CashUiTest {
    @TempDir Path directory;

    @BeforeAll static void startJavaFx() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(15,TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    private void onFx(Callable<Void> action) throws Exception {
        FutureTask<Void> task=new FutureTask<>(action);
        Platform.runLater(task); task.get(25,TimeUnit.SECONDS);
    }

    private FXMLLoader table(String id) throws IOException {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/table.fxml"));
        loader.setController(new TableController(id)); loader.load(); return loader;
    }

    @SuppressWarnings("unchecked")
    private void fill(HBox row) {
        ComboBox<AppConfig.Product> box=(ComboBox<AppConfig.Product>)row.getChildren().get(0);
        box.getSelectionModel().select(Main.config.products.stream().filter(p -> p.name.equals("Мохито")).findFirst().orElseThrow());
        TextField quantity=(TextField)row.getChildren().get(1);
        quantity.setText("2");
        ((TextField)row.getChildren().get(2)).setText("250");
        quantity.setText("1.5");
        assertEquals("2",quantity.getText());
    }

    @Test void allConfiguredTablesIncludingFifthUseOneViewAndPersistOrders() throws Exception {
        Main.config=AppConfig.defaults();
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            Main.register=register;
            for (String id:register.tableIds()) {
                onFx(() -> {
                    FXMLLoader loader=table(id);
                    fill((HBox)((VBox)loader.getNamespace().get("rows")).getChildren().get(0));
                    assertEquals("К добавлению: 500 ₽",((Label)loader.getNamespace().get("totalSum")).getText());
                    ((Button)loader.getNamespace().get("addPos")).fire();
                    return null;
                });
                FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
                assertEquals(500,CashState.total(register.order(id)));
            }
            onFx(() -> {
                FXMLLoader menu=new FXMLLoader(getClass().getResource("/tableMenu.fxml")); menu.load();
                assertEquals(Main.config.tables.size(),((FlowPane)menu.getNamespace().get("tables")).getChildren().size());
                FXMLLoader products=new FXMLLoader(getClass().getResource("/coctailMenu.fxml")); products.load();
                assertEquals(Main.config.products.size(),((VBox)products.getNamespace().get("products")).getChildren().size());
                return null;
            });
        } finally { Main.register=null; }
    }

    @Test void draftHasNoFourRowLimit() throws Exception {
        Main.config=AppConfig.defaults();
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            Main.register=register;
            onFx(() -> {
                FXMLLoader loader=table("5");
                TableController controller=loader.getController();
                for (int i=0;i<7;i++) controller.addRow();
                VBox rows=(VBox)loader.getNamespace().get("rows");
                assertEquals(8,rows.getChildren().size());
                for (var node:rows.getChildren()) fill((HBox)node);
                ((Button)loader.getNamespace().get("addPos")).fire();
                return null;
            });
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            assertEquals(8,register.order("5").size());
            assertEquals(4000,CashState.total(register.order("5")));
        } finally { Main.register=null; }
    }

    @Test void mainScreenShowsCorrectRevenueAndConfiguredEmployeePercentage() throws Exception {
        Main.config=AppConfig.defaults(); Main.config.employeePercent=new java.math.BigDecimal("12.5");
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            register.addItems("1",List.of(new OrderItem("Чай",1,100))); register.pay("1",CashState.PaymentMethod.CASH);
            register.addItems("2",List.of(new OrderItem("Мохито",2,250))); register.pay("2",CashState.PaymentMethod.TRANSFER);
        }
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            Main.register=register;
            onFx(() -> {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/mainUI.fxml")); loader.load();
                assertEquals("500",((Text)loader.getNamespace().get("revenueTransfer")).getText());
                assertEquals("100",((Text)loader.getNamespace().get("revenueCash")).getText());
                assertEquals("600",((Text)loader.getNamespace().get("revenue")).getText());
                assertEquals("75",((Text)loader.getNamespace().get("revenueStuff")).getText());
                new FXMLLoader(getClass().getResource("/confirmCloseShift.fxml")).load();
                return null;
            });
        } finally { Main.register=null; }
    }

    static class SlowStore extends StateStore {
        final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
        boolean block;
        SlowStore(Path directory) throws IOException { super(directory); }
        @Override protected void beforeCommit() throws IOException {
            if (!block) return;
            entered.countDown();
            try { if (!release.await(10,TimeUnit.SECONDS)) throw new IOException("Timed out"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
        }
    }

    @Test void slowDiskOperationDoesNotBlockJavaFxThread() throws Exception {
        Main.config=AppConfig.defaults();
        SlowStore store=new SlowStore(directory);
        try (CashRegister register=new CashRegister(store)) {
            Main.register=register; store.block=true;
            onFx(() -> {
                FXMLLoader loader=table("1");
                fill((HBox)((VBox)loader.getNamespace().get("rows")).getChildren().get(0));
                ((Button)loader.getNamespace().get("addPos")).fire();
                return null;
            });
            assertTrue(store.entered.await(5,TimeUnit.SECONDS));
            try { onFx(() -> { assertTrue(FxJobs.isBusy()); return null; }); }
            finally { store.release.countDown(); }
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            assertEquals(500,CashState.total(register.order("1")));
        } finally { store.release.countDown(); Main.register=null; }
    }

    @Test void settingsScreenSavesPercentageAndHistoryScreenShowsRefundAndAudit() throws Exception {
        Main.config=AppConfig.defaults();
        try (CashRegister register=new CashRegister(new StateStore(directory))) {
            Main.register=register;
            register.addItems("5",List.of(new OrderItem("Чай",1,100)));
            register.pay("5",CashState.PaymentMethod.CASH);
            String id=register.snapshot().receipts.get(0).id;
            onFx(() -> {
                FXMLLoader settings=new FXMLLoader(getClass().getResource("/settings.fxml")); settings.load();
                ((TextField)settings.getNamespace().get("percent")).setText("20");
                ((Button)settings.getNamespace().get("saveSettings")).fire();
                return null;
            });
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            assertEquals(new java.math.BigDecimal("20"),register.settings().employeePercent);
            assertEquals(new java.math.BigDecimal("20"),Main.config.employeePercent);
            FXMLLoader[] loaded=new FXMLLoader[1];
            onFx(() -> {
                loaded[0]=new FXMLLoader(getClass().getResource("/history.fxml")); loaded[0].load(); return null;
            });
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            onFx(() -> {
                ListView<?> receipts=(ListView<?>)loaded[0].getNamespace().get("receipts");
                assertEquals(1,receipts.getItems().size());
                assertEquals(id,((History.ReceiptRow)receipts.getItems().get(0)).id);
                ((HistoryController)loaded[0].getController()).refreshAudit(); return null;
            });
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            onFx(() -> {
                assertEquals(3,((ListView<?>)loaded[0].getNamespace().get("audit")).getItems().size()); return null;
            });
            register.refund(id,"Проверка возврата","Кассир");
            onFx(() -> { ((HistoryController)loaded[0].getController()).find(); return null; });
            FxJobs.whenIdle().get(15,TimeUnit.SECONDS);
            onFx(() -> {
                History.ReceiptRow row=(History.ReceiptRow)((ListView<?>)loaded[0].getNamespace().get("receipts")).getItems().get(0);
                assertNotNull(row.refundId); return null;
            });
        } finally { Main.register=null; }
    }
}
