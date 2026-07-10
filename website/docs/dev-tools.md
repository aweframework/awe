---
id: dev-tools
title: Dev Tools
sidebar_label: Dev Tools
---

Hot reload lets you edit an AWE application and see the change immediately, without stopping and
restarting the JVM. Frontend assets (JS, LESS, CSS) are rebuilt incrementally by Webpack, and the
AWE XML definitions (screens, queries, menus, and the rest) are reloaded in place in the running
context. It is a **development-only** mode, disabled by default, with zero production footprint.

## Quick path

```bash
npm run start:hot-reload
```

1. Run the command above in the project root (instead of `npm start`).
2. Edit a source file: a screen XML under `src/main/resources`, or a LESS/JS asset.
3. The browser refreshes automatically — for both XML definitions and rebuilt frontend bundles.

No JVM restart is involved for frontend assets or XML definitions. Java source changes still trigger
a `spring-boot-devtools` restart.

## What reloads, and how

| Change | Mechanism | Browser |
| --- | --- | --- |
| **LESS / CSS / JS** | Webpack watch rebuilds the bundle incrementally (~50 ms); the watcher detects the rebuilt bundle and broadcasts a refresh | Refreshes automatically |
| **AWE XML definitions** | The XML watcher syncs the file to the classpath and reloads the matching definitions in place | Refreshes automatically |
| **Java classes** | `spring-boot-devtools` restarts the application context | Refreshes automatically (devtools LiveReload) |

XML definitions covered: queries, maintains, services, enumerated, queues, emails, actions,
profiles, screens, locales and menus.

## How the launch command works

