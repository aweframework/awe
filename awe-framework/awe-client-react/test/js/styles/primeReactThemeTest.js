const fs = require('fs');
const path = require('path');

const REACT = path.join(__dirname, '../../..');
const SRC = path.join(REACT, 'src');
const PRIMEREACT = path.join(REACT, 'node_modules', 'primereact');
const {
  LARA_THEME, TABLE_FILE, TOKENS_FILE, buildTheme, buildThemeFrom, readPrimeReact, declaredTokens, PrimeReactThemePlugin
} = require('../../../../awe-design-tokens/primereact/primereact-theme.cjs');

const read = file => fs.readFileSync(file, 'utf8');
const sourceFiles = (dir, pattern) => fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
  const file = path.join(dir, entry.name);
  return entry.isDirectory() ? sourceFiles(file, pattern) : pattern.test(entry.name) ? [file] : [];
});

const COLOR = /#[\da-f]{3,8}\b|rgba?\(|hsla?\(/i;
const LENGTH = /-?\d*\.?\d+(?:px|rem|em|%)/g;
// Properties whose values must come from tokens: spacing, sizes, radii, type, shadows and focus rings
const GEOMETRY = /^(padding(-(top|right|bottom|left))?|(row-|column-)?gap|(min-)?height|border(-(top|bottom)-(left|right))?-radius|font-size|box-shadow|outline)$/;

// The rules of the theme (outside the licence header) as [selector, property, value]
const declarations = css => {
  const result = [];
  const blocks = [];
  css.slice(css.indexOf('*/') + 2).split('\n').forEach(line => {
    const trimmed = line.trim();
    if (trimmed.endsWith('{')) {
      blocks.push(trimmed.slice(0, -1).trim());
    } else if (trimmed === '}') {
      blocks.pop();
    } else {
      const match = /^([-\w]+)\s*:\s*(.+);$/.exec(trimmed);
      if (match) {
        result.push([blocks[blocks.length - 1], match[1], match[2]]);
      }
    }
  });
  return result;
};

// Rules that mention a class of a component, as in "from 'primereact/button'" -> .p-button, .p-button-label...
const ruleCount = (css, className) => css.split('\n')
  .filter(line => line.trim().endsWith('{') || line.trim().endsWith(','))
  .filter(line => new RegExp(`\\.p-${className}(?![a-z])`).test(line)).length;

