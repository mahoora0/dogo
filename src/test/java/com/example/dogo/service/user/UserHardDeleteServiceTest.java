package com.example.dogo.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(UserHardDeleteServiceTest.TestConfig.class)
class UserHardDeleteServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserHardDeleteService userHardDeleteService;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP ALL OBJECTS");
        List.of(
                "CREATE TABLE USERS (USER_NO BIGINT PRIMARY KEY)",
                "CREATE TABLE USER_SOCIAL_ACCOUNT (ID BIGINT PRIMARY KEY, USER_NO BIGINT NOT NULL, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE POST_REPORT (REPORT_ID BIGINT PRIMARY KEY, REPORTER_NO BIGINT NOT NULL, HANDLER_NO BIGINT, TARGET_OWNER_NO BIGINT NOT NULL, FOREIGN KEY (REPORTER_NO) REFERENCES USERS(USER_NO), FOREIGN KEY (HANDLER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE LOST_ITEM (LOST_ID BIGINT PRIMARY KEY, USER_NO BIGINT, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE FOUND_ITEM (FOUND_ID BIGINT PRIMARY KEY, USER_NO BIGINT, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE CHAT_ROOM (ROOM_ID BIGINT PRIMARY KEY, LOST_ID BIGINT, FOUND_ID BIGINT, INQUIRER_NO BIGINT NOT NULL, OWNER_NO BIGINT NOT NULL, FOREIGN KEY (LOST_ID) REFERENCES LOST_ITEM(LOST_ID), FOREIGN KEY (FOUND_ID) REFERENCES FOUND_ITEM(FOUND_ID), FOREIGN KEY (INQUIRER_NO) REFERENCES USERS(USER_NO), FOREIGN KEY (OWNER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE CHAT_MESSAGE (MESSAGE_ID BIGINT PRIMARY KEY, ROOM_ID BIGINT NOT NULL, SENDER_NO BIGINT NOT NULL, FOREIGN KEY (ROOM_ID) REFERENCES CHAT_ROOM(ROOM_ID), FOREIGN KEY (SENDER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE INQUIRY (INQUIRY_ID BIGINT PRIMARY KEY, USER_NO BIGINT, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE INQUIRY_FILE (FILE_ID BIGINT PRIMARY KEY, INQUIRY_ID BIGINT NOT NULL, FOREIGN KEY (INQUIRY_ID) REFERENCES INQUIRY(INQUIRY_ID))",
                "CREATE TABLE ANIMAL_REPORT (REPORT_ID BIGINT PRIMARY KEY, USER_NO BIGINT, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE ANIMAL_REPORT_IMAGE (IMAGE_ID BIGINT PRIMARY KEY, REPORT_ID BIGINT NOT NULL, FOREIGN KEY (REPORT_ID) REFERENCES ANIMAL_REPORT(REPORT_ID))",
                "CREATE TABLE ANIMAL_REPORT_IMAGE_EMBEDDING (EMBEDDING_ID BIGINT PRIMARY KEY, REPORT_ID BIGINT NOT NULL, FOREIGN KEY (REPORT_ID) REFERENCES ANIMAL_REPORT(REPORT_ID))",
                "CREATE TABLE ANIMAL_REPORT_MATCH (MATCH_ID BIGINT PRIMARY KEY, MISSING_REPORT_ID BIGINT NOT NULL, SIGHTING_REPORT_ID BIGINT NOT NULL, FOREIGN KEY (MISSING_REPORT_ID) REFERENCES ANIMAL_REPORT(REPORT_ID), FOREIGN KEY (SIGHTING_REPORT_ID) REFERENCES ANIMAL_REPORT(REPORT_ID))",
                "CREATE TABLE MISSING_PERSON_REPORT (REPORT_ID BIGINT PRIMARY KEY, USER_NO BIGINT, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))",
                "CREATE TABLE LOST_ITEM_IMAGE (IMAGE_ID BIGINT PRIMARY KEY, LOST_ID BIGINT NOT NULL, FOREIGN KEY (LOST_ID) REFERENCES LOST_ITEM(LOST_ID))",
                "CREATE TABLE FOUND_ITEM_IMAGE (IMAGE_ID BIGINT PRIMARY KEY, FOUND_ID BIGINT NOT NULL, FOREIGN KEY (FOUND_ID) REFERENCES FOUND_ITEM(FOUND_ID))",
                "CREATE TABLE ITEM_EMBEDDING (EMBEDDING_ID BIGINT PRIMARY KEY, ITEM_TYPE VARCHAR(10) NOT NULL, ITEM_ID BIGINT NOT NULL)",
                "CREATE TABLE ITEM_MATCH (MATCH_ID BIGINT PRIMARY KEY, LOST_ID BIGINT NOT NULL, FOUND_ID BIGINT NOT NULL, FOREIGN KEY (LOST_ID) REFERENCES LOST_ITEM(LOST_ID), FOREIGN KEY (FOUND_ID) REFERENCES FOUND_ITEM(FOUND_ID))"
        ).forEach(jdbcTemplate::execute);
    }

    @Test
    void deletesRelatedChatDataAndReporterReportsWhileKeepingHandledReportAudit() {
        jdbcTemplate.update("INSERT INTO USERS (USER_NO) VALUES (1), (2), (3)");
        jdbcTemplate.update("INSERT INTO USER_SOCIAL_ACCOUNT (ID, USER_NO) VALUES (10, 1)");
        jdbcTemplate.update("INSERT INTO LOST_ITEM (LOST_ID, USER_NO) VALUES (20, 1)");
        jdbcTemplate.update("INSERT INTO FOUND_ITEM (FOUND_ID, USER_NO) VALUES (21, 1), (22, 2)");
        jdbcTemplate.update("INSERT INTO ITEM_EMBEDDING (EMBEDDING_ID, ITEM_TYPE, ITEM_ID) VALUES (23, 'LOST', 20), (24, 'FOUND', 21), (25, 'FOUND', 22)");
        jdbcTemplate.update("INSERT INTO CHAT_ROOM (ROOM_ID, LOST_ID, INQUIRER_NO, OWNER_NO) VALUES (30, 20, 2, 2), (31, NULL, 2, 3)");
        jdbcTemplate.update("INSERT INTO CHAT_MESSAGE (MESSAGE_ID, ROOM_ID, SENDER_NO) VALUES (40, 30, 2), (41, 31, 1), (42, 31, 2)");
        jdbcTemplate.update("INSERT INTO POST_REPORT (REPORT_ID, REPORTER_NO, HANDLER_NO, TARGET_OWNER_NO) VALUES (50, 1, NULL, 2), (51, 2, 1, 1)");

        userHardDeleteService.deleteUser(1L);

        assertThat(count("USERS", "USER_NO = 1")).isZero();
        assertThat(count("USER_SOCIAL_ACCOUNT", "USER_NO = 1")).isZero();
        assertThat(count("CHAT_ROOM", "ROOM_ID = 30")).isZero();
        assertThat(count("CHAT_MESSAGE", "MESSAGE_ID IN (40, 41)")).isZero();
        assertThat(count("CHAT_ROOM", "ROOM_ID = 31")).isOne();
        assertThat(count("CHAT_MESSAGE", "MESSAGE_ID = 42")).isOne();
        assertThat(count("POST_REPORT", "REPORT_ID = 50")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT HANDLER_NO FROM POST_REPORT WHERE REPORT_ID = 51", Long.class)).isNull();
        assertThat(count("ITEM_EMBEDDING", "EMBEDDING_ID IN (23, 24)")).isZero();
        assertThat(count("ITEM_EMBEDDING", "EMBEDDING_ID = 25")).isOne();
    }

    @Test
    void rollsBackAllDeletesWhenUserRowStillHasAnUnhandledReference() {
        jdbcTemplate.execute("CREATE TABLE USER_DELETE_BLOCKER (ID BIGINT PRIMARY KEY, USER_NO BIGINT NOT NULL, FOREIGN KEY (USER_NO) REFERENCES USERS(USER_NO))");
        jdbcTemplate.update("INSERT INTO USERS (USER_NO) VALUES (1)");
        jdbcTemplate.update("INSERT INTO USER_SOCIAL_ACCOUNT (ID, USER_NO) VALUES (10, 1)");
        jdbcTemplate.update("INSERT INTO USER_DELETE_BLOCKER (ID, USER_NO) VALUES (20, 1)");

        assertThatThrownBy(() -> userHardDeleteService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class);

        assertThat(count("USERS", "USER_NO = 1")).isOne();
        assertThat(count("USER_SOCIAL_ACCOUNT", "USER_NO = 1")).isOne();
    }

    private long count(String table, String where) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
    }

    @Configuration
    @EnableTransactionManagement
    @Import(UserHardDeleteService.class)
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:mem:user_hard_delete;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }
}
