#!/usr/bin/env node
/* global require, console, process, OffscreenCanvas, ImageData, FileReader, createImageBitmap, fetch, window, document, Event */
/**
 * Pixel diff of the AWE AngularJS client between two builds of css/styles.css (issue #755).
 *
 * The same running application is captured twice with a threshold of 0: once with the `--before` stylesheet and once with the
 * `--after` one (served through a Playwright route, so the data and the JavaScript are the same). Screens x browsers x
 * viewports x themes are compared pixel by pixel and every difference is reported with its three images.
 *
 *   node tools/pixel-diff/pixel-diff.js --url http://localhost:24137 --before before.css --after after.css [options]
 *
 * Two applications (a change of the JavaScript or the templates, which a stylesheet swap cannot show): start the build before the
 * change and the build after it, and give both addresses instead of the stylesheets. The screens are captured against each one and
 * compared the same way (nothing is swapped, so there is no route to check).
 *
 *   node tools/pixel-diff/pixel-diff.js --url-before http://localhost:24137 --url-after http://localhost:24138 [options]
 *
 * Options: --out <dir> (default pixel-diff-out), --browsers chromium,firefox, --screens login,home,... (default: all),
 *          --viewports 1440x900,375x812, --themes light,dark, --retries 4 (captures again a screen that differs), --user test,
 *          --password test.
 * Exit code: 0 when something was captured and every capture is identical; 1 when one differs, one could not be captured (an
 * error entry in results.json), the stylesheet was not served in a capture (route hits 0) or nothing was captured; 2 on bad options.
 * Playwright: the `playwright` package, or the folder in PLAYWRIGHT_MODULE (for example the node package of the driver that
 * the awe-testing Maven dependency ships, `driver/package` inside com.microsoft.playwright:driver).
 * Browsers are not installed by this script (`npx playwright install chromium firefox`).
 */
const fs = require("fs");
const {Buffer} = require("buffer");
const path = require("path");
const {bestPairing, orderScreens, parseRetries, resolveMode, summarize, unknownScreens} = require("./summary");

const argv = process.argv.slice(2);
const option = (name, fallback) => {
  const index = argv.indexOf(`--${name}`);
  return index >= 0 ? argv[index + 1] : fallback;
};
const list = (name, fallback) => option(name, fallback).split(",").filter(Boolean);

const BEFORE = option("before");
const AFTER = option("after");
const OUT = path.resolve(option("out", "pixel-diff-out"));
const USER = option("user", "test");
const PASSWORD = option("password", "test");
const USAGE = "Usage: pixel-diff.js (--url <app url> --before <styles.css> --after <styles.css> | --url-before <app url> --url-after <app url>) "
  + "[--out dir] [--browsers a,b] [--screens a,b] [--viewports WxH,...] [--themes light,dark] [--retries n] [--user name] [--password value]";
// One side is a stylesheet served to the same application, or an application of its own; the combinations that would compare a side
// with itself are refused
let MODE;
try {
  MODE = resolveMode({url: option("url"), before: BEFORE, after: AFTER, urlBefore: option("url-before"), urlAfter: option("url-after")});
} catch (error) {
  console.error(`${error.message}\n${USAGE}`);
  process.exit(2);
}
const TWO_APPS = MODE.twoApps;
let RETRIES;
try {
  RETRIES = parseRetries(option("retries", "4"));
} catch (error) {
  console.error(`${error.message}\n${USAGE}`);
  process.exit(2);
}

