# Change Proposal: Per-user avatar image

Tracking: GitLab issue #572, MR !665.

## Summary
Give each AWE user a personal avatar image without adding any column to the already-overloaded `ope` (users) table. Store the avatar as a reference token in a new per-user satellite table `AweUserSettings`, expose that token to the existing `connectedUser` query as alias `image` so the current `AweAvatar` frontend component renders it, and add secure endpoints to upload and download the avatar. The change is additive: users without an avatar keep the existing icon/initial-letter fallback.

## Intent
Today the sidebar avatar can only show a single static image configured via the `image="..."` XML attribute (the same logo for everyone) or fall back to an icon and finally to the user initial. There is no per-user avatar because the data layer never provides one. This change adds the missing data path so each user can have their own avatar, while respecting the established AWE constraint that `ope` must not accumulate more per-user preference columns.

## Problem statement
- The frontend is already capable: `AweAvatar` (`awe-react/awe-react-client/src/components/AweAvatar.jsx`, ~line 33) computes `computedImage = getContextPath() + (values?.[0]?.image ?? image_attribute)` and only needs an `image` value to render a per-user picture; it already falls back to icon and then to the initial letter.
- The data layer is missing: the sidebar avatar loads through `target-action="connectedUser"`. That query (`awe-framework/awe-generic-screens/src/main/resources/application/awe/global/Queries.xml`, id `connectedUser`, ~line 1808) selects only `value`, `name`, `label` from table `ope` and returns no `image`.
- The `ope` table (DDL in `AWE_V1.0.0__Init_awe_schema.sql`, ~line 48) has no image/avatar column, and stakeholders explicitly do not want to add one because `ope` is already overloaded and heavily read.

## Goals
- Provide a per-user avatar image with no schema change to `ope`.
- Store the avatar as a reference token (not the binary payload) in a new, generically named per-user satellite table `AweUserSettings`, keyed by user, following the established `AweUsrFav` satellite pattern.
- Expose the avatar token to `connectedUser` as alias `image` (for example via a `LEFT JOIN` to `AweUserSettings`) so the existing `AweAvatar` component renders it with no frontend change.
- Let a user upload an image to be used as their avatar.
- Provide a secure endpoint to download an avatar image from its token, with authorization enforced.
- Ship the migration across all six supported database dialects (`h2`, `hsqldb`, `mysql`, `oracle`, `postgresql`, `sqlserver`).

## Non-goals
- Do NOT migrate `l1_lan` (language), `IdeThm` (theme), or any other existing preference out of `ope`. Those values are read across roughly 40 files (login, security filters, session, theme cascade user -> profile -> module). Relocating them is a separate single-source-of-truth migration with backfill — not a duplicate-with-fallback layer — and is out of scope here.
- Do NOT store additional per-user settings in `AweUserSettings` in this change. The table is intentionally named generically so future per-user settings can live there later, but THIS change adds only the avatar image token.
- Do NOT store the binary image inside `AweUserSettings`; the table holds only a reference token.
- Do NOT change the `AweAvatar` component behavior or its existing fallback chain (image -> icon -> initial).
- Do NOT introduce a new avatar for machine/system principals or non-`ope` identities.

## Scope
In scope for later spec/design/apply phases:
- New per-user satellite table `AweUserSettings` holding the avatar image token, keyed by user (`Ope varchar(20)` = `l1_nom` username), modeled on `AweUsrFav`.
- Versioned Flyway-style migration for `AweUserSettings` replicated across all six dialect folders under `awe-framework/awe-starters/awe-spring-boot-starter/src/main/resources/db/migration/<dialect>/`. The current latest migration is `V1.1.0`, so this change introduces the next version.
- Whether an `HIS*` audit mirror table is required: the reference satellite `AweUsrFav` has no `HIS*` mirror, so an audit mirror is NOT assumed mandatory; the spec/design phase must confirm whether the avatar token needs auditing and add a `HIS*` mirror only if the pattern for this data requires it.
- Query wiring: extend `connectedUser` to surface the avatar token as alias `image` via a `LEFT JOIN` to `AweUserSettings`, so users without a row simply return no image and keep the existing fallback.
- Upload path: allow a user to upload an image as their avatar (maintain/service + storage of the token, with the binary stored wherever the design determines is appropriate — not in `ope`).
- Download path: a secure endpoint that resolves the avatar token to the image bytes, enforcing authorization so a user cannot read arbitrary avatars beyond what policy allows.

Out of scope:
- Any change to `ope` schema or to existing `ope`-backed preferences.
- Frontend component changes.
- General-purpose per-user settings storage beyond the avatar token.

