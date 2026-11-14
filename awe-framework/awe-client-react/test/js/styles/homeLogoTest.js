import fs from 'fs';
import path from 'path';

// The navigation bar logo of the home screens is the logo of the application
// (awe.application.paths.image-navbar-logo), as in the AngularJS engine: index() publishes it as --awe-navbar-logo, and
// a logo other than the AWE mark hides the AWE wordmark (--awe-navbar-wordmark: none) and is fitted in the box
// (--awe-navbar-logo-size: contain) instead of the scaling of the AWE artwork, so the application logo is shown whole.
// The collapsed rail takes awe.application.paths.image-navbar-logo-collapsed (--awe-navbar-logo-rail). Without either of
// them the brand slot keeps the built-in AWE artwork: the mark with its wordmark.

const MODULE_ROOT = path.join(__dirname, '..', '..', '..');
const HOME_CSS = path.join(MODULE_ROOT, 'src', 'assets', 'css', 'pages', 'home.css');
const TEMPLATE = path.join(MODULE_ROOT, 'src', 'template.html');

const read = file => fs.readFileSync(file, 'utf8');

describe('navigation bar logo', () => {
  it('the brand slot takes the logo of the application and the AWE mark without it', () => {
    expect(read(HOME_CSS)).toContain(
      'background: var(--awe-navbar-logo, url("../../images/logos/awe-logo-mark.svg")) no-repeat left center / var(--awe-navbar-logo-size, auto 100%);');
  });

  it('a logo other than the AWE mark hides the AWE wordmark', () => {
    expect(read(HOME_CSS)).toContain('display: var(--awe-navbar-wordmark, block);');
  });

  it('the collapsed rail takes the logo of the rail or the logo of the navigation bar', () => {
    expect(read(HOME_CSS)).toContain(
      'background-image: var(--awe-navbar-logo-rail, var(--awe-navbar-logo, url("../../images/logos/awe-logo-mark.svg")));');
  });

  it('the logo of the collapsed rail is centred in the rail', () => {
    const railRule = read(HOME_CSS).match(/#page-home \.layout-static-inactive h1\.application-logo \{[^}]*\}/);
    expect(railRule).not.toBeNull();
    expect(railRule[0]).toContain('background-position: center;');
  });

  it('the template publishes the logo of the application and the logo of the rail', () => {
    const template = read(TEMPLATE);
    expect(template).toContain('navbarLogo');
    expect(template).toContain('--awe-navbar-logo');
    expect(template).toContain('--awe-navbar-wordmark');
    expect(template).toContain('--awe-navbar-logo-rail');
  });
});
