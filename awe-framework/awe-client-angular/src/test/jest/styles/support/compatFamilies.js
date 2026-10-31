/**
 * The families of slice 5 of #755 (the Bootstrap 3 modals, close button, dropdowns, navs, navbar, popovers, tooltip and component animations):
 * what each compat file serves and what stays in LESS, as data for compatFamily.js (the checks) and for the tests of the removal.
 * A vendored selector is in a family when it mentions one of its classes (see `use`) and every class it names is in `group` or `context`.
 * A selector with another class stays in LESS as a whole, because a rule never half moves, and the class has to be in `leftOut` with a reason.
 */
const ID = {match: name => name.startsWith("#"), reason: "selectors scoped to an element id (the main navbar and the main menu of pixel-admin): application chrome that stays in LESS"};
const THEME = {match: name => /^theme-/.test(name) || ["awe-theme", "base-theme"].includes(name), reason: "selectors that depend on a theme class of the body: themed, they stay with the theme rules"};

const animations = {
  title: "component animations",
  key: "animations",
  file: "component-animations.css",
  vendoredFile: "component-animations.less",
  use: name => ["fade", "collapse", "collapsing"].includes(name),
  group: ["fade", "collapse", "collapsing"],
  context: ["in"],
  sample: ".collapsing",
  after: ["input-groups-pixel-admin.css"],
  before: [],
  leftOut: [
    {names: ["modal", "modal-dialog", "modal-backdrop"], servedIn: "modals.css", examples: [".modal.fade .modal-dialog", ".modal-backdrop.fade"],
      reason: "the fade of a modal and of its backdrop: written with the modal rules (modals.css), which serve them"},
    {names: ["side-panel", "side-panel-right", "side-panel-left"], examples: [".modal.side-panel.fade .modal-dialog", ".modal.side-panel.side-panel-right.fade .modal-dialog"],
      reason: "the AWE side panel slides in with its own transition: behavior of the component, not Bootstrap"},
    {names: ["navbar-collapse"], servedIn: "navbar.css", examples: [".navbar-collapse.collapse"], reason: "the collapsed navbar: written with the navbar rules (navbar.css), which serve it"},
    ID
  ]
};

const modals = {
  title: "modals",
  key: "modals",
  file: "modals.css",
  vendoredFile: "modals.less",
  use: name => name === "close" || /^modal(-|$)/.test(name),
  group: ["modal", "modal-open", "modal-dialog", "modal-content", "modal-backdrop", "modal-header", "modal-title", "modal-body", "modal-footer", "modal-scrollbar-measure",
    "modal-lg", "modal-sm", "modal-alert", "modal-info", "modal-danger", "modal-warning", "modal-success", "close"],
  context: ["btn", "btn-block", "btn-group", "fade", "in"],
  sample: ".modal-footer .btn + .btn",
  after: ["component-animations.css"],
  before: [],
  excluded: [{selectors: [".modal-alert .modal-title", ".modal-alert .modal-body"],
    reason: "pixel-admin centres the confirm title and body with !important and the AWE 5 confirm (#885) sets them to the start with its own !important: served layered, the centred one would win over it"}],
  owned: [{selectors: [
    ".modal-alert .modal-header > i",
    ".modal-header .close:hover",
    ".modal-header .close:focus-visible",
    ".modal-content > div:first-child > .modal-body:first-child",
    ".modal.modal-alert .modal-dialog",
    ".modal.modal-alert .modal-content",
    ".modal.modal-alert .modal-content:before",
    ".modal.modal-alert .modal-content:after",
    ".modal.modal-alert .modal-header",
    ".modal.modal-alert .modal-header > i",
    ".modal.modal-alert .modal-title",
    ".modal.modal-alert .modal-body",
    ".modal.modal-alert .modal-footer",
    ".modal.modal-alert .modal-footer .btn + .btn"],
    reason: "Dialog rules of the AWE 5 design system (#885) restyles it in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {names: ["side-panel", "side-panel-right", "side-panel-left", "side-panel-body", "grid-columns-side-panel", "grid-columns-counter", "grid-columns-actions", "grid-column-item", "grid-column-label",
      "grid-column-handle", "grid-column-move", "grid-column-group", "grid-column-group-title", "grid-columns-header", "grid-columns-titles", "grid-columns-title", "grid-columns-close",
      "grid-columns-list", "grid-column-hidden", "grid-columns-reset", "is-loading", "window-advanced-panel", "window-advanced-reset"],
    examples: [".modal.side-panel .modal-dialog", ".modal.side-panel.side-panel-right .modal-dialog", ".modal.grid-columns-side-panel .grid-column-item"],
    reason: "the AWE side panel and the grid column editor and the window advanced search (#790) that are shown in one: component rules that restyle the modal"},
    {names: ["panel-identity", "panel-identity-avatar", "panel-identity-text", "panel-identity-title", "panel-identity-subtitle", "panel-identity-tags"],
      examples: [".modal.side-panel .panel-identity", ".modal.side-panel .panel-identity-avatar", ".modal.side-panel .panel-identity-tags"],
      reason: "the identity header of the AWE side panel (avatar, title, subtitle and tags): component rules that restyle the modal"},
    {names: ["pagination", "dp-off", "dp-item", "dp-nav", "dp-today", "dp-selected", "datepicker-dropdown", "bootstrap-timepicker-widget", "dropdown-menu", "minicolors-panel"],
      examples: [".modal .pagination a.dp-item:hover", ".modal .datepicker-dropdown", ".bootstrap-timepicker-widget .modal-footer", ".modal .minicolors-panel"],
      reason: "the date, time and color pickers of the plugins that are shown inside a modal: they stay with the plugin styles"},
    {names: ["awe-icon", "alert-awe", "grid-column-checkbox", "btn-awe", "btn-primary", "focused", "active"],
      examples: [".modal-alert .modal-header > i > .awe-icon", ".alert.alert-awe .close", ".modal.grid-columns-side-panel .grid-column-item .grid-column-checkbox",
        ".modal.grid-columns-side-panel .grid-columns-actions > .btn-awe", ".modal.grid-columns-side-panel .grid-columns-actions > .btn-awe.btn-primary.active"],
      reason: "the icon of the confirm, the alert close button and the actions of the grid column editor that the AWE 5 design system (#885) styles: component rules in the AWE LESS"},
    {names: ["alert", "alert-dismissable", "alert-dismissible"], servedIn: "alerts.css", examples: [".alert .close", ".alert-dismissable .close"], reason: "the close button of an alert: it belongs to the alerts (alerts.css), which serves it"},
    {names: ["bootbox", "bootbox-sm"], examples: [".bootbox .modal-footer", ".bootbox.bootbox-sm .modal-dialog"], reason: "the bootbox plugin dialogs: they stay with the plugin styles"},
    {names: ["note-editor", "note-dialog", "note-help-dialog", "modal-close"], examples: [".note-editor .note-dialog .note-help-dialog .modal-content", ".note-editor .note-dialog .note-help-dialog .modal-close"], reason: "the summernote dialogs: they stay with the plugin styles"},
    {names: ["awe-popover", "awe-popover-header"], examples: [".awe-popover .modal-footer", ".awe-popover .awe-popover-header .close"], reason: "the AWE popover directive reuses the modal footer and the close button: component rules"},
    {names: ["fa", "fa-times"], examples: [".modal-dialog .close.fa-times", ".modal-alert .modal-header .fa"], reason: "the icon library classes: icon sizes and glyphs stay with the icon styles"},
    {names: ["wizard", "wizard-wrapper"], examples: [".modal-content > .wizard .wizard-wrapper"], reason: "the wizard directive inside a modal: component rules"},
    THEME,
    ID
  ]
};

