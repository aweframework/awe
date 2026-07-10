# Tasks — Per-user Avatar Image

Strict TDD: every implementation task is RED (failing test) before GREEN (minimal code). Runner is `mvn`.

Targeted commands:
```bash
mvn test -pl awe-framework/awe-controller -am
mvn test -pl awe-tests/awe-boot -am
```

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 550–750 (6 dialect migrations + 6 test schema edits + XML descriptors + Java service/controller + security wiring + tests + locale) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | Slice 1 = migrations (6 dialects) + test schemas (6) + `AweUserSettings` table + `connectedUser` read path + query tests. Slice 2 = upload service + `AvatarController` download endpoint + security wiring + locale + integration tests. |
| Delivery strategy | resolved by orchestrator (`delivery_strategy` cached at session start) |
| Chain strategy | resolved by orchestrator (`chain_strategy` cached at session start) |

Decision needed before apply: Yes — confirm chained-PR slicing (or `size:exception`) before starting Slice 2.

## Scope guards (must remain true)

- [x] No column added to `ope`.
- [x] No `HIS*` mirror added for `AweUserSettings`.
- [x] No change to `AweAvatar` frontend component.
- [x] Avatar download token is never accepted from the client; `/avatar` resolves strictly from the session user.
- [x] Avatar upload never routes through the public `/file/upload` endpoint.

---

## Slice 1 — Schema, migrations, and read path (`connectedUser.image`)

### 1) RED: migration/schema tests for `AweUserSettings`
- [x] Add/extend an `awe-tests/awe-boot` integration test asserting the `AweUserSettings` table exists with `IdeUsrSet` PK, `Ope varchar(20) UNIQUE NOT NULL`, `AvatarImage varchar(4000) NULL`, after schema bootstrap.
- [x] Confirm the test fails (table does not exist yet).

### 2) GREEN: add migration files (6 dialects)
- [x] `awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/h2/AWE_V1.2.2__user_settings.sql` — copy `V1.0.2__favourites.sql` idiom (`IF NOT EXISTS`).
- [x] `.../hsqldb/AWE_V1.2.2__user_settings.sql` — copy that dialect's `V1.0.2__favourites.sql` idiom (plain `CREATE TABLE` if that's the existing style).
- [x] `.../mysql/AWE_V1.2.2__user_settings.sql` — copy `V1.0.2__favourites.sql` idiom (`IF NOT EXISTS`).
- [x] `.../oracle/AWE_V1.2.2__user_settings.sql` — `number(5)` PK, `varchar2`, named `CONSTRAINT pk_AweUserSettings`.
- [x] `.../postgresql/AWE_V1.2.2__user_settings.sql` — `integer` PK, `IF NOT EXISTS`. Do NOT add any placeholder for the missing `V1.2.0`/`V1.2.1` versions.
- [x] `.../sqlserver/AWE_V1.2.2__user_settings.sql` — match that dialect's own `V1.0.2__favourites.sql` existence-guard idiom exactly.
- [x] Every file: `CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)`, no `Act`/date columns, no `HIS*` mirror.

