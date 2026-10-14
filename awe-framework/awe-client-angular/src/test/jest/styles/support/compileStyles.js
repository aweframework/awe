const fs = require("fs");
const path = require("path");
const less = require("less");

const ROOT = path.resolve(__dirname, "../../../../..");
const LESS_DIR = path.join(ROOT, "src", "main", "resources", "less");
const ENTRY = path.join(LESS_DIR, "awe.less");

/**
 * Resolves the webpack style imports ("~package/file") against node_modules, as less-loader does in the real build
 */
class NodeModulesFileManager extends less.FileManager {
  supports(filename) {
    return filename.startsWith("~");
  }

  resolvePackage(name) {
    try {
      return require.resolve(name, {paths: [ROOT]});
    } catch {
      return null;
    }
  }

  loadFile(filename, currentDirectory, options, environment) {
    const base = path.join(ROOT, "node_modules", filename.slice(1));
    const isFile = file => fs.existsSync(file) && fs.statSync(file).isFile();
    const resolved = [base, `${base}.less`, `${base}.css`].find(isFile) || this.resolvePackage(filename.slice(1));
    return super.loadFile(resolved || base, currentDirectory, options, environment);
  }
}

/**
 * Compile a LESS source (the whole AWE stylesheet by default) with the same import resolution as the webpack build
 * @param {string} [source] LESS source, the awe.less entry point by default
 * @param {object} [options] Extra less options (modifyVars...)
 * @return {Promise<string>} Compiled CSS
 */
const compileStyles = (source, options = {}) => {
  const entrySource = source === undefined ? fs.readFileSync(ENTRY, "utf8") : source;
  return less.render(entrySource, {
    filename: ENTRY,
    plugins: [{install: (lessInstance, pluginManager) => pluginManager.addFileManager(new NodeModulesFileManager())}],
    ...options
  }).then(output => output.css);
};

module.exports = {compileStyles, LESS_DIR, ROOT};