const popovers = {
  title: "popovers",
  key: "popovers",
  file: "popovers.css",
  vendoredFile: "popovers.less",
  use: name => /^popover(-|$)/.test(name),
  group: ["popover", "popover-title", "popover-content"],
  context: ["arrow", "top", "bottom", "left", "right"],
  sample: ".popover.top > .arrow",
  markers: ["popover-text", "popover-close"],
  after: ["tooltip.css"],
  before: [],
  owned: [{selectors: [
    ".popover .popover-title",
    ".popover .popover-content"],
    reason: "Popover rules of the AWE 5 design system (#885) restyles it in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {names: ["popover-colorful", "popover-dark", "popover-warning", "popover-danger", "popover-success", "popover-info"],
      examples: [".popover-colorful + .popover", ".popover-warning + .popover .popover-title", ".popover-info.popover-dark.popover-colorful + .popover.top .arrow:after"],
      reason: "the colored and dark popover variants of pixel-admin (generated by its color mixins, with the contextual colors): no AWE screen writes the variants"},
    {names: ["help", "ng-hide-add", "ng-hide-remove", "ng-hide-add-active", "ng-hide-remove-active", "bg-icon"], examples: [".help.popover", ".help.popover.ng-hide-add"],
      reason: "the AWE help popover and its show and hide animation: behavior of the component, not Bootstrap"},
    {names: ["note-editor", "note-popover", "btn-group"], examples: [".note-editor .note-popover .popover", ".note-editor .note-popover .popover .popover-content .btn-group + .btn-group"],
      reason: "the summernote popovers: they stay with the plugin styles"},
    {names: ["target-message"], examples: [".target-message.popover-success + .popover", ".target-message.popover-success + .popover .popover-title"],
      reason: "the popover of a target message, whose colors the AWE 5 design system (#885) sets: component rules in the AWE LESS"},
    {names: ["ie8", "in"], examples: [".ie8 .popover.top.in"], reason: "Internet Explorer 8 hacks of pixel-admin: no supported browser applies them"},
    {names: ["editable-container"], examples: [".editable-container.popover"], reason: "the x-editable plugin popover: it stays with the plugin styles"},
    THEME,
    ID
  ]
};

const tooltip = {
  title: "tooltip",
  key: "tooltip",
  file: "tooltip.css",
  vendoredFile: "tooltip.less",
  use: name => /^tooltip(-|$)/.test(name),
  group: ["tooltip", "tooltip-inner", "tooltip-arrow"],
  context: ["top", "bottom", "left", "right", "top-left", "top-right", "bottom-left", "bottom-right", "in"],
  sample: ".tooltip.top .tooltip-arrow",
  after: ["modals.css"],
  before: ["popovers.css"],
  leftOut: [
    {names: ["tooltip-warning", "tooltip-danger", "tooltip-success", "tooltip-info"], examples: [".tooltip-warning + .tooltip .tooltip-inner", ".tooltip-info + .tooltip.bottom-right .tooltip-arrow"],
      reason: "the colored tooltip variants of pixel-admin (generated by its color mixins): no AWE screen writes the variants"},
    {names: ["slider"], examples: [".slider .tooltip.top", ".slider .tooltip-inner"], reason: "the slider plugin draws its own tooltip: it stays with the plugin styles"},
    {names: ["grid-cell-tooltip"], examples: [".grid-cell-tooltip.tooltip", ".grid-cell-tooltip.tooltip .tooltip-inner"], reason: "the AWE grid cell tooltip: component rules that restyle the tooltip"},
    THEME,
    ID
  ]
};

