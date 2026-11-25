import {installSectionSpy, markCurrentSection} from "../../../src/utilities/sectionSpy";

/**
 * #993: the section navigation of an editor (awe-editor styles) marks the button of the section at the top of its
 * scrolling body, the buttons and the sections in the same order
 */
describe("awe-client-react/test/js/utilities/sectionSpyTest.js", () => {
  let uninstall;
  const top = (element, value) => { element.getBoundingClientRect = () => ({top: value}); };

  /**
   * An editor whose body starts at 100 and three sections at the given tops
   */
  const editor = (tops) => {
    document.body.innerHTML = `<div class="awe-editor"><div class="awe-editor-body">
      <nav class="awe-editor-nav"><span>Sections</span><button id="NavA">A</button><button id="NavB">B</button><button id="NavC">C</button></nav>
      <div class="awe-editor-sections"><section id="SecA"></section><section id="SecB"></section><section id="SecC"></section></div>
    </div></div>`;
    const body = document.querySelector(".awe-editor-body");
    top(body, 100);
    Object.defineProperties(body, {clientHeight: {value: 600, configurable: true}, scrollHeight: {value: 2000, configurable: true}});
    ["SecA", "SecB", "SecC"].forEach((id, index) => top(document.getElementById(id), tops[index]));
    return body;
  };
  const current = () => Array.from(document.querySelectorAll("[aria-current='location']")).map(button => button.id);

  afterEach(() => {
    uninstall?.();
    uninstall = null;
    document.body.innerHTML = "";
  });

  it("marks the section whose top has reached the top of the body, and the last one at the end of the scroll", () => {
    const body = editor([100, 700, 1300]);
    expect(markCurrentSection(document.querySelector(".awe-editor"))).toBe(0);
    expect(current()).toEqual(["NavA"]);

    ["SecA", "SecB", "SecC"].forEach((id, index) => top(document.getElementById(id), [-500, 120, 720][index]));
    markCurrentSection(document.querySelector(".awe-editor"));
    expect(current()).toEqual(["NavB"]);

    // A short last section never reaches the top: in the last screen of the scroll the line slides down, so at the end
    // of the scroll the last section in sight is the current one
    body.scrollTop = 1400;
    ["SecA", "SecB", "SecC"].forEach((id, index) => top(document.getElementById(id), [-1100, -500, 600][index]));
    markCurrentSection(document.querySelector(".awe-editor"));
    expect(current()).toEqual(["NavC"]);
  });

  it("keeps the button clicked marked while the scroll it launches lasts, and follows the scroll again after it", () => {
    jest.useFakeTimers();
    const requestAnimationFrame = window.requestAnimationFrame;
    window.requestAnimationFrame = callback => setTimeout(callback, 0);
    try {
      const body = editor([100, 700, 1300]);
      uninstall = installSectionSpy(document);
      document.getElementById("NavB").click();
      // The scroll ends at the bottom: without the click the last section would be the current one
      body.scrollTop = 1400;
      ["SecA", "SecB", "SecC"].forEach((id, index) => top(document.getElementById(id), [-1100, -500, 600][index]));
      body.dispatchEvent(new Event("scroll"));
      jest.runAllTimers();
      expect(current()).toEqual(["NavB"]);

      jest.advanceTimersByTime(1500);
      body.dispatchEvent(new Event("scroll"));
      jest.runAllTimers();
      expect(current()).toEqual(["NavC"]);
    } finally {
      window.requestAnimationFrame = requestAnimationFrame;
      jest.useRealTimers();
    }
  });

  it("follows the scroll of the body of an editor once installed", () => {
    jest.useFakeTimers();
    const requestAnimationFrame = window.requestAnimationFrame;
    window.requestAnimationFrame = callback => setTimeout(callback, 0);
    try {
      const body = editor([100, 700, 1300]);
      uninstall = installSectionSpy(document);
      ["SecA", "SecB", "SecC"].forEach((id, index) => top(document.getElementById(id), [-1100, -500, 110][index]));

      body.dispatchEvent(new Event("scroll"));
      jest.runAllTimers();

      expect(current()).toEqual(["NavC"]);
    } finally {
      window.requestAnimationFrame = requestAnimationFrame;
      jest.useRealTimers();
    }
  });
});
