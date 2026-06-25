---
id: getting-started
title: Getting Started
sidebar_label: Getting Started
---

Start here if you want the fastest path from zero to a running AWE application. This guide gives you the happy path, then points you to deeper reference docs.

## Quick path

1. Install Java 17+ and Maven 3.x.
2. Choose the AngularJS or React archetype.
3. Generate your project with Maven.
4. Run the app with Spring Boot.
5. Confirm the generated application starts on `http://localhost:18080`.

## Prerequisites

- JDK 17 or higher
- Maven 3.x or higher
- Your preferred IDE or editor

## Choose your archetype

Use the frontend that matches your project.

| Option | Archetype | Use it when |
| --- | --- | --- |
| AngularJS | `awe-boot-angular-archetype` | You want the classic AWE AngularJS stack |
| React | `awe-boot-react-archetype` | You want the AWE React starter |

Use the current AWE version from Maven Central: [![Version](https://img.shields.io/maven-central/v/com.almis.awe/awe-starter-parent.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.almis.awe%22%20AND%20a:%22awe-starter-parent%22)

## Generate your project

### AngularJS project

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-angular-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

### React project

```bash
mvn -B archetype:generate \
 -DarchetypeGroupId=com.almis.awe \
 -DarchetypeArtifactId=awe-boot-react-archetype \
 -DarchetypeVersion=[Archetype version] \
 -DgroupId=com.mycompany.app \
 -DartifactId=my-app \
 -Dversion=1.0-SNAPSHOT
```

## Run the generated application

From the generated project directory:

```bash
mvn spring-boot:run
```

## What to expect

- The archetype creates a Maven-based AWE application.
- The generated app is configured to run on port `18080`.
- The default setup includes the standard AWE application structure under `src/main/resources/application/<your-acronym>/`.
- The generated configuration includes an embedded HSQLDB datasource so you can start locally without wiring an external database first.

Open `http://localhost:18080` after startup and confirm the application loads without errors.

## Next step after first run

Continue with [awe-101 Your first AWE App](training/awe-101.md) for the first hands-on tutorial. It picks up from this quick-start and shows you how to:

- understand the generated AWE folders,
- locate screens and menus,
- and make one small visible customization.

## Where to go next

Use these docs as follow-up references after the first run:

- [Project Structure](guides/project-structure.md) for the AWE XML layout and folders
- [awe-101 Your first AWE App](training/awe-101.md) for the first practical tutorial after quick-start
- [Installation](installation.md) for server and environment setup details
- [Deployment](deployment.md) for JAR, WAR, Docker, and cloud packaging options
