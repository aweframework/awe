# Apply Progress — Per-user Avatar Image (Slice 1 + Slice 2 + Slice 3 + Slice 4 + Slice 5, complete)

Strict TDD (RED -> GREEN -> REFACTOR) followed for every implementation task. Runner: `mvn`.
All 31 tasks (Slice 1: 1-9, Slice 2: 10-24, Slice 3: 25-30, Slice 4: 31) plus the Slice 5 CI
Oracle fix below are complete.

Slice 3 is a post-MR (!665) remediation of 6 findings from the verified 4R review: 2 BLOCKER
(upload endpoint unwired, misleading 404 test), 2 CRITICAL (client-declared-MIME-only content
validation, TOCTOU upsert race), plus a security-matcher rationale doc/test and a cache-freshness
regression test. See "Slice 3 — 4R review fixes" below.

Slice 5 fixes a CI-only Oracle failure in `connectedUser.image` found on MR !665's Oracle job
after Slices 1-4 were committed: the original AWE-computed (`ComputedColumnProcessor`) mechanism
for `image` worked on h2/hsqldb but returned `null` on Oracle even when an avatar token was
present. Fixed by moving the null-check to a SQL-level `<case>`/`<when condition="is not null">`,
which the database evaluates directly. See "Slice 5" below; this supersedes the `image` mechanism
described in Slice 1, task 7.

## Slice 1 — Schema, migrations, and read path (`connectedUser.image`)

### 1) RED: migration/schema tests for `AweUserSettings` — DONE
- [x] Added `UserSettingsSchemaTest` in `awe-tests/awe-boot` (package `com.almis.awe.test.integration.database`), asserting via `DatabaseMetaData` that `AweUserSettings` exists with `IdeUsrSet`, `Ope`, `AvatarImage` columns, `Ope` covered by a unique index, and `ope` gains no avatar column.
- [x] Confirmed RED: `mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsSchemaTest` — 2 of 3 failed (table/unique constraint missing) before the migration/schema changes.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/UserSettingsSchemaTest.java`

### 2) GREEN: migration files (6 dialects) — DONE
- [x] `h2/AWE_V1.2.2__user_settings.sql` — `IF NOT EXISTS`, matches `V1.0.2__favourites.sql` idiom.
- [x] `hsqldb/AWE_V1.2.2__user_settings.sql` — `IF NOT EXISTS` (confirmed hsqldb's own `V1.0.2__favourites.sql` also uses `IF NOT EXISTS`; corrected the design doc's speculative "plain CREATE TABLE" assumption after reading the actual file).
- [x] `mysql/AWE_V1.2.2__user_settings.sql` — `IF NOT EXISTS`.
- [x] `oracle/AWE_V1.2.2__user_settings.sql` — `number(5)` PK, `varchar2`, named `CONSTRAINT pk_AweUserSettings`.
- [x] `postgresql/AWE_V1.2.2__user_settings.sql` — `integer`-equivalent (`int`), `IF NOT EXISTS`; no placeholder for missing `V1.2.0`/`V1.2.1` (per-vendor isolated Flyway history confirmed valid).
- [x] `sqlserver/AWE_V1.2.2__user_settings.sql` — existence-guard idiom matching that dialect's own `V1.0.2__favourites.sql`.
- [x] Every file has `CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)`, no `Act`/date columns, no `HIS*` mirror.
- Files (new): `awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/{h2,hsqldb,mysql,oracle,postgresql,sqlserver}/AWE_V1.2.2__user_settings.sql`

### 3) GREEN: test schema bootstrap files (6 dialects) — DONE
- [x] Added `AweUserSettings` next to `AweUsrFav` in all six `awe-tests/awe-boot/src/main/resources/sql/schema-*.sql` files, matching each file's own dialect syntax.
- [x] Ran `mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsSchemaTest` — GREEN, all 3 tests pass.
- Files (modified): `awe-tests/awe-boot/src/main/resources/sql/schema-{h2,hsqldb,mysqldb,oracledb,postgresql,sqlserverdb}.sql`

### 4) TRIANGULATE: dialect parity check — DONE
- [x] Added `UserSettingsMigrationDialectParityTest` (parameterized over the six dialects) asserting each migration file defines the same columns/constraint, no `HIS*` mirror, and no `ALTER TABLE ope`.
- [x] Ran `mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsMigrationDialectParityTest` — 8/8 pass.
- Note: this is a structural (text-based) parity check across the six SQL files, not a live multi-engine DB run (only h2/hsqldb are actually executable in this environment). Real mysql/oracle/postgresql/sqlserver execution is left to CI's dedicated dialect profiles (`QueryMySQLTest`, `QueryOracleTest`, `QueryPostgresqlTest`, `QuerySQLServerTest` equivalents), which were not run here.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/UserSettingsMigrationDialectParityTest.java`

### 5-6) RED: `connectedUser` query tests (no avatar / avatar present) — DONE
- [x] Added `testConnectedUserWithoutAvatarReturnsNullImage` and `testConnectedUserWithAvatarReturnsAvatarImagePath` to `QueryTest.java` (run via `QueryHsqlTest` subclass), using `@WithMockUser(username="test")` + `setParameter("user","test")` against the seeded `ope` row (`l1_nom='test'`).
- [x] Confirmed RED: both failed (no `image` field in query result) before task 7.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/QueryTest.java`

### 7) GREEN: extend `connectedUser` in `Queries.xml` — DONE (SUPERSEDED by Slice 5, see below)
- [x] Added `LEFT JOIN AweUserSettings alias="aus"` on `aus.Ope = ope.l1_nom`.
- [x] Added internal field `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true" />` (excluded from client-facing JSON via `noprint`, precedent: `parentValue` field in `ProModTrePro`).
- [x] Added computed `image` via pure-XML `eval="true"` ternary: `'[avatarToken]' !== 'null' ? '/avatar' : null`, with `nullValue="null"` on the computed element (precedent: `awe-tests/awe-boot/.../Queries.xml:1633`, `'[parentValue]' === 'null' ? ...`). This was necessary because a null DB field renders as an empty string during expression substitution unless `nullValue` forces a literal `"null"` marker — the first attempt without `nullValue` incorrectly returned `/avatar` even when no row existed.
- [x] Kept `value`/`name`/`label`/`cacheable="true"` unchanged.
- [x] Ran tasks 5-6 tests — GREEN, both pass on h2/hsqldb; `avatarToken` confirmed absent from client-facing row JSON.
- **SUPERSEDED (Slice 5):** this computed-ternary mechanism passed on h2/hsqldb but failed on Oracle in CI (`QueryOracleTest`) — `image` evaluated to `null` even when a token was present. Replaced with a SQL-level `<case>`/`<when condition="is not null">` that the database evaluates directly, removing `ComputedColumnProcessor` (and the `avatarToken` field entirely) from this path. See "Slice 5 — Fix Oracle-only CI failure" below for the root-cause investigation and the final XML.
- Files: `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`

### 8) GREEN: `nextIdForUserSettings` + avatar-token read query — DONE
- [x] Added `nextIdForUserSettings` (`COALESCE(MAX(IdeUsrSet),0)+1` via `<operation alias="value" operator="ADD">` wrapping `<operation operator="COALESCE">`), mirroring `nextIdForFavourites`. Added explicit `alias="value"` on the outer `<operation>` (the `nextIdForFavourites` precedent omits it, which was found to leave the raw SQL expression as the JSON key — an alias is needed for a clean/predictable API surface).
- [x] Added supporting `getAllUserSettings` sub-query (mirrors `getAllFavourites`).
- [x] Added `getAvatarToken` query (`cacheable="true"`, keyed by `session="user"`) returning `avatarToken` for the connected user — for use by the upload/download service layer in Slice 2.
- [x] Added and ran smoke tests: `testNextIdForUserSettingsWhenTableEmpty`, `testNextIdForUserSettingsWhenRowsExist`, `testGetAvatarTokenReturnsStoredToken` — all pass.
- Files: `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/QueryTest.java`

### 9) REFACTOR: query XML cleanup — DONE
- [x] Validated `Queries.xml` against `queries.xsd` via `mvn compile -pl awe-framework/awe-generic-screens` (binds `xml-maven-plugin:validate` at the `compile` phase) — `BUILD SUCCESS`.
- [x] Formatting aligned with each block's immediate neighbors (the `connectedUser` block matches its surrounding self-closing-tag-with-space style; the new "User settings" block matches the favourites block's no-space style directly above it).
- [x] No functional change beyond the additive avatar fields.

## Scope guards verified

- [x] No column added to `ope` (verified by `testOpeTableUnchanged` in `UserSettingsSchemaTest` and by the `no ALTER TABLE ope` assertion in `UserSettingsMigrationDialectParityTest`).
- [x] No `HIS*` mirror added for `AweUserSettings` (verified by `testNoHistoricMirrorAdded`).
- [x] No frontend change (no files touched under `awe-client-angular` or the `awe-react` client).

## Test results (actual, evidence-based)

