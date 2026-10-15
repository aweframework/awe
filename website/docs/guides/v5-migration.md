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
| Password expiration (`PwdExp`) | The application sets the `PwdExp` parameter. After the upgrade, users whose last password change is older than `PwdExp` days are told that the password has expired (they used to get in) and have to set a new one, and users who changed it recently can log in again (they used to be rejected). | Nothing for correct data: the check is no longer inverted and the login guides the user to change the password (see the note below the table). Review the users that never changed the password and the value of `PwdExp`. | [Password expiration](../security/authentication.md#password-expiration-local-login), issue #846 |
| Menu JSON | The menu payload is smaller. | Nothing, unless a custom client reads `elementList` from a menu `Option`: read `options` instead. See the note below the table. | MR !792 |
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
| Virtual threads (#783, new) | Nothing changes unless you set `spring.threads.virtual.enabled=true`. | Optional: set the property to run Spring Boot and the AWE task executors on virtual threads. | [Virtual threads](../deployment.md#virtual-threads) |
| Client dependencies (`moment`, `lodash`, build tooling) | The AngularJS client no longer ships `moment` (it uses `day.js`), and the React client no longer lists `@redux-devtools/extension`, `redux-thunk`, `react-transition-group`, `deep-freeze`, `autoprefixer`, `postcss-loader` or `@types/*`. Your own code imports one of them and the build fails with `Cannot find module`. | Declare the package you import in your own `package.json`: AWE only installed these as an implementation detail and does not promise them to applications. `lodash` stays, but treat it the same way. See the note below the table. | [Client dependencies](#client-dependencies), issue #769 |
| Dark mode and themes (AngularJS client) | Users that never chose the light or the dark mode now get the dark mode when their operating system prefers it. The themes now declare their dark variables through a mixin, and the `--awe-*` design tokens exist. | Nothing for correct themes and LESS. To keep ignoring the operating system, set `@awe-follow-os-color-scheme: false` after importing AWE. Users can still choose the light or the dark mode in the user menu. | [Design tokens and dark mode](design-tokens-guide.md) (issue #770) |

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
| [#775](https://gitlab.com/aweframework/awe/-/issues/775) | Migrate charts from Highcharts to Apache ECharts with server-side rendering |
| [#776](https://gitlab.com/aweframework/awe/-/issues/776) | Replace angular-ui-grid with a modern grid component |
| [#778](https://gitlab.com/aweframework/awe/-/issues/778) | Enforce modern password hashing and a mandatory master key |
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
- [ ] The decision about the dark mode that follows the operating system was taken (`@awe-follow-os-color-scheme`)
- [ ] Browser tests compile; the `By` overloads and `getDriver()` were moved to `Locator` and `getBrowser()`
- [ ] The application builds and runs on JDK 21 with `java.version` 21
- [ ] Spring Boot 4.1 starters and test annotations replace the removed ones (`spring-boot-starter-web`, `@MockBean`...)
- [ ] Jackson imports moved to `tools.jackson.*`; `asText()`/`asInt()`/`asBoolean()` calls use the default-value overloads
- [ ] Clients of the AWE API send no content after the JSON body; Jackson 3 clients use the AWE mapper
- [ ] Own `logback-spring.xml` no longer declares the `applicationName` conversion rule
- [ ] Maintains that relied on partial commits were reviewed, or `awe.maintain.transaction.enabled=false` is set
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
