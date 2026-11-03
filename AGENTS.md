# AGENTS.md - AWE (Almis Web Engine)

## Project Overview

AWE is a Java 21 + Spring Boot + AngularJS multi-module Maven project. It consists of:
- `awe-framework/` - Core framework (Java + AngularJS frontend)
- `awe-tests/` - Integration and Selenium tests
- `awe-samples/` - Sample applications
- `website/` - Docusaurus documentation

---

## Build & Test Commands

### Maven Commands (Java)

```bash
# Full build
mvn clean install

# Run all unit tests
mvn test

# Run all tests including integration
mvn verify

# Run single test class
mvn test -Dtest=MyTestClass

# Run single test method
mvn test -Dtest=MyTestClass#myMethod

# Run tests in specific module
mvn test -pl awe-framework/awe-model -Dtest=MyTestClass

# Run only AngularJS frontend tests (Jest)
mvn test -pl awe-framework/awe-client-angular
```

### Integration & Browser Tests

For AWE's Spring integration (DB-backed) tests and browser suites, follow the
`awe-integration-tests` skill (`skills/awe-integration-tests/SKILL.md`). The browser suites run with Playwright, as in the
merge request pipeline (Selenium only runs on master, support/* and the develop schedule):

```bash
# Browser suite with Playwright (no display needed)
mvn -f awe-tests/awe-boot/pom.xml verify \
  -Dskip.junit=true -Dskip.selenium=false -DgenerateIntegrationReport=true -Dawe.test.browser=headless-chrome \
  -Dawe.test.tool=playwright -Dawe.test.allowed-recording=false -Dgroups=SchedulerIT
```

### JavaScript Tests (Jest)

```bash
# Run Jest tests directly
npm test --prefix awe-framework/awe-client-angular

# Run Jest tests via Maven
mvn test -pl awe-framework/awe-client-angular
```

---

## Code Style Guidelines

### Java Conventions

#### Naming
- **Classes**: PascalCase (e.g., `MaintainService`, `QueryService`)
- **Methods**: camelCase (e.g., `getData()`, `saveRecord()`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Packages**: `com.almis.awe.*` (lowercase)

#### Imports
- Sorted alphabetically
- External imports first, then blank line, then internal imports
- No wildcard imports (`.*`)

#### Types & Annotations
- Use Lombok extensively to reduce boilerplate:
  - `@Data` for entities
  - `@Slf4j` for logging
  - `@Builder` for builders
  - `@Service`, `@Component`, `@Repository` for Spring beans
- Prefer `List<T>`, `Set<T>` over concrete implementations in interfaces
- Use `Optional<T>` for nullable returns

#### Error Handling
- Use custom AWE exceptions from `com.almis.awe.exception`:
  - `AWException` - General errors
  - `AWEQueryException` - Query errors
  - `AWENotFoundException` - Not found errors
  - `AWESessionException` - Session errors
  - `AWERuntimeException` - Runtime errors
- Always log with SLF4J: `log.error("message", exception)`
- Never expose stack traces to clients (use error codes)

#### Logging
- Use Lombok's `@Slf4j` on all service/controller classes
- Log levels: `trace`, `debug`, `info`, `warn`, `error`
- Never log sensitive data (passwords, tokens, PII)

---

### JavaScript Conventions

#### Naming
- **Factories/Services**: PascalCase (e.g., `DataService`, `UtilityFactory`)
- **Functions/Variables**: camelCase (e.g., `getData()`, `isValid`)
- **Constants**: UPPER_SNAKE_CASE

#### Dependency Injection
- Always use array notation for minification safety:
  ```javascript
  aweApplication.factory('MyService', ['$http', '$log', function($http, $log) {
    // ...
  }]);
  ```

#### ES6 Features
- Use `import`/`export` for modules
- Prefer `const` over `let`, avoid `var`
- Use arrow functions where appropriate

#### JSDoc
- Document all public services and factories
- Use `@ngdoc` for AngularJS specific documentation

---

### UI and Styles

For any UI, UX or styling work in either engine (screens, components, themes, CSS/LESS), follow the `awe-ui` skill
(`skills/awe-ui/SKILL.md`): the AWE 5 design system in `website/docs/design-system/`, tokens only, light and dark.

---

### Test Policy

Every CI job has to pass on a GitLab SaaS `small` runner (2 vCPU, 8 GB): the budgets below are measured there, and a
test that only passes on a bigger machine is a broken test. Tests are code a reviewer reads and a pipeline runs on every
merge request, so each one has to earn its place. The rule is **one test per rule, in the lowest layer that can prove
it**.

| Layer | Proves | Where | Budget on a small runner |
|-------|--------|-------|--------------------------|
| Unit (JUnit 5, Jest) | One rule of one class or function, through its public interface | `src/test` of the module that owns the code | < 1 s per test; no Spring context, browser or network |
| Descriptor | A screen, query, maintain or service XML parses and builds what it declares | Descriptor tests of `awe-model`, `awe-generic-screens`, `awe-boot` | < 1 s per test |
| Spring (`@SpringBootTest`, `@AweDatabaseTest`) | Wiring, SQL on a real dialect, security filters | `awe-tests/awe-boot` only, sharing the cached context (no `@DirtiesContext`, no new property set unless the rule needs it) | < 30 s per class |
| Browser (`*IT.java`, Playwright) | A user flow no other layer can observe: navigation, side panel, grid interaction | `awe-tests/awe-boot` (AngularJS) or `awe-tests/awe-boot-react` (React) | < 20 s per test, < 3 min per class, < 15 min per suite job |

Decide the layer in this order:

1. **A unit or descriptor test can prove it**: that is the only test. Searches, filters, counts, validations and
   messages are query, maintain or service rules, not browser tests.
2. **It needs the database or the Spring wiring**: one Spring test in `awe-boot` on the embedded profile; the dialect
   jobs run it on every database.
3. **It needs the browser**: one flow per screen that opens it, acts and verifies the visible result. Not one browser
   test per CRUD operation, and never a browser test to re-check what a lower layer already proves.
4. **Engine**: a browser test goes in the engine the change touches. A backend or XML change gets its flow in one
   engine, the React application by default (the AWE 5 engine), both engines only when the behavior is engine-specific.

The Playwright adapter tests of `awe-testing` are the only unit tests allowed to open a browser.

#### What not to write

- Tests that assert CSS values, class lists or computed styles. The design system is checked against the tokens in
  review (`awe-ui` skill), not by tests.
- Tests that measure time (render time, ratios, durations): they are flaky on shared runners by design.
- `pause()` or `Thread.sleep()` in browser tests: wait on a condition (`waitForLoadingBar()`, element state).
- Golden files of whole templates or screens: assert the few things the rule names (title, key, the changed element).
- A test per generated branch (Lombok accessors, builders, enum values) or a test of the framework itself.
- Duplicate coverage: the same rule as a unit, a Spring and a browser test. Keep the lowest one.

#### Budget and cleanup

- Before adding a browser test, read the duration of its suite job (`E2E · <engine> · Chromium · <suite>`) in the
  latest `develop` pipeline. A suite over 15 minutes takes no new test until one is removed or made faster.
- A feature that replaces or removes a screen removes the tests of the old screen. A redesign keeps one flow per
  screen, not the old tests plus the new ones.
- A merge request that adds tests states in its description how many it adds, in which layer, and the duration of
  the affected jobs.
- A test class over about 500 lines, or a Jest file over about 1,000 lines, is split by rule or trimmed. Shared
  fixtures live in `awe-testing` or the module test helpers, never copied between tests.
- A flaky browser test is quarantined (`@Quarantine`) with an issue, never retried until green.
- A bug fix adds the test that failed before the fix, in the lowest layer that reproduces it; a regression caught in
  the browser is reproduced in a lower layer when one exists.

### Test Conventions

#### Java Tests
- **Unit tests**: `*Test.java` in `src/test/java/`
- **Browser tests**: `*IT.java` in `awe-tests/awe-boot` and `awe-tests/awe-boot-react`, tagged with their suite
  (`SchedulerIT`, `ApplicationIntegrationIT`, `CRUDCriteriaMatrixIT`, `RegressionWebsocketPrintIT`)
- **Test class naming**: `MyServiceTest`, `MyScreenTestsIT`
- Use JUnit 5 with `@Test`, `@BeforeEach`, `@Tag("integration")`
- Use AssertJ assertions: `assertThat(result).isNotNull()`
- Spring tests extend `AbstractSpringAppIntegrationTest`

#### JavaScript Tests (Jest)
- **Location**: `src/test/jest/` (AngularJS) and `test/js/` (React)
- **Pattern**: Jest tests with `describe()`, `it()`, `beforeEach()`
- Use Angular mocks through the Jest setup bootstrap when AngularJS modules require injection

---

## Git Workflow

- **Branch naming**:
  - Features: `feature/branch-name`
  - Hotfixes: `hotfix/branch-name`
- **Commit messages**: Clear, descriptive (imperative mood)
- **All merges** must pass CI pipeline tests

---

## Linting

### ESLint
Minimal config exists. Enforce:
- Semicolons required (`"semi": 2`)
- ES2018 features supported
- No unused variables

### Spotless (Java hygiene)
Merge requests run `Spotless` on the Java files they change (unused imports, trailing whitespace, final newline, import
order; no reformatting). Before committing Java changes run:

```bash
mvn spotless:apply -Dspotless.ratchetFrom=$(git merge-base HEAD origin/develop)
```

### General
- Keep lines under 120 characters when practical
- Use meaningful variable names
- Comment complex business logic, not obvious code

---

## Project Structure

```
awe/
├── awe-framework/
│   ├── awe-model/           # Data models, exceptions
│   ├── awe-controller/      # Services, controllers
│   ├── awe-client-angular/ # AngularJS frontend
│   ├── awe-modules/        # Feature modules
│   ├── awe-starters/      # Spring Boot starters
│   └── awe-testing/       # Test utilities
├── awe-tests/
│   └── awe-boot/          # Integration tests
├── awe-samples/           # Sample apps
└── website/               # Documentation
```
