const fs = require('fs');
const path = require('path');

// The AWE theme of PrimeReact is generated from the Lara light theme through the equivalence table
// (awe-framework/awe-design-tokens/primereact/primereact-theme.json). The design system rule is that no value of Lara
// reaches the client as it is: every colour and every geometry is an --awe-* token, and the width of a border is
// --awe-border-width or --awe-border-width-strong, so the theme of the application thickens or thins the borders of
// every PrimeReact component (#930).

const PRIMEREACT_THEME = require(path.join(__dirname, '..', '..', '..', '..', 'awe-design-tokens', 'primereact', 'primereact-theme.cjs'));
const PRIMEREACT_DIR = path.dirname(require.resolve('primereact/package.json'));
const TOKENS_CSS = fs.readFileSync(path.join(__dirname, '..', '..', '..', '..', 'awe-design-tokens', 'tokens.css'), 'utf8');

const {theme} = PRIMEREACT_THEME.readPrimeReact(PRIMEREACT_DIR);
const built = PRIMEREACT_THEME.buildThemeFrom(theme, PRIMEREACT_DIR);

const declarations = built.split('\n').filter(text => /^\s+[-\w]+\s*:/.test(text));

const borderWidths = declarations.filter(text => /^\s*border(?:-(?:top|right|bottom|left|width))?\s*:/.test(text));

const declarationsWith = (pattern) => declarations.filter(text => pattern.test(text));

const COLOUR_LITERAL = /#[\da-f]{3,8}\b|rgba?\(|hsla?\(/i;
const HAIRLINE = /(?<![\w.-])(?:1|2)px(?![\w-])/;
const BORDER_WIDTH_PROPERTY = /^\s*border(?:-(?:top|right|bottom|left|width))?\s*:/;
const LENGTH = /(?<![\w.-])\d*\.?\d+px(?![\w-])/g;

describe('awe-framework/awe-client-react/test/js/styles/primeReactTheme', () => {
  it('generates the theme', () => {
    expect(built.length).toBeGreaterThan(10000);
    expect(declarations.length).toBeGreaterThan(1000);
  });

  it('declares no border of the theme with a literal width', () => {
    expect(borderWidths.filter(text => HAIRLINE.test(text))).toEqual([]);
  });

  it('leaves no px length in a border declaration of the theme', () => {
    const lengths = declarations.filter(text => BORDER_WIDTH_PROPERTY.test(text))
      .flatMap(text => [...text.matchAll(LENGTH)].map(match => match[0]));
    expect(lengths).toEqual([]);
  });

  it('rewrites the 1px and 2px borders of a component and leaves any other width alone', () => {
    const translator = new PRIMEREACT_THEME.Translator({structural: ['0', '0px', '1px', '2px'], overrides: [], variables: {},
      values: {}, colors: {}, lengths: {}, radii: {}, fontSizes: {}, fontWeights: {}, durations: {}, rootVariables: {}});
    expect(translator.translate('.p-x', 'border', '1px solid #ffffff'))
      .toBe('var(--awe-border-width) solid #ffffff');
    expect(translator.translate('.p-x', 'border-bottom', '2px solid #ffffff'))
      .toBe('var(--awe-border-width-strong) solid #ffffff');
    expect(translator.translate('.p-x', 'border-left', '7px solid transparent')).toBe('7px solid transparent');
    expect(translator.translate('.p-x', 'padding', '1px')).toBe('1px');
  });

  it('replaces every colour literal with a token', () => {
    expect(declarationsWith(COLOUR_LITERAL)).toEqual([]);
  });

  it('reads only tokens the token file declares', () => {
    const declared = new Set([...TOKENS_CSS.matchAll(/^\s*(--awe-[\w-]+)\s*:/gm)].map(match => match[1]));
    const missing = [...new Set([...built.matchAll(/var\((--awe-[\w-]+)/g)].map(match => match[1]))]
      .filter(name => !declared.has(name));
    expect(missing).toEqual([]);
  });

  it('tokenises the width of a border of the mapping', () => {
    expect(built).toMatch(/border: var\(--awe-border-width\) solid var\(--awe-color-[\w-]+\)/);
  });
});
