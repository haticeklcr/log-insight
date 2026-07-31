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
            try (ResultSet tables = metaData.getTables(null, null, "analysis_job", null)) {
                assertThat(tables.next()).isTrue();
            }
            try (ResultSet columns = metaData.getColumns(null, null, "log_analysis", "analysis_name")) {
                assertThat(columns.next()).isTrue();
            }
        }
    }

    @Test
    void v5MigrationsCreateExpectedStatTables() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            for (String tableName : new String[]{
                    "analysis_logger_stat", "analysis_thread_stat", "analysis_status_code_stat",
                    "analysis_http_method_stat", "analysis_timeline_stat"}) {
                try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                    assertThat(tables.next()).as("Tablo bulunmali: " + tableName).isTrue();
                }
            }
        }
    }

    @Test
    void v5MigrationsAddExpectedColumns() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            for (String columnName : new String[]{
                    "requested_parser_type", "detected_log_format", "parsed_entry_count",
                    "unparsed_line_count", "first_log_timestamp", "last_log_timestamp",
                    "multiline_exception_count", "parse_quality_score", "format_confidence",
                    "format_detection_sample_size", "matched_sample_count"}) {
                try (ResultSet columns = metaData.getColumns(null, null, "log_analysis", columnName)) {
                    assertThat(columns.next()).as("log_analysis sutunu bulunmali: " + columnName).isTrue();
                }
            }

            for (String columnName : new String[]{
                    "requested_parser_type", "detected_log_format", "filter_start_time", "filter_end_time",
                    "filter_levels", "filter_logger", "filter_thread", "filter_message_contains",
                    "filter_status_codes", "filter_http_methods", "filter_path_contains"}) {
                try (ResultSet columns = metaData.getColumns(null, null, "analysis_job", columnName)) {
                    assertThat(columns.next()).as("analysis_job sutunu bulunmali: " + columnName).isTrue();
                }
            }

            try (ResultSet columns = metaData.getColumns(null, null, "frequent_error", "normalized_message")) {
                assertThat(columns.next()).isTrue();
            }
        }
    }
}