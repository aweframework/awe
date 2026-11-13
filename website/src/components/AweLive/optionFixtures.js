/**
 * Options the suggests, and the selects that load them, ask the server for while the user types. The docs have no
 * server: a live example lists `options` as {target action: [{value, label}]} and the request of the client is
 * answered from that list (see simulateFiles.js), filtered by the typed text as the server does.
 * Pure on purpose, so that node:test can run it.
 */

// Text without case or accents, so that "espana" finds "España"
const plain = text => String(text ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

const parse = body => {
  try {
    return JSON.parse(body) ?? {};
  } catch {
    return {};
  }
};

/**
 * Answer of the simulated server to the request of an option search.
 * @param {string} [url] Url of the request of the client
 * @param {string} [body] Body of the request: JSON with `suggest`, the typed text (or the values to check)
 * @param {Object<string, Array<{value: string, label: string}>>} [options] Options by target action
 * @returns {Array|undefined} The client actions of the answer (one `fill` with the datalist), undefined when the
 * request is not simulated
 */
function findOptionAnswer(url, body, options) {
  if (!url || !options) {
    return undefined;
  }
  const path = String(url).split(/[?#]/)[0];
  const target = path.slice(path.lastIndexOf('/') + 1);
  if (!Object.hasOwn(options, target)) {
    return undefined;
  }
  const { suggest } = parse(body);
  const rows = options[target].filter(row => {
    if (Array.isArray(suggest)) {
      return suggest.map(String).includes(String(row.value));
    }
    return plain(row.label).includes(plain(suggest));
  });
  return [{ type: 'fill', parameters: { datalist: { rows } } }];
}

module.exports = { findOptionAnswer };