const dropdowns = {
  title: "dropdowns",
  key: "dropdowns",
  file: "dropdowns.css",
  vendoredFile: "dropdowns.less",
  use: name => ["caret", "dropup"].includes(name) || /^dropdown(-|$)/.test(name),
  group: ["caret", "dropup", "dropdown", "dropdown-menu", "dropdown-menu-right", "dropdown-menu-left", "dropdown-header", "dropdown-backdrop", "dropdown-icon",
    "dropdown-xs", "dropdown-sm", "dropdown-md", "dropdown-lg", "dropdown-xl", "dropdown-submenu"],
  context: ["dropdown-toggle", "open", "pull-right", "pull-left", "divider", "btn-group", "btn", "navbar-fixed-bottom", "navbar-right", "active", "disabled"],
  servedBy: ["buttons.css", "button-groups.css"],
  markers: ["dropdown-style"],
  excluded: [{selectors: [".btn > .caret", ".dropup > .btn > .caret"],
    reason: "the caret inside a button keeps its color with !important, and the disabled spans of the pagination in a dark panel heading set their own: the cascade guard cannot prove a caret never sits there"},
  {selectors: [".open .btn.dropdown-toggle"],
    reason: "the open toggle of any button sets an !important background that the themed and colored buttons override with their own !important: served layered, it would repaint every open button of those colors"}],
  sample: ".dropdown-menu > li > a:hover",
  after: ["component-animations.css", "button-groups.css"],
  before: ["modals.css"],
  owned: [{selectors: [
    "ul.dropdown-menu > li > a:hover",
    "ul.dropdown-menu > li > a:focus",
    "ul.dropdown-menu > .active > a",
    "ul.dropdown-menu > .active > a:hover",
    "ul.dropdown-menu > .active > a:focus",
    "ul.dropdown-menu > .disabled > a",
    "ul.dropdown-menu > .disabled > a:hover",
    "ul.dropdown-menu > .disabled > a:focus"],
    reason: "Dropdown item rules of the AWE 5 design system (#885) restyles it in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {names: ["btn-primary", "btn-flat", "btn-awe", "btn-default", "btn-success", "btn-warning", "btn-danger", "btn-info", "btn-link", "btn-facebook", "btn-lg",
      "btn-group-vertical", "btn-group-justified", "input-group-btn", "ui-datepicker-buttonpane", "ui-priority-primary", "ui-spinner-button", "focus"],
    examples: [".open .btn-default.dropdown-toggle", ".open .btn.btn-flat.dropdown-toggle", ".open .btn-info.dropdown-toggle"],
    important: true,
    reason: "the open state of the button colors and of the themed buttons: a themed !important background that a layered copy would turn into a conflict, they stay with the button modifiers"},
    {names: ["navbar-nav", "navbar-inverse", "navbar-default", "navbar"], servedIn: "navbar.css", examples: [".navbar-nav .open .dropdown-menu", ".navbar-inverse .navbar-nav .open .dropdown-menu .divider", ".navbar .dropdown-menu > li > a > .badge"],
      reason: "the dropdowns of the navbar: they belong to the navbar family (navbar.css)"},
    {names: ["nav", "nav-tabs", "nav-justified", "nav-icon", "w-text"], servedIn: "navs.css", examples: [".nav-tabs .dropdown-menu", ".nav-justified > .dropdown .dropdown-menu", ".nav .dropdown > .dropdown-toggle .nav-icon"],
      reason: "the dropdowns inside tabs and navs: they belong to the navs family (navs.css)"},
    {names: ["note-editor", "note-toolbar", "note-color", "note-table", "note-para", "note-color-reset", "note-dimension-picker", "note-dimension-picker-mousecatcher", "note-dimension-picker-unhighlighted",
      "note-dimension-picker-highlighted", "note-palette-title", "right", "checked"], examples: [".note-editor .note-toolbar .dropdown-menu", ".note-editor .dropdown-menu.right"],
    reason: "the summernote toolbar dropdowns: they stay with the plugin styles"},
    {names: ["colorpicker", "colorpicker-visible", "colorpicker-position-top", "colorpicker-position-right", "colorpicker-position-bottom", "colorpicker-position-left"],
      examples: [".colorpicker.dropdown", ".colorpicker-visible .dropdown-menu"], reason: "the color picker plugin: it stays with the plugin styles"},
    {names: ["bootstrap-timepicker-widget", "timepicker-orient-top", "timepicker-orient-right", "timepicker-orient-bottom", "timepicker-orient-left", "modal"],
      examples: [".bootstrap-timepicker-widget.dropdown-menu", ".modal .bootstrap-timepicker-widget.dropdown-menu"], reason: "the time picker plugin: it stays with the plugin styles"},
    {names: ["ui-grid-menu", "ui-grid-menu-inner", "ui-grid-menu-close-button", "ui-grid-pager-control"], examples: [".open > .dropdown-toggle.ui-grid-menu .ui-grid-menu-inner .ui-grid-menu-close-button"],
      reason: "the ui-grid menu and pager buttons inside an open dropdown: they stay with the grid styles"},
    {names: ["tabdrop", "fa"], examples: [".tabdrop > .dropdown-toggle", ".tabdrop > .dropdown-toggle > .fa"], important: true,
      reason: "the bootstrap-tabdrop overflow tab: its !important cursor and icon sizes stay with the tabs of the application"},
    {names: ["badge", "label"], servedIn: "navbar.css", examples: [".navbar .dropdown-menu > li > a > .label"], reason: "badges and labels inside a dropdown: they belong to the labels and the navbar"},
    {names: ["panel-heading", "panel-footer", "window-buttons"], examples: [".panel-heading > .dropdown .dropdown-toggle", ".panel-footer > .pull-right > .btn-group .dropdown-menu", ".window-buttons > .btn-group .dropdown-menu"],
      reason: "dropdowns inside a panel heading or footer and the window buttons: they belong to the panel and the window directive"},
    THEME,
    ID
  ]
};

