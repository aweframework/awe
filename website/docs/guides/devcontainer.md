---
id: devcontainer
title: Development container
sidebar_label: Development container
---

The repository ships a [development container](https://containers.dev/) so you can build and test AWE without
installing anything on your machine except Docker and an editor that supports dev containers (VS Code with the
**Dev Containers** extension, or any tool that reads `.devcontainer/devcontainer.json`).

## What is inside

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | Temurin 21 | The CI builds on 21; the framework still targets Java 17 |
| Maven | 3.10.0 | Configured with the repository `.m2/settings.xml`, as the CI does |
| Node.js | 24.14.0 | The version the frontend-maven-plugin downloads for the build |
| Docker | docker-in-docker | Testcontainers and the Selenium docker browsers need a Docker daemon |
| Playwright browsers | Chromium and Firefox | In the version declared by `playwright.version` in `awe-framework/awe-dependencies/pom.xml`, with their system libraries |

Selenium browsers are **not** installed in the image: the Selenium tests run them as docker containers.
Nothing in the container holds credentials. `settings.xml` reads `MAVEN_REPO_USER`, `MAVEN_REPO_PASS` and the GPG
variables from the environment, so a build that only compiles and tests never needs them.

## Open the repository

1. Install Docker and the **Dev Containers** extension in VS Code.
2. Open the repository folder and run **Dev Containers: Reopen in Container**.
3. Wait for the first start: it builds the image and runs `.devcontainer/post-create.sh`, which downloads the
   Playwright browsers (about 1 GB). Next starts reuse the container.

Then build and test as usual:

```bash
mvn clean install -DskipTests
mvn test -pl awe-framework/awe-model
npm test --prefix awe-framework/awe-client-angular
```

`MAVEN_ARGS` points Maven to `.m2/settings.xml`, so you do not need `-s`. Maven profiles are not changed: the CI
excludes the `devtools` profile (`-P!devtools`); add it to your command line if you want the same.

## Ports

| Port | Used by |
| --- | --- |
| `8080` | AWE test applications (`awe-boot`, `awe-boot-react`) started with `mvn spring-boot:run` |
| `3000` | Website, with `npm start --prefix website` |

The ports are forwarded to your machine with a label. The container does not change the configuration of any
application: if port `8080` is taken on your machine, VS Code forwards it to another local port and shows which one.

## XML autocompletion

The container registers the AWE XML catalog
(`awe-framework/awe-generic-screens/src/main/resources/schemas/awe/catalog.xml`) in the **XML** extension of Red Hat,
so screens, menus, queries and the rest of the definitions get autocompletion, inline documentation and validation
without any further setup. See [XSD tooling](xsd-tooling.md) for the details and for other IDEs.

## Browser tests

Playwright tests find the browsers installed by the container. The Selenium docker browsers run on the Docker
daemon of the container (docker-in-docker); see the [Selenium test guide](selenium-test-guide.md) for how to
configure them.

## Docker inside the container

The container runs in privileged mode, because docker-in-docker needs it. Code that runs in the workspace (Maven
plugins, npm scripts, Testcontainers images) can therefore reach the Docker daemon of the container with elevated
access. This is acceptable for local development; do not open untrusted repositories or branches in this container.

## Keep it current

Renovate updates the image digest, the container features and the Node version in `.devcontainer/devcontainer.json`
with the rest of the dependencies. Node is pinned in two places, `.devcontainer/devcontainer.json` and `node.version`
in `awe-framework/awe-dependencies/pom.xml`; they must change together, and Renovate groups both in one merge request
(group `node toolchain`). If you change one by hand, change the other.

After a feature version changes in `devcontainer.json`, regenerate the lock file with the Dev Containers CLI:

```bash
npx -y @devcontainers/cli@0.89.0 upgrade --workspace-folder .
```

If you change `playwright.version` in `awe-framework/awe-dependencies/pom.xml`, rebuild the container
(**Dev Containers: Rebuild Container**) so the browsers match. `post-create.sh` accepts only a plain version there
(for example `1.63.0`) and fails with a message otherwise.
