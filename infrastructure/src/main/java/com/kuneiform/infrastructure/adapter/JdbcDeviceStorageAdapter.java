package com.kuneiform.infrastructure.adapter;

import com.kuneiform.domain.model.UserDevice;
import com.kuneiform.domain.port.DeviceStoragePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 * JDBC implementation of DeviceStoragePort using user_devices table. Works with PostgreSQL, MySQL,
 * and SQL Server. This is the default (primary) device storage adapter.
 */
@Slf4j
@Component
@Primary
@ConditionalOnExpression("!'${wedge.device.storage-type:}'.equals('in-memory')")
public class JdbcDeviceStorageAdapter implements DeviceStoragePort {

  private final JdbcTemplate jdbcTemplate;

  public JdbcDeviceStorageAdapter(@Qualifier("clientJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<UserDevice> findByUserId(String userId) {
    String sql = "SELECT * FROM user_devices WHERE user_id = ? ORDER BY last_used DESC";
    return jdbcTemplate.query(sql, new UserDeviceRowMapper(), userId);
  }

  @Override
  public Optional<UserDevice> findByDeviceId(String deviceId) {
    String sql = "SELECT * FROM user_devices WHERE device_id = ?";
    List<UserDevice> devices = jdbcTemplate.query(sql, new UserDeviceRowMapper(), deviceId);
    return devices.isEmpty() ? Optional.empty() : Optional.of(devices.get(0));
  }

  @Override
  public UserDevice save(UserDevice device) {
    String sql =
        """
                INSERT INTO user_devices (device_id, user_id, device_name, user_agent, ip_address,
                                           first_seen, last_used, authorization_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (device_id) DO UPDATE SET
                    device_name = EXCLUDED.device_name,
                    user_agent = EXCLUDED.user_agent,
                    ip_address = EXCLUDED.ip_address,
                    last_used = EXCLUDED.last_used,
                    authorization_id = EXCLUDED.authorization_id
                """;

    try {
      jdbcTemplate.update(
          sql,
          device.getDeviceId(),
          device.getUserId(),
          device.getDeviceName(),
          device.getUserAgent(),
          device.getIpAddress(),
          Timestamp.from(device.getFirstSeen()),
          Timestamp.from(device.getLastUsed()),
          device.getAuthorizationId());
      log.debug("Saved device: {} for user: {}", device.getDeviceId(), device.getUserId());
    } catch (Exception e) {
      // Try MySQL/SQL Server syntax if PostgreSQL fails
      String sqlAlt =
          """
                    INSERT INTO user_devices (device_id, user_id, device_name, user_agent, ip_address,
                                               first_seen, last_used, authorization_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        device_name = VALUES(device_name),
                        user_agent = VALUES(user_agent),
                        ip_address = VALUES(ip_address),
                        last_used = VALUES(last_used),
                        authorization_id = VALUES(authorization_id)
                    """;

      jdbcTemplate.update(
          sqlAlt,
          device.getDeviceId(),
          device.getUserId(),
          device.getDeviceName(),
          device.getUserAgent(),
          device.getIpAddress(),
          Timestamp.from(device.getFirstSeen()),
          Timestamp.from(device.getLastUsed()),
          device.getAuthorizationId());
      log.debug("Saved device: {} for user: {}", device.getDeviceId(), device.getUserId());
    }

    return device;
  }

  @Override
  public void deleteByDeviceId(String deviceId) {
    String sql = "DELETE FROM user_devices WHERE device_id = ?";
    int rows = jdbcTemplate.update(sql, deviceId);

    if (rows > 0) {
      log.info("Deleted device: {}", deviceId);
    }
  }

  @Override
  public void deleteByUserId(String userId) {
    String sql = "DELETE FROM user_devices WHERE user_id = ?";
    int rows = jdbcTemplate.update(sql, userId);
    log.info("Deleted {} device(s) for user: {}", rows, userId);
  }

  @Override
  public boolean existsByDeviceId(String deviceId) {
    String sql = "SELECT COUNT(*) FROM user_devices WHERE device_id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, deviceId);
    return count != null && count > 0;
  }

  /** RowMapper for UserDevice */
  private static class UserDeviceRowMapper implements RowMapper<UserDevice> {
    @Override
    public UserDevice mapRow(ResultSet rs, int rowNum) throws SQLException {
      return UserDevice.builder()
          .deviceId(rs.getString("device_id"))
          .userId(rs.getString("user_id"))
          .deviceName(rs.getString("device_name"))
          .userAgent(rs.getString("user_agent"))
          .ipAddress(rs.getString("ip_address"))
          .firstSeen(rs.getTimestamp("first_seen").toInstant())
          .lastUsed(rs.getTimestamp("last_used").toInstant())
          .authorizationId(rs.getString("authorization_id"))
          .build();
    }
  }
}
