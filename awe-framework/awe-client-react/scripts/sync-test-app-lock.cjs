/**
 * Refreshes the package-lock.json of the React test application (awe-tests/awe-boot-react) after a change of the
 * dependencies of the React client.
 *
 * The test application depends on the client through `file:../../awe-framework/awe-client-react/dist`, which only exists
 * after a client build, so a Renovate update of the client package.json cannot refresh that lock by itself and
 * applicationDependenciesTest fails ("<name>: lock <old>, client <new>"). Renovate runs this script as a post-upgrade
 * task: when there is no dist/package.json it writes a stub with the dependencies of the lock entry at the versions the
 * client declares, refreshes the lock without installing anything and removes the stub.
 *
 * A plain `npm install` keeps the lock entry of an unchanged file: specifier, so the script installs the dist folder
 * explicitly and then restores the package.json of the test application, which npm rewrites with its own key order.
 *
 * Usage (any working directory): node awe-framework/awe-client-react/scripts/sync-test-app-lock.cjs
 */
const fs = require('fs');
const path = require('path');
const {spawnSync} = require('child_process');

const CLIENT = path.join(__dirname, '..');
const DIST = path.join(CLIENT, 'dist');
const STUB = path.join(DIST, 'package.json');
const TEST_APPLICATION = path.join(CLIENT, '../../awe-tests/awe-boot-react');
const TEST_APPLICATION_PACKAGE = path.join(TEST_APPLICATION, 'package.json');
const LOCK_ENTRY = 'node_modules/awe-react-client';

const readJson = (file) => JSON.parse(fs.readFileSync(file, 'utf8'));

/**
 * Builds the package.json of the client as the test application lock sees it: the name, version, license, engines and
 * the dependency names of the lock entry, with the versions the client package.json declares.
 * @param {object} lock package-lock.json of the test application
 * @param {object} clientPackage package.json of the client
 * @returns {object} stub package.json for the dist folder
 */
function buildStubPackage(lock, clientPackage) {
  const entry = lock.packages && lock.packages[LOCK_ENTRY];
  if (!entry) {
    throw new Error(`The lock of the test application has no "${LOCK_ENTRY}" entry`);
  }
  const declared = clientPackage.dependencies || {};
  const missing = Object.keys(entry.dependencies || {}).filter((name) => !(name in declared));
  if (missing.length > 0) {
    throw new Error(`The client package.json does not declare the dependencies of the lock: ${missing.join(', ')}`);
  }
  const dependencies = Object.fromEntries(Object.keys(entry.dependencies || {}).map((name) => [name, declared[name]]));
  return {
    name: 'awe-react-client',
    version: entry.version,
    license: entry.license,
    dependencies,
    ...(entry.engines ? {engines: entry.engines} : {})
  };
}

function main() {
  if (fs.existsSync(STUB)) {
    console.log(`${STUB} exists: refreshing the lock with the built client`);
    return refreshLock();
  }
  const createdDist = !fs.existsSync(DIST);
  try {
    const stub = buildStubPackage(
      readJson(path.join(TEST_APPLICATION, 'package-lock.json')),
      readJson(path.join(CLIENT, 'package.json'))
    );
    fs.mkdirSync(DIST, {recursive: true});
    fs.writeFileSync(STUB, `${JSON.stringify(stub, null, 2)}\n`);
    return refreshLock();
  } catch (error) {
    console.error(`sync-test-app-lock: ${error.message}`);
    return 1;
  } finally {
    fs.rmSync(STUB, {force: true});
    if (createdDist) {
      fs.rmSync(DIST, {recursive: true, force: true});
    }
  }
}

function refreshLock() {
  const packageText = fs.readFileSync(TEST_APPLICATION_PACKAGE, 'utf8');
  const dist = path.relative(TEST_APPLICATION, DIST).split(path.sep).join('/');
  const args = ['install', '--package-lock-only', '--ignore-scripts', '--no-audit', '--no-fund', dist];
  let result;
  try {
    result = spawnSync('npm', args, {cwd: TEST_APPLICATION, stdio: 'inherit', shell: process.platform === 'win32'});
  } finally {
    fs.writeFileSync(TEST_APPLICATION_PACKAGE, packageText);
  }
  if (result.error) {
    console.error(`sync-test-app-lock: ${result.error.message}`);
    return 1;
  }
  return result.status === null ? 1 : result.status;
}

module.exports = {buildStubPackage};

if (require.main === module) {
  process.exitCode = main();
}