/** Screens: a route of the application (after the login) or a function that drives the page */
const HOME = "/screen/private/home";
const menu = (top, item) => async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.getByText(top, {exact: true}).first().click();
  await page.waitForTimeout(500);
  if (item) {
    await page.getByText(item, {exact: true}).first().click();
  }
};
const tab = name => async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.locator("#main-navbar .dropdown-toggle", {hasText: "Manager"}).first().click();
  await page.locator("#main-navbar a, #main-navbar button", {hasText: /^\s*Settings\s*$/}).last().click();
  await settle(page);
  await page.getByText(name, {exact: true}).first().click();
};
// A button of the toolbar of a screen that opens a modal dialog
const dialogOf = (route, button) => async (page, base) => {
  await page.goto(`${base}${HOME}/${route}`);
  await settle(page);
  await page.locator(`button:visible`, {hasText: button}).first().click();
  await page.locator(".modal-dialog:visible").first().waitFor({state: "visible", timeout: 10000});
  await page.waitForTimeout(500);
};
// A dropdown of the top bar (the avatar menu, an info dropdown) opened with a click on its toggle
const topBarDropdown = id => async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.locator(`#${id} > a.dropdown-toggle`).click();
  await page.locator(`#${id}.open`).waitFor({timeout: 10000});
  await page.waitForTimeout(500);
};
// An action of the client (the ones a server answer carries) launched through the action controller of the page
const clientAction = (route, action, ready) => async (page, base) => {
  await page.goto(`${base}${HOME}/${route}`);
  await settle(page);
  // Not returned: the promise of a confirm or a message only settles when the user answers, and evaluate would wait for it
  await page.evaluate(data => {
    window.angular.element(document.body).injector().get("ActionController").addActionList([data], false, {address: {view: "base"}});
  }, action);
  await page.locator(ready).first().waitFor({state: "visible", timeout: 10000});
  await page.waitForTimeout(800);
};
// The overflow menu of a tab strip: the tabs of the user settings and eight more, so the last ones go to the menu of the tabs
const tabOverflow = async (page, base) => {
  await tab("PROFILE")(page, base);
  await page.evaluate(() => {
    const nav = [...document.querySelectorAll(".nav-tabs")].find(item => item.getBoundingClientRect().width > 100 && item.querySelector("li"));
    for (let index = 1; index <= 8; index += 1) {
      const clone = nav.querySelector("li:not(.tabdrop)").cloneNode(true);
      clone.querySelector("a").textContent = `Extra tab ${index}`;
      nav.appendChild(clone);
    }
    window.dispatchEvent(new Event("resize"));
  });
  await page.waitForTimeout(1000);
  await page.locator("li.tabdrop > a.dropdown-toggle").click();
  await page.locator("li.tabdrop.open").waitFor({timeout: 10000});
  await page.waitForTimeout(500);
};
// A screen of the Test module with a side panel (a button opens it, the panel is a modal with the in class once it is shown)
const sidePanelOf = (open) => async (page, base) => {
  await page.goto(`${base}${HOME}/matrix-test-side-panel`);
  await settle(page);
  await open(page);
  await page.locator(".modal.side-panel.in").waitFor({timeout: 10000});
  await page.waitForTimeout(800);
};
// Slice 5 of #755 (the CSS of the dropdowns, navs, navbar, modals, tooltip, popovers and animations): the main menu of a phone, opened with its toggle
const mobileMenuOpen = async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.locator("#main-menu-toggle").click();
  // The toggle puts the menu in its other state on a phone: it adds mmc to the body
  await page.locator("body.mmc").waitFor({timeout: 10000});
  await page.waitForTimeout(300);
};
const PHONE = {width: 375, height: 812};
// The Bootstrap components that no screen of the application shows (every tab and pill variant, the default and inverse navbar, the eight tooltip
// and four popover placements, a modal dialog and the alert modals, the collapse and fade states), written the way the plugins write them over
// the page: the pixels compare the compat rules with the vendored ones where the screens do not reach
const FIXTURE = (() => {
  const nav = (kind, extra = "") => `<ul class="nav ${kind} ${extra}"><li class="active"><a href="#">Active</a></li><li><a href="#">Link</a></li><li class="disabled"><a href="#">Disabled</a></li>`
    + "<li class=\"dropdown open\"><a href=\"#\" class=\"dropdown-toggle\">Menu <span class=\"caret\"></span></a><ul class=\"dropdown-menu\"><li class=\"dropdown-header\">Header</li>"
    + "<li class=\"active\"><a href=\"#\">Active</a></li><li class=\"disabled\"><a href=\"#\">Disabled</a></li><li class=\"divider\"></li><li><a href=\"#\">Link</a></li></ul></li></ul>";
  const navbar = kind => `<div class="navbar ${kind}" style="margin:10px"><div class="navbar-header"><a class="navbar-brand" href="#">Brand</a></div><ul class="nav navbar-nav"><li class="active"><a href="#">Home</a></li>`
    + "<li><a href=\"#\">Link</a></li><li class=\"disabled\"><a href=\"#\">Off</a></li></ul><form class=\"navbar-form navbar-left\"><div class=\"form-group\"><input class=\"form-control\" placeholder=\"Search\"></div>"
    + "<button class=\"btn btn-default\">Go</button></form><p class=\"navbar-text navbar-right\">Signed in</p></div>";
  const placements = ["top", "right", "bottom", "left", "top-left", "top-right", "bottom-left", "bottom-right"];
  const tooltips = placements.map(place => `<div class="tooltip ${place} in" style="position:relative;display:inline-block;margin:24px"><div class="tooltip-arrow"></div><div class="tooltip-inner">${place}</div></div>`).join("");
  const popovers = ["top", "right", "bottom", "left"].map(place => `<div class="popover ${place}" style="position:relative;display:inline-block;margin:24px"><div class="arrow"></div><h3 class="popover-title">Title</h3><div class="popover-content">${place}</div></div>`).join("");
  const dialog = kind => `<div class="${kind}" style="position:relative"><div class="modal-dialog" style="margin:10px"><div class="modal-content"><div class="modal-header"><button class="close">&times;</button><h4 class="modal-title">Title</h4></div>`
    + "<div class=\"modal-body\">Body</div><div class=\"modal-footer\"><button class=\"btn btn-default\">Cancel</button><button class=\"btn btn-primary\">Accept</button></div></div></div></div>";
  const states = "<div class=\"collapse in\">Collapse in</div><div class=\"fade in\">Fade in</div><table><tbody class=\"collapse in\"><tr class=\"collapse in\"><td>Row</td></tr></tbody></table>";
  return [nav("nav-tabs"), nav("nav-pills"), nav("nav-pills nav-stacked"), nav("nav-tabs nav-justified"), navbar("navbar-default"), navbar("navbar-inverse"), tooltips, popovers, dialog(""), dialog("modal-alert modal-danger"),
    dialog("modal-alert modal-info"), states].join("");
})();
const bootstrapFixture = async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.evaluate(html => {
    const holder = document.createElement("div");
    holder.id = "s5-fixture";
    holder.style.cssText = "position:absolute;top:0;left:0;right:0;z-index:99999;background:#fff;padding:10px";
    holder.innerHTML = html;
    document.body.appendChild(holder);
  }, FIXTURE);
  await page.waitForTimeout(500);
};
// The tooltip of a clamped cell of a grid, as services/grid/commons.js writes it
const gridCellTooltip = async (page, base) => {
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.evaluate(() => {
    const tooltip = document.createElement("div");
    tooltip.className = "tooltip bottom in grid-cell-tooltip";
    tooltip.setAttribute("role", "tooltip");
    tooltip.innerHTML = "<div class=\"tooltip-arrow\"></div><div class=\"tooltip-inner\">The full text of a clamped cell</div>";
    tooltip.style.cssText = "top:200px;left:300px";
    document.body.appendChild(tooltip);
  });
  await page.waitForTimeout(300);
};
const SCREENS = [
  {name: "login", public: true, path: "/"},
  {name: "home", path: `${HOME}/information`},
  {name: "users", path: `${HOME}/users`},
  {name: "profiles", path: `${HOME}/profiles`},
  {name: "themes", path: `${HOME}/themes`},
  {name: "screen-configuration", path: `${HOME}/screen-configuration`},
  {name: "application-parameters", path: `${HOME}/application-parameters`},
  {name: "user-settings-profile", desktopOnly: true, run: tab("PROFILE")},
  {name: "user-settings-security", desktopOnly: true, run: tab("SECURITY")},
  {name: "user-settings-notifications", desktopOnly: true, run: tab("NOTIFICATIONS")},
  {name: "menu-open", desktopOnly: true, run: menu("Scheduler")},
  {name: "scheduler-management", desktopOnly: true, run: menu("Scheduler", "Scheduler management")},
  {name: "scheduler-tasks", desktopOnly: true, run: menu("Scheduler", "Task manager")},
  {name: "modal-help", run: dialogOf("users", "Help")},
  {name: "modal-print", run: dialogOf("users", "Print")},
  // Slice 4 of #755 (Bootstrap JavaScript replaced by native code): the screens a plugin drew. Compare two applications (--url-before,
  // --url-after), because the stylesheet is the same
  {name: "s4-avatar-menu", desktopOnly: true, run: topBarDropdown("ButUsrAct")},
  {name: "s4-info-dropdown", desktopOnly: true, run: topBarDropdown("notification-bell")},
  {name: "s4-tabdrop-menu", desktopOnly: true, run: tabOverflow},
  {name: "s4-confirm", run: clientAction("information", {type: "confirm", parameters: {title: "Confirm title", message: "Are you sure you want to do this?"}}, ".modal.in")},
  {name: "s4-side-panel", module: "Test", run: sidePanelOf(page => page.locator("#ButPnlRight").click())},
  {name: "s4-grid-columns-panel", module: "Test", run: sidePanelOf(page => page.locator("[data-testid='grid-columns-button']").first().click())},
  {name: "s4-accordion-open", module: "Test", run: async (page, base) => {
    await page.goto(`${base}${HOME}/accordion-test`);
    await settle(page);
    await page.locator(".accordion-toggle").nth(1).click();
    // The item is open (class in) once its height transition ends, whatever the time it takes
    await page.locator(".panel-collapse.in").first().waitFor({timeout: 10000});
    await page.waitForTimeout(300);
  }},
  {name: "s4-target-message", module: "Test", run: clientAction("screen-test",
    {type: "target-message", target: "CrtNum", parameters: {type: "error", title: "Popover title", message: "Popover message text"}}, ".popover.in")},
  // Slice 5 of #755: the mobile menu, the components no screen shows, and the grid cell tooltip
  {name: "s5-mobile-menu-open", phone: true, run: mobileMenuOpen},
  {name: "s5-bootstrap-fixture", run: bootstrapFixture},
  {name: "s5-grid-cell-tooltip", run: gridCellTooltip},
  // Screens of the Test module (awe-boot), picked for the Bootstrap families they use: col-*, panel-body/footer, pull-right, no-padding,
  // wizard-panel, panel-group (accordion). Left out on purpose, because their data is random and differs between two captures with
  // the same stylesheet: layout2-test and matrix-test (random grid values), chart-test, chart-series-test, grid-and-chart (random series)
  {name: "test-criteria-test", module: "Test", path: `${HOME}/criteria-test`},
  {name: "test-criteria-test-left", module: "Test", path: `${HOME}/criteria-test-left`},
  {name: "test-criteria-date-range", module: "Test", path: `${HOME}/criteria-date-range`},
  {name: "test-criteria-reset", module: "Test", path: `${HOME}/criteria-reset`},
  {name: "test-criteria-uploader", module: "Test", path: `${HOME}/criteria-uploader`},
  {name: "test-button-test", module: "Test", path: `${HOME}/button-test`},
  {name: "test-matrix-test-layout", module: "Test", path: `${HOME}/matrix-test-layout`},
  {name: "test-matrix-test-pager", module: "Test", path: `${HOME}/matrix-test-pager`},
  {name: "test-matrix-test-empty", module: "Test", path: `${HOME}/matrix-test-empty`},
  {name: "test-matrix-test-dialog-size", module: "Test", path: `${HOME}/matrix-test-dialog-size`},
  {name: "test-matrix-test-deferred", module: "Test", path: `${HOME}/matrix-test-deferred`},
  {name: "test-layout-test", module: "Test", path: `${HOME}/layout-test`},
  {name: "test-wizard-test", module: "Test", path: `${HOME}/wizard-test`},
  {name: "test-accordion-test", module: "Test", path: `${HOME}/accordion-test`},
  {name: "test-reveal-invalid-test", module: "Test", path: `${HOME}/reveal-invalid-test`},
  {name: "test-screen-test", module: "Test", path: `${HOME}/screen-test`},
  {name: "test-pivot-test", module: "Test", path: `${HOME}/pivot-test`},
  // Forms with validation states, disabled and read-only controls, input groups and button groups (slice 3d: buttons, button groups, forms
  // and input groups left the vendored LESS)
  {name: "new-user", path: `${HOME}/new-user`},
  {name: "new-site", path: `${HOME}/new-site`},
  {name: "new-database", path: `${HOME}/new-database`},
  {name: "new-email-server", path: `${HOME}/new-email-server`},
  {name: "customize-theme", path: `${HOME}/customize-theme`},
  {name: "change-password", path: `${HOME}/change-password`},
  {name: "test-matrix-test-radio", module: "Test", path: `${HOME}/matrix-test-radio`},
  {name: "test-matrix-test-row-detail", module: "Test", path: `${HOME}/matrix-test-row-detail`},
  {name: "test-initial-values", module: "Test", path: `${HOME}/test-initial-values`}
];

