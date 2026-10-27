// Shared by both clients (the AngularJS client imports it through its awe-client-react alias): the icon value of the
// screen XML or the data resolved to a Lucide icon of the AWE 5 sprite or, during the transition, to its current font.
// The tables live with the design tokens (awe-design-tokens/icons): the equivalences of the Font Awesome 4,
// Google Material Icons (mdi:) and PrimeIcons names, and the Lucide names the pinned lucide-static provides.
import equivalences from "../../../awe-design-tokens/icons/icon-equivalences.json";
import lucideNames from "../../../awe-design-tokens/icons/lucide-names.json";

const LUCIDE = "lucide";
// Family written in the icon value -> family of the equivalence table
const FAMILIES = {fa: "fa", mdi: "material", pi: "pi"};
// Prefix of a bare name -> family (a bare name without prefix is Font Awesome)
const PREFIXES = {"fa-": "fa", "pi-": "pi"};

const lucideIcons = new Set(lucideNames.icons);
const warned = new Set();

// Own keys only: an icon named like an Object property ("constructor") is not in a table
const lookUp = (table, name) => (table && Object.prototype.hasOwnProperty.call(table, name) ? table[name] : undefined);

const stripPrefix = (family, name) => {
  const prefix = Object.keys(PREFIXES).find(candidate => PREFIXES[candidate] === family);
  return prefix && name.startsWith(prefix) ? name.slice(prefix.length) : name;
};

// Family and name of an icon token: "fa:name", "mdi:name", "lucide:name", "pi:name", "fa-name", "pi-name" or "name"
const parseIcon = token => {
  const separator = token.indexOf(":");
  if (separator > -1) {
    const family = token.slice(0, separator);
    const tableFamily = FAMILIES[family] || family;
    return {family: tableFamily, name: stripPrefix(tableFamily, token.slice(separator + 1))};
  }
  const prefix = Object.keys(PREFIXES).find(candidate => token.startsWith(candidate));
  return prefix ? {family: PREFIXES[prefix], name: token.slice(prefix.length)} : {family: "fa", name: token};
};

const resolveLucide = name => {
  if (lucideIcons.has(name)) {
    return name;
  }
  const current = lookUp(lucideNames.aliases, name);
  if (current) {
    return current;
  }
  if (!warned.has(name)) {
    warned.add(name);
    // eslint-disable-next-line no-console
    console.warn(`[AWE] Unknown Lucide icon "lucide:${name}" (lucide-static ${lucideNames.version}): it is not shown`);
  }
  return null;
};

/**
 * Resolve the icon value of the screen XML or the data
 * @param {string} icon Icon value: the icon ("name", "fa-name", "fa:name", "mdi:name", "lucide:name", "pi-name",
 * "pi:name") and optionally extra classes after it ("level-up icon-rotate-90")
 * @returns {{family: string, name: string, classes: string}|null} The Lucide icon ({family: "lucide"}), or the icon in
 * its current font for a name without Lucide equivalence ({family: "fa" | "material" | "pi" | other family}); null
 * without icon or for an unknown Lucide name (warned once per name)
 */
export function resolveIcon(icon) {
  const [token, ...classes] = String(icon ?? "").trim().split(/\s+/);
  const {family, name} = parseIcon(token);
  if (!name) {
    return null;
  }
  const extraClasses = classes.join(" ");
  if (family === LUCIDE) {
    const lucideName = resolveLucide(name);
    return lucideName ? {family: LUCIDE, name: lucideName, classes: extraClasses} : null;
  }
  const table = Object.values(FAMILIES).includes(family) ? equivalences[family] : undefined;
  const lucideName = lookUp(table, name);
  if (lucideName) {
    return {family: LUCIDE, name: lucideName, classes: extraClasses};
  }
  return {family, name, classes: extraClasses};
}
