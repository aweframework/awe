---
id: layout-and-shell
title: Layout and shell
sidebar_label: Layout and shell
---

The application frame of AWE 5: a collapsible side menu, a light header, a breadcrumb and content in cards. The menu
options and the screen XML do not change; only the presentation does. See it in the
[sample screen](pathname:///design-system/prototype/).

```text
┌──────────────┬──────────────────────────────────────────────────┐
│ Brand slot   │ Header: menu toggle · global search · help ·     │
│              │         notifications · user menu                │
├──────────────┼──────────────────────────────────────────────────┤
│ Menu search  │ Breadcrumb                                       │
│ Section      │ Page title + actions                             │
│  Option      │ ┌ Card ───────────────┐ ┌ Card ───────────────┐  │
│   Sub-option │ │                     │ │                     │  │
│    Sub-sub   │ └─────────────────────┘ └─────────────────────┘  │
│ Powered by   │                                                  │
└──────────────┴──────────────────────────────────────────────────┘
```

## Brand slot

Top left of the side menu. It holds the **customer's** logo, as AWE products do today.

| Variant | Content | When |
|---------|---------|------|
| Wide logo | The customer's horizontal logo | Default |
| Mark + name | Square mark plus the product name and the customer name | The logo is square or illegible when small |
| AWE logo | The AWE logo | No customer logo configured |
| Rail | Square mark only | The menu is collapsed |

- The logo has a light and a dark variant, or one that works on both backgrounds.
- AWE attribution stays small and secondary: "Powered by AWE 5" with the AWE mark, in the menu footer.
- Logo height: `--awe-header-logo-height` (34px comfortable, 30px compact).

## Side menu

Up to **three levels** below a section: section › option › sub-option › sub-sub-option.

| Element | Rule |
|---------|------|
| Section label | Small uppercase muted text. Not clickable. |
| Level 1 option | Icon + label (+ badge). Options with children show a chevron. |
| Levels 2 and 3 | No icon. Indented, with a vertical guide line per level. Smaller and muted. |
| Active trail | Every ancestor of the current page is marked (weight, accent bar on the guide); the page itself has the highlight background. |
| Expand / collapse | Per level, animated (`--awe-motion-duration-expand`). Open levels are restored without animation. |
| Search | Filters at every depth, opens the path of each match and highlights the matched text. |
| Badges | Counters (neutral or danger) and "New". |

**Rail.** The menu collapses to an icon rail (`--awe-menu-width-collapsed`, 68px). The width change is animated.
Hovering, focusing or clicking a level 1 option opens a **flyout** with its levels 2 and 3. Keyboard: Enter or → opens
it and focuses the first link, ↑/↓ move, Esc or ← close it and return the focus.

**Tablet and below (≤1024px).** The menu becomes a drawer over the content with a mask. The rail is a desktop-only
affordance.

## Header

- Light, slightly translucent, sticky, height `--awe-header-height`.
- Left: menu toggle, global search (`Ctrl K` focuses it).
- Right: help, notifications (dot when unread; list in a popover), user menu (profile, preferences, shortcuts, switch
  company, sign out).

## Page

| Element | Rule |
|---------|------|
| Breadcrumb | Home icon, then each menu level down to the page. The current page is not a link. |
| Page title | `--awe-font-size-page-title`, one optional line of description in muted text. |
| Page actions | Right of the title. One primary action at most; the rest secondary, outlined, text or icon-only. |
| Cards | Every block (criteria, grid, tabs) is a card: surface, border, `--awe-radius-card`, `--awe-shadow-card`. |
| Card header | Title (+ subtitle) and card actions. A divider when the body is a form or a grid. |
| Grid of cards | `--awe-layout-gap` between cards. Two columns collapse to one below 1280px. |
| Maximize | A card can fill the content area; the menu and the header stay visible. |

## Login page

See it in the [login prototype](pathname:///design-system/prototype/login.html).

| Area | Content |
|------|---------|
| Left column | Customer logo, form centred vertically (max 420px), footer with copyright, "Powered by AWE 5", language selector and mode toggle |
| Right column | Image panel with a caption (title and one line) on a dark scrim |
| Below 960px | The image panel hides; the form is centred, max 400px |

Form: title "Sign in to the platform" in `--awe-font-size-display`, username, password with show/hide, "Forgot
password?", full-width primary "Sign in", divider "or continue with", full-width outlined single sign-on button.

**Image slot.** A product sets its own image with `--awe-login-image` (a `url()`) or the `<img class="login__img">`
slot (`object-fit: cover`). Default: the AWE logo on a dark brand gradient (`--awe-login-brand-background`). Other
options: a plain gradient of the primary (`--awe-login-gradient`) or no image (form centred alone). The caption scrim
keeps its text above 10:1 on any primary; check your own image. In the AngularJS engine the properties
`awe.application.paths.image-startup-background` and `awe.application.paths.image-startup-logo` still apply: when a
product changes them from their defaults, the image fills the panel and the logo replaces the AWE logo of the form.

**Single sign-on.** The button is generic ("Continue with your organization", key icon). Do not use third-party logos
or trademarks.

| State | Behaviour |
|-------|-----------|
| Validation errors | Required fields marked, message under each, focus on the first |
| Invalid credentials | Danger message at the top of the form, password cleared and focused |
| Caps Lock on | Warning under the password while typing |
| Loading | Spinner in the button, "Signing in…", fields disabled, `aria-busy` |
| Password expired | "Change your password" step: new and confirm fields, live requirement checklist (#849) |
| SSO in progress | Progress view with Cancel |

Fields use `autocomplete="username"`, `current-password` and `new-password`. Enter submits.
