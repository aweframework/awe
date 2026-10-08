---
name: awe-integration-tests
description: "Trigger: run integration tests, selenium IT, spring integration tests, awe-boot verify, run a test suite. Exact commands to run AWE's Spring integration and Selenium tests locally."
license: Apache-2.0
metadata:
  author: awe-team
  version: "1.0"
---

## Activation Contract

Use when running or debugging AWE's integration tests locally: Spring/JUnit DB-backed tests, or Selenium browser suites. Commands mirror `.gitlab-ci.yml` so a local pass predicts CI.

## Hard Rules

- Selenium ITs are ordered and stateful (`t000`→`t999`); run the whole suite tag, never a single method.
- `verify` on awe-boot starts its own app (`spring-boot:start`, port 8080) and stops it after — free 8080 first; do not leave a manual `spring-boot:run` instance up.
- **Linux / WSL:** prefix `xvfb-run -a` when there is no X11 `DISPLAY`: the FFmpeg video recorder (forced by `video.properties` `recorder.type=FFMPEG`, `video.mode=ALL`) queries AWT `getScreenSize` and throws `HeadlessException` without a display. CI has a virtual display.
- **Windows:** no `xvfb-run` — the desktop session is a real display, so no `HeadlessException`. Instead `ffmpeg` must be on the `PATH` (`WindowsFFmpegRecorder` shells out to `ffmpeg gdigrab`), run the command on a single line (`\` is bash-only), and do NOT pass `-Djava.awt.headless=true`. To skip recording entirely (no ffmpeg needed), unset `video.mode` / disable recording in `video.properties`.
- Selenium Manager / WebDriverManager auto-resolves the driver for the installed `google-chrome`; no manual chromedriver needed (needs network on first run).
- Run `mvn install -DskipTests` (or `-pl <module>`) before testing so awe-boot bundles your latest module resources.

## Execution Steps

### Selenium browser ITs
```bash
xvfb-run -a mvn -f awe-tests/awe-boot/pom.xml verify \
  -Dskip.junit=true -Dskip.selenium=false -DgenerateIntegrationReport=true \
  -Dawe.test.browser=headless-chrome -Dgroups=<SUITE_TAG>
```
- `<SUITE_TAG>` (JUnit `@Tag`): `SchedulerIT`, `ApplicationIntegrationIT`, `CRUDCriteriaMatrixIT`, `RegressionWebsocketPrintIT`.
- Browser: `headless-chrome` or `headless-firefox`.
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
Selenium is skipped by default (`-Dskip.selenium=true`); these run only the JUnit tests.

### Databases in containers (slice 1 of #764: available, not yet used by the suites)
`awe-testing` ships a Testcontainers utility (`com.almis.awe.testing.database`). The existing suites still use the `-P<db>` profiles and the CI service hostnames; nothing below is wired into them yet.

A Spring test class opts in with one annotation; the module needs the JDBC driver of that database:
```java
@SpringBootTest
@AweDatabaseTest(TestDatabase.POSTGRESQL)                  // HSQLDB, H2, MYSQL, POSTGRESQL, SQLSERVER, ORACLE
class MyQueryIT { }
@AweDatabaseTest(value = TestDatabase.MYSQL, flyway = true) // Flyway instead of schema/data scripts
```
- It writes `spring.datasource.*`, `spring.sql.init.*` (or `spring.flyway.*` + `awe.database.migration-modules`) with precedence over `@TestPropertySource`. A `@DynamicPropertySource` method can call `AweDatabaseProperties.register(registry, db, flyway)` instead.
- Script convention: `classpath:sql/schema-<db>.sql` and `classpath:sql/testdata-<db>.sql` (as in `awe-boot`).
- Each container starts once per JVM and is shared by every test class. Local reuse across runs: `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` (never in CI).
- Docker must be running. Without it the container tests are skipped (`@Testcontainers(disabledWithoutDocker = true)`) or fail with Testcontainers' own message.
- **External mode** (transition, CI services): `-Ddb.external=true` (or env `DB_EXTERNAL=true`) starts no container; url/user/password come from the environment, driver and scripts still come from the annotation.
- **SQL Server EULA**: the container runs only after you accept the Microsoft EULA explicitly: `-Dawe.testing.sqlserver.accept-eula=true` (or env `AWE_TESTING_SQLSERVER_ACCEPT_EULA=true`). Without it the test fails with a message saying so.
- Images are pinned `name:tag@sha256:digest` in `awe-framework/awe-testing/src/main/resources/awe-testing-images.properties`; Renovate updates that file. Oracle uses `gvenzl/oracle-free` 23 (first start is slow); the container user is `awe`, not `system`.

## Output Contract

Report the exact command run, the failsafe/surefire `Tests run:` summary, and `BUILD SUCCESS`/`FAILURE`. On Selenium failure, point to the screenshot/video: locally under `awe-tests/awe-boot/target/tests/selenium/screenshots/`; in GitLab CI under `browser-evidence/` in the job artifacts (linked at the end of the job log; the failed test's screenshot is also shown in the pipeline Tests tab via View details).

## References

- `.gitlab-ci.yml` — canonical CI test jobs (`IT_OPTS`, `UT_OPTS`, suite `TEST_TAGS`, per-DB `TEST_NAME`).
- `awe-tests/awe-boot/pom.xml` — `spring-boot:start`/`stop` around integration-test; `skip.selenium` / `skip.junit` flags.
