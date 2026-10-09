# AWE documentation website

The documentation of the AWE framework, published at <https://docs.aweframework.com>. It is built with
[Docusaurus](https://docusaurus.io/) 3.8.1 (pinned in `package.json`; Renovate updates it).

## Requirements

- Node.js 18 or later (the CI image, `website-build`, uses Node.js 24) and npm.
- No other tool is needed to write or build the English documentation.

## Install and run

Run the commands from the repository root, or drop `--prefix website` and run them in this folder.

```bash
npm --prefix website ci          # install the pinned dependencies (use ci, not install: it keeps package-lock.json untouched)
npm --prefix website start       # dev server with live reload at http://localhost:3000
npm --prefix website run build   # production build into website/build (the same step as the CI job "Build website")
npm --prefix website run serve   # serve website/build locally to check the production build
```

The build treats a broken link as an error. Broken anchors (`page#section`) are warnings in the log.

## Structure

| Path | Content |
|---|---|
| `docs/` | The current documentation (AWE 5, next version). English is the source language. |
| `versioned_docs/`, `versioned_sidebars/`, `versions.json` | Frozen snapshots of released minor versions (4.8 to 4.12). They are history: do not edit them. |
| `blog/` | Blog posts. |
| `sidebars.js` | Sidebar of the current docs. |
| `src/`, `static/` | Site pages, components and static files. |
| `docusaurus.config.js` | Site configuration (locales, versions, navbar). |
| `crowdin.yml` | Translation configuration. |

The 4.x maintenance line is not edited here: it is written on the `support/4.x` branch and injected as a live
"4.x (maintenance)" version at build time by `bin/website-maintenance-docs.sh`. See
[Documentation per line](docs/guides/release-lines-and-support.md#documentation-per-line).

## Writing docs

- Edit the English pages in `docs/`. Keep the front matter (`id`, `title`) and use relative links to other pages.
- Link to a section with its heading slug (`page.md#my-heading`). Docusaurus only knows the anchors it renders itself
  (headings, list items and `<Link id>`): a raw `<a name="...">`, `<a id="...">` or `<span id="...">` is **not** one, and
  every link to it is a broken anchor. For a target that cannot be a heading (a table cell, for example) make the page
  MDX (`format: mdx` in its front matter) and use the `Anchor` component, as `docs/properties.md` does:
  `import Anchor from '@site/src/components/Anchor';` and `<Anchor id="my-anchor"/>`.
- A change in behavior is not done until its page is updated. See "Definition of done" in `CONTRIBUTING.md`.

## Translations (Crowdin)

English is the source and Spanish (`es`) is translated in [Crowdin](https://crowdin.com/): the translated files live
in `website/i18n`, which is **not** in the repository (it is git-ignored). Never edit translations by hand.

- The pipeline job `Build website with i18n` runs `npm run crowdin:sync` (`docusaurus write-translations`, then
  `crowdin upload` and `crowdin download`) on `develop` and `master`, and then builds all locales. It needs the CI
  variables `CROWDIN_PROJECT_ID` and `CROWDIN_PERSONAL_TOKEN`.
- Locally you only get English content in every locale: without `website/i18n`, the `es` site shows the English
  text. To work on the English site only, run `npm --prefix website start` (the dev server serves the default locale).

## Generated reference

The "XSD reference" (`/reference`) is generated from the schemas in `awe-framework/awe-generic-screens` by
`scripts/xsd-reference/`. It is written to the ignored `reference/` folder before every `start`, `build`,
`write-translations`, `crowdin:sync` and `docusaurus` run (npm `pre` scripts), and by `docusaurus.config.js` when it is
missing, so any other docusaurus command finds it. It is English only and is not uploaded to Crowdin.

```
$ npm run generate:reference   # regenerate it and print the attribute documentation coverage
$ npm run test:reference       # unit tests of the generator
```

To document an attribute, add an `xs:annotation/xs:documentation` to it in the XSD.

## Broken anchors

`npm --prefix website run build` prints `Docusaurus found broken anchors!` followed by the pages and anchors that do
not exist. The CI job `Build website` keeps that log and runs `bin/check-website-anchors.sh` over it, so a merge
request fails when the **current** docs have a broken anchor. Docusaurus has one `onBrokenAnchors` setting for the whole
site (it stays at `warn`), and the frozen versions (`versioned_docs` and the 4.x maintenance line) have broken anchors
that must stay as they are, so the script ignores the pages under `/docs/<version>/` (with or without a locale prefix)
and fails for any other page. To check it locally:

```bash
npm --prefix website run build > /tmp/website-build.log 2>&1
bin/check-website-anchors.sh /tmp/website-build.log
```
