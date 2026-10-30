/**
 * The families of slice 8 of #755 (the Bootstrap 3 base styles: normalize, print, scaffolding, type and the element rules of code), same shape as compatFamilies.js.
 * The base styles are mostly element rules (body, a, h1, p, ul, code...), which no class finds, so each family lists the exact selectors that its vendored file compiled to
 * (baseStyles.selectors.json, written from the vendored sources before they were removed); the classes of the family (.h1, .text-muted, .img-responsive...) are in `group`.
 */
const SELECTORS = require("./baseStyles.selectors.json");

const THEME = {match: name => /^theme-/.test(name) || ["awe-theme", "base-theme"].includes(name), reason: "selectors that depend on a theme class of the body: themed, they stay with the theme rules"};
const ID = {match: name => name.startsWith("#"), reason: "selectors scoped to an element id (the main navbar and the main menu of pixel-admin): application chrome that stays in LESS"};
const IE = {match: name => /^ie\d$/.test(name), reason: "Internet Explorer hacks of pixel-admin: no supported browser applies them"};

const ROOT = {selectors: ["html"],
  reason: "the root of the page: its 10px font size is the base that every rem based rule of the AWE LESS and of the plugins is calculated from, and no token holds it (a token value would rescale them all)"};

const normalize = {
  title: "normalize",
  key: "normalize",
  file: "normalize.css",
  vendoredFile: "normalize.less",
  use: () => false,
  group: [],
  context: [],
  selectors: SELECTORS.normalize,
  sample: "button[disabled]",
  after: [],
  before: ["print.css", "scaffolding.css", "type.css", "code.css", "grid.css"],
  owned: [ROOT],
  hazardsReviewed: [
    {hazard: "body { width } over #main-navbar #main-menu-toggle", reason: "the !important width of `body` is a pixel-admin rule of the collapsed menu, not one served here"},
    {hazard: "body { height } over #main-menu.horizontal #main-menu-bg", reason: "the !important height of `body` is a pixel-admin rule of the horizontal menu, not one served here"},
    {hazard: "pre { background-color } over .log-viewer.black-log pre", reason: "the !important background of `pre` is a rule of the AWE log viewer, not one served here; the log viewer sets its own !important on the same element"}
  ],
  leftOut: []
};

const print = {
  title: "print",
  key: "print",
  file: "print.css",
  vendoredFile: "print.less",
  use: () => false,
  group: [],
  context: ["navbar"],
  selectors: SELECTORS.print,
  sample: "thead",
  after: ["normalize.css"],
  before: ["scaffolding.css", "type.css", "code.css", "grid.css"],
  owned: [{selectors: ["*", "*:before", "*:after", "pre", "blockquote", "select", ".btn > .caret", ".dropup > .btn > .caret", ".label", ".table", ".table td", ".table th", ".table-bordered th", ".table-bordered td"],
    reason: "the rules that print black on white (an !important color or background, a fixed grey or black border): no token stays the same in the dark mode, and a layered !important would win over the unlayered ones of the AWE print styles"}],
  leftOut: [
    {names: ["btn", "caret", "dropup"], examples: [".btn > .caret", ".dropup > .btn > .caret"], reason: "the caret of a button printed black: one of the rules that print black on white, which stay in LESS"},
    {names: ["label"], examples: [".label"], reason: "the border of a label printed black: one of the rules that print black on white, which stay in LESS"},
    {names: ["table", "table-bordered"], examples: [".table", ".table td", ".table-bordered th"], reason: "the table colors and borders of the print styles (an !important white background and grey borders): they stay in LESS with the table rules"}
  ]
};

const scaffolding = {
  title: "scaffolding",
  key: "scaffolding",
  file: "scaffolding.css",
  vendoredFile: "scaffolding.less",
  use: name => ["img-responsive", "img-rounded", "img-thumbnail", "img-circle", "sr-only"].includes(name),
  group: ["img-responsive", "img-rounded", "img-thumbnail", "img-circle", "sr-only"],
  context: [],
  selectors: SELECTORS.scaffolding,
  sample: ".img-thumbnail",
  after: ["print.css"],
  before: ["type.css", "code.css", "grid.css"],
  owned: [ROOT],
  hazardsReviewed: [
    {hazard: "body { width } over #main-navbar #main-menu-toggle", reason: "the !important width of `body` is a pixel-admin rule of the collapsed menu, not one served here"},
    {hazard: "body { height } over #main-menu.horizontal #main-menu-bg", reason: "the !important height of `body` is a pixel-admin rule of the horizontal menu, not one served here"}
  ],
  leftOut: [
    {servedIn: "forms.css", names: ["has-feedback", "form-control-feedback"], examples: [".has-feedback label.sr-only ~ .form-control-feedback"],
      reason: "the feedback icon of a form control that has a screen reader only label: it belongs to the forms (forms.css), which serves it"},
    THEME, ID, IE
  ]
};

const TYPE_CLASSES = ["h1", "h2", "h3", "h4", "h5", "h6", "lead", "small", "mark", "text-justify", "text-nowrap", "text-lowercase", "text-uppercase", "text-capitalize", "text-muted", "text-primary",
  "text-success", "text-info", "text-warning", "text-danger", "bg-primary", "bg-success", "bg-info", "bg-warning", "bg-danger", "page-header", "list-unstyled", "list-inline", "dl-horizontal", "initialism",
  "blockquote-reverse"];

const type = {
  title: "type",
  key: "type",
  file: "type.css",
  vendoredFile: "type.less",
  use: name => TYPE_CLASSES.includes(name),
  group: TYPE_CLASSES,
  context: ["pull-right"],
  selectors: SELECTORS.type,
  sample: "blockquote footer:before",
  after: ["scaffolding.css"],
  before: ["code.css", "grid.css"],
  leftOut: [
    {names: ["jumbotron"], examples: [".jumbotron h1", ".jumbotron .h1"], reason: "the headings of the jumbotron that slice 7 dropped (nothing writes it): they are frozen and not served"},
    {names: ["list-group-item", "active", "list-group-item-heading"], examples: [".list-group-item.active .list-group-item-heading > .small"],
      reason: "the small text of the heading of an active list group item: it belongs to the list groups (list-group.css), which serve it"},
    {names: ["panel-title"], examples: [".panel-title > .small", ".panel-title > .small > a"], reason: "the small text in a panel title: pixel-admin rules of the panels, which stay in LESS"},
    THEME, ID, IE
  ]
};

const code = {
  title: "code",
  key: "code",
  file: "code.css",
  vendoredFile: "code-leftovers.less",
  use: () => false,
  group: [],
  context: [],
  selectors: SELECTORS.code,
  sample: "pre code",
  after: ["type.css"],
  before: ["grid.css"],
  hazardsReviewed: [{hazard: "pre { background-color } over .log-viewer.black-log pre",
    reason: "the !important background of `pre` is a rule of the AWE log viewer, not one served here; the log viewer sets its own !important on the same element"}],
  leftOut: []
};

module.exports = {normalize, print, scaffolding, type, code};
