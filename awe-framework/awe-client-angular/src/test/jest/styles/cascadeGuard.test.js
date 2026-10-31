import {canMeet, cascadeCandidates, mediaMeet, specificity} from "./support/cascadeGuard";
import {readBaseline, vendoredBeforeRemoval} from "./support/vendoredBeforeRemoval";

// Compiling the LESS build takes several seconds on a loaded CI runner, over the 5 s default
jest.setTimeout(30000);

/**
 * Candidates that were reviewed and cannot change what an element renders. Each says why. Anything else must stay in the LESS: a
 * removed rule that used to beat a rule that is still in the LESS would now lose to it, because the compat rules are layered.
 * (Alternatives such as .panel-default and .panel-primary, or select and textarea, are not candidates: the guard knows no element
 * carries both, see canMeet.)
 */
// Pairs that the guard cannot tell apart from an element that matches both, reviewed by hand: the reason says why they never meet
const REVIEWED = [
  {removed: /^\.progress(\.active \.progress-bar|-bar\.active)$/, kept: /^(\.loader( \*| \.(circlebar|squarebar)-loader > div)|\.screen-skeleton > \*|\[ui-view]\[aria-busy="true"])$/,
    reason: "the striped animation of an active progress bar is the one that reads the motion tokens (it stops under prefers-reduced-motion); the animations of the loaders, the skeleton placeholders and the busy view root are drawn on the elements of those components, and no template writes a progress bar inside them"},
  {removed: /^\.modal\.fade \.modal-dialog$/, kept: /^\.modal-backdrop\.fade$/,
    reason: "the dialog and the backdrop are never one element (the backdrop is a sibling appended to the body); the AWE 5 reduced-motion rule (#885) also lists .modal.fade .modal-dialog itself, which stays in LESS and wins over the compat rule"},
  {removed: /^html input\[disabled]$/, kept: /^\.(grid-quick-filter|signin-mode) input$/,
    reason: "the radios of the quick filters and of the sign-in theme switch are invisible overlays with a pointer cursor: the quick filters set the default cursor on a disabled one in LESS (as the base styles did), and the theme switch radios are never disabled"},
  {removed: /^a\.label:(hover|focus)$/, kept: /^\.signin-language-list > \[(role="option"|aria-selected="true")]$/,
    reason: "the options of the sign-in language list are li elements (signin.js), never an a.label link, so the label hover and focus color never meets their option color"},
  {removed: /^\.has-(success|warning|error) \.(control-label|radio|checkbox)$/, kept: /^\.window-saved-search-field label$/,
    reason: "the fields of the save dialog of the saved searches (savedSearches.js) never sit in a .has-success, .has-warning or .has-error group: the name field shows its error with aria-invalid and its own message"}
];

