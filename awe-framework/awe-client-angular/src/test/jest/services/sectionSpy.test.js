// The section navigation of the editors (#993, awe-editor styles): the AngularJS application follows the scroll of their
// body and marks the button of the section in sight, as the React engine (shared sectionSpy utility)
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("Section spy of the editors", function () {
  beforeEach(function () {
    angular.mock.module("aweApplication");
    inject(["$httpBackend", function ($httpBackend) {
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      $httpBackend.when("POST", "./settings").respond(DefaultSettings);
    }]);
  });

  afterEach(function () {
    document.body.innerHTML = "";
  });

  it("marks the button of the section at the top of the body when the body of an editor scrolls", function () {
    jest.useFakeTimers();
    const requestAnimationFrame = window.requestAnimationFrame;
    window.requestAnimationFrame = callback => setTimeout(callback, 0);
    try {
      document.body.innerHTML = `<div class="awe-editor"><div class="awe-editor-body">
        <nav class="awe-editor-nav"><button id="NavA">A</button><button id="NavB">B</button></nav>
        <div class="awe-editor-sections"><section id="SecA"></section><section id="SecB"></section></div></div></div>`;
      const body = document.querySelector(".awe-editor-body");
      body.getBoundingClientRect = () => ({top: 100});
      document.getElementById("SecA").getBoundingClientRect = () => ({top: -400});
      document.getElementById("SecB").getBoundingClientRect = () => ({top: 120});

      body.dispatchEvent(new Event("scroll"));
      jest.runAllTimers();

      expect(document.getElementById("NavB").getAttribute("aria-current")).toBe("location");
      expect(document.getElementById("NavA").hasAttribute("aria-current")).toBe(false);
    } finally {
      window.requestAnimationFrame = requestAnimationFrame;
      jest.useRealTimers();
    }
  });
});
