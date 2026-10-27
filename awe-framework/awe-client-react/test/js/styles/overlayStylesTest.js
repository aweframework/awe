const fs = require('fs');
const path = require('path');

// The AWE 5 overlays and feedback of the React engine (#885): dialogs, confirm, toasts, target messages, popovers,
// tooltips and loaders styled with the --awe-* tokens over the PrimeReact markup (the screen XML and the test hooks do
// not change)
const REPOSITORY = path.join(__dirname, '../../../../..');
const SRC = path.join(REPOSITORY, 'awe-framework', 'awe-client-react', 'src');
const TOKENS = path.join(REPOSITORY, 'awe-framework', 'awe-design-tokens', 'tokens.css');
const PROTOTYPE_TOKENS = path.join(REPOSITORY, 'website', 'static', 'design-system', 'prototype', 'tokens.css');
const REFERENCE = [
  path.join(REPOSITORY, 'website', 'docs', 'design-system', 'tokens-reference.md'),
  path.join(REPOSITORY, 'website', 'i18n', 'es', 'docusaurus-plugin-content-docs', 'current', 'design-system',
    'tokens-reference.md')
];
const OVERLAY_STYLES = ['assets/css/overlays.css', 'containers/MessageContainer.less',
  'components/AweTargetMessage.less', 'components/AweLoader.less'];

