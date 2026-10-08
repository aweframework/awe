const fs = require('fs');
const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');
const PACKAGE = require('../../../package.json');
const WEBPACK = require('../../../webpack.common.js');
const ROOT = path.join(__dirname, '../../..');

// The published package follows the licence of the AWE repository
describe('package licence', () => {
  it('declares the Apache 2.0 licence of the repository', () => {
    expect(PACKAGE.license).toBe('Apache-2.0');
  });

  it('copies the licence text of the repository into the published package', () => {
    const patterns = WEBPACK.plugins
      .filter(plugin => plugin instanceof CopyPlugin)
      .flatMap(plugin => plugin.patterns);
    const licence = patterns.find(pattern => path.basename(pattern.from) === 'LICENSE.md');

    expect(licence).toBeDefined();
    expect(fs.existsSync(path.resolve(ROOT, licence.from))).toBe(true);
  });
});
