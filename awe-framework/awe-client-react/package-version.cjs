const fs = require('fs');
const path = require('path');

/**
 * Resolves the version published in the generated dist/package.json.
 *
 * The npm package follows the framework version: Maven exposes it to the build through the AWE_VERSION
 * environment variable. When it is missing or blank (e.g. a plain `npm run build`), the version declared
 * in package.json is used.
 *
 * @param {string} [packageJsonPath] Path of the package.json used as fallback
 * @returns {string} Package version
 */
const resolvePackageVersion = (packageJsonPath = path.join(__dirname, 'package.json')) => {
  const mavenVersion = (process.env.AWE_VERSION || '').trim();
  return mavenVersion || require(packageJsonPath).version;
};

/**
 * Verifies that a package.json carries the version Maven asked for, so a build that did not receive
 * AWE_VERSION (or ignored it) fails instead of publishing a stale version.
 *
 * @param {string} packageJsonPath Path of the package.json to check (e.g. dist/package.json)
 * @param {Object} [env] Environment holding AWE_VERSION
 * @throws {Error} When AWE_VERSION is missing/blank, the file cannot be read or the versions differ
 */
const verifyPackageVersion = (packageJsonPath, env = process.env) => {
  const expected = (env.AWE_VERSION || '').trim();
  if (!expected) {
    throw new Error('AWE_VERSION is missing or blank: the npm package version cannot be verified against the Maven version');
  }
  let actual;
  try {
    actual = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8')).version;
  } catch (error) {
    throw new Error(`Cannot read the package version from ${packageJsonPath}: ${error.message}`);
  }
  if (actual !== expected) {
    throw new Error(`Version mismatch in ${packageJsonPath}: found "${actual}" but the Maven version (AWE_VERSION) is "${expected}"`);
  }
};

if (require.main === module) {
  const [flag, file] = process.argv.slice(2);
  try {
    if (flag !== '--verify' || !file) {
      throw new Error('Usage: node package-version.cjs --verify <package.json>');
    }
    verifyPackageVersion(file);
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}

module.exports = {resolvePackageVersion, verifyPackageVersion};
