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

Support branches must be **protected branches** in GitLab (the pattern `support/*` is
protected with the same policy as `develop`). The credentials used by `Build package`
(Docker Hub), `Deploy snapshot` (Maven repository) and the release job (API token, GPG
keys, git identity) are protected CI variables, and GitLab only injects them into
pipelines of protected refs. On an unprotected support branch those jobs fail with an
empty Docker login and a `401 Unauthorized` from the Maven repository.

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
`.0`), on whichever branch the release runs from. A `4.13.0` release on `support/4.x`
would therefore create a `version-4.13.0` docs snapshot on that branch.

Publishing those per-line docs to the public site is out of scope for this change: the
GitLab Pages job only ever publishes from `develop`/`master`, and the docs site has a
single root, so a versioned 4.x snapshot is not yet reachable online. This is tracked as
a follow-up in issues #784 and #785.

## Support window (proposed)

As proposed to management, `support/4.x` is intended to receive security and critical
fixes until FMB 21 completes its migration to AWE 5, and for at least 18 months after the
5.0 GA release, whichever is later. This is a proposal, not a committed policy; check the
issue tracker for the final decision before relying on a specific end date.
