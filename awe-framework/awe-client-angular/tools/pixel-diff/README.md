# Pixel diff of `styles.css`

Compares the AngularJS client of a running AWE application rendered with two builds of `css/styles.css`, with a threshold of 0
(issue #755: no visual change when a vendored Bootstrap family is removed). Not part of the Maven or the CI build.

1. Build `styles.css` for each tree: `npm run build:production` in this module writes `target/classes/static/css/styles.css`.
   Copy it as `before.css` (tree before the change), swap the sources, build again and copy it as `after.css`.
2. Start the test application once (`awe-tests/awe-boot`, for example `java -Dserver.port=<free port> -jar awe-boot.jar`). Both
   captures use the same instance, so the data and the JavaScript are identical; the stylesheet is swapped through a Playwright
   route.
3. Run:

```bash
PLAYWRIGHT_MODULE=<folder of the playwright node package> \
  node tools/pixel-diff/pixel-diff.js --url http://localhost:<port> --before before.css --after after.css --out pixel-diff-out
```

`PLAYWRIGHT_MODULE` is optional when `playwright` is installed. The node package of the version AWE tests with is inside
`com.microsoft.playwright:driver` (`driver/package`), and its browsers are the ones `awe-testing` installs
(`npx playwright install chromium firefox` otherwise).

**Two applications.** A stylesheet swap cannot show a change of the JavaScript or of the templates (slice 4 of #755 replaced the
Bootstrap plugins). For those, start the build before the change and the build after it, each on its own free port, and give both
addresses instead of the stylesheets: `--url-before http://localhost:<port> --url-after http://localhost:<other port>`. Every screen is
captured against each application and compared the same way; nothing is swapped, so `results.json` carries `twoApps: true` and the
route hits are not checked. Both applications must start from the same data (a fresh database each). The tool refuses the same address on both sides (after
normalizing a trailing slash and the case), the same stylesheet on both sides, and any mix of the two modes (exit code 2).

By default it captures every screen of the `SCREENS` list of `pixel-diff.js` (`--screens a,b` picks some; an unknown name is an
error) in Chromium and Firefox, at 1440x900 and 375x812, in the light and the dark theme. The screens behind the menus are
marked `desktopOnly`: on a phone the menus are behind the hamburger. Animations and the caret are frozen.

**Modules.** A screen with `module: "Test"` needs that module selected (the settings button and the module selector of the top bar,
as `awe-testing` does). The tool selects it once, after the screens of the default module, at desktop size (the selector is in the
collapsed top bar on a phone) and then restores the viewport. The `test-*` screens are the Test module screens of `awe-boot` that use
the grid, the panels, `pull-right`, `no-padding`, `wizard-panel` and the accordion (`panel-group-*`). The screens with random data
(`layout2-test`, `matrix-test`, the chart tests) are not in the list: they differ between two captures with the same stylesheet.

**Slice 5 screens.** `s5-mobile-menu-open` opens the main menu of a phone with its toggle (the screen is marked `phone`: it is captured at 375x812 whatever the viewport of the capture, which is restored after the shot, and waits for `body.mmc`), `s5-grid-cell-tooltip` shows the tooltip of a clamped
grid cell as `services/grid/commons.js` writes it, and `s5-bootstrap-fixture` writes over the page the Bootstrap components that no screen of the
application shows (every tab and pill variant with an open dropdown, the default and inverse navbar, the placements of the tooltip and the popover, a modal
dialog and the alert modals, the collapse and fade states): the compat layer is compared with the vendored rules where the screens do not reach.
The same fixture was compared by computed style (every property of every element) while the slice was built, which names the property that changed.

**Retries.** A screen that differs is captured again (`--retries`, an integer, 4 by default) with each stylesheet and compared in
every pairing of before and after captures, because the pager of the grids has a layout race that flips between two states even
with the same stylesheet. The pairing that differs least is reported, and the before, after and diff images written are the ones of
that pairing. Limit: the minimum over pairings can hide a regression that shows in only some captures (a flaky one); a real
difference that shows in every capture is never hidden. Retries apply to any screen, not only to known racy ones.

**Results and exit code.** `results.json` has one entry per screen, browser, viewport and theme: pixels that differ,
`sizeMismatch`, the number of times the stylesheet route was hit in the before and the after captures, and `error` when the screen
could not be captured (also for an unknown `--screens` name). Exit code 0 only when something was captured and every capture is
identical; 1 when a capture differs, has an error, or had no stylesheet route hit (the swap did not happen, so both sides would be
the same CSS), or when nothing was captured; 2 for wrong options. The images of the captures that differ are written to `--out`.
The summary logic is unit tested (`src/test/jest/tools`).
