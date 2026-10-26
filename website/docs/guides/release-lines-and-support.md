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
| `Security · Dependencies` | yes | yes | yes | yes |
| Merge request title lint (blocking) and size warning | no | no | no | yes |
| Playwright browser integration tests | yes (Firefox: nightly scheduled pipeline only) | yes | no | yes (Chromium only, suites of the changed engine) |
| Selenium browser integration tests | weekly scheduled pipeline only | yes | yes | no |
| Sonar analysis (`sonar.branch.name` set) | yes | yes | yes | yes (MR analysis) |
| `Docs · Javadoc & schemas` | yes | yes | yes | no |
| `Package · Docker images` (Docker image) | yes | yes | yes | no |
| Docker floating tags (`4`, `4.12`) | no | no | yes | no |
| `Deploy · Maven snapshot` | yes | no | yes | no |
| `Release · Start` (manual) | yes | no | yes | no |
| `Deploy · Staging` / `Deploy · Production` (Kubernetes) | no | yes (prod) | no | no |
| DAST | no | yes | no | no |
| GitLab Pages (docs site) | yes | yes | no | no |
| Tag pipeline (Maven Central, milestones, release notes) | on tag | on tag | on tag | no |

The browser suites run with two tools, and both are blocking wherever they run: a failing suite
fails the pipeline, stops `Analysis · Sonar` (and with it the release jobs), and prevents Renovate
from automerging. A flaky test is rerun alone once and reported as flaky; a test that fails twice
fails the job, and a job is retried only when the runner fails (not on a test failure or a timeout).

- **Playwright** (jobs named `E2E · <engine> · Chromium · <suite>`, such as `E2E · React · Chromium · Scheduler`, and
  `E2E · <engine> · Firefox · <suite>`, such as `E2E · AngularJS · Firefox · Scheduler`, each one
  against the AngularJS and the React application) is the browser tool of the everyday pipeline: merge
  requests with code changes, `develop` and `master`. It does not run on `support/*`: `support/4.x`
  has no Playwright adapter. Merge requests run only the Chromium jobs (the ones with Chromium in their
  name), and only those of the engine whose code changed: a merge request that changes only the React client
  (`awe-framework/awe-client-react`) skips the AngularJS jobs, and one that changes only the AngularJS client skips
  the React jobs. The Firefox jobs and their browser cache job run on `master` and, on `develop`, only in the
  pipelines of the **"Nightly Firefox" pipeline schedule**, which must set the variable `FIREFOX_NIGHTLY=true`
  (CI/CD, Schedules). That pipeline runs only `Build · Maven`, `Build · Firefox cache` and the eight
  Firefox jobs; an ordinary push to `develop` does not run them.
- **Selenium** (jobs named `Selenium · <engine> · Chrome · <suite>` and `Selenium · <engine> · Firefox · <suite>`,
  each one against the AngularJS and the React application) runs on
  `master`, on `support/*` and in the pipelines of the **"Weekly Check" pipeline schedule on
  `develop`**. It does not run on merge requests or on an ordinary push to `develop`. The schedule
  is a project setting in GitLab (CI/CD, Schedules), not a file of the repository; the "Renovate"
  schedule runs only the Renovate job, and the "Nightly Firefox" schedule only the Firefox Playwright jobs.

