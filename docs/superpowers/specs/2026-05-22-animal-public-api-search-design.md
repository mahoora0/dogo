# Animal Public API Search Integration Design

## Goal

The animal report board should search user-created reports together with public animal data from the animal loss API and the rescued animal API.

## Approach

Public API data is synchronized into `ANIMAL_REPORT` instead of being fetched during search. The existing `/animal-reports` list, filters, pagination, details, and match logic can keep reading one local table. This avoids external API latency and outages during normal user searches.

## Data Model

Add source metadata to `ANIMAL_REPORT`:

- `SOURCE_TYPE`: `USER`, `ANIMAL_LOSS_API`, or `ANIMAL_PROTECTION_API`
- `API_PROVIDER`
- `EXTERNAL_ID`
- `RAW_PAYLOAD`
- `SYNCED_AT`

User-created reports keep `SOURCE_TYPE = USER`. Public API rows use nullable `USER_NO` and require `EXTERNAL_ID`.

## Mapping

The animal loss API maps to `REPORT_TYPE = MISSING`.

The rescued animal API maps to `REPORT_TYPE = SIGHTING`, with `SIGHTING_CARE_STATUS = PROTECTING` while protection data is active.

Common API fields such as breed, kind, color, sex, neuter state, happened date, place, image URL, and care/contact fields are mapped into existing report fields. Unknown or changed fields are preserved in `RAW_PAYLOAD` and skipped only when no stable external id exists.

## Sync

Two scheduled sync services run independently:

- `animal-loss`: imports recent missing animal notices.
- `animal-protection`: imports recent rescued/protected animals.

Both services are disabled by default unless their `*.sync.enabled` properties are enabled. Backfill-on-startup imports only when no rows for that source type exist.

## Search

The existing `AnimalReportService.search(...)` keeps searching persisted rows. Default searches include both user and public API rows. A source filter may be added later, but is not required for this feature.

## Error Handling

Missing service keys fail only the sync job, not the application. Malformed records are logged and skipped. Duplicate records are skipped by `(API_PROVIDER, EXTERNAL_ID)`.

## Testing

Tests cover XML parsing, entity source metadata, sync duplicate skipping, API image persistence, and existing search returning public API rows.