Targeted commands run:
```
mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsSchemaTest                    -> 3/3 pass
mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsMigrationDialectParityTest    -> 8/8 pass
mvn test -pl awe-tests/awe-boot -Dtest="QueryHsqlTest#testConnectedUserWithoutAvatarReturnsNullImage+testConnectedUserWithAvatarReturnsAvatarImagePath+testNextIdForUserSettingsWhenTableEmpty+testNextIdForUserSettingsWhenRowsExist+testGetAvatarTokenReturnsStoredToken" -> 5/5 pass
mvn test -pl awe-tests/awe-boot (full module)                                    -> 407/408 pass (1 pre-existing unrelated failure)
mvn test -pl awe-framework/awe-controller (full module)                         -> 525/525 pass
```

Pre-existing unrelated failure (NOT caused by this change, reproduced in isolation and confirmed present in this checkout regardless of Slice 1 changes):
`ActionControllerTest.testLaunchScreenDataActionError` — a whitespace/line-ending mismatch in a static 404 error-page template comparison. Confirmed via isolated re-run (`-Dtest=ActionControllerTest`) that it fails identically; the test file's git history shows no relation to avatar/user-settings/migration work. Not fixed as it is out of Slice 1 scope.

## Environment notes for future apply/verify runs

- `awe-tests/awe-boot` depends on `awe-generic-screens` (unpacked jar) and other reactor modules via the local `.m2` repository, not live reactor state, when running `mvn test -pl awe-tests/awe-boot` alone. After editing `Queries.xml` (or any `awe-generic-screens`/other dependency source), run `mvn install -pl awe-framework/awe-generic-screens -DskipTests` (or the relevant module) BEFORE re-running `awe-boot` tests, and clear `awe-tests/awe-boot/target/schemas`, `target/classes/application`, so the freshly built jar is re-unpacked (the `maven-dependency-plugin:unpack` step is `already unpacked`-cached otherwise).
- The default `awe-tests/awe-boot` test profile boots from `spring.sql.init.schema-locations=classpath:sql/schema-hsqldb.sql` against a **file-based** HSQLDB at `target/tests/db/awe-test` (`spring.flyway.enabled=false`). This file persists across test runs; if schema files are edited, delete `awe-tests/awe-boot/target/tests` to force a fresh bootstrap.
- Surefire in `awe-tests/awe-boot` filters by JUnit 5 tag (`<groups>${test.tags}</groups>`, default `integration`). New test classes intended to run under `mvn test -pl awe-tests/awe-boot` MUST carry `@Tag("integration")` at the class level, or they will silently report `Tests run: 0`.
- `DatabaseMetaData.getColumns()`/`getIndexInfo()` table-name patterns are case-sensitive against the identifier case actually stored by the dialect (HSQLDB folds unquoted identifiers to upper-case); use `metaData.storesLowerCaseIdentifiers()` to pick the right case rather than passing the mixed-case AWE table name literally.
- AWE's `ComputedColumnProcessor` substitutes a null DB field as an **empty string**, not the word `null`, when computing an `eval="true"` expression — unless the `<computed>` element sets `nullValue="null"` (or another sentinel), a plain `!= null`/`!== 'null'` ternary against a null-backed field will NOT correctly detect the null case. **This computed-ternary approach was later replaced (Slice 5) after it failed on Oracle in CI** — even with `nullValue="null"` set correctly, the computed still evaluated wrong on Oracle only, isolated to `ComputedColumnProcessor`'s row-lookup for this specific field/dialect combination. The takeaway for any future nullable-join field exposed as a derived value: prefer a SQL-level `<case>`/`<when condition="is not null">` over an AWE computed/`eval="true"` ternary when the field crosses a `LEFT JOIN` boundary — the SQL-level construct is evaluated by the database and has been verified dialect-portable (h2 + hsqldb locally; Oracle via CI), whereas the computed-ternary path has a confirmed, not-fully-root-caused Oracle-specific failure mode.

## Slice 2 — Upload, download endpoint, security, locale