### 3) GREEN: add table to test schema bootstrap files (6 dialects)
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-h2.sql`
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-hsqldb.sql`
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-mysqldb.sql`
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-oracledb.sql`
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-postgresql.sql`
- [x] `awe-tests/awe-boot/src/main/resources/sql/schema-sqlserverdb.sql`
- [x] Add `AweUserSettings` alongside the existing `AweUsrFav` definition in each file, matching that file's own dialect syntax.
- [x] Run `mvn test -pl awe-tests/awe-boot -am` — migration/schema test from step 1 now passes.

### 4) TRIANGULATE: dialect parity check
- [x] Add/extend a test (or assertion in the existing migration test) verifying column types/constraints are structurally equivalent across all six dialects (per spec scenario "Migration applies cleanly across all supported dialects").

### 5) RED: `connectedUser` query test — no avatar row
- [x] Add a failing query-service integration test asserting `connectedUser` returns `image = null` (or empty) when the connected user has no `AweUserSettings` row, while `value`/`name`/`label` remain unchanged.

### 6) RED: `connectedUser` query test — avatar row present
- [x] Extend the test to assert `image = "/avatar"` when the connected user has an `AweUserSettings` row with a non-null `AvatarImage` token.
- [x] Confirm both tests fail (no `image` field exists yet).

### 7) GREEN: extend `connectedUser` in `Queries.xml`
- [x] Edit `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`, query id `connectedUser` (currently line ~1808).
- [x] Add `LEFT JOIN AweUserSettings alias="aus"` on `aus.Ope = ope.l1_nom`.
- [x] Add internal field `<field id="AvatarImage" table="aus" alias="avatarToken" />` (not exposed to the client under this alias).
- [x] Add computed field `image` using the pure-XML `eval="true"` ternary pattern (precedent `awe-tests/awe-boot/.../Queries.xml:1605`), yielding `'/avatar'` when `avatarToken` is not null, else `null`.
- [x] Keep `value`, `name`, `label` fields and `cacheable="true"` unchanged.
- [x] Run the tests from steps 5–6 — both pass.

### 8) GREEN: add `nextIdForUserSettings` and avatar-token read query
- [x] Add `nextIdForUserSettings` query in `Queries.xml` (`COALESCE(MAX(IdeUsrSet),0)+1`), mirroring `nextIdForFavourites`.
- [x] Add a read query (e.g. `getUserSettings` / `getAvatarToken`) keyed by `session="user"` returning the current user's `AvatarImage` token, for use by the upload/download service layer.

### 9) REFACTOR: query XML cleanup
- [x] Verify XML validates against `queries.xsd`; align formatting/ordering with surrounding query blocks; no functional change.

**Slice 1 rollback boundary:** revert the 6 migration files, the 6 test-schema edits, and the `Queries.xml` changes; `connectedUser` reverts to its 3-field shape with no functional impact on any other caller.

---

## Slice 2 — Upload, download endpoint, security, locale

### 10) RED: `upsertUserAvatar` maintain test — insert path
- [x] Add a failing test asserting that when no `AweUserSettings` row exists for the session user, invoking the avatar upsert creates exactly one row with `IdeUsrSet` from `nextIdForUserSettings`, `Ope` from the session user, and the given token.

### 11) RED: `upsertUserAvatar` maintain test — update path
- [x] Add a failing test asserting that when a row already exists for the session user, invoking the upsert updates `AvatarImage` on the existing row rather than inserting a duplicate (spec: "at most one row per user").

### 12) GREEN: add `upsertUserAvatar` target in `Maintain.xml`
- [x] Edit `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Maintain.xml`.
- [x] Add `upsertUserAvatar` target: insert-or-update semantics, `IdeUsrSet` via `nextIdForUserSettings` on insert, `Ope` via `session="user"`, `AvatarImage` via the uploaded-token variable.
- [x] Run tests from steps 10–11 — both pass.

### 13) RED: `UserSettingsService.uploadAvatar` unit tests
- [x] Add failing unit tests for a new `UserSettingsService` (package alongside existing services in `awe-framework/awe-controller/src/main/java/com/almis/awe/service/`):
  - accepts `image/png`, `image/jpeg`, `image/gif` within 2MB and stores a token via `upsertUserAvatar`. (Originally also listed `image/webp`; removed post-merge — see Slice 3 note below — because stock JDK 17 ImageIO cannot decode WebP, so `image/webp` was rejected by content validation on every upload despite being allow-listed.)
  - rejects a disallowed MIME type with a localized error, and does NOT call the upsert.
  - rejects a file exceeding 2MB with a localized error, and does NOT call the upsert.
  - on re-upload, deletes the previously referenced file via `FileService.deleteFile` after the token update succeeds.

### 14) GREEN: implement `UserSettingsService`
- [x] Create `UserSettingsService extends ServiceConfig` with `uploadAvatar(MultipartFile)`:
  - validate MIME allow-list and 2MB max size (reject before calling `FileService.uploadFile`).
  - call `FileService.uploadFile(file, "avatar")` to get a `FileData`.
  - encode token via `FileUtil.fileDataToString` and upsert via the `upsertUserAvatar` maintain target for the session user.
  - delete the old file (previous token, if any) via `FileService.deleteFile` after the update succeeds; log and continue if delete fails.
- [x] Add `getAvatarForCurrentUser()` returning the current user's `FileData` (or empty) by reading the token query from step 8 and decoding via `FileUtil.stringToFileData`.
- [x] Run tests from step 13 — all pass.

### 15) TRIANGULATE: upload edge cases
- [x] Add tests for: empty file, missing MIME/content-type header, and a spec-required check that no `AweUserSettings` row is created/modified on a rejected upload (oversized or disallowed MIME).

### 16) REFACTOR: extract validation helpers
- [x] Extract MIME allow-list check and size-limit check into small private helper methods in `UserSettingsService` for readability/testability; no behavior change.

### 17) RED: `AvatarController` download tests
- [x] Add failing unit tests (Mockito, `@ExtendWith(MockitoExtension.class)`) in `awe-framework/awe-controller` asserting: authenticated user with a stored avatar returns `200`, correct `Content-Type`, and `Cache-Control: no-cache`; no avatar returns `404`; decode failure returns `404`. **Deviation from the literal task wording** (documented, not silently changed): `awe-controller` has no `spring-boot-starter-test`/`spring-security-test`/`@SpringBootTest` context anywhere in the module — every existing controller test there (`FileControllerTest`, `UploadControllerTest`, `TotpControllerTest`) is a plain Mockito unit test with no security filter chain to exercise. The MockMvc + `spring-security-test` + full filter chain assertions (401 unauthenticated, cross-user isolation) were written instead in `awe-tests/awe-boot` (see step 19), which is where `AbstractSpringAppIntegrationTest`/`SecurityIntegrationTest` already live and where the real `AweWebSecurityConfig` filter chain is wired.

### 18) GREEN: implement `AvatarController`
- [x] Create `AvatarController extends ServiceConfig` in `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/` alongside `FileController`.
- [x] `@GetMapping("/avatar")`: resolve session user via `UserSettingsService.getAvatarForCurrentUser()`, stream via `FileService.getFileStream(FileData)` (already sets `inline` disposition), layer `Cache-Control: no-cache` on top; return `404` when absent; never leak a `500` on decode failure (`stringToFileData`/`getFileStream` failures both surface as `AWException`, caught and treated as `404`).
- [x] Run tests from step 17 — all pass.
- [x] Registered `AvatarController` in `awe-spring-boot-starter`'s `AutoConfiguration.imports` (same mechanism as `FileController`/`UploadController`/etc. — controllers are plain `@RestController` classes registered directly as Spring Boot auto-configurations, not component-scanned).

### 19) RED: security matcher test for `/avatar`
- [x] Added `AvatarSecurityIntegrationTest` in `awe-tests/awe-boot` (MockMvc + `spring-security-test`, full filter chain via `AbstractSpringAppIntegrationTest`) asserting `/avatar` is `401` unauthenticated, and correctly resolves (`404` no-avatar / `404` on undecodable-token-referencing-a-missing-file, since a real file was never written to disk in the test) when authenticated. **This test empirically DISPROVES the design's assumption** (see step 20).

### 20) GREEN: wire `/avatar` into security config — DEVIATION FROM PLAN, EMPIRICALLY VERIFIED
- [x] **Did NOT add `/avatar` to `authenticatedRequestMatchers`.** Verified empirically (not assumed) that doing so is actively wrong: `PublicQueryMaintainAuthorization.check()` (`awe-framework/awe-controller/src/main/java/com/almis/awe/security/authorization/PublicQueryMaintainAuthorization.java`) only returns `isAuthenticated() || isPublicX(request)` when the request URI matches `QUERY_PUBLIC_LIST` or `MAINTAIN_PUBLIC_LIST` substrings (e.g. `/action/data`, `/action/maintain`, `/file/stream/maintain`, `/file/download/maintain`). For any OTHER route reaching this manager — including `/avatar`, which is neither a query nor a maintain action — `check()` falls through to `else return new AuthorizationDecision(false)`, i.e. **always denied**, even for a fully authenticated user. Adding `/avatar` to `authenticatedRequestMatchers` was tested directly: the result was a hard `403 Forbidden` (`AweAccessDeniedHandler: Forbidden. User: [test] attempted to access the protected URL: [/avatar]`) for BOTH `test` and `donald` even when correctly authenticated — a regression that would have made the endpoint permanently unusable. This directly contradicts the design doc's Decision 5 / "Important interaction" note, which assumed the manager "collapses to `isAuthenticated()`" for non-query/non-maintain routes — that assumption is **false** for the current `PublicQueryMaintainAuthorization` implementation.
- [x] **No `SecurityEndpoints.java` change was made.** `/avatar` is left ungated by `authenticatedRequestMatchers`/`fileRequestMatchers`, so it falls through to the final `.anyRequest().authenticated()` rule in `AweWebSecurityConfig#configureAuthorization` — a plain `.authenticated()` check with no `PublicQueryMaintainAuthorization` involved. Verified this is exactly the desired behavior: `401` unauthenticated, reachable (and correctly 404/200) once authenticated.
- [x] Ran `AvatarSecurityIntegrationTest` — 4/4 pass with the file unchanged (net diff on `SecurityEndpoints.java` is zero).
- [x] Regression-checked `SecurityIntegrationTest` (existing `/file/*`, `/action/*` matcher tests, `PublicQueryMaintainAuthorization`-gated paths) — 8/8 pass, unchanged.

