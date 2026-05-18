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
public class MissingPersonSchemaMigrator implements ApplicationRunner {

	private static final String TABLE_NAME = "MISSING_PERSON_REPORT";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public MissingPersonSchemaMigrator(JdbcTemplate jdbcTemplate, DataSource dataSource) {
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

		addColumnIfMissing("SOURCE_TYPE", "SOURCE_TYPE VARCHAR(30) NOT NULL DEFAULT 'USER'");
		addColumnIfMissing("EXTERNAL_ID", "EXTERNAL_ID VARCHAR(100)");
		addColumnIfMissing("API_PROVIDER", "API_PROVIDER VARCHAR(50)");
		addColumnIfMissing("RAW_PAYLOAD", "RAW_PAYLOAD TEXT");
		addColumnIfMissing("SYNCED_AT", "SYNCED_AT DATETIME");
	}

	private void addColumnIfMissing(String columnName, String columnDefinition) throws SQLException {
		if (!columnExists(columnName)) {
			jdbcTemplate.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + columnDefinition);
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

	private boolean hasColumn(DatabaseMetaData metadata, Connection connection, String columnName) throws SQLException {
		try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, TABLE_NAME, columnName)) {
			return columns.next();
		}
	}
}
