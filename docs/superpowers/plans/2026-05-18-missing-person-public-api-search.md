# Missing Person Public API Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the missing person board so user-created reports and future public Missing Alert OpenAPI records can be stored, searched, filtered, and displayed together.

**Architecture:** Reuse the existing single-table source pattern from lost/found items. `MISSING_PERSON_REPORT` gets source metadata, the entity exposes explicit user/public creation paths, and search remains a single Spring Data JPA specification over persisted rows.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA Specifications, Thymeleaf, H2/MySQL-compatible schema SQL, JUnit 5, Mockito, MockMvc.

---

## File Structure

- Modify `src/main/resources/schema.sql`: add source metadata columns, constraints, unique key, and search index to `MISSING_PERSON_REPORT`.
- Modify `src/main/java/com/example/dogo/entity/missing/MissingPersonReport.java`: add source fields and a `fromPublicApi` factory.
- Modify `src/main/java/com/example/dogo/dto/missing/MissingPersonView.java`: add source metadata for list cards.
- Modify `src/main/java/com/example/dogo/dto/missing/MissingPersonDetailView.java`: add source metadata for detail pages.
- Modify `src/main/java/com/example/dogo/service/missing/MissingPersonService.java`: map source metadata and add optional `sourceType` filtering.
- Modify `src/main/java/com/example/dogo/controller/missing/MissingPersonController.java`: accept and preserve `sourceType`.
- Modify `src/main/resources/templates/missing-persons/list.html`: show source labels and add source filter.
- Modify `src/main/resources/templates/missing-persons/detail.html`: show source label.
- Modify `src/test/java/com/example/dogo/service/missing/MissingPersonServiceTest.java`: cover user/public source mapping and filtering.
- Modify `src/test/java/com/example/dogo/controller/missing/MissingPersonControllerTest.java`: cover `sourceType` request/model handling.

---

### Task 1: Source Metadata Model

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/java/com/example/dogo/entity/missing/MissingPersonReport.java`
- Test: `src/test/java/com/example/dogo/service/missing/MissingPersonServiceTest.java`

- [ ] **Step 1: Write failing source metadata tests**

Add tests proving user-created reports default to `USER` and public API reports can be created with import metadata:

```java
@Test
void userCreatedReportDefaultsToUserSource() {
	MissingPersonReport report = new MissingPersonReport(
			new User("dev@dogo.local", "dev", "010-0000-0000"),
			13,
			"Korea",
			LocalDateTime.of(2026, 5, 18, 9, 30),
			"Seoul",
			170,
			new BigDecimal("58.0"),
			"Slim",
			"Oval",
			"Black",
			"Short",
			"Blue hoodie"
	);

	assertThat(report.getSourceType()).isEqualTo("USER");
assertThat(report.getSourceLabel()).isEqualTo("사용자 제보");
	assertThat(report.getExternalId()).isNull();
}

@Test
void publicApiReportStoresImportMetadata() {
	MissingPersonReport report = MissingPersonReport.fromPublicApi(
			"MISSING_ALERT",
			"case-20260518-001",
			"{\"id\":\"case-20260518-001\"}",
			13,
			"Korea",
			LocalDateTime.of(2026, 5, 18, 9, 30),
			"Seoul",
			170,
			new BigDecimal("58.0"),
			"Slim",
			"Oval",
			"Black",
			"Short",
			"Blue hoodie"
	);

	assertThat(report.getSourceType()).isEqualTo("PUBLIC_API");
assertThat(report.getSourceLabel()).isEqualTo("공공데이터");
	assertThat(report.getApiProvider()).isEqualTo("MISSING_ALERT");
	assertThat(report.getExternalId()).isEqualTo("case-20260518-001");
	assertThat(report.getRawPayload()).contains("case-20260518-001");
	assertThat(report.getSyncedAt()).isNotNull();
}
```

- [ ] **Step 2: Run the failing tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.service.missing.MissingPersonServiceTest
```

Expected: FAIL because `getSourceType`, `getSourceLabel`, `fromPublicApi`, and metadata getters do not exist yet.

- [ ] **Step 3: Implement schema and entity source fields**

In `schema.sql`, add the source columns to `MISSING_PERSON_REPORT`, replace the status-only check with source-aware checks, and add a public API unique key:

```sql
  USER_NO BIGINT,
  SOURCE_TYPE VARCHAR(30) NOT NULL DEFAULT 'USER',
  EXTERNAL_ID VARCHAR(100),
  API_PROVIDER VARCHAR(50),
  RAW_PAYLOAD TEXT,
  SYNCED_AT DATETIME,
```

```sql
  CONSTRAINT CK_MISSING_PERSON_SOURCE
    CHECK (
      (SOURCE_TYPE = 'USER' AND USER_NO IS NOT NULL AND EXTERNAL_ID IS NULL)
      OR
      (SOURCE_TYPE = 'PUBLIC_API' AND EXTERNAL_ID IS NOT NULL)
    ),
  UNIQUE KEY UK_MISSING_PERSON_PUBLIC_API (API_PROVIDER, EXTERNAL_ID),
  INDEX IDX_MISSING_PERSON_LIST (SOURCE_TYPE, STATUS, OCCURRED_AT DESC, REPORT_ID DESC),
```

In `MissingPersonReport`, add fields:

```java
@Column(name = "SOURCE_TYPE", nullable = false)
private String sourceType = "USER";

@Column(name = "EXTERNAL_ID")
private String externalId;

@Column(name = "API_PROVIDER")
private String apiProvider;

@Column(name = "RAW_PAYLOAD")
private String rawPayload;

@Column(name = "SYNCED_AT")
private LocalDateTime syncedAt;
```

Add a factory and label helper:

