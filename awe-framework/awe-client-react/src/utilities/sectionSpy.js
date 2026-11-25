/**
 * Section spy of the editor layout (#993, the awe-editor-* styles of a tag): while the body of an editor
 * (.awe-editor-body) scrolls, the button of its section navigation (.awe-editor-nav) that belongs to the section at the
 * top of the body is marked as the current one (aria-current="location" and the class awe-editor-nav-current). The
 * buttons of the navigation and the sections (.awe-editor-sections, the elements with an id) go in the same order: the
 * first button is the first section. Pure DOM, shared by the React and the AngularJS engines; only the editors (the
 * screens that use the awe-editor styles) are touched.
 */

const EDITOR = ".awe-editor";
const BODY = ".awe-editor-body";
const NAV_BUTTONS = ".awe-editor-nav button";
const SECTIONS = ".awe-editor-sections > [id]";
const CURRENT = "awe-editor-nav-current";
// A section is the current one once its top reaches this distance from the top of the body (its title is in sight)
const TOP_OFFSET = 48;
// The scroll launched by a click in the navigation keeps the clicked button marked: this long after the click, and as
// long as its scroll events keep coming
const CLICK_LOCK = 1000;
const SCROLL_GAP = 200;

/**
 * Index of the section at the top of the body of an editor: the last one whose top has reached a line near the top of
 * the body, the first one at the start. The last sections may be too short to reach the top: in the last screen of the
 * scroll the line slides down to the bottom of the body, so each of them is the current one in turn and the last one
 * at the end
 * @param {Element} body Body of the editor (it scrolls)
 * @param {Element[]} sections Sections, in order
 * @returns {number} Index of the current section, -1 without sections
 */
export function getCurrentSection(body, sections) {
  if (!sections.length) return -1;
  const height = body.clientHeight;
  const remaining = body.scrollHeight - height - body.scrollTop;
  const slide = height > 0 && body.scrollTop > 0 && remaining < height ? (height - Math.max(remaining, 0)) / height : 0;
  const top = body.getBoundingClientRect().top + TOP_OFFSET + slide * Math.max(height - 2 * TOP_OFFSET, 0);
  let current = 0;
  sections.forEach((section, index) => {
    if (section.getBoundingClientRect().top <= top) current = index;
  });
  return current;
}

/**
 * Mark the button of the current section of an editor and unmark the others
 * @param {Element} editor Editor (.awe-editor)
 * @returns {number} Index of the current section, -1 when the editor has no sections or no navigation
 */
export function markCurrentSection(editor) {
  const body = editor?.querySelector(BODY);
  const buttons = Array.from(editor?.querySelectorAll(NAV_BUTTONS) ?? []);
  if (!body || !buttons.length) return -1;
  const current = getCurrentSection(body, Array.from(body.querySelectorAll(SECTIONS)));
  buttons.forEach((button, index) => {
    const isCurrent = index === current;
    button.classList.toggle(CURRENT, isCurrent);
    if (isCurrent) {
      button.setAttribute("aria-current", "location");
    } else {
      button.removeAttribute("aria-current");
    }
  });
  return current;
}

/**
 * Mark a button of the navigation of an editor as the current one: the section it scrolls to, while that scroll lasts
 * @param {Element} button Button of the navigation
 */
function markClicked(button) {
  const editor = button.closest(EDITOR);
  const buttons = Array.from(editor?.querySelectorAll(NAV_BUTTONS) ?? []);
  buttons.forEach(item => {
    item.classList.toggle(CURRENT, item === button);
    if (item === button) {
      item.setAttribute("aria-current", "location");
    } else {
      item.removeAttribute("aria-current");
    }
  });
  // The scroll the click launches does not move the mark: a short last section would take it from a section that
  // cannot reach the top of the body
  editor.__aweSpyLock = Date.now() + CLICK_LOCK;
}

/**
 * Follow the scroll of every editor of a document: a scroll of a body marks its current section (once per frame), a
 * click on a button of its navigation marks that button while the scroll it launches lasts, and an editor that appears
 * is marked at once. Installed once per document
 * @param {Document} root Document
 * @returns {function} Function that stops following them
 */
export function installSectionSpy(root = document) {
  if (!root || root.__aweSectionSpy) return () => undefined;
  // The next frame, read on every scroll (the window may change it after the install)
  const frame = callback => typeof window !== "undefined" && window.requestAnimationFrame
    ? window.requestAnimationFrame(callback) : setTimeout(callback, 0);
  let pending = false;
  const onScroll = event => {
    const target = event.target;
    if (!(target instanceof Element) || !target.matches(BODY) || pending) return;
    const editor = target.closest(EDITOR);
    if (editor?.__aweSpyLock && Date.now() < editor.__aweSpyLock) {
      editor.__aweSpyLock = Math.max(editor.__aweSpyLock, Date.now() + SCROLL_GAP);
      return;
    }
    pending = true;
    frame(() => {
      pending = false;
      markCurrentSection(editor);
    });
  };
  const markNew = () => root.querySelectorAll(EDITOR).forEach(editor => {
    if (!editor.querySelector(`.${CURRENT}`)) markCurrentSection(editor);
  });
  const onClick = event => {
    const button = event.target instanceof Element ? event.target.closest(NAV_BUTTONS) : null;
    if (button) markClicked(button);
  };
  root.addEventListener("scroll", onScroll, true);
  root.addEventListener("click", onClick, true);
  const observer = typeof MutationObserver === "undefined" ? null : new MutationObserver(markNew);
  observer?.observe(root.body ?? root, { childList: true, subtree: true });
  root.__aweSectionSpy = true;
  return () => {
    root.removeEventListener("scroll", onScroll, true);
    root.removeEventListener("click", onClick, true);
    observer?.disconnect();
    delete root.__aweSectionSpy;
  };
}
