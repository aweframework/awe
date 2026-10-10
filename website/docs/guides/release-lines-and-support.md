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
| Merge request title lint (blocking) and size warning | no | no | no | yes |
| Playwright browser integration tests | yes | yes | no | yes |
| Selenium browser integration tests | weekly scheduled pipeline only | yes | yes | no |
| Sonar analysis (`sonar.branch.name` set) | yes | yes | yes | yes (MR analysis) |
| Generate javadoc & schemas | yes | yes | yes | no |
| Build package (Docker image) | yes | yes | yes | no |
| Docker floating tags (`4`, `4.12`) | no | no | yes | no |
| Deploy snapshot | yes | no | yes | no |
| Start a new release (manual) | yes | no | yes | no |
| Deploy staging / production (Kubernetes) | no | yes (prod) | no | no |
| DAST | no | yes | no | no |
| GitLab Pages (docs site) | yes | yes | no | no |
| Tag pipeline (Maven Central, milestones, release notes) | on tag | on tag | on tag | no |

The browser suites run with two tools, and both are blocking wherever they run: a failing suite
fails the pipeline, stops `Launch Sonar` (and with it the release jobs), and prevents Renovate
from automerging. A flaky test is rerun alone once and reported as flaky; a test that fails twice
fails the job, and a job is retried only when the runner fails (not on a test failure or a timeout).

- **Playwright** (jobs `Playwright IT 1/4` to `Playwright IT 4/4`: Chromium and Firefox, each one
  against the AngularJS and the React application) is the browser tool of the everyday pipeline: merge
  requests with code changes, `develop` and `master`. It does not run on `support/*`: `support/4.x`
  has no Playwright adapter.
- **Selenium** (jobs `Selenium IT 1/4` to `Selenium IT 4/4`: Chrome and Firefox, each one against
  the AngularJS and the React application) runs on
  `master`, on `support/*` and in the pipelines of the **"Weekly Check" pipeline schedule on
  `develop`**. It does not run on merge requests or on an ordinary push to `develop`. The schedule
  is a project setting in GitLab (CI/CD, Schedules), not a file of the repository; the "Renovate"
  schedule runs only the Renovate job.

Each tool runs the same four suite groups twice: against the AngularJS test application
(`awe-tests/awe-boot`) and against the React engine test application (`awe-tests/awe-boot-react`).
`Launch Sonar` waits for whichever browser jobs ran in the pipeline (both tools on `master` and in
the weekly schedule on `develop`).

Only failed tests leave evidence. Open the pipeline **Tests** tab, pick the failed test and use
**View details** to see its screenshot; the test output also links the screenshot and the video, and the
end of each job log links to the `browser-evidence/` folder in the job artifacts, where all the
screenshots and videos of that job are stored.

Tag pipeline jobs are branch-agnostic: they read the version from `$CI_COMMIT_TAG`, so
they behave the same whether the tag came from `develop` or from `support/4.x`.

Merge-request pipelines are selective: jobs are added according to the paths the merge
request touches. A change limited to `website/` builds only the documentation site; a change
limited to `awe-framework/awe-client-angular/` runs the frontend unit tests, the AngularJS
Playwright suites, Sonar and dependency scanning but not the database matrix nor the React
Playwright suites; a change limited to `awe-framework/awe-client-react/` runs the React unit
tests and lint, both sets of Playwright suites, Sonar and dependency scanning; a backend change
runs the build, the database matrix, both sets of Playwright suites, Sonar, dependency scanning
and the javadoc check, but neither the frontend unit tests nor the documentation build. A change to
`.gitlab-ci.yml` counts as touching everything; a change to the root `pom.xml` counts as a
backend change. Branch pipelines on `develop`, `master` and `support/*` always run the
complete set, except that each branch runs only the browser tools listed above. Merge-request pipelines are interruptible, so a new
push cancels the superseded pipeline automatically; pipelines on protected branches are not
cancelled. Database and browser jobs are generated from `parallel:matrix` definitions
(`Embedded DB Tests`, one job per database engine with and without Flyway, the Playwright and
Selenium browser jobs per suite), and every job has a timeout of roughly twice its observed duration.

Support branches must be **protected branches** in GitLab (the pattern `support/*` is
protected with the same policy as `develop`). The credentials used by `Build package`
(Docker Hub), `Deploy snapshot` (Maven repository) and the release job (API token, GPG
keys, git identity) are protected CI variables, and GitLab only injects them into
pipelines of protected refs. On an unprotected support branch those jobs fail with an
empty Docker login and a `401 Unauthorized` from the Maven repository.

## Merge request title and size

