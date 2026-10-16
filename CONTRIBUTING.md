# Contributing to AWE framework

:+1: :smile: First off, thanks for taking the time to contribute! :smile: :+1:

The following is a set of guidelines for contributing to AWE,
which is hosted in the [Almis Document Engine](https://gitlab.com/awe-team/awe) on GitLab.
These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to
this document in a merge request.

## Issues
If writing a bug report, please make sure it has enough info. Include all relevant information.

If requesting a feature, understand that we appreciate the input! However, it may not immediately fit our roadmap, and it may take a while for us to get to your request.

## Gitflow
We use Gitflow as our branch management system. Please follow gitflow's guidelines while contributing to the project.

- `master` holds the latest released code of the current major line.
- `develop` is the integration branch for the next release of the current major line.
- `feature/*` branches are based on `develop`.
- `hotfix/*` branches are based on `master` and target the current major line.
- `release/*` branches cut a release off `develop` before it merges to `master`.
- `support/<major>.x` branches (e.g. `support/4.x`) are long-lived maintenance lines for a
  previous major version that is no longer developed on `develop`/`master` but is still
  supported. They are configured in `pom.xml` as the gitflow-maven-plugin's
  `supportBranchPrefix`.

### Support branches

A support branch is cut once, when a new major line starts on `develop`: at that moment
`support/<major>.x` is branched off the last commit of the outgoing major line, and
`develop` is bumped to the next major `-SNAPSHOT`. Only a maintainer cuts a support
branch, using `bin/support-start.sh` (see that script's `-h` output); it is never created
ad hoc from a feature branch. The `support/*` pattern is a protected branch in GitLab with
the same policy as `develop`: protected CI variables (Docker Hub, Maven repository, GPG,
API token) are only available on protected refs, and without them the packaging, snapshot
deploy and release jobs of the support branch fail.

Once `support/4.x` exists, `hotfix/*` branches keep targeting `master` for the 5.x line.
Fixes for the 4.x line are never hotfix branches against `master`; they are regular merge
requests into `support/4.x`, based on `support/4.x` and following the same review and
testing rules as any other merge request.

Documentation for the 4.x line is edited only on `support/4.x`; it is published
automatically as the "4.x (maintenance)" version on the site after the next `develop`
pipeline runs (see [Documentation per line](website/docs/guides/release-lines-and-support.md#documentation-per-line)).

### Backport policy

A fix that affects both the current line and a support branch is born on the **oldest
affected line**. In practice, today that means:

1. Branch from `support/4.x`, fix the bug there, and open a merge request into
   `support/4.x`. It goes through the normal review and CI pipeline for that branch.
2. Once merged, promote the fix to `develop` with a **second**, separate merge request,
   labelled `backport`. Depending on how cleanly the change applies, promote it by
   cherry-picking the support-branch commit(s) or by merging the fix branch into
   `develop`; resolve any conflicts (API differences between the 4.x and 5.x lines) on
   that promoting merge request itself, not on the original one.

This flow never runs in reverse: a fix does not travel from `develop` down to
`support/4.x`. The only exception is a fix that only makes sense on `develop` because it
depends on 5.x-only code or APIs; such a fix is simply not backported.

Only the following changes are acceptable on a support branch: security fixes, critical
bug fixes, and dependency/CVE patches. New features and any change to public API are out
of scope for `support/*` and belong on `develop` instead.

### Releasing each line

| Branch | CI job | Script | Tag | Milestones |
|---|---|---|---|---|
| `develop` | `Start a new release` (manual) | `mvn gitflow:release` (develop -> master) | `vX.Y.Z` (`5.0.0`, `5.1.0`, ...) | `5.Y.Z` |
| `support/4.x` | `Start a new release` (manual) | `bin/support-release.sh` (hotfix flow, no merge to master/develop) | `vX.Y.Z` (`4.12.10`, `4.13.0`, ...) | `4.13.x` |

After a release of `develop`, bump `awe-testing.api-baseline.version` in `awe-framework/awe-testing/pom.xml` to the
version just released (it is the API compatibility baseline of `awe-testing`, checked by japicmp in the build; see
the Selenium test guide). `support/*` keeps the last 4.x release as its baseline.

Both jobs appear in the pipeline of their branch and are started manually. On `support/*`
the version stays in the `4.x` series; once `develop` moves to `5.0.0-SNAPSHOT`, its
releases produce `5.0.0` and later versions. Tag pipelines (Maven Central deploy, release notes, GitLab release, milestone
generation) are branch-agnostic: they derive the version from the tag, never from
checking out `develop`.

`support/*` branches otherwise run the same build, test, dependency scanning, Selenium
and Sonar jobs as `develop`; they additionally publish snapshot deploys and Docker
images. See the table in
[Release lines and support](website/docs/guides/release-lines-and-support.md) for the
full per-branch pipeline breakdown. Pins and dependency versions
are updated by the Renovate bot through merge requests (see
[Dependency updates](website/docs/guides/release-lines-and-support.md#dependency-updates)).
On `develop`, patch releases of direct Maven and npm dependencies automerge once the
merge-request pipeline passes and everything else is merged by a person; on `support/4.x`
only patch, pin, digest and security updates are opened and a maintainer merges them. Never bump a pinned
image by hand-editing a `latest` tag.

### Docker image tags

Every build publishes an image tagged with its exact version (e.g. `4.12.10-SNAPSHOT`).
Builds from a `support/*` branch additionally get two floating aliases, the major
version (`4`) and the major.minor version (`4.12`), which are repointed to every new
build on that line, snapshot or release. There is no `latest` tag on any branch.

### Support window (proposed)

As proposed to management, `support/4.x` is intended to receive security and critical
fixes until FMB 21 completes its migration to AWE 5, and for at least 18 months after
the 5.0 GA release, whichever is later.

## Code hygiene (Spotless)

Merge requests that change Java code run the `Spotless` job. It checks only the Java files your merge request changes (legacy
files are never flagged) for unused imports, trailing whitespace, a final newline and the import order (other imports, then
`javax`/`java`, then static imports). It never reformats or wraps code. To fix what it reports:

```bash
mvn spotless:apply -Dspotless.ratchetFrom=$(git merge-base HEAD origin/develop)   # fixes only the files you changed
mvn spotless:check -Dspotless.ratchetFrom=$(git merge-base HEAD origin/develop)   # the same check as the CI job (use your MR's target branch)
```

Without `-Dspotless.ratchetFrom` the goals run over every Java file. Spotless is not part of the normal build. The
`.editorconfig` at the root sets the same basics (2 spaces, LF, final newline) for editors.

## Database integration tests

The PostgreSQL, MySQL, SQL Server and Oracle tests of `awe-tests/awe-boot` (and their Flyway variants) start their database in a
[Testcontainers](https://testcontainers.com) container, so all you need is a running Docker (Docker Desktop, Colima or similar) and JDK 21; no database server to install and no
connection settings to change.
Install the modules once, then run a profile of the database you changed:

```bash
mvn install -DskipTests -Dskip.frontend=true -pl awe-tests/awe-boot -am
mvn -pl awe-tests/awe-boot -Ppostgresql test -Dskip.frontend=true   # postgresql-flyway, mysql, mysql-flyway, oracle, oracle-flyway
```

SQL Server (`-Psqlserver`, `-Psqlserver-flyway`) needs you to accept the Microsoft SQL Server licence explicitly:
`AWE_TESTING_SQLSERVER_ACCEPT_EULA=true mvn -pl awe-tests/awe-boot -Psqlserver test -Dskip.frontend=true`. The image is amd64-only,
so on Apple Silicon it runs under Docker Desktop's x86 emulation (it worked on Docker Desktop 27, a bit slower); the Oracle image
is large (about 6 GB), so the first run spends most of its time pulling it.

The pipeline runs the same profiles in the `MySQL Tests`, `PostgreSQL Tests`, `SQL Server Tests` and `Oracle Tests` jobs, with
Docker-in-Docker. H2 and HSQLDB run embedded. Like the commands above, these jobs run only `awe-tests/awe-boot`, with the AWE modules
that `Build project` installed: the other modules are tested once, by `All UT`. In a merge request the first failed job cancels
the rest of the pipeline, so fix it and push again; `develop`, `master` and `support/*` always run every job (#863). The
[`awe-integration-tests` skill](skills/awe-integration-tests/SKILL.md) has the details (expected counts, CI setup).

Where the pieces are: the annotation `@AweDatabaseTest` and the enum `TestDatabase` live in `awe-framework/awe-testing`
(`com.almis.awe.testing.database`), and the pinned images in
`awe-framework/awe-testing/src/main/resources/awe-testing-images.properties`. The
[Testing against each database](website/docs/supported-databases.md#testing-against-each-database) page of the docs shows the
annotation and the image table; the skill above has the CI setup of the `.database-containers` template.

## Browser tests: flaky tests, quarantine and guards

The browser suites (Playwright and Selenium, against `awe-tests/awe-boot` and `awe-tests/awe-boot-react`) block the pipeline,
so a flaky test is handled in three steps, all described in
[Flaky tests: rerun, chains and quarantine](website/docs/guides/selenium-test-guide.md#flaky-tests-rerun-chains-and-quarantine):

1. **Rerun**: a failed test of an independent class runs once more alone (failsafe `rerunFailingTestsCount`, Maven property
   `it.rerun-count`). If it passes it is reported as flaky: look for `Flakes:` in the job log and open an issue.
2. **Quarantine**: put `@Quarantine(issue = "#812", reason = "...")` (`com.almis.awe.testing.annotations`) on the test method.
   The blocking jobs leave it out (`it.excluded-groups`) and the jobs `Quarantine · AngularJS`, `Quarantine · React` and, for Selenium, `Quarantine · AngularJS · Chrome` and
   `Quarantine · React · Chrome` run
   it with `allow_failure: true`, so a failure there is a warning and its report is in the job artifacts.
3. **Get it out**: fix the cause, delete the annotation and close the issue in the same merge request; the merge request
   pipeline then runs the test as a blocking one.

To reproduce locally what the quarantine jobs run, add `-Dgroups=quarantine -Dit.excluded-groups= -Dit.rerun-count=0` to the
Selenium command of the `awe-integration-tests` skill.

Guard tests keep these rules from rotting (`BrowserTestDeclarationsGuardTest`, `BrowserTestSourcesGuardTest`,
`CiBrowserClassesGuardTest`); see [CI guard tests](website/docs/guides/selenium-test-guide.md#ci-guard-tests). Run one with
`mvn test -pl awe-tests/awe-boot-react -Dtest=CiBrowserClassesGuardTest -Dskip.frontend=true`.

## Merge request checks

Merge requests get fast jobs in the `lint` stage, which runs in parallel with the build instead of waiting for it. Blocking ones fail the pipeline; warnings end
with exit code 64, which is allowed to fail, so the pipeline shows an orange warning and the merge is never blocked.

Three checks are **not** pipeline jobs, only local commands you can run before you open the merge request: the title, the size
and the translation drift.

| Local check (not a pipeline job) | Checks | Run it locally |
|---|---|---|
| Title | Title follows Conventional Commits (`type(scope): description (#issue Ttask)`) | `bin/lint-mr-title.sh "feat: x (#1)"` (Node 22.12+) |
| Size | More than 400 changed lines (lockfiles, generated files, `website/docs`, `website/i18n` and binaries excluded) | `bin/mr-size.sh origin/develop` |
| Translation drift | An English page changed whose Spanish translation in `website/i18n/es` did not | `bin/mr-translations.sh origin/develop` |

| Job | Blocks? | Checks | Run it locally |
|---|---|---|---|
| `MR docs warning` | warning | Code changed with no docs change and neither "Docs updated" nor "No docs needed because: ..." ticked in the description | `CI_MERGE_REQUEST_DESCRIPTION="$(cat description.md)" bin/mr-docs.sh origin/develop` |
| `MR checks tests` | yes (only runs when those scripts, `.gitlab-ci.yml` or `.gitlab/commitlint` change) | The tests of the four scripts above (`bin/lint-mr-title.sh`, `bin/mr-size.sh`, `bin/mr-docs.sh`, `bin/mr-translations.sh`) and of `bin/check-website-anchors.sh` | `sh bin/test-mr-checks.sh` |
| `Markdown lint` | warning (allowed to fail; when a Markdown file under `website/` changes) | markdownlint of the English and Spanish pages and the blog: heading levels, spaces in emphasis or code, broken tables, undefined references (rules that fight Docusaurus/MDX are off) | `npm --prefix website run lint:md` (after `npm --prefix website ci`) |
| `Spotless` | yes (when Java or the Maven reactor changes) | Unused imports, trailing whitespace, final newline and import order in the Java files you changed | `mvn spotless:check -Dspotless.ratchetFrom=$(git merge-base HEAD origin/develop)`; `spotless:apply` fixes it |
| `Lint frontend` | yes (when a client changes) | ESLint of `awe-client-angular` and `awe-client-react`; errors, or more warnings than the `--max-warnings` of `lint:ci`, fail | `npm --prefix awe-framework/awe-client-angular run lint:ci` and the same for `awe-client-react` (after `npm ci`) |
| `Build website` | yes (when `website/` changes) | Unit tests of the website scripts, the site builds, and no broken anchor in the current docs (versioned docs excluded) | see below |

Run the website check as CI does (the build log is read by the anchors script; the `bin/website-maintenance-docs.sh` step of
CI, which injects the 4.x docs, is optional locally):

```bash
npm --prefix website ci
npm --prefix website run test:scripts
npm --prefix website run build > website-build.log 2>&1
bin/check-website-anchors.sh website-build.log
```

The title rules, the size exclusions, the docs and translation checks, the markdown lint and the frontend lint are explained in
[Merge request title and size](website/docs/guides/release-lines-and-support.md#merge-request-title-and-size) and
[Frontend lint](website/docs/guides/release-lines-and-support.md#frontend-lint-blocking). Which jobs a merge request gets
depends on the paths it touches (see
[What runs in CI on each branch](website/docs/guides/release-lines-and-support.md#what-runs-in-ci-on-each-branch)).

## Run the pipeline locally before pushing

Pipeline minutes are limited: run `bin/local-ci.sh` before you push. It runs the jobs that the merge request pipeline would
run for your branch, selected with the `rules:changes` globs read from `.gitlab-ci.yml`, and adds what SonarCloud would fail:
the coverage of the new lines (lines and conditions, 80% like the quality gate, from the JaCoCo and Jest reports) and a
warning for a few Sonar rules (S1192, S125, S5778, S1130) in the changed lines.

```bash
bin/local-ci.sh --list                                    # which jobs your changes trigger, and why
bin/local-ci.sh --title "feat(awe-model): x (#123 T1)"    # everything they trigger
bin/local-ci.sh --no-browser --no-db-containers           # without the Playwright suites and the Docker databases
bin/local-ci.sh --only all-ut,coverage                    # some jobs only (ids from --list)
bin/local-ci.sh --only playwright-react --suites scheduler                      # one browser suite
bin/local-ci.sh --only playwright-angular --browsers chromium,firefox            # also the Firefox suites of develop
```

It runs on macOS, Linux and Windows through WSL2. It needs git, Node.js 22.12+ with npm, Maven and the JDK of the build, 21 (found under
`~/Library/Java/JavaVirtualMachines`, `/Library/Java/JavaVirtualMachines`, `/usr/lib/jvm`, `~/.sdkman` or `~/.jdks`, or set
`LOCAL_CI_JDK21`), Docker for the database jobs, `lsof` or `ss` (iproute2) to check the ports of the browser suites, and on
Linux the system libraries of the Playwright browsers; it names what is missing and how to install it. The browser suites
run in Chromium, as in a merge request. Every JVM runs headless and everything it starts is stopped at the end, also on Ctrl-C.
One run per checkout is allowed. Runs of different checkouts no longer wait for each other for ports: the `awe-boot` test
context listens on a random port, and every browser suite starts its application, its JMX port (`spring-boot:start`) and its
JaCoCo port on free ports that the script picks at that moment (never 8080). The machine only limits two things, with locks
shared by all checkouts: one Oracle container at a time (`${TMPDIR:-/tmp}/awe-local-ci-oracle.lock`, or `LOCAL_CI_ORACLE_LOCK`)
and `LOCAL_CI_BROWSER_PARALLEL` browser applications at a time (default 2, about 2 GB each; slot locks
`${TMPDIR:-/tmp}/awe-local-ci-browser.<n>.lock`, or `LOCAL_CI_BROWSER_LOCK`). Both waits last at most `LOCAL_CI_PORTS_WAIT`
seconds (default 7200), polling every `LOCAL_CI_PORTS_POLL` seconds (default 10); once the Oracle wait times out in one job, the other
Oracle jobs of that run (even those running at the same time) fail at once with the same reason, shared through a file of the run. A live holder is never taken over. When a run is interrupted, its workers get `LOCAL_CI_POOL_GRACE` seconds (default 25) to stop their Maven runs; whatever is left is killed. Logs and the summary go to
`target/local-ci/<timestamp>/`. It exits with 1 when a blocking job fails; warnings (exit code 64 in CI) do not fail it. It
does not replace SonarCloud or dependency scanning, which only run in GitLab; secret detection is mirrored, as a warning, only
when [gitleaks](https://github.com/gitleaks/gitleaks) is installed (the analyzer of GitLab's job).

### Faster runs

- **Parallel unit-testing jobs.** All UT, the three Embedded DB profiles and the eight database profiles (12 rows) used to run one
  after another, 4-6 minutes each. They now run `LOCAL_CI_DB_PARALLEL` at a time (default 3; `1` restores one after another in
  your checkout). Three fits a 24 GB laptop: each job is a Maven JVM with the Spring test contexts (about 2 GB) plus, for the
  database jobs, a container (MySQL and PostgreSQL are small, SQL Server and Oracle take 2 GB or more), and Docker needs room
  of its own; raise it on more memory, lower it if the machine swaps. The default is the same set of jobs as the pipeline, only
  concurrent. Each job runs in a clone of your checkout (`target/local-ci/<timestamp>/work/<job>`; a clone that passed is
  deleted, a failed one is kept until the next run) so they never share a `target/` directory or the embedded HSQLDB, and brings its JaCoCo data
  back for the coverage job. The clones share the Maven repository of the checkout (read only), and the clones need
  `rsync` (it is on macOS and most Linux): without it the jobs run one at a time, in your checkout, with a warning. Oracle rows still queue on the Oracle lock. The summary keeps the order of
  the jobs. The browser suites stay one after another inside a run: they share the embedded database and `target/`.
- **`--fast` (`LOCAL_CI_SCOPE=affected`).** Opt-in and narrower than the pipeline, so a pass no longer predicts a pass of the jobs
  it drops. The default is exactly what the merge request pipeline creates. With `--fast`: a change of `.gitlab-ci.yml` alone
  does not trigger the heavy jobs (the pipeline runs them, because the file is in every `rules:changes` list); the database jobs
  run only when a framework module or `awe-boot` changed (with or without `--fast` they run only the `awe-boot` module, as the
  pipeline does); the
  AngularJS browser suites are left out for a change only in the React client, and the React suites for a change only in the
  AngularJS client or `awe-boot`. Use it while iterating, and run without it before you push. `--list --fast` shows what it
  drops and why.

### Windows (WSL2)

Run it in Ubuntu on WSL2, not in Git Bash, PowerShell or Cygwin (the script stops there):

1. Install Ubuntu: `wsl --install -d Ubuntu` in an administrator PowerShell, then open the Ubuntu terminal.
2. Install the tools: `sudo apt update && sudo apt install -y git openjdk-21-jdk maven lsof iproute2`, and Node.js 22.12 or
   later through [nvm](https://github.com/nvm-sh/nvm) or the NodeSource packages (the Ubuntu `nodejs` package is older).
3. Install the libraries of the Playwright browsers once. After the first build, the script compares the packages that
   Playwright's own `install-deps` would install for the selected browsers with the installed ones, and prints the exact
   command when some are missing: `sudo "<java>" -cp "<playwright jars>" com.microsoft.playwright.CLI install-deps chromium`
   (the CLI of the pinned Playwright version, as CI runs it). The check only advises; a step fails only if a browser cannot
   start, and its summary then repeats the command.
4. For the database jobs, install Docker Desktop on Windows and turn on its WSL integration for the Ubuntu distribution
   (Settings > Resources > WSL integration); otherwise pass `--no-db-containers`.
5. Clone the repository inside the WSL home (`~/IdeaProjects/awe`), not under `/mnt/c`: builds on the Windows drive are many
   times slower, and the script warns about it.

## XSD checks

Changes to the XSDs are protected by a catalog consistency unit test, the build validation of the application XML and the
unit tests of the XSD reference generator (`npm --prefix website run test:scripts`, already part of `Build website`). What each
one checks and how to run it is in
[Checks when you change a schema](website/docs/guides/xsd-tooling.md#checks-when-you-change-a-schema).

## Definition of done

A merge request is done when all of this is true:

- **Tests**: new behavior has unit tests, and a bug fix has a test that failed before and passes now.
  The merge request pipeline is green (check the failed jobs, not only the pipeline colour).
- **Docs**: the documentation in `website/docs` describes the new or changed behavior in the same merge
  request, or the description says why no docs are needed. The `MR docs warning` job reminds you when
  code changes and neither happens; it is a warning, not a blocker (see
  [Merge request title and size](website/docs/guides/release-lines-and-support.md#docs-warning-not-blocking)).
  The English pages are the source. Spanish translations live in `website/i18n/es` and are edited in the repository; a page without a translation shows the English one. Run `bin/mr-translations.sh origin/develop` locally to see whether you changed an English page whose translation you did not touch; the pipeline does not check it.
  When you review a page against the behavior of the code (not for a typo fix), record it in `website/scripts/docs-audit/reviews.json`: it feeds the generated *Documentation audit* page of the site (area, last review, Spanish status and notes of every page).
- **Migration guide**: a breaking change (a removed or renamed API, property, XML, database script or
  JSON field, or a changed default) has its entry in the
  [AWE 5 migration guide](website/docs/guides/v5-migration.md).
- **Review**: the checklist of the merge request template is complete, the title follows Conventional
  Commits, and a maintainer has reviewed it.

## Contributing guideline
- Please follow the repository's for all code and documentation.
- Create an issue and a merge request over the issue. Please fill the `features` or `bug` templates as best as possible.
- All feature branches should be based on `develop` and have the format `feature/branch_name`.
- Minor bug fixes, that is bug fixes that do not change, add, or remove any public API, should be based on `master` and have the format `hotfix/branch_name`.
- All merge requests should implement a single feature or fix a single bug. Merge Requests that involve multiple changes (it is our discretion what precisely this means) will be rejected with a reason.
- The title of the merge request follows Conventional Commits, `type(scope): description (#issue Ttask)`, because it becomes the commit message on `develop`; the pipeline does not check it, run `bin/lint-mr-title.sh` locally. Keep merge requests under about 400 changed lines (`bin/mr-size.sh` measures it locally). Details in [Merge request title and size](website/docs/guides/release-lines-and-support.md#merge-request-title-and-size).
- All commits should separated into logical units, i.e. unrelated changes should be in different commits within a pull request.
- Work over the new feature branch
- All new code must include unit tests. Bug fixes should have a test that fails previously and now passes. All new features should be covered. If your code does not have tests, or regresses old tests, it will be rejected.
- As you push over the project, the merge request pipeline will start. Please don't try to merge without passing all pipeline tasks
- When you finish your develop, remove the `WIP` status
- If your merge request has impacts on existing applications (a removed or renamed API, property, XML, database script or JSON field, or a changed default), add an entry to the [AWE 5 migration guide](website/docs/guides/v5-migration.md) in the same merge request.
