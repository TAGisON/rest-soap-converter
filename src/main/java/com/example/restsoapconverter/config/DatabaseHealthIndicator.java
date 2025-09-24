
package com.example.restsoapconverter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public Health health() {
        try {
            return checkDatabaseHealth();
        } catch (Exception e) {
            logger.error("❌ Database health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("database", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private Health checkDatabaseHealth() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {

            if (resultSet.next()) {
                // Check table existence
                boolean tablesExist = checkTablesExist(connection);

                return Health.up()
                        .withDetail("database", "restconvertor")
                        .withDetail("connection", "active")
                        .withDetail("tables", tablesExist ? "exist" : "missing")
                        .withDetail("status", "healthy")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", "Query failed")
                        .build();
            }
        }
    }

    private boolean checkTablesExist(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name IN ('soap_endpoints', 'rest_calls', 'mappings')")
        ) {
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.debug("🔍 Found {} required tables", count);
                return count == 3;
            }
        } catch (Exception e) {
            logger.warn("⚠️ Could not check table existence: {}", e.getMessage());
        }
        return false;
    }
}
