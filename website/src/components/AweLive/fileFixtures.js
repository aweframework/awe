/**
 * Files the file and PDF viewers read from the server (a POST to the REST url of their target action). The docs have
 * no server: a live example lists `files` as {target action: url of a static file} and the request of the viewer is
 * answered with that file (see simulateFiles.js).
 * Pure on purpose, so that node:test can run it.
 *
 * @param {string} [url] Url of the request of the viewer
 * @param {Object<string, string>} [files] Static file url by target action
 * @returns {string|undefined} Url of the static file, undefined when the request is not simulated
 */
function findFileFixture(url, files) {
  if (!url || !files) {
    return undefined;
  }
  const path = String(url).split(/[?#]/)[0];
  const target = path.slice(path.lastIndexOf('/') + 1);
  return Object.hasOwn(files, target) ? files[target] : undefined;
}

module.exports = { findFileFixture };
