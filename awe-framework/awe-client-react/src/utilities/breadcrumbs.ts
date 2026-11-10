import type {BreadcrumbLevel} from "../redux/registry/MenuRegistry";

// Levels shown before the hidden ones collapse into an ellipsis: more than this many levels collapse
export const MAX_BREADCRUMB_LEVELS = 4;

/**
 * Levels of the screen that is open: the screens it was opened from (not from the menu) and the menu path of its own
 * option. The menu path belongs to the screen only when it was built for the option of the screen; otherwise (a screen
 * that is not in the menu) the screen is a single level with its title.
 * The origin heals itself when the user comes back to a screen without going through the levels (browser back or
 * forward, a typed address): it is cut at the screen the user is on, and the menu levels of that screen it still holds
 * are dropped when the screen shows its own menu path. A screen opened from one that is on its own menu path (a launcher
 * card that drills down) shows only that menu path, as the screens it was opened from are already in it
 * @param {object[]} origin Levels of the screens the screen was opened from
 * @param {{items: object[], option: string|null}} breadcrumbs Menu path
 * @param {{option?: string, title?: string}} report Open screen
 * @param {string|null} [location] Current location, as the origin levels keep their path (pathname, search and hash)
 * @returns {object[]} Levels, outermost first
 */
export function getBreadcrumbLevels(
  origin: BreadcrumbLevel[],
  breadcrumbs: {items?: BreadcrumbLevel[]; option?: string | null} | undefined,
  report: {option?: string; title?: string} | undefined,
  location?: string | null
): BreadcrumbLevel[] {
  const items = breadcrumbs?.items || [];
  const ownPath = !breadcrumbs?.option || breadcrumbs.option === report?.option;
  const hasOwnPath = ownPath && items.length > 0;
  const cut = location ? origin.findIndex(level => level.path === location) : -1;
  const kept = cut < 0 ? origin : origin.slice(0, cut);
  const ownNames = new Set(items.map(level => level.name));
  const healed = cut >= 0 && hasOwnPath ? kept.filter(level => !level.name || !ownNames.has(level.name)) : kept;
  // A screen opened from one on its own menu path (a launcher card drilling down) repeats that path in the trail
  const left = healed[healed.length - 1]?.option;
  const trail = hasOwnPath && left && ownNames.has(left) ? [] : healed;
  const own = hasOwnPath ? items : (trail.length > 0 && report?.title ? [{label: report.title}] : items);
  return [...trail, ...own];
}

/**
 * Levels to keep after the user leaves the screen for another one that was not launched from the menu: the levels
 * shown, with the last one (the screen) turned into a level that goes back to it
 * @param {object[]} levels Levels shown
 * @param {string} path Path of the screen that is left
 * @param {string} [option] Option of the screen that is left, to know whether the next screen is on its menu path
 * @returns {object[]} Origin levels of the next screen
 */
export function originOf(levels: BreadcrumbLevel[], path: string, option?: string): BreadcrumbLevel[] {
  if (levels.length === 0) {
    return levels;
  }
  const {label} = levels[levels.length - 1];
  return [...levels.slice(0, -1), option ? {label, path, option} : {label, path}];
}

/**
 * Levels to render: all of them, or when there are more than the maximum, the first one, a collapsed marker and the
 * last two. The marker stays when the hidden levels are expanded, in front of them, so it can collapse them again
 * @param {object[]} levels Levels
 * @param {boolean} expanded Hidden levels are shown
 * @returns {object[]} Levels, with `{collapsed: true}` as the marker
 */
export function collapseLevels(levels: BreadcrumbLevel[], expanded: boolean): (BreadcrumbLevel & {collapsed?: boolean})[] {
  if (levels.length <= MAX_BREADCRUMB_LEVELS) {
    return levels;
  }
  return [levels[0], {collapsed: true, label: ""}, ...(expanded ? levels.slice(1) : levels.slice(-2))];
}
