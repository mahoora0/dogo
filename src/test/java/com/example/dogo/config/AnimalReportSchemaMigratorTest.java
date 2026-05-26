package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalReportSchemaMigratorTest {

	@Test
	void migrateAddsPublicApiColumnsToExistingAnimalReportTable() throws Exception {
		DataSource dataSource = dataSource();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("""
				CREATE TABLE ANIMAL_REPORT (
				  REPORT_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
				  USER_NO BIGINT NOT NULL,
				  REPORT_TYPE VARCHAR(20) NOT NULL,
				  STATUS VARCHAR(30) NOT NULL DEFAULT 'OPEN',
				  TITLE VARCHAR(300),
				  EVENT_DATE DATE NOT NULL,
				  REGION_NAME VARCHAR(100) NOT NULL,
				  DETAIL_PLACE VARCHAR(500) NOT NULL,
				  CONTACT_PUBLIC BOOLEAN NOT NULL DEFAULT TRUE,
				  ANIMAL_TYPE VARCHAR(20) NOT NULL,
				  GENDER VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
				  NEUTERED_STATUS VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
				  VIEW_COUNT INT NOT NULL DEFAULT 0,
				  IS_DELETED BOOLEAN NOT NULL DEFAULT FALSE,
				  REGDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  MODDATE DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");

		new AnimalReportSchemaMigrator(jdbcTemplate, dataSource).migrate();

		List<String> columns = jdbcTemplate.queryForList(
				"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ANIMAL_REPORT'",
				String.class
		);
		assertThat(columns).contains("SOURCE_TYPE", "API_PROVIDER", "EXTERNAL_ID", "RAW_PAYLOAD", "SYNCED_AT");
		String nullable = jdbcTemplate.queryForObject(
				"SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ANIMAL_REPORT' AND COLUMN_NAME = 'USER_NO'",
				String.class
		);
		assertThat(nullable).isEqualTo("YES");
		List<String> indexes = jdbcTemplate.queryForList(
				"SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_NAME = 'ANIMAL_REPORT'",
				String.class
		);
		assertThat(indexes).contains("UK_ANIMAL_PUBLIC_API", "IDX_ANIMAL_REPORT_SOURCE");
	}

	private DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:animal_report_migrator;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}
}
