const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '../../..');
const packageConfig = JSON.parse(fs.readFileSync(path.join(ROOT, 'package.json'), 'utf8'));
const declared = Object.keys({...packageConfig.dependencies, ...packageConfig.devDependencies});

// Dependencies nothing imports or configures: they only add install time, audit noise and published weight
const REMOVED = [
  '@babel/types',
  '@redux-devtools/extension',
  '@types/react',
  '@types/react-dom',
  '@types/react-redux',
  'autoprefixer',
  'deep-freeze',
  'postcss-loader',
  'react-helmet',
  'react-transition-group',
  'redux-thunk'
];

// Redux Toolkit bundles redux and reselect: import them through @reduxjs/toolkit so there is one copy of each
const PROVIDED_BY_TOOLKIT = ['redux', 'reselect'];
const major = (name) => Number(packageConfig.dependencies[name].replace(/^[^\d]*/, '').split('.')[0]);

describe('Dependencies of the React client', () => {
  it.each(REMOVED)('does not declare the unused %s', (name) => {
    expect(declared).not.toContain(name);
  });

  const sourceFiles = (dir) => fs.readdirSync(dir, {withFileTypes: true}).flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      return sourceFiles(full);
    }
    return /\.(js|jsx|cjs|mjs|ts|tsx)$/.test(entry.name) ? [full] : [];
  });
  const escape = (name) => name.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');

  it.each(PROVIDED_BY_TOOLKIT)('does not declare %s, which Redux Toolkit provides', (name) => {
    expect(declared).not.toContain(name);
  });

  it('uses Redux Toolkit 2 and react-redux 9', () => {
    expect(major('@reduxjs/toolkit')).toBe(2);
    expect(major('react-redux')).toBe(9);
  });

  // PrimeReact 11 requires React 19
  it('uses React 19 for react and react-dom, on the same version', () => {
    expect(major('react')).toBe(19);
    expect(packageConfig.dependencies['react-dom']).toBe(packageConfig.dependencies.react);
  });

  it('keeps immer on the major Redux Toolkit uses', () => {
    const toolkit = require('@reduxjs/toolkit/package.json');
    const toolkitImmer = Number(toolkit.dependencies.immer.replace(/^[^\d]*/, '').split('.')[0]);

    expect(major('immer')).toBe(toolkitImmer);
  });

  it.each([...REMOVED, ...PROVIDED_BY_TOOLKIT])('has no import or require of the removed %s', (name) => {
    const usage = new RegExp(`(?:from|import|require\\()\\s*['"]${escape(name)}(?:/[^'"]*)?['"]`);
    const files = [
      ...sourceFiles(path.join(ROOT, 'src')),
      ...sourceFiles(path.join(ROOT, 'test')),
      ...fs.readdirSync(ROOT).filter((file) => /\.(js|cjs)$/.test(file)).map((file) => path.join(ROOT, file))
    ];

    expect(files.filter((file) => usage.test(fs.readFileSync(file, 'utf8'))).map((file) => path.relative(ROOT, file)))
      .toEqual([]);
  });

  it('does not reference postcss-loader in the webpack configs', () => {
    const configs = ['webpack.common.js', 'webpack.dev.js', 'webpack.prod.js'];

    expect(configs.filter((file) => /postcss/i.test(fs.readFileSync(path.join(ROOT, file), 'utf8')))).toEqual([]);
  });

  it('keeps postcss, which the packaging tests use to parse the stylesheets', () => {
    expect(declared).toContain('postcss');
  });

  it('does not keep a PostCSS config nothing loads', () => {
    expect(fs.existsSync(path.join(ROOT, 'postcss.config.js'))).toBe(false);
  });

  it('does not depend on moment (dates are handled without it)', () => {
    expect(declared).not.toContain('moment');
  });

  it('keeps lodash on a release without known advisories', () => {
    const [major, minor, patch] = packageConfig.dependencies.lodash.split('.').map(Number);

    expect(major).toBe(4);
    expect(minor > 17 || (minor === 17 && patch >= 21)).toBe(true);
  });
});
