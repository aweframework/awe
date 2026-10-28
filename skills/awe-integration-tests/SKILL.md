---
name: awe-integration-tests
description: "Trigger: run integration tests, browser IT, playwright IT, selenium IT, spring integration tests, awe-boot verify, run a test suite. Exact commands to run AWE's Spring integration and browser (Playwright, Selenium) tests locally."
license: Apache-2.0
metadata:
  author: awe-team
  version: "1.2"
---

## Activation Contract

Use when running or debugging AWE's integration tests locally: Spring/JUnit DB-backed tests, or the browser suites. Commands mirror `.gitlab-ci.yml` so a local pass predicts CI.

The browser suites run with **Playwright by default** (`-Dawe.test.tool=playwright`): those are the jobs of every merge request (`E2E · <engine> · Chromium · <suite>`, `.playwright-testing` in `.gitlab-ci.yml`). Selenium (`awe.test.tool=selenium`, the property default) only runs on `master`, `support/*` and the `develop` schedule (`.rules-selenium`): use it only to reproduce one of those jobs, or for remote/service browsers, which Playwright does not support.

## Hard Rules

- Browser ITs are ordered and stateful (`t000`→`t999`); run the whole suite tag, never a single method.
- Run them with Playwright, as the merge request pipeline does: `-Dawe.test.tool=playwright -Dawe.test.allowed-recording=false`. Do not run Selenium unless you reproduce a `master`/`support/*`/schedule job.
- `verify` on awe-boot starts its own app (`spring-boot:start`, port 8080) and stops it after — free 8080 first, or move the run to a free port (see Execution Steps); do not leave a manual `spring-boot:run` instance up.
- **Selenium only, Linux / WSL:** prefix `xvfb-run -a` when there is no X11 `DISPLAY`: the FFmpeg video recorder (forced by `video.properties` `recorder.type=FFMPEG`, `video.mode=ALL`) queries AWT `getScreenSize` and throws `HeadlessException` without a display. CI has a virtual display.
- **Selenium only, Windows:** no `xvfb-run` — the desktop session is a real display, so no `HeadlessException`. Instead `ffmpeg` must be on the `PATH` (`WindowsFFmpegRecorder` shells out to `ffmpeg gdigrab`), run the command on a single line (`\` is bash-only), and do NOT pass `-Djava.awt.headless=true`. To skip recording entirely (no ffmpeg needed), unset `video.mode` / disable recording in `video.properties`.
- Playwright needs no display and no recorder (`awe.test.allowed-recording=false`, as in CI). It downloads its own browsers to `~/.cache/ms-playwright` on the first run (needs network); `headless-chrome` runs its Chromium, not the installed Chrome.
- Selenium Manager / WebDriverManager auto-resolves the driver for the installed `google-chrome`; no manual chromedriver needed (needs network on first run).
- Run `mvn install -DskipTests` (or `-pl <module>`) before testing so awe-boot bundles your latest module resources.

## Execution Steps

### Browser ITs with Playwright (default, as in merge requests)
```bash
mvn -f awe-tests/awe-boot/pom.xml verify \
  -Dskip.junit=true -Dskip.selenium=false -DgenerateIntegrationReport=true \
  -Dawe.test.browser=headless-chrome -Dawe.test.tool=playwright -Dawe.test.allowed-recording=false \
  -Dgroups=<SUITE_TAG>
```
- `<SUITE_TAG>` (JUnit `@Tag`): `SchedulerIT`, `ApplicationIntegrationIT`, `CRUDCriteriaMatrixIT`, `RegressionWebsocketPrintIT`.
- `-Dskip.selenium=false` enables the browser ITs whatever the tool (the flag predates Playwright).
- `-DgenerateIntegrationReport=true` is mandatory on `awe-boot`: its failsafe plugin lives only in the
  `generate-integration-report` profile, so without it the app starts and stops, **no test runs, and the build still ends
  in BUILD SUCCESS** (CI passes it in `IT_OPTS`). Never trust the exit code alone: check that the log has
  `Tests run: N` with N > 0 (reports in `target/surefire-reports/TEST-*IT.xml`).
- Port 8080 busy (a `spring-boot:run` or hot-reload app up): do not stop it; run on a free port with the flags of
  `bin/local-ci.sh`, e.g. for port 6311:
  `SERVER_PORT=6311 mvn ... "-Dspring-boot.run.arguments=--server.port=6311" -Dawe.test.server-port=6311 -Dawe.test.server-host=localhost -Dawe.test.start-url=http://localhost:6311/ -Dawe.test.jmx-port=9311 -Djacoco.port=6321`
  (another free set per suite run in parallel).
- Browser: `headless-chrome` (Chromium, the merge request jobs) or `headless-firefox` (the Firefox jobs run on `master` and the nightly `develop` schedule).
- React test application: `-f awe-tests/awe-boot-react/pom.xml` with the same options.
- Failure evidence: screenshot, page source, console and the Playwright trace of the failed test.

### Browser ITs with Selenium (only to reproduce master, support/* or schedule jobs)
```bash
xvfb-run -a mvn -f awe-tests/awe-boot/pom.xml verify \
  -Dskip.junit=true -Dskip.selenium=false -DgenerateIntegrationReport=true \
  -Dawe.test.browser=headless-chrome -Dgroups=<SUITE_TAG>
```
- Drop `xvfb-run -a` on a host with a real display (macOS, or Linux with X11).
- On **Windows** (PowerShell / cmd), no `xvfb-run` and single line — needs `ffmpeg` on the `PATH`:
```powershell
mvn -f awe-tests/awe-boot/pom.xml verify -Dskip.junit=true -Dskip.selenium=false -DgenerateIntegrationReport=true -Dawe.test.browser=headless-chrome -Dgroups=<SUITE_TAG>
```

### Spring / JUnit integration + unit tests (DB-backed)
```bash
# All (root reactor), frontend skipped
mvn verify -Dskip.frontend=true -DgenerateUnitReport=true
# Against one database profile
mvn verify -Dskip.frontend=true -P<db>          # db: h2 h2-flyway hsql-flyway oracle oracle-flyway mysql mysql-flyway postgresql postgresql-flyway sqlserver sqlserver-flyway
# One class (surefire), e.g. an integration test in awe-boot
mvn -pl awe-tests/awe-boot -am test -Dtest=MenuServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```
The browser ITs are skipped by default (`-Dskip.selenium=true`, whatever the tool); these run only the JUnit tests.

### Databases in containers (#764)
`awe-testing` ships a Testcontainers utility (`com.almis.awe.testing.database`). **PostgreSQL, MySQL, SQL Server and Oracle use it**: their `-Ppostgresql`, `-Pmysql`, `-Psqlserver`, `-Poracle` profiles and the `-flyway` variants of each start the database in a container, locally and in CI, with no external server. H2 and HSQLDB are embedded.

```bash
# Docker running (Docker Desktop, Colima...); the first run pulls the pinned image. Run Maven on JDK 21 (Lombok does not
# work on JDK 25). -Dskip.frontend=true saves the Node build.
mvn install -DskipTests -Dskip.frontend=true -pl awe-tests/awe-boot -am     # once, so awe-boot sees your modules
mvn -pl awe-tests/awe-boot -Ppostgresql test -Dskip.frontend=true          # or postgresql-flyway, mysql, mysql-flyway, oracle, oracle-flyway
AWE_TESTING_SQLSERVER_ACCEPT_EULA=true mvn -pl awe-tests/awe-boot -Psqlserver test -Dskip.frontend=true   # or sqlserver-flyway; see SQL Server EULA below
```
- Check the result is meaningful, not just green: `Tests run:` is non-zero and has no skips you cannot explain. Compare the counts with the latest develop pipeline (the `Unit · Java` and the database jobs of the same profile). The skips of `QueryTest` are the same on every engine, so a profile with more skips than the last develop run is suspect. Allow a minute or so per profile once the image is local.
- **A run that finds no test fails** (`failIfNoTests` in awe-boot). Do not trust a green build with `Tests run: 0`.
- The Flyway classes compare the versioned scripts shipped for the dialect with the successful rows of each `flyway_schema_<module>` table and query a migrated table; a database that was not migrated fails them.
- In CI the database jobs (`Database · H2`, `Database · H2 · Flyway`, `Database · HSQL · Flyway`, `Database · MySQL`, `Database · PostgreSQL`, `Database · SQL Server`, `Database · Oracle`, each of the last four with its `· Flyway` job, and `Integration · Keycloak`) extend `.database-testing`: they run only `mvn -f awe-tests/awe-boot/pom.xml verify -P<db>` with the AWE modules of the `Build · Maven` artifacts (the other modules run once, in `Unit · Java`), so a local `mvn -pl awe-tests/awe-boot -P<db> test` after `mvn install -DskipTests` reproduces them. In merge-request pipelines the first failed job cancels the rest (`workflow:auto_cancel:on_job_failure: all`; allow_failure jobs never trigger it); `develop`, `master` and `support/*` run every job (#863).
- In CI these jobs (`Database · MySQL`, `Database · PostgreSQL`, `Database · SQL Server`, `Database · Oracle` and their `· Flyway` jobs) extend `.database-containers` in `.gitlab-ci.yml`: the `docker:dind` service (one definition, anchor `&dind-service`, shared with `Package · Docker images`; the digest pin lives only there), `DOCKER_HOST`, `DOCKER_TLS_CERTDIR=""` and `TESTCONTAINERS_RYUK_DISABLED=true`. Docker Hub images are pulled through the GitLab group dependency proxy when `CI_DEPENDENCY_PROXY_*` exist; `DEPENDENCY_PROXY_DISABLED=true` pulls from Docker Hub. The job log says which registry is used on a line (`DATABASE IMAGES REGISTRY: ...`); a proxy failure shows as a failed image pull. A job that extends `.database-containers` must not define its own `before_script` (it would replace the proxy setup); include it with `!reference [.database-containers, before_script]` instead. `Database · SQL Server` and `Database · SQL Server · Flyway` set `AWE_TESTING_SQLSERVER_ACCEPT_EULA=true` in their own variables (the one place that accepts the Microsoft EULA in CI). The SQL Server image comes from `mcr.microsoft.com`, so the dependency proxy prefix does not apply to it (Testcontainers only prefixes images without a registry).
- To rehearse the CI topology on a laptop: run the dind image pinned in `.gitlab-ci.yml` (`--privileged`, `-e DOCKER_TLS_CERTDIR=`) with the name `docker` on a user network, and the CI Maven image on the same network with `-e DOCKER_HOST=tcp://docker:2375 -e TESTCONTAINERS_RYUK_DISABLED=true`.

A Spring test class opts in with one annotation; the module needs the JDBC driver of that database:
```java
@SpringBootTest
@AweDatabaseTest(TestDatabase.POSTGRESQL)                  // HSQLDB, H2, MYSQL, POSTGRESQL, SQLSERVER, ORACLE
class MyQueryIT { }
@AweDatabaseTest(value = TestDatabase.MYSQL, flyway = true) // Flyway instead of schema/data scripts
```
- It writes `spring.datasource.*`, `spring.sql.init.*` (or `spring.flyway.*` + `awe.database.migration-modules`) with precedence over `@TestPropertySource`. A `@DynamicPropertySource` method can call `AweDatabaseProperties.register(registry, db, flyway)` instead.
- Script convention: `classpath:sql/schema-<db>.sql` and `classpath:sql/testdata-<db>.sql` (as in `awe-boot`). In `awe-boot` the classes keep `@TestPropertySource("classpath:<db>.properties")` next to the annotation; that file holds only the pool settings and the validation query of the engine (the annotation supplies url, user, password, driver and scripts).
- Each container starts once per JVM and is shared by every test class. Local reuse across runs: `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` (never in CI).
- Docker must be running. Without it the tests that use `@AweDatabaseTest` fail with Testcontainers' own message (every database profile of `awe-boot` included): they are not skipped.
- There is no external-database mode: a database service in CI or a local server is not used by these tests any more. To debug against a database you already run, start a container of the same image by hand and point a throwaway `@DynamicPropertySource` at it.
- **SQL Server EULA**: the container runs only after you accept the Microsoft EULA explicitly: `-Dawe.testing.sqlserver.accept-eula=true` (or env `AWE_TESTING_SQLSERVER_ACCEPT_EULA=true`). Without it the test fails with a message saying so.
- Images are pinned `name:tag@sha256:digest` in `awe-framework/awe-testing/src/main/resources/awe-testing-images.properties`; Renovate updates that file. Oracle uses `gvenzl/oracle-free` 23 (first start is slow); the container user is `awe`, not `system`. The `oracle` and `oracle-flyway` profiles set `oracle.jdbc.J2EE13Compliant=true` and `oracle.jdbc.timezoneAsRegion=false` for the test JVM (as the CI command did): without them `testDatabaseVariableDate` and `testDatabaseQueryRequestBodyTemporalValues` fail with a `ByteArrayInputStream` serialization error. Expected counts on containers: `sqlserver` 247 tests / 7 skipped, `sqlserver-flyway` 6 / 1, `oracle` 245 / 7, `oracle-flyway` 6 / 1 (the skipped Flyway test is the optional `awe-boot` module, which ships no script for these dialects). SQL Server is amd64-only: on Apple Silicon it runs under Docker Desktop's emulation.

### IdP-first against a real Keycloak (#767)
`KeycloakIdpFirstTest` (awe-boot, tag `Keycloak`) starts the Keycloak image pinned in `awe-testing-images.properties` with the realm `awe-tests/awe-boot/src/test/resources/keycloak/awe-test-realm.json`, and AWE on a random free port. It needs Docker (skipped without it) and runs only with the `keycloak` profile:
```bash
mvn -pl awe-framework/awe-testing,awe-tests/awe-boot -Pkeycloak -Dskip.frontend=true -Dtest=KeycloakIdpFirstTest -Dsurefire.failIfNoSpecifiedTests=false test
```
Keycloak reaches AWE for the back-channel call through `host.testcontainers.internal`, or through the host in `AWE_TESTING_KEYCLOAK_BACK_CHANNEL_HOST` when the test JVM is not on the Docker host (CI job `Integration · Keycloak`).

## Output Contract

Report the exact command run (and the automation tool), the failsafe/surefire `Tests run:` summary, and `BUILD SUCCESS`/`FAILURE`. On a browser test failure, point to the screenshot/video: locally under `awe-tests/awe-boot/target/tests/selenium/screenshots/`; in GitLab CI under `browser-evidence/` in the job artifacts (linked at the end of the job log; the failed test's screenshot is also shown in the pipeline Tests tab via View details).

## References

- `.gitlab-ci.yml` — canonical CI test jobs (`IT_OPTS`, `UT_OPTS`, `IT_TOOL_OPTS` of `.playwright-testing`, `.rules-selenium`, suite `TEST_TAGS`, per-DB `TEST_NAME`).
- `website/docs/guides/selenium-test-guide.md#automation-tool-awetesttool` — `awe.test.tool`, Playwright browsers and installation.
- `awe-tests/awe-boot/pom.xml` — `spring-boot:start`/`stop` around integration-test; `skip.selenium` / `skip.junit` flags.
