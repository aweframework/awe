# User Avatar Image Specification

## Purpose

Define the data storage, upload, download, and query-exposure behavior that lets each AWE user have a personal avatar image, without adding any column to the `ope` table, and without changing the existing icon/initial-letter fallback for users who have no avatar.

## Requirements

### Requirement: AweUserSettings satellite table stores the avatar token

The system MUST provide a per-user satellite table named `AweUserSettings`, keyed by the user's username (`Ope varchar(20)`, matching `ope.l1_nom`), modeled on the existing `AweUsrFav` satellite pattern. The table MUST hold a reference token for the user's avatar image and MUST NOT hold the binary image payload. The table MUST NOT require any change to the `ope` table schema.

The migration MUST be shipped as a new versioned Flyway-style file, replicated identically (module, column set, types, constraints) across all six supported dialect folders: `h2`, `hsqldb`, `mysql`, `oracle`, `postgresql`, `sqlserver`.

#### Scenario: AweUserSettings table exists after migration

- GIVEN a supported dialect's migration set is applied to a fresh database
- WHEN the new migration for `AweUserSettings` runs
- THEN the `AweUserSettings` table MUST exist
- AND it MUST contain a column keyed by username equivalent to `Ope varchar(20)`
- AND it MUST contain a column holding an avatar image reference token
- AND the `ope` table MUST remain structurally unchanged

#### Scenario: Migration applies cleanly across all supported dialects

- GIVEN each of the six supported dialect migration folders (`h2`, `hsqldb`, `mysql`, `oracle`, `postgresql`, `sqlserver`)
- WHEN the new `AweUserSettings` migration is applied on each dialect
- THEN the migration MUST succeed on every dialect
- AND the resulting table structure MUST be equivalent across all dialects

#### Scenario: A user has at most one avatar settings row

- GIVEN a user uploads an avatar for the first time
- WHEN the avatar token is persisted
- THEN the system MUST create exactly one `AweUserSettings` row for that user
- AND a subsequent upload by the same user MUST update the existing row rather than create a duplicate

### Requirement: Users can upload an avatar image

The system MUST let an authenticated user set their own avatar image through a **stage-then-claim** mechanism, not a direct multipart upload endpoint. The user MUST first stage the image file through the framework's generic public uploader (`/file/upload`), which stores the bytes and returns an opaque reference token identifying the staged file. The user's client MUST then submit that staged-file token to an authenticated, session-bound `saveUserAvatar` maintain action, which MUST validate the staged file's size and MIME type before accepting it, and MUST reject staged files that exceed the configured maximum size or that are not an allowed image type (`image/png`, `image/jpeg`, `image/gif`). On successful validation, the system MUST persist a reference token for the staged image in the `AweUserSettings` row for the claiming user (insert if absent, update if present). The system MUST NOT allow a user to set or overwrite another user's avatar through this mechanism.

#### Scenario: Successful avatar upload

- GIVEN an authenticated user has staged a valid image within the allowed size and MIME type via the framework uploader
- WHEN the user claims the staged file through the `saveUserAvatar` maintain
- THEN the system MUST validate the staged file and accept it
- AND the system MUST persist the resulting reference token in the `AweUserSettings` row keyed by that user's username
- AND the response MUST indicate success

#### Scenario: Upload rejected for oversized file

- GIVEN an authenticated user has staged an image that exceeds the configured maximum upload size via the framework uploader
- WHEN the user claims the staged file through the `saveUserAvatar` maintain
- THEN the system MUST reject the claim
- AND no `AweUserSettings` row MUST be created or modified for that user
- AND the response MUST indicate the failure reason

#### Scenario: Upload rejected for disallowed MIME type

- GIVEN an authenticated user has staged a file whose MIME type is not in the allowed image type list via the framework uploader
- WHEN the user claims the staged file through the `saveUserAvatar` maintain
- THEN the system MUST reject the claim
- AND no `AweUserSettings` row MUST be created or modified for that user
- AND the response MUST indicate the failure reason

#### Scenario: Re-uploading replaces the previous avatar

- GIVEN a user already has an avatar token stored in `AweUserSettings`
- WHEN that same user stages and successfully claims a new valid avatar image
- THEN the system MUST update the existing `AweUserSettings` row with the new reference token
- AND the previous avatar MUST no longer be served as that user's current avatar

#### Scenario: A staged file rejected at claim time leaves no orphan and no data change

- GIVEN an authenticated user has staged a file via the framework uploader that fails validation (oversized, disallowed MIME type, or content that does not decode as an allowed image) at claim time
- WHEN the `saveUserAvatar` maintain rejects the claim
- THEN the system MUST delete the staged file from storage
- AND no `AweUserSettings` row MUST be created or modified for that user
- AND the response MUST indicate the failure reason

#### Scenario: A claim binds only to the claiming user's own row

- GIVEN an authenticated user submits a staged-file token to the `saveUserAvatar` maintain
- WHEN the claim is processed
- THEN the system MUST resolve the target user solely from the authenticated session, never from any client-supplied user identifier
- AND the system MUST persist or update the avatar token only in the `AweUserSettings` row belonging to that session user
- AND the system MUST NOT provide any input through which the claiming user could set or overwrite another user's avatar

### Requirement: Avatar upload UI is available in the profile tab of user-settings windows

The system MUST provide the avatar upload UI (the staging uploader control and a confirm action that triggers the `saveUserAvatar` claim) in the profile tab of the user-settings screen, implemented as part of the shared `profile-panel` in `settings.xml`. Because both the base user-settings window and the notifier user-settings window include this shared `profile-panel`, the avatar upload UI MUST appear in both windows without any change to either window's own screen descriptor.

