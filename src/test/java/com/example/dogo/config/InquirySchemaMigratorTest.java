package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InquirySchemaMigratorTest {

    @Test
    void migrateAddsPrivateColumnToExistingInquiryTable() throws Exception {
        DataSource dataSource = dataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE INQUIRY (
                  INQUIRY_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  USER_NO BIGINT,
                  CATEGORY VARCHAR(20) NOT NULL,
                  TITLE VARCHAR(200) NOT NULL,
                  CONTENT TEXT NOT NULL,
                  STATUS VARCHAR(30) NOT NULL DEFAULT 'UNREAD',
                  ANSWER TEXT,
                  ANSWERED_AT DATETIME,
                  REGDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  MODDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        new InquirySchemaMigrator(jdbcTemplate, dataSource).migrate();

        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'INQUIRY'",
                String.class
        );
        assertThat(columns).contains("IS_PRIVATE");
    }

    private DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:inquiry_migrator;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
