package com.almis.awe.testing.driver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The context click of the Playwright adapter against the context menu of the AngularJS client ({@code contextMenu.js}):
 * its {@code contextmenu} handler appends a full-screen mask at once and marks the menu as visible, the mask closes the
 * menu on {@code mouseup} (after a {@code $timeout}), and the menu itself is shown by {@code ng-show}, which with
 * {@code ngAnimate} is applied on the next animation frame. Chromium and Firefox fire {@code contextmenu} when the right
 * button is pressed, so a release that follows at once lands on the mask, that is still on top of a menu that is not
 * shown yet, and closes the menu that was just opened. The fixture has no timer of its own: what it waits for is the
 * frame, as the client does
 */
class PlaywrightBrowserDriverContextMenuTest extends AbstractPlaywrightBrowserTest {

  private static final Locator ROW = Locator.css("#row");
  private static final Locator MENU = Locator.css("#menu");
  private static final Locator MASK = Locator.css("#mask");
  private static final String PAGE = "<style>#mask { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 1050; }"
    + "#menu { position: absolute; z-index: 1051; width: 120px; height: 60px; background: #ccc; display: none; }</style>"
    + "<div id='row' style='position:absolute;left:100px;top:100px;width:200px;height:30px;background:#ddd'>row</div>"
    + "<div id='menu'>Add</div>"
    + "<script>window.log = [];"
    + "const menu = document.getElementById('menu'); let mask = null; let visible = false;"
    + "document.addEventListener('mouseup', e => window.log.push('mouseup:' + (e.target.id || e.target.tagName)), true);"
    // ContextMenu.hide: the menu is not visible any more and the mask is removed
    + "function hide() { if (visible) { visible = false; mask.remove(); menu.style.display = 'none'; } }"
    + "document.getElementById('row').addEventListener('contextmenu', e => {"
    + "  window.log.push('contextmenu'); e.preventDefault();"
    // ContextMenu.show: the menu goes where the pointer is, 16 pixels inside its corner
    + "  menu.style.left = (e.clientX - 16) + 'px'; menu.style.top = (e.clientY - 16) + 'px';"
    // The mask is appended at once and closes the menu after a timeout (the $timeout of the client)
    + "  mask = document.createElement('div'); mask.id = 'mask'; document.body.appendChild(mask);"
    + "  const close = () => { window.log.push('mask closes'); setTimeout(hide, 0); return false; };"
    + "  mask.addEventListener('click', close); mask.addEventListener('contextmenu', close); mask.addEventListener('mouseup', close);"
    + "  visible = true;"
    // ng-show shows the menu on the next animation frame (ngAnimate)
    + "  requestAnimationFrame(() => { if (visible) { menu.style.display = 'block'; } });"
    + "});</script>";

  private String log() {
    return String.valueOf(eval("window.log.join(',')"));
  }

  @Test
  void shouldReleaseTheRightButtonOnTheMenuAndNotOnTheMaskThatClosesIt() {
    show(PAGE);

    browser.contextClick(ROW);

    // The release found the menu under the pointer: the mask did not take it
    assertThat(log()).isEqualTo("contextmenu,mouseup:menu");
    assertThat(browser.isVisible(MENU)).isTrue();
    assertThat(browser.exists(MASK)).isTrue();
  }

  @Test
  void shouldStillCloseTheContextMenuWhenTheMaskIsClickedAfterwards() {
    show(PAGE);

    browser.contextClick(ROW);
    browser.click(MASK);
    // The mask closes the menu after a timeout
    page.waitForFunction("!document.getElementById('mask')");

    assertThat(browser.isVisible(MENU)).isFalse();
    assertThat(browser.exists(MASK)).isFalse();
  }
}