const navs = {
  title: "navs",
  key: "navs",
  file: "navs.css",
  vendoredFile: "navs.less",
  use: name => name === "nav" || /^nav-/.test(name) || ["tab-content", "tab-pane"].includes(name),
  group: ["nav", "nav-tabs", "nav-pills", "nav-stacked", "nav-justified", "nav-tabs-justified", "nav-divider", "nav-icon", "tab-content", "tab-pane"],
  context: ["active", "disabled", "open", "dropdown", "dropdown-menu", "dropdown-toggle"],
  excluded: [{selectors: [".nav-tabs.nav-justified > li > a"],
    reason: "pixel-admin removes the borders of the justified tab links with !important; the disabled links of the pagination and of the dark panel heading controls set their own !important borders and the cascade guard cannot prove they never nest"}],
  sample: ".nav-tabs > li.active > a",
  after: ["dropdowns.css"],
  before: ["modals.css"],
  owned: [{selectors: [
    ".nav-tabs ul.dropdown-menu"],
    reason: "Dropdown rule of the AWE 5 design system (#885) restyles it in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {servedIn: "navbar.css", names: ["navbar-nav", "navbar", "nav-icon-btn", "nav-icon-btn-success", "nav-icon-btn-warning", "nav-icon-btn-danger", "nav-icon-btn-info", "nav-header", "small-screen-text"],
      examples: [".navbar .nav-icon-btn > a .nav-icon", ".navbar .nav-icon-btn > a .small-screen-text"],
      reason: "the navs of the navbar (navbar-nav, the icon buttons): they belong to the navbar family (navbar.css)"},
    {names: ["badge"], servedIn: "badges.css", examples: [".nav-pills > li > a > .badge"], reason: "the badges inside a pill: they belong to the badges (badges.css), which serves them"},
    {names: ["badge", "label", "badge-success", "badge-warning", "badge-danger", "badge-info", "label-success", "label-warning", "label-danger", "label-info", "fa-caret-down"],
      examples: [".nav-tabs > li > a > .label", ".nav-tabs > li > a > .badge.badge-success", ".nav-tabs > li > a > .fa-caret-down"],
      reason: "badges, labels and icons inside a tab: pixel-admin rules that stay in LESS with the tabs"},
    {names: ["panel-heading", "panel-dark", "panel", "maximizing", "maximized"], examples: [".panel-heading .nav-tabs", ".panel-dark .panel-heading .nav-tabs > li > a", ".tab-pane > div > .panel:not(.maximizing):not(.maximized)"],
      reason: "tabs inside a panel heading and panels inside a tab pane: they belong to the panels"},
    {names: ["avatar", "avatar-icon", "avatar-image", "avatar-text"], examples: [".nav li.avatar", ".nav li.avatar .avatar-icon", ".nav li.avatar .avatar-text"], reason: "the AWE avatar menu entry: component rules of the avatar directive"},
    {names: ["nav-tabs-simple", "nav-tabs-sm", "nav-tabs-xs", "tab-container", "tab-container-sm", "tab-container-lg", "tab-invalid", "tab-content-bordered", "active-add", "active-add-active", "tocify", "w-text", "ui-tabs-active", "ui-tabs-nav",
      "tabdrop", "maximize-handler", "panel-title-icon", "awe-icon"],
      examples: [".tab-container .nav-tabs > li.tabdrop", ".tab-container .nav-tabs > li.active:not(.maximize-handler) > a", ".tab-container .nav-tabs > li > a .panel-title-icon", ".tab-container .nav-tabs > li > a .panel-title-icon > .awe-icon", ".nav-tabs.nav-tabs-xs > li > a", ".tab-container .nav-tabs > li > a", ".nav-tabs > li.tab-invalid > a", ".tab-pane.active-add", ".tocify .nav-pills > li", ".tab-content.tab-content-bordered", ".ui-tabs-nav.nav-tabs-simple > .ui-tabs-active > a"],
      reason: "the AWE tab container, its sizes, the invalid tab marker and the tab pane animation, and the table of contents plugin: behavior of the components, not Bootstrap"},
    THEME,
    ID
  ]
};

