# Design: Per-user avatar image

## Context and current state

Verified against the codebase (not assumed):

- **`connectedUser` query** (`awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`, id `connectedUser`, line 1808) selects `l1_nom` as `value`, `OpeNam` as `name`, and a computed `label`, from table `ope`, filtered by `l1_nom = :Usr` where `Usr` is bound from `session="user"`. It is `cacheable="true"`. There is no `image` field today.
- **File infrastructure already exists.** `FileService` (`awe-framework/awe-controller/src/main/java/com/almis/awe/service/FileService.java`) stores uploads on the **filesystem** under `baseConfigProperties.getComponent().getUploadFilePath()` (relative folder `tmp<n>`), never in the database. `uploadFile(MultipartFile, folder)` returns a `FileData`. `downloadFile(FileData, id)` / `getFileStream(...)` stream bytes back.
- **Token format already exists.** `FileUtil.fileDataToString(FileData)` (`awe-framework/awe-model/.../util/file/FileUtil.java`) produces the opaque reference token used across AWE: `Base64( gzip( compressJson( JSON(FileData) ) ) )`. `FileUtil.stringToFileData(String)` reverses it. `FileData` carries `fileName`, `fileSize`, `mimeType`, `relativePath`, `basePath`. **Corrected (post-MR !665 security review): `stringToFileData` itself performs NO validation and NO signature check — it is plain Base64+gzip, forgeable offline by any authenticated caller.** `FileUtil.sanitizeFileName` and `fixUntrustedPath` are applied only at upload/write time (`FileService.uploadFile`), never on the decode/read path. Any decode-then-read call site (such as `UserSettingsService.saveUserAvatar`, see Decision 8) that accepts a client-supplied token MUST apply its own containment guard before reading the file; see Decision 8's path-containment guard for the concrete fix.
- **Existing file endpoints are PUBLIC.** In `AweWebSecurityConfig#configureAuthorization` (line 215) the matchers from `SecurityEndpoints#getFileRequestMatchers()` — `/file/text`, `/file/stream`, `/file/download`, `/file/upload`, `/file/delete` — are `permitAll`. Only `/file/stream/maintain/**` and `/file/download/maintain/**` require authentication (`authenticatedRequestMatchers`). This means the generic `/file/download` endpoint CANNOT be reused for avatars: it is unauthenticated and accepts an arbitrary client-supplied `FileData` token, which would leak any file by token replay. A dedicated authenticated route is required.
- **ID generation is query-based, not sequence-based.** `AweUsrFav.IdeFav` is assigned by maintain target `addToFavourites` via `<field id="IdeFav" query="nextIdForFavourites"/>`, where `nextIdForFavourites` computes `COALESCE(MAX(IdeFav),0)+1`. There is no DB sequence for favourites. The avatar table follows the same pattern.
- **Migration state.** The proposal states the latest migration is `V1.1.0`; this is STALE. The real latest across all six dialect folders (`awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/<dialect>/`) is **`V1.2.1__Audit_HISope_changes.sql`** (h2/hsqldb/mysql/oracle/sqlserver) with `V1.2.0__OAuth2_ope_changes.sql` before it; postgresql tops out at `V1.1.0`. The next version is therefore **`V1.2.2`**, not `V1.1.1`/`V1.2.0`.
- **`AweUsrFav` shape** (reference satellite): `IdeFav` PK, `Ope varchar(20)` username, `Opt varchar(100)`, `Ord int`. No `Act`, no dates, and **no `HIS*` mirror**.
- **`AweAvatar` contract** (frontend, in the separate `awe-react` client, not in this repo): computes `computedImage = getContextPath() + (values?.[0]?.image ?? image_attribute)` and issues a browser **GET** for that URL, cookie/session-authenticated like any other same-origin asset. It falls back image -> icon -> initial. No frontend change is in scope.
- **Upload size limit** already exists: `spring.servlet.multipart.max-file-size = awe.application.component.upload-max-file-size` (default `100MB`) in `config/base.properties`.

## Design goals

1. Per-user avatar with zero schema change to `ope` and zero frontend change.
2. Reuse AWE's existing filesystem file store and `FileData` token encoding rather than inventing a new store.
3. Store only a reference token (not bytes) in the new `AweUserSettings` satellite table.
4. Surface the token to `connectedUser` as a `getContextPath()`-relative URL string in alias `image`, so `AweAvatar` renders it unchanged.
5. Add a NEW authenticated download route (the existing `/file/download` is public and unsafe for this) with an explicit authorization rule.
6. Keep everything additive and backward compatible (LEFT JOIN, null when absent).

---

## Decision 1 — Binary storage location + token format

**Decision:** Store the avatar **bytes on the filesystem** using the existing AWE upload store (`FileService.uploadFile` -> `awe.application.component.upload-file-path`), in a dedicated `avatar` destination folder. Store in `AweUserSettings` the existing **AWE `FileData` token** (`FileUtil.fileDataToString`) — the same opaque, compressed, Base64 reference AWE already uses everywhere for uploaded files. Do **not** add a blob column and do **not** touch `ope`.

**Rationale:**
- AWE already has a battle-tested filesystem store and a canonical token codec (`FileUtil`). Introducing a blob column or a new store would duplicate infrastructure and add DB weight for binary payloads — the opposite of the proposal's "keep `ope` light" intent.
- The token is opaque to the client and self-describing to the server (filename, relativePath, mimeType, size), so the download endpoint can reconstruct the exact file without a second lookup.
- The `AweUserSettings` column holds the token as text, keeping the table small.

**Token encoding decision:** reuse `FileUtil.fileDataToString` / `stringToFileData` verbatim. The stored value is the Base64 string. It is treated as a server-side secret capability reference: it is NOT the thing the client sends to download (see Decision 5). The DB column is sized for this Base64 payload (see Decision 2).

**Rejected alternatives:**
- *Blob column in `AweUserSettings`* — rejected: puts binary in the DB, heavier reads, and diverges from AWE's file model.
- *Opaque UUID + separate mapping* — rejected: adds a lookup table and a new ID scheme when `FileData` already encodes the location safely; UUID gives no extra security over an authenticated endpoint that ignores the client-supplied path (Decision 5).
- *New standalone document store* — rejected: out of proportion for one image per user; reuses nothing.

---

## Decision 2 — `AweUserSettings` column set + types (6 dialects)

**Decision:** Model on `AweUsrFav` (identifier + `Ope varchar(20)`), one row per user, with a token column and standard audit-lite columns matching the satellite pattern.

Columns:
- `IdeUsrSet` — PK, integer, assigned via `MAX+1` query (no sequence), mirroring `IdeFav`.
- `Ope varchar(20) NOT NULL` — username (`l1_nom`), the user key. **Unique** so the LEFT JOIN yields at most one row per user (favourites is many-per-user; avatar is one-per-user, so add a UNIQUE constraint here — a deliberate divergence from `AweUsrFav`).
- `AvatarImage varchar(4000)` (nullable) — the `FileData` reference token (Base64). 4000 chars is the safe common ceiling (Oracle `varchar2` pre-12c limit); a small `FileData` token is a few hundred chars, so this is generous. Use `varchar(4000)` uniformly for cross-dialect parity; nullable so a settings row can exist without an avatar.

No `Ord`, no `Opt`. `Act`/date columns are NOT added — `AweUsrFav` has none and the avatar token does not need lifecycle flags; absence of a token = no avatar.

### DDL per dialect

`AWE_V1.2.2__user_settings.sql`

