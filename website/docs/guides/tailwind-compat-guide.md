---
id: tailwind-compat
title: Tailwind and the Bootstrap 3 compatibility layer
sidebar_label: Tailwind and compatibility
---

The AngularJS client is moving from the vendored Bootstrap 3 and pixel-admin LESS (about 1.7 MB of `styles.css`) to
Tailwind v4 built on the [design tokens](design-tokens-guide.md) (issue #755). The move is done in slices, each one with no
visual change (since the AWE 5 tokens, the compat classes follow them: see below). This page describes what is in place today.

:::info What changed for a product
The first families are served only by the compat layer: the vendored Bootstrap grid, panels, utilities and responsive utilities
are removed from the LESS build (slice 2d). Your XML screens and templates keep rendering the same (see the
[pixel diff](#pixel-diff-of-the-removal)). A product LESS that used those Bootstrap rule sets as mixins or extended them has to
change: see the [AWE 5 migration guide](v5-migration.md).
:::

## What is built

`awe-client-angular` runs Tailwind next to the LESS build (`postcss-loader` + `@tailwindcss/postcss`, configured in
`postcss.config.js`). Both end up in the same `css/styles.css`. The Tailwind sources are in
`awe-client-angular/src/main/resources/tailwind/`:

| File | What it holds |
|------|---------------|
| `awe-tailwind.css` | Entry point: layer order, prefix, no preflight, no automatic source detection |
| `theme.css` | The `@theme` that maps every `--awe-*` token (see [Design tokens](design-tokens-guide.md#mapping-into-tailwind)) |
| `sources.css` | The folders Tailwind scans: AngularJS templates, directives and the XML screens of the framework, modules and test apps |
| `compat/` | The Bootstrap 3 class names written on Tailwind with `@apply`, one file per family |

### How it coexists with Bootstrap 3

- **Cascade layers.** Tailwind writes into `@layer theme, base, components, utilities`. The LESS is not layered, and an
  unlayered rule always beats a layered one, so Tailwind can never change what a vendored Bootstrap rule renders. When a slice
  removes a vendored Bootstrap family, its compat rule (already in the `components` layer) takes over with no other change, and
  it now loses to any unlayered rule of the same or higher specificity, whatever the order of the files.
- **`tw:` prefix.** Tailwind utilities are only written with the prefix (`tw:flex`, `tw:p-3`, `tw:bg-primary`). Tailwind and
  Bootstrap 3 share names (`hidden`, `table`, `collapse`, `container`...), so an unprefixed name always means Bootstrap.
  The responsive variants use the Bootstrap 3 breakpoints: `tw:sm:` from 768px, `tw:md:` from 992px and `tw:lg:` from 1200px.
- **No preflight.** Tailwind's reset is not imported: the Bootstrap base styles are served by the compat layer (slice 8: normalize, scaffolding, type, code and print), and the rules that an unlayered rule would beat stay in the LESS leftovers.
- **No automatic detection.** Only the folders of `sources.css` are scanned, so a class that is only written in a test or in the
  documentation is never emitted.

You can already write `style="tw:p-md tw:bg-panel"` in an XML screen of the framework and see it emitted, with a limitation: until
the vendored Bootstrap, pixel-admin and AWE LESS are removed (slice 6), a `tw:` utility only takes effect where no such rule sets the
same property on that element, because unlayered LESS beats layered Tailwind. Do not rely on `tw:` utilities to override an existing
style yet. A product has XML screens that this build cannot scan: for them, use the Bootstrap 3 names below, which the compat layer
always emits, or add your own optional Tailwind build.

## The compatibility layer

A Bootstrap 3 class name is reimplemented in `tailwind/compat/<family>.css`. A family is only complete when a Jest test proves
it equals the vendored Bootstrap rule (same declarations, same media queries) and the guard test shows every class of the XML
screens is still served.

| Family | Classes | State |
|--------|---------|-------|
| Grid | `container`, `container-fluid`, `row`, `col-{xs,sm,md,lg}-{1..12}`, `-offset-`, `-push-`, `-pull-`, `-offset-right-` (AWE) | **Done, vendored rule removed** (slice 2d) |
| Panels | `panel`, `panel-{heading,title,title-icon,body,footer}`, `panel-transparent`, `panel-{default,info,body-colorful}`, `panel-group`, `panel-collapse`, `panel-padding{,-h,-r}`, `panel-wide`, `tab-content-padding`, `form-group-margin`, `no-grid-gutter-h`, `rounded` | **Done, vendored rule removed** (slice 2d); the pixel-admin panel rules and what is listed below stay in LESS |
| Utilities | `pull-{left,right}`, `text-{left,right,center,justify,nowrap,lowercase,uppercase,capitalize}`, `hide`, `show`, `hidden`, `invisible`, `hidden-*`, `visible-*`, `clearfix`, `center-block`, `text-hide` | **Done, vendored rule removed** (slice 2d), except `text-*`, which `type.less` still defines |
| Spacing utilities | `no-padding*`, `padding-{sm,xs}*`, `no-margin*`, `no-border*`, `no-border-radius`, `no-bg`, `valign-*`, `height-{sm,md,lg,200}`, `text-{xxs,xs,sm,bg,lg,xlg,slg}`, `text-{slim,normal,semibold,bold,underline,default,light-gray}` | Done (slice 2b), vendored rule still active |
| Buttons | `btn`, `btn-{default,primary,success,info,warning,danger,link}`, `btn-{lg,sm,xs,block}`, with the `active`, `disabled` and `focus` states | Done, vendored rule removed (slice 3d); the hover, focus and active rules of `btn` and the colors, `btn-flat\|outline\|rounded\|labeled` and the theme selectors stay in LESS |
| Button groups | `btn-group`, `btn-group-{lg,sm,xs,vertical,justified}`, `btn-toolbar`, `[data-toggle="buttons"]`, and `dropdown-toggle` (the global `:focus` rule and the ones inside groups) | Done, vendored rule removed (slice 3d); the dropdown states (`.open`, `.dropup`), `.btn-outline` in a group and the `#ui-grid-twbs` copy stay in LESS |
| Forms | `form-group`, `form-control` (focus, placeholder, `readonly`/`disabled`), `input-{sm,lg}`, `form-group-{sm,lg}`, `form-control-static`, `control-label`, `help-block`, `form-horizontal`, `form-inline`, `has-feedback`, `form-control-feedback` (slice 3b), `checkbox`, `radio`, `*-inline` and `has-{success,warning,error}` (slice 3b states) | Done, vendored rule removed (slice 3d); `form-actions` (sign-in screens only), the `.has-* .form-control:focus` selectors, `.has-*.dark\|.simple` and the criterion, theme and `px`/`lbl` rules stay in LESS |
| Input groups | `input-group` (and `col-*` inside), `input-group-{addon,btn,lg,sm}` with their `> .form-control` and select/textarea variants, first/last child rules, button joins, and the `.has-*` addon, `.form-inline` and `.btn-toolbar` selectors that mention them | Done, vendored rule removed (slice 3d); the navbar-form copies, `.btn-outline\|flat` in a group, the themed primary button borders (`.theme-*`), `.dark` and `.no-background` addons and the criterion, panel heading, select2 and picker rules stay in LESS |
| Modal, dropdown, popover, collapse (behavior) | The behavior of `modal`, `dropdown-toggle`, the target-message `popover` and the accordion `collapse` | **Done, native** (slice 4): Bootstrap JavaScript is no longer shipped |
| Modals | `modal`, `modal-{open,dialog,content,backdrop,header,title,body,footer,scrollbar-measure,lg,sm}`, `modal-alert` with its `modal-{info,danger,warning,success}` variants, `close` | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (modals.less 12 of 26, close.less 0 of 4 compiled rules); the rest stays verbatim in the leftovers: the side panel and grid column editor rules, the plugin dialogs (bootbox, summernote, the pickers), the close button of the sign in screens and the theme selectors stay in LESS |
| Dropdowns | `caret`, `dropup`, `dropdown`, `dropdown-menu` (`pull-right`, `-right`, `-left`, the links and their states, `divider`, `dropdown-header`, `dropdown-{xs,sm,md,lg,xl}`, `dropdown-submenu`), `dropdown-backdrop`, `open > dropdown-menu` | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (2 of 31 compiled rules); the rest stays verbatim in the leftovers: `.open .btn.dropdown-toggle` (an `!important` background that the colored buttons override), the open states of the colored and themed buttons, the dropdowns of the navbar and of the plugins and the `tabdrop` overflow tab stay in LESS |
| Navs | `nav`, `nav-{tabs,pills,stacked,justified,tabs-justified,divider,icon}`, `tab-content`, `tab-pane` and the dropdown inside a tab | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (6 of 63 compiled rules); the rest stays verbatim in the leftovers: `.nav-tabs.nav-justified > li > a` (an `!important` border that could meet the pagination), the badges and labels in a tab, the AWE tab container and avatar entry and the navs of the main navbar stay in LESS |
| Navbar | `navbar`, `navbar-{header,collapse,fixed-top,static-top,fixed-bottom,brand,toggle,nav,form,btn,text,left,right,default,inverse,link}` with the navbar form, the icon buttons and the labels and badges of the navbar | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (23 of 180 compiled rules); the rest stays verbatim in the leftovers: `#main-navbar` and its menu (pixel-admin), `.navbar-header h1` (the logo is a `url()` relative to its LESS file) and the theme selectors stay in LESS |
| Popovers | `popover`, `popover-{title,content}` and the `arrow` of each placement | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (3 of 19 compiled rules); the rest stays verbatim in the leftovers: the colored and dark variants of pixel-admin, the help popover and the plugin popovers stay in LESS |
| Tooltip | `tooltip`, `tooltip-{inner,arrow}` and the placements | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (2 of 16 compiled rules); the rest stays verbatim in the leftovers: the colored variants of pixel-admin, the slider tooltip and `.grid-cell-tooltip` stay in LESS |
| Component animations | `fade`, `collapse`, `collapsing` | **Done** (slice 5): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (3 of 7 compiled rules); the rest stays verbatim in the leftovers: the `.in` states stay in LESS |
| Tables | `table`, `table-{condensed,bordered,striped,hover,responsive}` and the contextual rows and cells (`active`, `success`, `info`, `warning`, `danger`) | **Done** (slice 6): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (10 of 150 compiled rules); the rest stays verbatim in the leftovers: the element rules (`table`, `caption`, `th`, the column cells), the cells of `.table` (they lose to `td` and `th` rules of normalize, the date pickers and the plugins), the contextual and hover rows, the bordered and striped tables. The print border of the bordered table, the tables in a panel and the AWE grid rules on the table stay in LESS |
| Labels | `label`, `label-{default,primary,success,info,warning,danger}` with their link states, and the label inside a button or a pill | **Done** (slice 6): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (8 of 23 compiled rules); the rest stays verbatim in the leftovers: the base label and its link states. The labels of the navbar are served by the navbar family; the tag label and the labels in the tabs, the main menu and a list group stay in LESS |
| Badges | `badge`, `badge-{success,warning,danger,info}`, and the badge inside a button, a pill or a list group item | **Done** (slice 6): the compat rules equal the vendored ones, and all ten Bootstrap rules stay verbatim in the leftovers (each loses to a rule that stays: the cascade guard found no rule that could leave); the badges of the navbar are served by the navbar family, those of the tabs, the main menu, a colored panel heading and the ui-grid menu stay in LESS |
| Wells | `well`, `well-{lg,sm}` | **Done** (slice 6): the compat rules equal the vendored ones, and all four Bootstrap rules stay verbatim in the leftovers (the sizes lose to the pixel-admin `.well`); the x-editable date editor stays in LESS |
| Alerts | `alert`, `alert-{link,dismissable,dismissible,success,info,warning,danger}` with the close button of a dismissible alert | **Done** (slice 6): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (8 of 22 compiled rules); the rest stays verbatim in the leftovers. The AWE 5 message toast (`alert alert-dark alert-awe`, `less/awe/controllers/message.less`) and its enter and leave animation, the dark and page alerts of pixel-admin and the demo theme stay in LESS, and win over the alert rules |
| Breadcrumbs | `breadcrumb`, its items, separator and active item, `breadcrumb-page` and `breadcrumb-label` | **Done** (slice 6): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (2 of 4 compiled rules); the rest stays verbatim in the leftovers. The breadcrumb bar of the AWE 5 shell (`less/awe/breadcrumbs.less`) restyles the levels, the separator and the label, is written once in LESS and wins over the compat rules; the dark and unpadded breadcrumbs of pixel-admin stay in LESS |
| Progress bars | `progress`, `progress-bar`, `progress-bar-{success,info,warning,danger}`, `progress-striped`, `progress-bar-striped`, `active` | **Done** (slice 6): the compat rules equal the vendored ones, and all fourteen compiled rules leave the build (nothing stays; `progress-bars.less` has no leftovers). The `progress-bar-stripes` keyframes are in `progress-bars.css`, and the stripes and the width transition take their duration from `--awe-motion-duration-slow`, which the token file sets to `0ms` under `prefers-reduced-motion`: the bar still shows its progress and stops moving. The AWE progress column and the uploader (a flat rounded track), the dropzone preview and the progress bar in a panel heading stay in LESS |
| Pagination | `pagination`, `pagination-{lg,sm,xs}` with the links, the first and last item and the hover, active and disabled states | **Done** (slice 6): the compat rules equal the vendored ones, and all 36 Bootstrap rules stay verbatim in the leftovers (each loses to a rule that stays: the cascade guard found no rule that could leave). The pager of the AWE grid (`pagination pagination-size`, built in `grid.js` and `grid.html` with `uib-pagination`) is written in `less/awe/directives/grid.less` and wins over these rules; the date picker calendar, the DataTables pager and the pagination of a panel heading stay in LESS |
| Pager, media object, jumbotron, carousel and icon font | `pager`, `media*`, `jumbotron`, `carousel*` and `glyphicon*` (the Glyphicons Halflings font) | **Dropped** (slice 7): nothing in the framework writes these classes (`unusedBootstrapFamilies.test.js` reads the templates, the directives, the XML screens, the file manager of awe-tools and the React client), so their vendored rules are removed with no compat CSS. A product that writes them needs its own CSS |
| List groups | `list-group`, `list-group-item`, its links and the active, disabled and contextual (`list-group-item-{success,info,warning,danger}`) states, `list-group-item-heading` and `list-group-item-text` | **Done** (slice 7): the compat rules equal the vendored ones, and the vendored rule is removed only where no unlayered rule competes (2 of 83 compiled rules of the three families below); the rest stays verbatim in the leftovers. The badges and labels of an item, the list groups inside a panel and the pixel-admin icon stay in LESS |
| Thumbnails | `thumbnail`, its images, the linked thumbnail and `caption` | **Done** (slice 7): the compat rules equal the vendored ones; the base thumbnail, its images and the link border stay verbatim in the leftovers. The file manager of awe-tools writes the third-party `thumbnail` markup and keeps its own overrides |
| Responsive embeds | `embed-responsive`, `embed-responsive-{item,16by9,4by3}` | **Done** (slice 7): the compat rules equal the vendored ones, and all eight Bootstrap rules stay verbatim in the leftovers (each loses to a rule that stays) |
| Base styles | The Bootstrap element rules and classes of `normalize`, `print`, `scaffolding` (body, links, images, `img-*`, `sr-only`), `type` (headings and `h1`..`h6`, `lead`, `small`, `mark`, `text-*`, `bg-*`, `page-header`, lists, `dl-horizontal`, blockquotes) and `code` (`code`, `kbd`, `pre`, `samp`) | **Done** (slice 8): the compat rules equal the vendored ones (the family tests list the exact selectors of each vendored file); 226 of the 300 compiled rules left the build and 74 stay verbatim in the leftovers: `html` (its 10px font size is the base of every rem rule), the print rules that print black on white, and the rules that a pixel-admin or AWE rule would beat |

**Bootstrap JavaScript (slice 4).** The client no longer imports the JavaScript of Bootstrap 3 (`import "bootstrap"` and the `bootstrap` npm
package are gone; the CSS is unchanged). The four plugins AWE used are native and keep the same markup, classes and timing:

| Behavior | Native replacement | Markup and events kept |
|---|---|---|
| Modal (dialogs, side panels, confirm, the Columns side panel of the grid) | `AweModal` service (`services/modal.js`) | `.modal` gets `display: block` and `.in`; one `.modal-backdrop.in` per open modal is appended to the body; `body.modal-open` and the scrollbar padding; Esc, a click on the backdrop and `data-dismiss="modal"` close it, `data-keyboard="false"` and `data-backdrop="static"` are honored; `show.bs.modal`, `shown.bs.modal`, `hide.bs.modal` and `hidden.bs.modal` are jQuery events on the element, and `hidden.bs.modal` is raised after the backdrop is removed |
| Popover of the target messages | `createMessagePopover` (`services/messagePopover.js`) | `.popover.fade.in.{top,bottom,left,right}` with its `.arrow`, appended to the body; `auto bottom` flips above the target when there is no room; the popover stays inside the viewport and the arrow keeps pointing at the target; the title and the content are shown as text and the template is sanitized with Bootstrap's allow list plus the test hooks (`data-testid`, `data-testid-owner`, `data-type`) |
| Dropdown (user and info menus, the overflow menu of the tab strip) | `services/dropdown.js` | `data-toggle="dropdown"` toggles `.open` on the parent (or on the `data-target`) and sets `aria-expanded`; a click outside, on an item or Esc closes it; opening one closes the others; the arrows and the space bar work; `show.bs.dropdown`, `shown.bs.dropdown`, `hide.bs.dropdown` and `hidden.bs.dropdown` are raised on the dropdown |
| Collapse (accordion) | `services/collapse.js`, driven by the accordion item | The item is the single driver: `.collapse` and `.collapse.in` at rest, `.collapsing` with the 0.35 s height transition while it changes, `.collapsed` on the toggle while the panel is closed. The `data-toggle="collapse"` and `data-parent` attributes are gone from `accordionItem.html`: the single-open rule is the `autocollapse` of the accordion |

`modalChange` and the `ui-modal` directive work as before. `jsdom`-based tests need nothing: the services do not wait for a CSS transition
the element does not have. `bootstrapJsRemoval.spec.js` fails if the bundle imports Bootstrap again, if a source calls a Bootstrap plugin
(`.modal(`, `.popover(`, `.dropdown(`, `.collapse(`, `.tab(`, `.tooltip(`) or if a template uses a `data-*` attribute without a native handler.
Bootstrap JavaScript plugins that AWE did not use (`tab`, `tooltip`, `button`, `alert`, `carousel`, `scrollspy`, `affix`) were already
replaced by native code or were never called. Product templates that relied on the Bootstrap data-api have to change: see
[Bootstrap JavaScript is no longer shipped](v5-migration.md#bootstrap-javascript-is-no-longer-shipped).

**What remains vendored after slice 2d:** every other Bootstrap file (tables, pagination, alerts, labels...), all the
pixel-admin LESS (including its spacing, `utils.less` and panel rules), the `text-*` alignment and transform classes of
`type.less`, and the AWE LESS. `bootstrap.less` no longer imports `grid.less`, `panels.less`, `utilities.less` and
`responsive-utilities.less`, and the files are deleted. What the compat layer does not serve stays verbatim in
`less/legacy/panels-leftovers.less` and in `grid-columns.less`.

**Buttons, button groups, forms and input groups (slice 3d).** `bootstrap.less` no longer imports `buttons.less`, `button-groups.less`,
`forms.less` and `input-groups.less` (the files are deleted; `mixins/*.less` stay). It imports `buttons-leftovers.less`,
`button-groups-leftovers.less`, `forms-leftovers.less` and `input-groups-leftovers.less` in the same places. Each leftovers file holds,
verbatim as it compiled and in the same order, the rules that must stay unlayered: the ones that used to win over a rule that stays in the
LESS (the disabled button colors against the pixel-admin and AWE states, `.btn:active` and `.btn.active`, the group sizes, the select and
textarea heights of the input groups...), the ones that those would otherwise beat, and the rules the compat layer does not serve (the
global form element rules: `fieldset`, `legend`, `label`, `input[type=...]`; `.btn:focus`; the caret of a large button). The compat
rules are still there and equal; only the winner changes where an unlayered rule exists, and `cascadeGuard.test.js` proves there is none
left (with the pairs that no element can match, such as `select` and `textarea` or two button colors, excluded by `canMeet`). Only some of the compiled rules of the four files could leave (the exact numbers are in the step 2 entry of `support/vendored-removed.baseline.json`: the frozen rules, and the `kept` list of the ones that stay): the cascade guard found the rest must stay, because for this removal it also counts classes that the sources write together (`form-control input-sm`), element rules on a tag that carries the class (`label` and `btn`) and a shorthand against its longhand (`border-radius` and `border-top-right-radius`), all of which the pixel diff showed to matter.
The leftovers files also keep what Less would drop silently: `.btn-lg()` and `.btn-group-justified()` (called by `awe/mobile.less`),
`.form-inline()` (called by `navbar.less`) and the three `&:extend(.btn-xs|sm|lg)` of the button groups, which print the group sizes in
pixel-admin's rules. The frozen rules of the removal are in `vendored-removed.baseline.json` (step 2, with the list of the rules that
stay); `vendoredRemoval.test.js` checks the files are gone, that no frozen rule is in the build, that every leftover still is, and that no
call or extend of a removed rule set is left without its mixin. Slice 6 removes the leftovers together with the pixel-admin rules they compete with.

**Dropdowns, navs, navbar, modals, popovers, tooltip and component animations (slice 5).** `bootstrap.less` no longer imports
`dropdowns.less`, `navs.less`, `navbar.less`, `modals.less`, `close.less`, `popovers.less`, `tooltip.less` and `component-animations.less`
(the files are deleted). It imports `component-animations-leftovers.less`, `dropdowns-leftovers.less`, `navs-leftovers.less`,
`navbar-leftovers.less`, `close-leftovers.less`, `modals-leftovers.less`, `tooltip-leftovers.less` and `popovers-leftovers.less` in the same
places. The compat rules are equal to the compiled vendored ones (Bootstrap, pixel-admin and AWE copies of each selector, with the effective
declarations in every media query: `compatDropdowns.test.js` and its six siblings, one per family, share `support/compatFamily.js`). Of the 346
compiled rules of the eight files, only 51 left the build and 295 stay verbatim in the leftovers (the exact lists are in the step 3 entry of
`support/vendored-removed.baseline.json`), because pixel-admin, the themes and the AWE components restyle the same elements with unlayered rules
that a layered compat rule would lose to (the dropdown menu and its links, the tabs, the navbar links, the popover arrows...). The pixel diff of a first build found three ways in which a
rule can lose to an unlayered one that the earlier slices' guard did not see, and the guard now covers them for this removal: a rule inside a media
query that beats a base rule that stays (`.nav-justified > li { display: table-cell }` over `.nav > li`), a rule on every element (`body *`) and the
variants of one component that are written on one element (`navbar` with `navbar-default` and `navbar-right`, or a class AWE never writes but a product
can, such as `<a class="navbar-brand">` against the rules on `a`). Three more guards exist for this slice:

- **`!important`.** A layered `!important` beats every unlayered `!important`, whatever its specificity. `compatFamily.js` finds the served
  `!important` declarations that a rule left in LESS (same media, an overlapping property, another value) could meet on an element, and fails
  unless the selector is excluded with a reason: `.open .btn.dropdown-toggle` and `.nav-tabs.nav-justified > li > a`. It also models a declaration
  that a later normal one of the same rule repeats (`float: left !important; float: left` in `.navbar-left`): the `!important` stays, so it is
  frozen with the rule (`shadowed`).
- **`url()`.** A compat file writes no `url()`: a relative path is resolved from the folder of the file that writes it, so `.navbar-header h1`
  (the logo) stays in LESS.
- **Order.** `compatSlice5Order.test.js` pins the order of the imports of `compat/index.css` to the order `bootstrap.less` had.

**LESS variables.** The leftovers (and the compat files) hold the values as they compiled, so a value that came from a Bootstrap LESS variable is frozen:
the `.close` font size (`19.5px`), the colors of the active dropdown and nav links (`#337ab7`) and so on. A product that overrides those variables with
`modifyVars` or a LESS of its own no longer changes them. Values written as CSS custom properties (`var(--navbar-text-color, #9D4444)` and the other
`--*` of the themes) keep following the runtime theming; to change a frozen value, write your own unlayered rule.

The cascade guard (`cascadeGuard.test.js`) covers the third removal with the same broad matching as the second (classes written together,
element rules, a shorthand against its longhand) plus the three cases above, and found no pair left. Its pixel diff of the AWE screens (settings and the Test module, Chromium and
Firefox, 1440 and 375 pixels wide, light and dark theme, with the avatar and info dropdowns open, the tab overflow menu, the confirm dialog, the
dialogs, the side panels, the accordion, the target message popover and the mobile navbar) is identical to the one before the removal.
`vendoredRemoval.test.js` checks the files are gone, no frozen rule is in the build, every leftover still is, the build is smaller and no call or
extend of a removed rule set is left without its mixin. Slice 6 removes the leftovers together with the pixel-admin rules they compete with.

**Tables, labels, badges, wells, alerts, breadcrumbs, progress bars and pagination (slice 6).** `bootstrap.less` no longer imports `tables.less`,
`labels.less`, `badges.less`, `wells.less`, `alerts.less`, `breadcrumbs.less`, `progress-bars.less` and `pagination.less` (the files are deleted). It imports
`tables-leftovers.less`, `breadcrumbs-leftovers.less`, `pagination-leftovers.less`, `labels-leftovers.less`, `badges-leftovers.less`,
`alerts-leftovers.less` and `wells-leftovers.less` in the same places; progress bars leave nothing, so their import is gone. The compat rules are equal
to the compiled vendored ones (Bootstrap, pixel-admin and AWE copies of each selector: `compatTables.test.js` and its seven siblings, one per family, share
`support/compatFamily.js`, with the AWE 5 selectors of the message toast and of the breadcrumb bar declared as `owned` by the AWE LESS, and the
`!important` of the print border of the bordered table and of the disabled pagination link on hover excluded as hazards). Of the 263 compiled rules of the eight
files, only 42 left the build and 221 stay verbatim in the leftovers (the exact lists are in the step 4 entry of `support/vendored-removed.baseline.json`),
for the same reason as in the earlier removals: the element rules (`th`, `td`, `a`, `h2 span`), the plugins (the date pickers, DataTables, the scrollbars)
and the pixel-admin and AWE components restyle the same elements with unlayered rules that a layered compat rule would lose to. Two details are
specific to this removal:

- **Reduced motion.** The cascade guard found the striped animation of an active progress bar (`.progress.active .progress-bar`, `.progress-bar.active`) in
  conflict with the animations of the loaders, the skeleton placeholders and the busy view root (`.loader *`, `.screen-skeleton > *`,
  `[ui-view][aria-busy="true"]`). It is the one reviewed exclusion of the step (`cascadeGuard.test.js` states why no element is both: no template writes a progress bar inside
  them), so the animation leaves the LESS and the compat rule serves it with `--awe-motion-duration-slow`, which is `0ms` under `prefers-reduced-motion`.
  `compatProgressBars.test.js` pins that the compat file has no fixed animation time.
- **A rule that two files print.** `.list-group-item > .badge + .badge` is written by Bootstrap and by pixel-admin with the same declarations. The baseline counts
  rules by identity, so the Bootstrap copy stays in `badges-leftovers.less` (the build is the vendored one minus the frozen rules, and a test pins it).

The values of the leftovers are frozen as compiled (see *LESS variables* above): a product that overrides `@table-border-color`, `@label-default-bg` or the like with
`modifyVars` no longer changes the leftovers or the compat rules. `vendoredRemoval.test.js` checks the files are gone, no frozen rule is in the build, every
leftover still is, the build is smaller and no call or extend of a removed rule set is left without its mixin. The mixins of `less/legacy/mixins` that
the files called (`.label-variant()`, `.alert-variant()`, `.progress-bar-variant()`, `.table-row-variant()`, `.pagination-size()`) stay, and the pixel-admin
`.tables-theme`, `.labels-theme`, `.badges-theme`, `.progress-bars-theme` and `.pagination-theme` mixins that `less/awe/base.less` calls inside a comment still compile.

**Pager, media object, jumbotron, carousel and icon font (slice 7).** `bootstrap.less` no longer imports `pager.less`, `media.less`, `jumbotron.less`,
`carousel.less` and `glyphicons.less`, and the files are deleted, with the `glyphicons-halflings-regular` font files of the client (`libs/bootstrap-3.3.2/fonts`, a folder that no longer exists, see slice 9)
and the unreferenced copy in `awe-tools` (`src/main/resources/fonts`; the file manager bundle builds its own Bootstrap from npm). The orphan `theme.less` (the
optional Bootstrap theme, imported by nothing) and the `.pre-scrollable` helper of `code.less` are gone too. **There is no compat CSS for any of them**: no
template, directive, XML screen, `.stg` template, file manager or React file writes their classes (`unusedBootstrapFamilies.test.js`; the React client has its
own styles, and its `carousel` class is the PrimeReact widget styled by `AweCarousel.less`). A product that writes `media`, `jumbotron`, `carousel`, `pager`,
`glyphicon` or `pre-scrollable` in its own screens or templates has to provide that CSS itself, and its LESS that calls or extends one of their rule sets silently
loses those declarations. The 367 compiled rules are frozen in step 5 of `support/vendored-removed.baseline.json`, marked `unserved` (the compat tests skip them: no
compat file serves them on purpose). The element rules of `code.less` (`code`, `kbd`, `pre`, `samp`) are base styles: the compat layer serves them since slice 8 (see below), and only the block of code (`pre`) stays, in `pre-leftovers.less`.
`list-group.less`, `thumbnails.less` and `responsive-embed.less` are served by the compat layer instead of being dropped, because something still writes them: the
notifier settings screen writes `list-group` and `list-group-item`, and the file manager of `awe-tools` writes `embed-responsive` and the `thumbnail` of its third-party
markup (`unusedBootstrapFamilies.test.js` pins each use and its compat file, so the test fails the day the use goes and the file can follow). They are in step 6 of the
baseline (served, unlike step 5), and `bootstrap.less` imports `thumbnails-leftovers.less`, `list-group-leftovers.less` and `responsive-embed-leftovers.less` where the
files were. Only 2 of their 83 compiled rules left the build (`.list-group-item:first-child`, `.thumbnail .caption`); 81 stay verbatim (`.list-group` among them: the `ul, ol` margin of the typography still in the LESS would win over a layered rule on a `ul.list-group`), for the reason of the
earlier removals: the element rules, pixel-admin and the plugins restyle the same elements with unlayered rules that a layered compat rule would lose to. The thumbnail
leftovers keep the `&:extend(.img-responsive)` of the Bootstrap source, so the rule that `.img-responsive` prints for the thumbnail images stays where it was. The pixel-admin
`.list-groups-theme` mixin that `less/awe/base.less` calls inside a comment, the `.panel > .list-group` rules and the `.list-group-item > .badge` rules of the panels and badges
leftovers still compile. The file manager of awe-tools builds its own Bootstrap from npm and keeps its `.iconset .thumbnail` overrides.

**Base styles (slice 8).** `normalize.less`, `print.less`, `scaffolding.less`, `type.less` and the element rules of `code-leftovers.less` are served by the compat layer
(`normalize.css`, `print.css`, `scaffolding.css`, `type.css` and `code.css`, imported first, where the vendored build compiled them). Their rules are mostly element rules (`body`, `a`,
`h1`, `p`, `ul`, `code`...), which no class finds, so each family test lists the exact selectors that its vendored file compiled to (`support/baseStyles.selectors.json`), and two
element selectors that no element can both match (`code` and `fieldset`) do not count as competitors in the cascade order check. Of the 300 compiled rules, 226 left the build (step 7
of `support/vendored-removed.baseline.json`: normalize 64, print 12, scaffolding 15, type 127, code 8) and 74 stay verbatim in `normalize-leftovers.less`, `print-leftovers.less`,
`scaffolding-leftovers.less`, `type-leftovers.less` and `pre-leftovers.less` (`code-leftovers.less` is gone); the LESS build went from 815487 to 808896 bytes. What stays, besides the
rules that the cascade guard found an unlayered pixel-admin or AWE rule would beat: `html` (its 10px font size is the base that every rem rule of the AWE LESS and the plugins is
calculated from, and no token holds it), the print rules that print black on white (an `!important` color or background and fixed grey or black borders: no token stays the same in
the dark mode, and a layered `!important` would win over the unlayered ones of the AWE print styles), `.img-responsive` (the thumbnails extend it), `.text-success`, `.text-info`,
`.text-warning` and `.text-danger` (`icon.less` calls them as rule sets, and Less drops a call whose rule set is gone without a warning), `.text-left`, `.text-right` and `.text-center`
and `.sr-only-focusable` (the build prints them more than once, and the baseline needs every copy). The values that left the build follow the tokens, so they differ a little from
the compiled Bootstrap ones: the headings read the type scale (33, 27, 23, 17, 13 and 12px become `--awe-font-size-3xl`, `2xl`, `xl`, `lg`, `base` and `xs`; weight 500 and line height 1.1
become the medium weight and the tight line height), the body reads the base size and line height (1.428 becomes 1.5), links and the contextual text and background classes read the
primary, success, info, warning and danger text and subtle tokens, the margins of 9 and 18px become `--awe-space-2` and `--awe-space-5` (8 and 20px), the blockquote bar (5px) is the strong border
width (2px), `code`, `kbd` and `pre` read the danger, contrast and subtle surface tokens, and `.lead` is 400 instead of 300. The offsets of `dl-horizontal` (160 and 180px) are
written from the space tokens and stay exact. No new token was needed.

**Legacy Bootstrap 3 LESS (slice 9).** What is left of the vendored Bootstrap 3 build no longer lives under `libs/bootstrap-3.3.2`: it is AWE's own LESS in `less/legacy`, imported by
`awe.less` through `legacy/legacy.less` (the former `bootstrap.less`, with the same imports in the same order). The folder holds `variables.less`, `mixins.less` and `mixins/`, `grid-columns.less` and
the 28 `*-leftovers.less` files, moved with `git mv` and unchanged: the compiled `styles.css` is byte-identical to the one before the move. The Bootstrap MIT notice is the header of
`legacy.less` and `legacy/LICENSE-bootstrap.txt`. Wherever this guide says `bootstrap.less`, read `legacy/legacy.less`; the frozen baseline stores file names only, so its digests did not change.

**pixel-admin is no longer a vendored library (final step of #755).** The pixel-admin LESS that the build imports (67 files: the core files, the plugins and the notifications widget) is AWE's own
LESS in `less/legacy/pixel-admin`, imported by `awe.less` in the same order through `legacy/pixel-admin/pixel-admin.less`; `libs/pixel-admin-less` no longer exists. The demo, the pages, the rtl rules,
the other widgets and the images that no compiled rule points at were deleted; the two images that a rule still reaches stay in `legacy/pixel-admin/images`, and the minicolors sprite moved to
`legacy/images/plugins` with them, so the compiled `styles.css` is byte-identical to the one before the change. Wherever this guide says `libs/pixel-admin-less`, read `legacy/pixel-admin`.
`NOTICE-pixel-admin.txt` keeps the provenance of the files.

**The rule of the migration: while the pixel-admin and AWE LESS ship, a compat rule only takes over where no unlayered rule
competes.** A compat rule loses to any unlayered rule, whatever its specificity, so a vendored Bootstrap rule that used to win over a
rule that stays in the LESS (by specificity or by order) has to stay in the LESS too, until slice 6 removes what it competed with.
`cascadeGuard.test.js` enforces it: it lists every removed rule that used to beat a still-vendored rule on the same property with
another value and a class in common, and fails unless the pair is reviewed (the contextual panel colors, which never meet on one
element). Two cases found by the pixel diff: the accordion, whose `.panel-group` rules (no margin between panels, no border under
the heading) beat the pixel-admin `.panel` and `.panel-heading` rules and now stay whole in `panels-leftovers.less`, and the column
base rule: Bootstrap gave every column
`min-height: 1px` (so an empty column used as a spacer keeps its place) and AWE has `body * { min-height: 0 }`; as a layered
rule the 1px lost, so `grid-columns.less` keeps that base rule (`position`, `min-height`, side paddings) in LESS. Other element
rules (`td`, `th`, `fieldset`, `legend` padding or width, `colorpicker-saturation`) now also win over a `col-*` written on the same
element; no AWE screen does that, but a product might.

The utilities follow the compiled vendored CSS (`.affix`, the class of the Bootstrap affix plugin, is not reimplemented: nothing
uses it, and it is gone with the removal). Bootstrap writes `pull-*`,
`hide`, `show`, `hidden`, `hidden-*` and `visible-*` with `!important`, and for `!important` the layer order inverts: a layered
`!important` beats an unlayered one. So these compat rules are **not** inert next to the vendored LESS: on an element that
also carries a class with an unlayered `!important` of another value (`.stat-cell`, `.wizard-steps`, an Angular `ng-hide`...),
the compat rule would win. Nothing in the framework mixes them. `compatUtilities.test.js` fails when a class list written in
one attribute of a template, directive or screen starts to, but it cannot see classes added by `ng-class` or from JavaScript,
nor Angular's runtime `ng-hide` rule: a product that writes `show` or `visible-*` on an `ng-show` element should not rely on
`ng-show` yet.

The spacing and type classes keep the compiled values and their `!important` (spacing, borders, alignment, heights and font weights;
not the sizes, `text-underline` or the colors). Left out: `text-success|danger|warning|info`
(feedback slice). Two elements already mix a class with an unlayered `!important` of another value
(`panel-padding no-padding-t`, `no-border panel-transparent`); both resolve to the same computed value, and `compatSpacing.test.js`
lists them. Browsers without `color-mix()` get the
base color on `text-default a:hover` and `text-light-gray a:hover` (the build adds a fallback); the vendored rule gave them none.

The compat files read only `--awe-*` semantic or component tokens (see the awe-ui skill): no colour literal, no bare `px` size and no primitive
(`--awe-palette-*`) in them, which `compatTokens.test.js` checks. Each Bootstrap value was mapped by meaning (the blue of a link is `--awe-color-primary`, a
validation panel uses the `--awe-color-danger-*` family, a gap is the closest `--awe-space-*` step), so the classes follow the theme, the mode and the
density and **may look different from the compiled Bootstrap 3 values**. The new tokens are `--awe-border-width`, `--awe-border-width-strong` and
`--awe-grid-container-sm|md|lg`. The tests keep the selectors, the properties, `!important` and the cascade order, no longer the values. `compat/index.css` imports `panels.css` **before** `spacing.css`:
`panel-padding no-padding-t` is in screens, both are `!important` with the same specificity and `no-padding-t` must keep winning;
`compatPanels.test.js` pins the order. Left in LESS for now (the Bootstrap ones are in `panels-leftovers.less`): the contextual colors that nothing uses (`panel-primary|success|warning|danger`, `panel-dark`), the colored
accordions (`panel-group-info|success|...`), the tables and list groups inside a panel, the selectors that depend on a theme class of the body (`.theme-* .panel`) and the AWE panel, window and
wizard directives (`panel-awe`, `panel-sm|md|lg`, `maximized`, `wizard-panel`). `compatPanels.test.js` names every one of them.

Buttons and button groups (`buttons.css`, `button-groups.css`, imported in that order) are raw declarations in the order of the vendored build (many rules compete at the same specificity: pixel-admin colors over
Bootstrap's, the AWE default button over both). Themed values stay `var(--secondary-color, #e8e6e6)` with the compiled fallback. The `:hover`, `:focus`,
`:active` and `.active` selectors of `.btn` and of `.btn-{default,info,success,warning,danger}` stay in LESS: pixel-admin gives them `background: ... !important`, and
every AWE button (`btn btn-awe btn-*`, `services/button.js`) has a themed `!important` background of higher specificity. A layered `!important` beats an unlayered one
whatever its specificity, so serving them would repaint every AWE button. `compatButtons.test.js` names what stays and pins why. The `!important` that does move, `.btn { outline: none }` and the print `display: none`, is layered, so it beats any unlayered `!important` of a product on a button.

Forms are two files, in the order the vendored build compiles them: `forms.css` (Bootstrap `forms.less`) is imported before the utilities, the panels, the spacing and the buttons, and
`forms-pixel-admin.css` (the pixel-admin overrides of the same selectors) after the buttons and button groups. Values are the compiled ones, themed values stay `var(--text-color, #000000)` with the compiled fallback, and
there is no `!important` in either file. The vendor placeholder pseudo-elements are separate rules, because a browser drops a whole rule when one selector is unknown. Left in LESS: the global element
selectors of `forms.less` (`fieldset`, `legend`, `label`, `output`, `input[type=...]`), the navbar forms, `.has-*.dark|.simple` and the `.lbl` custom controls, the
`.has-* .form-control:focus` and `.form-control.focus` selectors (pixel-admin writes `border-color: ... !important`, and the themed `.has-*.dark` rules carry it too), `form-actions` (only styled inside the sign-in, change-password,
TOTP and SSO logout screens), and everything scoped to a criterion, a theme, an id or a plugin. `compatForms.test.js` names every one of them.

Input groups are `input-groups.css` (Bootstrap `input-groups.less`, imported after the buttons and the button groups and before `forms-pixel-admin.css`, as the vendored build orders it) and `input-groups-pixel-admin.css` (the pixel-admin overrides, imported after `forms-pixel-admin.css`). Values are the compiled ones, themed values keep `var(--x, fallback)` and neither file has `!important`. The `.has-*.dark` and `.no-background` addons carry an `!important` background, and the criterion, select2 and picker rules are scoped to their component, so they stay in LESS; `compatInputGroups.test.js` accounts for every vendored selector that mentions an input group with example selectors. The `add-on` class that the date directives write has no Bootstrap 3 rule: only the color picker and x-editable plugins style it.

The heading and footer corners are written as `0` on purpose: Bootstrap's `calc(var(--border-radius) - 1)` (a length minus a number) is invalid and
computes to 0 today, so nothing changes. `.panel-body` has padding 20px (pixel-admin over Bootstrap), and the AWE `panel-sm|md|lg` sizes stay
more specific than the compat rule.

The AWE grid keeps its own numbers, not Bootstrap's defaults: a **22px gutter** (11px each side) and containers of **742, 962
and 1162px**. The grid is a fixed list, because a framework cannot scan the XML of every product. It is generated:

```bash
npm run compat:grid --prefix awe-framework/awe-client-angular
```

## Checking a change

| Test | What it proves |
|------|----------------|
| `tailwindBuild.test.js` | Layer order, no preflight, only `tw:` utilities, no default theme, every token mapped, the scanned folders exist and scanning reaches XML, templates and directives |
| `compatGrid.test.js` | The generated grid file is up to date and every grid class equals the vendored Bootstrap (4 breakpoints x 12 columns, offsets, pushes, pulls, container, row) |
| `compatUtilities.test.js` | Every utility class equals the compiled vendored rule per media query, the XML utilities are served, and no element of the framework mixes a utility with a class whose unlayered `!important` would now lose |
| `compatSpacing.test.js` | Every spacing, border, alignment, height and type class equals the compiled vendored rule (selectors, `!important`, media), the XML classes of the family are served, nothing of the family is missed, and the reviewed `!important` mixes are the only ones |
| `compatPanels.test.js` | Every panel class and its descendant selectors equal the compiled vendored rule, every panel class of the templates, directives and screens is served or left out for a stated reason, `panels.css` is imported before `spacing.css`, and the reviewed `!important` mixes are the only ones |
| `compatInputGroups.test.js` | Every input group selector (and the form and button group selectors that mention one) equals the compiled vendored rule (selectors, media), themed values keep their `var()` fallback, no `!important` is served, every left-out selector is accounted for with example selectors, rules that compete at the same specificity keep the vendored order and both files are imported where the vendored build compiles them |
| `compatForms.test.js` | Every form control, layout, checkbox, radio and validation-state selector equals the compiled vendored rule (selectors, media), themed values keep their `var()` fallback, no `!important` is served, the form classes of the templates, directives and screens are served or left out for a stated reason, rules that compete at the same specificity keep the vendored order, and both files are imported where the vendored build compiles them |
| `compatButtons.test.js` | Every button and button group selector equals the compiled vendored rule (selectors, `!important`, media), themed values keep their `var()` fallback, the button classes of the templates, directives and screens are served or left out for a stated reason, rules that compete at the same specificity keep the vendored order, and the reviewed `!important` mixes are the only ones |
| `cascadeGuard.test.js` | No rule that a compat rule serves used to beat a rule that stays in the LESS (unlayered) with another value on the same property, but the reviewed pairs |
| `vendoredRemoval.test.js` | The four vendored files are gone and none of their compiled rules is in the vendored build, every removed selector is served by the compat layer (but `.affix`), every class of them that a template, directive or screen writes is served, no LESS extends or calls a rule set that was removed (Less drops those without a warning), and the LESS build is smaller |
| `xmlClasses.test.js` | Every class written in the `style` of an XML screen is served by the compat layer or by the vendored CSS, or is a known marker with no CSS rule |

### The frozen vendored rules

The equivalence tests compare the compat layer with the compiled vendored Bootstrap rule. Once a family is removed from the LESS,
the rule it compared with is gone, so the removed rules are **frozen** in
`src/test/jest/styles/support/vendored-removed.baseline.json` (generated from the tree before the removal, with their position in
the build) and the tests restore them next to the current build (`support/vendoredBeforeRemoval.js`). The file is not touched by a
normal change. To generate it again, from the tree before the removal:

```bash
git archive <commit before the removal> awe-framework/awe-client-angular/src/main/resources/less | tar -x -C /tmp/before
npm run compat:freeze-vendored --prefix awe-framework/awe-client-angular -- /tmp/before/awe-framework/awe-client-angular/src/main/resources/less
```

The script fails when the build after the removal has any rule that the build before did not have.

## Pixel diff of the removal

Slice 2d removed the four families with no visual change. The proof is a pixel diff with threshold 0 of the application
(`awe-tests/awe-boot`, one instance, same data): the screens are captured with the `styles.css` of the tree before the removal
(swapped in) and with the one after it, in Chromium and Firefox, at 1440x900 and 375x812, in the light and the dark theme.
`awe-framework/awe-client-angular/tools/pixel-diff/` holds the script (Playwright and its own PNG comparison); see its
README for how to run it. Result of the removal: 95 captures (15 screens on the desktop viewport, 9 on the phone one, whose menus are behind the hamburger), **95 identical, 0 differing pixels**. The first run found one real difference, an empty column without its 1px height (see above), fixed before the final run.

`styles.css` (webpack production build) went from 1,368,418 to 1,348,917 bytes (-19,501, -1.4%): the removal only drops the
vendored rules, the compat rules were already in the bundle. The rest of the budget comes with the next slices.

## Browsers

Cascade layers need Chrome 99, Firefox 97 or Safari 15.4. Since slice 2d they matter: the grid, the panel classes and the
utilities of this page are served only by rules inside a layer, so a browser without layers drops them and the screens lose their
layout. The slice that removes the rest of the vendored Bootstrap makes them the baseline of the AngularJS client for everything
else; the change is listed in the [AWE 5 migration guide](v5-migration.md).
