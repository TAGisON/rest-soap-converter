
package com.example.restsoapconverter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Creation EnvironmentPostProcessor - runs VERY early in Spring Boot lifecycle
 * This runs BEFORE any other Spring components including Flyway
 */
public class DatabaseCreationPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCreationPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String autoCreate = environment.getProperty("database.auto-create", "true");

        if ("true".equals(autoCreate) && datasourceUrl != null) {
            logger.info("🔍 [POST-PROCESSOR] Creating database before Spring context initialization...");
            createDatabaseIfNotExists(datasourceUrl, username, password);
        }
    }

    private void createDatabaseIfNotExists(String datasourceUrl, String username, String password) {
        String targetDatabase = extractDatabaseName(datasourceUrl);
        String serverUrl = getServerUrl(datasourceUrl);

        logger.info("🔍 [POST-PROCESSOR] Checking if database '{}' exists...", targetDatabase);

        try {
            // First, try to connect to the target database directly
            if (databaseExists(datasourceUrl, username, password)) {
                logger.info("✅ [POST-PROCESSOR] Database '{}' already exists", targetDatabase);
                return;
            }

            logger.warn("⚠️ [POST-PROCESSOR] Database '{}' does not exist. Creating...", targetDatabase);

            // Connect to postgres database to create the target database
            String postgresUrl = serverUrl + "/postgres";
            createDatabase(postgresUrl, targetDatabase, username, password);

            logger.info("✅ [POST-PROCESSOR] Successfully created database '{}'", targetDatabase);

        } catch (Exception e) {
            logger.error("❌ [POST-PROCESSOR] Failed to create database '{}': {}", targetDatabase, e.getMessage());
            // Continue - let Spring Boot fail with clearer message if needed
        }
    }

    private boolean databaseExists(String url, String username, String password) {
        try {
            // Load the PostgreSQL driver explicitly
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.warn("⚠️ [POST-PROCESSOR] PostgreSQL driver not found: {}", e.getMessage());
            return false;
        }

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            logger.info("✅ [POST-PROCESSOR] Successfully connected to target database");
            return true;
        } catch (SQLException e) {
            logger.debug("🔍 [POST-PROCESSOR] Cannot connect to target database: {}", e.getMessage());
            return false;
        }
    }

    private void createDatabase(String postgresUrl, String databaseName, String username, String password) throws SQLException {
        logger.info("🔧 [POST-PROCESSOR] Connecting to postgres database to create '{}'", databaseName);

        try (Connection connection = DriverManager.getConnection(postgresUrl, username, password);
             Statement statement = connection.createStatement()) {

            // Check if database already exists
            String checkQuery = String.format(
                "SELECT 1 FROM pg_database WHERE datname='%s'", databaseName);

            try (ResultSet rs = statement.executeQuery(checkQuery)) {
                if (rs.next()) {
                    logger.info("✅ [POST-PROCESSOR] Database '{}' already exists during creation check", databaseName);
                    return;
                }
            }

            // Create the database
            String createQuery = String.format(
                "CREATE DATABASE \"%s\" WITH ENCODING='UTF8'", databaseName);

            logger.info("🔧 [POST-PROCESSOR] Executing database creation: {}", createQuery);
            statement.executeUpdate(createQuery);

            logger.info("✅ [POST-PROCESSOR] Database '{}' created successfully", databaseName);

        } catch (SQLException e) {
            logger.error("❌ [POST-PROCESSOR] SQL Error creating database: {}", e.getMessage());
            throw e;
        }
    }

    private String extractDatabaseName(String url) {
        String[] parts = url.split("/");
        String dbPart = parts[parts.length - 1];

        if (dbPart.contains("?")) {
            dbPart = dbPart.substring(0, dbPart.indexOf("?"));
        }

        return dbPart;
    }

    private String getServerUrl(String url) {
        int lastSlash = url.lastIndexOf("/");
        return url.substring(0, lastSlash);
    }
}