`npm run start:hot-reload` runs two processes in parallel with [`concurrently`](https://www.npmjs.com/package/concurrently):

```json
"start:hot-reload": "concurrently -k -n watch,app -c cyan,green \"npm run build:watch\" \"mvn spring-boot:run -Phot-reload -Dspring-boot.run.profiles=hot-reload\""
```

- `npm run build:watch` — Webpack in watch mode, the sole writer of the frontend bundles.
- `mvn spring-boot:run -Phot-reload` — the application, with the Maven `hot-reload` profile active and
  the Spring `hot-reload` profile selected.

## Configuration

Hot reload relies on three pieces, all shipped by the archetype (and by `awe-boot`):

### 1. npm scripts

```json
{
  "scripts": {
    "build:watch": "webpack --config webpack.config.js --watch --mode development",
    "start:hot-reload": "concurrently -k -n watch,app -c cyan,green \"npm run build:watch\" \"mvn spring-boot:run -Phot-reload -Dspring-boot.run.profiles=hot-reload\""
  }
}
```

`concurrently` is a `devDependency`. `build:watch` owns the frontend bundles so the application
never rebuilds them behind the watcher's back.

### 2. Maven `hot-reload` profile

The profile keeps the Webpack watcher as the **sole bundle writer** — otherwise `mvn spring-boot:run`
would run `npm ci` and a second Webpack build, colliding with the watcher and leaving the app hanging
on the splash screen. It skips the inherited production frontend build with a single property
(`skip.frontend`, bound to the frontend plugin's `skip`) and keeps `spring-boot-devtools` for Java
hot restart:

```xml
<profile>
  <id>hot-reload</id>
  <!-- Webpack watch owns the frontend bundles here: skip node install, npm ci and the
       production build in one property so Maven never overwrites the watcher's output. -->
  <properties>
    <skip.frontend>true</skip.frontend>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-devtools</artifactId>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</profile>
```

### 3. Spring `hot-reload` profile

Selected by the `start:hot-reload` script (`-Dspring-boot.run.profiles=hot-reload`), it lives in
`application-hot-reload.properties` and relaxes static-resource handling so rebuilt bundles are
served immediately, and enables the XML watcher:

```properties
# Serve rebuilt bundles immediately (no content hashing, no caching)
spring.web.resources.cache.period=0
spring.web.resources.chain.cache=false
spring.web.resources.chain.strategy.content.enabled=false

# Enable the XML watcher (off by default) and watch the source folder
awe.application.xml-hot-reload=true
awe.application.xml-hot-reload-sources=src/main/resources

# Keep spring-boot-devtools from restarting when the synchronized XML changes
spring.devtools.restart.additional-exclude=application/**
```

Keeping the production `application.properties` untouched means production builds keep their
content hashing and caching.

## Frontend hot reload (JS / LESS / CSS)

Webpack watch recompiles the bundle on every save (~50 ms). Because the `hot-reload` Spring profile
disables asset hashing and caching, the new bundle is served on the next request.

The watcher also watches the webpack output (the `specific.js` / `specific.css` bundles under
`static/`): when a bundle is rebuilt it broadcasts the same `reload-page` refresh over the WebSocket,
so the browser reloads and picks up the new bundle automatically — no manual refresh. This reuses the
XML reload broadcast, so it needs no browser extension and behaves the same for every developer. As
with XML, it requires an open WebSocket connection; when none is available you refresh manually.

> **Images.** Images imported from JS or LESS are handled by this bundle refresh — small ones are
> inlined into the bundle, larger ones are emitted by webpack and their reference changes on rebuild.
> Images served directly under `static/` and referenced by URL (for example a logo referenced from a
> screen) are not hot reloaded; refresh the browser manually after replacing one.

## XML hot reload (backend definitions)

The loop for an XML change is:

```
edit an XML file under src/main/resources
        │
        ▼
synchronized to the exploded classpath (target/classes)
        │
        ▼
validated against its schema  ──► invalid ──► logged and skipped (previous version kept)
        │
        ▼ valid
matching definitions reloaded in the running context
        │
        ▼
connected browsers refreshed automatically
```

Three safeguards keep the loop fast and safe:

- **Schema validation.** Before reloading, the changed file is validated against its XSD (the same
  schema the compile-time `xml-maven-plugin` uses). An invalid edit is reported with a clean
  `line:column` message and the reload is **skipped**, so the running application keeps the last
  valid version instead of crashing on a parse error.
- **No-op saves are ignored.** Editors often rewrite a file on save even when nothing changed.
  A content hash guard skips the reload when the saved content is identical to the last processed
  version, so a plain `Ctrl+S` does not reload.
- **Automatic browser refresh.** After a successful reload the server broadcasts a refresh over the
  existing WebSocket connection, so the browser reloads the current screen without a manual refresh.
  This requires an open WebSocket connection (AWE keeps one); when none is available the reload still
  happens and you refresh manually.

### Properties

| Property | Description | Default |
| --- | --- | --- |
| [`awe.application.xml-hot-reload`](properties#awe.application.xml-hot-reload) | Enable the XML watcher (development only) | `false` |
| [`awe.application.xml-hot-reload-sources`](properties#awe.application.xml-hot-reload-sources) | Source directories to watch; changed XML is copied to the classpath before reloading | — |
| `spring.devtools.restart.additional-exclude` | Required when `spring-boot-devtools` is present, so synchronized XML changes under `target/classes` do not trigger a full devtools restart | — |

Use `additional-exclude` (which keeps the devtools defaults), not `exclude` (which replaces them).
Adjust the `application/**` pattern if your application XML root
([`awe.application.paths.application`](properties#awe.application.paths.application)) is not the
default `/application/`.

## Production safety

XML hot reload is development-only and disabled by default:

- In **jar-packaged** deployments the XML definitions live inside jars, so the watcher has nothing to
  watch even if the property is left enabled by mistake.
- In **exploded-classpath** deployments simply do not enable the property in production; the watcher
  logs a startup `WARN` as a reminder.

## Next step

- Frontend build and client setup: [Maven and Frontend](maven).
- Full property reference: [Properties](properties).
