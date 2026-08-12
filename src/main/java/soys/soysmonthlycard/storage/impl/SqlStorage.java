package soys.soysmonthlycard.storage.impl;

import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.storage.ClaimRecord;
import soys.soysmonthlycard.storage.DataStorage;
import soys.soysmonthlycard.storage.StorageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SQL 存储后端的公共实现（移植自 SOYSLinkTeam）。
 * <p>
 * SQLite 与 MySQL 共用同一套表结构与 CRUD 逻辑，子类只需提供：
 * 驱动类名、JDBC URL、连接创建方式与建表语句方言。
 * </p>
 * <p>
 * 连接策略：维持单个长连接并在每次使用前做有效性探测，配合对象锁串行化访问。
 * 插件的写操作本身已经被 StorageManager 收敛到异步队列，无需引入连接池。
 * 领取记录表结构：claims(uuid, tier, month)，以 (uuid, tier) 为主键。
 * </p>
 */
public abstract class SqlStorage implements DataStorage {

    protected final SOYSMonthlyCard plugin;
    protected final Object lock = new Object();

    protected String tablePrefix = "soys_";
    protected volatile boolean available = false;

    private Connection connection;

    protected SqlStorage(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    // ================================================================
    //  子类需实现的方言部分
    // ================================================================

    /** JDBC 驱动类名 */
    protected abstract String getDriverClass();

    /** 创建一个全新的数据库连接 */
    protected abstract Connection createConnection() throws SQLException;

    /** 建表与建索引语句，按顺序执行 */
    protected abstract String[] getSchemaStatements();

    // ================================================================
    //  表名
    // ================================================================

    protected String claimsTable() {
        return tablePrefix + "claims";
    }

    // ================================================================
    //  生命周期
    // ================================================================

    @Override
    public void initialize() throws Exception {
        try {
            Class.forName(getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到 JDBC 驱动 " + getDriverClass()
                    + "，请确认服务端已提供该驱动或手动放入 libraries 目录");
        }
        synchronized (lock) {
            connection = createConnection();
            try (Statement statement = connection.createStatement()) {
                for (String sql : getSchemaStatements()) {
                    statement.execute(sql);
                }
            }
        }
        available = true;
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            available = false;
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 关闭失败无需处理
                }
                connection = null;
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    /**
     * 获取一个可用连接，失效时自动重建。调用方必须持有 {@link #lock}。
     */
    protected Connection connection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(3)) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // 旧连接关闭失败可忽略
                }
            }
            connection = createConnection();
        }
        return connection;
    }

    /**
     * 主动探测连接（保活任务使用）。
     */
    public void keepAlive() {
        synchronized (lock) {
            try {
                connection().isValid(3);
            } catch (SQLException e) {
                plugin.getLogger().warning("[" + getType().getId() + "] 保活探测失败: " + e.getMessage());
            }
        }
    }

    // ================================================================
    //  读
    // ================================================================

    @Override
    public ClaimRecord load(UUID uuid) throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            Map<String, String> tiers = new HashMap<>();
            String sql = "SELECT tier, month FROM " + claimsTable() + " WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tiers.put(rs.getString("tier"), rs.getString("month"));
                    }
                }
            }
            return tiers.isEmpty() ? null : new ClaimRecord(uuid, tiers);
        }
    }

    @Override
    public Collection<ClaimRecord> loadAll() throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            Map<UUID, ClaimRecord> map = new HashMap<>();
            String sql = "SELECT uuid, tier, month FROM " + claimsTable();
            try (Statement statement = conn.createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = parseUuid(rs.getString("uuid"));
                    if (uuid == null) {
                        continue;
                    }
                    ClaimRecord rec = map.computeIfAbsent(uuid, ClaimRecord::new);
                    rec.setTier(rs.getString("tier"), rs.getString("month"));
                }
            }
            return new ArrayList<>(map.values());
        }
    }

    @Override
    public Set<UUID> loadAllUuids() throws Exception {
        synchronized (lock) {
            Set<UUID> set = new HashSet<>();
            String sql = "SELECT DISTINCT uuid FROM " + claimsTable();
            try (Statement statement = connection().createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                while (rs.next()) {
                    UUID uuid = parseUuid(rs.getString("uuid"));
                    if (uuid != null) {
                        set.add(uuid);
                    }
                }
            }
            return set;
        }
    }

    @Override
    public int countRecords() throws Exception {
        synchronized (lock) {
            try (Statement statement = connection().createStatement();
                 ResultSet rs = statement.executeQuery("SELECT COUNT(DISTINCT uuid) FROM " + claimsTable())) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ================================================================
    //  写
    // ================================================================

    @Override
    public void save(ClaimRecord record) throws Exception {
        synchronized (lock) {
            Connection conn = connection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                writeRecord(conn, record);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public void saveAll(Collection<ClaimRecord> records) throws Exception {
        if (records.isEmpty()) {
            return;
        }
        synchronized (lock) {
            Connection conn = connection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (ClaimRecord record : records) {
                    writeRecord(conn, record);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @Override
    public void delete(UUID uuid) throws Exception {
        synchronized (lock) {
            try (PreparedStatement ps = connection().prepareStatement(
                    "DELETE FROM " + claimsTable() + " WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void clear() throws Exception {
        synchronized (lock) {
            try (Statement statement = connection().createStatement()) {
                statement.executeUpdate("DELETE FROM " + claimsTable());
            }
        }
    }

    // ================================================================
    //  内部
    // ================================================================

    private void writeRecord(Connection conn, ClaimRecord record) throws SQLException {
        String sql = "REPLACE INTO " + claimsTable() + " (uuid, tier, month) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : record.getTiers().entrySet()) {
                ps.setString(1, record.getUuid().toString());
                ps.setString(2, entry.getKey());
                ps.setString(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    protected UUID parseUuid(String input) {
        if (input == null) {
            return null;
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
