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
public class AnimalReportSchemaMigrator implements ApplicationRunner {

	private static final String TABLE_NAME = "ANIMAL_REPORT";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public AnimalReportSchemaMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		migrate();
	}

	public void migrate() throws SQLException {
		if (!tableExists()) {
			return;
		}

		makeUserNoNullable();
		addColumnIfMissing("SOURCE_TYPE", "SOURCE_TYPE VARCHAR(40) NOT NULL DEFAULT 'USER'");
		addColumnIfMissing("API_PROVIDER", "API_PROVIDER VARCHAR(50)");
		addColumnIfMissing("EXTERNAL_ID", "EXTERNAL_ID VARCHAR(120)");
		addColumnIfMissing("RAW_PAYLOAD", "RAW_PAYLOAD TEXT");
		addColumnIfMissing("SYNCED_AT", "SYNCED_AT DATETIME");
		addColumnIfMissing("CARE_LOCATION_NAME", "CARE_LOCATION_NAME VARCHAR(200)");
		addColumnIfMissing("CARE_LOCATION_ADDRESS", "CARE_LOCATION_ADDRESS VARCHAR(500)");
		addColumnIfMissing("CARE_CONTACT_PHONE", "CARE_CONTACT_PHONE VARCHAR(100)");
		addIndexIfMissing("UK_ANIMAL_PUBLIC_API", "CREATE UNIQUE INDEX UK_ANIMAL_PUBLIC_API ON " + TABLE_NAME + " (API_PROVIDER, EXTERNAL_ID)");
		addIndexIfMissing("IDX_ANIMAL_REPORT_SOURCE", "CREATE INDEX IDX_ANIMAL_REPORT_SOURCE ON " + TABLE_NAME + " (SOURCE_TYPE, EVENT_DATE)");
		migrateReportTypeConstraint();
		migrateExistingProtectionRecords();
	}

	private void migrateReportTypeConstraint() {
		// MySQL 8.0.16+ uses DROP CHECK; H2 / MySQL 8.0.19+ use DROP CONSTRAINT. Try both.
		try {
			jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT CK_ANIMAL_REPORT_TYPE");
		} catch (Exception ignored) {
			try {
				jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " DROP CHECK CK_ANIMAL_REPORT_TYPE");
			} catch (Exception alsoIgnored) {
				// Constraint may not exist yet (fresh schema)
			}
		}
		try {
			jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME
					+ " ADD CONSTRAINT CK_ANIMAL_REPORT_TYPE CHECK (REPORT_TYPE IN "
					+ "('MISSING', 'SIGHTING', 'PROTECTING', 'RETURNED', 'TRANSFERRED'))");
		} catch (Exception ignored) {
			// Constraint already up to date or DB does not enforce check constraints
		}
	}

	private void makeUserNoNullable() {
		try {
			jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " MODIFY COLUMN USER_NO BIGINT NULL");
		} catch (Exception ignored) {
			try {
				jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ALTER COLUMN USER_NO SET NULL");
			} catch (Exception alsoIgnored) {
			}
		}
	}

	private void addColumnIfMissing(String columnName, String columnDefinition) throws SQLException {
		if (!columnExists(columnName)) {
			jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnDefinition);
		}
	}

	private void addIndexIfMissing(String indexName, String statement) throws SQLException {
		if (!indexExists(indexName)) {
			jdbcTemplate.execute(statement);
		}
	}

	private boolean tableExists() throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, TABLE_NAME, new String[]{"TABLE"})) {
				if (tables.next()) {
					return true;
				}
			}
			try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, TABLE_NAME.toLowerCase(), new String[]{"TABLE"})) {
				return tables.next();
			}
		}
	}

	private boolean columnExists(String columnName) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			if (hasColumn(metadata, connection, columnName)) {
				return true;
			}
			return hasColumn(metadata, connection, columnName.toLowerCase());
		}
	}

	private boolean indexExists(String indexName) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, TABLE_NAME, false, false)) {
				while (indexes.next()) {
					String foundName = indexes.getString("INDEX_NAME");
					if (indexName.equalsIgnoreCase(foundName)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private boolean hasColumn(DatabaseMetaData metadata, Connection connection, String columnName) throws SQLException {
		try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, TABLE_NAME, columnName)) {
			return columns.next();
		}
	}

	private void migrateExistingProtectionRecords() {
		try {
			String query = "SELECT REPORT_ID, RAW_PAYLOAD FROM ANIMAL_REPORT WHERE SOURCE_TYPE = 'ANIMAL_PROTECTION_API'";
			jdbcTemplate.query(query, (rs) -> {
				long reportId = rs.getLong("REPORT_ID");
				String rawPayload = rs.getString("RAW_PAYLOAD");
				String targetReportType = "TRANSFERRED";
				if (rawPayload != null) {
					if (rawPayload.contains("반환")) {
						targetReportType = "RETURNED";
					} else {
						targetReportType = "TRANSFERRED";
					}
				}
				jdbcTemplate.update("UPDATE ANIMAL_REPORT SET REPORT_TYPE = ?, SIGHTING_CARE_STATUS = NULL WHERE REPORT_ID = ?", targetReportType, reportId);
			});
		} catch (Exception e) {
			// Ignore database errors during startup migration
		}
	}
}
