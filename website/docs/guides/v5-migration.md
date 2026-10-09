---
id: v5-migration
title: AWE 5 Migration Guide
sidebar_label: AWE 5 Migration Guide
---

This guide lists what an application has to review when it moves from AWE 4 to AWE 5. It is a living page: every
change of the `develop` branch that can affect an existing application adds an entry here (see
[Maintaining this guide](#maintaining-this-guide)), so it grows until the final 5.0 release.

Entries describe only changes that are already merged. Planned changes are listed apart, in
[Coming in AWE 5](#coming-in-awe-5), and are not described in detail until they land.

## Who this guide is for

Developers of applications built on AWE 4 (the AngularJS client or the React client) and of products that extend AWE,
for example custom widgets or browser tests built on `awe-testing`.

**Prerequisites**

1. **Be on the latest 4.x release.** Upgrade to the last `4.x` version first and fix its deprecation warnings: a
   deprecation of the 4.x line that is removed in 5 is cheaper to solve on a line that still receives fixes.
2. Know which line you are on. AWE 4 is maintained on the `support/4.x` branch (security and critical fixes only) while
   `develop` builds AWE 5. Branches, tags, Docker tags and the support window are explained in
   [Release Lines and Support](release-lines-and-support.md#release-lines).
3. If your application used the React client in its `2.x` line, read the
   [React client upgrade guide](react-client-upgrade.md) as well: it has the step-by-step instructions that this page
   only summarizes.

## Compatibility matrix

Only what the repository declares today is listed. Items marked *planned* are not in `develop` yet.

| Component | AWE 4 (`support/4.x`) | AWE 5 (`develop`) | Notes |
|---|---|---|---|
| Java (build target) | 17 | 17 | The CI builds with JDK 21 but compiles for 17 (`java.version` in `awe-dependencies`). Java 21 as the baseline is *planned* ([#783](https://gitlab.com/aweframework/awe/-/issues/783)). |
| Spring Boot | 3.5.16 | 3.5.16 | Spring Boot 4 is *planned* ([#783](https://gitlab.com/aweframework/awe/-/issues/783)). |
| Node.js used by the Maven frontend build | v24.14.0 | v24.14.0 | `node.version` in `awe-dependencies`. Applications that build the React client with their own Node should use the same major version. |
| React client (`awe-react-client`) | `2.x`, own version line | Same version as the framework (`5.y.z`) | See [React client upgrade](react-client-upgrade.md). React 18.3.1. |
| Charts, React engine | Highcharts | Apache ECharts 6.1.0 | See [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Charts, AngularJS engine | Highcharts | Highcharts | ECharts for the AngularJS engine is *planned* ([#775](https://gitlab.com/aweframework/awe/-/issues/775)). |
| Browser tests (`awe-testing`) | Selenium | Selenium by default, Playwright as a pilot (`awe.test.tool`) | See [Selenium test guide](selenium-test-guide.md#automation-tool-awetesttool). |
| Browsers used to test the framework | Chrome, Firefox (Selenium) | Chromium and Firefox (Playwright), Chrome and Firefox (Selenium, scheduled) | A list of supported end-user browsers is not declared in the repository yet. |

## What changes

Each row says what you notice, what to do and where to read more. "MR" is a merge request of the
[AWE project](https://gitlab.com/aweframework/awe/-/merge_requests).

| Area | Symptom | What to do | More |
|---|---|---|---|
| React client package | The `awe-react-client` version of your `package.json` is `2.x`, or the `npm ci` of a 2.x project fails after the upgrade. | Set `awe-react-client` to the exact version of your AWE version (for example `5.0.0`), run `npm install` once and commit the new lock file. Do not mix a 5.x client with an AWE 4 server. | [React client upgrade](react-client-upgrade.md), [npm package](release-lines-and-support.md#npm-package) |
| Docker images | You need an image of the React test application. | `awe-boot-react` is built and published on `develop`, `master` and `support/*` with the same tags as `awe-boot`. There is no `latest` tag: pin a version. | [React test application image](release-lines-and-support.md#react-test-application-image) |
| Charts (React engine) | Charts are empty with an AWE 4 server, a series has a different color, 3D charts are flat, `.highcharts-*` CSS rules do nothing, or a custom component that imports Highcharts does not build. | Review the server log for `Highcharts chart-parameter` warnings, set colors and fonts in the XML, and declare Highcharts yourself if your own code imports it. | [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5), [React client upgrade](react-client-upgrade.md#charts-use-apache-echarts) (MR !854, !855, !856) |
| Browser tests | The compiler warns that `By` overloads and `getDriver()` are deprecated; tests depend on the order of the classes. | Move custom steps to the `Locator` overloads and to `getBrowser()`; set up the session in each test with `ensureLoggedIn`/`ensureModule`. To try Playwright, set `awe.test.tool=playwright`. Nothing is removed before 6.0. | [Selenium test guide](selenium-test-guide.md#api-compatibility-of-awe-testing), [independent test classes](selenium-test-guide.md#independent-test-classes), [Custom steps without Selenium types](selenium-test-guide.md#custom-steps-without-selenium-types) (MR !840, !841) |
| `jsoup` dependency | `org.jsoup:jsoup` is no longer on the classpath of `awe-model` and is no longer managed by the `awe-dependencies` BOM. | If your own code uses jsoup, declare the dependency and its version in your `pom.xml`. No action otherwise. See the note below the table. | MR !849 |
| Password expiration (`PwdExp`) | The application sets the `PwdExp` parameter. After the upgrade, users whose last password change is older than `PwdExp` days are rejected as expired (they used to get in), and users who changed it recently can log in again (they used to be rejected). | Nothing for correct data: the check is no longer inverted (see the note below the table). Review the users that never changed the password and the value of `PwdExp`. | [Password expiration](../security/authentication.md#password-expiration-local-login), issue #846 |
| Menu JSON | The menu payload is smaller. | Nothing, unless a custom client reads `elementList` from a menu `Option`: read `options` instead. See the note below the table. | MR !792 |
| SSO login of disabled or locked users | A user that is disabled or locked in AWE could log in through the identity provider; now the SSO login is refused with an error page. | Nothing, unless you relied on it: enable the user in AWE instead. | [Disabled and locked users](../security/authentication-sso.md#disabled-and-locked-users) |
| SSO handler beans | Your own code calls `AweWebSecurityConfig.authSuccessHandler()` (it now takes the `AweOauth2AuthenticationFailureHandler` bean as a parameter), or builds `AweOauth2AuthenticationSuccessHandler` with one argument (deprecated for removal). | Pass the failure handler: `new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler)`. Handler beans of your own are now honoured by the SSO login. | [Disabled and locked users](../security/authentication-sso.md#disabled-and-locked-users) |
| Scheduler database | Flyway fails with a checksum mismatch on `SCHEDULER_V1.0.5`, or a new database cannot be built from scratch. | Run the migration step described below. | MR !843 |

### jsoup in the input parameter sanitizer

`StringUtil.sanitizeInputParameter`, which `ScreenDataController` applies to the `optionId` path variable, used to pass
the escaped value through `Jsoup.clean(..., Safelist.basic())`. It now returns the value escaped by `escapeJson`,
`escapeJava` and `escapeHtml4` without that last step (MR !849). The escaping already turns `<` and `>` into entities,
so the difference is confined to the characters jsoup normalized after escaping: a double quote stays escaped as
`&quot;` instead of being turned back into `"`, and repeated or leading spaces are no longer collapsed or trimmed. Code
that compared the sanitized value with a fixed string needs the new value; the unit tests of `StringUtilTest` show the
exact output.

### Menu `Option` JSON

A menu `Option` used to serialize its children twice, under `elementList` and under `options`, which doubled the size of
the payload at each level of the menu tree. `elementList` is no longer part of the JSON of an `Option` (MR !792). The
clients of this repository read `options`, so nothing changes for them. The `elementList` of the screen tree
(components of a screen) is not affected.

### Password expiration (`PwdExp`)

When the `PwdExp` parameter is set, the local login evaluated the expiration the wrong way round: a password changed
within the last `PwdExp` days was rejected as expired and an older one was accepted. It is now evaluated correctly
(issue #846): the password is accepted until `PwdExp` days after the last change.

What to do:

- **Applications that do not set `PwdExp`:** nothing, passwords never expire.
- **Applications that set `PwdExp`:** users whose last change is older than `PwdExp` days can no longer log in until
  they change the password, and users with a recent change can log in. Check the value of `PwdExp` before the upgrade.
  To avoid locking many users out at once, review the password change dates before the upgrade and raise `PwdExp`
  (or deactivate it) for a transition period, until users have changed their passwords through the change-password
  option. A password set by an administrator in the users screen does not help here: it leaves the change date empty,
  which counts as expired while `PwdExp` is set (see below). The login screen has no guided password change yet.
- **Rolling back to AWE 4** after users changed their passwords brings the inverted check back: users with a recent
  change are rejected again. Deactivate `PwdExp` before rolling back.
- **Users that never changed the password** (empty date) are still rejected as expired when `PwdExp` is set, as before.
  This includes users whose password an administrator set in the users screen.
- **Test data:** an integration test that logs in with a user whose password date is old needs a `PwdExp` value larger than the age of that password (the AWE test apps use 36500 days).

### Scheduler migration `SCHEDULER_V1.0.5`

`SCHEDULER_V1.0.5__Unify_ftp_credentials_into_server.sql` carries the FTP credentials of each launcher over to the
server they point at. It no longer drops the `SrvUsr` and `SrvPwd` columns of `AweSchTskLch` and `HISAweSchTskLch`: they
stay as deprecated, nullable columns that the scheduler no longer reads or writes (MR !843). The script was changed
because applications whose own scripts still insert launchers with those columns could not build an empty database.

What to do:

- **Empty database or a database that has not applied V1.0.5 yet:** nothing, the scripts run in order.
- **Database that already applied the previous version of the script** (for example one built with 4.12.9 or 4.12.10):
  its checksum changed, so Flyway refuses to start. Run `flyway repair` once, or recreate the database. The columns that
  the old script dropped are not restored: if your own scripts insert them, add them back in your own migration.
- **Applications with their own scripts:** do not rely on the credential columns of the launcher tables, they are
  deprecated. Store the credentials in the server (`AweSchSrv`).

## Coming in AWE 5

These initiatives are open, labelled for the 5.0.0 milestone and expected to need action from applications. They are
listed so you can plan; each one adds its own entry above when it is merged.

| Issue | Title |
|---|---|
| [#755](https://gitlab.com/aweframework/awe/-/issues/755) | Replace vendored Bootstrap 3 with Tailwind and an AWE compatibility layer |
| [#775](https://gitlab.com/aweframework/awe/-/issues/775) | Migrate charts from Highcharts to Apache ECharts with server-side rendering |
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Replace angular-ui-grid with a modern grid component |
| [#778](https://gitlab.com/aweframework/awe/-/issues/778) | Enforce modern password hashing and a mandatory master key |
| [#783](https://gitlab.com/aweframework/awe/-/issues/783) | Upgrade the platform baseline to Java 21 and Spring Boot 4 |
| [#823](https://gitlab.com/aweframework/awe/-/issues/823) | Align builder action names and dead XSD values with the client contract |
| [#829](https://gitlab.com/aweframework/awe/-/issues/829) | Sanitize the HTML of grid cells in the React client (an inline `style` in a cell will be dropped; use a CSS class) |

## Upgrade checklist

Copy this list to the issue of your upgrade and tick it as you go.

```markdown
- [ ] The application runs on the latest 4.x release and builds without deprecation warnings
- [ ] `awe.version` (or the `awe-starter-parent` version) is set to the AWE 5 version
- [ ] `awe-react-client` is set to the exact same version, `npm install` was run and the lock file is committed
- [ ] Every screen with a chart was opened and the server log has no `Highcharts chart-parameter` warnings
- [ ] Custom code that imports Highcharts or reads `.highcharts-*` CSS was reviewed
- [ ] Custom code that uses jsoup declares its own dependency
- [ ] If the application sets `PwdExp`, the value and the users with an old or empty password change date were reviewed
- [ ] Custom clients do not read `elementList` from the menu JSON
- [ ] Flyway was repaired or the database recreated if `SCHEDULER_V1.0.5` had been applied before
- [ ] Browser tests compile; the `By` overloads and `getDriver()` were moved to `Locator` and `getBrowser()`
- [ ] The open items of "Coming in AWE 5" were reviewed against the application
```

## Maintaining this guide

This guide is only useful if it stays current, so it is part of the definition of done of a change:

- **Who:** the author of a merge request that can affect an existing application: a removed or renamed API, XML,
  property, database script or JSON field, a changed default or a dependency that applications receive from AWE.
- **When:** in the same merge request, tick the checkbox "If this MR has impacts on existing applications, I added an
  entry to the AWE 5 migration guide". The `has impacts` label marks these merge requests and issues.
- **How:** add one row to the table [What changes](#what-changes) with the symptom, what to do and a link to the
  detailed page, and a sub-section below it when the row is not enough. Describe only what is merged and was checked in
  the code; move the issue out of [Coming in AWE 5](#coming-in-awe-5) when it lands.
- **Where:** edit `website/docs/guides/v5-migration.md` on `develop`. Changes that only exist on the 4.x line belong in
  the documentation of `support/4.x` (see [Documentation per line](release-lines-and-support.md#documentation-per-line)).