// The Test module is not the default one: it is selected like awe-testing does (settings button, module selector). The selector is in
// the top bar, which is collapsed on a phone, so the step runs at desktop size and the viewport is restored after it
const selectModule = async (page, viewport, module, base) => {
  await page.setViewportSize({width: 1440, height: 900});
  await page.goto(`${base}${HOME}/information`);
  await settle(page);
  await page.locator("#ButSetTog").click();
  await page.locator("#s2id_module .select2-choice").click();
  await page.locator(".select2-results li", {hasText: module}).first().click();
  await settle(page);
  await page.setViewportSize(viewport);
};

const settle = async page => {
  await page.waitForLoadState("networkidle").catch(() => {});
  await page.waitForTimeout(1500);
};

const loadPlaywright = () => {
  const folder = process.env.PLAYWRIGHT_MODULE;
  return require(folder ? path.resolve(folder) : "playwright");
};

/** Count the pixels that differ (any channel, threshold 0) with the canvas of a Chromium page */
const comparer = async browserType => {
  const browser = await browserType.launch();
  const page = await browser.newPage();
  const compare = (a, b) => page.evaluate(async ([first, second]) => {
    const decode = async data => {
      const image = await createImageBitmap(await (await fetch(`data:image/png;base64,${data}`)).blob());
      const canvas = new OffscreenCanvas(image.width, image.height);
      const context = canvas.getContext("2d", {willReadFrequently: true});
      context.drawImage(image, 0, 0);
      return {width: image.width, height: image.height, pixels: context.getImageData(0, 0, image.width, image.height).data};
    };
    const [x, y] = [await decode(first), await decode(second)];
    if (x.width !== y.width || x.height !== y.height) {
      return {size: [x.width, x.height, y.width, y.height], differing: null, sizeMismatch: true, diff: null};
    }
    let differing = 0;
    const out = new Uint8ClampedArray(x.pixels.length);
    for (let i = 0; i < x.pixels.length; i += 4) {
      const same = x.pixels[i] === y.pixels[i] && x.pixels[i + 1] === y.pixels[i + 1] && x.pixels[i + 2] === y.pixels[i + 2] && x.pixels[i + 3] === y.pixels[i + 3];
      if (!same) {
        differing += 1;
        out[i] = 255;
        out[i + 3] = 255;
      } else {
        out[i] = out[i + 1] = out[i + 2] = x.pixels[i] >> 2;
        out[i + 3] = 255;
      }
    }
    const canvas = new OffscreenCanvas(x.width, x.height);
    canvas.getContext("2d").putImageData(new ImageData(out, x.width, x.height), 0, 0);
    const blob = await canvas.convertToBlob({type: "image/png"});
    const diff = await new Promise(resolve => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result.split(",")[1]);
      reader.readAsDataURL(blob);
    });
    return {size: [x.width, x.height], differing, sizeMismatch: false, diff: differing ? diff : null};
  }, [a.toString("base64"), b.toString("base64")]);
  return {compare, close: () => browser.close()};
};

