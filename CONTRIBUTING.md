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
ad hoc from a feature branch.

Once `support/4.x` exists, `hotfix/*` branches keep targeting `master` for the 5.x line.
Fixes for the 4.x line are never hotfix branches against `master`; they are regular merge
requests into `support/4.x`, based on `support/4.x` and following the same review and
testing rules as any other merge request.

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

Both jobs appear in the pipeline of their branch and are started manually. On `support/*`
the version stays in the `4.x` series; once `develop` moves to `5.0.0-SNAPSHOT`, its
releases produce `5.0.0` and later versions. Tag pipelines (Maven Central deploy, release notes, GitLab release, milestone
generation) are branch-agnostic: they derive the version from the tag, never from
checking out `develop`.

`support/*` branches otherwise run the same build, test, dependency scanning, Selenium
and Sonar jobs as `develop`; they additionally publish snapshot deploys and Docker
images. See the table in
[Release lines and support](website/docs/guides/release-lines-and-support.md) for the
full per-branch pipeline breakdown.

### Docker image tags

Every build publishes an image tagged with its exact version (e.g. `4.12.10-SNAPSHOT`).
Builds from a `support/*` branch additionally get two floating aliases, the major
version (`4`) and the major.minor version (`4.12`), which are repointed to every new
build on that line, snapshot or release. There is no `latest` tag on any branch.

### Support window (proposed)

As proposed to management, `support/4.x` is intended to receive security and critical
fixes until FMB 21 completes its migration to AWE 5, and for at least 18 months after
the 5.0 GA release, whichever is later.

## Contributing guideline
- Please follow the repository's for all code and documentation.
- Create an issue and a merge request over the issue. Please fill the `features` or `bug` templates as best as possible.
- All feature branches should be based on `develop` and have the format `feature/branch_name`.
- Minor bug fixes, that is bug fixes that do not change, add, or remove any public API, should be based on `master` and have the format `hotfix/branch_name`.
- All merge requests should implement a single feature or fix a single bug. Merge Requests that involve multiple changes (it is our discretion what precisely this means) will be rejected with a reason.
- All commits should separated into logical units, i.e. unrelated changes should be in different commits within a pull request.
- Work over the new feature branch
- All new code must include unit tests. Bug fixes should have a test that fails previously and now passes. All new features should be covered. If your code does not have tests, or regresses old tests, it will be rejected.
- As you push over the project, the merge request pipeline will start. Please don't try to merge without passing all pipeline tasks
- When you finish your develop, remove the `WIP` status