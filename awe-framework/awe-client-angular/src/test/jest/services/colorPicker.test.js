import {cleanupHeavyComponentTest, initHeavyComponentTest} from "../components/heavyComponentTestUtils";
import {keepColorPickerInViewport} from "../../../main/resources/js/awe/services/colorPicker";

/**
 * Colour picker of the colour criterion and column (#949): near the right edge of the screen it stays in the viewport
 */
describe("awe-framework/awe-client-angular/src/test/jest/services/colorPicker.test.js", () => {
  const VIEWPORT = 1024;
  const PICKER_WIDTH = 140;
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    jest.spyOn(window.Element.prototype, "clientWidth", "get").mockReturnValue(VIEWPORT);
    // As in the browser: the swatch at the right edge of the screen, the picker where the library placed it
    jest.spyOn(window.Element.prototype, "getBoundingClientRect").mockImplementation(function () {
      if (this.classList.contains("colorpicker")) {
        return {left: parseFloat(this.style.left) - window.pageXOffset, top: 0, width: PICKER_WIDTH, height: 150};
      }
      return {left: VIEWPORT - 24, top: 10, width: 24, height: 24};
    });
  });

  afterEach(() => {
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  it("shifts the picker of a swatch at the right edge to the left until it fits in the viewport", () => {
    const scope = refs.$rootScope.$new();
    keepColorPickerInViewport(scope);
    const swatch = refs.$compile("<span colorpicker ng-model='colour'></span>")(scope);
    document.body.appendChild(swatch[0]);
    scope.$digest();

    swatch.triggerHandler("click");

    const picker = document.body.querySelector(".colorpicker.colorpicker-visible");
    expect(parseFloat(picker.style.left) + PICKER_WIDTH).toBeLessThanOrEqual(VIEWPORT);
    expect(parseFloat(picker.style.left)).toBeGreaterThanOrEqual(0);
  });
});
