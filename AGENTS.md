# AGENTS.md - AWE (Almis Web Engine)

## Project Overview

AWE is a Java 17 + Spring Boot + AngularJS multi-module Maven project. It consists of:
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

# Skip Java tests, run only JavaScript (Karma)
mvn test -Dskip.junit=true -Dskip.karma=false
```

### JavaScript Tests (Karma + Jasmine)

```bash
# Run Karma tests via Maven
mvn test -pl awe-framework/awe-client-angular -Dskip.junit=true -Dskip.karma=false
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

### Test Conventions

#### Java Tests
- **Unit tests**: `*Test.java` in `src/test/java/`
- **Integration tests**: `*IT.java` in `awe-tests/awe-boot`
- **Test class naming**: `MyServiceTest`, `MyControllerIT`
- Use JUnit 5 with `@Test`, `@BeforeEach`, `@Tag("integration")`
- Use AssertJ assertions: `assertThat(result).isNotNull()`
- Integration tests extend `AbstractSpringAppIntegrationTest`

#### JavaScript Tests (Jasmine)
- **Location**: `src/test/js/` directories
- **Pattern**: Spec-style with `describe()`, `it()`, `beforeEach()`
- Use Angular mocks: `angular.mock.module()`, `inject()`

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