### 10-12) RED/GREEN: `upsertUserAvatar` maintain target — DONE
- [x] Added `UserSettingsMaintainTest` in `awe-tests/awe-boot` (`@Transactional`, `@WithMockUser(username="test")`) asserting insert-when-absent and update-when-present via the `/action/maintain/upsertUserAvatar` HTTP endpoint (see "gotcha" below for why direct service calls don't work here).
- [x] Confirmed RED: both tests failed with `AWException: Maintain operation -upsertUserAvatar- has not been defined`.
- [x] Added `insertUserAvatar` (plain SQL insert, `IdeUsrSet` via `query="nextIdForUserSettings"`) and `updateUserAvatar` (plain SQL update keyed by `Ope`) targets in `Maintain.xml`, plus `upsertUserAvatar` as a `<serve service="upsertUserAvatar">` target (mirrors `clickFavourite`'s `<serve>` pattern) delegating to a new `UserSettingsService.upsertUserAvatar(user, avatarToken)`, which checks existence via the `getAvatarToken` query and calls `insertUserAvatar`/`updateUserAvatar` accordingly through `MaintainService.launchPrivateMaintain`.
- [x] Registered `upsertUserAvatar` in `Services.xml` and `UserSettingsService` as a new `@Bean` in `AweAutoConfiguration` (mirrors `favouriteService`).
- [x] Ran tests — GREEN, 2/2 pass.
- Files: `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/{Maintain,Services}.xml`, `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java` (new), `awe-framework/awe-starters/awe-spring-boot-starter/src/main/java/com/almis/awe/autoconfigure/AweAutoConfiguration.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/UserSettingsMaintainTest.java` (new)

### 13-16) RED/GREEN/TRIANGULATE/REFACTOR: `UserSettingsService.uploadAvatar` — DONE
- [x] Added `UserSettingsServiceTest` (Mockito unit test, `awe-controller`) with 10 tests: accepts allowed MIME within 2MB; rejects disallowed MIME, oversized, empty file, and missing MIME (all via `AWException`, all asserting `verifyNoInteractions(maintainService)`/`verifyNoInteractions(fileService)` on rejection); deletes the previous file after a successful re-upload; continues (does not propagate) when the old-file delete fails; `getAvatarForCurrentUser` returns empty/decoded `FileData` correctly.
- [x] Confirmed RED: compile failure (`uploadAvatar`/`getAvatarForCurrentUser` did not exist).
- [x] Implemented `uploadAvatar(MultipartFile)`: validates MIME allow-list (`image/png|jpeg|webp|gif`, resolved server-side via `FileUtil.extractContentType`, never trusting the client filename) and 2MB size ceiling BEFORE calling `FileService.uploadFile`; encodes the token via `FileUtil.fileDataToString`; upserts via the same-class `upsertUserAvatar`; deletes the old file (if any) via `FileService.deleteFile` after the update succeeds, logging and continuing on delete failure.
- [x] Implemented `getAvatarForCurrentUser()`: reads the stored token via the `getAvatarToken` query for the session user, decodes via `FileUtil.stringToFileData`, returns `Optional.empty()` when absent.
- [x] Extracted `validateAvatarFile`/`validateAvatarMimeType`/`validateAvatarSize`/`deletePreviousAvatarFile` as private helpers during GREEN (no separate refactor commit needed — the design was already factored this way from the first implementation).
- [x] Ran tests — GREEN, 10/10 pass.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java` (new)

### 17-18, 21-22) RED/GREEN/TRIANGULATE/REFACTOR: `AvatarController` — DONE (with a documented test-location deviation)
- [x] **Deviation from literal task wording (documented, not silent):** the tasks specified "MockMvc + spring-security-test tests in awe-framework/awe-controller". Verified `awe-controller` has NO `spring-boot-starter-test`/`spring-security-test`/`@SpringBootTest` context anywhere in the module — every existing controller test (`FileControllerTest`, `UploadControllerTest`, `TotpControllerTest`) is a pure Mockito `@ExtendWith(MockitoExtension.class)` unit test with `@InjectMocks`, no security filter chain. Wrote `AvatarControllerTest` (`awe-controller`) as a Mockito unit test asserting response status/headers/body for the controller's own logic (200 + no-cache header, 404 no-avatar, 404 decode-failure). Wrote the full-filter-chain assertions (401 unauthenticated, cross-user isolation) as `AvatarSecurityIntegrationTest` in `awe-tests/awe-boot`, which already hosts `AbstractSpringAppIntegrationTest`/`SecurityIntegrationTest` with the real `AweWebSecurityConfig` wired via `@SpringBootTest` + `springSecurity()`.
- [x] Confirmed RED: `AvatarController` did not exist (compile failure).
- [x] Implemented `AvatarController extends ServiceConfig` (`awe-framework/awe-controller/.../controller/`), `@GetMapping("/avatar")`: resolves the avatar via `UserSettingsService.getAvatarForCurrentUser()`, streams via `FileService.getFileStream(FileData)` (already sets `inline` disposition + content type), layers `Cache-Control: no-cache` on top of the returned `ResponseEntity`; `404` when absent; catches `AWException` (covers both `stringToFileData` decode failures and `getFileStream` file-not-found, since `FileService.getFileStream` wraps all internal exceptions as `AWException`) and returns `404` — never a `500`.
- [x] Registered `AvatarController` in `awe-spring-boot-starter`'s `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (same mechanism as `FileController`/`UploadController` — plain `@RestController` classes registered as Spring Boot auto-configurations, not component-scanned).
- [x] REFACTOR: removed a dead `@ExceptionHandler(AWException.class)` copied from `FileController`'s pattern — unreachable in `AvatarController` since `getAvatar()` fully catches `AWException` internally.
- [x] Ran tests — GREEN, `AvatarControllerTest` 3/3 pass.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/AvatarController.java` (new), `awe-framework/awe-controller/src/test/java/com/almis/awe/controller/AvatarControllerTest.java` (new), `awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

### 19-20) RED/GREEN: security matcher — DONE, WITH A CRITICAL EMPIRICAL FINDING THAT OVERRIDES THE DESIGN
- [x] Added `AvatarSecurityIntegrationTest` in `awe-tests/awe-boot` (full `AweWebSecurityConfig` filter chain via `AbstractSpringAppIntegrationTest` + `springSecurity()`): unauthenticated `/avatar` -> `401`; authenticated no-avatar -> `404`; authenticated with-avatar-but-file-missing-on-disk -> `404` (never `500`); cross-user isolation (task 21).
- [x] **Empirically verified the design doc's Decision 5 / "Important interaction" note is WRONG, per the task's own instruction to "verify empirically, do not assume."** `PublicQueryMaintainAuthorization.check()` (`awe-framework/awe-controller/.../security/authorization/PublicQueryMaintainAuthorization.java`) does NOT collapse to `isAuthenticated()` for a route that is neither a query nor a maintain action. Reading the actual code: `check()` tests `isQueryAction(request)` (URI contains one of `QUERY_PUBLIC_LIST`) and `isMaintainAction(request)` (URI contains one of `MAINTAIN_PUBLIC_LIST`); for `/avatar`, BOTH are false, so it falls to `else return new AuthorizationDecision(false)` — always denied, regardless of authentication.
- [x] **Directly tested the design's literal instruction** (add `/avatar` to `authenticatedRequestMatchers`) and confirmed it breaks the feature: authenticated requests from both `test` and `donald` received `403 Forbidden` (`AweAccessDeniedHandler: Forbidden. User: [x] attempted to access the protected URL: [/avatar]`), not `200`/`404` as intended. This would have made the endpoint permanently unusable for every user, including the avatar's owner.
- [x] **Correct fix, verified: do nothing to `SecurityEndpoints.java`.** `/avatar` is not matched by `fileRequestMatchers` (public) nor `authenticatedRequestMatchers` (custom `PublicQueryMaintainAuthorization`-gated), so it falls through to the final `.anyRequest().authenticated()` rule in `AweWebSecurityConfig#configureAuthorization` — a plain, unconditional `.authenticated()` check. This is exactly the desired self-only, authenticated-only semantics, reached via a different (and correct) path than the design assumed.
- [x] Net diff on `SecurityEndpoints.java`: **zero** (edited then reverted after the empirical test above).
- [x] Ran `AvatarSecurityIntegrationTest` — GREEN, 4/4 pass, with `SecurityEndpoints.java` unchanged.
- [x] Regression-checked `SecurityIntegrationTest` (existing `/file/*`, `/action/*`, `PublicQueryMaintainAuthorization`-gated matcher tests) — GREEN, 8/8 pass, unchanged.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java` (new). No production file changed for this step.

### 23) GREEN: locale entries — DONE
- [x] Added `ERROR_TITLE_AVATAR_TOO_LARGE`/`ERROR_MESSAGE_AVATAR_TOO_LARGE` and `ERROR_TITLE_INVALID_AVATAR_MIME_TYPE`/`ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE` to `Locale-en-GB.xml`, `Locale-es-ES.xml`, `Locale-fr-FR.xml` (the three shipped locales — confirmed there is no `Locale-EN.xml`), in correct alphabetical position among the existing `ERROR_TITLE_*`/`ERROR_MESSAGE_*` blocks.
- [x] Keys match exactly what `UserSettingsService` already references (wired during step 14 GREEN — no code change needed here, only adding the locale entries themselves).
- [x] Validated via `mvn compile -pl awe-framework/awe-generic-screens` (XML schema validation runs at compile phase via `xml-maven-plugin:validate`) — `BUILD SUCCESS`.
- Files: `awe-framework/awe-generic-screens/src/main/resources/application/awe/locale/Locale-{en-GB,es-ES,fr-FR}.xml`

## Scope guards verified (Slice 2)

- [x] No column added to `ope` (no `ope`/`AweOpe` file touched in Slice 2).
- [x] No `HIS*` mirror added.
- [x] No `AweAvatar`/frontend file touched.
- [x] `/avatar` never accepts a client-supplied token; resolution is strictly `getSession().getUser()` -> `getAvatarToken` query -> `FileData`. Verified behaviorally by `secondAuthenticatedUserCannotRetrieveFirstUsersAvatar`.
- [x] Avatar upload routes through `UserSettingsService.uploadAvatar` -> `FileService.uploadFile(file, "avatar")`, never through `/file/upload` (which remains untouched and still `permitAll`).

## Test results (Slice 2, actual, evidence-based)

Targeted commands run:
```
mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsMaintainTest                        -> 2/2 pass
mvn test -pl awe-framework/awe-controller -Dtest=UserSettingsServiceTest               -> 10/10 pass
mvn test -pl awe-framework/awe-controller -Dtest=AvatarControllerTest                  -> 3/3 pass
mvn test -pl awe-tests/awe-boot -Dtest=AvatarSecurityIntegrationTest                   -> 4/4 pass
mvn test -pl awe-tests/awe-boot -Dtest=SecurityIntegrationTest,AvatarSecurityIntegrationTest -> 8/8 + 4/4 pass (no regression)
mvn test -pl awe-framework/awe-controller -am (full module)                            -> 538/538 pass
mvn test -pl awe-tests/awe-boot -am (full module)                                      -> 413/414 pass, 3 skipped (only failure: pre-existing ActionControllerTest.testLaunchScreenDataActionError)
```

Pre-existing unrelated failure (same one documented in Slice 1, confirmed still present and still unrelated):
`ActionControllerTest.testLaunchScreenDataActionError` — whitespace/line-ending mismatch in a static 404 error-page template comparison, present on `develop` regardless of this change. Not fixed, not attributed to Slice 2.

## Gotchas / non-obvious discoveries (Slice 2)

1. **`session="user"` variable resolution requires the AWE login/session-population flow, not just `@WithMockUser`.** `QueryUtil.getParameter` resolves a `session="X"` variable via `getSession().getParameter(X)`, which reads from AWE's own session storage (`AweSessionStorage`), populated only by `AweSessionDetails.onLoginSuccess()` on a REAL AWE login (`sessionService.setSessionParameter(SESSION_USER, ...)`). Spring Security's `@WithMockUser` populates the `SecurityContext`/principal but does NOT run AWE's login flow, so `session="user"` resolves to an empty string when a maintain/query is invoked directly against a mocked-auth `MockMvc` request without first calling `/session/set/user` (a test-only endpoint in `awe-tests/awe-boot`, `@Profile("gitlab-ci")`). Fix: call `POST /session/set/user` with the SAME `MockHttpSession` before exercising any target that reads `session="user"`. This bit both `UserSettingsMaintainTest` and `AvatarSecurityIntegrationTest` — first attempts silently inserted `Ope=''` instead of the intended user.
2. **`PublicQueryMaintainAuthorization` does NOT have an `isAuthenticated()` fallback for non-query/non-maintain routes.** It returns `AuthorizationDecision(false)` (hard deny) for any URI that doesn't match the hardcoded `QUERY_PUBLIC_LIST`/`MAINTAIN_PUBLIC_LIST` substrings. This means `authenticatedRequestMatchers` is NOT a generic "authenticated users only" bucket — it is specifically for query/maintain actions. A plain new controller route like `/avatar` must NOT be added there; it should be left ungated so it falls through to the final `.anyRequest().authenticated()` rule, which IS a plain `.authenticated()` check. This is the single most important correction versus the design doc for this change, verified empirically per the task's explicit instruction not to assume.
3. **`FileService.getFileStream`/`downloadFile` wrap ALL internal exceptions (including `IOException`/`FileNotFoundException`) as `AWException`.** This means a controller catching `AWException` around a `getFileStream`/`stringToFileData` call safely covers "file missing on disk" as well as "bad token" — both degrade to the same safe `404`, with no separate handling needed for the file-system case.
4. **`awe-controller` has zero Spring context/security tests.** All existing controller tests in that module are Mockito-only. Any test that needs the real `AweWebSecurityConfig` filter chain (matcher behavior, 401/403 semantics) must live in `awe-tests/awe-boot`, which is the only module with `@SpringBootTest` + `spring-security-test` wired via `AbstractSpringAppIntegrationTest`.

## Environment notes (carried over + Slice 2 additions)

- (Slice 1 notes retained below, still accurate for Slice 2 work.)
- After editing any `awe-generic-screens`/`awe-controller`/`awe-spring-boot-starter` source, run `mvn install -pl <module> -am -DskipTests` for the edited module (and its upstream deps) BEFORE re-running `awe-tests/awe-boot`, and clear `awe-tests/awe-boot/target/{tests,schemas,classes/application}` so the freshly built jar is re-unpacked.
- New test classes in `awe-tests/awe-boot` MUST carry `@Tag("integration")` at the class level or `mvn test` silently reports `Tests run: 0` for them (Surefire filters by JUnit 5 tag, default `integration`).

---

## Slice 3 — 4R review fixes (post-MR !665 verification remediation)

Fixes 1-6 from the verified 4R review on branch `feat/572-user-avatar-image` (MR !665, Slices 1+2
already committed). Strict TDD applied throughout: RED confirmed for every fix before GREEN.

### FIX 1 (BLOCKER) — wire the avatar upload endpoint — DONE
- [x] RED: `AvatarControllerTest#uploadAvatarDelegatesToUserSettingsServiceAndReturnsOk` — compile failure (`uploadAvatar` did not exist on `AvatarController`).
- [x] GREEN: added `@PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)` to `AvatarController`, delegating to the already-existing `UserSettingsService.uploadAvatar(MultipartFile)`, returning `200 OK`.
- [x] Authentication: confirmed the endpoint is protected by the same `.anyRequest().authenticated()` fallthrough as `GET /avatar` — deliberately NOT added to `authenticatedRequestMatchers` (see FIX 5).
- [x] **CSRF finding (investigated, not assumed):** `AweWebSecurityConfig#configureCsrf` enables CSRF globally via `CookieCsrfTokenRepository.withHttpOnlyFalse()` + a custom `SpaCsrfTokenRequestHandler` (SPA cookie/header double-submit pattern), applied to the whole filter chain with no path-based exemptions except `/action/logout`. The existing `/file/upload` endpoint (`UploadController`, also an authenticated multipart POST) is CSRF-protected under this same global config — confirmed via `UploadControllerTest`, which uses `.with(csrf())` in every multipart request. `/avatar` upload follows the exact same convention: no CSRF exemption was added, tests use `.with(csrf())`. This is consistent, not a special case.
- [x] Tests: `AvatarControllerTest` (unit, delegation), `AvatarSecurityIntegrationTest#unauthenticatedUploadRequestIsRejected` (401), `AvatarSecurityIntegrationTest#authenticatedUserCanUploadAndThenDownloadTheirOwnAvatar` (real success path, see FIX 2).
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/AvatarController.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/controller/AvatarControllerTest.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### FIX 2 (BLOCKER) — real end-to-end 200 test + rename the misleading test — DONE
- [x] Renamed `authenticatedUserWithAvatarReceivesImageBytes` -> `authenticatedUserWithUnresolvableAvatarFileReceives404` (it asserted 404, was never a "receives image bytes" test).
- [x] Added `authenticatedUserCanUploadAndThenDownloadTheirOwnAvatar`: real `POST /avatar` upload (real, `ImageIO`-generated 1x1 PNG bytes) through the real filter chain, then real `GET /avatar` -> asserts `200`, `Content-Type: image/png`, `Cache-Control: no-cache`, and body bytes byte-for-byte equal to the uploaded bytes.
- [x] Teardown: added `deleteUploadedAvatarFile` in `@AfterEach`, reading the stored token from the DB and calling `FileService.deleteFile` — necessary because the test class is `@Transactional` (DB row insert is rolled back automatically) but the real file write to disk is NOT covered by that rollback and would otherwise leak across test runs.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### FIX 3 (CRITICAL security) — validate image by content, not just declared MIME + nosniff test — DONE
- [x] RED: `UserSettingsServiceTest#uploadAvatarRejectsContentThatDoesNotMatchDeclaredMimeType` — failed with an unhandled `NullPointerException` (HTML/script bytes declared as `image/png` sailed through MIME validation and crashed further down instead of being cleanly rejected).
- [x] GREEN: added `UserSettingsService#validateAvatarContentIsImage`, using `javax.imageio.ImageIO.read(...)` to decode the actual bytes; rejects with a clean `AWException` when the bytes do not decode as an image. **Dependency check performed first:** grepped all poms for `tika`/`imageio` — neither existed. `ImageIO` is part of the JDK (`java.desktop` module), so no new dependency was introduced. Tika was not needed.
- [x] Updated pre-existing tests that used all-zero byte arrays declared as `image/png` (which are not valid image bytes) to use real `ImageIO`-generated PNG bytes, since they now must pass content validation, not just MIME-header validation.
- [x] Added `AvatarSecurityIntegrationTest#avatarResponseIncludesNosniffHeader` asserting `X-Content-Type-Options: nosniff` on `GET /avatar`. Confirmed via passing test that this is a Spring Security default header (no explicit disabling exists anywhere in `AweWebSecurityConfig`) — no controller-side change was needed to satisfy it.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### FIX 4 (CRITICAL correctness) — handle the upsert UNIQUE race gracefully — DONE
- [x] Investigated the exception path first (not assumed): `SQLMaintainConnector#launchAsSingleOperation` catches ALL exceptions from `statement.execute()` (including a SQL unique-constraint violation) and wraps them as `AWEQueryException` (an `AWException` subtype carrying the original cause) — never a raw `DataIntegrityViolationException` reaching `UserSettingsService`. The catch target is therefore `AWException`, not Spring's `DataIntegrityViolationException`.
- [x] RED: `UserSettingsServiceTest#upsertUserAvatarFallsBackToUpdateWhenInsertRaceLosesToConcurrentInsert` — stubs `insertUserAvatar` to throw `AWException` (simulating the losing side of a concurrent-insert race), confirmed the exception propagated uncaught before the fix.
- [x] GREEN: `UserSettingsService#upsertUserAvatar` now catches `AWException` from the insert path and retries as `updateUserAvatar` — the row must now exist (a concurrent request won the race and created it), so converging via update yields the same correct end state for both concurrent uploads instead of a raw failure.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`

### FIX 5 (doc + test) — explain the security-matcher decision — DONE
- [x] Added a class-level Javadoc comment on `AvatarController` explaining that `/avatar` is intentionally NOT added to `SecurityEndpoints.authenticatedRequestMatchers` (gated by `PublicQueryMaintainAuthorization`, which hard-denies non-query/non-maintain routes) and IS correctly protected by the `.anyRequest().authenticated()` catch-all.
- [x] Added a design.md addendum under "Security wiring impact" documenting the same finding and explicitly correcting the original design's "Important interaction" paragraph, which had (incorrectly) assumed the custom authorization manager collapses to `isAuthenticated()` for non-query/non-maintain routes.
- [x] Added `AvatarSecurityIntegrationTest#unauthenticatedUploadRequestIsRejected` (POST -> 401), extending the existing `unauthenticatedRequestIsRejected` (GET -> 401) so both verbs are covered.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/controller/AvatarController.java`, `openspec/changes/add-user-avatar-image/design.md`, `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/security/AvatarSecurityIntegrationTest.java`

### FIX 6 (regression test + design note) — avatar freshness after re-upload — DONE
- [x] Added `QueryTest#testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert`: primes the `queryData` cache by reading `connectedUser`/`getAvatarToken` (no-avatar state) for a user, upserts a new avatar token via the real `POST /action/maintain/upsertUserAvatar` path, then re-reads `connectedUser`/`getAvatarToken` through the same cached HTTP path and asserts the new value is visible — locking in that `MaintainLauncher.launchMaintain`'s `@CacheEvict(cacheNames = "queryData", allEntries = true)` keeps both queries fresh after any maintain call.
- [x] Added a one-line addendum to `design.md` Decision 7 stating that freshness relies on `MaintainLauncher`'s global `@CacheEvict`, not only on the browser-facing `Cache-Control: no-cache` header.
- Files: `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/QueryTest.java`, `openspec/changes/add-user-avatar-image/design.md`

## Test results (Slice 3, actual, evidence-based)

Targeted commands run:
```
mvn test -pl awe-framework/awe-controller -Dtest=AvatarControllerTest              -> 4/4 pass
mvn test -pl awe-framework/awe-controller -Dtest=UserSettingsServiceTest           -> 12/12 pass
mvn test -pl awe-tests/awe-boot -Dtest=AvatarSecurityIntegrationTest               -> 7/7 pass
mvn test -pl awe-tests/awe-boot -Dtest=UserSettingsMaintainTest,SecurityIntegrationTest,AvatarSecurityIntegrationTest -> 17/17 pass
mvn test -pl awe-tests/awe-boot -Dtest="QueryHsqlTest#testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert" -> 1/1 pass
mvn test -pl awe-framework/awe-controller -am (full module)                        -> 541/541 pass
mvn test -pl awe-tests/awe-boot -am (full module)                                  -> 418 total, 414 pass, 1 failure, 3 skipped
```

The single failure in the full `awe-tests/awe-boot` run is the same pre-existing, unrelated
`ActionControllerTest.testLaunchScreenDataActionError` documented in Slices 1 and 2 (whitespace/
line-ending mismatch in a static 404 template comparison; confirmed via `git log` that the file
was last touched by unrelated MRs — "Fix menu-screen restrictions", SSO/OAuth2 integration work —
with no relation to avatar/user-settings work). Not attributed to Slice 3.

## Scope guards verified (Slice 3)

- [x] No column added to `ope`.
- [x] No `HIS*` mirror added.
- [x] No `AweAvatar`/frontend file touched.
- [x] Avatar upload still does not route through the public `/file/upload` (new `/avatar` POST is a dedicated authenticated endpoint).
- [x] Download/upload remain strictly session-user (no new parameter accepts a target username or token).

---

## Slice 4 — Remove `image/webp` from the allow-list (post-MR !665 gate review fix)

A gate review on branch `feat/572-user-avatar-image` (MR !665) found a real regression:
`AVATAR_ALLOWED_MIME_TYPES` included `image/webp`, but `validateAvatarContentIsImage` (added in
Slice 3, task 27) uses `javax.imageio.ImageIO.read()`, which returns `null` for WebP on this
project's stock JDK 17 (no TwelveMonkeys/WebP ImageIO plugin on the classpath). Every legitimate
`.webp` upload was therefore wrongly rejected at content validation, despite passing the MIME
allow-list. `image/png`, `image/jpeg`, `image/gif` decode fine with stock ImageIO and were
unaffected.

### FIX (Slice 4, task 31) — DONE
- [x] RED: added `UserSettingsServiceTest#uploadAvatarRejectsWebpMimeTypeBeforeAttemptingContentValidation`, using real, well-formed WebP bytes (RIFF/WEBP/VP8 container) and a spied `MockMultipartFile` asserting `getInputStream()` is never invoked. This distinguishes "rejected at MIME allow-list" from "rejected at content decoding" — both currently throw `AWException`, so a plain `assertThrows` alone could not prove which stage rejected the upload. Confirmed RED: before the fix the test failed because content validation was reached (`getInputStream()` was called).
- [x] GREEN: removed `"image/webp"` from `AVATAR_ALLOWED_MIME_TYPES` in `UserSettingsService.java`, with a code comment explaining the stock-JDK ImageIO limitation and that a WebP ImageIO plugin (e.g. TwelveMonkeys) would be needed to support it as a future enhancement. No new dependency added.
- [x] Updated `ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE` in all three shipped locales (`Locale-en-GB.xml`, `Locale-es-ES.xml`, `Locale-fr-FR.xml`) to drop "WEBP" from the enumerated allowed types (now "PNG, JPEG and GIF" / "PNG, JPEG y GIF" / "PNG, JPEG et GIF").
- [x] Updated `design.md` Decision 6 (upload constraints) and the Decisions summary (#6) to state the allow-list is `png|jpeg|gif` and note WebP is intentionally excluded, with the same future-enhancement rationale.
- [x] Updated `tasks.md` task 13's historical wording and added a new "Slice 4" section documenting this fix.
- Files: `awe-framework/awe-controller/src/main/java/com/almis/awe/service/UserSettingsService.java`, `awe-framework/awe-controller/src/test/java/com/almis/awe/service/UserSettingsServiceTest.java`, `awe-framework/awe-generic-screens/src/main/resources/application/awe/locale/Locale-{en-GB,es-ES,fr-FR}.xml`, `openspec/changes/add-user-avatar-image/design.md`, `openspec/changes/add-user-avatar-image/tasks.md`

### Test results (Slice 4, actual, evidence-based)

```
mvn test -pl awe-framework/awe-controller -Dtest=UserSettingsServiceTest  -> 13/13 pass
mvn test -pl awe-framework/awe-controller -am (full module)               -> 542/542 pass
```

No regression: all pre-existing `UserSettingsServiceTest` cases (accept png/jpeg/gif, reject disallowed MIME, reject oversized, reject content-mismatched MIME, reject empty file, reject missing MIME, delete-previous-file, upsert race fallback, `getAvatarForCurrentUser` cases) continue to pass unchanged.

The pre-existing unrelated failure `ActionControllerTest.testLaunchScreenDataActionError` (in `awe-boot`) is out of scope for this module-scoped run and was not exercised here (`awe-controller` module only, per task scope).

## Scope guards verified (Slice 4)

- [x] No new dependency added (still uses JDK-provided `javax.imageio.ImageIO`).
- [x] No change to the 2MB size ceiling, the `image/png`/`image/jpeg`/`image/gif` entries, or any other validation path.
- [x] No `AweAvatar`/frontend file touched.
- [x] Existing tests not weakened: all prior assertions kept, only a new test added and locale/doc text updated.

---

## Slice 5 — Fix Oracle-only CI failure in `connectedUser.image` (post-MR !665 Oracle Oracle job fix)

`QueryOracleTest` (CI-only, no local Oracle available) failed 2 avatar tests:
`testConnectedUserWithAvatarReturnsAvatarImagePath` and
`testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert` — both asserting `image=/avatar` when a token
exists, but the row came back with `image=null` on Oracle only. `value`/`name`/`label` were
correct; `getAvatarToken` (a separate, non-computed query reading the same `AvatarImage` column)
also correctly returned the token on Oracle per the CI log — proving the LEFT JOIN and the
underlying column read were sound, and the fault was isolated to the `image` computed's
row-lookup/string-substitution step, on Oracle only. h2/hsqldb passed throughout.

### Root cause investigation — DONE
- [x] Read `ComputedColumnProcessor.computeExpression` (`awe-framework/awe-controller/.../service/data/processor/ComputedColumnProcessor.java`): `row.get(variableKey)` looks up the in-memory `Map<String, CellData>` row by the literal alias string (`"avatarToken"`); falls back to `computed.getNullValue()` only when the cell is absent or `getStringValue()` is empty.
- [x] Traced how that row map is built: `DataListBuilder.generateFromQueryResult` keys each row positionally from `columnNames`, itself derived from the Java-side Querydsl projection (`QTuple` args, `SimpleOperation` alias `PathBuilder.toString()`) — confirmed this keying is Java-side and dialect-independent for the general case, ruling out a naive "Oracle uppercases the alias key" explanation for this specific code path.
- [x] Confirmed via `SQLBuilder.getSqlFieldExpression`/`getFieldExpression` that a plain `<field id="AvatarImage" table="aus" alias="avatarToken">` compiles to `Expressions.as(buildPath("aus","AvatarImage"), buildPath("avatarToken"))` — a `SimpleOperation` whose last arg (`"avatarToken"`) is exactly what `generateFromQueryResult` uses as the row key.
- [x] Confirmed `noprint` has zero effect on SQL/row-building (only used later in `TransformCellProcessor`/`doNoPrint` for output shaping), ruling it out as a differentiator between `avatarToken` (via computed) and `getAvatarToken`'s own `avatarToken` output field (works fine on Oracle).
- [x] Given the SQL/row-keying layer could not be shown to differ by dialect from static analysis, and CI evidence shows the join/column-read itself is correct on Oracle (via `getAvatarToken` succeeding in the same CI run), concluded the fault is specific to `ComputedColumnProcessor`'s consumption of that cell for this computed on Oracle — not reproducible or diagnosable further without live Oracle access (unavailable in this environment). Did not guess further at the exact internal mechanism; instead eliminated the entire code path per the design's documented preferred fix.

### GREEN: SQL-level `<case>` fix (dialect-robust, matches design's preferred solution) — DONE
- [x] Replaced the `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true" />` + `<computed format="'[avatarToken]' !== 'null' ? '/avatar' : null" eval="true" alias="image" nullValue="null" />` pair in `connectedUser` with a single SQL-level `<case alias="image"><when left-field="AvatarImage" left-table="aus" condition="is not null"><then><constant value="/avatar"/></then></when><else><constant value="null" type="NULL"/></else></case>`.
- [x] Confirmed the `<case>`/`<when>`/`condition="is not null">` XML pattern is an existing, tested precedent in the repo (`awe-tests/awe-boot/.../Queries.xml`, e.g. `testCaseWithMultipleConditionInWhenSubquery`), and that `CaseWhen extends Filter` so `left-field`/`left-table`/`condition` resolve through `getCaseExpression`/`getFilters` directly to a Querydsl `BooleanExpression` against the DB column — never touching `ComputedColumnProcessor`.
- [x] Removed the `avatarToken` field/alias entirely (not just hidden via `noprint`): the `<when left-field="AvatarImage" left-table="aus">` filter references the joined column directly without projecting it as an output field, so there is nothing internal left to accidentally leak — stricter than the original "keep it noprint" guard.
- [x] Verified `mvn compile -pl awe-framework/awe-generic-screens` (XSD validation at compile phase) — `BUILD SUCCESS`.
- Files: `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`

### Test results (actual, evidence-based)
```
mvn install -pl awe-framework/awe-generic-screens -DskipTests                    -> BUILD SUCCESS
mvn install -pl awe-tests/awe-boot -am -DskipTests                               -> BUILD SUCCESS
mvn test -pl awe-tests/awe-boot -Dtest="QueryHsqlTest#testConnectedUserWithAvatarReturnsAvatarImagePath+testConnectedUserWithoutAvatarReturnsNullImage+testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert" -> 3/3 pass
mvn test -Ph2 -pl awe-tests/awe-boot -Dtest="QueryH2Test#testConnectedUserWithAvatarReturnsAvatarImagePath+testConnectedUserWithoutAvatarReturnsNullImage+testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert" -> 3/3 pass
mvn test -pl awe-tests/awe-boot (full module, default hsqldb profile)            -> 418 total, 414 pass, 1 failure, 3 skipped
```
Generated SQL confirmed in the log for both dialects:
`(case when aus.AvatarImage is not null then '/avatar' else null end) as image` — a portable,
database-evaluated `CASE WHEN`, identical in structure regardless of dialect-specific rendering
details Querydsl applies underneath.

The single failure in the full `awe-tests/awe-boot` run is the same pre-existing, unrelated
`ActionControllerTest.testLaunchScreenDataActionError` documented in Slices 1-4 (whitespace/
line-ending mismatch in a static 404 template comparison). Confirmed via the surefire XML report
(`TEST-com.almis.awe.test.integration.controller.ActionControllerTest.xml`) that it is the exact
same assertion failure as previously documented, unrelated to this fix.

**Oracle verification note:** Oracle cannot run locally in this environment (no Oracle instance
available). The fix is verified locally on h2 and hsqldb (both pass, both produce the same
portable `CASE WHEN` SQL), and the mechanism was chosen specifically because it removes
`ComputedColumnProcessor` — the component responsible for the Oracle-specific failure — from the
code path entirely, delegating the null-check to the database engine via Querydsl's
dialect-normalized `SQLTemplates`. Final confirmation that `QueryOracleTest` passes depends on the
CI Oracle job on the next pipeline run for MR !665.

### Scope guards verified (Slice 5)
- [x] No column/schema change (fix is `Queries.xml` only).
- [x] `value`/`name`/`label`/`cacheable="true"` unchanged (confirmed via the same tests' JSON assertions).
- [x] No new dependency added.
- [x] No `AweAvatar`/frontend file touched.
- [x] Existing tests not weakened: `testConnectedUserWithoutAvatarReturnsNullImage` (regression) still passes unchanged.

**Slice 5 rollback boundary:** revert the `connectedUser` query change in `Queries.xml` back to the computed-ternary form (or simply revert this slice's diff); Slice 1-4 behavior (schema, upload, download, security, content validation) remains fully intact and independent of this fix.

## Slice 6 — Avatar upload UI (stage-then-claim) — tasks 32-42

Implements design Decision 8: the avatar upload pivots from the direct multipart `POST /avatar`
(unreachable by the awe-react uploader) to AWE's standard stage-then-claim model.

### What was built
- `UserSettingsService.saveUserAvatar(String avatarToken)`: decodes the staged `FileData` token,
  validates it (MIME allow-list `png/jpeg/gif`, `ImageIO` magic-byte decode reading the staged
  bytes from disk, 2MB ceiling), deletes the staged orphan on rejection, then delegates to the
  existing `upsertUserAvatar(user, token)` with the user bound from session; overwrite deletes
  the previous file (existing `deletePreviousAvatarFile`).
- `Maintain.xml` target `saveUserAvatar` (`<variable id="avatarToken" name="CrtAvatar"/>`) +
  `Services.xml` binding to `UserSettingsService.saveUserAvatar(String)`.
- `settings.xml` `profile-panel`: `<criteria component="uploader" id="CrtAvatar"
  destination="avatar"/>` + confirm button `updateAvatar` running the `saveUserAvatar` maintain.
  No `user-settings.xml` edit — the shared `profile-panel` include surfaces the uploader in both
  the base and the notifier user-settings windows (verified by `SettingsScreenAvatarUploaderTest`).
- RETIRED: `@PostMapping("/avatar")` handler and `UserSettingsService.uploadAvatar(MultipartFile)`
  + its three `MultipartFile` validators (superseded; `GET /avatar` unchanged).
- Locale: `PARAMETER_AVATAR` added to Locale-en-GB/es-ES/fr-FR; existing avatar error keys reused.
- Tests migrated to the token/claim flow; new `SaveUserAvatarMaintainTest` and
  `SettingsScreenAvatarUploaderTest`; upsert-race and `GET /avatar` tests kept unchanged.

### Execution note
The apply agent was interrupted mid-verification (a full-suite `mvn test -pl awe-tests/awe-boot
-am` run without `-Dskip.frontend=true` rebuilt the whole reactor and hung the machine; orphaned
Maven/Surefire JVMs were killed). The orchestrator completed verification per the
`awe-integration-tests` skill: full reactor `mvn install -DskipTests -Dskip.frontend=true`, then
targeted `-Dtest=` classes only, no `-am`.

### Test evidence (targeted runs, surefire reports verified fresh)
```
awe-controller:  UserSettingsServiceTest 13/13, AvatarControllerTest 3/3
awe-boot:        SaveUserAvatarMaintainTest 2/2, SettingsScreenAvatarUploaderTest 2/2,
                 AvatarSecurityIntegrationTest 7/7, UserSettingsMaintainTest 2/2
Total: 29/29 pass, 0 failures.
```

### Scope guards verified (Slice 6)
- [x] No DB migration, no `connectedUser` change, no `GET /avatar` change, no security-config change.
- [x] No awe-react/frontend change (uploader component already existed).
- [x] `ope` untouched.
- [x] Upload stages via `/file/upload`; claim binds strictly to `session="user"`.

**Slice 6 rollback boundary:** revert the `settings.xml`/`Maintain.xml`/`Services.xml`/locale
edits and the `UserSettingsService`/`AvatarController` diff; Slices 1-5 (schema, read path,
download, `upsertUserAvatar`) remain fully functional — `image` simply stays null until a token
is inserted by other means.

### Post-MR !665 review fix — path-containment guard in `saveUserAvatar` (security hardening)

**What was wrong:** `saveUserAvatar(String avatarToken)` decoded a client-supplied `FileData`
token via `FileUtil.stringToFileData` — which performs NO validation and NO signature check
(plain Base64+gzip, forgeable offline). The read/decode path
(`validateAvatarContentIsImage` -> `FileService.getFullPath(fileData,false)+fileName` ->
`new FileInputStream(...)`) applied NO path sanitization; `sanitizeFileName`/`fixUntrustedPath`
run only at upload/write time, never here. `design.md` (Context bullet on the token format, and
Decision 8.6's residual note / Decision 5's token-validation note) incorrectly asserted this
decode path was already guarded — it was not. An authenticated user could forge a token with a
`fileName`/`relativePath`/`basePath` escaping the upload area (e.g. `..\..\..\..\somefile` or an
absolute path); if the target file happened to decode as an image, it would become their avatar
and be served back via `GET /avatar` — a scoped authenticated arbitrary-image-file READ oracle.

**Fix:** added `UserSettingsService.validateAvatarPathIsContainedInUploadArea(FileData)`, called
as the FIRST step of `saveUserAvatar`, strictly BEFORE (outside) the existing
validate-and-delete-orphan-on-failure `try/catch`. It rejects the claim (localized
`AWException`, reusing `ERROR_TITLE/MESSAGE_INVALID_AVATAR_MIME_TYPE`) if `fileName`,
`relativePath`, or `basePath` differ from their own `FileUtil.sanitizeFileName` /
`FileUtil.fixUntrustedPath`-sanitized form (any exception thrown while sanitizing, e.g. an
unparsable absolute Windows path, is also treated as rejected). Placing the guard outside the
try/catch is deliberate and safety-critical: on a forged token the catch block calls
`fileService.deleteFile(stagedFileData)`, which on a traversal path would delete the arbitrary
TARGET file rather than a legitimately staged one — the guard ensures `deleteFile` is never
invoked at all for a forged token.

**TDD evidence (`UserSettingsServiceTest`):**
- RED: added `saveUserAvatarRejectsForgedFileNameEscapingUploadDirAndNeverDeletes`,
  `saveUserAvatarRejectsForgedRelativePathEscapingUploadDirAndNeverDeletes`,
  `saveUserAvatarRejectsForgedAbsoluteBasePathEscapingUploadDirAndNeverDeletes` — confirmed
  failing against the pre-fix code (3/3 failures: `deleteFile` was invoked on the forged
  `FileData`, proving the described vulnerability).
- GREEN: implemented the guard; re-ran the full class: 16/16 pass (13 pre-existing + 3 new), no
  regressions on legit staged-token tests (`relativePath="avatar"`, plain filenames).
- Each new test asserts `verifyNoInteractions(maintainService)`, `verify(fileService,
  never()).deleteFile(any())`, and `verify(fileService, never()).getFullPath(any(),
  anyBoolean())` — proving the forged token is rejected before any file-system interaction.

**Docs corrected:** `design.md` Context bullet on the token format, Decision 5's token-validation
note, and Decision 8.6's residual note all corrected to state the true (unguarded) behavior of
`stringToFileData`'s decode path and reference this new guard.

**Scope:** touched only `UserSettingsService.java`, `UserSettingsServiceTest.java`, and
`design.md`/`apply-progress.md`. No XML descriptor, locale, or `AvatarController` change.

**Verification (targeted, per `awe-integration-tests` skill — no `-am`, no full boot suite):**
```
mvn install -pl awe-framework/awe-controller -DskipTests -Dskip.frontend=true -q
mvn -pl awe-framework/awe-controller test -Dtest=UserSettingsServiceTest -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
```

### Post-CI-regression fix — flawed `fixUntrustedPath`-equality guard replaced with real canonical containment (MR !665, `feat/572-user-avatar-image`)

**What broke:** commit `3c52065f` (the guard above) rejected a `fileName`/`relativePath`/
`basePath` whenever it differed from `FileUtil.fixUntrustedPath(value)`. This heuristic is
unsound for `basePath`: `fixUntrustedPath` does `Paths.get(".", path).normalize().toString()` —
it always PREPENDS `.` and re-normalizes relative to the current working directory. A real staged
file's `basePath` is always `baseConfigProperties.getComponent().getUploadFilePath()`, an
absolute (or `@`-relative-then-resolved-absolute) path — and an absolute path can never equal its
own `"."`-prepended, re-normalized "fixed" form. The guard therefore rejected every legitimately
staged file, not just forged ones. CI's "All UT" job caught this on the two integration tests that
exercise a real staged file end-to-end: `SaveUserAvatarMaintainTest
.authenticatedClaimResolvesToSessionUserAndPersistsToken` (expected a persisted token, got `null`
— `saveUserAvatar` threw before ever reaching the upsert) and `AvatarSecurityIntegrationTest
.authenticatedUserCanClaimAStagedAvatarAndThenDownloadIt` (expected `200`, got `404` for the same
reason). Both passed before the guard was added. The mocked `UserSettingsServiceTest` fixtures all
used a clean/absent `basePath` (never a real absolute upload path), so the flaw was invisible at
the unit level and only surfaced in integration tests against a real `FileService.uploadFile`
call.

**Fix:** removed `UserSettingsService.validateAvatarPathIsContainedInUploadArea` and its
`isContainedPathSegment` helper entirely. Added `FileService.isPathWithinUploadArea(FileData)`, a
real canonical-containment check colocated with `getFullPath` (which already owns the
`basePath`/`relativePath`/`baseConfigProperties` path-resolution logic this guard must mirror).
It resolves the trusted upload root the same way `FileService.uploadFile` establishes a staged
file's `basePath` (`StringUtil.getAbsolutePath(baseConfigProperties.getComponent()
.getUploadFilePath(), baseConfigProperties.getPaths().getBase())`), canonicalizes both that root
and the candidate file path (`getFullPath(fileData, false) + fileData.getFileName()`) via
`File.getCanonicalPath()` — which resolves `..` segments and symlinks — and accepts only a
candidate equal to, or nested under, the canonical root. Any exception (unparsable path, I/O
error) fails closed (`false`). `UserSettingsService.saveUserAvatar` now calls
`fileService.isPathWithinUploadArea(stagedFileData)` in the same position as before (BEFORE, and
outside, the validate-and-delete-orphan-on-failure `try/catch`, for the same reason as originally
documented: a forged token must never reach `fileService.deleteFile`).

**Why this design is correct where the previous one was not:** the previous guard compared a raw
value against a *sanitized version of itself* without any awareness of where that value would
ultimately resolve on disk. `isPathWithinUploadArea` instead performs the actual resolution
(mirroring `getFullPath`) and checks the *result* against the real trusted boundary — so a
legitimate absolute `basePath` resolves under the upload root and is accepted, while a forged
`fileName`/`relativePath` traversal or a forged absolute `basePath` pointing elsewhere resolves
outside the upload root (after canonicalization removes any `..` segments) and is rejected.

**TDD evidence:**
- RED (`awe-framework/awe-controller/src/test/java/com/almis/awe/service/FileServiceTest.java`,
  new unit test class): wrote `isPathWithinUploadArea` tests first — two legit-path cases (staged
  file with a relative sub-folder, staged file with no relative path) plus three forged-path cases
  (traversal in `fileName`, traversal in `relativePath`, forged absolute `basePath` outside the
  upload root) — confirmed failing (method did not exist yet).
- GREEN: implemented `FileService.isPathWithinUploadArea`; all 5 new tests pass. Iterated once:
  the first legit-path fixture used a `basePath` without a trailing separator, which does not
  match the real config shape (`uploadFilePath` default is `@upload/`, always trailing-slash) and
  produced a `getFullPath` path-concatenation artifact (`...\junitNNNNavatar\...`, missing
  separator) that failed containment for the wrong reason; fixed the fixture to append a trailing
  separator like the real config value, re-ran — passed.
- Updated `UserSettingsServiceTest`: the 3 forged-token tests now stub
  `fileService.isPathWithinUploadArea(any())` to return `false` (simulating `FileService`'s real
  containment check rejecting the forged token) instead of relying on the removed
  `fixUntrustedPath`-comparison logic executing inline. Every other `saveUserAvatar` test that
  exercises a legit staged path now explicitly stubs `isPathWithinUploadArea(any())` to return
  `true` (default Mockito boolean stub is `false`, which would otherwise reject every legit test
  too) — added directly in `stageFile(...)` for tests using it, and individually in the four tests
  that build a `FileData` inline without staging a real file (oversized, WebP, empty, missing
  MIME). Full class: 16/16 pass (13 pre-existing + 3 forged-token, all updated).

**Regression verification (the gate skipped before the previous fix landed) — exactly the two
integration tests CI failed on, run for real, per `awe-integration-tests` skill (no `-am`, no full
boot suite):**
```
mvn install -pl awe-framework/awe-controller -DskipTests -Dskip.frontend=true -q
mvn -pl awe-framework/awe-controller test -Dtest=UserSettingsServiceTest,FileServiceTest -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
(UserSettingsServiceTest: 16/16 — FileServiceTest: 5/5)

mvn -pl awe-tests/awe-boot test -Dtest=SaveUserAvatarMaintainTest,AvatarSecurityIntegrationTest,UserSettingsMaintainTest -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
(SaveUserAvatarMaintainTest: 2/2 — AvatarSecurityIntegrationTest: 7/7 — UserSettingsMaintainTest: 2/2)
```
`SaveUserAvatarMaintainTest.authenticatedClaimResolvesToSessionUserAndPersistsToken` and
`AvatarSecurityIntegrationTest.authenticatedUserCanClaimAStagedAvatarAndThenDownloadIt` — the two
tests CI reported failing — both pass.

**Scope:** `FileService.java` (new `isPathWithinUploadArea` method), `UserSettingsService.java`
(guard call site updated, flawed helpers removed), `FileServiceTest.java` (new unit test class),
`UserSettingsServiceTest.java` (forged/legit test stubs updated), this file. Confirmed no other
caller referenced the removed `validateAvatarPathIsContainedInUploadArea`/
`isContainedPathSegment` helpers (both were `private` to `UserSettingsService`). No XML
descriptor, locale, or `AvatarController` change.

### Post-MR !665 fix — home sidebar/navbar avatar (`ButUsrAct`) did not refresh after save

**What was wrong:** `ButUsrAct` (the avatar shown in the home sidebar/navbar, awe-react view
`base`) never reloaded after a user saved a new avatar via the `updateAvatar` button in the
user-settings modal (view `report`). The two components live in different views, so nothing in
the `report`-view save action ever reached `base`'s avatar component.

**Fix:** `UserSettingsService.saveUserAvatar(String)` now returns `ServiceData` (was `void`). On
a successful claim it returns `new ServiceData().addClientAction(new ClientAction("filter")
.setAddress(new ComponentAddress("base", "ButUsrAct", null, null)))` — the same
explicit-cross-view-address pattern already used by `UserService.changeUserMode` (`select` action
addressed to `base`/`currentMode`). Verified end-to-end: `MaintainService.manageMaintainQueries`
replaces its `result` with any non-null `ServiceData` returned by the `<serve>`d Java method, and
`ActionService.launchAction` appends `serviceData.getClientActionList()` to the `maintain` action's
`ok`-answer response list, so the `filter` client action rides the same `/action/maintain/saveUserAvatar`
response as the existing `end-load`/`message` actions. No XML change was needed: `Services.xml`'s
`<java classname="..." method="saveUserAvatar">` binding only names the method and parameter
types (`JavaConnector` resolves the method via `getMethod(name, paramTypes)`, ignoring the return
type), so widening `void` -> `ServiceData` requires no descriptor edit.

**TDD evidence:** confirmed RED first by stashing the production change and running
`UserSettingsServiceTest` with the new
`saveUserAvatarReturnsServiceDataWithFilterClientActionAddressedToBaseAvatar` test in place — compile
failure (`incompatible types: void cannot be converted to ServiceData`). Restored the implementation
(GREEN). Extended `SaveUserAvatarMaintainTest.authenticatedClaimResolvesToSessionUserAndPersistsToken`
with a `jsonPath("$[?(@.type == 'filter')]")` assertion on the real `/action/maintain/saveUserAvatar`
HTTP response, proving the action reaches the client end-to-end (not just the unit-level return
value).

**Known follow-up (deliberately not handled here):** when a user REPLACES an existing avatar
(same `/avatar` URL, new bytes behind it), the browser may still show a cached image after the
`filter` reload if it does not revalidate against `Cache-Control: no-cache` aggressively enough
(e.g. some browsers/proxies cache GET responses more eagerly for `<img>` tags than for
XHR/fetch). This is a pre-existing caveat of the stable-URL design (Decision 7) and is not
introduced by this fix; a future increment could append a cache-busting query parameter to the
`filter`/`select` client action if this is observed in practice.

**Verification (targeted, per `awe-integration-tests` skill — no `-am`, no full boot suite):**
```
mvn install -pl awe-framework/awe-controller -DskipTests -Dskip.frontend=true -q
mvn -pl awe-framework/awe-controller test -Dtest=UserSettingsServiceTest -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0

mvn -pl awe-tests/awe-boot test -Dtest=SaveUserAvatarMaintainTest,AvatarSecurityIntegrationTest,UserSettingsMaintainTest -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
(SaveUserAvatarMaintainTest: 2/2 — AvatarSecurityIntegrationTest: 7/7 — UserSettingsMaintainTest: 2/2)
```

**Scope:** `UserSettingsService.java` (`saveUserAvatar` return type + client action),
`UserSettingsServiceTest.java` (new assertion test), `SaveUserAvatarMaintainTest.java` (end-to-end
`jsonPath` assertion), this file. No XML descriptor, no security-config, no locale change.

---

## Post-MR !665 fix — avatar cache-buster: `connectedUser.image` becomes `/avatar?v=<hash>`

**What was wrong (the follow-up flagged in the previous fix, above):** the sidebar avatar `<img>`
now reloads its `connectedUser` data after a save (previous fix), but the `image` URL itself was
still the constant `/avatar` (Decision 7). Some browsers do not aggressively revalidate a
`Cache-Control: no-cache` `<img>` GET on a same-URL reload, so a re-uploaded avatar could still
render stale bytes even though the server-side data and the download endpoint were both already
correct.

**Fix (strict TDD, RED -> GREEN):** `connectedUser.image` is now a CHANGING URL,
`/avatar?v=<hash>`, where `<hash>` is a base36-encoded djb2 hash of the already-stored
`AvatarImage` token — no new DB column, no SQL-level hash function (none exist portably across
all six dialects), and never the raw token itself in a client-visible URL (Decision 1/5's threat
model: the raw `FileData` token is a server-side capability reference and must not be replayable
via the client). `GET /avatar` (Decision 5) is unaffected — it ignores the `?v` query parameter
entirely, resolving strictly from the session-bound `AweUserSettings` row as before.

**Mechanism:** reintroduced `<field id="AvatarImage" table="aus" alias="avatarToken"
noprint="true"/>` in `connectedUser` (`Queries.xml`), replacing the Slice-5 SQL-level
`<case>`/`<when condition="is not null">` with an AWE `<computed eval="true" nullValue="null">`:

```xml
<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true"/>
<computed alias="image" eval="true" nullValue="null"
  format="'[avatarToken]' === 'null' ? null : '/avatar?v=' + (function(s){var h=5381;for(var i=0;i&lt;s.length;i++){h=(((h&lt;&lt;5)+h)+s.charCodeAt(i))&gt;&gt;&gt;0;}return h.toString(36);})('[avatarToken]')"/>
```

The trim/ignorecase LEFT JOIN filter (`lower(trim(both from aus.Ope)) = lower(trim(both from
ope.l1_nom))`, the earlier Oracle char-padding fix) was left untouched.

**Why dialect-independent (verified by reading the code, not assumed):**
`ComputedColumnProcessor.evaluateExpression` calls `StringUtil.eval(value, context)` ->
`context.eval("js", expression)` against a GraalVM `Context` bean — this runs entirely in the JVM,
on the row already fetched into memory, strictly AFTER `SQLQueryConnector` executes the
(Querydsl-normalized) SQL and closes the result set. The generated SQL for `connectedUser` is now
IDENTICAL in shape across dialects (`select l1_nom as value, OpeNam as name, aus.AvatarImage as
avatarToken from ope ... left join AweUserSettings as aus on ...`) — only the raw token is
selected; the hash arithmetic (djb2, `>>> 0`-masked to 32 bits, base36-encoded) never touches SQL
or Querydsl's `SQLTemplates` dialect layer.

**KNOWN, EXPLICITLY ACCEPTED RISK (not silently glossed over):** this reintroduces the same
general code-path shape — a LEFT-JOINed `noprint` field read back by an `eval="true"` computed on
`connectedUser` — that Slice 5 found to fail specifically on Oracle (`image` evaluated to `null`
despite a present token), via a fault in `ComputedColumnProcessor`'s row-lookup step that was
**never conclusively root-caused** (Slice 5 eliminated the code path rather than diagnosing it
further, because Oracle was unavailable locally). The cache-buster requirement (a value-derived,
changing URL) cannot be expressed as a portable SQL-level `CASE WHEN` without a dialect-specific
SQL hash function (none exist uniformly across h2/hsqldb/mysql/oracle/postgresql/sqlserver), so
reverting to the `eval` computed was the only option that avoids both a SQL-level hash AND
embedding the raw token — but this is a deliberate trade-off, not a guarantee the earlier Oracle
issue cannot recur. **This MUST be confirmed by the Oracle CI job (`QueryOracleTest`) on the next
pipeline run before being trusted in production; it cannot be verified locally (no Oracle instance
in this environment, same limitation as Slice 5).** If Oracle CI reproduces the earlier symptom,
the documented fallback (see `design.md` Decision 7 ADDENDUM) is to move the hash computation out
of `ComputedColumnProcessor` into a small Java-side post-processing step over the already-built
datalist, sidestepping whatever Oracle-specific behavior affected the generic computed-column
pipeline for this field.

**TDD evidence (`QueryTest.java`, run via `QueryHsqlTest`/`QueryH2Test`):**
- RED: updated `testConnectedUserWithAvatarReturnsAvatarImagePath` and
  `testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert` to expect `/avatar?v=<hash>` (computed via a
  new `avatarTokenHash` test helper reproducing the production djb2/base36 algorithm in Java) —
  confirmed failing against the pre-fix `Queries.xml` (`image` still `/avatar`, unparameterized).
  `testConnectedUserWithoutAvatarReturnsNullImage` (the null/no-avatar case) was left UNCHANGED —
  not weakened.
- GREEN: applied the `Queries.xml` change above; re-ran — all pass.
- Extended `testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert` with two new assertions beyond the
  original freshness check: (1) **stability** — re-saving the SAME token (forcing another cache
  evict + recompute) yields the SAME `?v=` value; (2) **freshness** — saving a genuinely DIFFERENT
  token yields a DIFFERENT `?v=` value (`assertNotEquals`), proving the cache-buster actually
  busts the cache only when the avatar really changes.
- The Java `avatarTokenHash` test helper was cross-checked against a real Node.js execution of the
  exact same JS snippet embedded in `Queries.xml` (`fake-avatar-token` -> `1hbxnxy`,
  `fresh-avatar-token` -> `rlo4on`) before wiring it into assertions, and the actual test run
  against the real GraalVM-evaluated production expression reproduced the identical hash values —
  cross-verifying the Java test helper, the Node reference execution, and the production GraalVM
  expression all agree.

**Verification (targeted, per `awe-integration-tests` skill — no `-am`, no full boot suite):**
```
mvn install -pl awe-framework/awe-generic-screens -DskipTests -Dskip.frontend=true -q
mvn -pl awe-tests/awe-boot test -Dtest="QueryHsqlTest#testConnectedUserWithAvatarReturnsAvatarImagePath+testConnectedUserWithoutAvatarReturnsNullImage+testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert" -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0   (HSQLDB)

mvn test -Ph2 -pl awe-tests/awe-boot -Dtest="QueryH2Test#testConnectedUserWithAvatarReturnsAvatarImagePath+testConnectedUserWithoutAvatarReturnsNullImage+testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert" -Dsurefire.failIfNoSpecifiedTests=false
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0   (H2)
```

Generated SQL confirmed in the log for both dialects (only cosmetic reserved-word quoting differs
between HSQLDB and H2 for the `value` alias):
```
select l1_nom as "value", OpeNam as name, aus.AvatarImage as avatarToken
from ope ope
left join AweUserSettings as aus
  on lower(trim(both from aus.Ope)) = lower(trim(both from ope.l1_nom))
where lower(trim(both from l1_nom)) = lower('test') limit 30 offset 0
```
— confirming the trim/ignorecase JOIN filter (the earlier Oracle char-padding fix) is untouched,
and the query now only selects the raw `avatarToken`; `image` is derived entirely post-SQL.

Actual `image` values observed in the response JSON matched the expected hashes exactly in every
case: `/avatar?v=1hbxnxy` (`fake-avatar-token`), `/avatar?v=rlo4on` (`fresh-avatar-token`, twice —
proving stability across re-save), `/avatar?v=1js35f0` (`fresh-avatar-token-v2`, proving the
version differs when the token differs).

**Oracle verification note (same limitation as Slice 5):** Oracle cannot run locally in this
environment. Local verification covers h2 and hsqldb only. Given the KNOWN RISK documented above,
this fix's correctness on Oracle is NOT yet independently confirmed and depends on the
`QueryOracleTest` CI job on the next pipeline run for MR !665 — this is flagged as a residual risk
in the return summary, not asserted as resolved.

**Scope guards verified:**
- [x] No column/schema change (fix is `Queries.xml` + `QueryTest.java` only).
- [x] `value`/`name`/`label`/`cacheable="true"` unchanged.
- [x] `GET /avatar` unchanged; still ignores any query parameter and resolves strictly from
  `session="user"`.
- [x] No new dependency added (djb2 hash is a handful of lines of inline JS, evaluated by the
  already-present GraalVM `Context` bean).
- [x] No `AweAvatar`/frontend file touched.
- [x] The no-avatar (null) test assertion was NOT weakened — left exactly as it was.

**Rollback boundary:** revert `Queries.xml`'s `connectedUser` `image`/`avatarToken` fields back to
the Slice-5 SQL-level `<case>` (or simply revert this fix's diff) and revert the three test
assertions in `QueryTest.java`; the rest of the avatar feature (schema, upload, download, security,
`saveUserAvatar` claim flow) remains fully intact and independent of this fix.

**Scope:** `awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`
(`connectedUser` query), `awe-tests/awe-boot/src/test/java/com/almis/awe/test/integration/database/QueryTest.java`
(3 test methods updated/extended, 1 new private helper), `openspec/changes/add-user-avatar-image/design.md`
(Decision 7 addendum + query-section update + summary #7), this file. No Java service/controller
change, no migration, no security-config change, no locale change.
