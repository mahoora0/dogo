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
		addIndexIfMissing("UK_ANIMAL_PUBLIC_API", "CREATE UNIQUE INDEX UK_ANIMAL_PUBLIC_API ON " + TABLE_NAME + " (API_PROVIDER, EXTERNAL_ID)");
		addIndexIfMissing("IDX_ANIMAL_REPORT_SOURCE", "CREATE INDEX IDX_ANIMAL_REPORT_SOURCE ON " + TABLE_NAME + " (SOURCE_TYPE, EVENT_DATE)");
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
}
