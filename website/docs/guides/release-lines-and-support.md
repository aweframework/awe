---
id: release-lines-and-support
title: Release Lines and Support
sidebar_label: Release Lines and Support
---

This guide is for developers of AWE-based products and for AWE maintainers. It explains
how AWE maintains more than one major version at a time, what runs in CI on each branch,
how to release each line, and how a fix travels between lines.

## Release lines

AWE develops one major version at a time on `develop`, while previous major versions
that are still supported live on dedicated `support/<major>.x` branches.

- **`develop` / `5.x`** — the active development line. `develop` is the integration
  branch for the next `5.y.z` release; `master` holds the latest released `5.x` code.
  This is where new features and API changes land.
- **`support/4.x`** — the maintenance line for AWE 4. It was cut from `develop` at the
  moment `develop` moved on to `5.0.0-SNAPSHOT`, so it starts from the last `4.12.x`
  state of `develop` rather than from an older tag. Only security fixes, critical bug
  fixes, and dependency/CVE patches land here; no new features, no API changes.

A support branch is a normal long-lived branch, not a fork: it has its own CI pipeline,
its own releases, and its own tags, and it is retired once its support window ends.

## Versioning and tags

Every branch follows semantic versioning (`MAJOR.MINOR.PATCH`), with `-SNAPSHOT` between
releases. `develop` moves through `5.0.0-SNAPSHOT`, `5.0.1-SNAPSHOT`, etc.; `support/4.x`
moves through `4.12.10-SNAPSHOT`, `4.12.11-SNAPSHOT`, and so on. Release tags use the
`v` prefix configured in the gitflow-maven-plugin (`versionTagPrefix=v`), for example
`v4.12.10` or `v5.0.0`.

Because both lines are versioned independently, a tag alone tells you everything: a
`v4.y.z` tag always comes from `support/4.x`, and a `v5.y.z` (or later) tag always comes
from `develop`/`master`.

## What runs in CI on each branch

| Job / area | `develop` | `master` | `support/4.x` | Merge request |
|---|---|---|---|---|
| Build, unit/DB/frontend tests, javadoc | yes | yes | yes | yes |
| Dependency scanning | yes | yes | yes | yes |
| Selenium (browser) integration tests | yes | yes | yes | yes |
| Sonar analysis (`sonar.branch.name` set) | yes | yes | yes | yes (MR analysis) |
| Generate javadoc & schemas | yes | yes | yes | no |
| Build package (Docker image) | yes | yes | yes | no |
| Docker floating tags (`4`, `4.12`) | no | no | yes | no |
| Deploy snapshot | yes | no | yes | no |
| Start a new release (manual) | yes | no | yes | no |
| Deploy staging / production (Kubernetes) | no | yes (prod) | no | no |
| DAST | no | yes | no | no |
| Crowdin sync | yes | no | no | no |
| GitLab Pages (docs site) | yes | yes | no | no |
| Tag pipeline (Maven Central, milestones, release notes) | on tag | on tag | on tag | no |

Tag pipeline jobs are branch-agnostic: they read the version from `$CI_COMMIT_TAG`, so
they behave the same whether the tag came from `develop` or from `support/4.x`.

Merge-request pipelines are selective: jobs are added according to the paths the merge
request touches. A change limited to `website/` builds only the documentation site; a change
limited to `awe-framework/awe-client-angular/` runs the frontend unit tests, the Selenium
suites, Sonar and dependency scanning but not the database matrix; a backend change runs
the build, the database matrix, the Selenium suites, Sonar, dependency scanning and the
javadoc check, but neither the frontend unit tests nor the documentation build. A change to
`.gitlab-ci.yml` counts as touching everything; a change to the root `pom.xml` counts as a
backend change. Branch pipelines on `develop`, `master` and `support/*` always run the
complete set. Merge-request pipelines are interruptible, so a new
push cancels the superseded pipeline automatically; pipelines on protected branches are not
cancelled. Database and browser jobs are generated from `parallel:matrix` definitions
(`Embedded DB Tests`, one job per database engine with and without Flyway, `Firefox IT` and
`Chrome IT` per suite), and every job has a timeout of roughly twice its observed duration.

Support branches must be **protected branches** in GitLab (the pattern `support/*` is
protected with the same policy as `develop`). The credentials used by `Build package`
(Docker Hub), `Deploy snapshot` (Maven repository) and the release job (API token, GPG
keys, git identity) are protected CI variables, and GitLab only injects them into
pipelines of protected refs. On an unprotected support branch those jobs fail with an
empty Docker login and a `401 Unauthorized` from the Maven repository.

