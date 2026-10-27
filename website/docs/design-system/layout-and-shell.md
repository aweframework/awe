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

**Footer.** Under a line, "Powered by **AWE** 5" with the AWE mark and a button with `chevrons-left` that collapses
the menu; on the rail only the button (turned to expand) remains, and the option search is hidden. Both engines style it from the home screen
markup, after the menu:

```xml
<tag type="div" style="sidebar-footer">
  <tag type="span" style="sidebar-powered">
    <tag type="span" label="SIDEBAR_POWERED_BY"/>
    <tag type="strong" label="SIDEBAR_POWERED_BRAND"/>
  </tag>
  <!-- React: info-button with a toggle-class of layout-static-inactive on .layout-container -->
  <!-- AngularJS: button with style="sidebar-collapse no-class" and a toggle-menu action -->
  <info-button id="collapse-menu" title="BUTTON_TOGGLE_MENU" icon="lucide:chevrons-left" style="sidebar-collapse">
    <button-action type="toggle-class" target-action="layout-static-inactive" target=".layout-container"/>
  </info-button>
</tag>
```

**Tablet and below (≤1024px).** The menu becomes a drawer over the content with a mask. The rail is a desktop-only
affordance.

## Header

- Light, slightly translucent, sticky, height `--awe-header-height`.
- Left: menu toggle (`panel-left` icon), global search (`Ctrl K` focuses it).
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
keeps its text above 10:1 on any primary; check your own image.

**Layouts.** Both engines build the sign-in page from the `awe.application.login.*` properties, with no change to the
sign-in XML. `awe.application.login.layout` picks the layout, set as `data-awe-login-layout` on the page root; any
other value is `split`.

| Layout | What it shows |
|--------|---------------|
| `split` (default) | The form column next to the image panel with its caption; the footer under the form. Below 960px the panel hides. |
| `form-only` | The form alone, centred, with its footer. No image panel. |
| `card` | The background image (or the AWE brand panel) fills the screen under `--awe-login-backdrop-veil`, the form sits in a centred card and the footer under the card, on the card surface. The caption stays on the backdrop and hides below 960px; the card keeps the backdrop. |

**Configurable parts.** Colours come from the theme (`awe.application.theme-primary`, `theme-surface`); the login adds
no colour property. An empty text hides its element.

| Part | Property | Default |
|------|----------|---------|
| Logo of the form | `awe.application.login.logo` | The AWE logo |
| Logo in dark mode | `awe.application.login.logo-dark` | The logo |
| Image of the panel (`split`) or the backdrop (`card`) | `awe.application.login.background` | The AWE brand panel |
| Canvas effect of the image panel, by the name the application registered | `awe.application.login.background-effect` | None (the image) |
| Form title, subtitle under it | `awe.application.login.title`, `awe.application.login.subtitle` | `SCREEN_TEXT_LOGIN_TITLE` ("Welcome to AWE"), `SCREEN_TEXT_LOGIN_SUBTITLE` ("Enter your credentials to continue.") |
| Caption over the image (title and one line) | `awe.application.login.panel-title`, `awe.application.login.panel-subtitle` | `SCREEN_TEXT_LOGIN_PANEL_TITLE` ("AWE Framework"), `SCREEN_TEXT_LOGIN_PANEL_SUBTITLE` ("Low coding complete functional web applications") |
| Footer text (left), for instance the copyright | `awe.application.login.footer-text` | None |
| "Powered by AWE 5" (under the footer text) | `awe.application.login.powered-by` | `true` |
| Language selector (right of the footer), with the languages of `awe.application.language-list` | `awe.application.login.language-selector` | `true` |
| Light, dark and system mode selector (right of the footer) | `awe.application.login.mode-selector` | `true` |

The password field has a show/hide button and warns while Caps Lock is on. Before signing in, the chosen language
applies to the page and the chosen mode is kept in the browser (`localStorage`); after signing in, the user's own
language and mode apply.

- **Texts** are a locale key or a literal: a value that is a whole locale key shows its translation in the language of
  the user; any other value shows as it is.
- **Images** take a URL relative to the application root (`images/acme/logo.svg`), an absolute URL
  (`https://cdn.acme.com/bg.jpg`) or a data URI. A configured logo replaces the AWE logo and wordmark of the form.