## Proposed high-level approach
1. Create `AweUserSettings` as a per-user satellite table keyed by user, mirroring the `AweUsrFav` shape (identifier + `Ope varchar(20)` user key), with a column holding the avatar image reference token. Replicate the DDL across the six dialect migration folders as the next version after `V1.1.0`.
2. Add a maintain/service path so a user can upload an image; persist the image and store its reference token in `AweUserSettings` for that user (insert or update). Storage location for the binary is a design decision, but it must not be a column on `ope`.
3. Add a secure download endpoint that accepts an avatar token, enforces authorization, and returns the image bytes. `AweAvatar` builds the image URL from `getContextPath() + image`, so the token exposed as `image` should resolve through this endpoint.
4. Extend the `connectedUser` query with a `LEFT JOIN` to `AweUserSettings` and add `<field ... alias="image" />` so the current frontend receives the avatar with no client change. Absence of a row yields a null/empty `image`, preserving the existing icon/initial fallback with no ambiguity (the avatar never lives in `ope`).
5. Cover the new behavior with tests: migration applies cleanly on the CI-exercised dialects, `connectedUser` returns `image` when present and null when absent, upload stores a token, and download enforces authorization.

## Compatibility and backward-compatible behavior (framework-level)
- Purely additive: no existing column, query field, or endpoint is removed or renamed. `connectedUser` gains one new alias (`image`); its existing `value`, `name`, and `label` outputs are unchanged.
- Users with no `AweUserSettings` row behave exactly as today — the `LEFT JOIN` returns no image and `AweAvatar` falls back to icon, then initial. Existing deployments see no visual change until a user uploads an avatar.
- `ope` is untouched, so all ~40 files that read `ope`-backed preferences (login, security filters, session, theme cascade) are unaffected.
- The migration is a new versioned file per dialect; it does not alter existing migrations. Applications on any of the six supported dialects pick up the table on the next migration run. Environments using a dialect not present in the migration set are unaffected because no new column is added to existing tables.
- The `AweAvatar` component contract is unchanged; it already consumes an optional `image` value, so no frontend release is coupled to this change.

## Rollback plan
- Because the change is additive and gated by data presence, functional rollback is achievable by reverting the code that wires `image` into `connectedUser` and the upload/download endpoints; the `AweUserSettings` table can remain in place harmlessly (unused) to avoid a destructive down-migration.
- If full removal is required, drop `AweUserSettings` (and its `HIS*` mirror if one is added) via a new versioned migration per dialect rather than editing the original migration; never mutate an already-released migration file.
- Reverting only the `connectedUser` join restores the prior avatar behavior immediately (icon/initial fallback) even if the table and upload endpoint remain.
- No `ope` rollback is needed since `ope` is never modified.

## Risks
- Token-to-binary resolution and storage location are unspecified here; the design must choose where avatar bytes live (filesystem, blob table, existing document store) without touching `ope`, and define the token format.
- Download authorization must be explicit: who may fetch which user's avatar. An unguarded endpoint would leak images by token guessing.
- Multi-dialect DDL drift: the six dialect files must stay consistent (types, identifier lengths, defaults); a mismatch could pass on one dialect and fail on another in CI.
- `HIS*` audit mirror decision is deferred; if downstream conventions expect audit mirrors for user-owned data, the spec must add one to avoid an inconsistent schema.
- Upload input handling (content type, size limits, allowed formats) needs validation to avoid storing unsafe or oversized payloads.

## Open questions for spec/design
- Where are the avatar image bytes stored, and what exactly does the reference token encode?
- Is an `HIS*` audit mirror for `AweUserSettings` required, or does the `AweUsrFav` precedent (no mirror) apply?
- What is the exact next migration version (`V1.1.1` vs `V1.2.0`) and the precise column set/types for `AweUserSettings` across the six dialects?
- What authorization rule governs the download endpoint (self-only, same-profile, admin, public)?
- What upload constraints apply (max size, allowed MIME types, image dimensions/normalization)?
- Does the download route need to align with `getContextPath()`-relative URLs so `AweAvatar`'s `computedImage` resolves correctly?

## Success criteria
- A user can upload an image and subsequently see it as their avatar in the sidebar with no frontend code change.
- `connectedUser` returns an `image` alias populated from `AweUserSettings` when a row exists and null/empty otherwise.
- Users without an avatar keep the existing icon/initial fallback and see no regression.
- The avatar download endpoint enforces authorization and rejects unauthorized access.
- `ope` schema is unchanged and no existing `ope`-backed behavior regresses.
- The new migration applies cleanly across the supported dialects exercised in CI.
