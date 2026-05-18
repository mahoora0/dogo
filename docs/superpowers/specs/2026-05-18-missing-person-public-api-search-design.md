# Missing Person Public API Search Integration Design

## Context

The missing person board currently stores and searches only user-created reports in `MISSING_PERSON_REPORT`. The lost and found item domains already use a single-table source model with `SOURCE_TYPE` to mix user posts and police/public-data posts in the same list, search, sort, and detail flows.

The missing person board should follow the same pattern so users can search one board and see both site-uploaded reports and future public Missing Alert OpenAPI records.

## Goals

- Store user-created missing person reports and public API-based reports in `MISSING_PERSON_REPORT`.
- Keep the current board search, sort, pagination, and detail flow working against one table.
- Add enough source metadata to distinguish user posts from synchronized public API posts.
- Prevent duplicate public API imports.
- Keep future API field changes isolated to the public API client, parser, and mapper layer.

## Non-Goals

- Implement the actual Missing Alert OpenAPI client before the official request and response schema is available.
- Add real-time API calls to the search page.
- Build semantic or image similarity matching for missing persons in this step.
- Redesign the missing person UI beyond source labeling or optional source filtering.

## Recommended Approach

Use a single-table integration model:

- User posts continue to be written directly to `MISSING_PERSON_REPORT`.
- Public API records are synchronized into the same table.
- `SOURCE_TYPE` marks whether a row came from a user or a public API source.
- `EXTERNAL_ID` identifies the original public record and is unique for public API rows.
- Search continues to query `MISSING_PERSON_REPORT` through `MissingPersonRepository`.

This matches the existing lost and found item architecture and keeps pagination, sorting, detail routing, and repository usage simple.

## Data Model

Add these columns to `MISSING_PERSON_REPORT`:

- `SOURCE_TYPE VARCHAR(30) NOT NULL DEFAULT 'USER'`
- `EXTERNAL_ID VARCHAR(100)`
- `API_PROVIDER VARCHAR(50)`
- `RAW_PAYLOAD TEXT`
- `SYNCED_AT DATETIME`

Recommended constraints and indexes:

- `CHECK (SOURCE_TYPE IN ('USER', 'PUBLIC_API'))`
- User-created rows require `USER_NO IS NOT NULL` and `EXTERNAL_ID IS NULL`.
- Public API rows require `EXTERNAL_ID IS NOT NULL`.
- Unique index on `(API_PROVIDER, EXTERNAL_ID)` to prevent duplicate imports.
- Search/list index should include `SOURCE_TYPE`, `STATUS`, `OCCURRED_AT`, and `REPORT_ID`.

The entity should expose source metadata but keep creation paths explicit:

- User report factory or constructor sets `SOURCE_TYPE` to `USER`.
- Public API factory or mapper sets `SOURCE_TYPE` to `PUBLIC_API`, `API_PROVIDER`, `EXTERNAL_ID`, `RAW_PAYLOAD`, and `SYNCED_AT`.

## Search Behavior

The default `/missing-persons` search returns both source types.

Existing filters remain:

- `keyword`
- `status`
- `sortBy`
- `sortDir`
- `page`
- `size`

An optional `sourceType` filter can be added without changing the core repository model:

- empty: all reports
- `USER`: site-uploaded reports
- `PUBLIC_API`: synchronized public API reports

Keyword search should continue to search normalized text fields such as nationality, occurred place, body type, face shape, hair color, hair style, and clothing. If the public API provides person name, gender, case number, or guardian/contact-facing fields later, those fields should be added deliberately to the entity and search specification after confirming privacy and display requirements.

## Synchronization Boundary

When the official OpenAPI schema is available, add a dedicated missing-person public API package:

- `service.missing.client`: HTTP client for the Missing Alert OpenAPI.
- `service.missing.parser`: JSON or XML response parsing.
- `service.missing.mapper`: converts API records into `MissingPersonReport`.
- `service.missing.sync`: upsert orchestration and sync result reporting.

The controller and search service should not call the public API directly. They should only read persisted reports. This avoids API latency, rate limits, or outages affecting normal board search.

## Error Handling

- Search ignores public API availability because it reads local data only.
- Sync should be idempotent and skip or update rows based on `(API_PROVIDER, EXTERNAL_ID)`.
- Invalid or incomplete API records should be logged and skipped instead of breaking the full sync.
- If a public API record disappears from the source, the first implementation should leave the local row untouched unless the API provides a clear deletion or cancellation signal.

## UI Behavior

List cards and detail pages should display a compact source label:

- User post: `사용자 제보`
- Public API post: `공공데이터`

If `sourceType` filtering is added, place it near the existing status filter as a small segmented/radio control:

- `전체`
- `사용자 제보`
- `공공데이터`

The UI should not expose raw API payloads or internal external IDs to normal users.

## Testing

Unit and controller tests should cover:

- User-created reports are saved with source type `USER`.
- Public API records can be represented with source type `PUBLIC_API`.
- Duplicate public API rows are blocked or upserted by `(API_PROVIDER, EXTERNAL_ID)`.
- Default search returns both user and public API rows.
- `sourceType` filter returns only the requested source type if implemented.
- List/detail views expose source labels without exposing raw payloads.

Schema-level tests or repository integration tests should verify:

- User rows can be inserted with `USER_NO` and no `EXTERNAL_ID`.
- Public API rows can be inserted with `EXTERNAL_ID` and no user.
- Duplicate public API keys are rejected.

## Implementation Sequence

1. Add source metadata fields, constraints, and indexes to `schema.sql`.
2. Update `MissingPersonReport` with source fields and explicit construction paths.
3. Add source metadata to missing-person DTO views.
4. Extend `MissingPersonService.search` with optional `sourceType` filtering.
5. Update the list and detail templates to show source labels.
6. Add focused service, controller, and repository/schema tests.
7. Add public API sync packages later when the official API schema is available.
