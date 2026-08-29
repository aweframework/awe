import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";
import "../../../../main/resources/js/awe/app";
import "../../../../main/resources/webpack/locals-en-GB.config";
import "../../../../main/resources/webpack/locals-es-ES.config";

const LOG_VIEWER_TEMPLATE_URL = "test-log-viewer-template";
const LOG_VIEWER_TEMPLATE = `
  <div class="log-viewer" ui-dependency="dependencies" ng-cloak>
    <div class="content">
      <pre class="test-line">...</pre>
      <pre class="visible-text"></pre>
    </div>
  </div>
`;

/**
 * Builds an action-shaped object matching the ActionController contract:
 * {@code action.attr(key)} reads a named attribute (mirrors controllers/message.test.js).
 */
function actionWith(parameters) {
  return {
    attr: jest.fn(key => ({parameters})[key])
  };
}

describe("aweLogViewer", () => {
  let $rootScope;
  let $compile;
  let $httpBackend;
  let $templateCache;
  let constructedComponent;
  let serverData;
  let actionController;
  let utilities;
  let testLineHeight;

  beforeEach(() => {
    testLineHeight = 12;
    serverData = {getAngularTemplateUrl: jest.fn(() => LOG_VIEWER_TEMPLATE_URL)};
    actionController = {
      acceptAction: jest.fn(),
      resolveAction: jest.fn((action, {service, method}) => service[method]())
    };

    function ComponentMock(scope, id) {
      constructedComponent = {asComponent: jest.fn(() => true), controller: {parameters: {stickBottom: true}}, id, scope};
      scope.controller = constructedComponent.controller;
      return constructedComponent;
    }

    angular.mock.module("aweApplication", {
      Component: ComponentMock,
      ServerData: serverData,
      ActionController: actionController
    });
    inject(["$rootScope", "$compile", "$httpBackend", "$templateCache", "AweUtilities", (_$rootScope_, _$compile_, _$httpBackend_, _$templateCache_, _AweUtilities_) => {
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $templateCache = _$templateCache_;
      utilities = _AweUtilities_;
      jest.spyOn(utilities, "stickToBottom").mockImplementation(() => {});
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      $templateCache.put(LOG_VIEWER_TEMPLATE_URL, LOG_VIEWER_TEMPLATE);
    }]);

    // jsdom performs no real layout: pin the probe's measured height so the fast-path/height
    // math under test is deterministic, while leaving every other jQuery .height() call (the
    // viewport container) untouched.
    const originalHeight = $.fn.height;
    jest.spyOn($.fn, "height").mockImplementation(function (...args) {
      if (this.hasClass && this.hasClass("test-line")) {
        return testLineHeight;
      }
      return originalHeight.apply(this, args);
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  function compileLogViewer() {
    const element = $compile("<awe-log-viewer log-viewer-id='viewerId'></awe-log-viewer>")($rootScope);
    $rootScope.$digest();
    document.body.appendChild(element[0]);
    return element;
  }

  it("appends new content and grows the content height by the measured line height", () => {
    const element = compileLogViewer();

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0", "line1", "line2"]}));

    expect(element.find(".content").height()).toBe(3 * testLineHeight);
    expect(actionController.acceptAction).toHaveBeenCalled();
  });

  it("replace mode atomically swaps content instead of concatenating onto it", () => {
    const element = compileLogViewer();

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["a", "b", "c", "d", "e"]}));
    expect(element.find(".content").height()).toBe(5 * testLineHeight);

    // A replace page always carries the full current window: fewer lines than what was
    // previously appended proves the old content was discarded, not concatenated onto.
    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["x", "y"], replace: true}));

    expect(element.find(".content").height()).toBe(2 * testLineHeight);
  });

  it("keeps using the last non-zero line height once the probe measures zero again", () => {
    const element = compileLogViewer();

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0", "line1"]}));
    expect(element.find(".content").height()).toBe(2 * testLineHeight);

    // Simulate the probe transiently reporting zero (e.g. around a reset/replace repaint):
    // the cached line height must survive, or the scrollbar and stick-to-bottom math break.
    testLineHeight = 0;
    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line2"], replace: true}));

    expect(element.find(".content").height()).toBe(12);
  });

  it("prefers the computed line-height when the jQuery probe height is unusable", () => {
    const element = compileLogViewer();

    // Real-world case: jQuery .height() subtracts the pre's padding from a zero-height
    // (hidden or unsettled) probe and returns a negative value, while the computed
    // line-height stays a reliable positive per-line metric.
    element.find(".test-line")[0].style.lineHeight = "17px";
    testLineHeight = -17;

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0", "line1", "line2"]}));

    expect(element.find(".content").height()).toBe(3 * 17);
  });

  it("pins scroll to the bottom on append when stickBottom is enabled", () => {
    compileLogViewer();

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0"]}));

    expect(utilities.stickToBottom).toHaveBeenCalledWith(expect.any(Boolean), testLineHeight, true, expect.anything());
  });

  it("onReset clears the accumulated content so the next append starts from empty", () => {
    const element = compileLogViewer();
    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0", "line1"]}));
    expect(element.find(".content").height()).toBe(2 * testLineHeight);

    $rootScope.$broadcast("/action/reset", actionWith({}));
    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0"]}));

    expect(element.find(".content").height()).toBe(1 * testLineHeight);
  });

  it("round-trips the received window version on the next poll", () => {
    actionController.addActionList = jest.fn();
    serverData.getServerAction = jest.fn((address, values) => values);
    compileLogViewer();

    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0"], version: "v-abc"}));
    constructedComponent.reload();

    const sentValues = actionController.addActionList.mock.calls[0][0][0];
    expect(sentValues.version).toBe("v-abc");
  });

  it("sends no version on the first poll before any log-delta was received", () => {
    actionController.addActionList = jest.fn();
    serverData.getServerAction = jest.fn((address, values) => values);
    compileLogViewer();

    constructedComponent.reload();

    const sentValues = actionController.addActionList.mock.calls[0][0][0];
    expect(sentValues.version).toBeNull();
  });

  it("onReset clears the remembered window version so the next poll echoes none", () => {
    actionController.addActionList = jest.fn();
    serverData.getServerAction = jest.fn((address, values) => values);
    compileLogViewer();
    $rootScope.$broadcast("/action/log-delta", actionWith({log: ["line0"], version: "v-abc"}));

    $rootScope.$broadcast("/action/reset", actionWith({}));
    constructedComponent.reload();

    const sentValues = actionController.addActionList.mock.calls[0][0][0];
    expect(sentValues.version).toBeNull();
  });
});
