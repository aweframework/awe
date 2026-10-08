const path = require('path');
const babel = require('@babel/core');

const ROOT = path.join(__dirname, '../../..');
const SOURCE_FILE = path.join(ROOT, 'src', 'index.js');

// The same inline presets webpack.common.js hands to babel-loader
const LOADER_OPTIONS = {presets: ['@babel/preset-env', '@babel/preset-react']};

const resolveOptions = (envName) => babel.loadOptions({
  ...LOADER_OPTIONS,
  cwd: ROOT,
  filename: SOURCE_FILE,
  envName
});

const pluginNames = (options) => options.plugins.map((plugin) => plugin.file ? plugin.file.request : String(plugin.key));

// Coverage is measured by Jest (babel-jest adds its own instrumentation). Instrumenting the webpack
// builds would ship __coverage__ counters inside the published bundle.
describe('Babel configuration of the webpack builds', () => {
  it.each(['development', 'production'])('does not instrument the %s build with istanbul', (envName) => {
    const names = pluginNames(resolveOptions(envName));

    expect(names.filter((name) => /istanbul/i.test(name))).toEqual([]);
  });

  it('keeps a single Babel config file at the module root', () => {
    const fs = require('fs');

    expect(fs.existsSync(path.join(ROOT, '.babelrc'))).toBe(false);
    expect(fs.existsSync(path.join(ROOT, 'babel.config.js'))).toBe(true);
  });
});
