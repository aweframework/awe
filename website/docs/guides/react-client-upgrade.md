---
id: react-client-upgrade
title: Upgrading the React client
sidebar_label: Upgrading the React client
---

This guide is for applications that depend on the `awe-react-client` npm package in its 2.x line (for example, a project
generated with the React archetype of AWE 4) and move to the client published with AWE 5.

## What changed in the package

The React client now lives in the AWE repository (`awe-framework/awe-client-react`) and is released together with the
framework:

- **The package version is the framework version.** AWE `5.0.0` publishes `awe-react-client@5.0.0`, AWE `5.1.0`
  publishes `awe-react-client@5.1.0`, and so on. Before, the client had its own version line (`2.x`) that was not
  related to the framework version.
- **It is published from the release tags of AWE 5 and later** (`v5.*`), with an
  [npm provenance](https://docs.npmjs.com/generating-provenance-statements) statement that links each version to the
  pipeline and commit that built it. Prerelease versions (`-rc`, `-beta`...) are published under the `next` dist-tag,
  final versions under `latest`. See [Release Lines and Support](release-lines-and-support.md#npm-package).
- **Use the version that matches your AWE version.** The client and the server of an application are released and tested
  together. The 5.x client draws the charts from a model that only the AWE 5 server sends (see below), so do not mix it
  with an AWE 4 server.

## Upgrade steps

1. Upgrade the AWE version of your application (the `awe.version` property or the `awe-starter-parent` version in the
   `pom.xml`) to AWE 5.
2. Set the client in the `package.json` of the application to the same version, exactly:

   ```json
   "dependencies": {
     "awe-react-client": "5.0.0"
   }
   ```

   The React archetype of AWE 5 generates this dependency from its own version, so a new project starts aligned. Pin
   the exact version (or update the framework and the client together): a range such as `^5.0.0` can install a client
   newer than your server.
3. Reinstall the dependencies and commit the new lock file. The lock of a 2.x project resolves `awe-react-client` to a
   2.x version, and `npm ci` fails when it does not match `package.json`, so run `npm install` once:

   ```bash
   npm install
   ```

   If your project was generated from the archetype of AWE 5, there is no lock file yet: the first `npm install` creates
   it, and committing it makes the following builds reproducible.
4. Build the application and review the points below.

## What to review

### Charts use Apache ECharts

The React engine draws charts with [Apache ECharts](https://echarts.apache.org/) instead of Highcharts. The chart XML
does not change: the AWE 5 server translates it into an ECharts model and the client draws that model. A server that
does not send the model (an AWE 4 server) produces empty charts and a warning in the browser console.

The `highcharts` and `highcharts-react-official` dependencies are gone from the client and `echarts` 6.1.0 was added.
If your application has custom components that import Highcharts, declare it in your own `package.json` (check its
licence) or move to ECharts. The complete list of chart differences (palette, themes, 3D charts, formats, printing,
CSS rules) is in [Upgrading to AWE 5](../api/chart.md#upgrading-to-awe-5).

### HTML in cells is sanitized

The text of a grid cell (and the label of a `tag`) can carry HTML, because the `TEXT_HTML` and `MARKDOWN_HTML` transforms
of a query generate it. The React client no longer puts that HTML in the page as it comes: it sanitizes it with
[DOMPurify](https://github.com/cure53/DOMPurify) and a strict allow-list.

- **Tags kept**: `a`, `abbr`, `b`, `blockquote`, `br`, `code`, `del`, `em`, `h1` to `h6`, `hr`, `i`, `ins`, `li`, `mark`,
  `ol`, `p`, `pre`, `s`, `small`, `span`, `strong`, `sub`, `sup`, `u` and `ul`.
- **Attributes kept**: `class`, `href`, `target` and `title`. A link with `target` gets `rel="noopener noreferrer"`.
- **Removed**: scripts, frames, images, forms, styles, inline `style`, `data-*` attributes, event handlers (`onclick`,
  `onerror`...) and links to `javascript:` urls. The text inside a removed tag stays.

The labels of a `tag` follow the same rules, with the same list: none of the labels and locales of the framework, its
modules and its test applications carries markup that the list removes (they hold text, and a few use line breaks or
Markdown-like characters, which stay as text). If a label of your application uses an inline `style`, replace it with a
CSS class (`class` is kept).

A value that the server escaped (`TEXT_HTML` escapes `<` and `>`) is shown as text, tags included. If a cell of your
application showed an image, an inline style or another tag that is not in the list, use the column that renders it (for
instance an image column) or a CSS class instead.

### Dependencies of the client

The client installs its own dependencies, and several of them were updated since `awe-react-client` 2.2.5, among them
`react-router` (7.13.2 to 7.18.2), `i18next-http-backend` (2.6.1 to 4.0.2, a major version), `primereact`
(10.9.8 to 10.9.9) and `@reduxjs/toolkit` (1.9.5 to 1.9.7). If your application declares any of these packages itself,
align the versions with the ones of the client to avoid two copies in the bundle. The versions are listed in the
`package.json` of the published package, and the published runtime dependencies are also listed in the CycloneDX SBOM
kept as an artifact of the `Publish npm` job of each release tag (see
[Release Lines and Support](release-lines-and-support.md#npm-package)).

### Other changes

Between `awe-react-client` 2.2.5 and the first AWE 5 release the client also received behaviour fixes (criteria, grids,
trees, wizards, printing, the scheduler screens) found while its integration suites were migrated into the AWE
repository, plus `data-testid` attributes on its components so that browser tests do not depend on CSS classes. The
history of the 2.x line, with the merge requests of each release, is kept in
[`CHANGELOG-2.x.md`](https://gitlab.com/aweframework/awe/-/blob/master/awe-framework/awe-client-react/CHANGELOG-2.x.md).
From AWE 5 on, the changes of the client are part of the release notes of the framework.

The `2.x` versions already published stay available on npm. Applications that stay on AWE 4 keep using them: AWE 5
does not publish new `2.x` versions.
