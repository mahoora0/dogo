package com.example.dogo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ChatSchemaMigrator implements ApplicationRunner {

    private static final String MESSAGE_TABLE_NAME = "CHAT_MESSAGE";
    private static final String ROOM_TABLE_NAME = "CHAT_ROOM";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public ChatSchemaMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrate();
    }

    public void migrate() throws SQLException {
        if (tableExists(MESSAGE_TABLE_NAME)) {
            addColumnIfMissing(MESSAGE_TABLE_NAME, "IS_READ", "IS_READ BOOLEAN NOT NULL DEFAULT FALSE");
            addColumnIfMissing(MESSAGE_TABLE_NAME, "FILE_URL", "FILE_URL VARCHAR(500) NULL");
            addColumnIfMissing(MESSAGE_TABLE_NAME, "FILE_NAME", "FILE_NAME VARCHAR(255) NULL");
            addColumnIfMissing(MESSAGE_TABLE_NAME, "FILE_SIZE", "FILE_SIZE BIGINT NULL");
            addColumnIfMissing(MESSAGE_TABLE_NAME, "FILE_GROUP_ID", "FILE_GROUP_ID VARCHAR(80) NULL");
        }
        if (tableExists(ROOM_TABLE_NAME)) {
            addColumnIfMissing(ROOM_TABLE_NAME, "ANIMAL_REPORT_ID", "ANIMAL_REPORT_ID BIGINT NULL");
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) throws SQLException {
        if (!columnExists(tableName, columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                return tables.next();
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            if (hasColumn(metadata, connection, tableName, columnName)) {
                return true;
            }
            return hasColumn(metadata, connection, tableName, columnName.toLowerCase());
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }
}