```java
public static MissingPersonReport fromPublicApi(
		String apiProvider,
		String externalId,
		String rawPayload,
		Integer age,
		String nationality,
		LocalDateTime occurredAt,
		String occurredPlace,
		Integer heightCm,
		BigDecimal weightKg,
		String bodyType,
		String faceShape,
		String hairColor,
		String hairStyle,
		String clothing
) {
	MissingPersonReport report = new MissingPersonReport();
	report.sourceType = "PUBLIC_API";
	report.apiProvider = apiProvider;
	report.externalId = externalId;
	report.rawPayload = rawPayload;
	report.syncedAt = LocalDateTime.now();
	report.age = age;
	report.nationality = nationality;
	report.occurredAt = occurredAt;
	report.occurredPlace = occurredPlace;
	report.heightCm = heightCm;
	report.weightKg = weightKg;
	report.bodyType = bodyType;
	report.faceShape = faceShape;
	report.hairColor = hairColor;
	report.hairStyle = hairStyle;
	report.clothing = clothing;
	return report;
}

public String getSourceLabel() {
	return "PUBLIC_API".equals(sourceType) ? "공공데이터" : "사용자 제보";
}
```

- [ ] **Step 4: Run the source metadata tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.service.missing.MissingPersonServiceTest
```

Expected: PASS for the new source metadata tests.

### Task 2: Search and DTO Source Filtering

**Files:**
- Modify: `src/main/java/com/example/dogo/dto/missing/MissingPersonView.java`
- Modify: `src/main/java/com/example/dogo/dto/missing/MissingPersonDetailView.java`
- Modify: `src/main/java/com/example/dogo/service/missing/MissingPersonService.java`
- Test: `src/test/java/com/example/dogo/service/missing/MissingPersonServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Update view assertions to expect source metadata and add a source filter verification:

```java
assertThat(result.getContent().get(0).sourceType()).isEqualTo("USER");
assertThat(result.getContent().get(0).sourceLabel()).isEqualTo("User report");
```

Add:

```java
@Test
void searchAcceptsSourceTypeFilter() {
	when(missingPersonRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(List.of()));

	missingPersonService.search("Seoul", "OPEN", "PUBLIC_API", PageRequest.of(0, 9));

	verify(missingPersonRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
}
```

- [ ] **Step 2: Run the failing service tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.service.missing.MissingPersonServiceTest
```

Expected: FAIL because the service method signature and DTO fields do not include source metadata.

- [ ] **Step 3: Update DTOs and service mapping**

Add `sourceType` and `sourceLabel` as the last fields in both missing-person records:

```java
String status,
String statusLabel,
String sourceType,
String sourceLabel
```

Change service search signature:

```java
public Page<MissingPersonView> search(String keyword, String status, String sourceType, Pageable pageable)
```

Add source filtering to `searchSpec`:

```java
String normalizedSourceType = blankToNull(sourceType);
if (normalizedSourceType != null) {
	predicates.add(criteriaBuilder.equal(root.get("sourceType"), normalizedSourceType));
}
```

Map the new DTO fields:

```java
report.getStatus(),
statusLabel(report.getStatus()),
report.getSourceType(),
report.getSourceLabel()
```

- [ ] **Step 4: Run service tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.service.missing.MissingPersonServiceTest
```

Expected: PASS.

### Task 3: Controller and UI Source Filter

**Files:**
- Modify: `src/main/java/com/example/dogo/controller/missing/MissingPersonController.java`
- Modify: `src/main/resources/templates/missing-persons/list.html`
- Modify: `src/main/resources/templates/missing-persons/detail.html`
- Test: `src/test/java/com/example/dogo/controller/missing/MissingPersonControllerTest.java`

- [ ] **Step 1: Write failing controller test changes**

Update mocked `MissingPersonView` and `MissingPersonDetailView` constructor calls with:

```java
"USER",
"User report"
```

Update list stubbing:

```java
when(missingPersonService.search(eq("Korea"), eq("OPEN"), eq("PUBLIC_API"), any(PageRequest.class)))
		.thenReturn(new PageImpl<>(List.of(report)));
```

Update request and assertions:

```java
mockMvc.perform(get("/missing-persons")
		.param("keyword", "Korea")
		.param("status", "OPEN")
		.param("sourceType", "PUBLIC_API"))
	.andExpect(model().attribute("sourceType", "PUBLIC_API"));
```

- [ ] **Step 2: Run the failing controller tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.controller.missing.MissingPersonControllerTest
```

Expected: FAIL because the controller does not accept or preserve `sourceType`.

- [ ] **Step 3: Update controller and templates**

Add request parameter:

```java
@RequestParam(required = false) String sourceType,
```

Call service with source type:

```java
Page<?> reportPage = missingPersonService.search(keyword, status, sourceType, PageRequest.of(safePage, safeSize, sort));
```

Preserve the model value:

```java
model.addAttribute("sourceType", sourceType);
```

In list/detail templates, display `report.sourceLabel`. Add the `sourceType` query parameter to pagination links and a compact source filter beside the existing status filters.

- [ ] **Step 4: Run controller tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.controller.missing.MissingPersonControllerTest
```

Expected: PASS.

### Task 4: Full Verification

**Files:**
- Verify all modified files.

- [ ] **Step 1: Run focused missing-person tests**

Run:

```powershell
./gradlew.bat test --tests com.example.dogo.service.missing.MissingPersonServiceTest --tests com.example.dogo.controller.missing.MissingPersonControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run full test suite**

Run:

```powershell
./gradlew.bat test
```

Expected: PASS, or report unrelated pre-existing failures with exact failing tests.

- [ ] **Step 3: Review diff**

Run:

```powershell
git diff --check
git diff --stat
```

Expected: no whitespace errors; changes limited to schema, missing-person entity/service/controller/DTO/templates/tests, and plan docs.