## Supply chain

Every image and service referenced in `.gitlab-ci.yml` is pinned (jobs that come from
GitLab's own security templates use the analyzer images GitLab maintains): an immutable version tag where
the upstream publishes one (`maven`, `docker`, `mysql`, `postgres`, `mssql`,
`release-cli`), or a `tag@sha256:digest` reference where only a `latest` tag exists (the
`docker-tools` images, `selenoid/firefox`, `selenoid/chrome`, `epiclabs/docker-oracle-
xe-11g`). The `tag` part of `tag@digest` is kept only so the reference stays
human-readable; the digest is what actually freezes the image. Nothing in `.gitlab-ci.yml`
tracks a floating `latest` tag. [Renovate](#dependency-updates)
is responsible for opening merge requests that bump these pins (including re-resolving the
digests), so a pin is never updated by hand-editing `latest`.

`Build project` generates a CycloneDX SBOM (`target/awe-sbom.json`) for the whole Maven
reactor with the `cyclonedx-maven-plugin`. It is published as a pipeline artifact, and `Build
package` attaches that same SBOM to the `awe-boot` image pushed to the GitLab registry as
a [cosign](https://github.com/sigstore/cosign) attestation, so the SBOM travels with the
image itself, not only with the pipeline run. It is not declared as a GitLab `cyclonedx`
report on purpose: the dependency list only accepts SBOMs carrying GitLab's own CycloneDX
properties and is already fed by the `gemnasium` dependency-scanning job, which covers the
same Maven reactor. A frontend (npm) SBOM is a follow-up, not covered yet.

`Build package` also signs the `awe-boot` image keylessly, using GitLab's own OIDC
identity (`id_tokens: SIGSTORE_ID_TOKEN`) instead of a stored private key: both the GitLab
registry image and the Docker Hub image are signed by digest, once, since every floating
tag (`$PROJECT_VERSION`, and on `support/*` branches the major/major.minor aliases) points
at that same digest. Verify a released image with:

The certificate identity is the pipeline definition that produced the signature
(`https://gitlab.com/aweframework/awe//.gitlab-ci.yml@refs/heads/<branch>`), so the
expression below only accepts images built from the release lines, not from a feature branch:

```bash
cosign verify \
  --certificate-identity-regexp '^https://gitlab.com/aweframework/awe//\.gitlab-ci\.yml@refs/heads/(develop|master|support/.+)$' \
  --certificate-oidc-issuer https://gitlab.com \
  registry.gitlab.com/aweframework/awe/awe-boot:<version>

cosign verify-attestation --type cyclonedx \
  --certificate-identity-regexp '^https://gitlab.com/aweframework/awe//\.gitlab-ci\.yml@refs/heads/(develop|master|support/.+)$' \
  --certificate-oidc-issuer https://gitlab.com \
  registry.gitlab.com/aweframework/awe/awe-boot:<version>
```

Scanners run at different points in the pipeline:

- **Dependency scanning** (`gemnasium-maven-dependency_scanning`) runs on merge requests
  and on `develop`/`master`/`support/*` branch pipelines, as before.
- **Secret detection** (`secret_detection`) runs on every merge request and every branch
  pipeline.
- **Container scanning** (`container_scanning`) runs on `develop`, `master` and
  `support/*` branch pipelines only, after `Build package` has pushed the image; it has
  `allow_failure: true` for the first weeks after adoption, so findings surface without
  blocking releases, and is tightened once triaged.

OWASP dependency-check is intentionally not used: it needs an NVD API key to run at a
practical speed, and its function is already covered by GitLab's dependency scanning plus
the CycloneDX SBOM, so adding it would only duplicate that coverage.

## Dependency updates

Dependencies are kept current by a self-hosted [Renovate](https://docs.renovatebot.com/)
bot. It runs as the `Renovate` job in `.gitlab-ci.yml`, started only by the pipeline
schedule named "Renovate", which sets `RENOVATE_RUN=true`. That pipeline contains no other
job. The configuration is `renovate.json` on `develop`: Renovate always reads the default
branch, also when it updates `support/4.x`.

**What it manages.** Maven (`pom.xml` properties, BOM imports and plugins, including the
BOM in `awe-framework/awe-dependencies/pom.xml`), npm manifests and their
`package-lock.json` files (lock-file maintenance runs on Monday early morning), the
`awe-boot` Dockerfile, every `image:` and `services:` entry in `.gitlab-ci.yml`
(re-resolving `latest@sha256` digests and pinning version tags with digests), plus the
Node and npm versions of the frontend-maven-plugin and the inline CycloneDX plugin
coordinate, and the two `docker-compose` tooling stacks under `awe-tests` (observability,
Oracle). It does not manage the root `package.json` (an unused leftover) or the archetype
templates.

| Branch | Updates opened | Automerge |
|---|---|---|
| `develop` | Everything: major, minor, patch, pin, digest, lock file and security | Patch releases of direct Maven and npm dependencies, 3 days after release, only when the merge-request pipeline passes (the approval rule is lifted for those merge requests only). Digests, pins, CI images, the Dockerfile, compose files and lock-file maintenance wait for a human |
| `support/4.x` | Patch, pin, digest and security fixes (OSV) | None: a maintainer merges |

Related updates are grouped into one merge request: Spring Boot, Spring Cloud, Selenium
(with the WebDriver manager), Maven plugins, Babel, Jest, Docusaurus, the CI images, the
docker-compose images and the Node toolchain. At most 8 bot merge requests are open at once
(security fixes are exempt from that limit). Every merge request carries the `update-dependencies` label (plus `security` for
vulnerability fixes) and a Conventional Commit message, `fix(deps)` for runtime
dependencies and `chore(deps)` otherwise. A "Dependency Dashboard" issue lists pending,
open and blocked updates; major updates of the legacy AngularJS client
(`awe-client-angular`, `awe-tools` and the `awe-tests` applications) are only opened after
someone ticks them there, since that client is being replaced in AWE 5. Security alerts come from the OSV database and cover direct
dependencies only.

Bot merge requests go through the selective merge-request pipeline: a `pom.xml` change
runs the backend and Selenium jobs, an npm change under the client runs the frontend jobs,
a `website/` change runs the website build, and a `.gitlab-ci.yml` change runs everything.
Automerge is limited to the two managers whose changes that pipeline exercises end to end;
the `awe-boot` image, for instance, is only built on branch pipelines, so its Dockerfile
pins are merged by a person.

### Setup and operations

1. Create a project access token with role Developer, scopes `api` and
   `write_repository`, and an expiry of at most one year. Rotate it before it expires.
2. Add the CI variables `RENOVATE_TOKEN` and `RENOVATE_GITHUB_COM_TOKEN`, both masked,
   protected and with **environment scope `renovate`** (the job declares that environment,
   so no other job on a protected ref receives them). The GitHub token is a personal access
   token with no scopes (public read only); Renovate needs it to download the Node build it
   uses to refresh `package-lock.json` files and to fetch changelogs. Without it GitHub's
   anonymous rate limit is hit within one run and npm merge requests arrive with a stale
   lock file, which `npm ci` rejects.
3. Create the pipeline schedule "Renovate" on `develop`, for example `0 5 * * 1-5` in the
   Europe/Madrid timezone, with the variable `RENOVATE_RUN=true`. Lock-file maintenance
   only runs when a Renovate pipeline happens on a Monday between 00:00 and 05:59
   Europe/Madrid (`lockFileMaintenance.schedule` in `renovate.json`), so keep at least one
   weekly run inside that window.
4. Run it first with an extra schedule variable `RENOVATE_DRY_RUN=full`, read the job log,
   then remove that variable. Automerge additionally relies on two project settings that
   hold today and must stay that way: Developers are allowed to merge into `develop`, and
   "Prevent editing approval rules in merge requests" is off (Renovate lifts the approval
   rule by adding a zero-approval rule to its own merge requests). If either changes, bot
   merge requests simply stay open.
5. The `Validate renovate config` job runs on every merge request that touches
   `renovate.json`. Locally, from the repository root, run
   `npx --yes --package renovate -- renovate-config-validator --strict`.

## Releasing from a support branch

Releases are triggered with the manual `Start a new release` pipeline job, on a push
pipeline for `support/4.x` (not a merge request pipeline). The job dispatches by branch:
`develop` still runs `mvn gitflow:release`; any `support/*` branch runs
`bin/support-release.sh` instead.

`bin/support-release.sh` does the following, non-interactively:

1. Verifies it is running on a `support/*` branch (from `$CI_COMMIT_REF_NAME` in CI, or
   the current branch locally) and that the working tree is clean.
2. Reads the current `-SNAPSHOT` version from `pom.xml` and computes the release version
   (the same version with `-SNAPSHOT` stripped, or the `RELEASE_VERSION` override) and
   the next patch snapshot.
3. Runs `mvn gitflow:hotfix-start -DfromBranch=support/4.x -DhotfixVersion=<version>`
   followed by `gitflow:hotfix-finish` with `-DskipMergeProdBranch` and
   `-DskipMergeDevBranch`, so the release is tagged without merging into `master` or
   `develop`. This is the gitflow-maven-plugin's hotfix flow, reused here because the
   plugin version pinned by AWE has no dedicated "release a support branch" goal.
4. Bumps the branch to the next patch `-SNAPSHOT` with `versions:set` and
   `versions:set-property` (mirroring both the `<version>` and the `<revision>`
   property, across every module `pom.xml`), commits that bump, and pushes the branch and
   the new tag together with `git push --atomic`.
5. Runs `bin/docusaurus.sh`, which cuts a versioned documentation snapshot only when the
   release is a minor or major version (`X.Y.0`); a patch release like `4.12.10` does not
   get its own docs snapshot.

After the job finishes, check:

- The new tag (`v4.y.z`) exists and its tag pipeline ran (Maven Central deploy, release
  notes, GitLab release, milestone generation).
- `support/4.x` is back on the next `-SNAPSHOT` version in every `pom.xml`.
- The Docker image for the new version was pushed, and the floating `4` / `4.12` tags
  were repointed to it (see below).

## Backporting a fix

The full policy lives in [`CONTRIBUTING.md`](https://gitlab.com/aweframework/awe/-/blob/develop/CONTRIBUTING.md):
a fix that affects both lines is born on `support/4.x`, then promoted to `develop`.

Worked example, for a null-pointer fix in the query engine:

1. Branch `fix/758-query-npe` from `support/4.x`, fix the bug, open merge request
   **"Fix NPE in QueryService#execute when filter is empty"** into `support/4.x`. Review
   and merge as usual.
2. Open a second merge request, **"[backport] Fix NPE in QueryService#execute when
   filter is empty"**, from a branch based on `develop`, cherry-picking (or merging) the
   commit from step 1. Label it `backport`. Resolve any conflicts caused by 5.x-only API
   changes directly on this merge request, and merge it into `develop`.

A fix never travels the other way (from `develop` down to `support/4.x`), unless it only
makes sense on `develop` because it depends on 5.x-only code; such fixes are not
backported at all.

## Docker image tags

Every build is tagged with its exact project version, for example
`aweframework/awe-boot:4.12.10-SNAPSHOT` or `awe-boot:5.0.0`. Builds from `support/4.x`
additionally get two floating aliases: the major version (`4`) and the major.minor
version (`4.12`). These aliases are repointed on every build of that line, including
snapshot builds between releases, so `4` and `4.12` always point at the latest `support/4.x`
build, not necessarily a released version. There is no `latest` tag on any branch or
line: always pin an explicit version or, deliberately, one of these floating aliases.

## Documentation per line

`bin/docusaurus.sh` cuts a versioned documentation snapshot (via
`yarn docusaurus docs:version`) only for minor or major releases (versions ending in
`.0`) on `develop`/`master`; it is a no-op on a `support/*` branch, since the
maintenance line publishes its docs live instead (see below). Frozen snapshots already
cut before a line moved to maintenance (`4.8.0` through `4.12.0`) stay in
`versioned_docs/` as read-only history.

The `4.x` maintenance line is published as a live "4.x (maintenance)" version, built
directly from `support/4.x` on every `develop`/`master` website build. `bin/website-
maintenance-docs.sh` fetches `support/4.x`, snapshots its `website/docs` and
`website/sidebars.js` with Docusaurus' own `docs:version` tooling, and restores
`develop`'s docs before the site builds; nothing from this injection is ever committed.
A documentation change for the 4.x line is therefore made **once**, in a merge request
against `support/4.x`, and appears on the public site automatically after the next
`develop` pipeline runs, without any change on `develop` itself (see issue #787).
Crowdin also receives the `support/4.x` sources for translation, the same way it
receives `develop`'s.

## Support window (proposed)

As proposed to management, `support/4.x` is intended to receive security and critical
fixes until FMB 21 completes its migration to AWE 5, and for at least 18 months after the
5.0 GA release, whichever is later. This is a proposal, not a committed policy; check the
issue tracker for the final decision before relying on a specific end date.