const navbar = {
  title: "navbar",
  key: "navbar",
  file: "navbar.css",
  vendoredFile: "navbar.less",
  use: name => /^navbar(-|$)/.test(name),
  group: ["navbar", "navbar-header", "navbar-collapse", "navbar-fixed-top", "navbar-static-top", "navbar-fixed-bottom", "navbar-brand", "navbar-toggle", "navbar-nav", "navbar-form",
    "navbar-btn", "navbar-text", "navbar-left", "navbar-right", "navbar-default", "navbar-inverse", "navbar-link"],
  context: ["dropdown", "dropdown-menu", "dropdown-menu-left", "dropdown-header", "caret", "open", "active", "disabled", "pull-right", "container", "container-fluid", "collapse", "in", "icon-bar",
    "divider", "form-group", "form-control", "form-control-static", "control-label", "input-group", "input-group-btn", "input-group-addon", "radio", "checkbox", "has-feedback",
    "form-control-feedback", "btn-link", "btn-sm", "btn-xs", "nav-icon-btn", "nav-icon", "nav-icon-btn-success", "nav-icon-btn-warning", "nav-icon-btn-danger", "nav-icon-btn-info", "small-screen-text", "label", "badge"],
  servedBy: ["dropdowns.css"],
  markers: ["navbar-form-awe"],
  importantReviewed: [{rule: "#main-wrapper.horizontal", property: "padding-left",
    reason: "the horizontal main wrapper is a container of the navbar and never carries a navbar class; the selector-level guard below covers the real elements"}],
  excluded: [{selectors: [".navbar-header h1"], url: true,
    reason: "an AWE rule paints the logo with a url() written relative to its LESS file: written in another folder the path would no longer resolve, so the selector stays in LESS"}],
  sample: ".navbar-default .navbar-nav > .active > a",
  after: ["dropdowns.css", "navs.css"],
  before: ["modals.css"],
  owned: [{selectors: [
    ".navbar-header h1:after"],
    reason: "Brand rule of the AWE 5 design system (#885) restyles it in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {names: ["mmc", "mme", "main-menu-right", "no-main-menu", "horizontal", "navbar-icon", "navbar-inner", "nav-header", "collapsing", "animate-mm-sm", "animate-mm-md", "animate-mm-lg", "disable-mm-animation", "collapsed"],
      examples: [".mmc #main-navbar #main-menu-toggle .navbar-icon", ".no-main-menu #main-navbar .navbar-header", ".animate-mm-sm #main-navbar .navbar-inner", "#main-navbar .navbar-toggle:not(.collapsed)"],
      reason: "the states of the pixel-admin main navbar and main menu (collapsed, expanded, right, horizontal, animated, right to left): application chrome that stays in LESS"},
    {names: ["info", "info-button-sm", "info-button-md", "info-button-lg"], examples: [".info .navbar-form .info-button-sm"], reason: "the AWE info component inside a navbar form: component rules"},
    THEME,
    ID
  ]
};

/*
 * The families of slice 6 of #755 (the Bootstrap 3 tables, labels, badges, wells, alerts, breadcrumbs, progress bars and pagination), same shape as above
 */
const IE = {match: name => /^ie\d$/.test(name), reason: "Internet Explorer hacks of pixel-admin: no supported browser applies them"};

const tables = {
  title: "tables",
  key: "tables",
  file: "tables.css",
  vendoredFile: "tables.less",
  use: name => ["table", "table-condensed", "table-bordered", "table-striped", "table-hover", "table-responsive"].includes(name),
  group: ["table", "table-condensed", "table-bordered", "table-striped", "table-hover", "table-responsive"],
  context: ["active", "success", "info", "warning", "danger"],
  sample: ".table-hover > tbody > tr:hover",
  excluded: [{selectors: [".table-bordered th", ".table-bordered td"],
    reason: "the print rule sets the table borders with !important and the table themes and the date picker set their own !important borders: served layered, it would win over them"}],
  after: [],
  before: ["forms.css"],
  leftOut: [
    {names: ["panel", "panel-collapse", "panel-body", "wizard-panel"], examples: [".panel > .table", ".panel > .panel-collapse > .table", ".panel > .panel-body + .table", ".wizard-panel > .table"],
      reason: "the tables inside a panel, a collapsible panel and a wizard panel: they belong to the panels (panels-leftovers.less)"},
    {names: ["datepicker"], examples: [".table-striped .datepicker table tr td"], reason: "the date picker plugin inside a striped table: it stays with the plugin styles"},
    {names: ["grid", "frozen-div", "treegrid", "treegrid-sm", "treegrid-md", "treegrid-lg", "component", "column-input", "checkbox", "visible-value", "table-header"],
      examples: [".grid .table.table-bordered th", ".grid .frozen-div .table.table-bordered th", ".treegrid.treegrid-sm .table.table-bordered td.component .column-input", ".treegrid .table-header > .table"],
      reason: "the AWE grid and tree grid components restyle the bordered table: component rules that stay in LESS"},
    THEME,
    ID
  ]
};

