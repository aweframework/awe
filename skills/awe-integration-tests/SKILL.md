---
name: awe-integration-tests
description: "Trigger: run integration tests, selenium IT, spring integration tests, awe-boot verify, run a test suite. Exact commands to run AWE's Spring integration and Selenium tests locally."
license: Apache-2.0
metadata:
  author: awe-team
  version: "1.1"
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

### Databases in containers (#764)
`awe-testing` ships a Testcontainers utility (`com.almis.awe.testing.database`). **PostgreSQL, MySQL, SQL Server and Oracle use it**: their `-Ppostgresql`, `-Pmysql`, `-Psqlserver`, `-Poracle` profiles and the `-flyway` variants of each start the database in a container, locally and in CI, with no external server. H2 and HSQLDB are embedded.

```bash
# Docker running (Docker Desktop, Colima...); the first run pulls the pinned image. Run Maven on JDK 17 or 21 (Lombok does not
# work on JDK 25). -Dskip.frontend=true saves the Node build.
mvn install -DskipTests -Dskip.frontend=true -pl awe-tests/awe-boot -am     # once, so awe-boot sees your modules
mvn -pl awe-tests/awe-boot -Ppostgresql test -Dskip.frontend=true          # or postgresql-flyway, mysql, mysql-flyway, oracle, oracle-flyway
AWE_TESTING_SQLSERVER_ACCEPT_EULA=true mvn -pl awe-tests/awe-boot -Psqlserver test -Dskip.frontend=true   # or sqlserver-flyway; see SQL Server EULA below
```
- Check the result is meaningful, not just green: `Tests run:` is non-zero and has no skips you cannot explain. Compare the counts with the latest develop pipeline (the `All UT` and the database jobs of the same profile). The skips of `QueryTest` are the same on every engine, so a profile with more skips than the last develop run is suspect. Allow a minute or so per profile once the image is local.
- **A run that finds no test fails** (`failIfNoTests` in awe-boot). Do not trust a green build with `Tests run: 0`.
- The Flyway classes compare the versioned scripts shipped for the dialect with the successful rows of each `flyway_schema_<module>` table and query a migrated table; a database that was not migrated fails them.
- In CI these jobs (`MySQL Tests`, `PostgreSQL Tests`, `SQL Server Tests`, `Oracle Tests`) extend `.database-containers` in `.gitlab-ci.yml`: the `docker:dind` service (one definition, anchor `&dind-service`, shared with `Build package`; the digest pin lives only there), `DOCKER_HOST`, `DOCKER_TLS_CERTDIR=""` and `TESTCONTAINERS_RYUK_DISABLED=true`. Docker Hub images are pulled through the GitLab group dependency proxy when `CI_DEPENDENCY_PROXY_*` exist; `DEPENDENCY_PROXY_DISABLED=true` pulls from Docker Hub. The job log says which registry is used on a line (`DATABASE IMAGES REGISTRY: ...`); a proxy failure shows as a failed image pull. A job that extends `.database-containers` must not define its own `before_script` (it would replace the proxy setup); include it with `!reference [.database-containers, before_script]` instead. `SQL Server Tests` sets `AWE_TESTING_SQLSERVER_ACCEPT_EULA=true` in its own variables (the one place that accepts the Microsoft EULA in CI). The SQL Server image comes from `mcr.microsoft.com`, so the dependency proxy prefix does not apply to it (Testcontainers only prefixes images without a registry).
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

## Output Contract

Report the exact command run, the failsafe/surefire `Tests run:` summary, and `BUILD SUCCESS`/`FAILURE`. On Selenium failure, point to the screenshot/video: locally under `awe-tests/awe-boot/target/tests/selenium/screenshots/`; in GitLab CI under `browser-evidence/` in the job artifacts (linked at the end of the job log; the failed test's screenshot is also shown in the pipeline Tests tab via View details).

## References

- `.gitlab-ci.yml` — canonical CI test jobs (`IT_OPTS`, `UT_OPTS`, suite `TEST_TAGS`, per-DB `TEST_NAME`).
- `awe-tests/awe-boot/pom.xml` — `spring-boot:start`/`stop` around integration-test; `skip.selenium` / `skip.junit` flags.
