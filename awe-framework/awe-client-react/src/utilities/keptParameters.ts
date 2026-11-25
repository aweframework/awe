/**
 * Parameters kept on reload (#993): the criteria with keep-on-reload="true" keep the value the server gives them when
 * their screen is generated in the browser tab (sessionStorage), and the screen request sends it again when the screen
 * is opened without a value for it, as a reload of the browser does (it sends no values). A value sent by the screen the
 * user comes from wins. The AngularJS client applies the same rules (services/keptParameters.js).
 *
 * Framework-agnostic: pure functions over the screen data and a Web Storage.
 */

const PREFIX = "awe-kept-parameters:";

/** A component of the screen data, as the server sends it */
interface KeptComponent {
  id?: string;
  // Any controller: only its keepOnReload attribute is read
  controller?: object;
  model?: {selected?: unknown};
}

/** The part of the Web Storage the rules use */
export interface KeptStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

const isEmpty = (value: unknown): boolean => value === undefined || value === null || value === "" ||
  (Array.isArray(value) && value.length === 0);

const isKept = (component: KeptComponent): boolean => {
  const keepOnReload = (component?.controller as {keepOnReload?: unknown} | undefined)?.keepOnReload;
  return keepOnReload === true || keepOnReload === "true";
};

// A single selected value goes alone, as the form sends the value of a criterion with one value
const asSent = (selected: unknown): unknown => (Array.isArray(selected) && selected.length === 1 ? selected[0] : selected);

/**
 * Values to keep of a screen: the selected value of each criterion with keep-on-reload that has one, as the form sends
 * it (a single value alone, several as a list)
 * @param components Components of the screen data
 * @returns Values by criterion identifier (none when no criterion keeps a value)
 */
export function getKeptParameters(components: KeptComponent[] = []): Record<string, unknown> {
  return Object.fromEntries((components || [])
    .filter(component => component?.id && isKept(component) && !isEmpty(component.model?.selected))
    .map(component => [component.id as string, asSent(component.model?.selected)]));
}

/**
 * Keep the values of a screen in the storage of the tab, under its option. A screen without values to keep leaves the
 * stored ones as they are
 * @param storage Web Storage (sessionStorage), when the browser gives one
 * @param option Option of the screen
 * @param components Components of the screen data
 */
export function storeKeptParameters(storage: KeptStorage | null | undefined, option: string | null | undefined,
                                    components: KeptComponent[] = []): void {
  const kept = getKeptParameters(components);
  if (!storage || !option || Object.keys(kept).length === 0) {
    return;
  }
  try {
    storage.setItem(PREFIX + option, JSON.stringify(kept));
  } catch {
    // A full or blocked storage only loses the values on reload
  }
}

/**
 * Parameters of a screen request with the values kept for the screen: each kept value goes when the parameters have
 * none for it
 * @param storage Web Storage (sessionStorage), when the browser gives one
 * @param option Option of the screen
 * @param parameters Parameters of the request
 * @returns The parameters, with the kept values they lack
 */
export function withKeptParameters<T extends Record<string, unknown>>(storage: KeptStorage | null | undefined,
                                                                      option: string | null | undefined,
                                                                      parameters: T): T {
  if (!storage || !option) {
    return parameters;
  }
  let kept: unknown;
  try {
    kept = JSON.parse(storage.getItem(PREFIX + option) || "null");
  } catch {
    return parameters;
  }
  if (kept === null || typeof kept !== "object" || Array.isArray(kept)) {
    return parameters;
  }
  const missing = Object.entries(kept as Record<string, unknown>)
    .filter(([name, value]) => isEmpty(parameters[name]) && !isEmpty(value));
  return missing.length === 0 ? parameters : {...parameters, ...Object.fromEntries(missing)};
}

/**
 * Storage of the tab, null when the browser does not give one
 * @returns sessionStorage or null
 */
export function getTabStorage(): KeptStorage | null {
  try {
    return typeof window === "undefined" ? null : window.sessionStorage;
  } catch {
    return null;
  }
}
