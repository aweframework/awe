/* global module */
/**
 * Pure helpers of the pixel diff tool (no browser): the summary and exit code of a run, the choice of the reported pairing of
 * captures and the validation of the options. Covered by src/test/jest/tools/pixelDiffSummary.test.js.
 */

/**
 * A capture is trusted only if the stylesheet route was hit in both of its captures: otherwise both used the served CSS. With two
 * applications (--url-before, --url-after) nothing is swapped, so there is no route to hit.
 */
const stylesheetServed = result => Boolean(result.twoApps)
  || (Boolean(result.routeHits) && result.routeHits.before > 0 && result.routeHits.after > 0);

/**
 * @param {Array<object>} results One entry per screen x browser x viewport x theme: differing (pixels), sizeMismatch, routeHits
 *   {before, after} and, when the screen could not be captured, error
 * @return {{captures: number, identical: number, different: number, errors: number, exitCode: number}} Exit code 0 only when
 *   something was captured and nothing differs, failed or was compared without the stylesheet swap
 */
const summarize = results => {
  const failed = results.filter(result => result.error || !stylesheetServed(result));
  const compared = results.filter(result => !failed.includes(result));
  const different = compared.filter(result => result.sizeMismatch || result.differing !== 0);
  const identical = compared.length - different.length;
  return {
    captures: compared.length,
    identical,
    different: different.length,
    errors: failed.length,
    exitCode: compared.length === 0 || different.length > 0 || failed.length > 0 ? 1 : 0
  };
};

/**
 * Of every comparison of a before capture with an after capture of one screen, the one that differs least: the pair that
 * rendered in the same state. A size mismatch has no pixel count and only wins when every pairing has one.
 * @param {Array<{before: number, after: number, differing: number|null, sizeMismatch?: boolean}>} comparisons Pairings
 * @return {object} The chosen pairing
 */
const bestPairing = comparisons => comparisons.filter(pairing => !pairing.sizeMismatch)
  .reduce((best, pairing) => (best === null || pairing.differing < best.differing ? pairing : best), null) || comparisons[0];

/**
 * @param {string|undefined} value Value of --retries
 * @return {number} A non-negative integer
 */
const parseRetries = value => {
  if (!/^\d+$/.test(String(value))) {
    throw new Error(`--retries must be a non-negative integer, got ${JSON.stringify(value)}`);
  }
  return Number(value);
};

/** @return {string[]} The requested screens that are not in the list of known ones */
const unknownScreens = (requested, known) => requested.filter(name => !known.includes(name));

/**
 * Order of the screens of a capture: the public ones (before the login), the ones of the default module, then the ones of each
 * module together, so a module is selected once. Order inside each group is kept.
 * @param {Array<{name: string, public?: boolean, module?: string}>} screens Screens
 * @return {Array<object>} A new list
 */
const orderScreens = screens => {
  const modules = [...new Set(screens.filter(screen => screen.module).map(screen => screen.module))];
  return [
    ...screens.filter(screen => screen.public),
    ...screens.filter(screen => !screen.public && !screen.module),
    ...modules.flatMap(module => screens.filter(screen => screen.module === module))
  ];
};

const normalizeUrl = value => String(value).trim().replace(/\/+$/, "").toLowerCase();

/**
 * Decide how the two sides are built from the options, and refuse the combinations that would compare a side with itself:
 * a stylesheet swap (--url, --before, --after) or two applications (--url-before, --url-after), never a mix of both.
 * @param {{url?: string, before?: string, after?: string, urlBefore?: string, urlAfter?: string}} options Options
 * @return {{twoApps: boolean, urlBefore: string, urlAfter: string}} The mode and the address of each side
 */
const resolveMode = ({url, before, after, urlBefore, urlAfter}) => {
  const twoApps = Boolean(urlBefore || urlAfter);
  if (twoApps) {
    if (url || before || after) {
      throw new Error("--url-before and --url-after cannot be mixed with --url, --before or --after");
    }
    if (!(urlBefore && urlAfter)) {
      throw new Error("both --url-before and --url-after are needed");
    }
    if (normalizeUrl(urlBefore) === normalizeUrl(urlAfter)) {
      throw new Error("--url-before and --url-after are the same application: nothing would differ");
    }
    return {twoApps, urlBefore, urlAfter};
  }
  if (!url) {
    throw new Error("--url is needed (or --url-before and --url-after)");
  }
  if (!before || !after) {
    throw new Error("--before and --after are needed with --url");
  }
  if (before === after) {
    throw new Error("--before and --after are the same stylesheet: nothing would differ");
  }
  return {twoApps, urlBefore: url, urlAfter: url};
};

module.exports = {resolveMode, orderScreens, summarize, bestPairing, parseRetries, unknownScreens, stylesheetServed};