Each tool runs the same four suite groups twice: against the AngularJS test application
(`awe-tests/awe-boot`) and against the React engine test application (`awe-tests/awe-boot-react`).
`Analysis · Sonar` waits for whichever browser jobs ran in the pipeline (both tools on `master` and in
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
Chromium Playwright suites, Sonar and dependency scanning but not the database jobs nor the React
Playwright suites; a change limited to `awe-framework/awe-client-react/` runs the React unit
tests and lint, both Chromium sets of Playwright suites (AngularJS and React), Sonar and dependency scanning; a backend change
runs the build, the database jobs, both Chromium sets of Playwright suites (AngularJS and React), Sonar, dependency scanning
and the javadoc check, but neither the frontend unit tests nor the documentation build. A change to
`.gitlab-ci.yml` counts as touching everything; a change to the root `pom.xml` counts as a
backend change. Branch pipelines on `develop`, `master` and `support/*` always run the
complete set, except that each branch runs only the browser tools listed above. Merge-request pipelines are interruptible, so a new
push cancels the superseded pipeline automatically; pipelines on protected branches are not
cancelled. Database and browser jobs are explicit jobs, not `parallel:matrix` definitions, so their names say what they run:
one job per database profile (`Database · H2`, `Database · MySQL · Flyway`, ...) and one per Playwright or Selenium
browser suite, and every job has a timeout of roughly twice its observed duration.

Support branches must be **protected branches** in GitLab (the pattern `support/*` is
protected with the same policy as `develop`). The credentials used by `Package · Docker images`
(Docker Hub), `Deploy · Maven snapshot` (Maven repository) and the release job (API token, GPG
keys, git identity) are protected CI variables, and GitLab only injects them into
pipelines of protected refs. On an unprotected support branch those jobs fail with an
empty Docker login and a `401 Unauthorized` from the Maven repository.

## Merge request title and size

The `lint` stage runs fast jobs on every merge request pipeline, in parallel with the build: `MR · Docs warning`
(see [Docs warning](#docs-warning-not-blocking)), [`Lint · Markdown`](#markdown-lint) and, for the merge requests that change a client,
[`Lint · Frontend`](#frontend-lint-blocking). The title, size and translation drift checks are **not** pipeline jobs: they
are scripts you can run locally, described below.

### Title format (local check) {#title-format-blocking}

The merge request is squash merged, so its title becomes the commit message on `develop`.
It must follow Conventional Commits. The pipeline does not check it (the former `MR title lint` job was removed), so
check it yourself with [commitlint](https://commitlint.js.org/) (see below) before you open the merge request:

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
To fix a title, edit it in GitLab.

The rules are in `.gitlab/commitlint/commitlint.config.mjs`; the commitlint versions are pinned
in `.gitlab/commitlint/package.json` and `package-lock.json`, which Renovate updates. To check a
title locally (Node.js 22.12 or later):

```bash
bin/lint-mr-title.sh "feat(awe-model): new option model (#795 T1a)"
```

### Size warning (local check) {#size-warning-not-blocking}

The script `bin/mr-size.sh` counts the lines the merge request changes (additions plus deletions)
and warns above **400**, the size a reviewer can still read with attention. It is a local
command, no longer a pipeline job (`MR size warning` was removed): it ends with exit code 64 when the
size is above the limit and lists the biggest files. Splitting the work into smaller merge requests
(one behavior with its tests and docs each) keeps reviews fast.

These files are not counted: lockfiles (`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`,
`*.lock`, `skills-lock.json`), generated files (`*.min.js`, `*.min.css`, `*.svg`, `generated/`
and `target/` folders), `CHANGELOG.md`, the website documentation (`website/docs/**`,
`website/i18n/**`) and binary files. Run it locally against the target branch:

```bash
bin/mr-size.sh origin/develop   # base to compare with; head is HEAD
```

### Docs warning (not blocking)

The job `MR · Docs warning` is part of the definition of done: documentation changes with the code.
It warns when a merge request changes code (`awe-framework/`, `awe-samples/`, `bin/` or `pom.xml`,
without test sources, lockfiles or markdown) but changes no documentation (`website/`,
`CONTRIBUTING.md`, `README.md` or `AGENTS.md`) and does not say why none is needed. Like the size
warning, it ends with exit code 64, which is allowed to fail, so the pipeline shows a warning and
never blocks the merge.

To clear it, update the docs in the merge request, or tick one of the two items of the merge
request template: **Docs updated**, or **No docs needed because:** with the reason written after
it (the `…` of the template is not a reason). A merge request whose title is a dependency update
(`chore(deps)`, `build(deps)` or `fix(deps)`, which Renovate uses for runtime dependencies) is not checked. The job reads the description through the CI
variable `CI_MERGE_REQUEST_DESCRIPTION`; when GitLab truncates a long description (it keeps 2700
characters) the items cannot be read and the job does not warn. The check is advisory: when it
cannot do its work (no base to compare with, a base commit missing from a shallow clone) it prints a
notice and ends with 0. Run it locally against the target branch (it compares with the merge base,
as a merge request does):

```bash
CI_MERGE_REQUEST_DESCRIPTION="$(cat description.md)" bin/mr-docs.sh origin/develop
```

### Translation drift warning (local check) {#translation-drift-warning-not-blocking}

Spanish translations live in the repository (`website/i18n/es`), and a page without a translation
falls back to English. The script `bin/mr-translations.sh` warns when a merge request changes
an English page under `website/docs` whose Spanish translation exists in
`website/i18n/es/docusaurus-plugin-content-docs/current` and does not change that translation in
the same merge request. Frozen versions are not checked. It is a local command, no longer a
pipeline job (`MR translation drift warning` was removed); it ends with exit code 64 when a translation is stale, or 0:

```bash
bin/mr-translations.sh origin/develop
```

`bin/test-mr-checks.sh` tests the four scripts; the job `MR · Script tests` runs it in merge
requests that change them.

### Lint · Spanish docs (blocking) {#spanish-docs-lint}

Merge requests build the English site only, so the job `Lint · Spanish docs` checks the Spanish translation without
building it. It runs `npm --prefix website run check:translations` in the merge requests that change `website/`, in
a few seconds, and fails when a Spanish page under `website/i18n/es/docusaurus-plugin-content-docs/current`:

- does not compile as MDX (the report gives the file and line);
- differs from the English page of the same path: a missing or extra heading anchor, another number of fenced code
  blocks, or another front matter `id` or `slug`; a Spanish page with no English page is an error, and an English page
  without translation is not (Docusaurus falls back to English). The generated `properties.md` and `rest-api.md` are skipped;
- links to a `.md` page that exists neither in the Spanish nor in the English tree.

Run it locally with `npm --prefix website run check:translations`, optionally followed by the paths of the pages to check.

### Lint · Markdown (non-blocking) {#markdown-lint}

The job `Lint · Markdown` runs [markdownlint](https://github.com/DavidAnson/markdownlint) on the English and Spanish pages
and on the blog, in the merge requests that change a Markdown file under `website/` or its configuration
(`website/.markdownlint-cli2.jsonc`). It is allowed to fail: the pipeline shows an orange warning and the merge is never
blocked. The configuration turns off the rules that fight Docusaurus and MDX (line length, inline HTML and JSX, front
matter before the first heading, list and blank-line style), so what it reports is a real problem: heading levels that
skip a level, spaces inside emphasis or code spans, broken tables and undefined reference links. Fix what the report
shows in the pages you changed; do not mass-fix the existing ones in an unrelated merge request.

Run it locally with `npm --prefix website run lint:md`, or check only some pages with `npm --prefix website run lint:md:files -- docs/api/screens.md` (paths relative to `website/`).

## Frontend lint (blocking)

The job `Lint · Frontend` runs [ESLint](https://eslint.org/) on both front-end engines, the AngularJS
client (`awe-client-angular`) and the React client (`awe-client-react`), in the `lint` stage, so a
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
`Lint · Frontend` already ran it: lint runs once per pipeline.

## TypeScript screen contract (blocking) {#typescript-screen-contract}

The React client is typed against the data the server sends. The Java model in `awe-model` is the source of truth: the
Maven profile `contract` generates `awe-framework/awe-client-react/src/contract/contract.ts` from it (with
typescript-generator), and that file is committed. Never edit it by hand; the hand-written additions (the loose component
controller and the flat grid column) live in `src/contract/index.ts`.

After changing a class of the screen model, regenerate the contract (JDK 21) and commit the result:

```bash
mvn -Pcontract -pl awe-framework/awe-model process-classes
```

`Build · Maven` regenerates it from the classes it has just built (`bin/check-contract.sh`) and fails when the committed
file differs, with the command above in the message. `Lint · Frontend` type checks the React client against the committed
file, so a model change that breaks the client fails the merge request either at the drift check or at the type check.

## Run the pipeline locally {#run-the-pipeline-locally}

`bin/local-ci.sh` runs on your machine the jobs that the merge request pipeline would run for your branch. It reads the
`rules:changes` of `.gitlab-ci.yml` to choose them, and adds the SonarCloud coverage gate (80% of the new lines) and a
warning for a few Sonar rules. `bin/local-ci.sh --list` shows what your changes trigger and why. The browser suites run in
Chromium, as in a merge request (`--browsers firefox` adds the Firefox suites of the nightly schedule).

The unit-testing jobs (Unit · Java and the database jobs) run three at a time, each in a clone of your checkout
(`LOCAL_CI_DB_PARALLEL`, default 3; `1` runs them one after another). The `awe-boot` test context listens on a random port and
every browser suite picks free ports for its application, so runs of different checkouts do not wait for each other; only one
Oracle container, and `LOCAL_CI_BROWSER_PARALLEL` browser applications (default 2), run at a time on the machine. `--fast`
(or `LOCAL_CI_SCOPE=affected`) is an opt-in scope narrower than the pipeline: it leaves out the database jobs when no
framework module or `awe-boot` changed, the browser suites of the engine the change does not touch, and the heavy jobs for a
change of `.gitlab-ci.yml` alone. A pass with `--fast` does not predict a pass of the jobs it drops; run without it before
pushing.

It works on macOS, Linux and Windows through WSL2 (Ubuntu), not in Git Bash. Under WSL2, clone the repository inside the
WSL home, not under `/mnt/c`, where builds are many times slower. The prerequisites and the WSL2 setup are in
[`CONTRIBUTING.md`](https://gitlab.com/aweframework/awe/-/blob/develop/CONTRIBUTING.md#run-the-pipeline-locally-before-pushing).

## Supply chain

Every image and service referenced in `.gitlab-ci.yml` is pinned (jobs that come from
GitLab's own security templates use the analyzer images GitLab maintains): an immutable version tag where
the upstream publishes one (`maven`, `docker`, `release-cli`), or a `tag@sha256:digest` reference where only a `latest` tag exists (the
`docker-tools` images, `selenoid/firefox`, `selenoid/chrome`). The `tag` part of `tag@digest` is kept only so the reference stays
human-readable; the digest is what actually freezes the image. Nothing in `.gitlab-ci.yml`
tracks a floating `latest` tag. [Renovate](#dependency-updates)
is responsible for opening merge requests that bump these pins (including re-resolving the
digests), so a pin is never updated by hand-editing `latest`.

`Build · Maven` generates a CycloneDX SBOM (`target/awe-sbom.json`) for the whole Maven
reactor with the `cyclonedx-maven-plugin`. It is published as a pipeline artifact, and
`Package · Docker images` attaches that same SBOM to the `awe-boot` image pushed to the GitLab registry as
a [cosign](https://github.com/sigstore/cosign) attestation, so the SBOM travels with the
image itself, not only with the pipeline run. It is not declared as a GitLab `cyclonedx`
report on purpose: the dependency list only accepts SBOMs carrying GitLab's own CycloneDX
properties and is already fed by the `gemnasium` dependency-scanning job, which covers the
same Maven reactor. The React client has its own CycloneDX SBOM, generated by the `Deploy · npm` job
(see [npm package](#npm-package)).

`Package · Docker images` builds both images for `linux/amd64`, the platform of the GKE nodes, whatever runner takes
the job (`IMAGE_PLATFORM`), and fails before pushing an image built for another platform.

`Package · Docker images` also signs the `awe-boot` and `awe-boot-react` images keylessly, using GitLab's own OIDC
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

- **Dependency scanning** (`Security · Dependencies`) runs on merge requests
  and on `develop`/`master`/`support/*` branch pipelines, as before.
- **Secret detection** (`Security · Secrets`) runs on every merge request and every branch
  pipeline.
- **Container scanning** (`container_scanning`) runs on `develop`, `master` and
  `support/*` branch pipelines only, after `Package · Docker images` has pushed the image; it has
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
`package-lock.json` files (lock-file maintenance runs on Friday night), the
`awe-boot` Dockerfile, every `image:` and `services:` entry in `.gitlab-ci.yml`
(re-resolving `latest@sha256` digests and pinning version tags with digests), plus the
Node and npm versions of the frontend-maven-plugin and the inline CycloneDX plugin
coordinate, the `docker-compose` observability stack under `awe-tests`, and the Testcontainers
database images pinned in `awe-testing-images.properties`. It does not manage the root `package.json` (an unused leftover) or the archetype
templates.

Container images stay on their major version: for the Dockerfile base images, the `.gitlab-ci.yml` images and
services, the compose stack and the Testcontainers images, Renovate proposes digests, patch and minor updates (the
security rebuilds of the images included), never a new major. Moving to a new Java (for example `eclipse-temurin` 21
to 25) or tool major is a manual decision.

| Branch | Updates opened | Automerge |
|---|---|---|
| `develop` | Everything: major, minor, patch, pin, digest, lock file and security | Patch releases of direct Maven and npm dependencies, once they are old enough (see below), only when the merge-request pipeline passes (the approval rule is lifted for those merge requests only). Digests, pins, CI images, the Dockerfile, compose files and lock-file maintenance wait for a human |
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

Renovate does not propose a version as soon as it is published: every update waits until its version is 14 days
old, and major updates wait 30 days, so other projects find the broken releases first. When the newest version is
still too recent, Renovate proposes the newest one that is old enough (`internalChecksFilter: strict`); the waiting
versions are listed on the Dependency Dashboard. Security fixes, digests, pins and lock-file maintenance do not wait, nor do image tags whose registry gives no release date.

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
3. Create the pipeline schedule "Renovate" on `develop`, for example `0 22 * * 5` (Fridays at 22:00) in the
   Europe/Madrid timezone, with the variable `RENOVATE_RUN=true`. Lock-file maintenance
   only runs when a Renovate pipeline happens on a Friday between 22:00 and 23:59
   Europe/Madrid (`lockFileMaintenance.schedule` in `renovate.json`), so keep at least one
   weekly run inside that window.
4. Run it first with an extra schedule variable `RENOVATE_DRY_RUN=full`, read the job log,
   then remove that variable. Automerge additionally relies on two project settings that
   hold today and must stay that way: Developers are allowed to merge into `develop`, and
   "Prevent editing approval rules in merge requests" is off (Renovate lifts the approval
   rule by adding a zero-approval rule to its own merge requests). If either changes, bot
   merge requests simply stay open.
5. The `Lint · Renovate config` job runs on every merge request that touches
   `renovate.json`. Locally, from the repository root, run
   `npx --yes --package renovate -- renovate-config-validator --strict`.

## Releasing from a support branch

Releases are triggered with the manual `Release · Start` pipeline job, on a push
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

`Package · Docker images` builds a second image, `awe-boot-react`, from `awe-tests/awe-boot-react`: the
test application that runs on the React engine, the counterpart of `awe-boot`. It follows the
same rules as `awe-boot` (built on `develop`, `master` and `support/*`, with the same tags, GitLab
registry `registry.gitlab.com/aweframework/awe/awe-boot-react` and Docker Hub
`aweframework/awe-boot-react`, signed by digest and carrying the SBOM attestation). A separate job,
`Package · React jar`, packages its jar from the modules and the client bundle built by
`Build · Maven`. No deploy job uses this image: staging and production deploy `awe-boot` only.

## npm package

The React client is published to npm as
[`awe-react-client`](https://www.npmjs.com/package/awe-react-client) by the `Deploy · npm` job.
It is not published to a Maven repository.

- **When.** Only in the pipelines of release tags of the 5.x line and later (`v5.*`, `v6.*`...),
  after `Deploy · Maven Central` has succeeded. A `v4.*` tag from `support/4.x` never publishes
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
