---
id: overview
title: AWE 5 design system
sidebar_label: Overview
---

:::caution Proposed design
This section describes the **proposed** design system of AWE 5 (issue #883). The implementation in the AngularJS and
React engines is pending. Until it lands, the [live prototype](pathname:///design-system/prototype/) is the
reference, and the current tokens are documented in [Design tokens](../guides/design-tokens-guide.md).
:::

AWE 5 gets a new visual language: new tokens, typography, density, icons, shell and components. One design system and
one token contract apply to **both** engines, so a screen defined in XML looks the same in AngularJS and in React.

## Quick path

1. Open the [prototype](pathname:///design-system/prototype/) and switch the theme with the **Theme** button.
2. Read [Color and themes](color-and-themes.md) to understand the token tiers.
3. Before you write CSS, read [For developers](for-developers.md).

## Goals

| Goal | What it means |
|------|---------------|
| A new product | AWE 5 must look like a new product, not a recoloured AWE 4: type, spacing, shapes, icons, shell and components change. |
| Both engines on par | One token contract (`--awe-*`) and the same component rules for AngularJS and React. |
| Light and dark | Every colour token has a light and a dark value. The user chooses, or the operating system decides. |
| Simple themes | A theme is a **primary** palette plus a **surface** palette. Nothing else to configure. |
| Accessible | Text meets WCAG 2.2 AA (4.5:1) in both modes and with every primary. Focus is always visible. |

## Principles

- **Tokens only.** Components read `var(--awe-*)`. No colour literals, no sizes outside the scales.
- **Content first.** Neutral surfaces, one accent colour, colour used for meaning (state, selection, action).
- **Calm density.** Comfortable by default, compact on demand, the same layout in both.
- **Same XML, new presentation.** Screens, menus and actions keep their XML. Only the rendering changes.
- **Predictable states.** Every component defines hover, focus, active, disabled, invalid, loading and empty.

## Sections

| Page | Read it when |
|------|--------------|
| [Color and themes](color-and-themes.md) | You need a colour, a theme or the light/dark model. |
| [Foundations](foundations.md) | You need type, spacing, density, radius, elevation, motion or icons. |
| [Layout and shell](layout-and-shell.md) | You work on the app frame, the menu, the header or the login page. |
| [Components](components.md) | You build or style a component, or need the rules of an AWE element. |
| [Token reference](tokens-reference.md) | You need the exact name or value of a token, or the mapping from today's tokens. |
| [For developers](for-developers.md) | You write CSS, add a component or configure a product theme. |

## Live prototype

The prototype is static HTML and CSS built on the proposed tokens. It uses a fictitious customer, *Veloria Logistics*.

| Page | Shows |
|------|-------|
| [Sample screen](pathname:///design-system/prototype/) | Shell, criteria, grid, tabs, accordion, dialogs, toasts. |
| [AWE 5 components](pathname:///design-system/prototype/components.html) | The new AWE 5 elements, grouped, with their issue numbers. |
| [Login](pathname:///design-system/prototype/login.html) | Sign-in page and its states. |
| [Design tokens](pathname:///design-system/prototype/tokens.html) | Palettes, semantic colours in both modes, type, spacing, radius, elevation. |

The **Theme** button of every page changes only attributes of `<html>`: mode, primary, surface, density and two
prototype-only comparisons (customer logo, login image).

## Decisions behind this section

| Decision | Choice |
|----------|--------|
| Token architecture | Three tiers: primitive, semantic, component. See [Color and themes](color-and-themes.md). |
| Typography | Inter. |
| Density | Two densities: comfortable (14px, default) and compact (13px). |
| Icons | Lucide line icons in both engines; the `fa-*` names of the XML keep working through an equivalence table. |
| Layout | New shell: collapsible side menu, light header with search and user, breadcrumb, content in cards. |
| Licences | PrimeReact stays on 10.9.9 (MIT). No PrimeUI-licensed package is used. |
