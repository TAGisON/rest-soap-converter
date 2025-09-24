
package com.example.restsoapconverter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class StartupBanner {

    private static final Logger logger = LoggerFactory.getLogger(StartupBanner.class);

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port}")
    private int serverPort;

    private final DataSource dataSource;

    public StartupBanner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printStartupBanner() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            String databaseVersion = connection.getMetaData().getDatabaseProductVersion();

            logger.info("🚀 =============================================================");
            logger.info("🎯 {} STARTED SUCCESSFULLY!", applicationName.toUpperCase());
            logger.info("🚀 =============================================================");
            logger.info("🌐 Server: http://localhost:{}", serverPort);
            logger.info("🔗 Health: http://localhost:{}/actuator/health", serverPort);
            logger.info("📊 Metrics: http://localhost:{}/actuator/metrics", serverPort);
            logger.info("🗄️ Database: {} ({})", databaseName, databaseVersion);
            logger.info("🔧 SOAP Endpoints: http://localhost:{}/admin/endpoints", serverPort);
            logger.info("🧼 SOAP Services: http://localhost:{}/ws/", serverPort);
            logger.info("🚀 =============================================================");
            logger.info("✅ REST-to-SOAP Converter is ready to process requests!");
            logger.info("🚀 =============================================================");

        } catch (Exception e) {
            logger.error("❌ Could not retrieve database information: {}", e.getMessage());
        }
    }
}