const labels = {
  title: "labels",
  key: "labels",
  file: "labels.css",
  vendoredFile: "labels.less",
  use: name => name === "label" || /^label-(default|primary|success|info|warning|danger)$/.test(name),
  group: ["label", "label-default", "label-primary", "label-success", "label-info", "label-warning", "label-danger"],
  context: ["btn", "pull-right", "nav-pills", "active"],
  sample: ".label-primary[href]:hover",
  after: ["navbar.css"],
  before: ["badges.css"],
  leftOut: [
    {servedIn: "navbar.css", names: ["navbar", "dropdown-menu", "nav-icon-btn", "nav-icon-btn-success", "nav-icon-btn-warning", "nav-icon-btn-danger", "nav-icon-btn-info"],
      examples: [".navbar .label", ".navbar .dropdown-menu > li > a > .label", ".navbar .nav-icon-btn > a .label", ".navbar .nav-icon-btn.nav-icon-btn-success .label"],
      reason: "the labels inside the navbar (its dropdowns and icon buttons): they belong to the navbar family (navbar.css), which serves them"},
    {names: ["navigation", "mmc-dropdown-open-ul", "mm-dropdown", "mme", "mmc"], examples: ["#main-menu .navigation .label"],
      reason: "the labels of the pixel-admin main menu: application chrome that stays in LESS"},
    {names: ["nav-tabs", "nav-tabs-simple", "nav-tabs-sm", "nav-tabs-xs", "ui-tabs-nav", "ui-tabs-active"],
      examples: [".nav-tabs > li > a > .label", ".nav-tabs.nav-tabs-sm > li > a > .label", ".nav-tabs:not(.nav-tabs-simple) > li.active > a > .label", ".ui-tabs-nav:not(.nav-tabs-simple) > .ui-tabs-active > a > .label"],
      reason: "the labels inside a tab (pixel-admin sizes and colors of the tab container and of the jQuery UI tabs): they stay with the tabs"},
    {names: ["right-to-left"], examples: [".right-to-left .label.label-tag:before"], reason: "the right to left mirror of the tag label of pixel-admin: no AWE screen is right to left"},
    {names: ["label-tag"], examples: [".label.label-tag", ".label.label-tag:before"], reason: "the tag label of pixel-admin (an arrow shaped label): no AWE screen, template or directive writes it"},
    {names: ["list-group-item"], examples: [".list-group-item > .label", ".list-group-item > .label + .label"], reason: "the labels inside a list group item: pixel-admin rules of the list groups, which stay in LESS"},
    {names: ["panel-heading-controls"], examples: [".panel-heading-controls > .label"], reason: "the labels in the controls of a panel heading: they belong to the panels"},
    THEME,
    ID,
    IE
  ]
};

const badges = {
  title: "badges",
  key: "badges",
  file: "badges.css",
  vendoredFile: "badges.less",
  use: name => name === "badge" || /^badge-(success|warning|danger|info)$/.test(name),
  group: ["badge", "badge-success", "badge-warning", "badge-danger", "badge-info"],
  context: ["btn", "btn-xs", "btn-default", "btn-primary", "btn-success", "btn-info", "btn-warning", "btn-danger", "list-group-item", "active", "nav-pills", "pull-right"],
  sample: ".nav-pills > li > a > .badge",
  after: ["labels.css"],
  before: ["modals.css"],
  leftOut: [
    {servedIn: "navbar.css", names: ["navbar", "dropdown-menu", "nav-icon-btn"], examples: [".navbar .badge", ".navbar .dropdown-menu > li > a > .badge", ".navbar .nav-icon-btn > a .badge"],
      reason: "the badges inside the navbar (its dropdowns and icon buttons): they belong to the navbar family (navbar.css), which serves them"},
    {names: ["navigation", "mmc-dropdown-open-ul", "mm-dropdown", "mme", "mmc"], examples: ["#main-menu .navigation .badge"], reason: "the badges of the pixel-admin main menu: application chrome that stays in LESS"},
    {names: ["nav-tabs", "nav-tabs-simple", "nav-tabs-sm", "nav-tabs-xs", "ui-tabs-nav", "ui-tabs-active"],
      examples: [".nav-tabs > li > a > .badge", ".nav-tabs.nav-tabs-sm > li > a > .badge", ".nav-tabs:not(.nav-tabs-simple) > li.active > a > .badge", ".ui-tabs-nav:not(.nav-tabs-simple) > .ui-tabs-active > a > .badge"],
      reason: "the badges inside a tab (pixel-admin sizes and colors of the tab container and of the jQuery UI tabs): they stay with the tabs"},
    {names: ["panel-primary", "panel-success", "panel-warning", "panel-danger", "panel-default", "panel-info", "panel-heading", "panel-heading-controls"],
      examples: [".panel-primary > .panel-heading .badge", ".panel-default > .panel-heading .badge", ".panel-heading-controls .badge"],
      reason: "the badges in the heading of a colored panel and in the controls of a panel heading: they belong to the panels (panels-leftovers.less)"},
    {names: ["ui-grid-menu", "ui-grid-menu-inner", "ui-grid-menu-close-button", "ui-grid-pager-control"], examples: [".ui-grid-menu .ui-grid-menu-inner .ui-grid-menu-close-button .badge", ".ui-grid-pager-control button .badge"],
      reason: "the badges of the ui-grid menu and pager buttons: they stay with the grid styles"},
    THEME,
    ID,
    IE
  ]
};

