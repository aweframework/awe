const fs = require('fs');
const path = require('path');

// Side panel (#843): the header and the footer stay in view and the body scrolls between them, without showing under
// the footer
const SIDE_PANEL = path.join(__dirname, '../../../src/components/AweSidePanel.less');

const withoutComments = css => css.replace(/\/\*[^]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '');
const escape = selector => selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// The declarations of the first rule written with the selector
const rule = (css, selector) => {
  const match = withoutComments(css).match(new RegExp(`(?:^|[{};])\\s*${escape(selector)}\\s*\\{([^{}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('awe-react-client/test/js/styles/sidePanelStylesTest.js', () => {
  const css = fs.readFileSync(SIDE_PANEL, 'utf8');

  it('sticks the footer to the bottom of the scrolling body', () => {
    const footer = rule(css, '.modal-footer');
    expect(footer).toMatch(/position: sticky;/);
    expect(footer).toMatch(/bottom: 0;/);
    // Above the icons of the fields that scroll under it (the suggest arrow has z-index 1) (#793)
    expect(footer).toMatch(/z-index: 2;/);
  });

  it('leaves no gap under the footer where the body would show through', () => {
    expect(rule(css, '.p-sidebar-content:has(> .modal-footer)')).toMatch(/padding-bottom: 0;/);
    expect(rule(css, '.modal-footer')).toMatch(/padding: var\(--awe-space-md\) 0 var\(--awe-space-lg\);/);
  });

  it('shows the icon of the title before it, in the colour of the title and at the icon size, as the AngularJS engine (#793)', () => {
    expect(rule(css, '.side-panel-title')).toMatch(/display: inline-flex;[^]*align-items: center;[^]*gap: var\(--awe-space-2\);/);
    const icon = rule(css, '.panel-title-icon');
    expect(icon).toMatch(/display: inline-flex;/);
    expect(icon).not.toMatch(/color:/);
    const svg = rule(css, '.panel-title-icon > .awe-icon');
    expect(svg).toMatch(/width: var\(--awe-size-icon\);/);
    expect(svg).toMatch(/height: var\(--awe-size-icon\);/);
  });

  it('draws the identity header as the design: large avatar, title, muted subtitle and tags, as the AngularJS engine (#793 S18)', () => {
    expect(rule(css, '.panel-identity')).toMatch(/display: flex;[^]*align-items: center;[^]*gap: var\(--awe-space-3\);/);
    const avatar = rule(css, '.panel-identity-avatar');
    expect(avatar).toMatch(/width: var\(--awe-space-10\);/);
    expect(avatar).toMatch(/height: var\(--awe-space-10\);/);
    expect(avatar).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    const title = rule(css, '.panel-identity-title');
    expect(title).toMatch(/margin: 0;/);
    expect(title).toMatch(/font-size: var\(--awe-font-size-dialog-title\);/);
    expect(title).toMatch(/font-weight: var\(--awe-font-weight-semibold\);/);
    expect(title).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    const subtitle = rule(css, '.panel-identity-subtitle');
    expect(subtitle).toMatch(/font-size: var\(--awe-font-size-sm\);/);
    expect(subtitle).toMatch(/color: var\(--awe-color-text-muted\);/);
    const tags = rule(css, '.panel-identity-tags');
    expect(tags).toMatch(/display: flex;[^]*flex-wrap: wrap;[^]*gap: var\(--awe-space-1-5\);[^]*margin-top: var\(--awe-space-1\);/);
  });

  it('styles the panel with tokens only', () => {
    const plain = withoutComments(css);
    expect(plain).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(plain).not.toMatch(/\b(rgba?|hsla?)\(/);
    expect(plain.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    (plain.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--awe-/));
  });
});
