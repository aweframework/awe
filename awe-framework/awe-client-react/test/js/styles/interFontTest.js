const fs = require('fs');
const path = require('path');

// Inter (#885): the font of the AWE 5 type scale, shared by both engines from the design tokens folder
const CLIENTS = path.join(__dirname, '../../../..');
const FONTS = path.join(CLIENTS, 'awe-design-tokens', 'fonts');
const REACT = path.join(CLIENTS, 'awe-client-react');
const PROTOTYPE_FONTS = path.join(CLIENTS, '..', 'website', 'static', 'design-system', 'prototype', 'assets', 'fonts');

const read = file => fs.readFileSync(file, 'utf8');
const tokens = read(path.join(CLIENTS, 'awe-design-tokens', 'tokens.css'));

describe('Inter font (#885)', () => {
  const fontFace = read(path.join(FONTS, 'inter.css'));

  it('declares the Inter Variable face that the base font family token names first', () => {
    expect(tokens).toMatch(/--awe-font-family-base: 'Inter Variable',/);
    expect(fontFace).toMatch(/@font-face\s*\{[^}]*font-family: 'Inter Variable';/);
    expect(fontFace).toMatch(/font-weight: 100 900;/);
    expect(fontFace).toMatch(/font-display: swap;/);
    expect(fontFace).toMatch(/src: url\('\.\/inter-latin-wght-normal\.woff2'\) format\('woff2-variations'\);/);
  });

  it('ships the same font file as the prototype, with its SIL Open Font License', () => {
    expect(fs.readFileSync(path.join(FONTS, 'inter-latin-wght-normal.woff2')))
      .toEqual(fs.readFileSync(path.join(PROTOTYPE_FONTS, 'inter-latin-wght-normal.woff2')));
    expect(read(path.join(FONTS, 'LICENSE-inter-OFL.txt'))).toContain('SIL OPEN FONT LICENSE Version 1.1');
    // The notice survives the minification of the bundled stylesheet
    expect(fontFace).toMatch(/\/\*![^]*Inter[^]*SIL Open Font License/);
  });

  it('loads the font in the React engine and copies its licence next to the bundle', () => {
    expect(read(path.join(REACT, 'src', 'main.css'))).toMatch(/@import "\.\.\/\.\.\/awe-design-tokens\/fonts\/inter\.css";/);
    expect(read(path.join(REACT, 'webpack.common.js'))).toMatch(/awe-design-tokens[/\\'", ]+fonts[/\\'", ]+LICENSE-inter-OFL\.txt/);
  });

  it('sets the base size and line height of the type scale on the body', () => {
    const body = read(path.join(REACT, 'src', 'main.css')).match(/\nbody \{[^}]*\}/)[0];
    expect(body).toContain('font-family: var(--awe-font-family-base);');
    expect(body).toContain('font-size: var(--awe-font-size-base);');
    expect(body).toContain('line-height: var(--awe-line-height-base);');
  });
});
