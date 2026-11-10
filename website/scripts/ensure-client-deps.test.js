const test = require('node:test');
const assert = require('node:assert');
const path = require('node:path');
const {CLIENT_DIR, needsInstall, ensureClientDeps} = require('./ensure-client-deps');

test('the client is the React engine next to the website', () => {
  assert.strictEqual(path.relative(path.join(__dirname, '..', '..'), CLIENT_DIR),
    path.join('awe-framework', 'awe-client-react'));
});

const LOCK = path.join('/client', 'package-lock.json');
const INSTALLED = path.join('/client', 'node_modules', '.package-lock.json');

// Modification time of each file, a missing file has none
const mtimes = times => file => times[file];

test('an install is needed when the installed lock file of the client is missing', () => {
  assert.strictEqual(needsInstall('/client', mtimes({[LOCK]: 10})), true);
});

test('no install is needed when the client dependencies were installed after its lock file changed', () => {
  assert.strictEqual(needsInstall('/client', mtimes({[LOCK]: 10, [INSTALLED]: 20})), false);
});

test('an install is needed when the lock file of the client changed after the last install', () => {
  assert.strictEqual(needsInstall('/client', mtimes({[LOCK]: 30, [INSTALLED]: 20})), true);
});

test('a missing install runs npm ci in the client without its lifecycle scripts', () => {
  const runs = [];
  ensureClientDeps({mtime: () => undefined, run: (command, options) => runs.push({command, cwd: options.cwd})});
  assert.deepStrictEqual(runs, [{command: 'npm ci --include=dev --ignore-scripts --no-audit --no-fund', cwd: CLIENT_DIR}]);
});

test('an up-to-date install runs nothing', () => {
  const runs = [];
  ensureClientDeps({mtime: file => (file.endsWith(path.join('node_modules', '.package-lock.json')) ? 20 : 10),
    run: command => runs.push(command)});
  assert.deepStrictEqual(runs, []);
});