const wells = {
  title: "wells",
  key: "wells",
  file: "wells.css",
  vendoredFile: "wells.less",
  use: name => ["well", "well-lg", "well-sm"].includes(name),
  group: ["well", "well-lg", "well-sm"],
  context: [],
  sample: ".well blockquote",
  after: ["panels.css"],
  before: ["modals.css"],
  leftOut: [
    {names: ["editable-date"], examples: [".editable-date.well"], reason: "the date editor of the x-editable plugin: it stays with the plugin styles"},
    THEME,
    ID
  ]
};

const alerts = {
  title: "alerts",
  key: "alerts",
  file: "alerts.css",
  vendoredFile: "alerts.less",
  use: name => name === "alert" || /^alert-(link|dismissable|dismissible|success|info|warning|danger)$/.test(name),
  group: ["alert", "alert-link", "alert-dismissable", "alert-dismissible", "alert-success", "alert-info", "alert-warning", "alert-danger"],
  context: ["close"],
  sample: ".alert-success hr",
  after: ["badges.css"],
  before: ["wells.css"],
  leftOut: [
    {names: ["alert-dark", "alert-page"], examples: [".alert.alert-dark", ".alert.alert-danger.alert-dark", ".alert.alert-page"],
      reason: "the dark and the page alerts of pixel-admin (the message zone writes alert alert-dark alert-awe): component rules that stay in LESS"},
    {names: ["alert-awe", "description", "ng-enter", "ng-leave", "ng-enter-active", "ng-leave-active"],
      examples: [".alert.alert-awe", ".alert.alert-awe > div", ".alert.alert-awe .description", ".alert.ng-enter", ".alert.ng-enter.ng-enter-active", ".alert.ng-leave", ".alert.ng-leave.ng-leave-active"],
      reason: "the AWE 5 message toast (#885, less/awe/controllers/message.less) and its enter and leave animation: component rules in the AWE LESS, unlayered, that win over the alert rules"},
    {names: ["demo-theme"], examples: [".demo-theme .alert strong"], reason: "the demo theme of pixel-admin: it depends on a theme class of the body"},
    THEME,
    ID
  ]
};

const breadcrumbs = {
  title: "breadcrumbs",
  key: "breadcrumbs",
  file: "breadcrumbs.css",
  vendoredFile: "breadcrumbs.less",
  use: name => /^breadcrumb(-|$)/.test(name),
  group: ["breadcrumb", "breadcrumb-page", "breadcrumb-label"],
  context: ["active"],
  sample: ".breadcrumb > li + li:before",
  after: ["navbar.css"],
  before: ["labels.css"],
  owned: [{selectors: [
    ".breadcrumb.breadcrumb-page > li",
    ".breadcrumb.breadcrumb-page > li:before",
    ".breadcrumb.breadcrumb-page > li.active",
    ".breadcrumb.breadcrumb-page .breadcrumb-label"],
    reason: "The breadcrumb bar of the AWE 5 shell (#885, less/awe/breadcrumbs.less) restyles the levels, the separator and the label in the AWE LESS (unlayered, so it wins over the compat rule): it stays there, written once"}],
  leftOut: [
    {names: ["breadcrumb-dark", "breadcrumb-no-padding"], examples: [".breadcrumb.breadcrumb-dark > li + li:before", ".breadcrumb.breadcrumb-dark li", ".breadcrumb.breadcrumb-no-padding"],
      reason: "the dark and the unpadded breadcrumb of pixel-admin: no AWE screen, template or directive writes them"},
    THEME,
    ID
  ]
};

const progressBars = {
  title: "progress bars",
  key: "progress-bars",
  file: "progress-bars.css",
  vendoredFile: "progress-bars.less",
  use: name => ["progress", "progress-bar", "progress-striped"].includes(name) || /^progress-bar-(striped|success|info|warning|danger)$/.test(name),
  group: ["progress", "progress-bar", "progress-striped", "progress-bar-striped", "progress-bar-success", "progress-bar-info", "progress-bar-warning", "progress-bar-danger"],
  context: ["active"],
  sample: ".progress-bar.active",
  after: ["alerts.css"],
  before: ["wells.css"],
  leftOut: [
    {names: ["panel-heading-controls"], examples: [".panel-heading-controls > .progress"], reason: "the progress bar in the controls of a panel heading: it belongs to the panels"},
    {names: ["dz-preview"], examples: [".dz-preview .progress"], reason: "the progress bar of a dropzone preview: it stays with the plugin styles"},
    {names: ["progress-text", "column-progress", "criterion", "uploader"],
      examples: [".progress .progress-text", ".column-progress .progress", ".column-progress .progress .progress-bar:not([class*=\"progress-bar-\"])", ".criterion.uploader .progress-bar"],
      reason: "the AWE progress column and the uploader criterion restyle the bar (a flat rounded track, no stripes): component rules in the AWE LESS, unlayered, that win over the bar rules"},
    THEME,
    ID
  ]
};

