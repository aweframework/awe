const fs = require('fs');
const path = require('path');

const MAIN_CSS = path.join(__dirname, '../../../src/main.css');
const ANGULAR_TOKENS = path.join(__dirname, '../../../../awe-client-angular/src/main/resources/less/awe/tokens.less');

const declaredTokens = file => {
  if (!fs.existsSync(file)) {
    throw new Error(`The design tokens of the AngularJS client were not found at ${file}: the React tokens are checked against them`);
  }
  return Object.fromEntries(
  [...fs.readFileSync(file, 'utf8').matchAll(/^\s*(--awe-[a-z0-9-]+)\s*:\s*([^;]+);/gm)].map(match => [match[1], match[2].trim()])
  );
};

// The design tokens (#770) are declared by the AngularJS client; the React client declares the ones that have a
// PrimeReact equivalent so a page reads the same name in both clients
describe('design tokens', () => {
  const react = declaredTokens(MAIN_CSS);
  const angular = declaredTokens(ANGULAR_TOKENS);

  it('declares only tokens of the contract of the AngularJS client, with the same names', () => {
    expect(Object.keys(react).filter(name => !(name in angular))).toEqual([]);
  });

  it('maps the 13 tokens that have a PrimeReact equivalent, and no others', () => {
    expect(react).toEqual({
      '--awe-color-primary': 'var(--primary-color)',
      '--awe-color-on-primary': 'var(--primary-color-text)',
      '--awe-color-text': 'var(--text-color)',
      '--awe-color-background': 'var(--surface-ground)',
      '--awe-color-surface': 'var(--surface-card)',
      '--awe-color-panel': 'var(--surface-card)',
      '--awe-color-border': 'var(--surface-border)',
      '--awe-color-danger': 'var(--danger-color)',
      '--awe-color-warning': 'var(--warning-color)',
      '--awe-color-success': 'var(--success-color)',
      '--awe-color-info': 'var(--info-color)',
      '--awe-radius': 'var(--border-radius)',
      '--awe-font-family-base': 'var(--font-family)'
    });
  });
});
