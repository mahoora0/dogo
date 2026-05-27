# Animal Public API Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import animal loss and rescued animal public API records into the animal report board so normal search returns both user and public data.

**Architecture:** Add source metadata to `AnimalReport`, parse both public APIs into one record model, map records into `ANIMAL_REPORT`, and run independent scheduled sync services. Search remains a local database query.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, RestClient, DOM XML parser, Thymeleaf, H2 tests.

---

### Task 1: Animal Report Source Metadata

**Files:**
- Modify: `src/main/java/com/example/dogo/entity/animal/AnimalReport.java`
- Modify: `src/main/resources/schema.sql`
- Test: `src/test/java/com/example/dogo/service/animal/AnimalReportServiceSearchTest.java`

- [x] Write failing tests proving public API rows can be created without a user and are returned by existing search.
- [x] Run the focused test and verify it fails before implementation.
- [x] Add source metadata fields, nullable user mapping, public API factory, and schema columns.
- [x] Run the focused test and verify it passes.

### Task 2: Public API Parser

**Files:**
- Create: `src/main/java/com/example/dogo/dto/animal/AnimalPublicApiPage.java`
- Create: `src/main/java/com/example/dogo/dto/animal/AnimalPublicApiRecord.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalPublicApiXmlParser.java`
- Test: `src/test/java/com/example/dogo/service/animal/api/AnimalPublicApiXmlParserTest.java`

- [x] Write failing parser tests for loss API and protection API sample XML.
- [x] Run the parser test and verify it fails before implementation.
- [x] Implement a flexible XML parser that accepts common v2 response fields.
- [x] Run the parser test and verify it passes.

### Task 3: Public API Clients

**Files:**
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalLossApiClient.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalProtectionApiClient.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/DataGoKrAnimalLossApiClient.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/DataGoKrAnimalProtectionApiClient.java`

- [x] Implement RestClient wrappers for the configured base URLs and service keys.
- [x] Request XML output and date/page query parameters.
- [x] Fail fast with a clear exception when a service key is absent.

### Task 4: Mapper, Sync Service, and Images

**Files:**
- Create: `src/main/java/com/example/dogo/dto/animal/AnimalPublicApiSyncResult.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalPublicApiMapper.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalPublicApiImageService.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalPublicApiSyncService.java`
- Test: `src/test/java/com/example/dogo/service/animal/api/AnimalPublicApiSyncServiceTest.java`

- [x] Write failing sync tests for saving new loss/protection records and skipping duplicates.
- [x] Run the sync test and verify it fails before implementation.
- [x] Implement mapper, image persistence for external image URLs, duplicate checks, and event publication.
- [x] Run the sync test and verify it passes.

### Task 5: Scheduled Runners and Configuration

**Files:**
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalLossApiSyncRunner.java`
- Create: `src/main/java/com/example/dogo/service/animal/api/AnimalProtectionApiSyncRunner.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application.properties`

- [x] Add runners controlled by existing `animal-loss.*` and `animal-protection.*` properties.
- [x] Add missing test property defaults.
- [x] Run compile/tests for the touched package.

### Task 6: Final Verification

**Files:**
- All touched Java/schema/docs files.

- [x] Run focused animal API tests.
- [x] Run `./gradlew.bat test`.
- [x] Review `git diff` for accidental unrelated edits.