const pagination = {
  title: "pagination",
  key: "pagination",
  file: "pagination.css",
  vendoredFile: "pagination.less",
  use: name => ["pagination", "pagination-lg", "pagination-sm", "pagination-xs"].includes(name),
  group: ["pagination", "pagination-lg", "pagination-sm", "pagination-xs"],
  context: ["active", "disabled"],
  sample: ".pagination > .active > a",
  excluded: [{selectors: [".pagination > li.disabled > a:hover"],
    reason: "pixel-admin keeps the border of a disabled link on hover with !important, and the disabled links of the pagination in a dark panel heading and the justified tabs set their own: served layered, it would win over them"}],
  after: ["breadcrumbs.css"],
  before: ["labels.css"],
  leftOut: [
    {names: ["panel-heading-controls", "panel-dark"], examples: [".panel-heading-controls > .pagination", ".panel-dark .panel-heading-controls > .pagination li a"],
      reason: "the pagination in the controls of a panel heading and in a dark one: they belong to the panels"},
    {names: ["dp-item", "dp-nav", "dp-nav-right", "dp-nav-left", "dp-off", "dp-today", "dp-selected", "dp-item-sm", "dp-nav-sm", "dp-item-lg", "dp-nav-lg", "modal"],
      examples: [".pagination a.dp-item", ".pagination a.dp-nav.dp-nav-right", ".pagination a.dp-nav.dp-nav-left:after", ".pagination a.dp-off", ".pagination a.dp-today", ".modal .pagination a.dp-selected", ".pagination a.dp-item.dp-item-lg"],
      reason: "the date picker plugin draws its calendar as a pagination: it stays with the plugin styles"},
    {names: ["dataTables_paginate"], examples: [".dataTables_paginate .pagination > li > a"], reason: "the DataTables plugin pager: it stays with the plugin styles"},
    {names: ["grid", "treegrid", "awe-icon"], examples: [".grid .pagination", ".grid .pagination > li > a .awe-icon", ".treegrid .pagination-sm a"],
      reason: "the pager of the AWE grid and tree grid (the grid footer writes pagination pagination-size): component rules in the AWE LESS, unlayered, that win over the pagination rules"},
    THEME,
    ID
  ]
};

const listGroup = {
  title: "list groups",
  key: "list-group",
  file: "list-group.css",
  vendoredFile: "list-group.less",
  use: name => /^list-group(-|$)/.test(name),
  group: ["list-group", "list-group-item", "list-group-item-heading", "list-group-item-text", "list-group-item-success", "list-group-item-info", "list-group-item-warning", "list-group-item-danger"],
  context: ["active", "disabled", "small"],
  sample: "a.list-group-item:hover",
  after: ["alerts.css"],
  before: ["wells.css"],
  leftOut: [
    {servedIn: "badges.css", names: ["badge"], examples: [".list-group-item > .badge", ".list-group-item.active > .badge", ".list-group-item > .badge + .badge", "a.list-group-item.active .badge"],
      reason: "the badges inside a list group item: they belong to the badges family (badges.css), which serves them"},
    {names: ["label"], examples: [".list-group-item > .label", ".list-group-item > .label + .label"], reason: "the labels inside a list group item: pixel-admin rules of the list groups, which stay in LESS"},
    {names: ["list-group-icon"], examples: [".list-group-icon"], reason: "the icon of a list group item of pixel-admin: no AWE screen, template or directive writes it"},
    {names: ["panel", "panel-collapse", "panel-heading", "panel-footer", "panel-group"],
      examples: [".panel > .list-group", ".panel > .panel-collapse > .list-group", ".panel > .list-group .list-group-item", ".panel-heading + .list-group .list-group-item:first-child", ".list-group + .panel-footer", ".panel-group .panel-heading + .panel-collapse > .list-group"],
      reason: "the list groups inside a panel: they stay in panels-leftovers.less with the rest of the panel rules that compete with pixel-admin"},
    {names: ["wizard-panel"], examples: [".wizard-panel > .list-group", ".wizard-panel > .list-group .list-group-item"], reason: "the list groups of the pixel-admin wizard panel: application chrome that stays in LESS"},
    {names: ["panel-dark", "panel-body-colorful", "panel-warning", "panel-danger", "panel-success", "panel-info"],
      examples: [".panel.panel-dark.panel-body-colorful .list-group", ".panel-warning.panel-dark.panel-body-colorful .list-group-item"], reason: "the list groups of the colorful dark panels of pixel-admin: they stay with the panel colors in LESS"},
    THEME,
    ID,
    IE
  ]
};
const thumbnails = {
  title: "thumbnails",
  key: "thumbnails",
  file: "thumbnails.css",
  vendoredFile: "thumbnails.less",
  use: name => name === "thumbnail",
  group: ["thumbnail"],
  context: ["caption", "active"],
  sample: "a.thumbnail:hover",
  after: ["badges.css"],
  before: ["alerts.css"],
  leftOut: [THEME, ID]
};
const responsiveEmbed = {
  title: "responsive embeds",
  key: "responsive-embed",
  file: "responsive-embed.css",
  vendoredFile: "responsive-embed.less",
  use: name => /^embed-responsive(-|$)/.test(name),
  group: ["embed-responsive", "embed-responsive-item", "embed-responsive-16by9", "embed-responsive-4by3"],
  context: [],
  sample: ".embed-responsive.embed-responsive-16by9",
  after: ["list-group.css"],
  before: ["wells.css"],
  leftOut: [THEME, ID]
};
module.exports = {listGroup, thumbnails, responsiveEmbed, animations, modals, popovers, tooltip, dropdowns, navs, navbar, tables, labels, badges, wells, alerts, breadcrumbs, progressBars, pagination};