### 21) TRIANGULATE: cross-user isolation check
- [x] `AvatarSecurityIntegrationTest#secondAuthenticatedUserCannotRetrieveFirstUsersAvatar`: user `test` has an avatar row, user `donald` (authenticated, no avatar row of his own) requests `/avatar` and receives `404`, never `test`'s bytes — confirmed behaviorally (session-bound resolution, no path/token parameter exists to guess or pass another user's identity).

### 22) REFACTOR: controller/service cleanup
- [x] Reviewed `AvatarController` against `FileController` conventions (constructor injection, `@Slf4j`, `ServiceConfig` base). Removed a dead `@ExceptionHandler(AWException.class)` that was copied from `FileController`'s pattern but is unreachable in `AvatarController`, since `getAvatar()` fully catches `AWException` internally and never lets it propagate to the framework's exception-handling layer — leaving it in would have been misleading no-op code. No behavior change (verified 3/3 `AvatarControllerTest` still pass after removal).

### 23) GREEN: locale entries
- [x] Added `ERROR_TITLE_AVATAR_TOO_LARGE`/`ERROR_MESSAGE_AVATAR_TOO_LARGE` and `ERROR_TITLE_INVALID_AVATAR_MIME_TYPE`/`ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE` in `Locale-en-GB.xml`, `Locale-es-ES.xml`, `Locale-fr-FR.xml` (all three shipped locales — there is no `Locale-EN.xml`), in correct alphabetical position among existing `ERROR_TITLE_*`/`ERROR_MESSAGE_*` keys.
- [x] Keys match exactly what `UserSettingsService`'s `validateAvatarMimeType`/`validateAvatarSize` already reference (wired during step 14, confirmed by cross-check, no code change needed here).
- [x] Validated via `mvn compile -pl awe-framework/awe-generic-screens` (XML schema validation at compile phase) — `BUILD SUCCESS`.