#### Scenario: Avatar upload UI appears in the base user-settings window

- GIVEN a user opens the base application's user-settings screen
- WHEN the profile tab is displayed
- THEN the avatar upload UI MUST be visible in that tab
- AND no edit to the base user-settings screen descriptor MUST have been required to display it

#### Scenario: Avatar upload UI appears in the notifier user-settings window

- GIVEN a user opens the notifier module's user-settings screen
- WHEN the profile tab is displayed
- THEN the avatar upload UI MUST be visible in that tab
- AND no edit to the notifier user-settings screen descriptor MUST have been required to display it

### Requirement: Secure download endpoint resolves a token to image bytes

The system MUST provide a download endpoint that accepts an avatar reference token and returns the corresponding image bytes. The endpoint MUST enforce authorization: a request MUST be rejected unless the requester is authenticated and permitted, under the applicable authorization rule, to view the avatar identified by that token. The endpoint MUST NOT allow an unauthenticated or unauthorized caller to retrieve image bytes for any token, including by guessing or enumerating tokens.

#### Scenario: Authorized download returns image bytes

- GIVEN an authenticated user requests the download endpoint with a token that is permitted for that user under the applicable authorization rule
- WHEN the request is processed
- THEN the system MUST return the image bytes associated with that token
- AND the response MUST use a content type consistent with the stored image

#### Scenario: Unauthenticated download is rejected

- GIVEN a caller with no valid authentication session or credentials
- WHEN the caller requests the download endpoint with any token
- THEN the system MUST reject the request
- AND the system MUST NOT return image bytes

#### Scenario: Authenticated but unauthorized download is rejected

- GIVEN an authenticated user requests the download endpoint with a token that does not belong to that user and is not otherwise permitted under the applicable authorization rule
- WHEN the request is processed
- THEN the system MUST reject the request
- AND the system MUST NOT return image bytes

#### Scenario: Unknown token is rejected without leaking existence information

- GIVEN a request to the download endpoint uses a token that does not correspond to any stored avatar
- WHEN the request is processed
- THEN the system MUST reject the request
- AND the response MUST NOT reveal whether the token was structurally valid but unassigned versus disallowed for the caller

### Requirement: connectedUser exposes the avatar token as image

The `connectedUser` query MUST be extended with a `LEFT JOIN` to `AweUserSettings` on the connected user's username, and MUST expose the avatar reference token as a new field aliased `image`. This MUST be additive: the existing `value`, `name`, and `label` fields returned by `connectedUser` MUST remain unchanged in name, meaning, and value for all users.

When the connected user has no corresponding `AweUserSettings` row, the `image` alias MUST be null (or empty, consistent with how the query engine represents an absent `LEFT JOIN` field), and this MUST NOT cause the query to fail or omit the user's row.

#### Scenario: connectedUser returns the avatar token when present

- GIVEN the connected user has an `AweUserSettings` row with a stored avatar reference token
- WHEN the `connectedUser` query is executed for that user
- THEN the result MUST include an `image` field populated with that user's avatar reference token
- AND the existing `value`, `name`, and `label` fields MUST be present and unchanged

#### Scenario: connectedUser returns null image when no avatar exists

- GIVEN the connected user has no `AweUserSettings` row
- WHEN the `connectedUser` query is executed for that user
- THEN the result MUST include the user's row with `image` as null (or empty)
- AND the existing `value`, `name`, and `label` fields MUST be present and unchanged
- AND the query MUST NOT fail or exclude the user's row because of the missing join match

### Requirement: Existing avatar fallback behavior is preserved

The change MUST NOT alter the `AweAvatar` frontend component or its existing fallback chain (image, then icon, then initial letter). Users who have never uploaded an avatar MUST continue to see the same icon or initial-letter avatar they see today, with no visual regression.

#### Scenario: User without an avatar sees no visual change

- GIVEN a user has never uploaded an avatar and has no `AweUserSettings` row
- WHEN that user's sidebar avatar is rendered
- THEN the rendered avatar MUST be the same icon or initial-letter fallback as before this change
- AND no frontend code change MUST be required to preserve this behavior

#### Scenario: User with an avatar sees their uploaded image with no frontend change

- GIVEN a user has successfully uploaded an avatar and `connectedUser` now returns a populated `image` value
- WHEN that user's sidebar avatar is rendered by the existing `AweAvatar` component
- THEN the component MUST render the uploaded avatar image using its existing `image` consumption logic
- AND this MUST require no change to the `AweAvatar` component itself

### Requirement: ope table and existing preferences remain unaffected

This change MUST NOT add, remove, or modify any column on the `ope` table, and MUST NOT alter any existing `ope`-backed preference (including `l1_lan` and `IdeThm`) or the behavior of code paths that read them (login, security filters, session, theme cascade).

#### Scenario: ope schema is unchanged after migration

- GIVEN the new `AweUserSettings` migration has been applied
- WHEN the `ope` table schema is inspected
- THEN it MUST be identical to its pre-change schema
- AND no new column MUST have been added for avatar or image data

#### Scenario: Existing ope-backed preference behavior is unaffected

- GIVEN a deployment that relies on existing `ope`-backed preferences such as language or theme
- WHEN this change is applied
- THEN login, security filter, session, and theme cascade behavior MUST continue to function exactly as before
- AND none of these paths MUST depend on or be affected by `AweUserSettings`
