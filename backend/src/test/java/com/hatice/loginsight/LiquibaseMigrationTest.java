package com.hatice.loginsight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LiquibaseMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationsCreateExpectedTables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet tables = metaData.getTables(null, null, "log_analysis", null)) {
                assertThat(tables.next()).isTrue();
            }
            try (ResultSet tables = metaData.getTables(null, null, "frequent_error", null)) {
                assertThat(tables.next()).isTrue();
            }
        }
    }
}