const FREEZE = `*, *::before, *::after { animation: none !important; transition: none !important; caret-color: transparent !important; scroll-behavior: auto !important; }`;

/**
 * Capture screens with one stylesheet: one context per browser, viewport and theme, one login
 * @return {Promise<{images: object, errors: object, hits: number}>} Screenshots by screen, error messages by screen, and how many times
 *   the stylesheet route was hit (0 means the swap did not happen)
 */
const capture = async (browserType, side, viewport, theme, screens) => {
  const {css, url} = side;
  const images = {};
  const errors = {};
  let hits = 0;
  const browser = await browserType.launch();
  try {
    const context = await browser.newContext({viewport, colorScheme: theme, reducedMotion: "reduce", deviceScaleFactor: 1});
    if (css) {
      await context.route(/\/css\/styles\.css/, route => {
        hits += 1;
        return route.fulfill({status: 200, contentType: "text/css", body: css});
      });
    }
    const page = await context.newPage();
    const shoot = async name => {
      await page.addStyleTag({content: FREEZE});
      await page.waitForTimeout(300);
      images[name] = await page.screenshot({fullPage: false});
    };
    const attempt = async (screen, action) => {
      try {
        // The pointer stays where the previous screen left it: a hover help popover of the previous screen can cover the next click
        await page.mouse.move(1, 1);
        // A screen marked `phone` is captured at a phone size whatever the viewport of the capture, which is restored after the shot
        if (screen.phone) {
          await page.setViewportSize(PHONE);
        }
        await action();
        await shoot(screen.name);
      } catch (error) {
        errors[screen.name] = error.message.split("\n")[0];
      } finally {
        if (screen.phone) {
          await page.setViewportSize(viewport);
        }
      }
    };
    for (const screen of screens.filter(entry => entry.public)) {
      await attempt(screen, async () => {
        await page.goto(`${url}${screen.path}`);
        await page.waitForSelector("#cod_usr");
        await settle(page);
      });
    }
    await page.goto(`${url}/`);
    await page.waitForSelector("#cod_usr");
    await page.fill("#cod_usr", USER);
    await page.fill("#pwd_usr", PASSWORD);
    await page.keyboard.press("Enter");
    await page.waitForSelector("#main-navbar", {timeout: 30000});
    await settle(page);
    const ordered = orderScreens(screens).filter(entry => !entry.public);
    let selected = null;
    for (const screen of ordered) {
      await attempt(screen, async () => {
        if (screen.module && screen.module !== selected) {
          await selectModule(page, viewport, screen.module, url);
          selected = screen.module;
        }
        if (screen.run) {
          await screen.run(page, url);
        } else {
          await page.goto(`${url}${screen.path}`);
        }
        await settle(page);
      });
    }
  } catch (error) {
    // The login or the browser failed: every screen without a picture is an error
    screens.filter(screen => !images[screen.name]).forEach(screen => {
      errors[screen.name] = errors[screen.name] || error.message.split("\n")[0];
    });
  } finally {
    await browser.close();
  }
  return {images, errors, hits};
};

