const fs = require('fs');
const os = require('os');
const path = require('path');
const {spawnSync} = require('child_process');
const PACKAGE = require('../../../package.json');
const HELPER = path.join(__dirname, '../../../package-version.cjs');
const {resolvePackageVersion, verifyPackageVersion} = require(HELPER);

describe('resolvePackageVersion', () => {
  const originalVersion = process.env.AWE_VERSION;

  afterEach(() => {
    if (originalVersion === undefined) {
      delete process.env.AWE_VERSION;
    } else {
      process.env.AWE_VERSION = originalVersion;
    }
  });

  it('returns the AWE_VERSION environment variable when it is set', () => {
    process.env.AWE_VERSION = '5.1.2-SNAPSHOT';

    expect(resolvePackageVersion()).toBe('5.1.2-SNAPSHOT');
  });

  it('trims the AWE_VERSION environment variable', () => {
    process.env.AWE_VERSION = '  5.1.2  ';

    expect(resolvePackageVersion()).toBe('5.1.2');
  });

  it('falls back to the package.json version when AWE_VERSION is not set', () => {
    delete process.env.AWE_VERSION;

    expect(resolvePackageVersion()).toBe(PACKAGE.version);
  });

  it('falls back to the package.json version when AWE_VERSION is blank', () => {
    process.env.AWE_VERSION = '   ';

    expect(resolvePackageVersion()).toBe(PACKAGE.version);
  });

  it('falls back to the package.json version when AWE_VERSION is empty', () => {
    process.env.AWE_VERSION = '';

    expect(resolvePackageVersion()).toBe(PACKAGE.version);
  });

  it('reads the fallback from the package.json next to the helper', () => {
    delete process.env.AWE_VERSION;

    expect(resolvePackageVersion(path.join(__dirname, '../../../package.json'))).toBe(PACKAGE.version);
  });
});

describe('verifyPackageVersion', () => {
  let tmpDir;

  const writePackage = (version) => {
    const file = path.join(tmpDir, 'package.json');
    fs.writeFileSync(file, JSON.stringify({name: 'awe-react-client', version}));
    return file;
  };

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'awe-package-version-'));
  });

  afterEach(() => {
    fs.rmSync(tmpDir, {recursive: true, force: true});
  });

  it('accepts a package.json whose version matches AWE_VERSION', () => {
    expect(() => verifyPackageVersion(writePackage('5.0.0-SNAPSHOT'), {AWE_VERSION: '5.0.0-SNAPSHOT'})).not.toThrow();
  });

  it('fails with both versions in the message when the versions differ', () => {
    expect(() => verifyPackageVersion(writePackage('2.2.5'), {AWE_VERSION: '5.0.0-SNAPSHOT'}))
      .toThrow(/2\.2\.5.*5\.0\.0-SNAPSHOT|5\.0\.0-SNAPSHOT.*2\.2\.5/);
  });

  it('fails when AWE_VERSION is missing', () => {
    expect(() => verifyPackageVersion(writePackage('5.0.0-SNAPSHOT'), {})).toThrow(/AWE_VERSION/);
  });

  it('fails when AWE_VERSION is blank', () => {
    expect(() => verifyPackageVersion(writePackage('5.0.0-SNAPSHOT'), {AWE_VERSION: '  '})).toThrow(/AWE_VERSION/);
  });

  it('fails when the package.json does not exist', () => {
    expect(() => verifyPackageVersion(path.join(tmpDir, 'missing.json'), {AWE_VERSION: '5.0.0'}))
      .toThrow(/missing\.json/);
  });

  describe('command line', () => {
    const run = (file, env) => spawnSync(process.execPath, [HELPER, '--verify', file], {
      env: {PATH: process.env.PATH, ...env},
      encoding: 'utf8'
    });

    it('exits with 0 when the versions match', () => {
      const result = run(writePackage('5.0.0'), {AWE_VERSION: '5.0.0'});

      expect(result.status).toBe(0);
    });

    it('exits with a non-zero code and prints the reason when the versions differ', () => {
      const result = run(writePackage('2.2.5'), {AWE_VERSION: '5.0.0'});

      expect(result.status).not.toBe(0);
      expect(result.stderr).toMatch(/2\.2\.5/);
      expect(result.stderr).toMatch(/5\.0\.0/);
    });

    it('exits with a non-zero code when no package.json path is given', () => {
      const result = spawnSync(process.execPath, [HELPER, '--verify'], {
        env: {PATH: process.env.PATH, AWE_VERSION: '5.0.0'},
        encoding: 'utf8'
      });

      expect(result.status).not.toBe(0);
    });
  });
});
