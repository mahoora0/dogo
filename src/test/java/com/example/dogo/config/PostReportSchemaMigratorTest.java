package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostReportSchemaMigratorTest {

    @Test
    void migrateAddsChatMessageToTargetTypeConstraint() throws Exception {
        DataSource dataSource = dataSource("post_report_migrator_allow");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyPostReportTable(jdbcTemplate);

        assertThat(targetTypeCheckClause(jdbcTemplate)).doesNotContain("CHAT_MESSAGE");

        new PostReportSchemaMigrator(jdbcTemplate, dataSource).migrate();

        assertThat(targetTypeCheckClause(jdbcTemplate)).contains("CHAT_MESSAGE");
    }

    @Test
    void migrateIsIdempotent() throws Exception {
        DataSource dataSource = dataSource("post_report_migrator_idempotent");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createLegacyPostReportTable(jdbcTemplate);

        PostReportSchemaMigrator migrator = new PostReportSchemaMigrator(jdbcTemplate, dataSource);
        migrator.migrate();
        migrator.migrate(); // second run must not fail or duplicate the constraint

        List<String> clauses = jdbcTemplate.queryForList(
                "SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                        + "WHERE CONSTRAINT_NAME = 'CK_POST_REPORT_TARGET_TYPE'",
                String.class);
        assertThat(clauses).hasSize(1);
        assertThat(clauses.get(0)).contains("CHAT_MESSAGE");
    }

    @Test
    void migrateSkipsWhenConstraintAlreadyAllowsChatMessage() throws Exception {
        DataSource dataSource = dataSource("post_report_migrator_skip");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE POST_REPORT (
                  REPORT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  TARGET_TYPE VARCHAR(30) NOT NULL,
                  CONSTRAINT CK_POST_REPORT_TARGET_TYPE
                    CHECK (TARGET_TYPE IN ('LOST_ITEM', 'FOUND_ITEM', 'ANIMAL_REPORT', 'MISSING_PERSON', 'CHAT_MESSAGE'))
                )
                """);

        // Should be a no-op and must not throw.
        new PostReportSchemaMigrator(jdbcTemplate, dataSource).migrate();

        assertThat(targetTypeCheckClause(jdbcTemplate)).contains("CHAT_MESSAGE");
    }

    @Test
    void migrateDeletesOnlyOrphanedChatReports() throws Exception {
        DataSource dataSource = dataSource("post_report_migrator_orphan");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE POST_REPORT (
                  REPORT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  REPORTER_NO BIGINT NOT NULL,
                  TARGET_TYPE VARCHAR(30) NOT NULL,
                  TARGET_ID BIGINT NOT NULL,
                  TARGET_OWNER_NO BIGINT NOT NULL,
                  TARGET_TITLE VARCHAR(300) NOT NULL,
                  TARGET_URL VARCHAR(500) NOT NULL,
                  REASON_TYPE VARCHAR(30) NOT NULL,
                  STATUS VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                  CONSTRAINT CK_POST_REPORT_TARGET_TYPE
                    CHECK (TARGET_TYPE IN ('LOST_ITEM', 'FOUND_ITEM', 'ANIMAL_REPORT', 'MISSING_PERSON', 'CHAT_MESSAGE'))
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE CHAT_MESSAGE (
                  MESSAGE_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  CONTENT VARCHAR(100)
                )
                """);
        // 존재하는 메시지 1개 (id=1)
        jdbcTemplate.execute("INSERT INTO CHAT_MESSAGE (CONTENT) VALUES ('hi')");
        // 살아있는 메시지(1)에 대한 신고 + 고아 메시지(999)에 대한 신고 + 게시글 신고
        jdbcTemplate.execute("INSERT INTO POST_REPORT (REPORTER_NO, TARGET_TYPE, TARGET_ID, TARGET_OWNER_NO, TARGET_TITLE, TARGET_URL, REASON_TYPE, STATUS) VALUES (10, 'CHAT_MESSAGE', 1, 20, 'a: hi', '/admin/reports/chat/1', 'FRAUD', 'PENDING')");
        jdbcTemplate.execute("INSERT INTO POST_REPORT (REPORTER_NO, TARGET_TYPE, TARGET_ID, TARGET_OWNER_NO, TARGET_TITLE, TARGET_URL, REASON_TYPE, STATUS) VALUES (10, 'CHAT_MESSAGE', 999, 20, 'b: gone', '/admin/reports/chat/9', 'FRAUD', 'PENDING')");
        jdbcTemplate.execute("INSERT INTO POST_REPORT (REPORTER_NO, TARGET_TYPE, TARGET_ID, TARGET_OWNER_NO, TARGET_TITLE, TARGET_URL, REASON_TYPE, STATUS) VALUES (10, 'LOST_ITEM', 999, 20, 'lost', '/lost-items/999', 'FRAUD', 'PENDING')");

        new PostReportSchemaMigrator(jdbcTemplate, dataSource).migrate();

        // 고아 채팅 신고(999)만 삭제, 살아있는 채팅 신고(1)와 게시글 신고는 유지
        Integer orphan = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM POST_REPORT WHERE TARGET_TYPE = 'CHAT_MESSAGE' AND TARGET_ID = 999", Integer.class);
        Integer alive = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM POST_REPORT WHERE TARGET_TYPE = 'CHAT_MESSAGE' AND TARGET_ID = 1", Integer.class);
        Integer post = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM POST_REPORT WHERE TARGET_TYPE = 'LOST_ITEM'", Integer.class);
        assertThat(orphan).isZero();
        assertThat(alive).isEqualTo(1);
        assertThat(post).isEqualTo(1);
    }

    private String targetTypeCheckClause(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS "
                        + "WHERE CONSTRAINT_NAME = 'CK_POST_REPORT_TARGET_TYPE'",
                String.class);
    }

    private void createLegacyPostReportTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE POST_REPORT (
                  REPORT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                  TARGET_TYPE VARCHAR(30) NOT NULL,
                  CONSTRAINT CK_POST_REPORT_TARGET_TYPE
                    CHECK (TARGET_TYPE IN ('LOST_ITEM', 'FOUND_ITEM', 'ANIMAL_REPORT', 'MISSING_PERSON'))
                )
                """);
    }

    private DataSource dataSource(String name) {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:" + name + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
                "sa", "", true);
        dataSource.setDriverClassName("org.h2.Driver");
        return dataSource;
    }
}