### 24) Full regression pass
- [x] Ran `mvn test -pl awe-framework/awe-controller -am` — **538/538 pass** (525 Slice-1 baseline + 10 `UserSettingsServiceTest` + 3 `AvatarControllerTest`).
- [x] Ran `mvn test -pl awe-tests/awe-boot -am` — **413/414 pass, 3 skipped** (only failure: the known pre-existing `ActionControllerTest.testLaunchScreenDataActionError`, a whitespace/line-ending mismatch in a static 404 template, present on `develop` and unrelated to this change — confirmed already documented in Slice 1's apply-progress).
- [x] Confirmed no existing `ope`-backed preference test regressed: `SecurityIntegrationTest` 8/8, `SessionControllerTest` 3/3, `QueryHsqlTest` (includes `connectedUser`/theme/login-adjacent query tests) 199/199 (3 skipped, pre-existing), all green.

**Slice 2 rollback boundary:** revert `Maintain.xml` addition, `UserSettingsService`, `AvatarController`, the `SecurityEndpoints` matcher, and locale entries; Slice 1's read path (`connectedUser.image` always null) remains intact and functional.

---

## Traceability (spec requirement → tasks)

| Spec requirement | Tasks |
|---|---|
| AweUserSettings satellite table stores the avatar token | 1–4 |
| Users can upload an avatar image | 10–16, 23 |
| Secure download endpoint resolves a token to image bytes | 17–22 |
| connectedUser exposes the avatar token as image | 5–9 |
| Existing avatar fallback behavior is preserved | no frontend task (enforced as scope guard; verified by absence of any `AweAvatar` change) |
| ope table and existing preferences remain unaffected | 2–3 (schema-only additive migration), 24 (regression pass) |

## Parallelization notes

- Tasks 1–9 (Slice 1) must complete before 10–23 (Slice 2), since upload/download depend on the table and `nextIdForUserSettings`/read query.
- Within Slice 1: migration tasks (2) for the 6 dialects can be done in parallel by dialect; test-schema tasks (3) can be done in parallel by dialect; both must land before task 4 (parity check) and before task 7 (query GREEN) is verified end-to-end.
- Within Slice 2: `upsertUserAvatar` (10–12) must precede `UserSettingsService` (13–16); `UserSettingsService` must precede `AvatarController` (17–18); security wiring (19–20) can start once the controller route name (`/avatar`) is fixed (after step 18) but is independent of upload-side work (13–16) and could run in parallel with it.
- Locale task (23) is independent and can run in parallel with 17–20 once error keys are decided.

---

## Slice 3 — 4R review fixes (post-MR !665 verification remediation)

Fixes found by the 4R review after Slices 1+2 were committed. Strict TDD applied to each.

### 25) RED/GREEN: wire the avatar upload HTTP endpoint (was a BLOCKER — `uploadAvatar` existed but had no entry point)
- [x] Added `AvatarControllerTest#uploadAvatarDelegatesToUserSettingsServiceAndReturnsOk` (RED: `uploadAvatar` did not exist on the controller).
- [x] Added `@PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)` to `AvatarController`, delegating to `UserSettingsService.uploadAvatar(file)`, returning `200` on success.
- [x] Confirmed the endpoint is authenticated via the same `.anyRequest().authenticated()` fallthrough as `GET /avatar` (not added to `authenticatedRequestMatchers`, see task 27).
- [x] CSRF: investigated `AweWebSecurityConfig#configureCsrf` and the existing `/file/upload` test (`UploadControllerTest`, uses `.with(csrf())`). CSRF is enabled globally via `CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler`; no endpoint-specific CSRF exemption exists for authenticated multipart POSTs. `/avatar` upload follows the exact same convention (tests use `.with(csrf())`), no CSRF disabling introduced.
- [x] Added integration coverage in `AvatarSecurityIntegrationTest`: authenticated POST with a valid small real PNG → success (via the new end-to-end test, task 26); unauthenticated POST → `401` (`unauthenticatedUploadRequestIsRejected`).
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/AvatarController.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/controller/AvatarControllerTest.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### 26) GREEN: real end-to-end 200 test + rename the misleading 404 test (was a BLOCKER)
- [x] Renamed `AvatarSecurityIntegrationTest#authenticatedUserWithAvatarReceivesImageBytes` to `authenticatedUserWithUnresolvableAvatarFileReceives404` (it asserted `404`, not `200` — the old name was misleading).
- [x] Added `authenticatedUserCanUploadAndThenDownloadTheirOwnAvatar`: authenticated user uploads a real, decodable PNG via `POST /avatar`, then `GET /avatar` → asserts `200`, `Content-Type: image/png`, `Cache-Control: no-cache`, and body bytes equal to the uploaded bytes. Runs through the real filter chain (`AbstractSpringAppIntegrationTest`). Cleans up the uploaded file (via `FileService.deleteFile`, since `@Transactional` rollback does not cover filesystem writes) and the DB row in `@AfterEach`.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### 27) RED/GREEN: validate image by content, not just declared MIME + nosniff test (was CRITICAL security)
- [x] Added `UserSettingsServiceTest#uploadAvatarRejectsContentThatDoesNotMatchDeclaredMimeType` (RED: HTML/script bytes declared as `image/png` caused an unhandled `NullPointerException` further down the pipeline instead of a clean rejection — proving content was never validated).
- [x] Implemented `UserSettingsService#validateAvatarContentIsImage`: decodes the uploaded bytes via `javax.imageio.ImageIO` (JDK-provided, no new dependency — confirmed no Tika/ImageIO dependency existed in any pom before this change) and rejects when the bytes do not decode as an image, regardless of the declared `Content-Type`.
- [x] Updated existing tests that previously used all-zero byte arrays (which are not valid image bytes) to use real, `ImageIO`-generated PNG bytes, since content validation now rejects them otherwise.
- [x] Added `AvatarSecurityIntegrationTest#avatarResponseIncludesNosniffHeader` asserting `X-Content-Type-Options: nosniff` on `GET /avatar` — confirmed as a Spring Security default header (no explicit disabling exists in `AweWebSecurityConfig`), not something `AvatarController` needs to set itself.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### 28) RED/GREEN: handle the upsert UNIQUE race gracefully (was CRITICAL correctness)
- [x] Added `UserSettingsServiceTest#upsertUserAvatarFallsBackToUpdateWhenInsertRaceLosesToConcurrentInsert`: stubs `insertUserAvatar` to throw `AWException` (simulating a concurrent UNIQUE(Ope) violation surfacing through `MaintainLauncher`/`SQLMaintainConnector`, which wrap all SQL exceptions as `AWEQueryException`, an `AWException` subtype — verified by reading `SQLMaintainConnector#launchAsSingleOperation`), asserts the call falls back to `updateUserAvatar` and returns cleanly instead of propagating.
- [x] Implemented the fallback in `UserSettingsService#upsertUserAvatar`: catch `AWException` from the insert path and retry as `updateUserAvatar`, since a losing insert means a concurrent request already created the row.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`

### 29) Doc + test: explain the security-matcher decision (doc + test)
- [x] Added a code comment on `AvatarController` explaining why `/avatar` is NOT added to `SecurityEndpoints.authenticatedRequestMatchers` (that group is gated by `PublicQueryMaintainAuthorization`, which hard-denies non-query/non-maintain routes) and why the `.anyRequest().authenticated()` catch-all is the correct, intentional protection.
- [x] Added a design.md addendum (Security wiring impact section) documenting the same, correcting the original "Important interaction" paragraph which assumed (incorrectly) that `PublicQueryMaintainAuthorization` collapses to `isAuthenticated()` for non-query/non-maintain routes.
- [x] Extended `AvatarSecurityIntegrationTest#unauthenticatedRequestIsRejected` (GET) with a new `unauthenticatedUploadRequestIsRejected` (POST) — both assert `401`.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/AvatarController.java`, `openspec/changes/add-user-avatar-image/design.md`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### 30) RED/GREEN: avatar freshness after re-upload (regression test + design note)
- [x] Added `QueryTest#testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert`: primes the `queryData` cache by reading `connectedUser`/`getAvatarToken` for a user with no avatar, upserts a new avatar token via `POST /action/maintain/upsertUserAvatar`, then re-reads `connectedUser`/`getAvatarToken` through the same cached path and asserts the new value is visible — proving `MaintainLauncher`'s global `@CacheEvict(cacheNames = "queryData", allEntries = true)` keeps the cache fresh across avatar changes.
- [x] Added a one-line note to `design.md` Decision 7 documenting that server-side freshness relies on `MaintainLauncher`'s global `@CacheEvict`, not only on the browser-facing `Cache-Control: no-cache`.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/QueryTest.java`, `openspec/changes/add-user-avatar-image/design.md`

**Slice 3 rollback boundary:** revert the `AvatarController.uploadAvatar` endpoint, the `UserSettingsService` content-validation and upsert-fallback changes, and the associated tests; Slice 2's existing (unreachable) `uploadAvatar` service method and `GET /avatar` download path remain intact and functional, but the upload path becomes unreachable again (same state as before Slice 3).

---

## Slice 4 — Remove `image/webp` from the allow-list (post-MR !665 gate review fix)

A gate review found a real regression introduced by Slice 3's `validateAvatarContentIsImage`
(task 27): `image/webp` was in `AVATAR_ALLOWED_MIME_TYPES`, but stock JDK 17 `ImageIO` has no
WebP reader (no TwelveMonkeys/WebP plugin on the classpath), so `ImageIO.read()` always returns
`null` for a real WebP file. Every legitimate `.webp` avatar upload was wrongly rejected at the
content-validation stage, even though WebP was declared "allowed" at the MIME stage — a
confusing, effectively broken allow-list entry. `image/png`, `image/jpeg`, `image/gif` decode
fine with stock ImageIO and are unaffected.

### 31) RED/GREEN: remove `image/webp` from the allow-list
- [x] Added `UserSettingsServiceTest#uploadAvatarRejectsWebpMimeTypeBeforeAttemptingContentValidation`: uses real, well-formed WebP bytes (not garbage bytes, which would be rejected either way for an unrelated reason) and a spied `MockMultipartFile` asserting `getInputStream()` is never called — proving the rejection happens at the MIME allow-list stage, not at content decoding.
- [x] Confirmed RED: before the fix, the test failed because `image/webp` was still allow-listed, so validation reached `validateAvatarContentIsImage` and called `getInputStream()`.
- [x] Removed `"image/webp"` from `AVATAR_ALLOWED_MIME_TYPES` in `UserSettingsService.java`; added a code comment explaining why (no stock-JDK ImageIO WebP reader; TwelveMonkeys or similar would be needed to support it).
- [x] Confirmed GREEN: `mvn test -pl awe-framework/awe-controller -am` — 542/542 pass.
- [x] Updated `ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE` in all three shipped locales (`Locale-en-GB.xml`, `Locale-es-ES.xml`, `Locale-fr-FR.xml`) to drop "WEBP" from the enumerated allowed types.
- [x] Updated `design.md` Decision 6 and the Decisions summary (#6) to state the allow-list is `png|jpeg|gif` and note WebP is intentionally excluded.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`, `awe-framework/awe-generic-screens/src/main/resources/application/awe/locale/Locale-{en-GB,es-ES,fr-FR}.xml`, `openspec/changes/add-user-avatar-image/design.md`

**Slice 4 rollback boundary:** revert `AVATAR_ALLOWED_MIME_TYPES` to include `image/webp`, revert the locale message changes, and remove the new test; no other behavior changes.

---

## Slice 6 — Avatar upload UI (stage-then-claim)

Per design Decision 8 (8.1–8.9): the uploader criterion stages via the public
`/file/upload`; a new authenticated `saveUserAvatar` maintain validates the
staged token and delegates to the EXISTING `upsertUserAvatar`. The direct
multipart `POST /avatar` + `uploadAvatar(MultipartFile)` are retired.

**Scope guards (must remain true):**
- [ ] No migration, no schema change, no `AweUserSettings` DDL touch.
- [ ] No `connectedUser` change; reuses existing `getAvatarToken` query.
- [ ] No `GET /avatar` change.
- [ ] No `SecurityEndpoints`/security-config change.
- [ ] No `user-settings.xml` edit (base or notifier) — only `settings.xml` `profile-panel`.
- [ ] No `awe-react`/frontend change — uploader component already exists.

### 32) RED: `UserSettingsServiceTest` — `saveUserAvatar(String)` staged-token cases
- [ ] Add failing unit tests driving `saveUserAvatar(String avatarToken)` (not `MultipartFile`): accepts an allowed-MIME, in-size, decodable staged `FileData` token and delegates to `upsertUserAvatar`.
- [ ] Add case: disallowed MIME on the staged `FileData` is rejected with the existing localized error and does NOT call `upsertUserAvatar`.
- [ ] Add case: staged file exceeding 2MB is rejected with the existing localized error and does NOT call `upsertUserAvatar`.
- [ ] Add case: staged content that does not decode as an image (`ImageIO.read` returns null) is rejected.
- [ ] Add NEW case: on any rejection above, `FileService.deleteFile` IS called on the staged file (orphan cleanup) — behavior the retired `MultipartFile` path never needed since rejected multipart bytes were never persisted.
- [ ] Add case: `getSession().getUser()` is the only source of the target user — no parameter accepts a username.

### 33) GREEN: implement `UserSettingsService.saveUserAvatar(String)`
- [ ] Add `saveUserAvatar(String avatarToken)` to `UserSettingsService`: `FileUtil.stringToFileData(avatarToken)`, then run the three staged-file validators (MIME allow-list, `ImageIO` magic-byte decode, 2MB size) against the resolved `FileData`, reusing `AVATAR_ALLOWED_MIME_TYPES`/`AVATAR_MAX_FILE_SIZE`/the two existing error-key pairs.
- [ ] On validation failure: `fileService.deleteFile(stagedFileData)` (delete the orphan) THEN throw the localized `AWException`.
- [ ] On success: capture the previous token (`getStoredAvatarToken`), call the EXISTING `upsertUserAvatar(user, avatarToken)`, then `deletePreviousAvatarFile` on the prior token (unchanged overwrite semantics).
- [ ] Run tests from step 32 — all pass.

### 34) REFACTOR: extract staged-`FileData` validators
- [ ] Adapt the three existing `MultipartFile` validators (`validateAvatarMimeType`, `validateAvatarContentIsImage`, `validateAvatarSize`) into `FileData`-based equivalents (read bytes via `fileService.getFileStream`/resolved path for the `ImageIO` check); no behavior change beyond the input type.

### 35) RED: `Maintain.xml`/`Services.xml` wiring test — `saveUserAvatar` target
- [ ] Add a failing integration test (in `awe-tests/awe-boot`, alongside `AvatarSecurityIntegrationTest`/existing maintain tests) asserting the `saveUserAvatar` maintain target exists, requires authentication, resolves `user` from `session="user"`, and reads the token from a variable named `avatarToken`.

### 36) GREEN: wire `saveUserAvatar` maintain + service binding
- [ ] Add to `awe-framework/awe-generic-screens/.../global/Maintain.xml`:
  ```xml
  <target name="saveUserAvatar">
    <serve service="saveUserAvatar">
      <variable id="avatarToken" type="STRING" name="CrtAvatar"/>
    </serve>
  </target>
  ```
- [ ] Add to `awe-framework/awe-generic-screens/.../global/Services.xml`:
  ```xml
  <service id="saveUserAvatar">
    <java classname="com.almis.awe.service.UserSettingsService" method="saveUserAvatar">
      <service-parameter type="STRING" name="avatarToken" />
    </java>
  </service>
  ```
- [ ] Run the test from step 35 — passes.

### 37) RED: screen assertion — uploader + confirm button in `profile-panel`
- [ ] Add/extend a screen-descriptor test (or XML-schema/structure assertion consistent with existing screen tests) asserting `settings.xml`'s `profile-panel` contains a `criteria` with `component="uploader"` id `CrtAvatar` and destination `avatar`, and a button `updateAvatar` wired to `server-action="maintain"` `target-action="saveUserAvatar"`.

### 38) GREEN: add uploader criterion + confirm button to `settings.xml`
- [ ] Edit `awe-framework/awe-generic-screens/src/main/resources/application/awe/screen/users/settings.xml`, `profile-panel`, ABOVE the existing `name`/`email` criteria block:
  ```xml
  <criteria label="PARAMETER_AVATAR" component="uploader" id="CrtAvatar" destination="avatar" icon="image"/>
  <tag type="div" style="text-right">
    <button id="updateAvatar" label="BUTTON_CONFIRM" type="button" style="btn-primary">
      <button-action type="server" server-action="maintain" target-action="saveUserAvatar"/>
    </button>
  </tag>
  ```
- [ ] Run the test from step 37 — passes.
- [ ] Manually verify (read, do not edit) `awe/screen/users/user-settings.xml` and `awe-notifier/.../screen/user-settings.xml` still each `<include target-screen="settings" target-source="profile-panel"/>` with zero diff needed — confirms both windows surface the new uploader.

### 39) DELETE: retire `POST /avatar` + `uploadAvatar(MultipartFile)` + validators
- [ ] Remove `@PostMapping(value = "/avatar", ...)` `uploadAvatar(MultipartFile)` from `AvatarController`; drop now-unused `PostMapping`/`MultipartFile`/`RequestParam`/`ResponseStatus`/`MediaType` imports if no longer referenced. Keep `GET /avatar` (`getAvatar()`) unchanged.
- [ ] Remove `UserSettingsService.uploadAvatar(MultipartFile)` and its three `MultipartFile`-based private validators (`validateAvatarFile`, `validateAvatarMimeType`, `validateAvatarContentIsImage`, `validateAvatarSize` — superseded by step 34's `FileData`-based versions). Keep `getAvatarForCurrentUser`, `getStoredAvatarToken`, `deletePreviousAvatarFile`, `upsertUserAvatar` unchanged.

### 40) Migrate/adjust existing tests to the token/maintain flow
- [ ] Remove `AvatarControllerTest#uploadAvatarDelegatesToUserSettingsServiceAndReturnsOk` (the `POST` handler no longer exists).
- [ ] Remove the multipart `POST /avatar` cases from `AvatarSecurityIntegrationTest` (`unauthenticatedUploadRequestIsRejected`, `authenticatedUserCanUploadAndThenDownloadTheirOwnAvatar`'s upload leg) and any other test using `MockMvcRequestBuilders.multipart("/avatar")`.
- [ ] Migrate the `UserSettingsServiceTest` upload/validation cases (`uploadAvatarAcceptsAllowedMimeType...`, `...RejectsDisallowedMimeType`, `...RejectsWebpMimeType...`, `...RejectsOversizedFile`, `...RejectsContentThatDoesNotMatch...`, `...RejectsEmptyFile`, `...RejectsMissingMimeType`, `...DeletesPreviousFileAfterSuccessfulReupload`, `...ContinuesWhenOldFileDeleteFails`, `rejectedUploadDoesNotCallUpsert`) to drive `saveUserAvatar(String token)` with a staged `FileData` fixture instead of `MockMultipartFile`.
- [ ] Keep the `upsertUserAvatar` race-fallback test and the `getAvatarForCurrentUser`/`GET /avatar` tests unchanged.
- [ ] Add a maintain-path auth assertion for `saveUserAvatar` (e.g. in `AvatarSecurityIntegrationTest` or a maintain-focused security test): unauthenticated claim attempt is rejected; authenticated claim resolves to the session user only.

### 41) GREEN: locale labels
- [ ] Add an uploader label (e.g. `PARAMETER_AVATAR`) to `Locale-en-GB.xml`, `Locale-es-ES.xml`, `Locale-fr-FR.xml` in correct alphabetical position. Reuse existing `BUTTON_CONFIRM` and the four existing avatar error keys unchanged.
- [ ] Validate via `mvn compile -pl awe-framework/awe-generic-screens` — `BUILD SUCCESS`.

### 42) Full regression pass
- [ ] Run `mvn test -pl awe-framework/awe-controller -am` — all pass, no `uploadAvatar(MultipartFile)` references remain.
- [ ] Run `mvn test -pl awe-tests/awe-boot -am` — all pass (excluding the known pre-existing unrelated `ActionControllerTest` failure documented in Slice 2).
- [ ] Grep the repo for `uploadAvatar` and confirm zero remaining references outside this task's own history/design notes.

**Slice 6 rollback boundary:** revert `settings.xml`, `Maintain.xml`'s `saveUserAvatar` target, `Services.xml`'s `saveUserAvatar` binding, `UserSettingsService.saveUserAvatar` + its staged-file validators, and the locale label; re-add the retired `POST /avatar` handler and `uploadAvatar(MultipartFile)` + validators from git history if a revert is needed. Slice 2's `GET /avatar` download and `upsertUserAvatar` remain intact and functional throughout.

---

## Slice 6 traceability (spec requirement → tasks)

| Spec requirement | Tasks |
|---|---|
| Users can upload an avatar image (stage-then-claim) | 32–34, 36, 40 |
| A staged file rejected at claim time leaves no orphan and no data change | 32, 33 |
| A claim binds only to the claiming user's own row | 32, 33, 36, 40 |
| Avatar upload UI is available in the profile tab of user-settings windows | 37–38 |

## Slice 6 Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 180–260 (1 screen XML block, 1 maintain target, 1 service binding, 1 new service method + refactored validators, ~10 migrated/adjusted tests, 3 locale files) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR — rides the existing MR !665 as further commits |
| Delivery strategy | size:exception already in effect for this MR (established in Slices 1–4) |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low
