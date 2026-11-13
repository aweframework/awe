const { findFileFixture } = require('./fileFixtures');
const { findOptionAnswer } = require('./optionFixtures');

const urlOf = input => (typeof input === 'string' ? input : input?.url);

/**
 * The simulator of the file requests of one window: the viewers of the client ask the server for their file with fetch,
 * which has nothing to answer in the docs, so while an example is mounted the fetch of the window answers them with a
 * static file. The patch of window.fetch is the only way in: the viewers call it directly. It is installed with the
 * first example, shared by the ones mounted at the same time and removed with the last one.
 * A function of the window it patches (and of its logger) so that node:test can run it with a fake one.
 *
 * @param {{fetch: function}} [win] Window whose fetch is patched; without one nothing is simulated
 * @param {{warn: function}} [logger] Where a fixture that cannot be read is reported
 * @returns {{simulate: function(Object<string, string>): function(): void}} simulate({target action: static file url})
 * returns the function that stops it
 */
function createFileSimulator(win, logger = console) {
  const registry = new Set();
  let originalFetch = null;
  let patched = null;

  function simulatedFetch(input, init) {
    for (const entry of registry) {
      const answer = findOptionAnswer(urlOf(input), init?.body, entry.options);
      if (answer) {
        return Promise.resolve(new Response(JSON.stringify(answer), { status: 200, headers: { 'Content-Type': 'application/json' } }));
      }
      const file = findFileFixture(urlOf(input), entry.files);
      if (file) {
        // The viewers read a file as the server sends it: status 201 with its content. A fixture that cannot be read
        // is not hidden behind that status: the failure reaches the viewer and the console
        return originalFetch(file).then(response => {
          if (!response.ok) {
            logger.warn(`The simulated file ${file} could not be read (status ${response.status})`);
            return response;
          }
          return new Response(response.body, { status: 201, headers: response.headers });
        }, error => {
          logger.warn(`The simulated file ${file} could not be read`, error);
          throw error;
        });
      }
    }
    return originalFetch(input, init);
  }

  function register(entry) {
    registry.add(entry);
    if (!originalFetch) {
      originalFetch = win.fetch;
      patched = (input, init) => simulatedFetch(input, init);
      win.fetch = patched;
    }
    return () => {
      registry.delete(entry);
      // Another patch on top of ours is not ours to remove, and it may still call ours: then ours stays installed,
      // passing every request through to the real fetch until an example is mounted again
      if (registry.size === 0 && win.fetch === patched) {
        win.fetch = originalFetch;
        originalFetch = null;
        patched = null;
      }
    };
  }

  const isEmpty = items => !items || Object.keys(items).length === 0;

  // One entry by call: two examples with the same object stop independently
  function simulate(files) {
    return isEmpty(files) || !win ? () => {} : register({ files });
  }

  function simulateOptions(options) {
    return isEmpty(options) || !win ? () => {} : register({ options });
  }

  return { simulate, simulateOptions };
}

/**
 * Puts the static file urls of an example under the base url of the site.
 * @param {Object<string, string>} [files] Static file url by target action
 * @param {function(string): string} withBaseUrl Adds the base url of the site to a url
 * @returns {Object<string, string>|undefined} The files with their final urls
 */
function withBaseUrls(files, withBaseUrl) {
  return files && Object.fromEntries(Object.entries(files).map(([target, url]) => [target, withBaseUrl(url)]));
}

const simulator = createFileSimulator(typeof window === 'undefined' ? undefined : window);

/**
 * Answers the requests of the file and PDF viewers for the target actions of `files` ({target action: static file
 * url}) with the static file, while the example is mounted. Any other request goes to the real fetch.
 * @param {Object<string, string>} [files] Static file url by target action
 * @returns {function(): void} Stops simulating
 */
const simulateFiles = files => simulator.simulate(files);

/**
 * Answers the searches of the suggests for the target actions of `options` ({target action: [{value, label}]}) from
 * that list, filtered by the typed text, while the example is mounted. Any other request goes to the real fetch.
 * @param {Object<string, Array>} [options] Options by target action
 * @returns {function(): void} Stops simulating
 */
const simulateOptions = options => simulator.simulateOptions(options);

module.exports = { createFileSimulator, simulateFiles, simulateOptions, withBaseUrls };