describe('AWE theme of PrimeReact (#885)', () => {
  const primereact = readPrimeReact(PRIMEREACT);
  const table = JSON.parse(read(TABLE_FILE));
  const theme = buildThemeFrom(primereact.theme, PRIMEREACT);
  const rules = declarations(theme);

  it('is built from the Lara theme of the pinned primereact 10 (MIT)', () => {
    expect(primereact.version).toBe(table.source.version);
    expect(primereact.version).toMatch(/^10\./);
    expect(LARA_THEME).toBe(path.normalize(table.source.theme));
  });

  it('keeps the licences of primereact and of the Aura preset whose values it follows', () => {
    const header = theme.slice(0, theme.indexOf('*/'));
    expect(theme.startsWith('/*!')).toBe(true);
    expect(header).toContain(`primereact ${primereact.version}`);
    expect(header).toContain('Copyright (c) 2016-2025 PrimeTek');
    expect(header).toContain('@primeuix/themes 2.0.3 (Aura preset)');
    expect(header).toContain('Copyright (c) 2025 PrimeTek');
    expect(header.match(/MIT License/g)).toHaveLength(2);
  });

  it('leaves no colour literal: every colour is a token', () => {
    expect(rules.filter(([, , value]) => COLOR.test(value))).toEqual([]);
  });

  it('leaves no literal size, spacing, radius, font size, shadow or focus ring, only the structural values', () => {
    const literals = rules.filter(([, property]) => GEOMETRY.test(property))
      .flatMap(([selector, property, value]) => (value.replace(/var\([^)]*\)/g, '').match(LENGTH) || [])
        .filter(length => !table.structural.includes(length))
        .map(length => `${selector} { ${property}: ${value} } -> ${length}`));
    expect(literals).toEqual([]);
    expect(rules.filter(([, property, value]) => property === 'font-weight' && !/^var\(--awe-font-weight-/.test(value))).toEqual([]);
  });

  it('reads only tokens of the AWE token file, never the primitive palettes', () => {
    const tokens = declaredTokens(read(TOKENS_FILE));
    const used = [...new Set([...theme.matchAll(/var\((--awe-[\w-]+)/g)].map(match => match[1]))];
    expect(used.filter(name => !tokens.has(name))).toEqual([]);
    expect(used.filter(name => name.startsWith('--awe-palette-'))).toEqual([]);
  });

  it('drops the palette and the font faces of Lara and only feeds the variables PrimeReact reads in its own styles', () => {
    expect(theme).not.toMatch(/@font-face|--(blue|indigo|gray|primary)-\d+\s*:|--primary-color\s*:|--surface-[a-z0-9]+\s*:/);
    const root = rules.filter(([selector]) => selector === ':root').map(([, property]) => property);
    expect(root).toEqual(['--border-radius', '--inline-spacing', '--content-padding']);
  });

  it('keeps the rules of every PrimeReact component the client imports', () => {
    // The class of the component when it is not its package name, and the packages without theme rules
    const classes = {calendar: 'datepicker', column: 'datatable', columngroup: 'datatable', row: 'datatable',
      editor: 'editor-container', inputtextarea: 'inputtext'};
    const unthemed = ['api', 'hooks', 'utils', 'ripple', 'progressspinner'];
    const imported = [...new Set(sourceFiles(SRC, /\.(js|jsx)$/).flatMap(file =>
      [...read(file).matchAll(/(?:from\s+|import\s+)["']primereact\/([a-z]+)["']/g)].map(match => match[1])))].sort();
    expect(imported).toEqual(expect.arrayContaining(['button', 'datatable', 'dialog', 'dropdown', 'inputtext']));

    const missing = imported.filter(name => !unthemed.includes(name)).map(name => classes[name] || name)
      .filter(className => ruleCount(theme, className) === 0 || ruleCount(theme, className) !== ruleCount(primereact.theme, className));
    expect(missing).toEqual([]);
    expect(unthemed.map(name => ruleCount(primereact.theme, name))).toEqual([0, 0, 0, 0, 0]);
  });

  it('maps the values of Lara through the table and fails on a value the table does not map', () => {
    const options = {table, tokensCss: read(TOKENS_FILE), version: primereact.version, license: primereact.license, auraLicense: 'MIT'};
    expect(buildTheme(primereact.theme, options)).toContain('  .p-button {\n    color: var(--awe-color-on-primary);');

    const withRule = rule => `${primereact.theme}\n.p-button {\n  ${rule};\n}\n`;
    expect(() => buildTheme(withRule('color: #123456'), options)).toThrow(/does not map 1 values[\s\S]*\.p-button \{ color: #123456 \} -> #123456/);
    expect(() => buildTheme(withRule('margin: 0.3rem'), options)).toThrow(/\.p-button \{ margin: 0\.3rem \} -> 0\.3rem/);
    expect(() => buildTheme(withRule('box-shadow: 0 0 0 3px transparent'), options)).toThrow(/box-shadow: 0 0 0 3px transparent/);
  });

  it('draws the icon and unit addons of the criteria as a part of the field, at the control size of the density', () => {
    const addon = rules.filter(([selector]) => selector === '.p-inputgroup-addon')
      .reduce((result, [, property, value]) => ({...result, [property]: value}), {});
    expect(addon).toEqual(expect.objectContaining({
      background: 'var(--awe-color-field)',
      color: 'var(--awe-color-text-muted)',
      padding: 'var(--awe-field-padding-y) var(--awe-field-padding-x)',
      'min-width': 'var(--awe-size-control)'
    }));
  });

  it('draws the rows and the borders of the grids with the grid tokens', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    ['datatable', 'treetable'].forEach(table => {
      const body = `.p-${table} .p-${table}-tbody > tr`;
      expect(value(body, 'background')).toBe('var(--awe-grid-row-background)');
      expect(value(`${body} > td`, 'border')).toBe('1px solid var(--awe-grid-border)');
      expect(value(`.p-${table} .p-${table}-thead > tr > th`, 'border')).toBe('1px solid var(--awe-grid-border)');
      expect(value(`${body}.p-highlight`, 'background')).toBe('var(--awe-grid-row-selected-background)');
      expect(value(`${body}.p-highlight`, 'color')).toBe('var(--awe-grid-row-selected-text)');
    });
    const hover = rules.filter(([selector, property]) => /^\.p-(datatable|treetable)\.p-(datatable|treetable)-(selectable|hoverable-rows) .*:hover$/.test(selector)
      && property === 'background').map(([, , declared]) => declared);
    expect(hover.length).toBeGreaterThan(0);
    hover.forEach(declared => expect(declared).toBe('var(--awe-grid-row-hover-background)'));
    // The grids of AWE are small tables: their cells take the padding of the density too
    ['datatable', 'treetable'].forEach(table => ['thead > tr > th', 'tbody > tr > td', 'tfoot > tr > td'].forEach(cell =>
      expect(value(`.p-${table}.p-${table}-sm .p-${table}-${cell}`, 'padding'))
        .toBe('var(--awe-grid-cell-padding-y) var(--awe-grid-cell-padding-x)')));
  });

  it('shows the sorted column with the header background, the emphasis text and the arrow in the primary', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    ['datatable', 'treetable'].forEach(table => {
      const sorted = `.p-${table} .p-sortable-column.p-highlight`;
      expect(value(sorted, 'background')).toBe('var(--awe-grid-header-background)');
      expect(value(sorted, 'color')).toBe('var(--awe-color-text-emphasis)');
      expect(value(`${sorted} .p-sortable-column-icon`, 'color')).toBe('var(--awe-color-primary-text)');
    });
    expect(value('.p-treetable .p-sortable-column:not(.p-highlight):hover', 'color')).toBe('var(--awe-color-text-hover)');
    expect(value('.p-datatable .p-sortable-column:not(.p-highlight):not(.p-sortable-disabled):hover', 'color'))
      .toBe('var(--awe-color-text-hover)');
    expect(value('.p-datatable .p-sortable-column:not(.p-highlight):not(.p-sortable-disabled):hover', 'background'))
      .toBe('var(--awe-grid-header-background)');
  });

  it('sizes the paginator buttons and the row buttons of the grids with the small control of the density', () => {
    // A rule of several selectors is listed under its last one
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    expect(value('.p-paginator .p-paginator-pages .p-paginator-page', 'height')).toBe('var(--awe-size-control-sm)');
    expect(value('.p-paginator .p-paginator-pages .p-paginator-page', 'border-radius')).toBe('var(--awe-radius-md)');
    expect(value('.p-paginator .p-paginator-last', 'min-width')).toBe('var(--awe-size-control-sm)');
    expect(value('.p-paginator .p-paginator-first', 'border-top-left-radius')).toBe('var(--awe-radius-md)');
    expect(value('.p-paginator .p-paginator-last', 'border-bottom-right-radius')).toBe('var(--awe-radius-md)');
    expect(value('.p-paginator .p-dropdown', 'height')).toBe('var(--awe-size-control-sm)');
    expect(value('.p-datatable .p-datatable-tbody > tr > td .p-row-editor-cancel', 'width')).toBe('var(--awe-size-control-sm)');
    expect(value('.p-datatable .p-datatable-tbody > tr > td .p-row-editor-cancel', 'border-radius')).toBe('var(--awe-radius-md)');
    expect(value('.p-treetable .p-treetable-tbody > tr > td .p-treetable-toggler', 'height')).toBe('var(--awe-space-6)');
  });

  it('draws the outlined buttons as the neutral outlined buttons of the prototype, not in the primary colour', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    expect(value('.p-button.p-button-outlined', 'background-color')).toBe('var(--awe-color-surface)');
    expect(value('.p-button.p-button-outlined', 'color')).toBe('var(--awe-color-text)');
    expect(value('.p-button.p-button-outlined', 'border')).toBe('1px solid var(--awe-color-field-border)');
    expect(value('.p-button.p-button-outlined:not(:disabled):hover', 'background')).toBe('var(--awe-color-surface-subtle)');
    expect(value('.p-button.p-button-outlined:not(:disabled):hover', 'color')).toBe('var(--awe-color-text)');
    expect(value('.p-button.p-button-outlined:not(:disabled):hover', 'border')).toBe('1px solid var(--awe-color-field-border-hover)');
    expect(value('.p-button.p-button-outlined:not(:disabled):active', 'background')).toBe('var(--awe-color-surface-hover)');
    expect(value('.p-button.p-button-outlined:not(:disabled):active', 'color')).toBe('var(--awe-color-text)');
    // The outlined buttons of a severity (danger outlined...) keep their colour
    const danger = rules.filter(([selector, name]) => selector.startsWith('.p-button.p-button-danger.p-button-outlined,')
      && name === 'color').map(([, , declared]) => declared);
    expect(danger).toEqual(['var(--awe-color-danger-text)']);
  });

  it('shows the focus ring of the buttons for the keyboard only, not on a click, as the prototype', () => {
    const buttonSelectors = [...new Set(rules.map(([selector]) => selector))].filter(selector => /\.p-button/.test(selector));
    // Every :focus of a button rule is a :focus-visible (a :focus-within or a :has(...:focus-visible) stays)
    expect(buttonSelectors.filter(selector => /:focus(?![-\w])/.test(selector))).toEqual([]);
    const ring = rules.filter(([selector, name]) => selector === '.p-button:focus-visible' && name === 'box-shadow');
    expect(ring.length).toBeGreaterThan(0);
    expect(rules.some(([selector]) => /^\.p-button\.p-button-danger:not\(:disabled\):focus-visible/.test(selector))).toBe(true);
    // A field keeps its ring on a click: it is where the user types
    expect(rules.some(([selector]) => /^\.p-inputtext:enabled:focus(?![-\w])/.test(selector))).toBe(true);
  });

  it('turns the :focus of a button into :focus-visible in every selector of a list, and only for buttons', () => {
    const selectorLines = theme.slice(theme.indexOf('*/') + 2).split('\n').map(line => line.trim())
      .filter(line => line.endsWith(',') || line.endsWith('{'));
    expect(selectorLines.filter(line => /\.p-button/.test(line) && /:focus(?![-\w])/.test(line))).toEqual([]);
    // Every selector of a list is rewritten, not only the first one
    expect(selectorLines).toContain('.p-button.p-button-danger:not(:disabled):focus-visible, .p-button-group.p-button-danger > '
      + '.p-button:not(:disabled):focus-visible, .p-splitbutton.p-button-danger > .p-button:not(:disabled):focus-visible, '
      + '.p-fileupload-choose.p-button-danger:not(:disabled):focus-visible {');
    expect(selectorLines.some(line => /^\.p-inputtext:enabled:focus(?![-\w])/.test(line))).toBe(true);
  });

  it('adds no ripple to the clicks, as the prototype', () => {
    const main = read(path.join(SRC, 'main.jsx'));
    expect(main).toMatch(/const value = \{ripple: false\};/);
    expect(main).not.toMatch(/ripple: true/);
  });

  it('draws the dialogs, the popovers, the menus and the tooltips as the overlays of the prototype', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    const overlayBorder = '1px solid var(--awe-color-overlay-border)';
    ['.p-dialog', '.p-overlaypanel', '.p-confirm-popup', '.p-contextmenu']
      .forEach(selector => expect([selector, value(selector, 'border')]).toEqual([selector, overlayBorder]));
    // The arrow of a flipped popover keeps the border colour, as the arrow of a popover below its anchor does
    expect(value('.p-overlaypanel.p-overlaypanel-flipped:before', 'border-top-color')).toBe('var(--awe-color-overlay-border)');
    expect(value('.p-confirm-popup.p-confirm-popup-flipped:before', 'border-top-color')).toBe('var(--awe-color-overlay-border)');

    // The last content of a dialog without footer keeps the radius of the dialog
    expect(value('.p-dialog .p-dialog-content:last-of-type', 'border-bottom-left-radius')).toBe('var(--awe-radius-modal)');
    expect(value('.p-dialog .p-dialog-content:last-of-type', 'border-bottom-right-radius')).toBe('var(--awe-radius-modal)');
    // The footer is a band under the content, with the actions at the end separated by the gap of the footer
    expect(value('.p-dialog .p-dialog-footer', 'padding')).toBe('var(--awe-space-3) var(--awe-modal-padding)');
    expect(value('.p-dialog .p-dialog-footer', 'background')).toBe('var(--awe-color-surface-subtle)');
    expect(value('.p-dialog .p-dialog-footer', 'border-top')).toBe('1px solid var(--awe-color-border)');
    expect(value('.p-dialog .p-dialog-footer button', 'margin')).toBe('0');

    expect(value('.p-tooltip .p-tooltip-text', 'padding')).toBe('var(--awe-space-1) var(--awe-space-2)');
    expect(value('.p-tooltip .p-tooltip-text', 'border-radius')).toBe('var(--awe-radius-sm)');
  });

  it('draws the toasts and the messages as the prototype: tinted surface, coloured border and readable text', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    const severities = {info: 'info', success: 'success', warn: 'warning', error: 'danger'};
    Object.entries(severities).forEach(([severity, color]) => {
      const toast = `.p-toast .p-toast-message.p-toast-message-${severity}`;
      expect(value(toast, 'background')).toBe(`color-mix(in srgb, var(--awe-color-${color}-subtle), var(--awe-color-overlay) 40%)`);
      expect(value(toast, 'color')).toBe('var(--awe-color-text)');
      expect(value(toast, 'border-width')).toBe('1px');
      expect(value(`${toast} .p-toast-icon-close`, 'color')).toBe(`var(--awe-color-${color}-text)`);

      const message = `.p-message.p-message-${severity}`;
      expect(value(message, 'background')).toBe(`color-mix(in srgb, var(--awe-color-${color}-subtle), var(--awe-color-surface) 45%)`);
      expect(value(message, 'color')).toBe('var(--awe-color-text)');
      expect(value(`${message} .p-message-icon`, 'color')).toBe(`var(--awe-color-${color}-text)`);

      const inline = `.p-inline-message.p-inline-message-${severity}`;
      expect(value(inline, 'background')).toBe(`color-mix(in srgb, var(--awe-color-${color}-subtle), var(--awe-color-surface) 45%)`);
      expect(value(inline, 'color')).toBe('var(--awe-color-text)');
      expect(value(inline, 'border-width')).toBe('1px');
    });
    expect(value('.p-toast .p-toast-message', 'border-radius')).toBe('var(--awe-radius-lg)');
    expect(value('.p-toast .p-toast-message', 'margin')).toBe('0 0 var(--awe-space-2) 0');
    expect(value('.p-toast .p-toast-message .p-toast-message-content .p-toast-message-text', 'margin')).toBe('0 0 0 var(--awe-space-3)');
    expect(value('.p-toast .p-toast-message .p-toast-message-content .p-toast-summary', 'font-weight')).toBe('var(--awe-font-weight-semibold)');
    expect(value('.p-toast .p-toast-message .p-toast-message-content .p-toast-detail', 'margin')).toBe('var(--awe-space-0-5) 0 0 0');
    expect(value('.p-message', 'border-radius')).toBe('var(--awe-radius-content)');
    expect(value('.p-inline-message', 'border-radius')).toBe('var(--awe-radius-content)');
    expect(value('.p-message .p-message-text', 'font-size')).toBe('var(--awe-font-size-sm)');
    expect(value('.p-inline-message .p-inline-message-text', 'font-size')).toBe('var(--awe-font-size-sm)');
  });

  it('draws the progress bar on the secondary track with round ends', () => {
    const value = (selector, property) => rules.filter(([candidate, name]) => candidate === selector && name === property)
      .map(([, , declared]) => declared).pop();
    expect(value('.p-progressbar', 'background')).toBe('var(--awe-color-secondary)');
    expect(value('.p-progressbar', 'border-radius')).toBe('var(--awe-radius-full)');
    expect(value('.p-progressbar .p-progressbar-value', 'background')).toBe('var(--awe-color-primary)');
  });

  it('fails when the table uses a token the token file does not declare', () => {
    const options = {table: {...table, colors: {...table.colors, '#4f46e5': '--awe-color-unknown'}}, tokensCss: read(TOKENS_FILE),
      version: primereact.version, license: primereact.license, auraLicense: 'MIT'};
    expect(() => buildTheme(primereact.theme, options)).toThrow('tokens the AWE token file does not declare: --awe-color-unknown');
  });

  it('is bundled where the client imports the Lara theme, instead of saga-blue and the compiled themes', () => {
    const main = read(path.join(SRC, 'main.jsx'));
    expect(main).toContain("import 'primereact/resources/themes/lara-light-indigo/theme.css';");
    expect(main).not.toMatch(/saga-blue|resources\/themes\/(?!lara-light-indigo\/)/);
    expect(read(path.join(SRC, 'components', 'AweApp.jsx'))).not.toMatch(/css\/themes|useHeadStylesheet/);
    const webpack = read(path.join(REACT, 'webpack.common.js'));
    expect(webpack).toMatch(/new PrimeReactThemePlugin\(\{\s*primereactDir: path\.resolve\(__dirname, "node_modules", "primereact"\)/);
    // Bundled by the client, so the application gets the AWE theme in css/main.css and not the Lara one
    expect(webpack).toMatch(/nodeExternals\(\{[\s\S]*allowlist: \['primereact\/resources\/themes\/lara-light-indigo\/theme\.css'\]/);

    // ...and kept in css/main.css by the chunks of the development and production builds
    const lara = path.join(PRIMEREACT, LARA_THEME);
    const chunkTests = ['webpack.dev.js', 'webpack.prod.js'].flatMap(file =>
      Object.values(require(path.join(REACT, file)).optimization.splitChunks.cacheGroups).map(group => group.test));
    expect(chunkTests.map(test => [test.test(lara), test.test(path.join(PRIMEREACT, 'button', 'button.esm.js'))]))
      .toEqual([[false, true], [false, true]]);

    const compiler = {options: {module: {rules: []}}};
    new PrimeReactThemePlugin({primereactDir: PRIMEREACT}).apply(compiler);
    expect(compiler.options.module.rules).toEqual([expect.objectContaining({
      resource: path.join(PRIMEREACT, LARA_THEME), enforce: 'pre'
    })]);
  });

  it('lets the React engine set the colour mode the tokens resolve with', () => {
    const css = read(path.join(SRC, 'main.css')).replace(/\s+/g, ' ');
    expect(css).toContain(':root { color-scheme: light dark; }');
    expect(css).toContain(':root.light { color-scheme: light; }');
    expect(css).toContain(':root.dark { color-scheme: dark; }');
    // The shared token file leaves the mode to each engine (the AngularJS one sets it when it adopts the tokens)
    expect(read(TOKENS_FILE)).not.toMatch(/color-scheme\s*:/);
  });
});