- **Dark mode.** While the page is dark (the user's mode, or the system one), the form shows `logo-dark` when it is set,
  else `logo`; it switches when the mode changes.
- **Powered by AWE 5.** Set `powered-by` to `false` to hide it; without it and without a footer text there is no footer.
- **Deprecated fallback.** When `logo` or `background` is not set, a value of
  `awe.application.paths.image-startup-logo` or `awe.application.paths.image-startup-background` changed from its
  default still brands the sign-in page. Move it to the `login` property. In AngularJS those two properties still style
  the change password and 2FA screens, which `awe.application.login.*` does not reach.

Branding a customer in `application.properties`:

```properties
awe.application.theme-primary=#0f766e
awe.application.login.layout=card
awe.application.login.logo=images/acme/logo.svg
awe.application.login.logo-dark=images/acme/logo-white.svg
awe.application.login.background=images/acme/sign-in.jpg
awe.application.login.subtitle=Customer portal
awe.application.login.panel-title=ACME Logistics
awe.application.login.panel-subtitle=Every shipment, one place
awe.application.login.footer-text=\u00A9 2026 ACME Logistics
```

Spring Boot reads `.properties` files as ISO-8859-1: write characters outside ASCII as Unicode escapes (`\u00A9` for ©) or use `application.yml`, which is read as UTF-8.

**Card tokens.** The card layout reads `--awe-login-card-width` (480px, 420px in compact density),
`--awe-login-card-padding`, `--awe-login-card-background` (the surface), `--awe-login-card-radius` and
`--awe-login-card-shadow` (those of a modal), and `--awe-login-backdrop-veil`, the veil over the backdrop image. See
the [tokens reference](tokens-reference.md#tier-3-component).

**Custom background effect.** `awe.application.login.background-effect` takes the name of a canvas effect the
application registers: AWE ships no effect, each product writes its own. The name is trimmed; an empty value, the
default, keeps the background image. The effect takes the place of the image panel of `split` and of the backdrop of
`card`, under the veil and the scrim, with the caption over it; `form-only` has no background, so it draws nothing, and
it hides with the panel below 960px in `split`. The framework gives the canvas no colour of its own: the effect paints
its own background (an opaque one, or a transparent one to show the page background).

The framework adds `<div class="signin-effect" aria-hidden="true"><canvas></canvas></div>` where the background image
goes, calls the registered effect with that canvas and stops it when the page goes away. The page root carries
`data-awe-login-effect="<name>"` and, while the effect runs, `data-awe-login-effect-running`.

| Case | What happens |
|------|--------------|
| Unknown name | Nothing is drawn and the background image stays (no wrapper covers the panel) |
| Effect registered after the page is drawn | It starts when it registers, so a bundle that loads later still draws it |
| Effect that throws | Reported with `console.error` (AngularJS `$log.error`) and the page and its image stay as they are |
| Page goes away | The function the effect returned is called (AngularJS: on `$destroy`) |

The contract is one function, `effect(canvas) => stop | void`: it draws on the canvas and returns the function that
stops it (cancel the frames, the observers and the listeners), or nothing when it has nothing to stop. The registry is
shared by both engines through `globalThis.aweLoginEffects`, so the effect is written once.

```js
// Application bundle, before or after the page is drawn (React)
import {registerLoginEffect} from "awe-react-client";

registerLoginEffect("brand-wave", canvas => {
  const context = canvas.getContext("2d");
  let frame = requestAnimationFrame(function draw() {
    // Paint the background and the animation of the product on the canvas
    frame = requestAnimationFrame(draw);
  });
  return () => cancelAnimationFrame(frame);
});
```

```js
// Application bundle (AngularJS): the same global registry
aweLoginEffects.register("brand-wave", function (canvas) {
  var context = canvas.getContext("2d");
  var frame = requestAnimationFrame(function draw() {
    frame = requestAnimationFrame(draw);
  });
  return function () {
    cancelAnimationFrame(frame);
  };
});
```

```properties
awe.application.login.background-effect=brand-wave
```

The canvas fills the panel (100% of its width and height): size its drawing buffer with `getBoundingClientRect()` and
`devicePixelRatio`. A canvas does not resolve CSS custom properties, so read the tokens of the product with
`getComputedStyle(canvas)`. Motion, reduced motion (`prefers-reduced-motion`), animation and cleanup are the
responsibility of the effect.

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
