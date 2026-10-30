const fs = require("fs");
const path = require("path");
const {getRules, TAILWIND_DIR} = require("./compileTailwind");
const {normalizeSelector} = require("./compatEquivalence");

/**
 * Rules of one compat file, as written (comments dropped, no Tailwind build): lets a test tell which file serves a selector
 * @param {string} file File name in tailwind/compat (for example `input-groups.css`)
 * @return {Array} Flattened rules in source order
 */
const fileRules = file => getRules(fs.readFileSync(path.join(TAILWIND_DIR, "compat", file), "utf8").replace(/\/\*[\s\S]*?\*\//g, ""));

/**
 * @param {string} file File name in tailwind/compat
 * @return {Set<string>} Normalized selectors that the file writes
 */
const fileSelectors = file => new Set(fileRules(file).map(rule => normalizeSelector(rule.selector)));

/** The compat files of slice 5 (the dropdowns, navs, navbar, modals, tooltip, popovers and component animations) */
const SLICE5_FILES = ["component-animations.css", "dropdowns.css", "navs.css", "navbar.css", "modals.css", "tooltip.css", "popovers.css"];

/** The compat files of slice 6 (the tables, labels, badges, wells, then the alerts, breadcrumbs, progress bars and pagination as they are written) */
const SLICE6_FILES = ["tables.css", "labels.css", "badges.css", "wells.css", "alerts.css", "breadcrumbs.css", "progress-bars.css", "pagination.css"];

/** The compat files of slice 7 (the list groups, thumbnails and responsive embeds that something in the tree still writes) */
const SLICE7_FILES = ["list-group.css", "thumbnails.css", "responsive-embed.css"];

/** The compat files of the families that come after the earlier ones (slices 5, 6 and 7) */
const LATER_FILES = [...SLICE5_FILES, ...SLICE6_FILES, ...SLICE7_FILES];

/**
 * @return {Set<string>} Normalized selectors that a file of slice 5 or 6 writes: a selector of an earlier family that sits in a navbar, a modal, a
 * dropdown or next to a label is served by the file of that family, which has its own test, so the tests of the earlier families do not count it as theirs
 */
const laterSelectors = () => new Set(LATER_FILES.flatMap(file => [...fileSelectors(file)]));

module.exports = {fileRules, fileSelectors, laterSelectors, LATER_FILES, SLICE5_FILES, SLICE6_FILES, SLICE7_FILES};