Four fast jobs of the `build` stage check every merge request pipeline, before anything is built.
A fifth one, [`Lint frontend`](#frontend-lint-blocking), checks the front-end code of the merge requests that change it.

### Title format (blocking)

The merge request is squash merged, so its title becomes the commit message on `develop`.
The job `MR title lint` checks it with [commitlint](https://commitlint.js.org/) and fails the
pipeline when it does not follow Conventional Commits:

```
type(scope): description (#issue Ttask)
```

- **type**: `feat`, `fix`, `chore`, `ci`, `docs`, `test`, `refactor`, `build`, `perf`, `style`
  or `revert`, in lower case. Add `!` after the type or scope for a breaking change.
- **scope**: optional and an open list: the module or area, such as `awe-client-react`,
  `awe-model`, `ci` or `deps`. Several scopes are separated by a comma.
- **description**: any case, no full stop at the end. Put the issue reference last, for example
  `(#794 T7)` for a task of an issue or `(#795)` for the issue. The header can have up to 150
  characters. A `Draft:` prefix is ignored.

```
feat(awe-model): ECharts option model for charts beside the Highcharts one (#795 T1a)
fix: guard null dates in DateUtil.asLocalTime
chore(deps): update dependency postcss to v8.5.28 (develop)
```

The titles Renovate writes already follow the format, so its merge requests pass without changes.
To fix a failing title, edit it in GitLab and retry the job.

The rules are in `.gitlab/commitlint/commitlint.config.mjs`; the commitlint versions are pinned
in `.gitlab/commitlint/package.json` and `package-lock.json`, which Renovate updates. To check a
title locally (Node.js 22.12 or later):

```bash
bin/lint-mr-title.sh "feat(awe-model): new option model (#795 T1a)"
```

### Size warning (not blocking)

The job `MR size warning` counts the lines the merge request changes (additions plus deletions)
and warns above **400**, the size a reviewer can still read with attention. It does not fail the
pipeline: the job ends with exit code 64, which is allowed to fail, and the pipeline shows a
warning. The job log lists the biggest files. Splitting the work into smaller merge requests
(one behavior with its tests and docs each) keeps reviews fast.

These files are not counted: lockfiles (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`,
`*.lock`, `skills-lock.json`), generated files (`*.min.js`, `*.min.css`, `*.svg`, `generated/`
and `target/` folders), `CHANGELOG.md`, the website documentation (`website/docs/**`,
`website/i18n/**`) and binary files. Run it locally against the target branch:

```bash
bin/mr-size.sh origin/develop   # base to compare with; head is HEAD
```

### Docs warning (not blocking)

The job `MR docs warning` is part of the definition of done: documentation changes with the code.
It warns when a merge request changes code (`awe-framework/`, `awe-samples/`, `bin/` or `pom.xml`,
without test sources, lockfiles or markdown) but changes no documentation (`website/`,
`CONTRIBUTING.md`, `README.md` or `AGENTS.md`) and does not say why none is needed. Like the size
warning, it ends with exit code 64, which is allowed to fail, so the pipeline shows a warning and
never blocks the merge.

To clear it, update the docs in the merge request, or tick one of the two items of the merge
request template: **Docs updated**, or **No docs needed because:** with the reason written after
it (the `…` of the template is not a reason). A merge request whose title is a dependency update
(`chore(deps)` or `build(deps)`) is not checked. The job reads the description through the CI
variable `CI_MERGE_REQUEST_DESCRIPTION`; when GitLab truncates a long description (it keeps 2700
characters) the items cannot be read and the job does not warn. The check is advisory: when it
cannot do its work (no base to compare with, a base commit missing from a shallow clone) it prints a
notice and ends with 0. Run it locally against the target branch (it compares with the merge base,
as a merge request does):

```bash
CI_MERGE_REQUEST_DESCRIPTION="$(cat description.md)" bin/mr-docs.sh origin/develop
```

### Translation drift warning (not blocking)

Spanish translations live in the repository (`website/i18n/es`), and a page without a translation
falls back to English. The job `MR translation drift warning` warns when a merge request changes
an English page under `website/docs` whose Spanish translation exists in
`website/i18n/es/docusaurus-plugin-content-docs/current` and does not change that translation in
the same merge request. Frozen versions are not checked. It ends with exit code 64 (allowed to
fail) or 0, like the other warnings; run it locally with:

```bash
bin/mr-translations.sh origin/develop
```

`bin/test-mr-checks.sh` tests the four scripts; the job `MR checks tests` runs it in merge
requests that change them.

## Frontend lint (blocking)

The job `Lint frontend` runs [ESLint](https://eslint.org/) on both front-end engines, the AngularJS
client (`awe-client-angular`) and the React client (`awe-client-react`), in the `build` stage, so a
failure shows in about a minute, before the unit tests start. It runs in the merge requests that
change a client and on `develop`, `master` and `support/*`.

Each client has a `lint:ci` script in its `package.json` with `--max-warnings` set to the number of
warnings it has today. An ESLint error fails the job, and so does any warning above that number:
**the warnings can only go down**. A merge request that fixes warnings lowers the number in the same
change; `npm run lint` prints the current count.

```bash
npm --prefix awe-framework/awe-client-angular run lint:ci
npm --prefix awe-framework/awe-client-react run lint:ci
```

The Maven build runs the same `lint:ci` before the unit tests, so `mvn test` fails locally with the
same limit. CI skips that Maven step (`-Dskip.lint=true`, set in `MAVEN_CLI_OPTS`), because
`Lint frontend` already ran it: lint runs once per pipeline.

## Supply chain

Every image and service referenced in `.gitlab-ci.yml` is pinned (jobs that come from
GitLab's own security templates use the analyzer images GitLab maintains): an immutable version tag where
the upstream publishes one (`maven`, `docker`, `release-cli`), or a `tag@sha256:digest` reference where only a `latest` tag exists (the
`docker-tools` images, `selenoid/firefox`, `selenoid/chrome`). The `tag` part of `tag@digest` is kept only so the reference stays
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
same Maven reactor. The React client has its own CycloneDX SBOM, generated by the `Publish npm` job
(see [npm package](#npm-package)).

`Build package` also signs the `awe-boot` and `awe-boot-react` images keylessly, using GitLab's own OIDC
identity (`id_tokens: SIGSTORE_ID_TOKEN`) instead of a stored private key: both the GitLab
registry image and the Docker Hub image are signed by digest, once, since every floating
tag (`$PROJECT_VERSION`, and on `support/*` branches the major/major.minor aliases) points
at that same digest.

The certificate identity is the pipeline definition that produced the signature
(`https://gitlab.com/aweframework/awe//.gitlab-ci.yml@refs/heads/<branch>`), so the
expression below only accepts images built from the release lines, not from a feature branch.
Verify a released image with:

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

The same commands verify `awe-boot-react`. The SBOM attached to both images is the one of the whole Maven reactor.

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
coordinate, the `docker-compose` observability stack under `awe-tests`, and the Testcontainers
database images pinned in `awe-testing-images.properties`. It does not manage the root `package.json` (an unused leftover) or the archetype
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
line: always pin an explicit version or, deliberately, one of these floating aliases. The
`awe-boot-react` image follows the same tagging.

## React test application image

`Build package` builds a second image, `awe-boot-react`, from `awe-tests/awe-boot-react`: the
test application that runs on the React engine, the counterpart of `awe-boot`. It follows the
same rules as `awe-boot` (built on `develop`, `master` and `support/*`, with the same tags, GitLab
registry `registry.gitlab.com/aweframework/awe/awe-boot-react` and Docker Hub
`aweframework/awe-boot-react`, signed by digest and carrying the SBOM attestation). A separate job,
`Build React package`, packages its jar from the modules and the client bundle built by `Build
project`. No deploy job uses this image: staging and production deploy `awe-boot` only.

## npm package

The React client is published to npm as
[`awe-react-client`](https://www.npmjs.com/package/awe-react-client) by the `Publish npm` job.
It is not published to a Maven repository.

- **When.** Only in the pipelines of release tags of the 5.x line and later (`v5.*`, `v6.*`...),
  after `Deploy Maven Central` has succeeded. A `v4.*` tag from `support/4.x` never publishes
  it, because that line has no React client.
- **Version.** The tag without the `v`: `v5.0.0` publishes `awe-react-client@5.0.0`, which is also the
  Maven version of the tagged commit. The job builds the client with that version and fails if the
  generated `dist/package.json` does not carry exactly it.
- **Dist-tags.** A prerelease version (any version with a hyphen, such as `5.1.0-rc.1`) is published
  under the `next` dist-tag, a final version under `latest`.
- **Provenance.** The package is published with `npm publish --provenance`: GitLab issues an OIDC
  token for the job (`id_tokens: SIGSTORE_ID_TOKEN`) and npm records a signed statement that links
  the version to this repository, the tag and the pipeline that built it. Check it in a project
  that installs the package with `npm audit signatures`. The `repository` field of the package must
  match the GitLab project for npm to accept the statement.
- **Credentials.** The job authenticates with the `NPM_AUTH_TOKEN` CI variable, which must be a masked and protected
  variable, available to the protected tag pipelines. It is written to a job-local `.npmrc` through the environment and
  never printed.
- **Reruns.** npm refuses to publish a version twice, so the job checks first with `npm view` and,
  when the version already exists, ends successfully without publishing again.
- **SBOM.** The job writes a CycloneDX SBOM of the runtime dependencies of the client
  (`awe-react-client-sbom.json`, from `npm sbom`) as a job artifact that does not expire. It is not
  attached to the GitLab release.

Applications moving from the 2.x line follow the
[React client upgrade guide](react-client-upgrade.md). The React archetype generates a project that
depends on the exact version of the archetype.

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
Spanish translations of the 4.x pages are not part of that injection: the page falls back to
English unless a translation exists under `website/i18n/es` on `develop`.

## Support window (proposed)

As proposed to management, `support/4.x` is intended to receive security and critical
fixes until FMB 21 completes its migration to AWE 5, and for at least 18 months after the
5.0 GA release, whichever is later. This is a proposal, not a committed policy; check the
issue tracker for the final decision before relying on a specific end date.
