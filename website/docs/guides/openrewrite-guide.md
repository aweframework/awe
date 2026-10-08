---
id: openrewrite
title: OpenRewrite recipes
sidebar_label: OpenRewrite
---

[OpenRewrite](https://docs.openrewrite.org/) rewrites Java sources with recipes: it can move the AWE code to Java 21
idioms or upgrade Spring Boot, and later migrate applications from AWE 4 to AWE 5. The recipes live in `rewrite.yml` at
the root of the repository and run only through the `rewrite` Maven profile, which is never active by default: the normal
build does not touch it.

## See what a recipe would change

```bash
mvn -Prewrite rewrite:dryRun
```

Nothing is modified. The goal prints the files each recipe would change and writes the changes as a patch in
`target/rewrite/rewrite.patch` (the aggregate of the reactor, at the root). Review it and apply what you want:

```bash
git apply target/rewrite/rewrite.patch
```

Add `-pl <module>` to try a single module, for example `-pl awe-framework/awe-model`. The first run downloads the recipe
jars. The modules of the reactor must be resolvable (installed in the local repository, or built in the same command).

The `Rewrite dry-run` job of the pipeline does the same on demand: it is manual and never blocks a merge request. Start
it and download the patch from the artifacts of the job.

## Active recipe and others

| Recipe | State | What it does |
|---|---|---|
| `com.almis.awe.rewrite.Java21Migration` | Active in the profile | Runs `org.openrewrite.java.migrate.UpgradeToJava21` |
| `com.almis.awe.rewrite.SpringBoot4Migration` | Defined, not active | Runs `org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0` |

Run another recipe without editing the pom:

```bash
mvn -Prewrite rewrite:dryRun -Drewrite.activeRecipes=com.almis.awe.rewrite.SpringBoot4Migration
```

List every recipe the plugin knows with `mvn -Prewrite rewrite:discover`. To apply a recipe instead of previewing it, use
`rewrite:run` on a clean branch and review the diff as any other change.

:::note
No recipe has been applied to the code yet. The versions of the plugin and of the recipe libraries are the
`plugin-rewrite.version`, `rewrite-migrate-java.version` and `rewrite-spring.version` properties of the root `pom.xml`.
:::
