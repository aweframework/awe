---
name: awe-integration-tests
description: "Trigger: run integration tests, selenium IT, spring integration tests, awe-boot verify, run a test suite. Exact commands to run AWE's Spring integration and Selenium tests locally."
license: Apache-2.0
metadata:
  author: pablo-vidal
  version: "1.0"
---

## Activation Contract

Use when running or debugging AWE's integration tests locally: Spring/JUnit DB-backed tests, or Selenium browser suites. Commands mirror `.gitlab-ci.yml` so a local pass predicts CI.

## Hard Rules

- Selenium ITs are ordered and stateful (`t000`→`t999`); run the whole suite tag, never a single method.
- `verify` on awe-boot starts its own app (`spring-boot:start`, port 8080) and stops it after — free 8080 first; do not leave a manual `spring-boot:run` instance up.
- Prefix `xvfb-run -a` when there is no X11 `DISPLAY` (WSL / headless host): the test video recorder calls AWT `getScreenSize` and throws `HeadlessException` without a display. CI has a virtual display.
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
- Drop `xvfb-run -a` on a host with a real display.

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

## Output Contract

Report the exact command run, the failsafe/surefire `Tests run:` summary, and `BUILD SUCCESS`/`FAILURE`. On Selenium failure, point to the artifact screenshot/video under `awe-tests/awe-boot/target/tests/selenium/screenshots/`.

## References

- `.gitlab-ci.yml` — canonical CI test jobs (`IT_OPTS`, `UT_OPTS`, suite `TEST_TAGS`, per-DB `TEST_NAME`).
- `awe-tests/awe-boot/pom.xml` — `spring-boot:start`/`stop` around integration-test; `skip.selenium` / `skip.junit` flags.
