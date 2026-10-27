const fs = require('fs');
const path = require('path');

// The server error page (#885): a Thymeleaf template shared by both engines, styled with the AWE 5 tokens. The page has
// no attributes on <html>, so it follows the colour mode of the system with the default palettes of the token file,
// which the generic screens publish as css/awe-design-tokens/tokens.css (under /css/**, open without a session)
const REPOSITORY = path.join(__dirname, '../../../../..');
const GENERIC_SCREENS = path.join(REPOSITORY, 'awe-framework', 'awe-generic-screens');
const ERROR_PAGE = path.join(GENERIC_SCREENS, 'src', 'main', 'resources', 'templates', 'error.html');

const read = file => fs.readFileSync(file, 'utf8');
const style = html => html.match(/<style>([^]*?)<\/style>/)[1].replace(/\/\*[^]*?\*\//g, '');
const rule = (css, selector) => {
  const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = css.match(new RegExp(`(?:^|[},])\\s*(?:[^{}]*,\\s*)?${escaped}\\s*(?:,[^{}]*)?\\{([^}]*)\\}`, 'm'));
  return match ? match[1] : null;
};

describe('server error page (#885)', () => {
  const page = read(ERROR_PAGE);
  const css = style(page);

  it('links the AWE token file the generic screens publish, before its own styles', () => {
    expect(page).toContain('<link rel="stylesheet" href="/css/awe-design-tokens/tokens.css">');
    expect(page.indexOf('awe-design-tokens/tokens.css')).toBeLessThan(page.indexOf('<style>'));
  });

  it('publishes the token file of the engines as a static resource of the generic screens', () => {
    const pom = read(path.join(GENERIC_SCREENS, 'pom.xml')).replace(/\s+/g, ' ');
    expect(pom).toContain('<directory>${project.basedir}/../awe-design-tokens</directory>'
      + ' <targetPath>${project.build.directory}/classes/static/css/awe-design-tokens</targetPath>'
      + ' <filtering>false</filtering> <includes> <include>tokens.css</include> </includes>');
  });

  it('follows the colour mode of the system: the dead layout-dark rules are gone', () => {
    expect(rule(css, ':root')).toMatch(/color-scheme: light dark;/);
    expect(css).not.toMatch(/layout-dark|layout-light|\.dark\b/);
  });

  it('styles the page with tokens only: no colour literals or gradients', () => {
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgba?|hsla?|oklch|linear-gradient)\(/);
    expect(css).not.toMatch(/:\s*(white|black)\b/);
    // Sizes come from tokens, except the hairlines and the breakpoint of the media query (where var() does not apply)
    expect(css.replace(/@media \([^)]*\)/g, '').match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
    (css.match(/var\(--[\w-]+/g) || []).forEach(variable => expect(variable).toMatch(/^var\(--awe-/));
    expect(css).not.toMatch(/var\(--awe-palette-/);
  });

  it('draws the page as the prototype: card on the background, danger badge and message, primary and secondary buttons', () => {
    expect(rule(css, 'body')).toMatch(/background: var\(--awe-color-background\);/);
    expect(rule(css, 'body')).toMatch(/font-family: var\(--awe-font-family-base\);/);
    expect(rule(css, '.error-container')).toMatch(/background: var\(--awe-color-surface\);/);
    expect(rule(css, '.error-container')).toMatch(/border-radius: var\(--awe-radius-card\);/);
    expect(rule(css, '.error-icon')).toMatch(/color: var\(--awe-color-danger-text\);/);
    expect(rule(css, '.error-icon')).toMatch(/background: var\(--awe-color-danger-subtle\);/);
    expect(rule(css, '.error-title')).toMatch(/color: var\(--awe-color-text-emphasis\);/);
    expect(rule(css, '.error-message')).toMatch(/border: 1px solid var\(--awe-color-danger-border\);/);
    expect(rule(css, '.btn-primary')).toMatch(/background: var\(--awe-color-primary\);/);
    expect(rule(css, '.btn-primary')).toMatch(/color: var\(--awe-color-on-primary\);/);
    expect(rule(css, '.btn-secondary')).toMatch(/background: var\(--awe-color-secondary\);/);
    expect(rule(css, '.btn:focus-visible')).toMatch(/outline: var\(--awe-focus-ring\);/);
  });

  it('keeps the motion of the card only for users who do not prefer reduced motion', () => {
    expect(rule(css, '.error-container')).toMatch(/animation: slideUp var\(--awe-motion-duration-slow\) var\(--awe-motion-easing-enter\);/);
  });

  it('keeps the markup and the Thymeleaf attributes of the page', () => {
    const thymeleaf = [...page.matchAll(/\s(th:[\w-]+)="([^"]*)"/g)].map(match => `${match[1]}=${match[2]}`);
    expect(thymeleaf).toEqual([
      'th:utext=${title} ?: \'Oops! Something went wrong\'',
      'th:utext=${icon} ?: \'⚠\'',
      'th:utext=${title} ?: \'Oops! Something went wrong\'',
      'th:utext=${messagePrefix} ?: \'Unable to complete the process:\'',
      'th:utext=${message} ?: \'An unexpected error occurred. Please try again later.\'',
      'th:utext=${backButtonText} ?: \'Go Back\'',
      'th:data-home-url=${homeUrl} ?: \'/\'',
      'th:utext=${homeButtonText} ?: \'Home\''
    ]);
    ['error-container', 'error-icon', 'error-title', 'error-message', 'error-actions', 'btn btn-primary', 'btn btn-secondary']
      .forEach(className => expect(page).toContain(`class="${className}"`));
  });
});
