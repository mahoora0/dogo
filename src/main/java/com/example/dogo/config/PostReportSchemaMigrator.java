package com.example.dogo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class PostReportSchemaMigrator implements ApplicationRunner {

    private static final String TABLE_NAME = "POST_REPORT";
    private static final String CONSTRAINT_NAME = "CK_POST_REPORT_TARGET_TYPE";
    private static final String CHECK_CLAUSE =
            "TARGET_TYPE IN ('LOST_ITEM', 'FOUND_ITEM', 'ANIMAL_REPORT', 'MISSING_PERSON', 'CHAT_MESSAGE')";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PostReportSchemaMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        migrate();
    }

    public void migrate() throws SQLException {
        if (!tableExists(TABLE_NAME)) {
            return;
        }
        migrateTargetTypeConstraint();
        cleanupOrphanedChatReports();
    }

    private void migrateTargetTypeConstraint() {
        if (constraintAllowsChatMessage()) {
            return;
        }
        dropConstraintIfExists();
        jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME
                + " ADD CONSTRAINT " + CONSTRAINT_NAME + " CHECK (" + CHECK_CLAUSE + ")");
    }

    /**
     * 채팅 테이블이 재생성되며 MESSAGE_ID가 재사용된 탓에, 더 이상 존재하지 않는 메시지를 가리키는
     * 옛 채팅 신고 기록이 남아 새 메시지 신고와 ID가 충돌(유령 중복)할 수 있다. 고아 기록만 정리한다.
     */
    private void cleanupOrphanedChatReports() throws SQLException {
        if (!tableExists("CHAT_MESSAGE")) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM " + TABLE_NAME + " WHERE TARGET_TYPE = 'CHAT_MESSAGE' "
                        + "AND TARGET_ID NOT IN (SELECT MESSAGE_ID FROM CHAT_MESSAGE)");
    }

    private boolean constraintAllowsChatMessage() {
        List<String> clauses = jdbcTemplate.queryForList(
                "SELECT CHECK_CLAUSE FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                String.class,
                CONSTRAINT_NAME);
        return clauses.stream().anyMatch(clause -> clause != null && clause.contains("CHAT_MESSAGE"));
    }

    private void dropConstraintIfExists() {
        try {
            jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + CONSTRAINT_NAME);
        } catch (DataAccessException ignored) {
            // Constraint does not exist yet; ADD below will create it.
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
}
