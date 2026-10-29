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
| Java (build target and runtime) | 17 | **21** | `java.version` in `awe-dependencies` and in the archetypes. A JDK 21 is required to build and to run. See [Java 21 and Spring Boot 4.1](#java-21-and-spring-boot-41). |
| Spring Boot | 3.5.16 | **4.1.1** | Spring Framework 7.0, Spring Security 7.0, JUnit 6, Flyway 11. |
| Spring Cloud | 2025.0.3 | **2025.1.3** | |
| Jackson | 2.x (`com.fasterxml.jackson`) | **3.2.1** (`tools.jackson`) | Annotations stay in `com.fasterxml.jackson.annotation`. See [Jackson 3](#jackson-3). |
| java-jwt / springdoc-openapi | 3.18.2 / 2.8.13 | 4.5.0 / 3.0.3 | |
| Node.js used by the Maven frontend build | v24.14.0 | v24.14.0 | `node.version` in `awe-dependencies`. Applications that build the React client with their own Node should use the same major version. |
| React client (`awe-react-client`) | `2.x`, own version line | Same version as the framework (`5.y.z`) | See [React client upgrade](react-client-upgrade.md). React 19.3.1 (AWE 4: React 18). |
| Charts, React engine | Highcharts | Apache ECharts 6.1.0 | See [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Charts, AngularJS engine | Highcharts | Apache ECharts 6.1.0 | Same option builder as the React engine. See [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5). |
| Server chart rendering (`ChartService`) | Highcharts export server | AWE chart server (Apache ECharts, Node) | See [Chart server](../api/chart.md#chart-server). |
| Browser tests (`awe-testing`) | Selenium | Selenium by default, Playwright as a pilot (`awe.test.tool`) | See [Selenium test guide](selenium-test-guide.md#automation-tool-awetesttool). |
| Browsers used to test the framework | Chrome, Firefox (Selenium) | Chromium and Firefox (Playwright), Chrome and Firefox (Selenium, scheduled) | A list of supported end-user browsers is not declared in the repository yet. |

## What changes

Each row says what you notice, what to do and where to read more. "MR" is a merge request of the
[AWE project](https://gitlab.com/aweframework/awe/-/merge_requests).

| Area | Symptom | What to do | More |
|---|---|---|---|
| React client package | The `awe-react-client` version of your `package.json` is `2.x`, or the `npm ci` of a 2.x project fails after the upgrade. | Set `awe-react-client` to the exact version of your AWE version (for example `5.0.0`), run `npm install` once and commit the new lock file. Do not mix a 5.x client with an AWE 4 server. | [React client upgrade](react-client-upgrade.md), [npm package](release-lines-and-support.md#npm-package) |
| React 19 | A custom React widget or component of your application warns or fails with React 19 (for example it uses `defaultProps` on a function component, `findDOMNode`, string refs or `react-dom/test-utils`), or it imported `react-helmet`, which no longer comes with the client. | Update your own React code and its libraries to versions that support React 19, and declare `react-helmet-async` (or set the title in an effect) if your code used `react-helmet`. Keep `react` and `react-dom` on the version the client declares. | [React 19 upgrade guide](https://react.dev/blog/2024/04/25/react-19-upgrade-guide) (#883) |
| Docker images | You need an image of the React test application. | `awe-boot-react` is built and published on `develop`, `master` and `support/*` with the same tags as `awe-boot`. There is no `latest` tag: pin a version. | [React test application image](release-lines-and-support.md#react-test-application-image) |
| Charts (React engine) | Charts are empty with an AWE 4 server, a series has a different color, 3D charts are flat, `.highcharts-*` CSS rules do nothing, or a custom component that imports Highcharts does not build. | Review the server log for `Highcharts chart-parameter` warnings, set colors and fonts in the XML, and declare Highcharts yourself if your own code imports it. | [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5), [React client upgrade](react-client-upgrade.md#charts-use-apache-echarts) (MR !854, !855, !856) |
| Charts (AngularJS engine) | 3D charts are flat, a series has a different color, a stock chart has a slider instead of the Highstock range selector and scrollbar, there is no Reset zoom button, Highcharts options of `chart-parameter` or `chart-options` do nothing, `.highcharts-*` CSS rules do nothing, Highcharts scripts added in `scripts.xml` (themes, plugins) fail, or browser tests that look for `.highcharts-*` elements fail. | Review the server log for `Highcharts chart-parameter` warnings, set colors and fonts in the XML, remove Highcharts scripts and `.highcharts-*` rules, and wait for charts with the `getChart`/`getChartZoomed` instructions of `awe-testing`. | [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5), [Changes from AWE 4](../api/chart.md#changes-from-awe-4-highcharts) (issue #795) |
| Chart `theme` attribute | A chart with `theme="..."` loads, but the colors of its Highcharts theme are gone; code that calls `setTheme` of `ChartBuilder` or `Chart` does not compile. | Remove the attribute and the `setTheme` calls. The charts follow the light or dark theme of the application; set the colors with the `color` of the series or the `colors` parameter, and the fonts with `chart-parameter`. | [Styling a chart](../api/chart.md#styling-a-chart) (issue #795) |
| Server chart rendering (`ChartService`) | `renderChart` fails: the chart is no longer sent to a Highcharts export server, and the default URL (`https://export.highcharts.com`) is gone. | Run the AWE chart server (image `registry.gitlab.com/aweframework/docker-tools/awe-chart-server:latest`) and set `awe.application.chart-server-url`. `awe.application.highcharts-server-url` is removed and no longer read: rename it to `awe.application.chart-server-url`. | [Chart server](../api/chart.md#chart-server) (issue #795) |
| Browser tests | The compiler warns that `By` overloads and `getDriver()` are deprecated; tests depend on the order of the classes. | Move custom steps to the `Locator` overloads and to `getBrowser()`; set up the session in each test with `ensureLoggedIn`/`ensureModule`. To try Playwright, set `awe.test.tool=playwright`. Nothing is removed before 6.0. | [Selenium test guide](selenium-test-guide.md#api-compatibility-of-awe-testing), [independent test classes](selenium-test-guide.md#independent-test-classes), [Custom steps without Selenium types](selenium-test-guide.md#custom-steps-without-selenium-types) (MR !840, !841) |
| `jsoup` dependency | `org.jsoup:jsoup` is no longer on the classpath of `awe-model` and is no longer managed by the `awe-dependencies` BOM. | If your own code uses jsoup, declare the dependency and its version in your `pom.xml`. No action otherwise. See the note below the table. | MR !849 |
| Password expiration (`PwdExp`) | The application sets the `PwdExp` parameter. After the upgrade, users whose last password change is older than `PwdExp` days are told that the password has expired (they used to get in) and have to set a new one, and users who changed it recently can log in again (they used to be rejected). | Nothing for correct data: the check is no longer inverted and the login guides the user to change the password (see the note below the table). Review the users that never changed the password and the value of `PwdExp`. | [Password expiration](../security/authentication.md#password-expiration-local-login), issue #846 |
| Menu JSON | The menu payload is smaller. | Nothing, unless a custom client reads `elementList` from a menu `Option`: read `options` instead. See the note below the table. | MR !792 |
| Screen components JSON | The `screen-data` answer of screens with nested containers is much smaller. | Nothing, unless a custom client reads `elementList` from the `controller` of an entry of `components`: read the children from `structure` instead. See the note below the table. | Issue #907 |
| SSO login of disabled or locked users | A user that is disabled or locked in AWE could log in through the identity provider; now the SSO login is refused with an error page. | Nothing, unless you relied on it: enable the user in AWE instead. | [Disabled and locked users](../security/authentication-sso.md#disabled-and-locked-users) |
| SSO users with several roles | A user whose token carries several roles with the `filter-authority-prefix` may get a different profile than before: AWE 5 uses the first name in alphabetical order that exists as an AWE profile (AWE 4 took the first role of an unordered set). | Send one prefixed role per user, or check that the alphabetical choice is the expected one. Roles can now be read from other claims with `awe.security.sso.roles-claims`. | [Several roles: which profile wins](../security/authentication-sso.md#several-roles-which-profile-wins) |
| SSO handler beans | Your own code calls `AweWebSecurityConfig.authSuccessHandler()` (it now takes the `AweOauth2AuthenticationFailureHandler` bean as a parameter), or builds `AweOauth2AuthenticationSuccessHandler` with one argument (deprecated for removal). | Pass the failure handler: `new AweOauth2AuthenticationSuccessHandler(accessService, failureHandler)`. Handler beans of your own are now honoured by the SSO login. | [Disabled and locked users](../security/authentication-sso.md#disabled-and-locked-users) |
| Scheduler database | Flyway fails with a checksum mismatch on `SCHEDULER_V1.0.5`, or a new database cannot be built from scratch. | Run the migration step described below. | MR !843 |
| Default folders (developer module, scheduler logs) | The developer module path and the scheduler execution logs are now in a folder without the stray `}` that the default value used to end with (`<user.home>/awe-developer` instead of `<user.home>/awe-developer}`, `<logging.file.path or java.io.tmpdir>/scheduler` instead of `.../scheduler}`). Only applies if you did not set `awe.developer.path` or `awe.scheduler.execution-log-path`. | Move your existing files from the old folder to the new one if you need them, or set the property to the old path. | MR !889 |
| JDK and Spring Boot (#783) | The application does not start or build on JDK 17; Maven reports unknown starters such as `spring-boot-starter-web` or `spring-boot-starter-aop`; `@MockBean` does not compile. | Build and run on JDK 21, set `java.version` to 21 and move to the Spring Boot 4.1 starters and test annotations. | [Java 21 and Spring Boot 4.1](#java-21-and-spring-boot-41) |
| Jackson 3 (#783) | Your code does not compile because an AWE method now takes or returns `tools.jackson.databind.node.ObjectNode` (or `JsonNode`, `ArrayNode`, `ObjectMapper`); `asText()` or `asInt()` throws `JsonNodeException`. | Change the imports to `tools.jackson.*`, use the default-value overloads (`asString("")`, `asInt(0)`...) and replace `JsonProcessingException` with `JacksonException`. | [Jackson 3](#jackson-3), [Jackson 3 public API](#jackson-3-public-api) |
| JSON requests with trailing content (#783) | A client gets HTTP 400 for a body such as `{...},` or `{...}}` that AWE 4 accepted. | Fix the client: AWE 5 rejects content after the root JSON value. The rest of the JSON format is the AWE 4 one. | [JSON contract](#json-contract) |
| Jackson 3 clients of AWE (#783) | A client built on Spring 7 / Jackson 3 defaults fails with `Cannot deserialize value of type AnswerType from String "OK"`. | Configure the client with the AWE mapper: `MessageConverterUtil.withJsonMapper(restTemplate, DataListUtil.getMapper())`. | [JSON contract](#json-contract) |
| Logback configuration (#783) | The application fails at startup with `Logback configuration error detected` after the upgrade. | Remove the `applicationName` conversion rule from your own `logback-spring.xml`; use `%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}`. | [Logging, database and web](#logging-database-and-web) |
| Login redirects (#783) | An unauthenticated browser request is redirected to `Location: /` instead of an absolute URL. | Nothing, unless something parses the absolute URL: accept a relative one. Custom `AuthorizationManager` implementations must take `Supplier<? extends Authentication>`. | [Spring Security 7](#spring-security-7) |
| Build plugins (#783) | Your `pom.xml` declares `lombok-maven-plugin` through the AWE plugin management and the build cannot resolve its version; `selenium-server` has no managed version. | Declare `maven-antrun-plugin` for delombok (AWE's `delombok` execution) or drop it; declare the `selenium-server` version yourself. | [Java 21 and Spring Boot 4.1](#java-21-and-spring-boot-41) |
| Maintain transactions (#781) | A maintain that fails now leaves nothing behind: maintains launched from Java services and `retrieve-data` join the outer maintain, a step that returns `ERROR` rolls back, and a maintain launched inside an application `@Transactional` joins that transaction. A test that reads with its own JDBC connection inside a `@Transactional` test does not see the rows (or waits on a lock). | Nothing for correct maintains. If the application relies on partial commits, set `awe.maintain.transaction.enabled=false` or move that work to its own maintain. In tests, read through the transaction (`JdbcTemplate`, `TransactionAwareDataSourceProxy`). | [Maintain transactions](#maintain-transactions), [Transactions](../api/maintain-definition.md#transactions) |
| Date utilities (#782) | The compiler warns that `DateUtil` methods that take or return `java.util.Date` or `Calendar` are deprecated. | Nothing is required: they keep their behavior and are not removed before 6.0. Move to the `java.time` methods of `com.almis.awe.model.util.data.date`. | [Date utilities](#date-utilities) |
| MenuService (#782) | Only for projects that extend `MenuService`: a method you override is no longer called by the other `MenuService` methods (for example, overriding `getOptionByName` no longer changes `getOptionScreen`). The compiler warns that the seven-argument `MenuService` constructor is deprecated. | Nothing if you only call `MenuService`. If you extend it, move the customization to your own bean of the collaborator that owns the method. | [MenuService](#menuservice) |
| Spring Session with Redis (#861) | With several instances, users are logged out at random and screens fail after login (for example `Index 0 out of bounds for length 0`): the session cookie is `JSESSIONID` instead of `SESSION`, because the sessions stay in each server. | Replace `spring-session-data-redis` with `spring-boot-starter-session-data-redis`, rename `spring.session.redis.*` to `spring.session.data.redis.*` and update the Redis classes in `spring.autoconfigure.exclude`. | [Java 21 and Spring Boot 4.1](#java-21-and-spring-boot-41) |
| `TemplateService.getTagList(Screen, String)` (#861) | Code that calls this method directly does not compile: it now declares `throws AWENotFoundException`. A tag list missing from the screen raises this error instead of an `IndexOutOfBoundsException`. | Catch or propagate `AWENotFoundException` (or `AWException`). | issue #861 |
| SQLBuilder (#782) | Only for projects that extend `SQLQueryBuilder` or `SQLMaintainBuilder` and call removed private methods of `SQLBuilder` by reflection. The protected methods keep their signatures and still call each other in the same order. | Nothing if you only override protected methods. Replace reflective calls to the removed private methods with the protected methods. | [SQLBuilder](#sqlbuilder) |
| Screen contract actions (#823) | Java code that uses the `awe-builder` `Action` enum now sends `check-records-generated`, `select-first-row` and `select-last-row` (they used to send names that no engine handled, so the action did nothing). The AngularJS engine now runs `toggle-class`, and the React engine shows `target-message` as a popover on the target component, as AngularJS does. The compiler warns that `Action.FORWARD`, `GET_LOCALS`, `CHECK_LEAF_SELECTED` and `CHECK_PARENT_SELECTED` are deprecated, and the IDE marks some XML values as deprecated. | Check the screens where these actions now run. Remove the deprecated values: they do nothing and are removed in 6.0. | [Screen contract values](#screen-contract-values) |
| Virtual threads (#783, new) | Nothing changes unless you set `spring.threads.virtual.enabled=true`. | Optional: set the property to run Spring Boot and the AWE task executors on virtual threads. | [Virtual threads](../deployment.md#virtual-threads) |
| Client dependencies (`moment`, `lodash`, build tooling) | The AngularJS client no longer ships `moment` (it uses `day.js`), and the React client no longer lists `@redux-devtools/extension`, `redux-thunk`, `react-transition-group`, `deep-freeze`, `autoprefixer`, `postcss-loader` or `@types/*`. Your own code imports one of them and the build fails with `Cannot find module`. | Declare the package you import in your own `package.json`: AWE only installed these as an implementation detail and does not promise them to applications. `lodash` stays, but treat it the same way. See the note below the table. | [Client dependencies](#client-dependencies), issue #769 |
| Themes and dark mode (AngularJS client) | The screens take the AWE 5 look of the theme: a theme name (`default`, `sunset`...) or a database theme is a preset of a primary, a surface and a mode, rendered on `<html>` (`dark` / `light` class, `data-awe-primary`, `data-awe-surface`, `data-awe-density`). The classic LESS themes, the `.theme-<name>` rules and `/css/themeVariables.css` are gone (the server no longer serves it: the request answers 404): CSS of the application that sets the AWE 4 theme variables (`--primary-color`, `--panel-background-color`...) or styles `.theme-<name>` changes nothing, and the *Customize theme* screen is gone (the `Customize` button of the *Themes* screen, the `customize-theme` menu option, the `customizeTheme` maintain, the `themeConfiguration` and `nextIdForThemeColors` queries and the locale keys only that screen used, such as `SCREEN_TITLE_THM_CUSTOM`): the colours it saved (table `AweThmCol`, kept unused) are not applied. Users that never chose a mode get the one of their operating system. | Read the `--awe-*` tokens in your CSS instead of the theme variables, and style the mode with `html.dark` if you must. Give a theme its colours with its primary, surface (palette or hex) and mode in the new and update screens of *Themes*, or with the `awe.application.theme-*` properties. Remove the `customize-theme` option (screen `customize-themes`) from the `private.xml` menu of your application. To ignore the operating system set `awe.application.theme-mode=light` (or `dark`); `@awe-follow-os-color-scheme` no longer does anything and the property `awe.application.theme-follow-os-color-scheme` is removed: delete it from your configuration. | [Design tokens and dark mode](design-tokens-guide.md), [Theme configuration](../design-system/for-developers.md) (issues #770, #885) |
| Loading indicators (both clients) | A grid or tree grid without `icon-loading` shows placeholder rows shaped like its columns while its rows load, instead of the spinner. A screen shows a placeholder layout while it opens; the first screen of the page (opening the application or reloading it) loads under the splash animation instead. Criteria show a placeholder with their shape (label and field, textarea rows, checkbox or radio, picklist options), buttons one as wide as their label, and charts their area with a hint of the chart type. Skeleton visuals appear only after 150 ms, so fast loads show nothing, and the shimmer stops with `prefers-reduced-motion: reduce`. A grid reload with rows on screen shows the placeholder rows too; with `icon-loading="skeleton-dim"` it keeps the rows dimmed instead of replacing them. Criteria, buttons and charts with a custom `icon-loading` keep their icon. The `data-testid` values of the loaders do not change, so browser test waits keep working. | Nothing. To keep the spinner in one grid, set `icon-loading="spinner"` on it; to keep it in every grid, set `awe.application.component.grid-icon-loading=spinner`. To dim the rows on a reload instead of showing the placeholders, use `skeleton-dim` in the attribute or the property. | [Grid attributes](../api/grids.md#general-attributes), [Properties](../properties#awe.application.component.grid-icon-loading) (issues #774, #856, #864) |
| Security audit (#779, new) | Flyway applies `AWE_V1.3.0__Security_audit_events.sql` and creates the `AweSecAudEvt` table; security events are recorded in it and in the `awe.security.audit` logger; the **Security audit** option appears in the security menu. | Nothing. To switch the audit off set `awe.security.audit.enabled=false`; restrict the new option to the profiles that may see it. See the note below the table. | [Security audit](#security-audit), [Security audit page](../security/security-audit.md) |
| Password hashing (#778) | New passwords are stored as BCrypt hashes (`{bcrypt}$2a$...`, 68 characters) instead of RIPEMD-160 hashes (40 characters), and each user is rehashed at the next successful login. Flyway applies `AWE_V1.4.0__Password_hash_length.sql`. Own XML that compares `l1_pas` with `STRING_HASH_RIPEMD160` stops matching the rehashed users, and external systems that read `l1_pas` find a format they do not know. | Widen `l1_pas` before the upgrade if you manage your own schema, write passwords with `STRING_HASH_PASSWORD` and check them in Java, and keep `awe.security.password-encoder=ripemd160` while an external system still reads `l1_pas`. Back up `ope`: rehashed users cannot log in to AWE 4 again. | [Password hashing and master key](#password-hashing-and-master-key) |
| Master key (#778) | The application does not start: `awe.security.master-key is required: it encrypts stored data ...`. | Set `awe.security.master-key`. An application that never set it must set the former AWE 4 default value to keep decrypting its stored data, and then plan a rotation. | [Master key](#master-key) |
| Grid toolbar | Every grid shows a toolbar with a *Columns* button and a *Maximize* button, and a new table `AweUsrGrdCnf` is created by the Flyway migration `AWE_V1.3.1` (it stores the column layout of each user). The Columns open in a popover anchored to the button (`columns-panel="popover"`, the default), with *Restore default*, *Cancel* and *Save*; there is no *Show all*. *Save* and *Restore default* show no message when they work, and *Save* closes the Columns. In React, the group headers of a grid are now computed from the visible columns of each group. | Nothing for the descriptors, which keep working. Set `show-toolbar="false"` on the grids that must not show the toolbar. To keep the modal dialog, set `columns-panel="dialog"` on the grid or `awe.application.component.grid-columns-panel=dialog`. Browser tests: replace `clickGridColumnsShowAll` (deprecated) with `showGridColumn` for each hidden column, and drop the success message checks after `clickGridColumnsSave` / `clickGridColumnsReset` (the steps wait for the save). Applications that do not use the Flyway migrations of AWE have to create `AweUsrGrdCnf` themselves (see the script of their database in `db/migration`). | [Toolbar](../api/grids.md#toolbar), issue #813 |
| Icons (both clients) | Icons look different: every Font Awesome name (`icon="name"`) and `mdi:` name of the equivalence table is drawn as a Lucide line icon, an inline `svg` that uses the sprite `icons/lucide-sprite.svg`, instead of the `fa fa-<name>` or Material Icons element (and, in React, instead of the PrimeIcons of the AWE components). Names without equivalence (brand icons such as `rebel`) keep their font. CSS of the application that targets the font glyph (`.fa-<name>:before`, `content` or `font-size` on `.fa`, `.pi-*`) no longer applies to the mapped icons. The element keeps the XML name in `data-icon`, so browser tests that locate icons keep working. | Nothing for the descriptors. Move the CSS that styled the glyph to the `svg.awe-icon` (it takes the text colour, `currentColor`, and the `--awe-size-icon` tokens). For new screens use `icon="lucide:<name>"` with any Lucide name. | [Icons](../api/icons.md), [Equivalence table](../api/icons.md#equivalence-table) (issue #885) |
| Icon sprite and security configuration | The sign-in page (and every public screen) shows no icons: `GET /icons/lucide-sprite.svg` answers `401` because the application builds its own security configuration with its own list of public web resources. | Add `/icons/**` to the public web resources of your security configuration, next to `/css/**`, `/js/**` or `/fonts/**`. The AWE security configuration already allows it. | issue #885 |
| Messages (AngularJS client) | The alerts of the message zone are toasts at the top right, below the header, tinted with the colour of their type, as in the React client. | Nothing. Review CSS of the application that positions or styles the alerts of the message zone. | [Overlays](../design-system/components.md#overlays) (issue #885) |
| Dialogs and confirms (AngularJS client) | When a confirm or a screen dialog opens, the keyboard focus moves into it (its first control) and goes back to the control that opened it when it closes. Escape now closes a confirm (as its cancel button: the action is rejected) and a dialog with a title (as its close button); a dialog without a title still does not close with Escape. A browser test that pressed Escape expecting nothing to happen, or that types on the page while a dialog is open, behaves differently. | Review browser tests that press Escape with a confirm or a dialog open, or that rely on the focus staying on the page. | issue #885 |
| Grid row height (AngularJS client) | Grid rows are 43 px high in the comfortable density and 32 px in the compact one (they were 27 px) and change when the user changes the density. A `row-height` set in the XML is kept, and is now honoured on tree grids too (they ignored it and used 27 px). | Nothing. Set `row-height` on a grid that must keep a fixed height, and check the tree grids that set it. | [Grid attributes](../api/grids.md#general-attributes) (issue #885) |
| Sign-in, change password and 2FA screens (both clients) | The sign-in, the change and expired password and the 2FA screens take the AWE 5 login layout: a form column next to a brand panel (hidden on narrow screens), large controls and full-width buttons. In AngularJS the pixel-admin `pages/signin.less` is no longer imported, so CSS of the application that overrides its rules may no longer apply. In AngularJS `awe.application.paths.image-startup-background` and `awe.application.paths.image-startup-logo` still style the change password and 2FA screens; for the sign-in page see the next row. | Review the CSS of the application that styles these screens, in light and dark. | [Login page](../design-system/layout-and-shell.md#login-page) (issue #885) |
| Configurable sign-in page (both clients, new) | The sign-in page is branded with the `awe.application.login.*` properties: layout (`split`, `form-only` or `card`), logo and dark mode logo, background image, form title and subtitle, caption over the image, footer text and "Powered by AWE 5". `awe.application.paths.image-startup-logo` and `awe.application.paths.image-startup-background` are deprecated for the sign-in page: a value changed from its default is still used when `awe.application.login.logo` or `awe.application.login.background` is not set. The change password and 2FA screens of AngularJS keep using them. | Move a customized `image-startup-logo` and `image-startup-background` to `awe.application.login.logo` and `awe.application.login.background`, with a URL relative to the application root (without the leading `../`). Optional: set the rest of the `awe.application.login.*` properties. | [Login page](../design-system/layout-and-shell.md#login-page), [AWE Base Properties](../properties.md#awe-base-properties) |
| Resizable (both clients) | Dragging the grabber of a `<resizable>` works as a splitter: it follows the pointer and only the element next to the grabber gives or takes the size, so the other panes of the parent no longer shrink or grow with it (they used to share the change). After a drag that element keeps its size along the axis, and the other expanding elements take the changes of the window size. In React, a resizable no longer splits its children in panels (#847): children that relied on a splitter between them are stacked. | Nothing for screens with the AngularJS structure (a resizable around one pane, its neighbour next to it). In React screens, put the second pane next to the resizable instead of inside it. | [Resizable](../api/resizable.md#how-a-drag-shares-the-space) (issue #892) |
| Dark mode (React client) | The `layout-dark` and `layout-light` classes are no longer set on `<body>`: the mode is the `dark` or `light` class of `<html>`, next to `data-awe-primary`, `data-awe-surface` and `data-awe-density`. CSS of the application on `.layout-dark` no longer applies. The theme stylesheet `<link>` and the 18 compiled PrimeReact themes are gone: PrimeReact reads the AWE 5 tokens. | Style the mode with `html.dark`, or better with the `--awe-*` tokens, which already change with the mode. Remove the `add-class`/`remove-class` dependencies on `layout-dark` from your own copy of `info-buttons.xml`. | [Design tokens and dark mode](design-tokens-guide.md), [Theme configuration](../design-system/for-developers.md) (issue #885) |
| AWE 5 shell (both clients) | The vertical home screens show a footer under the side menu ("Powered by AWE 5" and a button that collapses the menu) and the menu button of the header has the `panel-left` icon. The collapsed menu hides the option search and opens the options of a first level option as a flyout when the pointer is on it. In React, a separator option without label is drawn as a line in the vertical menu (it was hidden), the user menu shows the `name` of the `connectedUser` value instead of its label ("Manager" instead of "Manager (test)") with the profile below it, outlined buttons are neutral (they used the primary colour) and select criteria are as high as text criteria. The `connectedUser` query returns a new `description` field with the profile name. | Nothing for the descriptors. Browser tests of React applications that check the logged user with `checkLogin`/`ensureLoggedIn` must expect the name without the user code. Applications with their own copy of `home-sidebar.xml` or `home_vertical.xml` can add the footer as described in the layout guide. | [Side menu](../design-system/layout-and-shell.md#side-menu), [Avatar](../api/info.md#avatar-attributes) (issue #900) |

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

### Screen components JSON

Each entry of `components` in the `screen-data` answer carried a `controller` with the whole `elementList` subtree of
the component, so a definition was sent once in `structure` and once more for each component that contained it. On
large screens most of the answer was those copies. `elementList` is no longer part of the JSON of a component
`controller` (issue #907); `structure` still has it, and the fields computed from the children (`columnModel`,
`buttonModel`, `headerModel` of grids, `children` of avatars and info dropdowns) are still sent. The clients of this
repository build the component tree from `structure`, so nothing changes for them.

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
  which counts as expired while `PwdExp` is set (see below). Users do not need an administrator: the login guides them
  to set a new password (see below).
- **Rolling back to AWE 4** after users changed their passwords brings the inverted check back: users with a recent
  change are rejected again. Deactivate `PwdExp` before rolling back.
- **Users that never changed the password** (empty date) are still expired when `PwdExp` is set, as before. This
  includes users whose password an administrator set in the users screen: they have to change it at the first login
  (documented decision, issue #849).
- **Expired passwords can be changed from the login** (issue #849). The user gets a localized message (the message used
  to be Spring's English text under the invalid credentials title) and is taken to the public
  `change-expired-password` screen in both web engines, then signs in with the new password. The user never gets a
  session with the expired password. `POST /api/authenticate` answers `401` with a localized message and the code
  `PASSWORD_EXPIRED`; REST clients cannot change the password and have to use the web application. See
  [Password expiration](../security/authentication.md#password-expiration-local-login).
- **The `ChdPwd` maintain target is no longer public.** It could be called without a session to change the password of
  any user. It now needs a session and changes the password of the logged in user only. A custom screen that relied on
  the anonymous call has to use `ChdPwdExpired` (expired passwords) or sign in first. An application that replaces the
  whole `public` menu has to add the `change-expired-password` option.
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

### Client dependencies

Issue #769 cleaned up the npm dependencies of both clients:

- **`moment` is replaced by `day.js`** in the AngularJS client. The dates keep the `dd/mm/yyyy` format that the server
  reads and the validation rules behave as before (impossible dates such as `31/02/2020` are still rejected).
  `moment` was bundled inside the AWE bundle and was never a global, so only code that imports it from the
  `awe-client-angular` package is affected. **Deprecation:** do not rely on `moment` or `lodash` coming with AWE; add
  them to your own `package.json` if you use them. AWE may change or drop them again without a major version.
- **Unused packages were removed** from both clients (see the table row). The React client still installs
  `react-transition-group` through `primereact`, but import it only if you declare it yourself.
- **Karma leftovers are gone** from the AngularJS client (Jasmine specs nobody ran, the Istanbul Babel environments and
  the `babel-plugin-istanbul`, `exports-loader` and `@babel/types` packages). Coverage comes from Jest.
- Major upgrades that need their own migration are not part of this clean-up: AngularJS 1.5, `angular-ui-router` 0.4,
  `angular-ui-grid` (4.12.7 changes the package entry point), Highcharts and jQuery/Bootstrap (#755, #772), Redux Toolkit 2
  (#807) and React 19.

### Java 21 and Spring Boot 4.1

Issue #783 moves AWE 5 to Java 21, Spring Boot 4.1.1 (Spring Framework 7.0, Spring Security 7.0) and Jackson 3.

- **JDK 21 is required** to build and run. Set `java.version` to 21 in your `pom.xml` (the AWE 5 archetypes already do).
  The build must run on a JDK 21 as well: on an older JDK the compilation fails.
- **Spring Boot 4.1 removes everything that 4.0 deprecated**: the old starter names (`spring-boot-starter-web`,
  `-aop`, `-oauth2-*`), `@MockBean`/`@SpyBean` and `spring-boot-jackson2`. Use the modular starters
  (`spring-boot-starter-webmvc`, `-aspectj`, `-security-oauth2-*`) and the explicit test starters. The
  [OpenRewrite](https://docs.openrewrite.org) recipes `UpgradeSpringBoot_4_0` and `SpringBootProperties_4_1` do most of
  this work; review their output.
- Classes that Spring Boot 4 moved and that applications may use directly: `ErrorController`
  (`org.springframework.boot.webmvc.error`), JMS auto-configuration (`org.springframework.boot.jms.autoconfigure`),
  OAuth2 resource server auto-configuration (split into `OAuth2ResourceServerAutoConfiguration` and
  `OAuth2ResourceServerWebSecurityAutoConfiguration`) and `TestRestTemplate`
  (`org.springframework.boot.resttestclient`).
- `awe-spring-boot-starter` now brings `spring-boot-starter-cache` and `spring-boot-jms`, and `awe-controller` brings
  `spring-boot-starter-webmvc`, so applications that relied on them transitively keep working.
- **Tests.** `@SpringBootTest` no longer provides MockMvc or `TestRestTemplate`: add `@AutoConfigureMockMvc` or
  `@AutoConfigureTestRestTemplate`. Mockito `@Mock` fields in Spring tests are no longer initialized (Spring Boot 4
  removed `MockitoTestExecutionListener`) and stay `null`: add `@ExtendWith(MockitoExtension.class)`, with
  `@MockitoSettings(strictness = Strictness.LENIENT)` to keep the lenient stubbing. With JUnit 6, a test that
  wildcard-imports both `org.junit.jupiter.api.*` and `org.springframework.http.*` no longer compiles
  (`reference to MediaType is ambiguous`): import `org.springframework.http.MediaType` explicitly.
- **Selenium** follows the Spring Boot BOM (4.43.0 with Spring Boot 4.1.1) instead of the 4.18.1 that AWE pinned, and
  `awe-dependencies` no longer manages `selenium-server`.
- **`lombok-maven-plugin` is gone.** AWE ran delombok (the sources of the Javadoc) with this abandoned plugin. It now
  uses the `delombok` execution of `maven-antrun-plugin`, managed in `awe-dependencies`, with the Lombok version of
  the Spring Boot BOM. The `skip.delombok` property keeps working. If your `pom.xml` declared `lombok-maven-plugin`,
  declare `maven-antrun-plugin` instead (it inherits the `delombok` execution) or remove it.
- **Spring Session with Redis needs its starter** (#861). Spring Boot 4 moved the Spring Session auto-configuration to
  its own module: with `spring-session-data-redis` alone the application starts without any error, but the sessions
  stay in each server and are lost when a request reaches another instance. Depend on
  `spring-boot-starter-session-data-redis` instead, and rename the `spring.session.redis.*` properties to
  `spring.session.data.redis.*`. The Redis auto-configuration classes moved too: an exclusion of
  `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` no longer excludes anything. Exclude
  `org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration` and
  `org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration` where there is no
  Redis. With Spring Session active the session cookie is `SESSION`.
- `TaskConfig` and `SchedulerTaskConfig` constructors now also take the `Environment`. This only matters if your code
  creates these configuration classes itself.

### Jackson 3

- **Package change.** `com.fasterxml.jackson.core` and `com.fasterxml.jackson.databind` become `tools.jackson.core` and
  `tools.jackson.databind`. The annotations stay in `com.fasterxml.jackson.annotation`. Every AWE method that takes or
  returns `ObjectNode`, `JsonNode`, `ArrayNode` or `ObjectMapper` now uses the Jackson 3 type, so code that calls it
  must change its imports. The full list is in [Jackson 3 public API](#jackson-3-public-api).
- `JsonProcessingException` becomes `JacksonException`, which is unchecked.
- **Jackson 3 throws where Jackson 2 returned a default** (measured on jackson-databind 3.2.1):

  | Call | Jackson 2 | Jackson 3 |
  |---|---|---|
  | `asText()` / `asString()` on an array, object or POJO node | `""` | throws `JsonNodeException` |
  | `asText()` / `asString()` on `NullNode` | `"null"` | `""` |
  | `asInt()` / `asLong()` / `asDouble()` on an array or non-numeric text | `0` | throws |
  | `asBoolean()` on an array or non-boolean text | `false` | throws |
  | `intValue()` on a non-numeric node | `0` | throws |

  AWE calls the default-value overloads (`asString("")`, `asInt(0)`, `asLong(0L)`, `asDouble(0.0)`,
  `asBoolean(false)`), which give the Jackson 2 results except for the text of `NullNode` (`""` instead of
  `"null"`). Do the same in your own code, or handle the exception.
- `ObjectMapper.writer(PrettyPrinter)` was removed: use `writer().with(prettyPrinter)`. `MappingJackson2HttpMessageConverter`
  becomes `JacksonJsonHttpMessageConverter`. java.time support is part of Jackson 3, so `JavaTimeModule` is not
  registered any more.
- Spring Boot 4 auto-configures a `JsonMapper`. To replace it, define a `JsonMapper` bean (an `ObjectMapper` bean is
  not enough). Spring Boot 4 also registers every Jackson module on the classpath; set
  `spring.jackson.find-and-add-modules=false` to opt out.
- java-jwt 4 still depends on Jackson 2, so Jackson 2 stays on the classpath. Do not pass Jackson 2 types to AWE APIs.

### JSON contract

AWE 5 keeps the **AWE 4 JSON format** on Jackson 3 and becomes strict about trailing content. AWE applies its contract
to the `JsonMapper` of Spring Boot with a `JsonMapperBuilderCustomizer` (`DataListUtil.applyAweJsonContract`, also used
by the `DataListUtil` mapper), so the HTTP layer and the injected mapper use it.

| Behavior | Jackson 3 default | AWE 5 |
|---|---|---|
| Enums | written and read with `toString()` (`MaintainType.INSERT` -> `"I"`) | **by name** (`"INSERT"`), as in AWE 4 |
| Property order | alphabetical | **declaration order**, as in AWE 4 |
| `null` for a primitive field | error | **accepted** (default value), as in AWE 4 |
| Dates, durations, unknown properties, view inclusion | Jackson 3 defaults | Spring Boot 3 behavior through `spring.jackson.use-jackson2-defaults=true` (set in AWE's `base.properties`) |
| Content after the root JSON value (`{...},`) | rejected | **rejected with HTTP 400** (AWE 4 ignored it) |

- **Clients that send trailing content** after the JSON body now get HTTP 400 and must be fixed.
- **Jackson 3 clients of AWE.** The format did not change, so Jackson 2 clients keep working. A client with the
  Jackson 3 defaults (for example a plain `new RestTemplate()` with Spring 7) reads enums with `toString()` and fails
  with `Cannot deserialize value of type AnswerType from String "OK"`. Configure it with the AWE mapper:
  `MessageConverterUtil.withJsonMapper(restTemplate, DataListUtil.getMapper())`, or disable
  `EnumFeature.READ_ENUMS_USING_TO_STRING`. `spring.jackson.use-jackson2-defaults` does not help here, because a plain
  `RestTemplate` does not use the Spring Boot mapper. The REST and microservice connectors of AWE already do this.
- To change the contract, add your own `JsonMapperBuilderCustomizer` or `spring.jackson.*` properties; a customizer
  that runs after AWE's overrides it.
- The OpenAPI document of the AWE REST API is unchanged (AWE keeps the `JsonNode` schema as `{}` for springdoc).

### Spring Security 7

- **Login redirects are relative.** An unauthenticated browser request gets `Location: /` instead of
  `Location: http://host/` (Spring Security 7 default). Reverse proxies that rewrite absolute `Location` headers are not
  affected; anything that parsed the absolute URL must accept a relative one.
- `AuthorizationManager.authorize` takes a `Supplier<? extends Authentication>`; update your own implementations.
- `DaoAuthenticationProvider` requires the `UserDetailsService` in its constructor.
- Spring Boot 4 moved servlet web security to `ServletWebSecurityAutoConfiguration`; tests that list security
  auto-configurations explicitly (for example with `ApplicationContextRunner`) must add it.

### Logging, database and web

- **Logback.** Spring Boot 4 removed `org.springframework.boot.logging.logback.ApplicationNameConverter`. A Logback
  configuration that still declares `<conversionRule conversionWord="applicationName" .../>` stops the application at
  startup. AWE's `awe-log.xml` and `scheduler-log.xml` use `%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}` (same
  output); a `logback-spring.xml` copied from AWE 4 or Spring Boot 3 must do the same.
- **Metrics distribution bounds.** A `management.metrics.distribution.maximum-expected-value.http.server.requests` below
  2 minutes, set without a `minimum-expected-value`, now makes **every HTTP request fail with 500**
  (`InvalidConfigurationException: maximumExpectedValue ... must be equal to or greater than minimumExpectedValue`): the
  prefix also matches the `http.server.requests.active` long task timer, whose default minimum is 2 minutes. Set both
  bounds, for example `minimum-expected-value.http.server.requests=1ms`.
- **H2 2.4 rejects the `DATETIME` type** (Spring Boot 4.1 manages H2 2.4). The AWE H2 script
  `AWE_V1.0.0__Init_awe_schema.sql` now declares `HISAweAppPar.HISdat` as `TIMESTAMP`, so its Flyway checksum changes:
  recreate your local H2 database (or run `flyway repair`). H2 is only meant for local development and tests. Your own H2
  scripts that use `DATETIME` must use `TIMESTAMP`.
- **Flyway 11 removed `cleanOnValidationError`**: `spring.flyway.clean-on-validation-error` has no effect.
- File upload uses `commons-fileupload2-jakarta-servlet6` (package `org.apache.commons.fileupload2.jakarta.servlet6`).
- In war deployments to an external container, `server.forward-headers-strategy` has no effect (Spring Boot 4); define a
  `ForwardedHeaderFilter` bean if you need it.
- **jasypt-spring-boot 3.0.5** keeps decrypting `ENC(...)` properties, but it does not declare Spring Boot 4 support
  yet: check it when you upgrade it.

### Maintain transactions

Issue #781 runs each maintain inside one transaction of its datasource (`awe.maintain.transaction.enabled`, `true` by
default). Compared with AWE 4:

- A failing statement, or a step that returns an `ERROR` result without throwing, rolls back the **whole** maintain.
  In AWE 4 an `ERROR` result without an exception was committed.
- Maintains launched from a Java service (`serve`) and `retrieve-data` on the same datasource **join** the outer
  maintain. In AWE 4 they used other connections: a nested maintain stayed committed when the outer one failed, and
  `retrieve-data` did not see the rows the maintain had just written.
- A maintain launched inside a Spring transaction of the application (`@Transactional`, `TransactionTemplate`) on the
  same datasource **joins it**. In AWE 4 it always committed on its own connection.
- A Java service that catches the failure of a nested maintain and returns `OK` now makes the outer maintain fail
  with the generic maintain error, and everything is rolled back.
- `<commit/>` still commits in the middle of the maintain.
- **Tests.** In a `@Transactional` test the maintains now join the test transaction and are rolled back with it. A
  test that reads or cleans with its own connection (`dataSource.getConnection()`) does not see their rows, and on
  HSQLDB it waits forever on the lock. Read through the transaction: `JdbcTemplate`, or
  `new TransactionAwareDataSourceProxy(dataSource).getConnection()`. A test that must check what a maintain commits
  should not be `@Transactional`.

Set `awe.maintain.transaction.enabled=false` to get the AWE 4 behavior. See [Transactions](../api/maintain-definition.md#transactions).

### Date utilities

Issue #782 splits `DateUtil` by responsibility into the `com.almis.awe.model.util.data.date` package, which works with
`java.time`. `DateUtil` stays as a facade with the same methods, results and runtime classes as in AWE 4.

| Class | Responsibility |
|---|---|
| `DateFormats` | The date and time formats (the `DateUtil` constants point to them). |
| `DateTimeParser` | Text to `LocalDate`, `LocalTime` and `LocalDateTime`; `parseAnyFormat` detects the format. |
| `DateTimeFormatting` | `java.time` values to text. |
| `DateTextConverter` | Text from one format to another (lenient: invalid input is returned unchanged). |
| `DateFormatDetector` | Which format a text is written in. |
| `ElapsedTimeFormatter` | Human-readable elapsed time ("3 days ago"). |
| `LegacyDateConverter` | `java.util.Date` and `java.sql` types to and from `java.time`. |

The `DateUtil` methods whose signature uses `java.util.Date` or `Calendar` are deprecated; their Javadoc names the
replacement. Methods that use `java.sql` types (`web2SqlDate`, `sqlDat2WebDate`...) are kept for JDBC code. Some
deprecated methods return a `java.sql.Timestamp` or `java.sql.Time` declared as `Date` (`web2Timestamp`, `web2Time`,
`sql2JavaDate`, `jsonDate`, `getDateWithTimeFromCriteria`, `addTimeToDate`); they still do.

One behavior is fixed: the elapsed time (`DateUtil.elapsedTime`, `dateSince` and the `ELAPSED_TIME` and `DATE_SINCE` transforms)
now counts hours on a 24-hour clock. AWE 4 showed 13 hours as "1 hour" and 12 hours in milliseconds.

In the scheduler, `TaskDAO` keeps its methods and delegates to the classes of `com.almis.awe.scheduler.dao.task`.
`TaskDAO.getProgress(Date, long)` and `TaskDAO.getElapsedTime(Date)` are deprecated in favour of their `Instant`
overloads; a call with a literal `null` now needs a cast.

### MenuService

Issue #782 splits `MenuService` by responsibility into the `com.almis.awe.service.menu` package. `MenuService` keeps
its 33 public methods, with the same signatures and results, and delegates each one to a collaborator. The methods
that `Services.xml` calls by name (`getNameOptionList`, `getModuleList`, `refreshMenu`, the restriction tree
services...) stay on `MenuService`.

| Collaborator | Responsibility | `MenuService` methods that delegate to it |
|---|---|---|
| `MenuProvider` | Load the menus and apply restrictions and favourites. | `getMenu`, `getMenuWithRestrictions`, `getMenuWithAllRestrictions`, `refreshMenu` |
| `MenuOptionLookup` | Find options and check whether they are available. | `getOptionByName`, `getAvailableOptionByName`, `checkOptionAddress`, `isAvailableOption` |
| `MenuScreenResolver` | Resolve the screen of a menu or option and run its initial loads. | `getDefaultScreen`, `getOptionScreen`, `getAvailableOptionScreen`, `getScreen` |
| `MenuSuggestionLists` | Suggestion lists of screens, options and modules. | `getAvailableScreenList`, `getAllScreenList`, `getNameOptionList`, `getModuleList` |
| `MenuRestrictionTreeService` | Profile restrictions and the restriction tree by user or profile. | `getScreenRestrictions`, `getMenuOptionTree`, `getMenuOptionTreeByModule`, `updateMenuRestrictionTree`, `allowMenuOption`, `restrictMenuOption`, `removeRestriction` |

Notes for projects that extend `MenuService`:

- **An override only affects direct callers.** In AWE 4 the methods of `MenuService` called each other, so overriding
  `getOptionByName` also changed `getOptionScreen`, and overriding `getScreen` changed the suggestion lists. Now the collaborators call each other, not `MenuService`: your override is used by the code that calls
  `MenuService` directly (controllers, `Services.xml`, your own services), but not by the other `MenuService`
  methods.
- **Customize the collaborator instead.** Every collaborator is a bean declared with `@ConditionalOnMissingBean`.
  Declare your own bean of the same type (usually a subclass) and AWE uses it everywhere, `MenuService` included:

  ```java
  @Bean
  public MenuOptionLookup menuOptionLookup(MenuProvider menuProvider, BaseConfigProperties baseConfigProperties) {
    return new MyMenuOptionLookup(menuProvider, baseConfigProperties);
  }
  ```

- **Constructors.** The seven-argument constructor (`QueryService`, `ScreenComponentGenerator`, ...) is deprecated
  but still works: it builds its own default collaborators, which share the application context of your
  `MenuService`, so they do not see the collaborator beans you declare. Move a `MenuService` subclass to the
  constructor that takes the five collaborators. Nothing is removed before 6.0.

### SQLBuilder

Issue #782 moves the pure logic of `SQLBuilder` to the `com.almis.awe.service.data.builder.sql` package. Unlike
`MenuService`, `SQLBuilder` is split without changing how its methods call each other, because projects replace the
`SQLQueryBuilder` and `SQLMaintainBuilder` beans with their own subclasses:

- Every public and protected method of `SQLBuilder` keeps its signature, and the protected methods still call each
  other in the same order. An override of `getOperandExpression`, `getVariableAsExpression`, `getStringExpression`,
  `buildPath` or any other protected method still applies wherever it did in AWE 4.
- Only logic that calls no overridable method has moved: the private methods of the filters and the body of a few
  protected methods. The protected methods are still there and delegate to the helpers.
- The generated SQL and the classes of the bound values do not change: a `DATE` variable is still bound as
  `java.sql.Date` and a `TIMESTAMP` variable as `java.sql.Timestamp`. Date operations (`ADD_DAYS`, `SUB_MONTHS`...)
  and the `TRUNCDATE` function are still typed as `java.util.Date`, which decides how the JDBC driver reads them.

| Helper | Responsibility | Moved from `SQLBuilder` |
|---|---|---|
| `SqlFilterConditions` | The condition of a filter (`eq`, `in`, `like`, `exists`...) and the union of a filter group. | Private `applyFilterCondition`, `getFilterEq`, `getFilterIn`, `getFilterLike`, `getFilterExists`, `getBooleanExpressionResults` |
| `SqlOperations` | The expression of an operation (`CONCAT`, `ADD`, `ADD_DAYS`, `SUBSTRING`...). | Body of protected `generateOperationExpression` |
| `SqlFieldFunctions` | The functions and casts of a field. | Bodies of protected `getExpressionFunction` and `getExpressionCast`; the unsupported-value error stays in `SQLBuilder` |
| `SubstringValidator` | The operand count and index types of `SUBSTRING`. | Private `validateIntegerCompatibleOperand`, `validateConstantIntegerOperand`, `validateFieldIntegerOperand`; `validateSubstringOperation` stays and throws the localized error |
| `SqlListValues` | Which values of a list variable a filter binds, and the `LIST_TO_STRING` join. | Part of private `convertVariableListToExpression` and `getListAsStringExpression`, private `addStringToListIfValid` |
| `SqlPaths` | The path of a single node. | Body of protected `buildPath(String)` |
| `SqlDateValues` | The `SYSTEM_TIME` value, read from a `Clock`, and the `TIMESTAMP` value. | Inline code of `getVariableAsExpression` and `getVariableAsExpressionOrNull` |

Notes for projects that extend `SQLQueryBuilder` or `SQLMaintainBuilder`:

- **Keep overriding the protected methods.** The helpers are `final` classes with static methods: they are not
  extension points. To change an operator, a function or a cast, override `generateOperationExpression`,
  `getExpressionFunction` or `getExpressionCast` as before; a `super` call returns the same expression as in AWE 4.
- **Review reflective calls.** The private methods listed above no longer exist in `SQLBuilder`. A subclass that
  calls them through reflection fails with `NoSuchMethodException`; call the protected method that uses them instead
  (`getFilterGroups` for the filters, `getOperationExpression` for `SUBSTRING`).
- **Nothing else changes.** The constructors, the bean declarations (`@ConditionalOnMissingBean`, prototype scope)
  and the reflective join methods of `SQLQueryBuilder` are unchanged.

### Screen contract values

Issue #823 aligns the values that the builders emit and the XSDs accept with what the AngularJS and React engines
handle.

Fixed, so the action now runs:

- `Action.CHECK_RECORDS_GENERATED`, `Action.SELECT_FIRST_ROW` and `Action.SELECT_LAST_ROW` send `check-records-generated`,
  `select-first-row` and `select-last-row` (they sent `check-records-selected`, `select-first-rows` and
  `select-last-rows`).
- `toggle-class` runs in the AngularJS engine, as it already did in React.
- `target-message` runs in the React engine. As in AngularJS, it shows a popover on the target component (or on the
  grid cell, when the target has a row and a column) with the type, text and timeout of the message, and a new one
  replaces the previous one. When the target is not on screen, React shows the message as a notification.
- `close-window`, handled by both engines, is now a valid button action in `general.xsd`.

Deprecated: still valid, no behaviour in either engine, removed in 6.0:

| Value | Where | Instead |
|---|---|---|
| `forward`, `check-leaf-selected`, `check-parent-expanded` | Button action type (`general.xsd`), builder `Action` | Remove it. |
| `get-locals` | Button action type, builder `Action.GET_LOCALS` | Remove it. The client keeps loading the locals with the `get-locals` server action. |
| `check-parent-selected` | Builder `Action.CHECK_PARENT_SELECTED` (not in the XSD) | Remove it. |
| `<frame>`, `<javascript>` | Screen elements (`screen.xsd`) | Remove them: no engine renders them. |
| `component="progress"` on a criterion | `criteriaComponentType` (`screen.xsd`) | Remove it. The `progress` grid column is not affected. |

### Security audit

AWE 5 records the security events of the application (issue #779): logins, logouts, locks, password and second factor
changes, SSO provisioning, denied accesses and REST tokens. See [Security audit](../security/security-audit.md).

- **New table.** The migration `AWE_V1.3.0__Security_audit_events.sql` creates `AweSecAudEvt` in every database that
  Flyway migrates. Applications that build their schema with their own scripts must add the table.
- **On by default.** Events go to the table, to the JSON log (logger `awe.security.audit`) and, with the observability
  starter, to the `awe.security.audit.events` counter. `awe.security.audit.enabled=false` switches the whole feature
  off; `awe.security.audit.database.enabled=false` keeps the log only. Events older than 365 days are purged every day
  at 03:00 (`awe.security.audit.database.retention-days`, `awe.security.audit.database.purge-cron`).
- **Automatic lockout off by default.** `awe.security.lockout.max-attempts` stays `0`. When an application sets a
  value, `ope.NumLog` is used as the counter of consecutive failed logins (wrong password): a successful login resets
  it, and the account is locked (`ope.PwdLck`) when it reaches the limit. Code of your own that writes `NumLog` for
  other purposes would interfere with the lockout.
- **New menu option.** `security-audit` (screen `SecAud`) is added under Settings > Security access in the `private` menu.
  Applications that replace the whole menu do not get it unless they add it.

### Password hashing and master key

AWE 5 hashes user passwords with BCrypt and requires a master key from configuration (issue #778). Products upgraded
from AWE 4 keep working with their RIPEMD-160 hashes and their encrypted data, but some steps must be done **before**
the upgrade. Review both checklists.

#### Password hashing

| Topic | AWE 4 | AWE 5 |
|---|---|---|
| Algorithm of new passwords | RIPEMD-160, without salt (40 characters) | BCrypt, with salt and the `{bcrypt}` prefix (68 characters). `awe.security.password-encoder=ripemd160` keeps the AWE 4 format |
| Stored RIPEMD-160 hashes | Validate | Keep validating, and are rehashed with BCrypt on the next successful login |
| `ope.l1_pas`, `HISope.l1_pas` | `char(40)` | `varchar(255)` (Flyway `AWE_V1.4.0__Password_hash_length.sql`) |
| Current password checks (settings and change password screens) | SQL comparison of hashes | Java, with `PasswordEncoder.matches` |
| `OpePas` | Written by the standard screens (SHA-256 copy) | No longer written; the column stays |
| Query `getUserData` | Returns the stored hash (`oldPassword`) | No longer returns it |

Checklist:

- [ ] **Choose the encoder.** `awe.security.password-encoder=bcrypt` (default) or `ripemd160`. With `bcrypt`, a user
  whose hash is RIPEMD-160 is rehashed with BCrypt on the next successful login with the database authentication. A
  failed rehash (for example a column that is still `char(40)`) does not break the login: the user signs in with the
  current hash and the log shows the WARN `The password of user <user> could not be rehashed with the configured
  algorithm. It keeps the current hash`. With `ripemd160`, new passwords keep the AWE 4 format and nothing is
  rehashed; BCrypt hashes already stored keep validating.
- [ ] **Widen the column before upgrading.** Flyway applies `AWE_V1.4.0__Password_hash_length.sql` on the six
  supported databases: `ope.l1_pas` and `HISope.l1_pas` become `varchar(255)`. Products that manage their own schema
  must apply the equivalent change **before** upgrading (see the script of their database in `db/migration`). The
  script can be applied again, and it never shrinks a column that is already wider than 255 characters.
- [ ] **Review your own XML that compares or writes `l1_pas`.** A BCrypt hash has its own salt, so the hash of the
  same password is different every time: an SQL filter that compares `l1_pas` with a `STRING_HASH_RIPEMD160` variable
  stops matching as soon as the user is rehashed. Write passwords with `STRING_HASH_PASSWORD` (it hashes with the
  configured encoder) and check them in Java with `PasswordEncoder.matches`, as `CurrentPasswordChangeService` does.
  `STRING_HASH_RIPEMD160` keeps working and is deprecated. See [Variable types](../api/query-definition.md#variable-types).
- [ ] **Review the overridden standard targets.** If your own `Maintain.xml` or `Queries.xml` redefines the password
  targets, compare them with AWE 5: `UsrUpdPwd` and `ChdPwdPub` now call a Java service that checks the current
  password and then launches `UsrUpdPwdUpdate` and `ChdPwdPubUpdate`; the query `UsrChkPwd` is removed.
- [ ] **Review external systems that read `l1_pas`.** A system that compares or copies the hash (another application,
  a report, a single sign-on bridge) does not understand BCrypt. Keep `awe.security.password-encoder=ripemd160` until
  it is adapted.
- [ ] **Back up `ope` before the upgrade.** There is no rollback to AWE 4 for users already rehashed: AWE 4 cannot
  validate a BCrypt hash. To roll back, restore the backup (users who changed their password after the upgrade lose
  the change).
- [ ] **Adapt anything that reads `OpePas`.** The standard screens no longer write it (it held an unsalted SHA-256
  copy of the password). The column is kept; its values are no longer updated.
- [ ] **Adapt custom screens that read `oldPassword`.** `getUserData` no longer sends the stored hash to the browser,
  and the settings screens no longer validate the current password in the browser (`validation` with
  `criterion:'oldPassword'`): the server checks it. A wrong current password still answers OK and updates nothing.
- [ ] **Insert scripts for users.** The [Encrypt util](default-screens.md#encrypt-util) screen now shows the
  **Password hash** of the text, ready to insert into `ope.l1_pas`, with the configured algorithm or, when you choose
  it in **Password algorithm**, with the AWE 4 RIPEMD-160 hash.
- [ ] **Masked values in the SQL log.** The SQL logged (and carried by query errors) shows `'********'` instead of the
  values of `STRING_HASH_*` and `STRING_ENCRYPT` variables and of the values written to or compared with the columns
  of `awe.database.log-masked-fields` (default `l1_pas`); the statement executed is unchanged. Add your own sensitive
  columns to the property if you need them masked too.

#### Master key

`awe.security.master-key` encrypts stored data: `STRING_ENCRYPT` variables, `ENC(...)` properties and the values of
the Encrypt util screen. AWE 4 used a published default value when the property was not set; AWE 5 has no default.

Checklist:

- [ ] **Set the master key.** Without it the application does not start, and Spring Boot reports this reason for the
  property `awe.security.master-key`:

  ```text
  awe.security.master-key is required: it encrypts stored data (encrypted parameters and properties). Applications
  upgraded from AWE 4 that never configured it must set the former default value (see the AWE 5 migration guide) to
  keep decrypting their stored data, then rotate it.
  ```

- [ ] **Applications that never set it** encrypted their data with the AWE 4 default value. Set that value, copied
  literally (`%25` included), to keep decrypting the `STRING_ENCRYPT` data and the `ENC(...)` properties:

  ```properties
  awe.security.master-key=B1Le3s%25abc75TeBe05
  ```

  The value is accepted, and each startup logs the WARN `awe.security.master-key is set to the published AWE 4
  default value. Keep it only while stored data encrypted with it is needed, and rotate it (see the AWE 5 migration
  guide)`.
- [ ] **Plan the rotation.** The AWE 4 default value is public. Choose a new secret key per environment, generate the
  new values from their plain text in the Encrypt util screen with the new key in the `Phrase key` field (`Encrypted`
  for the database columns, `Encrypted property` for `ENC(...)`), and deploy the new values together with the new
  `awe.security.master-key`.
- [ ] **Jasypt and the JWT secret follow the master key.** `jasypt.encryptor.password` (the `ENC(...)` properties) and
  the default of `awe.rest.api.jwt.secret` no longer hide a copy of the AWE 4 value: they take the master key. Unless
  `awe.rest.api.jwt.secret` is set, rotating the master key invalidates the local JWT tokens already issued.
- [ ] **Use only `awe.security.master-key`.** The legacy name `awe.security.master.key` still wins for jasypt and the
  JWT secret when it is set, but it does not satisfy the required `awe.security.master-key`, and `STRING_ENCRYPT`
  always uses `awe.security.master-key`. Move the value to the new name and remove the old one, so that both cannot
  differ.

### Jackson 3 public API

The public and protected members of the AWE 5 framework modules whose signature uses a Jackson 3 type
(`tools.jackson.core` or `tools.jackson.databind`). Code that calls one of them, overrides it or extends its class with
Jackson 2 types must move to the Jackson 3 ones. Types are shown by their simple name; the list was taken from the
compiled classes, so it includes the getters and setters that Lombok generates.

<details>
<summary>Show the list (204 members)</summary>

**`awe-controller`**

```java
// com.almis.awe.controller.ActionController
  List<ClientAction> launchAction(String, ObjectNode)
  List<ClientAction> launchAction(String, String, ObjectNode)
// com.almis.awe.controller.FileController
  ResponseEntity<String> getFileAsText(ObjectNode)
  ResponseEntity<FileSystemResource> getFileAsStream(ObjectNode)
  ResponseEntity<FileSystemResource> getFileAsStreamPOST(ObjectNode, String)
  ResponseEntity<byte[]> downloadFile(ObjectNode)
  ResponseEntity<byte[]> downloadFileMaintain(ObjectNode, String)
  ServiceData deleteFile(ObjectNode)
// com.almis.awe.controller.ScreenDataController
  ScreenData getDefaultScreenData(ObjectNode)
  ScreenData getOptionScreenData(String, ObjectNode)
// com.almis.awe.controller.UploadController
  UploadController(FileService, BroadcastService, ObjectMapper, BaseConfigProperties)
// com.almis.awe.security.authentication.filter.JsonAuthenticationFilter
  JsonAuthenticationFilter(AweElements, ObjectMapper)
// com.almis.awe.service.ChartService
  ChartService(ObjectMapper, BaseConfigProperties)
// com.almis.awe.service.EmailService
  Future<ServiceData> sendEmail(String, ObjectNode)
// com.almis.awe.service.MaintainService
  ServiceData launchMaintain(String, ObjectNode)
  ServiceData launchMaintain(String, ObjectNode, DatabaseConnection, boolean)
  ServiceData launchPrivateMaintain(String, ObjectNode)
  ServiceData launchPrivateMaintain(String, ObjectNode, DatabaseConnection, boolean)
  DatabaseConnection getDatabaseConnection(ObjectNode)
  ServiceData launchMaintain(Target, ObjectNode, DatabaseConnection, boolean)
// com.almis.awe.service.QueryService
  ServiceData launchQuery(String, ObjectNode)
  ServiceData launchQuery(String, ObjectNode, boolean)
  ServiceData launchPrivateQuery(String, ObjectNode)
// com.almis.awe.service.connector.AbstractRestConnector
  protected AbstractRestConnector(ClientHttpRequestFactory, ObjectMapper)
// com.almis.awe.service.connector.JavaConnector
  JavaConnector(ObjectMapper)
// com.almis.awe.service.connector.MicroserviceConnector
  MicroserviceConnector(ClientHttpRequestFactory, QueryUtil, ObjectMapper, RestConfigProperties)
// com.almis.awe.service.connector.RestConnector
  RestConnector(ClientHttpRequestFactory, ObjectMapper, RestConfigProperties)
// com.almis.awe.service.data.builder.AbstractQueryBuilder
  protected ObjectNode parameters
  protected void addComponentSort(ArrayNode)
  protected String getVariableAsString(JsonNode)
  ObjectNode getParameters()
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.EnumBuilder
  ArrayNode getEnumeratedAsJson()
  ArrayNode getOptionListAsJson(EnumeratedGroup)
  ObjectNode getJsonRow(Global)
// com.almis.awe.service.data.builder.QueueBuilder
  QueueBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.SQLMaintainBuilder
  SQLMaintainBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.SQLQueryBuilder
  SQLQueryBuilder setComponentSort(ArrayNode)
  SQLQueryBuilder setParameters(ObjectNode)
  AbstractQueryBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.builder.XMLEmailBuilder
  ObjectNode getParameters()
  XMLEmailBuilder setParameters(ObjectNode)
// com.almis.awe.service.data.connector.maintain.EmailMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.MaintainConnector
  abstract <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.MaintainLauncher
  ServiceData launchMaintain(MaintainQuery, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.QueueMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.SQLMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.maintain.ServiceMaintainConnector
  <T extends MaintainQuery> ServiceData launch(T, DatabaseConnection, ObjectNode)
// com.almis.awe.service.data.connector.query.AbstractQueryConnector
  protected AbstractQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.service.data.connector.query.EnumQueryConnector
  EnumQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData launchEnum(String, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueryConnector
  abstract ServiceData launch(Query, ObjectNode)
  abstract ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueryLauncher
  ServiceData launchQuery(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.QueueQueryConnector
  QueueQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.SQLQueryConnector
  SQLQueryConnector(AweDatabaseContextHolder, QueryUtil, javax.sql.DataSource, BaseConfigProperties, AweElements, NumericService, EncodeService, DatabaseConfigProperties, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.connector.query.ServiceQueryConnector
  ServiceQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceData launch(Query, ObjectNode)
  ServiceData subscribe(Query, ComponentAddress, ObjectNode)
// com.almis.awe.service.data.processor.CompoundColumnProcessor
  CompoundColumnProcessor(AweElements, BaseConfigProperties, Compound, Map<String, QueryParameter>, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.service.report.ReportDesigner
  ReportDesigner(QueryService, ObjectMapper, BaseConfigProperties)
  com.almis.ade.api.bean.input.PrintBean getPrintDesign(List<Element>, ObjectNode)
  com.almis.ade.api.bean.input.PrintBean getPrintDesign(List<Element>, ObjectNode, boolean)
  boolean hasSpreadsheetOnlyColumns(List<Element>, ObjectNode)
// com.almis.awe.service.report.ReportDesigner$1
  type: extends TypeReference<List<PrintColumnData>>
// com.almis.awe.service.report.ReportDesigner$2
  type: extends TypeReference<Map<String, Object>>
// com.almis.awe.service.report.ReportGenerator
  FileData generateReportFormat(com.almis.ade.api.fluid.engine.generic.TemplateExporterBuilder, String, String, String, ObjectNode)
// com.almis.awe.service.report.ReportGenerator$GeneratedScreenReportContext
  ReportGenerator$GeneratedScreenReportContext(List<FileData>, ObjectNode)
  ObjectNode parameters()
// com.almis.awe.service.screen.ScreenComponentGenerator
  Map<String, ScreenComponent> generateComponentMap(Screen, ScreenData, Menu, ObjectNode)
```

**`awe-model`**

```java
// com.almis.awe.config.ServiceConfig
  ObjectNode getRequestParameters()
  ObjectNode getMutableRequestParameters()
  JsonNode getRequestParameter(String)
  JsonNode getRequestParameter(String, ObjectNode)
  String getRequestParameterAsString(String, ObjectNode)
  void putRequestParameter(ObjectNode, String, JsonNode)
  void putRequestParameter(ObjectNode, String, String)
  void putPropagatedRequestParameter(String, JsonNode)
  void mergePropagatedRequestParameters(ObjectNode)
// com.almis.awe.model.component.AweRequest
  AweRequest(HttpServletRequest, HttpServletResponse, ObjectMapper)
  void setParameterList(ObjectNode)
  JsonNode getParameter(String)
  ObjectNode getParameterList()
  ObjectNode getParametersSafe()
  List<CellData> getParameterAsCellDataList(JsonNode)
  CellData getParameterAsCellData(JsonNode)
  ObjectNode getParameters()
  ObjectMapper getMapper()
  AweRequest setParameters(ObjectNode)
// com.almis.awe.model.component.PrototypeRequestBeanHolder
  ObjectNode getRequestDataSnapshot()
  void mergeRequestData(ObjectNode)
  ObjectNode getPendingPropagationSnapshot()
// com.almis.awe.model.component.RequestDataHolder
  ObjectNode getRequestData()
  void setRequestData(ObjectNode)
// com.almis.awe.model.dto.FilterColumn
  FilterColumn(ObjectNode)
// com.almis.awe.model.dto.QueryData
  ObjectNode getParameters()
  QueryData setParameters(ObjectNode)
// com.almis.awe.model.dto.QueryParameter
  QueryParameter(JsonNode)
  QueryParameter(JsonNode, boolean, ParameterType)
  JsonNode getValue()
  void setValue(JsonNode)
// com.almis.awe.model.dto.SortColumn
  SortColumn(ObjectNode)
// com.almis.awe.model.entities.Element
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.actions.ComponentAddress
  static ComponentAddress fromJson(JsonNode)
// com.almis.awe.model.entities.queries.Query
  String getQueryKeys(ObjectNode)
// com.almis.awe.model.entities.screen.Screen
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.Dialog
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.Window
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.chart.Chart
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.chart.ChartSeriePoint
  List<JsonNode> getPositionPoint()
  void setPositionPoint(List<JsonNode>)
// com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.grid.AbstractGrid
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.panelable.Panelable
  List<Element> getReportStructure(List<Element>, String, ObjectNode, String)
// com.almis.awe.model.entities.screen.component.pivottable.PivotSorter
  void addSorter(String, JsonNode)
  Map<String, JsonNode> getSorterMap()
  void setSorterMap(Map<String, JsonNode>)
  PivotSorter(Map<String, JsonNode>)
// com.almis.awe.model.entities.screen.data.AweThreadInitialization
  ObjectNode getParameters()
  AweThreadInitialization setParameters(ObjectNode)
// com.almis.awe.model.util.data.DataListUtil
  static JsonMapper$Builder applyAweJsonContract(JsonMapper$Builder)
  static ObjectMapper getMapper()
  static ArrayNode getColumnAsArrayNode(DataList, String)
// com.almis.awe.model.util.data.QueryUtil
  QueryUtil(BaseConfigProperties, DatabaseConfigProperties, ObjectMapper, PrototypeRequestBeanHolder)
  List<SortColumn> getSortList(ArrayNode)
  Map<String, QueryParameter> getDefaultVariableMap(ObjectNode)
  Map<String, QueryParameter> getVariableMap(List<Variable>, ObjectNode, Integer)
  Map<String, QueryParameter> getVariableMap(Query, ObjectNode)
  void addToVariableMap(Map<String, QueryParameter>, Query, ObjectNode)
  void addDataListToRequestParameters(DataList, ObjectNode)
  Map<String, QueryParameter> getVariableMap(MaintainQuery, ObjectNode)
  Map<String, QueryParameter> getVariableMap(MaintainQuery, ObjectNode, List<ServiceInputParameter>)
  ObjectNode getParameters()
  ObjectNode getParameters(ObjectNode)
  <T> ObjectNode getParameters(T)
  ObjectNode getParameters(String, String, String)
  ObjectNode getParameters(String)
  ObjectNode getParameters(ObjectNode, String, String, String)
  JsonNode getParameter(Variable, ObjectNode)
  JsonNode getParameter(String, JsonNode, ParameterType, String)
  JsonNode getRequestParameter(String, ObjectNode)
  JsonNode getRequestParameter(String)
  boolean variableIsList(Variable, ObjectNode)
  boolean isEmptyVariable(JsonNode)
  boolean isNullVariable(JsonNode)
// com.almis.awe.model.util.data.StringUtil
  static ArrayNode toArrayNode(String, String)
  static List<String> asList(JsonNode)
// com.almis.awe.model.util.web.MessageConverterUtil
  static RestTemplate withJsonMapper(RestTemplate, ObjectMapper)
```

**`awe-builder`**

```java
// com.almis.awe.builder.client.grid.AddRowActionBuilder
  AddRowActionBuilder(RowPosition, String, ObjectNode)
// com.almis.awe.builder.client.grid.RowsActionBuilder
  RowsActionBuilder(String, RowPosition, String, ObjectNode)
// com.almis.awe.builder.client.grid.UpdateCellActionBuilder
  UpdateCellActionBuilder(ComponentAddress, JsonNode)
// com.almis.awe.builder.client.grid.UpdateRowActionBuilder
  UpdateRowActionBuilder(String, ObjectNode)
  UpdateRowActionBuilder(String, String, ObjectNode)
```

**`awe-notifier`**

```java
// com.almis.awe.notifier.service.NotifierService
  NotifierService(QueryService, MaintainService, BroadcastService, ObjectMapper)
```

**`awe-rest`**

```java
// com.almis.awe.rest.controller.AweRestController
  AweRestController(QueryService, MaintainService, AweRequest, JWTTokenService, org.modelmapper.ModelMapper, ObjectMapper)
// com.almis.awe.rest.security.JWTAuthenticationEntryPoint
  JWTAuthenticationEntryPoint(ObjectMapper)
// com.almis.awe.rest.security.JWTAuthenticationFilter
  JWTAuthenticationFilter(AuthenticationManager, ObjectMapper, JWTTokenService)
  JWTAuthenticationFilter(AuthenticationManager, ObjectMapper, JWTTokenService, AweElements)
```

**`awe-scheduler`**

```java
// com.almis.awe.scheduler.dao.CalendarDAO
  CalendarDAO(org.quartz.Scheduler, QueryService, QueryUtil, ObjectMapper)
// com.almis.awe.scheduler.dao.TaskDAO
  ObjectNode getProgressNode(long)
  ObjectNode getStatusIcon(TaskExecution)
  ServiceData loadExecutionScreen(String, ObjectNode)
// com.almis.awe.scheduler.log.DatabaseExecutionLogStore
  ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.log.ExecutionLogStore
  abstract ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.log.FileExecutionLogStore
  ObjectNode locatorNode(ExecutionKey)
// com.almis.awe.scheduler.service.RemoteSchedulerService
  RemoteSchedulerService(SchedulerService, RemoteScheduler, ObjectMapper, boolean)
// com.almis.awe.scheduler.service.RemoteSchedulerService$1
  type: extends TypeReference<List<String>>
// com.almis.awe.scheduler.service.SchedulerService
  ServiceData loadExecutionScreen(String, JsonNode)
// com.almis.awe.scheduler.service.report.SchedulerEmailReportService
  SchedulerEmailReportService(QueryUtil, MaintainService, QueryService, ObjectMapper)
// com.almis.awe.scheduler.service.scheduled.MaintainJobService
  MaintainJobService(ExecutionService, MaintainService, QueryUtil, TaskDAO, ApplicationEventPublisher, ObjectMapper, ExecutionLogStore, java.time.Duration, boolean, java.net.URI, boolean, String, String, RestTemplate)
// com.almis.awe.scheduler.service.scheduled.MaintainJobService$1
  type: extends TypeReference<Map<String, Object>>
```

**`awe-tools`**

```java
// com.almis.awe.tools.controller.FileManagerController
  FileManagerController(FileManagerService, ServletContext, ObjectMapper)
// com.almis.awe.tools.service.FileManagerService
  JsonNode uploadFile(String, List<MultipartFile>)
  JsonNode fileOperation(ObjectNode)
  JsonNode error(Exception)
```

**`awe-notifier-spring-boot-starter`**

```java
// com.almis.awe.notifier.autoconfigure.NotifierConfig
  NotifierService notifierService(QueryService, MaintainService, BroadcastService, ObjectMapper)
```

**`awe-rest-spring-boot-starter`**

```java
// com.almis.awe.rest.autoconfigure.RestSecurityConfiguration
  RestSecurityConfiguration(PublicQueryMaintainAuthorization, AuthenticationManager, AuthenticationManager, UserDetailsService, ObjectMapper, AweRestConfigProperties, BaseConfigProperties, SecurityConfigProperties)
```

**`awe-scheduler-spring-boot-starter`**

```java
// com.almis.awe.scheduler.autoconfigure.SchedulerConfig
  RemoteSchedulerService remoteSchedulerService(SchedulerService, RemoteScheduler, ObjectMapper)
  MaintainJobService maintainJobService(ExecutionService, MaintainService, QueryUtil, TaskDAO, ApplicationEventPublisher, ObjectMapper, ExecutionLogStore, RestTemplate)
  SchedulerEmailReportService emailReportService(QueryUtil, MaintainService, QueryService, ObjectMapper)
  CalendarDAO calendarDAO(org.quartz.Scheduler, QueryService, QueryUtil, ObjectMapper)
```

**`awe-spring-boot-starter`**

```java
// com.almis.awe.autoconfigure.AweAutoConfiguration
  AweRequest aweRequest(HttpServletRequest, HttpServletResponse, ObjectMapper)
  ObjectMapper objectMapper()
  QueryUtil queryUtil(BaseConfigProperties, DatabaseConfigProperties, ObjectMapper, PrototypeRequestBeanHolder)
  ChartService chartService(ObjectMapper, BaseConfigProperties)
  ReportDesigner reportDesigner(QueryService, ObjectMapper, BaseConfigProperties)
  JavaConnector javaConnector(ObjectMapper)
  MicroserviceConnector microserviceConnector(ClientHttpRequestFactory, QueryUtil, ObjectMapper, RestConfigProperties)
  RestConnector restConnector(ClientHttpRequestFactory, ObjectMapper, RestConfigProperties)
  EnumQueryConnector enumQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
  ServiceQueryConnector serviceQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.autoconfigure.AweWebSecurityConfig
  AweWebSecurityConfig(ApplicationContext, BaseConfigProperties, SecurityConfigProperties, AweSessionDetails, AweElements, ActionService, ObjectMapper, AccessService, ErrorPageService, MultiTenantOAuth2Config, MultiTenantFilter)
  JsonAuthenticationFilter jsonAuthenticationFilter(BaseConfigProperties, AweElements, ActionService, ObjectMapper)
// com.almis.awe.autoconfigure.JmsConfig
  QueueQueryConnector queueQueryConnector(QueryUtil, BaseConfigProperties, AweElements, NumericService, EncodeService, ObjectMapper)
// com.almis.awe.autoconfigure.SQLConfig
  SQLQueryConnector sqlQueryConnector(AweDatabaseContextHolder, QueryUtil, javax.sql.DataSource, BaseConfigProperties, AweElements, NumericService, EncodeService, DatabaseConfigProperties, ObjectMapper)
```

</details>

## Coming in AWE 5

These initiatives are open, labelled for the 5.0.0 milestone and expected to need action from applications. They are
listed so you can plan; each one adds its own entry above when it is merged.

| Issue | Title |
|---|---|
| [#755](https://gitlab.com/aweframework/awe/-/issues/755) | Replace vendored Bootstrap 3 with Tailwind and an AWE compatibility layer |
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Replace angular-ui-grid with a modern grid component |
| [#829](https://gitlab.com/aweframework/awe/-/issues/829) | Sanitize the HTML of grid cells in the React client (an inline `style` in a cell will be dropped; use a CSS class) |

## Upgrade checklist

Copy this list to the issue of your upgrade and tick it as you go.

```markdown
- [ ] The application runs on the latest 4.x release and builds without deprecation warnings
- [ ] `awe.version` (or the `awe-starter-parent` version) is set to the AWE 5 version
- [ ] `awe-react-client` is set to the exact same version, `npm install` was run and the lock file is committed
- [ ] Every screen with a chart was opened and the server log has no `Highcharts chart-parameter` warnings
- [ ] Custom code that imports Highcharts or reads `.highcharts-*` CSS was reviewed
- [ ] Highcharts scripts (themes, plugins) were removed from `scripts.xml` of AngularJS applications
- [ ] The `theme` attribute was removed from the charts, and `setTheme` calls from the builders
- [ ] Custom code that uses jsoup declares its own dependency
- [ ] If the application sets `PwdExp`, the value and the users with an old or empty password change date were reviewed
- [ ] Custom clients do not read `elementList` from the menu JSON
- [ ] Custom clients do not read `elementList` from the `controller` of the `screen-data` components
- [ ] Flyway was repaired or the database recreated if `SCHEDULER_V1.0.5` had been applied before
- [ ] The decision about the dark mode that follows the operating system was taken (`awe.application.theme-mode`)
- [ ] CSS of the application no longer sets the AWE 4 theme variables (`--primary-color`...) or styles `.theme-<name>`
- [ ] Browser tests compile; the `By` overloads and `getDriver()` were moved to `Locator` and `getBrowser()`
- [ ] The application builds and runs on JDK 21 with `java.version` 21
- [ ] Spring Boot 4.1 starters and test annotations replace the removed ones (`spring-boot-starter-web`, `@MockBean`...)
- [ ] Spring Session with Redis uses `spring-boot-starter-session-data-redis` and `spring.session.data.redis.*` properties
- [ ] Jackson imports moved to `tools.jackson.*`; `asText()`/`asInt()`/`asBoolean()` calls use the default-value overloads
- [ ] Clients of the AWE API send no content after the JSON body; Jackson 3 clients use the AWE mapper
- [ ] Own `logback-spring.xml` no longer declares the `applicationName` conversion rule
- [ ] Maintains that relied on partial commits were reviewed, or `awe.maintain.transaction.enabled=false` is set
- [ ] The security audit was reviewed: `AweSecAudEvt` exists, the `security-audit` option is restricted, `awe.security.lockout.max-attempts` decided
- [ ] Screens and builders use no deprecated contract value (`forward`, `get-locals`, `<frame>`, `<javascript>`...)
- [ ] `ope` was backed up, `l1_pas` is `varchar(255)` and `awe.security.password-encoder` was decided (own XML and external readers of `l1_pas` reviewed)
- [ ] `awe.security.master-key` is set (the AWE 4 default value if the application never set it) and its rotation is planned
- [ ] CSS of the application that styled icon font glyphs (`.fa-*:before`, `.pi-*`) or the `.layout-dark` class was reviewed
- [ ] An own security configuration allows `/icons/**` (the sign-in shows its icons)
- [ ] CSS overrides of the sign-in, change password and 2FA screens were reviewed in light and dark
- [ ] AngularJS browser tests that press Escape in a confirm or dialog, or rely on the focus, were reviewed
- [ ] React browser tests expect the user name without the user code in `checkLogin`/`ensureLoggedIn`
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
