const fs = require('fs');
const path = require('path');
const {PALETTE} = require('../../../src/utilities/echartsOption');

const MAIN_CSS = path.join(__dirname, '../../../src/main.css');
const ANGULAR_TOKENS = path.join(__dirname, '../../../../awe-client-angular/src/main/resources/less/awe/tokens.less');
const ANGULAR_STYLES = path.join(__dirname, '../../../../awe-client-angular/src/main/resources/less/awe.less');
const SHARED_TOKENS = path.join(__dirname, '../../../../awe-design-tokens/tokens.css');

const declaredTokens = file => {
  if (!fs.existsSync(file)) {
    throw new Error(`The design tokens of the AngularJS client were not found at ${file}: the React tokens are checked against them`);
  }
  return Object.fromEntries(
  [...fs.readFileSync(file, 'utf8').matchAll(/^\s*(--awe-[a-z0-9-]+)\s*:\s*([^;]+);/gm)].map(match => [match[1], match[2].trim()])
  );
};

// The stylesheets a file imports, resolved to absolute paths
const importedFiles = (file, pattern) => [...fs.readFileSync(file, 'utf8').matchAll(pattern)]
  .map(match => path.resolve(path.dirname(file), match[1]));

// The AWE 5 token file (#885) is shared by both clients: the AngularJS one declares its current token names after it
// until it adopts the AWE 5 values; the React one reads the file as it is (its PrimeReact theme is token-driven)
describe('AWE 5 token file', () => {
  const shared = declaredTokens(SHARED_TOKENS);

  it('gives the grid header and footer an opaque background, so the rows do not show through them when they scroll', () => {
    const prototype = declaredTokens(path.join(__dirname, '../../../../../website/static/design-system/prototype/tokens.css'));
    [shared, prototype].forEach(tokens => {
      expect(tokens['--awe-color-grid-header']).not.toMatch(/transparent|surface-subtle/);
      expect(tokens['--awe-grid-header-background']).toBe('var(--awe-color-grid-header)');
    });
    expect(shared['--awe-color-grid-header']).toBe(prototype['--awe-color-grid-header']);
  });

  it('is imported by the stylesheet of the React client', () => {
    expect(importedFiles(MAIN_CSS, /@import\s+(?:url\()?["']([^"']+)["']/g)).toContain(SHARED_TOKENS);
  });

  it('is the same file the AngularJS client imports', () => {
    expect(importedFiles(ANGULAR_STYLES, /@import\s+\(inline\)\s+"([^"]+)"/g)).toContain(SHARED_TOKENS);
  });

  it('gives both clients the same token set', () => {
    const react = new Set([...Object.keys(shared), ...Object.keys(declaredTokens(MAIN_CSS))]);
    const angular = new Set([...Object.keys(shared), ...Object.keys(declaredTokens(ANGULAR_TOKENS))]);
    expect([...react].sort()).toEqual([...angular].sort());
    expect(react.size).toBe(Object.keys(shared).length);
  });
});

// The React client dropped the bridge of its current token names (#885, "Por motor"): no --awe-* name is declared
// outside the shared file, and no stylesheet of the client reads a variable of a PrimeReact theme any more
describe('design tokens of the React client', () => {
  const PRIMEREACT_VARIABLE = /var\(--(primary-color|primary-\d+|surface-[a-z0-9]+|text-color|highlight-|focus-ring|maskbg|font-family|border-radius|inline-spacing|content-padding|(blue|green|yellow|cyan|pink|indigo|teal|orange|bluegray|purple|red|gray)-\d+)\b/;
  const stylesheets = dir => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      // The compiled themes are no longer loaded (they are removed from the client)
      return file.endsWith(path.join('static', 'css', 'themes')) ? [] : stylesheets(file);
    }
    return /\.(css|less)$/.test(entry.name) ? [file] : [];
  });

  it('declares no token of its own: the values come from the AWE 5 token file', () => {
    expect(declaredTokens(MAIN_CSS)).toEqual({});
  });

  it('reads no variable of a PrimeReact theme in its stylesheets', () => {
    const offending = stylesheets(path.join(__dirname, '../../../src'))
      .flatMap(file => fs.readFileSync(file, 'utf8').split('\n')
        .filter(line => PRIMEREACT_VARIABLE.test(line))
        .map(line => `${path.relative(path.join(__dirname, '../../..'), file)}: ${line.trim()}`));
    expect(offending).toEqual([]);
  });
});

// The series palette of the charts: the print and server renders cannot read CSS, so they use a copy of the light values
describe('chart series palette', () => {
  const tokens = new Map();
  // The first declaration of a token is the one of the default theme, where emerald is the primary color
  [...fs.readFileSync(SHARED_TOKENS, 'utf8').matchAll(/^\s*(--awe-[a-z0-9-]+)\s*:\s*([^;]+);/gm)]
    .forEach(([, name, value]) => tokens.has(name) || tokens.set(name, value.trim()));
  const resolve = value => {
    const reference = value.match(/^var\((--[a-z0-9-]+)\)$/);
    return reference ? resolve(tokens.get(reference[1])) : value;
  };
  const lightReference = name => tokens.get(name).match(/^light-dark\(\s*(var\([^)]+\))\s*,/)[1];
  const seriesTokens = Array.from({length: 8}, (_, index) => `--awe-color-chart-series-${index + 1}`);

  it('orders the hues so that no series is close to the default primary color', () => {
    const hues = seriesTokens.map(name => lightReference(name).match(/^var\(--awe-(?:palette-)?([a-z]+)-600\)$/)[1]);

    expect(hues).toEqual(['primary', 'orange', 'sky', 'fuchsia', 'lime', 'indigo', 'rose', 'teal']);
  });

  it('is mirrored by the fallback palette of the chart option, in the light values of the default theme', () => {
    expect(PALETTE).toEqual(seriesTokens.map(name => resolve(lightReference(name))));
  });
});
