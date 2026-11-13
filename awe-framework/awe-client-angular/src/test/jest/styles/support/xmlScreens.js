const fs = require("fs");
const path = require("path");
const {TAILWIND_DIR} = require("./compileTailwind");

/**
 * Folders that sources.css scans: the templates, the directives and the XML screens (application folders) of the
 * framework, the modules and the test applications.
 * The screens of the React client (application/awe-react) are not read: the React client has its own styles.
 * @param {RegExp} [filter] Keeps only the sources whose path matches, all by default
 * @return {string[]} Folders
 */
const sourceFolders = (filter = /./) => {
  const sources = fs.readFileSync(path.join(TAILWIND_DIR, "sources.css"), "utf8");
  return [...sources.matchAll(/@source\s+"([^"]+)"/g)].map(match => match[1])
    .filter(source => filter.test(source))
    .flatMap(source => {
      const [head, ...tail] = source.split("*");
      const base = path.resolve(TAILWIND_DIR, head);
      return tail.length ? fs.readdirSync(base).map(entry => path.join(base, entry, ...tail)) : [base];
    });
};

const walk = folder => (fs.existsSync(folder) ? fs.readdirSync(folder, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(folder, entry.name);
  return entry.isDirectory() ? walk(file) : [file];
}) : []);

/**
 * XML screens that sources.css scans
 * @return {string[]} Files, with forward slashes
 */
const xmlScreens = () => sourceFolders(/\/application$/).flatMap(walk).map(file => file.split(path.sep).join("/"))
  .filter(file => /\/screen\/.+\.xml$/.test(file) && !file.includes("/application/awe-react/"));

/**
 * Every HTML, JavaScript and XML file that sources.css scans (templates, directives and screens)
 * @return {string[]} Files
 */
const sourceFiles = () => sourceFolders().flatMap(walk).filter(file => /\.(html|js|xml)$/.test(file) && !file.includes(`${path.sep}awe-react${path.sep}`));

/**
 * Count the classes written in the `style` attribute of the XML screens
 * @return {Map<string, number>} Uses by class name
 */
const usedXmlClasses = () => {
  const used = new Map();
  xmlScreens().forEach(file => {
    [...fs.readFileSync(file, "utf8").matchAll(/\bstyle\s*=\s*"([^"]*)"/g)].forEach(match => {
      match[1].split(/\s+/).filter(token => /^[A-Za-z][\w-]*$/.test(token)).forEach(token => {
        used.set(token, (used.get(token) || 0) + 1);
      });
    });
  });
  return used;
};

module.exports = {sourceFolders, sourceFiles, walk, xmlScreens, usedXmlClasses};