const read = file => fs.readFileSync(path.isAbsolute(file) ? file : path.join(SRC, file), 'utf8');
const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule whose selector list holds the selector
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[},])\\s*(?:[^{}]*,\\s*)?${escape(selector)}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('AWE 5 overlay and feedback styles (#885)', () => {
  it.each(OVERLAY_STYLES)('styles %s with tokens only: no colour literals, sizes, layers or mode selectors', file => {
    const css = withoutComments(read(file));
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Only hairlines are written in px: every other size comes from a token
    expect(css.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    expect(css).not.toMatch(/\d(\.\d+)?(rem|em)\b/);
    expect(css).not.toMatch(/z-index:\s*\d/);
    expect(css).not.toMatch(/\.dark\b|\.light\b|data-awe-density|prefers-color-scheme|layout-(dark|light)/);
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--awe-/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
  });

  it('is loaded by the client after the PrimeReact theme', () => {
    const main = read('main.jsx');
    expect(main).toContain("import './assets/css/overlays.css';");
    expect(main.indexOf("import './assets/css/overlays.css';"))
      .toBeGreaterThan(main.indexOf("import 'primereact/resources/themes/lara-light-indigo/theme.css';"));
  });

  it('draws the title of a dialog as the prototype and blurs what the mask covers', () => {
    const overlays = read('assets/css/overlays.css');
    expect(rule(overlays, '.p-dialog .p-dialog-header .p-dialog-title')).toMatch(/letter-spacing: var\(--awe-letter-spacing-tight\);/);
    expect(rule(overlays, '.p-dialog .p-dialog-header .p-dialog-title')).toMatch(/line-height: var\(--awe-line-height-snug\);/);
    expect(rule(overlays, '.p-dialog-mask.p-component-overlay')).toMatch(/backdrop-filter: var\(--awe-mask-backdrop-filter\);/);
  });

  it('writes the tooltips in the small medium type of the prototype', () => {
    const tooltip = rule(read('assets/css/overlays.css'), '.p-tooltip .p-tooltip-text');
    expect(tooltip).toMatch(/font-size: var\(--awe-font-size-xs\);/);
    expect(tooltip).toMatch(/font-weight: var\(--awe-font-weight-medium\);/);
    expect(tooltip).toMatch(/line-height: var\(--awe-line-height-snug\);/);
  });

  it('shows the toasts over the page with the toast layer, blurring what they cover, the title in the colour of the type', () => {
    const toasts = read('containers/MessageContainer.less');
    expect(rule(toasts, '.p-toast')).toMatch(/z-index: var\(--awe-z-toast\);/);
    expect(rule(toasts, '.p-toast .p-toast-message')).toMatch(/backdrop-filter: var\(--awe-toast-backdrop-filter\);/);
    Object.entries({info: 'info', success: 'success', warn: 'warning', error: 'danger'}).forEach(([severity, color]) =>
      expect(rule(toasts, `.p-toast .p-toast-message-${severity} .p-toast-summary`))
        .toMatch(new RegExp(`color: var\\(--awe-color-${color}-text\\);`)));
    expect(rule(toasts, '.p-toast .p-toast-message .p-toast-detail')).toMatch(/font-size: var\(--awe-font-size-sm\);/);
  });

  it('draws the warning of the confirm in a round badge of the warning colour, next to the message', () => {
    const confirm = read('containers/MessageContainer.less');
    expect(rule(confirm, '.awe-confirm-dialog .p-dialog-content')).toMatch(/display: flex;/);
    expect(rule(confirm, '.awe-confirm-dialog .p-dialog-content')).toMatch(/gap: var\(--awe-space-4\);/);
    const icon = rule(confirm, '.awe-confirm-dialog .confirm-icon');
    expect(icon).toMatch(/width: var\(--awe-space-10\);/);
    expect(icon).toMatch(/height: var\(--awe-space-10\);/);
    expect(icon).toMatch(/font-size: var\(--awe-size-icon-lg\);/);
    expect(icon).toMatch(/color: var\(--awe-color-warning-text\);/);
    expect(icon).toMatch(/background: var\(--awe-color-warning-subtle\);/);
    expect(icon).toMatch(/border-radius: var\(--awe-radius-full\);/);
  });

  it('marks a target message with the colour of its type on its edge and its title', () => {
    const message = read('components/AweTargetMessage.less');
    expect(rule(message, '.target-message')).toMatch(/border-left: var\(--awe-space-1\) solid var\(--awe-color-info\);/);
    Object.entries({success: 'success', warning: 'warning', danger: 'danger'}).forEach(([type, color]) =>
      expect(rule(message, `.target-message-${type} .target-message-title`))
        .toMatch(new RegExp(`color: var\\(--awe-color-${color}-text\\);`)));
    expect(rule(message, '.target-message-title')).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
  });

  it('draws the popover as the other overlays', () => {
    // Its spacing follows the rem offsets of the panel body it holds, so only its colours and surface are checked
    const popover = read('components/AwePopover.less');
    expect(withoutComments(popover)).not.toMatch(/#[0-9a-fA-F]{3,8}\b|\b(rgba?|hsla?)\(/);
    expect(popover).toMatch(/border: 1px solid var\(--awe-color-overlay-border\);/);
    expect(popover).toMatch(/border-radius: var\(--awe-radius-overlay\);/);
    expect(popover).toMatch(/box-shadow: var\(--awe-shadow-overlay\);/);
  });

  it('shows the tooltip of a cut grid cell in the tooltip layer', () => {
    const tooltip = rule(read('components/AweGrid.less'), '.grid-cell-tooltip.p-tooltip');
    expect(tooltip).toMatch(/z-index: var\(--awe-z-tooltip\);/);
  });

  it('draws the loaders in the primary colour and stops them when the user prefers reduced motion', () => {
    const loader = read('components/AweLoader.less');
    expect(rule(loader, '.p-progress-spinner-circle')).toMatch(/stroke: var\(--awe-color-primary\);/);
    // Only the dash animation: the colour cycle of PrimeReact is dropped
    expect(rule(loader, '.p-progress-spinner-circle')).toMatch(/animation-name: p-progress-spinner-dash;/);
    expect(withoutComments(loader)).toMatch(/background-color: var\(--awe-color-primary\);/);
    expect(withoutComments(loader)).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{[^}]*animation: none;/);
  });

  it('declares the new overlay tokens in the token file of the engines and of the prototype, and documents them', () => {
    const declared = (file, name) => (read(file).match(new RegExp(`^\\s*${name}\\s*:\\s*([^;]+);`, 'm')) || [])[1];
    ['--awe-toast-backdrop-filter', '--awe-mask-backdrop-filter'].forEach(name => {
      expect(declared(TOKENS, name)).toBeDefined();
      expect(declared(PROTOTYPE_TOKENS, name)).toBe(declared(TOKENS, name));
      REFERENCE.forEach(file => expect(read(file)).toContain(`| \`${name}\` | \`${declared(TOKENS, name)}\` |`));
    });
  });
});
