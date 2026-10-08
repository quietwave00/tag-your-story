# ALBUM-MBID-001 — Remove Album MusicBrainz ID

## Status

- State: Complete; user confirmed `test`, `check`, and `scripts/verify.sh` success on 2026-09-22
- Scope approved by user request on 2026-09-22: remove `album.musicbrainz_id` globally, including DDL and related code
- Decision: `ADR-011-album-musicbrainz-id-removal.md`

## Goal

Use Spotify ID for Catalog Album identity and visible ALBUM resolved System Tags for Album tag display. Remove the optional Release Group MBID cache from Album storage while preserving first-import Album genre evidence when MusicBrainz matching succeeds.

## Changes

1. Remove Album MBID entity field/index, imported snapshot field and attach path. Keep Track Recording MBID.
2. Keep MusicBrainz Release Group matching and ALBUM genre observation; retain its ID only within the provider call and observation `external_ref`.
3. Remove Album identity lock/read and update affected tests.
4. Update reference DDL, server spec and System Tag architecture. Production launch has not occurred, so no removal migration runbook is needed. Completed plans remain historical; ADR-011 supersedes their Album MBID policy. Update the active Catalog query plan where it refers to Album MBID attach.

## Acceptance Criteria

- [x] Album entity/schema has no `musicbrainz_id` column or index, and neither import nor identity attach reads/writes it.
- [x] A matched MusicBrainz Release Group can still create ALBUM observation/resolved tags; Track Recording MBID remains idempotent.
- [x] Existing visible ALBUM resolved tags remain the source for Album display; raw assertion/HIDDEN tags are not exposed as visible tags.
- [x] User-run `./gradlew test`, `./gradlew check`, `./scripts/verify.sh` passed; scoped diff review completed. Production launch has not occurred, so no removal migration is needed.

## Verification

User reported successful `./gradlew test`, `./gradlew check`, and `./scripts/verify.sh` after the API module test fixture fix. Codex reviewed references and `git diff --check` statically; Codex did not run Gradle.