/**
 * Compare the captures of one screen. A screen that differs is captured again, with each stylesheet, and compared in every
 * pairing, so a pair that rendered in the same state is found (the grid pager has a layout race). The pairing that differs least is
 * reported with its own images. Limit: a regression that shows in only some captures can hide behind a clean pairing.
 */
const compareScreen = async (comparing, playwright, setup, screen, first, second) => {
  const seen = {before: [first.images[screen.name]], after: [second.images[screen.name]]};
  const pairings = [];
  const compareAll = async () => {
    pairings.length = 0;
    for (const [i, one] of seen.before.entries()) {
      for (const [j, other] of seen.after.entries()) {
        const result = await comparing.compare(one, other);
        pairings.push({before: i, after: j, differing: result.sizeMismatch ? null : result.differing, sizeMismatch: result.sizeMismatch, size: result.size, diff: result.diff});
      }
    }
    return bestPairing(pairings);
  };
  let best = await compareAll();
  const extra = {before: 0, after: 0};
  for (let attempt = 0; (best.sizeMismatch || best.differing !== 0) && attempt < RETRIES; attempt += 1) {
    const [again, later] = [await capture(playwright, setup.before, setup.viewport, setup.theme, [screen]),
      await capture(playwright, setup.after, setup.viewport, setup.theme, [screen])];
    extra.before += again.hits;
    extra.after += later.hits;
    if (again.images[screen.name]) {
      seen.before.push(again.images[screen.name]);
    }
    if (later.images[screen.name]) {
      seen.after.push(later.images[screen.name]);
    }
    best = await compareAll();
  }
  return {best, images: {before: seen.before[best.before], after: seen.after[best.after]}, extra};
};

