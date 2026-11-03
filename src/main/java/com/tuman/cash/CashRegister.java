package com.tuman.cash;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import com.tuman.cash.CashState.PaymentMethod;

/** Business operations touch the selected order and current shift, never copy all history. */
public final class CashRegister implements AutoCloseable {
    private final StateStore store;

    public CashRegister(StateStore store) throws IOException { this(store, null); }
    public CashRegister(StateStore store, AppConfig config) throws IOException {
        this.store=store;
        store.initialize(config);
    }

    /** Full export for diagnostics/tests; normal screens use order() and tableIds(). */
    public CashState snapshot() throws IOException { return store.load(); }

    public List<String> tableIds() throws IOException {
        return store.read(c -> {
            List<String> ids=new ArrayList<>();
            try (Statement ps=c.createStatement(); ResultSet rs=ps.executeQuery("SELECT id FROM dining_tables WHERE active=1 ORDER BY sort_order")) {
                while (rs.next()) ids.add(rs.getString(1));
            }
            return ids;
        });
    }

    private void requireTable(Connection c, String id) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT 1 FROM dining_tables WHERE id=? AND active=1")) {
            ps.setString(1,id);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Неизвестный или отключённый стол.");
            }
        }
    }

    public List<OrderItem> order(String id) throws IOException {
        return store.read(c -> { requireTable(c,id); return StateStore.items(c,id); });
    }

    private String tableName(Connection c,String id) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT name FROM dining_tables WHERE id=?")) {
            ps.setString(1,id);
            try (ResultSet rs=ps.executeQuery()) { return rs.next() ? rs.getString(1) : id; }
        }
    }

    public void addItems(String id, List<OrderItem> items) throws IOException {
        if (items==null || items.isEmpty()) throw new IllegalArgumentException("Выберите хотя бы одну позицию.");
        List<OrderItem> checked=new ArrayList<>();
        for (OrderItem item:items) checked.add(item.copy());
        store.transaction(c -> {
            requireTable(c,id);
            List<OrderItem> order=StateStore.items(c,id);
            order.addAll(checked);
            CashState.total(order);
            StateStore.replaceOrder(c,id,order);
            StringJoiner added=new StringJoiner("; ");
            checked.forEach(item -> added.add(item.name+" × "+item.quantity+" × "+item.price+" ₽"));
            audit(c,currentActor(),"Заказ",id,tableName(c,id)+". Добавлено: "+added+". Сумма заказа: "+CashState.total(order)+" ₽");
            return null;
        });
    }

    public void changeItem(String id, int index, OrderItem item) throws IOException {
        OrderItem checked=item.copy();
        store.transaction(c -> {
            requireTable(c,id);
            List<OrderItem> order=StateStore.items(c,id);
            OrderItem before=order.set(index,checked);
            CashState.total(order);
            StateStore.replaceOrder(c,id,order);
            audit(c,currentActor(),"Заказ",id,tableName(c,id)+". Позиция "+(index+1)+": "+before.name+", "+before.quantity+" × "+before.price
                    +" ₽ → "+checked.name+", "+checked.quantity+" × "+checked.price+" ₽");
            return null;
        });
    }

    public void removeItem(String id, int index) throws IOException {
        store.transaction(c -> {
            requireTable(c,id);
            List<OrderItem> order=StateStore.items(c,id);
            OrderItem removed=order.remove(index);
            StateStore.replaceOrder(c,id,order);
            audit(c,currentActor(),"Заказ",id,tableName(c,id)+". Удалена позиция: "+removed.name+", "+removed.quantity+" × "+removed.price+" ₽");
            return null;
        });
    }

    public void pay(String id, PaymentMethod method) throws IOException {
        if (method==null) throw new IllegalArgumentException("Выберите способ оплаты.");
        store.transaction(c -> {
            requireTable(c,id);
            List<OrderItem> items=StateStore.items(c,id);
            if (items.isEmpty()) throw new IllegalArgumentException("Стол уже оплачен или заказ пуст.");
            CashState.Receipt receipt=new CashState.Receipt();
            receipt.id=UUID.randomUUID().toString();
            receipt.shiftId=StateStore.shiftId(c);
            receipt.tableId=id;
            receipt.method=method;
            receipt.paidAt=Instant.now().toString();
            receipt.total=CashState.total(items);
            receipt.items=items;
            Math.addExact(StateStore.revenue(c,null),receipt.total);
            Math.addExact(StateStore.revenue(c,method),receipt.total);
            StateStore.insertReceipt(c,receipt);
            StateStore.replaceOrder(c,id,List.of());
            audit(c,currentActor(),"Оплата",receipt.id,tableName(c,id)+". Оплата "+receipt.total+" ₽, "+method.label);
            return null;
        });
    }

    public int revenue(PaymentMethod method) throws IOException { return store.read(c -> StateStore.revenue(c,method)); }

    public AppConfig settings() throws IOException {
        return store.read(c -> AppConfig.fromJson(StateStore.metadata(c,"settings_json")));
    }

    public AppConfig saveSettings(AppConfig candidate,String actor) throws IOException {
        AppConfig next=candidate.copy();
        return store.transaction(c -> {
            AppConfig previous=AppConfig.fromJson(StateStore.metadata(c,"settings_json"));
            StateStore.reconcileTables(c,next);
            StateStore.putMetadata(c,"settings_json",next.toJson());
            audit(c,actor,"Настройки", "settings",settingsChanges(previous,next));
            return next.copy();
        });
    }

    private String settingsChanges(AppConfig old,AppConfig next) {
        List<String> changes=new ArrayList<>();
        if (old.employeePercent.compareTo(next.employeePercent)!=0) changes.add("Процент сотрудника: "+old.employeePercent+" → "+next.employeePercent);
        Map<String,AppConfig.Product> products=new LinkedHashMap<>();
        for (AppConfig.Product item:old.products) products.put(item.id,item);
        for (AppConfig.Product item:next.products) {
            AppConfig.Product before=products.remove(item.id);
            if (before==null) changes.add("Добавлено в меню: "+item.name+", "+item.price+" ₽");
            else if (!before.name.equals(item.name) || !before.price.equals(item.price) || !Objects.equals(before.recipe,item.recipe))
                changes.add("Меню: "+before.name+" ("+before.price+" ₽) → "+item.name+" ("+item.price+" ₽)");
        }
        products.values().forEach(item -> changes.add("Удалено из меню: "+item.name));
        Map<String,String> tables=new LinkedHashMap<>();
        for (AppConfig.Table item:old.tables) tables.put(item.id,item.name);
        for (AppConfig.Table item:next.tables) {
            String before=tables.remove(item.id);
            if (before==null) changes.add("Добавлен стол: "+item.name);
            else if (!before.equals(item.name)) changes.add("Стол: "+before+" → "+item.name);
        }
        tables.values().forEach(name -> changes.add("Удалён стол: "+name));
        if (changes.isEmpty()) changes.add("Настройки сохранены; состав меню и названия не изменились.");
        return String.join("\n",changes);
    }

    private static void audit(Connection c,String actor,String action,String entity,String details) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("INSERT INTO audit_log(occurred_at,actor,action,entity_id,details) VALUES(?,?,?,?,?)")) {
            ps.setString(1,Instant.now().toString()); ps.setString(2,actor==null || actor.isBlank() ? "Оператор" : actor);
            ps.setString(3,action); ps.setString(4,entity); ps.setString(5,details); ps.executeUpdate();
        }
    }

    private static String currentActor() { return System.getProperty("user.name","Оператор"); }

    private static final String HISTORY_SELECT="SELECT r.id,r.paid_at,r.table_id,r.method,r.total,f.id,f.refunded_at,f.reason,t.name FROM receipts r LEFT JOIN refunds f ON f.receipt_id=r.id JOIN dining_tables t ON t.id=r.table_id";

    private static History.ReceiptRow historyRow(ResultSet rs) throws SQLException {
        History.ReceiptRow row=new History.ReceiptRow();
        row.id=rs.getString(1); row.paidAt=rs.getString(2); row.tableId=rs.getString(3);
        row.method=PaymentMethod.valueOf(rs.getString(4)); row.total=rs.getInt(5);
        row.refundId=rs.getString(6); row.refundedAt=rs.getString(7); row.reason=rs.getString(8);
        row.tableName=rs.getString(9);
        return row;
    }

    public History.Page history(LocalDate from,LocalDate through,java.time.ZoneId zone,String search,PaymentMethod method,int offset) throws IOException {
        if (offset<0 || zone==null || (from!=null && through!=null && from.isAfter(through))) throw new IllegalArgumentException("Проверьте период поиска.");
        String text=search==null ? "" : search.trim();
        if (text.length()>200) throw new IllegalArgumentException("Поисковый запрос слишком длинный.");
        return store.read(c -> {
            StringBuilder sql=new StringBuilder(HISTORY_SELECT+" WHERE 1=1");
            List<Object> args=new ArrayList<>();
            if (from!=null) { sql.append(" AND r.paid_at_ms>=?"); args.add(from.atStartOfDay(zone).toInstant().toEpochMilli()); }
            if (through!=null) { sql.append(" AND r.paid_at_ms<?"); args.add(through.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()); }
            if (method!=null) { sql.append(" AND r.method=?"); args.add(method.name()); }
            if (!text.isEmpty()) {
                sql.append(" AND (casefold(r.id) LIKE ? ESCAPE '\\' OR casefold(r.table_id) LIKE ? ESCAPE '\\' OR casefold(t.name) LIKE ? ESCAPE '\\' OR EXISTS(SELECT 1 FROM receipt_items i WHERE i.receipt_id=r.id AND casefold(i.name) LIKE ? ESCAPE '\\'))");
                String like="%"+text.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                args.add(like); args.add(like); args.add(like); args.add(like);
            }
            sql.append(" ORDER BY r.paid_at_ms DESC,r.id DESC LIMIT 51 OFFSET ?"); args.add(offset);
            try (PreparedStatement ps=c.prepareStatement(sql.toString())) {
                for (int i=0;i<args.size();i++) ps.setObject(i+1,args.get(i));
                List<History.ReceiptRow> rows=new ArrayList<>();
                try (ResultSet rs=ps.executeQuery()) { while (rs.next()) rows.add(historyRow(rs)); }
                boolean more=rows.size()>50;
                if (more) rows.remove(rows.size()-1);
                return new History.Page(rows,more);
            }
        });
    }

    public History.ReceiptDetail receipt(String id) throws IOException {
        return store.read(c -> detail(c,id));
    }

    private static History.ReceiptDetail detail(Connection c,String id) throws SQLException {
        History.ReceiptDetail detail=new History.ReceiptDetail();
        try (PreparedStatement ps=c.prepareStatement(HISTORY_SELECT+" WHERE r.id=?")) {
            ps.setString(1,id);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Чек не найден.");
                detail.receipt=historyRow(rs);
            }
        }
        detail.items=new ArrayList<>();
        try (PreparedStatement ps=c.prepareStatement("SELECT name,quantity,price FROM receipt_items WHERE receipt_id=? ORDER BY line")) {
            ps.setString(1,id);
            try (ResultSet rs=ps.executeQuery()) {
                while (rs.next()) detail.items.add(new OrderItem(rs.getString(1),rs.getInt(2),rs.getInt(3)));
            }
        }
        return detail;
    }

    public void refund(String receiptId,String reason,String actor) throws IOException {
        String checked=reason==null ? "" : reason.trim();
        if (checked.isEmpty() || checked.length()>500) throw new IllegalArgumentException("Укажите причину возврата, до 500 символов.");
        store.transaction(c -> {
            History.ReceiptDetail original=detail(c,receiptId);
            History.ReceiptRow receipt=original.receipt;
            if (receipt.refundId!=null) throw new IllegalArgumentException("Этот чек уже возвращён.");
            Math.subtractExact(StateStore.revenue(c,null),receipt.total);
            Math.subtractExact(StateStore.revenue(c,receipt.method),receipt.total);
            String id=UUID.randomUUID().toString();
            try (PreparedStatement ps=c.prepareStatement("INSERT INTO refunds VALUES(?,?,?,?,?,?)")) {
                ps.setString(1,id); ps.setString(2,receiptId); ps.setString(3,StateStore.shiftId(c));
                ps.setString(4,Instant.now().toString()); ps.setString(5,checked);
                ps.setString(6,actor==null || actor.isBlank() ? "Оператор" : actor); ps.executeUpdate();
            }
            audit(c,actor,"Возврат",receiptId,"Полный возврат "+receipt.total+" ₽. Причина: "+checked);
            return null;
        });
    }

    public History.AuditPage audit(int offset) throws IOException {
        if (offset<0) throw new IllegalArgumentException("Неверная страница журнала.");
        return store.read(c -> {
            List<History.AuditRow> rows=new ArrayList<>();
            try (PreparedStatement ps=c.prepareStatement("SELECT id,occurred_at,actor,action,entity_id,details FROM audit_log ORDER BY id DESC LIMIT 51 OFFSET ?")) {
                ps.setInt(1,offset);
                try (ResultSet rs=ps.executeQuery()) {
                    while (rs.next()) {
                        History.AuditRow row=new History.AuditRow(); row.id=rs.getLong(1); row.at=rs.getString(2);
                        row.actor=rs.getString(3); row.action=rs.getString(4); row.entityId=rs.getString(5); row.details=rs.getString(6); rows.add(row);
                    }
                }
            }
            boolean more=rows.size()>50;
            if (more) rows.remove(rows.size()-1);
            return new History.AuditPage(rows,more);
        });
    }

    public Path closeShift(LocalDate date) throws IOException {
        Objects.requireNonNull(date,"Укажите дату смены.");
        Path[] savedReport={null};
        try {
            return store.transaction(c -> {
                try (Statement ps=c.createStatement(); ResultSet rs=ps.executeQuery("SELECT 1 FROM order_items LIMIT 1")) {
                    if (rs.next()) throw new IllegalArgumentException("Перед закрытием смены оплатите открытые столы.");
                }
                String shift=StateStore.shiftId(c);
                StringBuilder report=new StringBuilder("Смена: "+date+"\nНомер смены: "+shift
                        +"\nИтого за смену:\nВсего: "+StateStore.revenue(c,null)
                        +"\nНаличными: "+StateStore.revenue(c,PaymentMethod.CASH)
                        +"\nБезналом: "+StateStore.revenue(c,PaymentMethod.CARD)
                        +"\nПереводом: "+StateStore.revenue(c,PaymentMethod.TRANSFER)+"\n\nОплаченные чеки:\n");
                for (CashState.Receipt receipt:StateStore.receipts(c,shift)) {
                    report.append(receipt.id).append(" | ").append(receipt.paidAt).append(" | стол ")
                            .append(receipt.tableId).append(" | ").append(receipt.method).append(" | ")
                            .append(receipt.total).append('\n');
                    for (OrderItem item:receipt.items) report.append("  ").append(item.name).append(" × ")
                            .append(item.quantity).append(" × ").append(item.price).append(" = ").append(item.total()).append('\n');
                }
                report.append("\nВозвраты этой смены:\n");
                try (PreparedStatement ps=c.prepareStatement("SELECT f.receipt_id,r.total,r.method,f.refunded_at,f.reason FROM refunds f JOIN receipts r ON r.id=f.receipt_id WHERE f.shift_id=? ORDER BY f.rowid")) {
                    ps.setString(1,shift);
                    try (ResultSet rs=ps.executeQuery()) {
                        while (rs.next()) report.append(rs.getString(1)).append(" | -").append(rs.getInt(2)).append(" | ")
                                .append(rs.getString(3)).append(" | ").append(rs.getString(4)).append(" | ").append(rs.getString(5)).append('\n');
                    }
                }
                savedReport[0]=store.writeReport(date+"-"+shift+"-"+UUID.randomUUID()+".txt",report.toString());
                try (PreparedStatement ps=c.prepareStatement("UPDATE metadata SET value=? WHERE key='shift_id'")) {
                    ps.setString(1,UUID.randomUUID().toString()); ps.executeUpdate();
                }
                audit(c,currentActor(),"Смена",shift,"Смена закрыта за "+date+". Отчёт: "+savedReport[0].getFileName());
                return savedReport[0];
            });
        } catch (IOException e) {
            if (savedReport[0]!=null) throw new IOException("Отчёт сохранён в "+savedReport[0]
                    +", но смена не закрыта: запись не удалась. Суммы оставлены без изменений.",e);
            throw e;
        }
    }

    @Override public void close() throws IOException { store.close(); }
}
