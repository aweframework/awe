/**
 * Install the dependencies of the React client before the website starts or builds.
 *
 * The component catalog renders the real awe-client-react components, so the awe-live plugin compiles the client sources
 * and needs its node_modules. Installing them here keeps the website jobs of the pipeline unchanged: `npm run build` and
 * `npm start` run this script first. Lifecycle scripts are skipped, the website build compiles what it needs. The
 * devDependencies are always installed (the Lucide sprite reads lucide-static), also when the build image sets
 * NODE_ENV=production, which would omit them.
 */
const fs = require('node:fs');
const path = require('node:path');
const {execSync} = require('node:child_process');

const CLIENT_DIR = path.resolve(__dirname, '..', '..', 'awe-framework', 'awe-client-react');
const INSTALL_COMMAND = 'npm ci --include=dev --ignore-scripts --no-audit --no-fund';

/**
 * Modification time of a file
 *
 * @param {string} file File path
 * @returns {number|undefined} Time in milliseconds, undefined when the file does not exist
 */
const fileMtime = file => fs.statSync(file, {throwIfNoEntry: false})?.mtimeMs;

/**
 * Whether the client dependencies are missing or older than its lock file. npm writes node_modules/.package-lock.json on
 * every install, so a lock file changed after it (a pull that updates a dependency) asks for a new install.
 *
 * @param {string} clientDir Folder of the client
 * @param {(file: string) => number|undefined} mtime Modification time of a file, undefined when it does not exist
 * @returns {boolean} True when the dependencies have to be installed
 */
function needsInstall(clientDir, mtime) {
  const installed = mtime(path.join(clientDir, 'node_modules', '.package-lock.json'));
  return installed === undefined || mtime(path.join(clientDir, 'package-lock.json')) > installed;
}

/**
 * Install the client dependencies when they are missing or out of date
 *
 * @param {{mtime?: (file: string) => number|undefined, run?: (command: string, options: object) => void}} io File
 * times and runner
 */
function ensureClientDeps({mtime = fileMtime, run = execSync} = {}) {
  if (needsInstall(CLIENT_DIR, mtime)) {
    console.log(`[awe-live] Installing the React client dependencies (${INSTALL_COMMAND})`);
    run(INSTALL_COMMAND, {cwd: CLIENT_DIR, stdio: 'inherit'});
  }
}

if (require.main === module) {
  ensureClientDeps();
}

module.exports = {CLIENT_DIR, needsInstall, ensureClientDeps};