**h2 / hsqldb:**
```sql
CREATE TABLE IF NOT EXISTS AweUserSettings (
    IdeUsrSet int NOT NULL PRIMARY KEY,     -- Table identifier
    Ope varchar(20) NOT NULL,               -- Username (l1_nom)
    AvatarImage varchar(4000),              -- Avatar image reference token (FileData)
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
);
```
(hsqldb: `CREATE TABLE` without `IF NOT EXISTS` if the dialect baseline omits it; match the style used in that dialect's `V1.0.2__favourites.sql` — h2 uses `IF NOT EXISTS`, hsqldb uses plain `CREATE TABLE`.)

**mysql:**
```sql
CREATE TABLE IF NOT EXISTS AweUserSettings (
    IdeUsrSet int NOT NULL PRIMARY KEY,
    Ope varchar(20) NOT NULL,
    AvatarImage varchar(4000),
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
);
```

**oracle:**
```sql
CREATE TABLE AweUserSettings (
    IdeUsrSet number(5) CONSTRAINT pk_AweUserSettings PRIMARY KEY NOT NULL,
    Ope varchar2(20) NOT NULL,
    AvatarImage varchar2(4000),
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
);
```

**postgresql:**
```sql
CREATE TABLE IF NOT EXISTS AweUserSettings (
    IdeUsrSet integer NOT NULL PRIMARY KEY,
    Ope varchar(20) NOT NULL,
    AvatarImage varchar(4000),
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
);
```
Note: postgresql migrations currently stop at `V1.1.0` (it never received `V1.2.0`/`V1.2.1`, which were `ope`/`HISope` column retypes not needed on postgres). The new file is still named `V1.2.2` to keep the version line aligned across dialects. This is NOT a Flyway contiguity problem (verified): `spring.flyway.locations=classpath:db/migration/{vendor}` gives each dialect its own isolated history and no out-of-order setting exists (default false); Flyway checks ordering only within one dialect, so postgres going V1.1.0 to V1.2.2 is valid. Do NOT add no-op placeholders for postgres.

**sqlserver:**
```sql
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='AweUserSettings' AND xtype='U')
CREATE TABLE AweUserSettings (
    IdeUsrSet int NOT NULL PRIMARY KEY,
    Ope varchar(20) NOT NULL,
    AvatarImage varchar(4000),
    CONSTRAINT uk_AweUserSettings_Ope UNIQUE (Ope)
);
```
(Match the guard idiom used in that dialect's existing `V1.0.2__favourites.sql`; if it uses a plain `CREATE TABLE`, follow that instead — the apply phase must copy the dialect's own conventions.)

**PK strategy:** manual assignment via query `MAX(IdeUsrSet)+1` (see Decision on maintain wiring), consistent with `AweUsrFav`. No sequence object is created (none exists for favourites).

---

## Decision 3 — `HIS*` audit mirror

**Decision:** **No `HIS*` mirror.** Justification: the reference satellite `AweUsrFav` has none; the avatar token is user-owned cosmetic preference data, not security- or money-relevant, and carries no regulatory audit need. The `HISope`/`HISAwe*` mirrors in this schema exist for identity/authorization/config tables (`ope`, `AwePro`, `AweMod`, ...), not for user cosmetic satellites. Adding a mirror would introduce trigger/maintain audit wiring with no consumer. If a future change adds audited settings to `AweUserSettings`, the mirror can be added then as its own migration; it is not warranted now.

---

## Decision 4 — Migration version + filenames

**Decision:** Next version is **`V1.2.2`** (the real latest is `V1.2.1`; the proposal's `V1.1.0` premise is stale). File name per dialect:

```
awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/h2/AWE_V1.2.2__user_settings.sql
.../hsqldb/AWE_V1.2.2__user_settings.sql
.../mysql/AWE_V1.2.2__user_settings.sql
.../oracle/AWE_V1.2.2__user_settings.sql
.../postgresql/AWE_V1.2.2__user_settings.sql
.../sqlserver/AWE_V1.2.2__user_settings.sql
```

Rationale: `V1.2.2` is a minor additive table creation on top of the `V1.2.x` line; it is not a new feature epoch requiring `V1.3.0`. Prefix `AWE_` and double-underscore description match every existing file. All six files must be byte-consistent in intent (same columns, same constraint), differing only in dialect syntax — the apply phase must diff them against each dialect's `V1.0.2__favourites.sql` to copy that dialect's exact idioms (`IF NOT EXISTS`, `number(5)` vs `int`, `varchar2` vs `varchar`, sqlserver existence guard).

Also add the table to the awe-boot test schema files (`awe-tests/awe-boot/src/main/resources/sql/schema-*.sql`) which already contain `AweUsrFav`, so integration tests that bootstrap from those scripts see the new table.

---

## Decision 5 — Download endpoint authorization rule

**Decision:** **Self-only, session-authenticated, server-resolved token.** The download route is a NEW authenticated endpoint that ignores any client-supplied file token and instead resolves the avatar for **the currently authenticated user** from `AweUserSettings`.

Route: `GET /avatar` (context-path relative, no path parameter). Behavior:
1. Require an authenticated session (add its matcher to `authenticatedRequestMatchers`, NOT `fileRequestMatchers`). Unauthenticated -> AWE's standard `401`/entry-point handling.
2. Server reads the session username (`session="user"`, same binding `connectedUser` uses).
3. Query `AweUserSettings` for that user's `AvatarImage` token.
4. If present, `FileUtil.stringToFileData(token)` and stream the bytes via `FileService.getFileStream(FileData)` with the stored `mimeType` and `Content-Disposition: inline`.
5. If absent -> `404` (frontend then falls back to icon/initial exactly as today).

**Why self-only, and why not reuse `/file/download`:**
- The existing `/file/download` is `permitAll` and trusts a client-supplied `FileData` token — a classic capability-replay/enumeration leak. Reusing it for avatars would let anyone fetch any stored file by replaying a token. REJECTED.
- Binding the resolved file to the authenticated principal means there is **no client-supplied path or token to guess or tamper with**; authorization is implicit in "you can only fetch your own avatar." This is the strictest safe default and needs no ACL table.
- The `image` alias returned by `connectedUser` is a static relative URL (`/avatar`, see Decision 7), identical for every user; it carries no secret, so cache/log exposure is harmless.

**Rejected alternatives:**
- *Public (token in URL)* — rejected: leaks images by token guessing (an explicit proposal risk).
- *Same-profile / admin visibility of others' avatars* — rejected for v1: no screen needs to render another user's avatar (the sidebar shows only the connected user). If a future feature (e.g. a user grid with avatar thumbnails) needs cross-user avatars, add a separate authenticated `GET /avatar/{username}` guarded by `AccessService`/profile checks then; do not widen the v1 endpoint speculatively.
- *Token-in-DB used as the download key* — rejected: makes the token a bearer secret that must be protected in transit/logs; self-only resolution avoids that entirely.

**Token validation:** the stored token is never accepted from the client; the server produces it. On resolution, `stringToFileData` failure, missing file on disk, or empty token all resolve to `404` (never a `500` leaking internals). **Corrected (post-MR !665 security review):** this route's safety comes from the token being server-produced and session-bound, NOT from any path sanitization on the decode step — `FileUtil.sanitizeFileName`/`fixUntrustedPath` are NOT applied on `stringToFileData`'s decode/read path (they only run at upload/write time). This distinction matters because the CLAIM path (`saveUserAvatar`, Decision 8) decodes a client-supplied token and therefore cannot rely on "server produces the token" — it needs, and now has, its own explicit path-containment guard (see Decision 8.6's corrected residual note).

---

## Decision 6 — Upload constraints

**Decision:**
- **Max size:** cap avatars well below the global 100MB multipart limit. Enforce an avatar-specific ceiling of **2MB** in the upload service (reject larger with a localized error, reusing the `MaxUploadSizeExceededException` message path). Do not raise the global limit.
- **Allowed MIME types:** allow-list **`image/png`, `image/jpeg`, `image/gif`** only. Reject anything else (validated server-side via `FileData.getMimeType()` from `FileUtil.extractContentType`, not by trusting the client filename extension). WebP intentionally excluded — stock JDK 17 ImageIO cannot decode it; supporting it would require a WebP ImageIO plugin (e.g. TwelveMonkeys) as a future enhancement.
- **Normalization:** v1 does NOT re-encode or resize server-side (keeps scope tight and avoids an image-processing dependency). The frontend already renders the raw image; a square avatar is a UI concern handled by existing CSS. Store the uploaded bytes as-is under the `avatar` folder. Note this as a possible future enhancement.
- **Overwrite semantics:** one avatar per user. On new upload, delete the previously referenced file (via `FileService.deleteFile(oldFileData)`) after the token update succeeds, to avoid orphaned files; if delete fails, log and continue (the row already points to the new file).

**Rationale:** aligns with the existing upload pipeline and limits, adds a proportionate avatar-specific guard, and avoids storing unsafe/oversized payloads (an explicit proposal risk). MIME allow-list + size cap are the minimum safe validation.

---

## Decision 7 — URL alignment (`getContextPath() + image`)

**Decision:** `connectedUser` returns the literal string **`/avatar`** in the `image` alias when the user has an avatar, and null/empty otherwise. `AweAvatar` then builds `computedImage = getContextPath() + "/avatar"`, which resolves to the authenticated download endpoint (Decision 5). No token appears in the URL.

Because the URL is static per deployment, `AweAvatar`'s browser GET is cookie/session-authenticated (same-origin) and the server resolves the current user's avatar. When the user has no row, `image` is null -> `AweAvatar` falls back to icon/initial exactly as today.

**Cache consideration:** `connectedUser` is `cacheable="true"`. Returning a constant `/avatar` string (not a per-file token) keeps the cached value stable even when a user changes their avatar — the URL never changes, only the bytes behind it do. To avoid the browser caching a stale image after an upload, the download response should send `Cache-Control: no-cache` (or the frontend can append a cache-buster; but since no frontend change is in scope, `no-cache` on the endpoint is the chosen mechanism). This is a deliberate design point: **the identity of the URL is stable; freshness is controlled by response headers.**

**Server-side cache freshness (verified, not assumed):** the `queryData` cache entry for `connectedUser` (and for `getAvatarToken`) is not left to expire on its own after an avatar upload. `MaintainLauncher.launchMaintain` carries `@CacheEvict(cacheNames = "queryData", allEntries = true)`, so **every** maintain call — including `upsertUserAvatar` — flushes the entire `queryData` cache, forcing `connectedUser` to be recomputed (and see the new `AweUserSettings` row) on the very next read. Browser-side `Cache-Control: no-cache` on `GET /avatar` handles image-byte freshness; this global maintain-triggered evict is what makes the server-side `image` alias (and the underlying avatar token) fresh after a re-upload. Regression-locked by `QueryTest#testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert`.

Producing the `image` value: since the URL is constant, `connectedUser` does not need to embed the token. It only needs to know *whether* a row exists. Emit `image` as a SQL-level `<case>`/`<when condition="is not null">` on the joined `AvatarImage` column, gated on the LEFT JOIN row presence — see the `connectedUser` query section above for the exact XML.

**Oracle CI fix (post-MR !665, resolved):** the initial implementation emitted `image` via an AWE computed (`ComputedColumnProcessor`, `eval="true"` ternary reading a projected `avatarToken` field). This passed on h2/hsqldb but failed on Oracle only (`QueryOracleTest`): `image` evaluated to `null` even when a token was present, while `getAvatarToken` — a separate, non-computed query reading the same column — correctly returned the token, proving the LEFT JOIN and column read were sound on Oracle and the fault was isolated to the computed's row-lookup/string-substitution step for this field on this dialect. The fix moves the null-check into the SQL `CASE WHEN` itself (dialect-normalized by Querydsl, evaluated by the database), removing `ComputedColumnProcessor` and the `avatarToken` field/alias from this path entirely. See the "Final mechanism" note under the `connectedUser` query section for full detail.

**ADDENDUM (post-MR !665, cache-buster) — `image` becomes a CHANGING URL, `/avatar?v=<hash>`:** the constant-URL design above has a known caveat, documented in `apply-progress.md`'s "home sidebar/navbar avatar" fix: because the `<img>` src (`/avatar`) never changes, some browsers do not aggressively revalidate a `Cache-Control: no-cache` GET issued from an `<img>` tag after the client-side `filter` reload that follows a save, so a stale image can persist visually even though the server-side data and bytes are already fresh. The fix is to make `connectedUser.image` a **version-stamped URL**, `/avatar?v=<hash>`, where `<hash>` is derived from the already-stored `AvatarImage` token — changing the URL string itself (not just response headers) forces the browser to treat a re-uploaded avatar as a new resource. `GET /avatar` (Decision 5) is unaffected: it ignores the `?v` query parameter entirely and keeps resolving the avatar strictly from the session-bound `AweUserSettings` row, so download behavior, authorization, and the `no-cache` header are unchanged.

Constraints honored, per the original design goals: **no new DB column** (the version is derived, not stored) and **no SQL-level hash function** (portable hash functions do not exist uniformly across all six dialects — `CASE`/`CAST` do, but `MD5`/`CRC32`/equivalents are dialect-specific or absent, e.g. Oracle's is `ORA_HASH`, SQL Server's is `CHECKSUM`/`HASHBYTES`, H2/HSQLDB have none built-in — so any SQL-level hash would itself be a six-way dialect-drift risk, the exact class of problem Decision 4/design goal #1 is trying to avoid). The **raw token is never placed in the URL**: Decision 1 established the stored `AvatarImage` token as the same opaque `FileData` codec used elsewhere as a server-side capability reference (see the Context bullet on `FileUtil.stringToFileData`'s lack of validation); if the raw token appeared client-side in `image`, and any endpoint ever accepted it as an input again, it would be a directly replayable capability string. Hashing collapses it to a non-reversible, non-cryptographic version fingerprint that changes if and only if the token changes, which is exactly the property a cache-buster needs and nothing more — the hash need not resist deliberate forgery (an attacker who can already read `connectedUser` already knows their own avatar's freshness state), only accidental staleness.

**Mechanism — reintroduces `ComputedColumnProcessor` for this field (KNOWN RISK, explicitly accepted, see below):** producing `/avatar?v=<hash>` requires actually reading the token value (not just its null-ness), so the SQL-level `<case>`/`CASE WHEN` from the resolved-Slice-5 mechanism above is no longer sufficient by itself — it can express "row exists y/n" in pure SQL, but computing a string hash of a column value, portably, in SQL, across six dialects, is precisely the dialect-drift problem the design goals reject (see constraints paragraph above). The only remaining option that avoids both a SQL-level hash function AND embedding the raw token is to hash the token in Java, after the row is fetched — which means going back through an AWE `<computed eval="true">` (`ComputedColumnProcessor`, backed by GraalVM JS via `StringUtil.eval`/`context.eval("js", ...)`, confirmed by reading the processor directly) reading a re-introduced `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true"/>`. This is the SAME general shape (LEFT JOIN column -> `noprint` field -> `eval="true"` computed reading `[avatarToken]`) that Slice 5 found to fail specifically on Oracle for this exact query, via a fault that was never conclusively root-caused (see the "Why the original computed-ternary plan failed on Oracle" note below) — Slice 5 eliminated the code path entirely rather than diagnosing it further, precisely because Oracle was unavailable locally.

**Why this is dialect-independent in principle, despite the above:** `ComputedColumnProcessor.evaluateExpression` calls `StringUtil.eval(value, context)` -> `context.eval("js", expression)` — a GraalVM `Context` bean, evaluated entirely in the JVM, on the row already materialized in memory (`Map<String, CellData>`), strictly AFTER `SQLQueryConnector` has executed the (dialect-normalized, Querydsl-built) SQL and closed the result set. Nothing about the hash computation itself touches the database or Querydsl's `SQLTemplates` layer, so the arithmetic (djb2, `>>> 0`-masked to 32 bits, base36-encoded) is bit-for-bit identical regardless of which of the six dialects produced the row. The one thing this argument does NOT cover is whether `ComputedColumnProcessor`'s *row-lookup* step (`row.get("avatarToken")`, the exact step Slice 5's investigation could not fully explain on Oracle) behaves identically for `connectedUser` specifically on Oracle now that the field is reintroduced — that is a genuinely open question, not resolved by this reasoning, and can only be closed by the Oracle CI job (`QueryOracleTest`) actually running green on this exact XML.

**Residual risk and required follow-up:** this reintroduces exactly the code path Slice 5's note below says "do not revert to." The decision to do so anyway is deliberate — a pure SQL-level `<case>` cannot produce a value-derived hash without either a dialect-specific SQL hash function (rejected above) or the raw token in the URL (rejected in Decision 5/1's threat model) — but it is NOT risk-free. **Before this is trusted in production, the Oracle CI job (`QueryOracleTest`) covering `testConnectedUserWithAvatarReturnsAvatarImagePath` and `testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert` MUST be observed green on the actual pipeline; it cannot be verified locally (no Oracle instance in this environment), same limitation as Slice 5.** If Oracle CI reproduces the earlier `image=null`-despite-token-present symptom, the fallback is to move the hash computation out of `ComputedColumnProcessor` entirely — e.g. a small Java-side post-processing step over the already-built datalist (outside the generic computed-column pipeline), which sidesteps whatever Oracle-specific behavior affected `ComputedColumnProcessor`'s row lookup for this field — rather than attempting a third XML-only iteration.

---

## `connectedUser` query change

Add a LEFT JOIN to `AweUserSettings` on username and expose `image`. The join returns at most one row (UNIQUE `Ope`), preserving `cacheable` single-row semantics.

```xml
<!-- GET CONNECTED USER -->
<query id="connectedUser" cacheable="true">
  <table id="ope" />
  <field id="l1_nom" alias="value" />
  <field id="OpeNam" alias="name" />
  <computed format="[name] ([value])" alias="label" />
  <field id="AvatarImage" table="aus" alias="avatarToken" noprint="true"/>
  <computed alias="image" eval="true" nullValue="null"
    format="'[avatarToken]' === 'null' ? null : '/avatar?v=' + (function(s){var h=5381;for(var i=0;i&lt;s.length;i++){h=(((h&lt;&lt;5)+h)+s.charCodeAt(i))&gt;&gt;&gt;0;}return h.toString(36);})('[avatarToken]')"/>
  <join type="LEFT">
    <table id="AweUserSettings" alias="aus" />
    <and>
      <!-- trim: ope.l1_nom is char(20) (blank-padded on Oracle); without trim the varchar2 vs char comparison never matches on Oracle -->
      <filter condition="eq" left-field="Ope" left-table="aus" right-field="l1_nom" right-table="ope" trim="true" ignorecase="true" />
    </and>
  </join>
  <where>
    <and>
      <filter left-field="l1_nom" condition="eq" right-variable="Usr" ignorecase="true" />
    </and>
  </where>
  <variable id="Usr" type="STRING" session="user" />
</query>
```

**CURRENT mechanism (post-MR !665 cache-buster, supersedes the SQL-level `<case>` below):** `image` is now `/avatar?v=<hash>` (non-null token) or `null` (no token), where `<hash>` is a base36-encoded djb2 hash of the joined `AvatarImage` column, computed by an AWE `<computed eval="true">` reading a reintroduced `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true"/>`. This is evaluated entirely post-SQL by `ComputedColumnProcessor` via GraalVM JS (`context.eval("js", ...)`), so the hash arithmetic itself is dialect-independent by construction. See Decision 7's ADDENDUM above for the full rationale, the constraints that force this shape (no new column, no SQL-level hash function, never the raw token in the URL), and — importantly — the explicit acknowledgment that this reintroduces the same general code path (`ComputedColumnProcessor` reading a LEFT-JOINed `noprint` field for this exact query) that the "Why the original computed-ternary plan failed on Oracle" note immediately below describes as having an unresolved, Oracle-only failure mode. This is a known, accepted residual risk pending Oracle CI confirmation, not a claim that the earlier Oracle issue cannot recur.

**Slice 5 mechanism (superseded by the cache-buster ADDENDUM above; kept here for history — root-caused and fixed a post-MR !665 CI Oracle failure at the time):** `image` was computed **at the SQL level** via an AWE `<case>`/`<when condition="is not null">` on the joined `AvatarImage` column, compiling to a portable `CASE WHEN aus.AvatarImage IS NOT NULL THEN '/avatar' ELSE NULL END` evaluated by the database engine itself — not by AWE's `ComputedColumnProcessor` string-substitution layer. This is the same `<case>`/`CaseWhen` construct already proven in the repo (`awe-tests/awe-boot/.../Queries.xml`, `testCaseWithMultipleConditionInWhenSubquery` and others) and it removes the need for a separate `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true">` — the `<when left-field="AvatarImage" left-table="aus">` filter references the joined column directly without projecting it as a client-visible or internal output field at all, so there is no `avatarToken` alias to accidentally leak and nothing for the client to ever see beyond the derived `image` string.

**Why the original computed-ternary plan failed on Oracle (root cause, confirmed by reading `ComputedColumnProcessor.computeExpression`):** the original approach projected `<field id="AvatarImage" table="aus" alias="avatarToken" noprint="true" />` and then read it back via `<computed format="'[avatarToken]' !== 'null' ? '/avatar' : null" eval="true" alias="image" nullValue="null" />`. `ComputedColumnProcessor.computeExpression` resolves `[avatarToken]` via `row.get("avatarToken")` on the in-memory `Map<String, CellData>` row built by `DataListBuilder.generateFromQueryResult`, and only falls back to `computed.getNullValue()` when the cell is absent or its `getStringValue()` is empty. CI evidence showed `getAvatarToken` (a separate, plain, non-computed query selecting the same `AvatarImage` field) correctly returned the token on Oracle, while `connectedUser`'s computed-driven `image` still evaluated to `null` — proving the LEFT JOIN and the underlying column read were both fine on Oracle, and the fault was isolated to the computed's string-substitution/lookup step for this specific field, on this dialect only. Rather than continue chasing the exact internal Querydsl/row-keying interaction that differentiates Oracle here, the fix removes `ComputedColumnProcessor` from this code path entirely by pushing the null-check into the SQL `CASE WHEN`, which is dialect-normalized by Querydsl's `SQLTemplates` layer and evaluated by the database, not by AWE's Java-side computed engine. This is strictly more robust than diagnosing (and re-diagnosing per dialect) a string-substitution edge case in `ComputedColumnProcessor`.

The `<when left-field="AvatarImage" left-table="aus" condition="is not null">` clause is a SQL predicate against the joined table's column, independent of any projected/aliased output field — so `avatarToken` no longer needs to exist as a field at all. This is stricter than "not exposed to the client": it removes the internal alias entirely, leaving `image` as the only artifact of this join visible anywhere in the query's output contract, consumed by `AweAvatar` exactly as before.

Historical note (superseded): the original plan called for a **pure-XML `eval="true"` computed ternary**, citing the precedent `awe-tests/awe-boot/.../Queries.xml:1605` (`<computed alias="icon" format="[Prg2] < 0 ? 'exclamation' : null" eval="true"/>`). That approach worked on h2/hsqldb but failed the `QueryOracleTest` CI job (`testConnectedUserWithAvatarReturnsAvatarImagePath`, `testAvatarUpdateIsVisibleAfterCacheEvictOnUpsert`) with `image=null` when a token was actually present.

**Update (post-MR !665 cache-buster):** the "do not revert to the computed-ternary form" guidance above was correct for Slice 5's constraints (presence/absence only), but the cache-buster requirement (a value-derived, changing URL) cannot be expressed as a portable SQL-level `CASE WHEN` without a dialect-specific SQL hash function — so the computed-ternary form (now hashing, not a plain presence check) has been deliberately reintroduced. This is a known, explicitly accepted risk, not an oversight; see Decision 7's ADDENDUM and the "CURRENT mechanism" note above for the full justification and the required Oracle CI follow-up.

---

## Java service / controller touchpoints

- **New controller** `AvatarController` (`awe-framework/awe-controller/.../controller/`): `@GetMapping("/avatar")` returning `ResponseEntity<FileSystemResource>` (or `byte[]`), `inline`, `Cache-Control: no-cache`; resolves current user, reads token, delegates to `FileService`. Extends `ServiceConfig` like `FileController`. `404` when no avatar.
- **New service methods** on a settings service (new `UserSettingsService extends ServiceConfig`, or extend `FileService`): `uploadAvatar(MultipartFile)` — validate MIME/size, `FileService.uploadFile(file, "avatar")`, then upsert token into `AweUserSettings` for the session user (delete old file); `getAvatarForCurrentUser()` — return `FileData` or empty.
- **Reuse** `FileService.uploadFile` / `getFileStream` / `deleteFile` and `FileUtil` token codec unchanged.
- **`AweUserDetails`/`ope` untouched.**

## XML descriptor impact

- **`Queries.xml`** (`awe-generic-screens/.../global/Queries.xml`): edit `connectedUser` (LEFT JOIN + `image`); add `nextIdForUserSettings` (`COALESCE(MAX(IdeUsrSet),0)+1`) mirroring `nextIdForFavourites`; add a `getUserSettings`/`getAvatarToken` read query keyed by session user.
- **`Maintain.xml`** (`awe-generic-screens/.../global/Maintain.xml`): add `upsertUserAvatar` target. Because `Ope` is UNIQUE and one-per-user, model as insert-or-update: either a `<serve service="...">` that decides insert vs update, or an update target plus a guarded insert. `IdeUsrSet` from `nextIdForUserSettings` on insert, `Ope` from `session="user"`, `AvatarImage` from the uploaded token variable.
- **`Services.xml`** (`awe-generic-screens/.../global/Services.xml`): register the avatar upload/settings service(s) if invoked through maintain `<serve>`.
- **Locale**: add `ERROR_TITLE_*`/`ERROR_MESSAGE_*` for invalid MIME and size, plus any upload UI labels, in `awe/locale/Locale-*.xml`.

## Security wiring impact

- Add `PathPatternRequestMatcher...matcher("/avatar")` to `SecurityEndpoints.authenticatedRequestMatchers` (authenticated), NOT to `fileRequestMatchers` (public). This is the single security-config edit and it is additive.
- **Important interaction (verified — apply must validate, not assume):** the `authenticatedRequestMatchers` group is NOT gated by a plain `.authenticated()` — it is gated by `.access(publicQueryMaintainAuthorization(elements))`, a custom `AuthorizationManager` (`PublicQueryMaintainAuthorization`) whose rule is `isAuthenticated() || isPublicQuery/Maintain(request)`. For a plain controller route like `GET /avatar` (neither a query nor a maintain action), `getTarget(request)` resolves the last URI segment (`"avatar"`) and `elements.getQuery("avatar")` finds nothing, so `isPublicQuery` returns `false` and the effective rule collapses to `isAuthenticated()` — which is the desired self-only behavior. Apply MUST add an integration test asserting `/avatar` returns `401` unauthenticated and `200` authenticated, to confirm this custom manager treats the route as auth-required (do not rely on the reasoning alone).
- The avatar **upload** goes through the existing authenticated action/maintain path (or a new authenticated `/file`-style endpoint that is added to `authenticatedRequestMatchers`, never `permitAll`). Do not route avatar upload through the public `/file/upload`.
- CSRF: the download is a GET (no CSRF token needed, matches existing static/file GET behavior). Upload uses the same CSRF-protected action path as other authenticated writes.

**ADDENDUM (empirically verified during apply, supersedes the "Important interaction" paragraph above):** the assumption that `PublicQueryMaintainAuthorization` "collapses to `isAuthenticated()`" for a non-query/non-maintain route is **false**. Its `check()` method only grants `isAuthenticated() || isPublicX(request)` when the URI matches a hardcoded `QUERY_PUBLIC_LIST`/`MAINTAIN_PUBLIC_LIST` substring; for any other route (including `/avatar`) it falls through to `return new AuthorizationDecision(false)` — an unconditional deny, even for a fully authenticated user. Adding `/avatar` to `authenticatedRequestMatchers` was tested directly and produced a hard `403 Forbidden` for authenticated users, which would have made the endpoint permanently unusable.

**Corrected decision:** do NOT add `/avatar` to `SecurityEndpoints.authenticatedRequestMatchers`. Leave it unmatched by both `fileRequestMatchers` (public) and `authenticatedRequestMatchers` (`PublicQueryMaintainAuthorization`-gated), so it falls through to the final `.anyRequest().authenticated()` catch-all in `AweWebSecurityConfig#configureAuthorization` — a plain, unconditional authentication check, which is exactly the desired self-only semantics. This rationale is also documented as a code comment on `AvatarController` so a future maintainer does not "fix" this by adding the matcher. Confirmed by `AvatarSecurityIntegrationTest` (401 unauthenticated for both `GET` and `POST /avatar`, correct resolution once authenticated, zero net diff on `SecurityEndpoints.java`).

## Cross-module and DB-profile consequences

- **`ope` untouched** -> the ~40 files reading `ope`-backed preferences (login, security filters, session, theme cascade) are unaffected. No `HISope` change.
- **Six-dialect DDL drift** is the main DB risk: the `AweUserSettings` DDL must match each dialect's `V1.0.2__favourites.sql` idioms exactly. postgresql's migration line lags (`V1.1.0` latest), but this is a non-issue (verified): per-vendor isolated Flyway locations (`classpath:db/migration/{vendor}`) and no `out-of-order` setting mean each dialect's history is checked independently, so postgres `V1.1.0 -> V1.2.2` is valid. Do NOT add placeholder migrations and do NOT edit released migrations.
- **Test schema parity**: `awe-tests/awe-boot/.../sql/schema-*.sql` must also gain `AweUserSettings` so integration tests bootstrapped from those scripts (rather than Flyway) see the table.
- **Modules**: change is confined to `awe-controller` (Java), `awe-generic-screens` (XML), `awe-spring-boot-starter` (migrations + security matcher). No REST/scheduler/notifier/developer module impact. The REST API (`awe-rest`) does not expose `connectedUser`, so REST auth modes are unaffected.

## Test strategy notes (strict TDD, runner `mvn`)

RED before GREEN, small increments:

1. **Migration applies** on CI-exercised dialects: table exists, `Ope` unique, `AvatarImage` nullable. (`awe-tests/awe-boot` integration; targeted `mvn test -pl awe-tests/awe-boot -am`.)
2. **`connectedUser` query**: returns `image = "/avatar"` when a matching `AweUserSettings` row with non-null token exists; returns null `image` when no row (LEFT JOIN), preserving existing `value`/`name`/`label`. Query-service integration test.
3. **Upload service**: accepts allowed MIME within 2MB, stores a token in `AweUserSettings` (insert then update path / overwrite deletes old file); rejects disallowed MIME and oversize with the localized error; stores the `FileData` token, not bytes, in the row.
4. **`AvatarController` `GET /avatar`**: authenticated self-user returns the correct bytes and `mimeType` with `no-cache`; no-avatar user returns `404`; **unauthenticated returns `401` and does not stream** (MockMvc + `spring-security-test`). Assert a user cannot fetch another user's avatar because resolution is bound to the session principal (no path/token input exists to attempt it).
5. **Security config**: `/avatar` is in `authenticatedRequestMatchers`, not `permitAll`; existing `/file/*` matchers unchanged.

Targeted commands:
```bash
mvn test -pl awe-framework/awe-controller -am
mvn test -pl awe-tests/awe-boot -am
```

---

## Decision 8 — Avatar upload UI (PIVOT: uploader criterion + claim maintain, supersedes the `POST /avatar` upload path)

**Status of prior upload decision:** Decision 6's *constraints* (2MB cap, `image/png|jpeg|gif` allow-list, magic-byte `ImageIO` check, WebP exclusion, overwrite-deletes-old) STAND. What is **SUPERSEDED** is the *upload transport*: the earlier increment shipped a direct multipart controller route `POST /avatar` bound to `UserSettingsService.uploadAvatar(MultipartFile)`. That mechanism does not fit AWE's screen-driven upload model and is retired here (see "Retirement" below). Decision 5's `GET /avatar` download, the `AweUserSettings` table, `connectedUser.image`, `upsertUserAvatar(user, token)`, and the `insertUserAvatar`/`updateUserAvatar`/`getAvatarToken`/`nextIdForUserSettings` descriptors all STAND unchanged.

### 8.1 Why the pivot is forced (verified)

AWE's only screen upload primitive is the awe-react `uploader` criteria component (`AweInputUploader` + `useUpload`). Verified in this repo on the backend side: its hook always POSTs to the framework endpoint `getContextPath() + getRestUrl("file","upload")` = **`/file/upload`** (`UploadController.handleFileUpload`, `awe-controller/.../controller/UploadController.java`), with `destination`, `address`, `u`, the connection header, Authorization + X-XSRF-TOKEN. The endpoint is NOT configurable to hit an arbitrary route, so it can never POST to `POST /avatar`. Therefore a screen-driven avatar upload MUST go through `/file/upload`, which is a **staging** step: it calls `FileService.uploadFile(file, destination)` (stores bytes on the filesystem under the `destination` folder) and then **broadcasts** a `file-uploaded` client action carrying `path = FileUtil.fileDataToString(fileData)` (the AWE `FileData` token), plus `name`/`size`/`type`. The uploader criterion captures that `path` token as **its own criterion value**. A separate, authenticated maintain must then CLAIM that staged token for the session user.

This is the standard AWE two-phase file model (stage via public `/file/upload`, claim via an authenticated maintain that reads the criterion's token) — the same shape every other AWE upload uses. The direct-multipart `POST /avatar` was an out-of-band path the uploader can never drive.

### 8.2 Screen change — `settings.xml` `profile-panel` only (zero `user-settings.xml` change, appears in both windows)

**Decision:** Add the avatar uploader to the `profile-panel` of the SINGLE source screen `awe-framework/awe-generic-screens/.../screen/users/settings.xml`, in the existing `SCREEN_TEXT_USER_DATA` block, placed **above** the `name`/`email` criteria (avatar is the visual identity header for the profile tab).

**Verified duplication-safety:** both `awe/screen/users/user-settings.xml` (profile + security tabs) and the notifier override `awe-notifier/.../screen/user-settings.xml` (profile + security + notifications tabs) render the profile tab via `<include target-screen="settings" target-source="profile-panel"/>`, pulling from this one `settings.xml`. So editing `settings.xml`'s `profile-panel` surfaces the avatar UI in BOTH the base and notifier user-settings windows with no duplication and no edit to either `user-settings.xml`. Confirmed by reading all three files.

**Uploader criterion + save trigger.** Add:
- A `<criteria component="uploader" id="CrtAvatar" .../>` whose `destination` folder is `avatar` (matching the existing `AVATAR_UPLOAD_FOLDER = "avatar"` constant and Decision 1's folder) so staged avatar bytes land in the same folder the download path reads from. If the uploader component supports an image `accept` restriction, set it to the allowed image types as a client-side convenience; server-side validation (8.4) remains authoritative regardless.
- A **confirm button** (`id="updateAvatar"`, `BUTTON_CONFIRM`, `btn-primary`) that runs the claim maintain via `<button-action type="server" server-action="maintain" target-action="saveUserAvatar"/>`, mirroring the sibling `updateUserData` button already in this panel (which does `validate` then `server`/`maintain`/`updateUserData`).

**Save trigger decision — explicit confirm button, NOT auto-save on upload.** Rationale: (1) consistency with the two existing buttons in this exact panel (`updateUserData`, `updatePassword`) and with AWE screens like `scheduler-parameters.xml` that stage inputs then commit on an explicit action; (2) `/file/upload` only STAGES bytes — the user has not committed intent until they confirm, and an auto-save-on-upload would persist a token the moment a file is picked, defeating the ability to cancel; (3) it keeps the claim inside the standard CSRF-protected authenticated maintain action path. Auto-save-on-upload (a `dependency` firing `maintain-silent` on the uploader's value change, like the `enable2fa` checkbox pattern) is a viable alternative but REJECTED for v1 for the commit-intent reason above; it can be revisited if UX asks for it.

**Token flow from criterion to maintain.** The uploader criterion's value after staging IS the `FileData` token string (the broadcast `path`). The `saveUserAvatar` maintain reads it as a normal criterion-sourced variable: `<variable id="avatarToken" type="STRING" name="CrtAvatar"/>` (variable `name` = the criterion id), exactly how any maintain reads a criterion value. No custom plumbing — the token rides the standard criterion-value channel.

### 8.3 Backend claim path — new `saveUserAvatar` maintain wrapping validation + the EXISTING `upsertUserAvatar`

**Decision:** Add a new authenticated maintain target `saveUserAvatar` (in `awe-generic-screens/.../global/Maintain.xml`) that `<serve>`s a new service method `UserSettingsService.saveUserAvatar(String avatarToken)`. That method:
1. Reads the session user via `getSession().getUser()` (never a client-supplied username) — same binding `connectedUser`/`getAvatarToken` use.
2. Decodes the staged token with `FileUtil.stringToFileData(avatarToken)`.
3. **Validates the staged `FileData`** (MIME allow-list + magic-byte image decode + 2MB), see 8.4.
4. Calls the EXISTING `upsertUserAvatar(user, FileUtil.fileDataToString(validatedFileData))` — the already-shipped insert-or-update primitive with the TOCTOU/UNIQUE fallback — passing the user bound from session and the (re-encoded, unchanged) token.
5. Reuses the EXISTING overwrite semantics (Decision 6): capture the previous token first, and after a successful upsert delete the previously referenced file via `FileService.deleteFile(oldFileData)`, log-and-continue on failure. This is the same `deletePreviousAvatarFile` behavior the retired `uploadAvatar(MultipartFile)` had.

**Maintain wiring (mirrors the existing `upsertUserAvatar` target, adding validation):**
```xml
<target name="saveUserAvatar">
  <serve service="saveUserAvatar">
    <variable id="avatarToken" type="STRING" name="CrtAvatar"/>
  </serve>
</target>
```
Service registration in `Services.xml` binds `saveUserAvatar` -> `UserSettingsService.saveUserAvatar(String avatarToken)` with a single `STRING avatarToken` service-parameter; the user is resolved inside the method from session, NOT passed as a parameter, so the maintain can never be driven to write another user's row.

**Why a new `saveUserAvatar` method rather than reusing the raw `upsertUserAvatar` target directly from the screen:** the existing `upsertUserAvatar` target performs NO validation — it trusts the token blindly. Wiring the screen straight to it would let a crafted request claim an arbitrary staged file (e.g. a non-image staged by `/file/upload`) with no MIME/size check. `saveUserAvatar` is the validating gate in front of the existing `upsertUserAvatar`. Keep `upsertUserAvatar` as the internal upsert primitive; do not expose it directly to the screen.

### 8.4 Validating a STAGED `FileData` (not a `MultipartFile`)

**Decision:** Port the three retired checks (`validateAvatarMimeType`, `validateAvatarContentIsImage`, `validateAvatarSize`) to operate on the staged file resolved from the token, reusing the SAME constants (`AVATAR_ALLOWED_MIME_TYPES`, `AVATAR_MAX_FILE_SIZE`, the two error-key pairs) and the SAME `ImageIO.read` magic-byte approach. Mechanism:
- **MIME:** validate `fileData.getMimeType()` against `AVATAR_ALLOWED_MIME_TYPES`. Note `FileService.uploadFile` set this via `FileUtil.extractContentType(file)` at staging time, so it is the server-derived type, not a client filename guess — the same trust level the retired path had.
- **Magic-byte image decode:** open the staged file's bytes via `FileService.getFileStream(fileData)` / the resolved `getFullPath(fileData, false) + fileName`, and run `ImageIO.read(inputStream)`; reject if it decodes to `null` (rejects non-image bytes mislabeled as an allowed image, including real WebP, which stock JDK 17 ImageIO cannot decode — WebP stays excluded).
- **Size:** validate `fileData.getFileSize()` (or the on-disk file length) against `AVATAR_MAX_FILE_SIZE` (2MB).
- On any failure, throw the localized `AWException` (same error keys) AND delete the just-staged orphan file (`FileService.deleteFile(fileData)`), because unlike the retired path — where a rejected `MultipartFile` was never persisted — here the bytes are ALREADY on disk from the `/file/upload` staging step. This orphan cleanup on rejection is NEW behavior the pivot requires; call it out in tasks.

**Interaction with `/file/upload`'s own size cap (verified):** `/file/upload` enforces only the global `spring.servlet.multipart.max-file-size` (default 100MB) via `MaxUploadSizeExceededException`; it does NOT know about the avatar-specific 2MB. So the avatar 2MB ceiling MUST be re-applied at claim time in `saveUserAvatar` (a >2MB but <100MB image stages successfully at `/file/upload`, then is rejected by `saveUserAvatar` and its orphan deleted). Do not attempt to make `/file/upload` avatar-aware — it is a shared generic endpoint.

### 8.5 Retirement — delete `POST /avatar` + `uploadAvatar(MultipartFile)` (recommended: delete, not keep-as-API)

**Decision — DELETE, do not keep as a parallel API:**
- Remove the `@PostMapping("/avatar")` handler `uploadAvatar(MultipartFile)` from `AvatarController` (keep the class, keep `GET /avatar` unchanged; drop the now-unused `PostMapping`/`MultipartFile`/`RequestParam`/`ResponseStatus` imports).
- Remove `UserSettingsService.uploadAvatar(MultipartFile)` and its three private `MultipartFile` validators, migrating their logic into the new `saveUserAvatar(String)` staged-file validators (8.4). Keep `getAvatarForCurrentUser`, `getStoredAvatarToken`, `deletePreviousAvatarFile`, and `upsertUserAvatar` — all still used.
- Update tests: remove `AvatarControllerTest.uploadAvatarDelegatesToUserSettingsServiceAndReturnsOk`; migrate the `UserSettingsServiceTest` upload/validation cases (`uploadAvatarAcceptsAllowedMimeType...`, `...RejectsDisallowedMimeType`, `...RejectsWebpMimeType...`, `...RejectsOversizedFile`, `...RejectsContentThatDoesNotMatch...`, `...RejectsEmptyFile`, `...RejectsMissingMimeType`, `...DeletesPreviousFileAfterSuccessfulReupload`, `...ContinuesWhenOldFileDeleteFails`, `rejectedUploadDoesNotCallUpsert`) to drive `saveUserAvatar(String token)` with a staged `FileData` instead of a `MockMultipartFile`. Keep the `upsertUserAvatar` race test and the `getAvatarForCurrentUser` tests as-is.
- Remove the multipart `POST /avatar` case from `AvatarSecurityIntegrationTest` and replace it with a maintain-path auth assertion (the `saveUserAvatar` maintain requires authentication and binds to `session="user"`).

**Rationale for delete over keep-as-API:** a prior review already flagged DUPLICATE upsert/upload paths on this change. Keeping `POST /avatar` alive alongside the uploader+maintain flow would (1) leave a second, uploader-unreachable write path that must still be security-tested and maintained, (2) re-introduce exactly the duplicate-path smell the review objected to, and (3) risk drift between two validators. There is no external consumer: `awe-react`'s uploader cannot call it, and no REST client is documented against it. Delete it.

### 8.6 Security — public staging, authenticated claim, self-only (accepted AWE pattern)

**Decision / confirmation:** `/file/upload` is intentionally PUBLIC (`permitAll` per Decision 5's security map) — it only STAGES bytes into a `tmp*`/`avatar` folder and returns a token; staging alone grants nothing. The `saveUserAvatar` **maintain** is authenticated (maintain actions run through the authenticated action path with CSRF) and resolves the user from `session="user"`, so a user can only ever claim a staged file into THEIR OWN `AweUserSettings` row — they cannot set another user's avatar. `GET /avatar` download stays self-only (Decision 5, unchanged). This stage-public / claim-authenticated split is the established AWE upload pattern (every AWE uploader stages via the same public `/file/upload`), so it is the accepted convention, not a new exception.

**Residual note (in scope for tasks, not a blocker):** because `/file/upload` is public, an unauthenticated caller can stage arbitrary bytes into the upload area (a pre-existing framework property, not introduced here). The avatar feature does not widen this: only an authenticated `saveUserAvatar` call can bind a staged token to a user, and it re-validates MIME/size, so a staged non-image or oversized file can never become anyone's avatar. No new security-config edit is required for this increment (the `saveUserAvatar` target rides the existing authenticated maintain matcher; `GET /avatar` keeps its Decision-5-addendum catch-all handling).

**Corrected (post-MR !665 security review) — path-containment guard added to `saveUserAvatar`:** an earlier version of this note additionally asserted the decode/read path was guarded by `FileUtil.sanitizeFileName`/`fixUntrustedPath`. That was FALSE. `FileUtil.stringToFileData(String)` — the decode step `saveUserAvatar` calls on the client-supplied `avatarToken` — performs NO validation and NO signature check (plain Base64+gzip, forgeable offline by any authenticated caller); `sanitizeFileName`/`fixUntrustedPath` run ONLY at upload/write time inside `FileService.uploadFile`, never on this decode-then-read path. Without a guard, an authenticated caller could forge a token whose `fileName`/`relativePath`/`basePath` escape the upload area (e.g. `..\..\..\etc\passwd` or an absolute path); `validateAvatarContentIsImage` would then read that arbitrary file from disk via `FileService.getFullPath(fileData, false) + fileData.getFileName()` with no sanitization, and if it happens to decode as an image, it becomes the caller's avatar and is served back via `GET /avatar` — a scoped authenticated arbitrary-image-file READ oracle. Decision 8 (`saveUserAvatar`) now adds an explicit `validateAvatarPathIsContainedInUploadArea` guard that runs BEFORE the existing MIME/content/size validation, rejecting any `fileName`/`relativePath`/`basePath` that differs from its own `FileUtil.sanitizeFileName`/`fixUntrustedPath`-sanitized form. This guard MUST run outside (before) the validate-and-delete-orphan-on-failure `try/catch`: on a forged token, calling `fileService.deleteFile(stagedFileData)` inside that catch would delete the arbitrary target file rather than a legitimately staged one, so a forged token is rejected with no `deleteFile` call at all. `GET /avatar` (Decision 5) is unaffected: it never accepts a client-supplied token, resolving the avatar strictly from the session-bound, server-produced `AweUserSettings` row.

### 8.7 Locale

Add avatar UI labels to `awe/locale/Locale-*.xml`: an uploader label (e.g. `PARAMETER_AVATAR` / `SCREEN_TEXT_AVATAR`) and the confirm button reuses the existing `BUTTON_CONFIRM`. The four validation error keys already exist (`ERROR_TITLE_INVALID_AVATAR_MIME_TYPE`, `ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE`, `ERROR_TITLE_AVATAR_TOO_LARGE`, `ERROR_MESSAGE_AVATAR_TOO_LARGE`) and are reused unchanged by the staged-file validators.

### 8.8 Spec + tasks impact (for sdd-spec / sdd-tasks)

**Spec requirement to change — "Users can upload an avatar image":** its mechanism must be restated from "an upload path that lets an authenticated user submit an image" (implying a direct multipart endpoint) to the **stage-then-claim** model: the user stages the image through the framework uploader (public `/file/upload`), then an authenticated self-bound `saveUserAvatar` maintain validates the staged file and persists the token. The three existing scenarios (successful upload, oversized rejection, disallowed-MIME rejection) stay valid in intent but their WHEN/THEN should reference validation-at-claim-time on the staged file (and add: a staged file rejected at claim time is deleted, leaving no `AweUserSettings` change). Add a scenario: an authenticated user's claim binds only to their own row (no target-user input exists). The download and `connectedUser` requirements are UNCHANGED. Add a NEW requirement (or scenario) that the avatar upload UI appears in the profile tab of both the base and notifier user-settings windows via the shared `settings.xml` `profile-panel`, requiring no `user-settings.xml` edit.

**New tasks (high level, for sdd-tasks):** (1) add uploader criterion + confirm button to `settings.xml` `profile-panel`; (2) add `saveUserAvatar` maintain target + `Services.xml` binding; (3) add `UserSettingsService.saveUserAvatar(String)` with staged-`FileData` validation reusing existing constants, orphan-delete-on-rejection, and delegation to existing `upsertUserAvatar`; (4) DELETE `POST /avatar` handler + `uploadAvatar(MultipartFile)` + its 3 validators; (5) migrate/adjust the affected unit + security integration tests to the token/maintain flow; (6) add locale labels. No migration, no `connectedUser`, no `GET /avatar`, no security-config change.

### 8.9 Cross-module and DB-profile consequences

- **XML descriptor impact:** `settings.xml` (screen), `Maintain.xml` (new `saveUserAvatar` target), `Services.xml` (new binding), `Locale-*.xml` (labels). No `Queries.xml` change (reuses `getAvatarToken`).
- **Java touchpoints:** `UserSettingsService` (new `saveUserAvatar`, removed `uploadAvatar(MultipartFile)` + 3 validators), `AvatarController` (remove `POST` handler, keep `GET`).
- **Cross-module:** the notifier `user-settings.xml` override consumes the shared `settings.xml` `profile-panel`, so the avatar UI reaches the notifier window automatically — no `awe-notifier` edit. No REST/scheduler/developer/builder impact.
- **DB / dialects:** ZERO. No migration, no schema touch — this increment is UI + service transport only; the `AweUserSettings` table and all six-dialect DDL from Decisions 2/4 are untouched.

---

## Decisions summary

1. Bytes on the existing filesystem upload store (`awe.application.component.upload-file-path`, `avatar` folder); token = existing AWE `FileData` Base64 codec (`FileUtil`). No blob, no `ope` change.
2. `AweUserSettings(IdeUsrSet PK, Ope varchar(20) UNIQUE NOT NULL, AvatarImage varchar(4000) NULL)`; PK via `MAX+1` query (no sequence); DDL per dialect matching each `V1.0.2__favourites.sql` idiom.
3. No `HIS*` mirror (follows `AweUsrFav`; cosmetic non-audited data).
4. Next migration is **`V1.2.2`** (`AWE_V1.2.2__user_settings.sql`) across all six dialects — the proposal's `V1.1.0` premise was stale (real latest is `V1.2.1`).
5. Download is **self-only**, authenticated, server-resolved from session user; new `GET /avatar` endpoint (NOT the public `/file/download`); `401` unauth, `404` no-avatar; no client-supplied token.
6. Upload: max **2MB**, allow-list `image/png|jpeg|gif`, no server re-encode in v1, overwrite deletes old file. WebP intentionally excluded — stock JDK 17 ImageIO cannot decode it; supporting it would require a WebP ImageIO plugin (e.g. TwelveMonkeys) as a future enhancement.
7. `connectedUser.image` returns **`/avatar?v=<hash>`** (context-path relative) when a row exists, null otherwise — a CHANGING URL, not the earlier constant `/avatar`, so re-uploading an avatar produces a new URL and forces a browser refetch (the `filter` client action already reloads `connectedUser` after a save). `<hash>` is a non-cryptographic djb2/base36 hash of the stored `AvatarImage` token (never the raw token, which is a replayable capability reference), computed post-SQL by an AWE `eval="true"` computed (GraalVM JS via `ComputedColumnProcessor`) reading a reintroduced `<field alias="avatarToken" noprint="true">`, because a value-derived hash cannot be expressed as a portable SQL-level `CASE WHEN` across all six dialects without a dialect-specific SQL hash function. This deliberately reintroduces the general code-path shape (LEFT-JOINed `noprint` field read by an `eval` computed) that an earlier, never-fully-root-caused Oracle-only failure affected for this exact query (see Decision 7's ADDENDUM) — accepted as a known risk pending Oracle CI confirmation. `GET /avatar` ignores `?v` and is otherwise unchanged.
8. **Upload UI = uploader criterion + claim maintain (PIVOT).** The avatar uploader `<criteria component="uploader" destination="avatar">` + confirm button go in the SINGLE `settings.xml` `profile-panel` (appears in both base and notifier user-settings windows via the shared `<include>`, zero `user-settings.xml` edit). The uploader STAGES the file via the public `/file/upload`, which returns a `FileData` token as the criterion value; an authenticated, `session="user"`-bound **`saveUserAvatar`** maintain then VALIDATES the staged file (same 2MB + `png/jpeg/gif` + `ImageIO` magic-byte checks, now on the staged `FileData`; deletes the orphan on rejection) and delegates to the EXISTING `upsertUserAvatar(user, token)`. The direct multipart `POST /avatar` + `UserSettingsService.uploadAvatar(MultipartFile)` are **SUPERSEDED and DELETED** (the react uploader can't call them; a prior review flagged duplicate upload paths). `GET /avatar` download, the `AweUserSettings` table, `connectedUser.image`, and Decision 6's constraints are unchanged. No migration, no security-config change.
