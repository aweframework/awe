const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {spawnSync} = require('node:child_process');

const SITE = path.resolve(__dirname, '../../../..');
// Where the examples of the catalog name the files they serve from static/live
const SOURCES = ['src/components/AwePlayground/specs', 'docs/components', 'i18n/es/docusaurus-plugin-content-docs/current/components'];

const filesOf = dir => fs.readdirSync(dir, {withFileTypes: true})
  .flatMap(entry => (entry.isDirectory() ? filesOf(path.join(dir, entry.name)) : [path.join(dir, entry.name)]));

const referenced = () => new Set(SOURCES.flatMap(source => filesOf(path.join(SITE, source)))
  .flatMap(file => fs.readFileSync(file, 'utf8').match(/\/live\/[\w./-]+\.\w+/g) ?? []));

test('the examples point at files that exist', () => {
  const urls = [...referenced()];

  assert.ok(urls.length > 0);
  for (const url of urls) assert.ok(fs.existsSync(path.join(SITE, 'static', url)), url);
});

test('the files of the examples are not ignored by git: the published site would not have them', () => {
  for (const url of referenced()) {
    const {status} = spawnSync('git', ['check-ignore', '-q', `static${url}`], {cwd: SITE});
    assert.equal(status, 1, `${url} is ignored by git`);
  }
});
