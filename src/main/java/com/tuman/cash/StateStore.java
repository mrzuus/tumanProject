package com.tuman.cash;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** A single local SQLite connection, protected against a second application instance. */
public class StateStore implements AutoCloseable {
    private final Path directory;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private Connection connection;

    public StateStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath();
        Files.createDirectories(this.directory);
        lockChannel = FileChannel.open(this.directory.resolve("cash.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = lockChannel.tryLock();
            if (acquired == null) throw new IOException("Касса уже запущена.");
        } catch (IOException | OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("Не удалось открыть данные. Возможно, касса уже запущена.", e);
        }
        lock = acquired;
    }

    public Path directory() { return directory; }

    synchronized void initialize(AppConfig config) throws IOException {
        if (connection != null) return;
        if (config!=null) config.validate();
        Path db = directory.resolve("cash.sqlite");
        Path marker = directory.resolve("cash.initialized");
        boolean existing = !Files.notExists(db);
        if (!existing && (!Files.notExists(marker) || !Files.notExists(directory.resolve("cash.sqlite-wal"))
                || !Files.notExists(directory.resolve("cash.previous.sqlite")))) {
            throw new IOException("База кассы отсутствует: " + db
                    + ". Касса не обнулена. Восстановите данные из резервной копии при закрытой программе.");
        }
        if (existing && Files.size(db) == 0) throw new IOException("База кассы пуста: " + db + ". Требуется восстановление.");
        try {
            Class.forName("org.sqlite.JDBC");
            if (!existing) createDatabase(db, config==null ? AppConfig.load(directory) : config);
            connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            org.sqlite.Function.create(connection,"casefold",new org.sqlite.Function() {
                @Override protected void xFunc() throws SQLException {
                    String value=value_text(0);
                    result(value==null ? "" : value.toLowerCase(Locale.ROOT));
                }
            });
            execute(connection, "PRAGMA foreign_keys=ON", "PRAGMA busy_timeout=5000", "PRAGMA synchronous=FULL");
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("PRAGMA quick_check")) {
                if (!rs.next() || !"ok".equals(rs.getString(1))) throw new IOException("База кассы повреждена. Требуется восстановление.");
            }
            int version;
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("PRAGMA user_version")) {
                if (!rs.next()) throw new IOException("Неизвестная версия базы кассы.");
                version=rs.getInt(1);
            }
            if (version!=2 && version!=3) throw new IOException("Неизвестная версия базы кассы.");
            String shift = shiftId(connection);
            UUID.fromString(shift);
            backup();
            if (version==2) migrateV3();
            String saved=metadata(connection,"settings_json");
            AppConfig selected=config!=null ? config : saved==null ? AppConfig.load(directory) : AppConfig.fromJson(saved);
            transaction(c -> {
                reconcileTables(c,selected);
                putMetadata(c,"settings_json",selected.toJson());
                return null;
            });
            if (Files.notExists(marker)) Files.writeString(marker, "SQLite cash database\n", StandardOpenOption.CREATE_NEW);
        } catch (SQLException | ClassNotFoundException | RuntimeException | IOException e) {
            if (connection != null) {
                try { connection.close(); } catch (SQLException closeError) { e.addSuppressed(closeError); }
                connection = null;
            }
            throw new IOException("Не удалось открыть данные кассы: " + e.getMessage(), e);
        }
    }

    private void migrateV3() throws IOException {
        transaction(c -> {
            execute(c,"ALTER TABLE receipts ADD COLUMN paid_at_ms INTEGER NOT NULL DEFAULT 0");
            try (Statement select=c.createStatement(); ResultSet rows=select.executeQuery("SELECT id,paid_at FROM receipts");
                 PreparedStatement update=c.prepareStatement("UPDATE receipts SET paid_at_ms=? WHERE id=?")) {
                int count=0;
                while (rows.next()) {
                    update.setLong(1,java.time.Instant.parse(rows.getString(2)).toEpochMilli());
                    update.setString(2,rows.getString(1)); update.addBatch();
                    if (++count%1000==0) update.executeBatch();
                }
                update.executeBatch();
            }
            execute(c,
                "CREATE TABLE refunds(id TEXT PRIMARY KEY, receipt_id TEXT NOT NULL UNIQUE REFERENCES receipts(id), shift_id TEXT NOT NULL, refunded_at TEXT NOT NULL, reason TEXT NOT NULL CHECK(length(trim(reason))>0), actor TEXT NOT NULL)",
                "CREATE INDEX refund_shift ON refunds(shift_id)",
                "CREATE INDEX receipt_history_time ON receipts(paid_at_ms DESC,id DESC)",
                "CREATE TABLE audit_log(id INTEGER PRIMARY KEY, occurred_at TEXT NOT NULL, actor TEXT NOT NULL, action TEXT NOT NULL, entity_id TEXT NOT NULL, details TEXT NOT NULL)",
                "PRAGMA user_version=3");
            return null;
        });
    }

    static String metadata(Connection c,String key) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("SELECT value FROM metadata WHERE key=?")) {
            ps.setString(1,key);
            try (ResultSet rs=ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    static void putMetadata(Connection c,String key,String value) throws SQLException {
        try (PreparedStatement ps=c.prepareStatement("INSERT INTO metadata VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1,key); ps.setString(2,value); ps.executeUpdate();
        }
    }

    private void backup() throws SQLException, IOException {
        Path temporary = directory.resolve(".backup-" + UUID.randomUUID() + ".sqlite");
        try {
            try (PreparedStatement ps = connection.prepareStatement("VACUUM INTO ?")) {
                ps.setString(1,temporary.toString()); ps.execute();
            }
            Files.move(temporary,directory.resolve("cash.previous.sqlite"),
                    StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    private void createDatabase(Path db, AppConfig config) throws SQLException, IOException {
        CashState legacy = readLegacy();
        Path temporary = Files.createTempFile(directory, ".cash-migration-", ".sqlite");
        try {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + temporary)) {
                execute(c, "PRAGMA synchronous=FULL", "PRAGMA foreign_keys=ON");
                c.setAutoCommit(false);
                try {
                    execute(c,
                        "CREATE TABLE metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL)",
                        "CREATE TABLE dining_tables(id TEXT PRIMARY KEY, name TEXT NOT NULL, active INTEGER NOT NULL, sort_order INTEGER NOT NULL)",
                        "CREATE TABLE order_items(id INTEGER PRIMARY KEY, table_id TEXT NOT NULL REFERENCES dining_tables(id), name TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), price INTEGER NOT NULL CHECK(price>=0))",
                        "CREATE TABLE receipts(id TEXT PRIMARY KEY, shift_id TEXT NOT NULL, table_id TEXT NOT NULL REFERENCES dining_tables(id), paid_at TEXT NOT NULL, method TEXT NOT NULL CHECK(method IN ('CASH','CARD','TRANSFER')), total INTEGER NOT NULL CHECK(total>=0))",
                        "CREATE TABLE receipt_items(receipt_id TEXT NOT NULL REFERENCES receipts(id), line INTEGER NOT NULL, name TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity>0), price INTEGER NOT NULL CHECK(price>=0), PRIMARY KEY(receipt_id,line))",
                        "CREATE INDEX receipt_shift_method ON receipts(shift_id,method)",
                        "CREATE INDEX order_table ON order_items(table_id)",
                        "PRAGMA user_version=2");
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO metadata VALUES('shift_id',?)")) {
                        ps.setString(1, legacy.shiftId); ps.executeUpdate();
                    }
                    Set<String> ids = new LinkedHashSet<>(legacy.tables.keySet());
                    for (CashState.Receipt receipt : legacy.receipts) ids.add(receipt.tableId);
                    for (String id : ids) {
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO dining_tables VALUES(?,?,0,0)")) {
                            ps.setString(1,id); ps.setString(2,id); ps.executeUpdate();
                        }
                    }
                    for (Map.Entry<String,List<OrderItem>> entry : legacy.tables.entrySet()) {
                        replaceOrder(c, entry.getKey(), entry.getValue());
                    }
                    for (CashState.Receipt receipt : legacy.receipts) insertReceipt(c, receipt,false);
                    reconcileTables(c, config);
                    c.commit();
                } catch (SQLException | IOException | RuntimeException e) {
                    c.rollback();
                    throw e;
                }
            }
            Files.move(temporary, db, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }

    private CashState readLegacy() throws IOException {
        Path file = directory.resolve("cash-state.json");
        if (Files.notExists(file)) {
            if (!Files.notExists(directory.resolve("cash-state.previous.json"))) {
                throw new IOException("Основной файл cash-state.json отсутствует, но найдена резервная копия. "
                        + "Касса не обнулена: восстановите файл явно, чтобы не откатить оплаты незаметно.");
            }
            return CashState.empty();
        }
        try {
            CashState state = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                    .readValue(file.toFile(), CashState.class);
            if (state == null || state.tables == null || state.tables.isEmpty()) throw new IOException("Нет столов в старом файле.");
            state.validate();
            return state;
        } catch (RuntimeException | IOException e) {
            throw new IOException("Не удалось прочитать " + file + ". Файл оставлен без изменений. Требуется восстановление.", e);
        }
    }

    static void reconcileTables(Connection c, AppConfig config) throws SQLException, IOException {
        Set<String> configured = new HashSet<>();
        for (AppConfig.Table table : config.tables) configured.add(table.id);
        try (Statement ps = c.createStatement(); ResultSet rs = ps.executeQuery("SELECT DISTINCT table_id FROM order_items")) {
            while (rs.next()) {
                if (!configured.contains(rs.getString(1))) {
                    throw new IOException("В настройках удалён стол с неоплаченным заказом: " + rs.getString(1)
                            + ". Верните его в settings.json и оплатите заказ.");
                }
            }
        }
        execute(c, "UPDATE dining_tables SET active=0");
        int sort = 0;
        for (AppConfig.Table table : config.tables) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO dining_tables VALUES(?,?,1,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,active=1,sort_order=excluded.sort_order")) {
                ps.setString(1,table.id); ps.setString(2,table.name); ps.setInt(3,sort++); ps.executeUpdate();
            }
        }
    }

    @FunctionalInterface interface Work<T> { T run(Connection connection) throws SQLException, IOException; }

    synchronized <T> T read(Work<T> work) throws IOException {
        if (connection == null) throw new IOException("Хранилище кассы не открыто.");
        try { return work.run(connection); }
        catch (SQLException e) { throw new IOException("Не удалось прочитать данные кассы.", e); }
    }

    synchronized <T> T transaction(Work<T> work) throws IOException {
        if (connection == null) throw new IOException("Хранилище кассы не открыто.");
        try {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                beforeCommit();
                connection.commit();
                return result;
            } catch (SQLException | IOException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            } finally { connection.setAutoCommit(true); }
        } catch (SQLException e) { throw new IOException("Не удалось сохранить данные кассы. Операция отменена.", e); }
    }

    /** Failure injection hook used to verify transaction rollback. */
    protected void beforeCommit() throws IOException { }

    static void execute(Connection c, String... statements) throws SQLException {
        try (Statement ps = c.createStatement()) { for (String sql : statements) ps.execute(sql); }
    }

    static String shiftId(Connection c) throws SQLException, IOException {
        try (Statement ps = c.createStatement(); ResultSet rs = ps.executeQuery("SELECT value FROM metadata WHERE key='shift_id'")) {
            if (!rs.next()) throw new IOException("Нет номера активной смены. Требуется восстановление.");
            return rs.getString(1);
        }
    }

    static List<OrderItem> items(Connection c, String tableId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT name,quantity,price FROM order_items WHERE table_id=? ORDER BY id")) {
            ps.setString(1,tableId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new OrderItem(rs.getString(1),rs.getInt(2),rs.getInt(3)));
            }
        }
        return items;
    }

    static void replaceOrder(Connection c, String tableId, List<OrderItem> items) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM order_items WHERE table_id=?")) {
            ps.setString(1,tableId); ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO order_items(table_id,name,quantity,price) VALUES(?,?,?,?)")) {
            for (OrderItem item : items) {
                ps.setString(1,tableId); ps.setString(2,item.name); ps.setInt(3,item.quantity); ps.setInt(4,item.price); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    static void insertReceipt(Connection c, CashState.Receipt receipt) throws SQLException {
        insertReceipt(c,receipt,true);
    }

    private static void insertReceipt(Connection c,CashState.Receipt receipt,boolean withEpoch) throws SQLException {
        String sql=withEpoch ? "INSERT INTO receipts(id,shift_id,table_id,paid_at,method,total,paid_at_ms) VALUES(?,?,?,?,?,?,?)"
                : "INSERT INTO receipts(id,shift_id,table_id,paid_at,method,total) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,receipt.id); ps.setString(2,receipt.shiftId); ps.setString(3,receipt.tableId);
            ps.setString(4,receipt.paidAt); ps.setString(5,receipt.method.name()); ps.setInt(6,receipt.total);
            if (withEpoch) ps.setLong(7,java.time.Instant.parse(receipt.paidAt).toEpochMilli());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO receipt_items VALUES(?,?,?,?,?)")) {
            for (int line = 0; line < receipt.items.size(); line++) {
                OrderItem item = receipt.items.get(line);
                ps.setString(1,receipt.id); ps.setInt(2,line); ps.setString(3,item.name);
                ps.setInt(4,item.quantity); ps.setInt(5,item.price); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    static List<CashState.Receipt> receipts(Connection c, String shift) throws SQLException {
        List<CashState.Receipt> result = new ArrayList<>();
        String sql = "SELECT id,shift_id,table_id,paid_at,method,total FROM receipts";
        if (shift != null) sql += " WHERE shift_id=?";
        sql += " ORDER BY rowid";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            if (shift != null) ps.setString(1,shift);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashState.Receipt receipt = new CashState.Receipt();
                    receipt.id=rs.getString(1); receipt.shiftId=rs.getString(2); receipt.tableId=rs.getString(3);
                    receipt.paidAt=rs.getString(4); receipt.method=CashState.PaymentMethod.valueOf(rs.getString(5)); receipt.total=rs.getInt(6);
                    receipt.items=new ArrayList<>();
                    try (PreparedStatement lines = c.prepareStatement("SELECT name,quantity,price FROM receipt_items WHERE receipt_id=? ORDER BY line")) {
                        lines.setString(1,receipt.id);
                        try (ResultSet rows = lines.executeQuery()) {
                            while (rows.next()) receipt.items.add(new OrderItem(rows.getString(1),rows.getInt(2),rows.getInt(3)));
                        }
                    }
                    result.add(receipt);
                }
            }
        }
        return result;
    }

    static int revenue(Connection c, CashState.PaymentMethod method) throws SQLException, IOException {
        String sql="SELECT COALESCE(SUM(total),0) FROM receipts WHERE shift_id=?";
        if (method != null) sql += " AND method=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,shiftId(c));
            if (method != null) ps.setString(2,method.name());
            long payments;
            try (ResultSet rs=ps.executeQuery()) { rs.next(); payments=rs.getLong(1); }
            String returned="SELECT COALESCE(SUM(r.total),0) FROM refunds f JOIN receipts r ON r.id=f.receipt_id WHERE f.shift_id=?";
            if (method!=null) returned+=" AND r.method=?";
            try (PreparedStatement refund=c.prepareStatement(returned)) {
                refund.setString(1,shiftId(c));
                if (method!=null) refund.setString(2,method.name());
                try (ResultSet rs=refund.executeQuery()) { rs.next(); return Math.toIntExact(payments-rs.getLong(1)); }
            }
        }
    }

    public synchronized CashState load() throws IOException {
        if (connection == null) initialize(null);
        return read(c -> {
            CashState state = CashState.empty();
            state.shiftId=shiftId(c);
            try (Statement ps=c.createStatement(); ResultSet rs=ps.executeQuery("SELECT id FROM dining_tables WHERE active=1 ORDER BY sort_order")) {
                while (rs.next()) state.tables.put(rs.getString(1),items(c,rs.getString(1)));
            }
            state.receipts=receipts(c,null);
            state.validate();
            return state;
        });
    }

    public Path writeReport(String name, String text) throws IOException {
        Path reports = directory.resolve("reports");
        Files.createDirectories(reports);
        Path target = reports.resolve(name);
        Path temporary = Files.createTempFile(reports, ".report-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer bytes = java.nio.charset.StandardCharsets.UTF_8.encode(text);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            if (!Files.notExists(target)) throw new IOException("Отчёт уже существует: " + target);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
            return target;
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override public synchronized void close() throws IOException {
        IOException error=null;
        try { if (connection != null) connection.close(); }
        catch (SQLException e) { error=new IOException("Не удалось закрыть базу кассы.",e); }
        finally {
            connection=null;
            try { if (lock.isValid()) lock.release(); } finally { lockChannel.close(); }
        }
        if (error != null) throw error;
    }
}
