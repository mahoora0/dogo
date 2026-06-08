package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSchemaMigratorTest {

    @Test
    void migrateAddsReadColumnToExistingChatMessageTable() throws Exception {
        DataSource dataSource = dataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE CHAT_MESSAGE (
                  MESSAGE_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  ROOM_ID BIGINT NOT NULL,
                  SENDER_NO BIGINT NOT NULL,
                  CONTENT VARCHAR(1000) NOT NULL,
                  MESSAGE_TYPE VARCHAR(20) NOT NULL,
                  CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE CHAT_ROOM (
                  ROOM_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  FOUND_ID BIGINT NULL,
                  LOST_ID BIGINT NULL,
                  INQUIRER_NO BIGINT NOT NULL,
                  OWNER_NO BIGINT NOT NULL,
                  CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        new ChatSchemaMigrator(jdbcTemplate, dataSource).migrate();

        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CHAT_MESSAGE'",
                String.class
        );
        assertThat(columns).contains("IS_READ", "FILE_GROUP_ID");
        List<String> roomColumns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CHAT_ROOM'",
                String.class
        );
        assertThat(roomColumns).contains("ANIMAL_REPORT_ID");
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:chat_migrator;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
