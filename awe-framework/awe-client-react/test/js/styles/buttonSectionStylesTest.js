const fs = require('fs');
const path = require('path');

// Ghost and icon-only buttons and section titles with an icon and a separator (#793), with the AWE 5 tokens, as the
// AngularJS engine draws them
const BUTTON = path.join(__dirname, '../../../src/components/AweButton.less');
const TAG = path.join(__dirname, '../../../src/components/Tag.less');

const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule written with the selector
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[{};])\\s*${escape(selector)}\\s*\\{([^{}]*)\\}`, 'm'));
  return match ? match[1] : null;
};
const read = file => fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';

describe('awe-react-client/test/js/styles/buttonSectionStylesTest.js', () => {
  const button = read(BUTTON);
  const tag = read(TAG);

  it('draws a ghost button without background nor border, with the muted text, lit by the pointer', () => {
    const ghost = rule(button, '.p-button.btn-ghost');
    expect(ghost).toMatch(/color: var\(--awe-color-text-muted\);/);
    expect(ghost).toMatch(/background: transparent;/);
    expect(ghost).toMatch(/border-color: transparent;/);
    const hover = rule(button, '.p-button.btn-ghost:enabled:hover');
    expect(hover).toMatch(/color: var\(--awe-color-text-hover\);/);
    expect(hover).toMatch(/background: var\(--awe-color-surface-hover\);/);
    expect(rule(button, '.p-button.btn-ghost:enabled:active')).toMatch(/background: var\(--awe-color-secondary-hover\);/);
  });

  it('keeps a visible focus ring on a ghost button', () => {
    expect(rule(button, '.p-button.btn-ghost:focus-visible')).toMatch(/box-shadow:[^;]*var\(--awe-color-focus-ring\);/);
  });

  it('draws an icon-only button as a square of the control size', () => {
    const iconOnly = rule(button, '.p-button.no-label');
    expect(iconOnly).toMatch(/width: var\(--awe-size-control\);/);
    expect(iconOnly).toMatch(/padding: 0;/);
    expect(rule(button, '.p-button.no-label.p-button-sm')).toMatch(/width: var\(--awe-size-control-sm\);/);
    expect(rule(button, '.p-button.no-label.p-button-lg')).toMatch(/width: var\(--awe-size-control-lg\);/);
  });

  it('draws a section title with its icon in one line and a separator above every title but the first', () => {
    const title = rule(tag, '.awe-form-section-title');
    expect(title).toMatch(/display: flex;/);
    expect(title).toMatch(/align-items: center;/);
    expect(title).toMatch(/gap: var\(--awe-space-2\);/);
    expect(title).toMatch(/font-size: var\(--awe-font-size-section-title\);/);
    expect(title).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(title).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    expect(title).not.toMatch(/border-top/);
    expect(rule(tag, '.awe-form-section-title .tag-icon')).toMatch(/color: var\(--awe-color-text-muted\);/);
    const separator = rule(tag, '.awe-form-section-title ~ .awe-form-section-title');
    expect(separator).toMatch(/margin-top: var\(--awe-space-5\);/);
    expect(separator).toMatch(/padding-top: var\(--awe-space-5\);/);
    expect(separator).toMatch(/border-top: 1px solid var\(--awe-color-border-subtle\);/);
  });

  it('keeps the section title of the design system (#910) apart from the form section title', () => {
    const title = rule(tag, '.awe-section-title');
    expect(title).toMatch(/text-transform: uppercase;/);
    expect(title).not.toMatch(/border-top/);
    expect(rule(tag, '.awe-section-title::after')).toMatch(/background: var\(--awe-color-border\);/);
  });

  it.each([['buttons', BUTTON], ['tags', TAG]])('styles the %s with tokens only', (_name, file) => {
    const plain = withoutComments(read(file));
    expect(plain).not.toBe('');
    expect(plain).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(plain).not.toMatch(/\b(rgba?|hsla?)\(/);
    expect(plain.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    (plain.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--awe-/));
  });
});
