---
id: adr-001-react-modern-client
title: "ADR 001: React is the modern client of AWE 5"
sidebar_label: "ADR 001: React modern client"
---

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-27 |
| Scope | AWE 5.x web clients (`awe-client-react`, `awe-client-angular`) |
| Issue | #777 |

## Context

AWE has two web engines that render the same XML screen contract:

- **AngularJS** (`awe-client-angular`): the original client. Long-term support of AngularJS ended on 31 December 2021,
  so it has had no security support from its authors for years.
- **React** (`awe-client-react`): built first as a separate project, the `awe-react-client` 2.x npm package used by the
  React archetype of AWE 4.

AWE 5 needs a modern web client that is a first-class part of the framework, and a clear statement of which client new
projects should use. Many products built on AWE, including Almis' own products, use the AngularJS client today, and they
must be able to move to AWE 5 without rewriting their front end.

## Decision

1. **React is the modern client of AWE 5** and the recommended client for new projects.
2. **AngularJS stays supported in compatibility mode through the whole 5.x line.** Compatibility mode means:
   - Both engines implement the same XML screen contract. A screen feature added to the contract (a component, an
     attribute, an action, a dependency or an event) works in both engines, and a screen behaves the same in both.
   - The AngularJS client receives bug fixes, security fixes and dependency (CVE) updates for every 5.x release.
   - Work that does not change the screen contract, such as the TypeScript adoption, the state management refactors or
     performance tuning of the engine, is done in the React client only.
3. **Moving a project to React is a project decision, not a version decision.** Upgrading to AWE 5 does not require a
   change of client: an AWE 4 project on AngularJS upgrades to AWE 5 and keeps AngularJS.
4. **The module keeps its name**, `awe-client-react`. It matches the npm package `awe-react-client` and the
   `awe-boot-react` image, and a rename would change published coordinates without any benefit.

Both engines stay in AWE 5, and the framework does not rule out more engines in the future: this decision names the
recommended client, it does not replace one engine with the other.

## Why React

- **The engine already existed.** The React client was already published and used through the React archetype of AWE 4.
  Choosing modern Angular instead would have meant writing a third engine from scratch.
- **It is part of the framework now.** Since #794 the React engine lives in the AWE repository. It is built and tested in
  the same pipeline as the server, with the same Playwright and Selenium browser suites as the AngularJS engine, and it is
  published with the framework version as the `awe-react-client` npm package and as the `awe-boot-react` image (see
  [Release Lines and Support](../guides/release-lines-and-support.md)).
- **The team.** The AWE team already builds and maintains the React engine, so the modern client is the one the team can
  evolve fastest.
- **The archetype.** `awe-boot-react-archetype` already generates a React application, so new projects can start on the
  modern client today.
- **A shared design base.** Both engines use the same design tokens (#770) and the same Tailwind base with the Bootstrap 3
  compatibility layer (#755), so a screen looks the same in both. See the [design system](../design-system/overview.md).

## Consequences

- New projects should start from `awe-boot-react-archetype`. See [Getting Started](../getting-started.md).
- AWE 4 projects need no change to their client when they upgrade. See the [AWE 5 migration guide](../guides/v5-migration.md)
  and, for projects already on the React client 2.x, [Upgrading the React client](../guides/react-client-upgrade.md).
- A merge request that changes the screen contract must cover both engines, or link the issue that brings the change to
  the other engine.
- The browser suites keep running against both applications (AngularJS and React) for the whole 5.x line.

## Parity checklist

Parity is measured against the AngularJS client: components, attributes, actions, dependencies and events. This is the
state of every React parity issue of the AWE 5 plan on 2026-10-27. Like the rest of this ADR, the checklist is a dated
record and is not updated afterwards: each issue keeps its own current state.

### Done

| Issue | Scope | Merge requests |
| --- | --- | --- |
| #794 | React engine moved into the framework repository, built, tested and published with AWE 5 | !764, !778, !805, !853, !858, !859, !867, !1000 |
| #798 | Map component in the React engine (AngularJS: #882, see [Remaining gaps](#remaining-gaps)) | !1071, !1072, !1073, !1104 |
| #799 | Maximize and restore panels | !1090, !1111 |
| #800 | Formatted-text column with sanitized HTML | !1057 |
| #801 | Context menu on criteria, buttons, tabs and wizards | !1092, !1093, !1094, !1095, !1096 |
| #803 | Central keyboard shortcut registration | !1055, !1056, !1070, !1168 |
| #805 | ComponentRegistry and Redux deltas split | !1069, !1108, !1132, !1156, !1158 |
| #806 | No unnecessary re-renders from selectors | !940 |
| #807 | Redux Toolkit 2 and react-redux 9 | !1069, !1089 |
| #808 | Typed server-client contract and TypeScript adoption | !1083, !1106, !1109, !1123, !1160, !1162, !1169, !1171 |
| #809 | componentsReducer split per component family | !1108, !1127, !1128, !1129, !1130, !1131, !1155 |
| #810 | Explicit action registry and AngularJS parity test | !938 |
| #811 | Router navigate available to thunks from startup | !938 |
| #817 | Print and resize client actions | !938 |
| #818 | Dependency parity: `contains` conditions, missing events and specific targets | !1064, !1065, !1066, !1080, !1081 |
| #819 | Chart events and zoom-linked charts | !939 |
| #820 | Date formats and criteria options of the screen contract | !1018 |
| #821 | Grid, treegrid, pivot-table and file-viewer attributes | !1068, !1107, !1118, !1124, !1133, !1134, !1159 |
| #825 | Grid cells that wrap and rows that adapt their height | !1024 |
| #847 | Resizable, tag and window parity | !888, !921, !1172 |
| #868 | Audit screen layout and charts, deferred dialog select | !1067, !1084 |
| #871 | Enter in date, time and date range criteria submits the form | !1173 |
| #872 | Enter on criteria stops propagation and prevents the default action | !1168 |
| #887 | Grid heights in dialogs and flexible panels, date range layout | !1161 |
| #888 | Move grid rows with drag and drop | !1165 |
| #889 | Expanded submenu marks the open screen, not its first option | !1163 |
| #891 | Redux reducers and thunks in TypeScript | !1171 |
| #892 | Resizable split layout of grid dependencies | !1172 |

### In progress

| Issue | Scope | State |
| --- | --- | --- |
| #880 | A click in a grid cell launches every column click dependency, and `server-download` fails on a cell address | Being fixed |

### Remaining gaps

These were found while closing the checklist. They are tracked in their own issues and do not block this decision.

| Gap | Proposal |
| --- | --- |
| The `<map>` component exists only in the React engine (#798); on AngularJS a `<map>` renders nothing. | Implement it in AngularJS as described in #882, as compatibility mode requires. |
| A menu option with a `server-action` other than `screen` blocks the action queue in both engines (#879). | Fix it in the server generator, as proposed in #879; it is not specific to React. |
| The archetypes do not say which one is recommended: both Maven descriptions only name their engine. | Mark `awe-boot-react-archetype` as the recommended archetype for new projects in its description (#897). |