const main = async () => {
  const playwright = loadPlaywright();
  const before = {css: TWO_APPS ? null : fs.readFileSync(BEFORE), url: MODE.urlBefore};
  const after = {css: TWO_APPS ? null : fs.readFileSync(AFTER), url: MODE.urlAfter};
  const known = SCREENS.map(screen => screen.name);
  const names = list("screens", known.join(","));
  const results = [];
  unknownScreens(names, known).forEach(name => {
    results.push({browser: null, viewport: null, theme: null, screen: name, differing: null, error: `unknown screen (known: ${known.join(", ")})`});
  });
  const all = SCREENS.filter(screen => names.includes(screen.name));
  const viewports = list("viewports", "1440x900,375x812").map(value => ({width: Number(value.split("x")[0]), height: Number(value.split("x")[1])}));
  const comparing = await comparer(playwright.chromium);
  fs.mkdirSync(OUT, {recursive: true});
  for (const browserName of list("browsers", "chromium,firefox")) {
    for (const viewport of viewports) {
      for (const theme of list("themes", "light,dark")) {
        // The user menu and the top menu are behind the hamburger on a phone: those screens are desktop only
        const screens = all.filter(screen => !(screen.desktopOnly && viewport.width < 768));
        console.log(`${browserName} ${viewport.width}x${viewport.height} ${theme}`);
        const first = await capture(playwright[browserName], before, viewport, theme, screens);
        const second = await capture(playwright[browserName], after, viewport, theme, screens);
        for (const screen of screens) {
          const entry = {browser: browserName, viewport: `${viewport.width}x${viewport.height}`, theme, screen: screen.name,
            routeHits: {before: first.hits, after: second.hits}, ...(TWO_APPS ? {twoApps: true} : {})};
          const failure = first.errors[screen.name] || second.errors[screen.name];
          if (failure || !first.images[screen.name] || !second.images[screen.name]) {
            results.push({...entry, differing: null, error: failure || "not captured"});
            console.log(`  ${screen.name}: ERROR ${failure || "not captured"}`);
            continue;
          }
          const {best, images, extra} = await compareScreen(comparing, playwright[browserName], {before, after, viewport, theme}, screen, first, second);
          const file = `${browserName}-${viewport.width}x${viewport.height}-${theme}-${screen.name}`;
          if (best.sizeMismatch || best.differing !== 0) {
            fs.writeFileSync(path.join(OUT, `${file}-before.png`), images.before);
            fs.writeFileSync(path.join(OUT, `${file}-after.png`), images.after);
            if (best.diff) {
              fs.writeFileSync(path.join(OUT, `${file}-diff.png`), Buffer.from(best.diff, "base64"));
            }
          }
          results.push({...entry, differing: best.differing, sizeMismatch: Boolean(best.sizeMismatch), size: best.size,
            routeHits: {before: first.hits + extra.before, after: second.hits + extra.after}});
          console.log(`  ${screen.name}: ${best.sizeMismatch ? "size mismatch" : `${best.differing} differing pixels`}`);
        }
      }
    }
  }
  await comparing.close();
  fs.writeFileSync(path.join(OUT, "results.json"), JSON.stringify(results, null, 2));
  const summary = summarize(results);
  console.log(`\n${summary.captures} captures, ${summary.identical} identical, ${summary.different} different, ${summary.errors} errors`);
  process.exit(summary.exitCode);
};

main().catch(error => {
  console.error(error);
  process.exit(2);
});