describe("awe-framework/awe-client-angular/src/test/jest/styles/cascadeGuard", () => {
  describe("specificity", () => {
    it.each([
      [".panel", 100], [".panel-group .panel", 200], ["td", 1], ["body *", 1], ["#a .b", 10100], [".a > .b:hover", 300], [".a:not(.b)", 200], [".a:before", 101]
    ])("%s is %i", (selector, expected) => {
      expect(specificity(selector)).toBe(expected);
    });
  });

  describe("canMeet (can one element match both selectors)", () => {
    it.each([
      [".btn-default.disabled", ".btn-default:hover", true],
      [".btn.disabled", ".btn-default:hover", true],
      [".btn-default.disabled", ".btn-success:hover", false],
      [".btn-lg", ".btn-sm", false],
      ["select.input-group-lg > .form-control", "textarea.input-group-lg > .form-control", false],
      [".panel-info > .panel-heading", ".panel-primary > .panel-heading", false],
      [".panel-info > .panel-heading", ".panel-info > .panel-footer", true],
      [".a > .x", ".b > .x", true],
      // Ancestors need not be the same element: an element can sit inside a .panel-info that sits inside a .panel-primary
      [".panel-info .x", ".panel-primary .x", true],
      [".has-error .form-control", ".has-success .form-control", true],
      [".panel-info .panel-heading", ".panel-primary .panel-heading", true],
      // Compounds linked to the subject by > or + are the same element for both selectors when the combinators agree
      [".panel-info + .x", ".panel-primary + .x", false],
      [".panel-info > .panel-heading > .x", ".panel-primary > .panel-heading > .x", false],
      [".panel-info > .panel-heading .x", ".panel-primary > .panel-heading .x", true],
      [".panel-info > .x", ".panel-primary .x", true],
      [".panel-info > .x", ".panel-primary + .x", true]
    ])("%s and %s: %s", (first, second, expected) => {
      expect(canMeet(first, second)).toBe(expected);
    });
  });

  describe("the later removals (the second step on: buttons, forms, then the dropdowns, navs, navbar, modals, tooltip, popovers and animations)", () => {
    const rule = (selector, declarations) => ({selector, media: "", declarations});
    const pair = step => {
      const winner = rule(".dropdown-menu > li > a", {"border-radius": "3px"});
      const loser = rule(".dropdown-menu a", {"border-top-left-radius": "0"});
      return cascadeCandidates([loser, winner], [{...winner, step}]);
    };

    it("sees a shorthand beat a longhand of a rule that stays, which the first removal never did", () => {
      expect(pair(1)).toEqual([]);
      [2, 3].forEach(step => expect(pair(step).map(found => found.property)).toEqual(["border-radius/border-top-left-radius"]));
    });

    it("sees the same declaration with another value, on any step", () => {
      const winner = rule(".navbar-brand", {"padding-top": "15px"});
      const loser = rule(".navbar-brand", {"padding-top": "10px"});
      [1, 3].forEach(step => expect(cascadeCandidates([loser, winner], [{...winner, step}]).map(found => found.kept)).toEqual([".navbar-brand"]));
    });
  });

  describe("media queries (the third removal)", () => {
    it.each([
      ["", "(min-width: 768px)", true],
      ["(min-width: 768px)", "(min-width: 768px)", true],
      ["(min-width: 768px)", "(min-width: 992px)", true],
      ["(min-width: 768px)", "(max-width: 767px)", false],
      ["(min-width: 768px)", "(max-width: 991px)", true],
      ["(max-width: 767px)", "(min-width: 768px) and (max-width: 991px)", false],
      ["screen and (-webkit-min-device-pixel-ratio: 0)", "(min-width: 768px)", true]
    ])("%s and %s can apply together: %s", (first, second, expected) => {
      expect(mediaMeet(first, second)).toBe(expected);
    });

    const rule = (selector, declarations, media = "") => ({selector, media, declarations});

    it("sees a rule inside a media query beat a base rule that stays, which the earlier removals never did", () => {
      const winner = rule(".nav-justified > li", {display: "table-cell"}, "(min-width: 768px)");
      const loser = rule(".nav > li", {display: "block"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 2}])).toEqual([]);
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}]).map(found => found.kept)).toEqual([".nav > li"]);
    });

    it("sees a rule on every element (`body *`) meet a class rule, which the earlier removals never did", () => {
      const winner = rule(".navbar", {"min-height": "50px"});
      const loser = rule("body *", {"min-height": "0"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 2}])).toEqual([]);
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}]).map(found => found.kept)).toEqual(["body *"]);
    });

    it("sees the element a class is written on even when no AWE source writes it (a product can): `a` meets .navbar-brand", () => {
      const winner = rule(".navbar-default .navbar-brand", {color: "#777"});
      const loser = rule("a", {color: "#337ab7"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}]).map(found => found.kept)).toEqual(["a"]);
    });
  });

  describe("lists (the sixth removal)", () => {
    const rule = (selector, declarations) => ({selector, media: "", declarations});

    it.each(["ul", "ol"])("sees `%s` meet .list-group, which Bootstrap writes on a list though the AWE sources write it on a div (the pixel diff found 9px instead of 20px)", tag => {
      const winner = rule(".list-group", {"margin-bottom": "20px", "padding-left": "0"});
      const loser = rule(tag, {"margin-bottom": "9px"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 6}]).map(found => `${found.kept} ${found.property}`)).toEqual([`${tag} margin-bottom`]);
    });
  });

  describe("variants of one component (the third removal)", () => {
    const rule = (selector, declarations) => ({selector, media: "", declarations});

    it("sees `.navbar-default` (border-color) beat `.navbar` (border), which are written on one element though no AWE source writes them together", () => {
      const winner = rule(".navbar-default", {"border-color": "#e7e7e7"});
      const loser = rule(".navbar", {border: "1px solid transparent"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 2}])).toEqual([]);
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}]).map(found => found.kept)).toEqual([".navbar"]);
    });

    it("sees `.navbar-right` beat `.navbar-text`", () => {
      const winner = rule(".navbar-right", {"margin-right": "-11px"});
      const loser = rule(".navbar-text", {"margin-right": "11px"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}]).map(found => found.kept)).toEqual([".navbar-text"]);
    });

    it("does not meet classes of another component", () => {
      const winner = rule(".navbar-right", {"margin-right": "-11px"});
      const loser = rule(".panel-title", {"margin-right": "11px"});
      expect(cascadeCandidates([loser, winner], [{...winner, step: 3}])).toEqual([]);
    });
  });

  describe("a rule that a compat rule serves", () => {
    let candidates;

    beforeAll(async () => {
      candidates = cascadeCandidates(await vendoredBeforeRemoval(), readBaseline().served);
    });

    const reviewed = pair => REVIEWED.some(entry => entry.removed.test(pair.removed) && entry.kept.test(pair.kept));

    it("does not beat, by specificity or order, a rule that stays in the LESS (unlayered) with another value on the same property", () => {
      expect(candidates.filter(pair => !reviewed(pair)).map(pair => `${pair.removed} over ${pair.kept} { ${pair.property}: ${pair.removedValue} vs ${pair.keptValue} }`))
        .toEqual([]);
    });

    it("keeps only reviewed exclusions that still apply, each with its reason", () => {
      REVIEWED.forEach(entry => {
        expect(entry.reason.length).toBeGreaterThan(20);
        expect(candidates.some(pair => entry.removed.test(pair.removed) && entry.kept.test(pair.kept))).toBe(true);
      });
    });
  });
});
