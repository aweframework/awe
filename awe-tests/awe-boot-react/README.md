# test-react

`test-react` is the Spring Boot + AWE integration application used to validate `awe-react-client` as a real consumer.

## Recommended entry point

Use the repository root scripts instead of invoking this module manually whenever possible.

From the repository root:

```bash
npm install
npm run dev:start
```

## Module-only commands

If you are already inside `test-react`, the main commands are:

```bash
npm run build:development
npm run build:production
npm run build
npm run start
```

Legacy aliases are still supported:

```bash
npm run test-dev
npm run test-prod
```

## Full Maven build

```bash
mvn clean install
```

## Spring Boot run

```bash
mvn spring-boot:run
```

## Important

This module consumes `awe-react-client` from `file:../awe-react-client/dist`.
If you want to test the latest client changes here, rebuild and resync from the repository root:

```bash
npm run app:sync
```
