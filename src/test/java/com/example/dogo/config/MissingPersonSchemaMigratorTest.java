package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MissingPersonSchemaMigratorTest {

	@Test
	void migrateAddsSourceColumnsToExistingMissingPersonTable() throws Exception {
		DataSource dataSource = dataSource();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
				CREATE TABLE MISSING_PERSON_REPORT (
				  REPORT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
				  USER_NO BIGINT,
				  AGE INT NOT NULL,
				  NATIONALITY VARCHAR(100) NOT NULL,
				  OCCURRED_AT DATETIME NOT NULL,
				  OCCURRED_PLACE VARCHAR(500) NOT NULL,
				  BODY_TYPE VARCHAR(100) NOT NULL,
				  FACE_SHAPE VARCHAR(100) NOT NULL,
				  HAIR_COLOR VARCHAR(100) NOT NULL,
				  HAIR_STYLE VARCHAR(100) NOT NULL,
				  CLOTHING VARCHAR(1000) NOT NULL,
				  STATUS VARCHAR(30) NOT NULL DEFAULT 'OPEN',
				  IS_DELETED BOOLEAN NOT NULL DEFAULT FALSE,
				  REGDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  MODDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");

		new MissingPersonSchemaMigrator(jdbcTemplate, dataSource).migrate();

		List<String> columns = jdbcTemplate.queryForList(
				"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'MISSING_PERSON_REPORT'",
				String.class
		);
		assertThat(columns).contains("SOURCE_TYPE", "EXTERNAL_ID", "API_PROVIDER", "RAW_PAYLOAD", "SYNCED_AT", "PERSON_NAME", "GENDER");
	}

	private DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:missing_person_migrator;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}
}
