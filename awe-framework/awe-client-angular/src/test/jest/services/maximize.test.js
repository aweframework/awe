// Source-traceable Jest owner for the Karma maximize service spec.
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

require("../../js/services/maximize.js");

describe("Maximize Jest logging contract", () => {
  let Maximize;
  let Utilities;

  const controller = {
    maximize: true
  };

  const getNode = (node, resizing, parent) => ({
    val: () => node,
    offset: () => ({top: 100, left: 100}),
    scrollTop: () => 0,
    scrollLeft: () => 0,
    outerWidth: () => 100,
    outerHeight: () => 100,
    offsetParent: () => parent,
    clone: () => resizing,
    parents: () => parent,
    css: () => resizing,
    addClass: () => resizing,
    removeClass: () => resizing,
    find: () => resizing,
    empty: () => null,
    parentsUntil: () => [parent, resizing, parent]
  });

  beforeEach(() => {
    angular.mock.module("aweApplication");

    inject(["$injector", ($injector) => {
      Maximize = $injector.get("Maximize");
      Utilities = $injector.get("AweUtilities");
    }]);
  });

  it("maximizes panels without dumping internal jQuery nodes to console.info", () => {
    const element = $(document.createElement("div"));
    const parent = $(document.createElement("div"));
    const resizing = $(document.createElement("div"));
    const $scope = {
      controller,
      $root: {},
      $on: jest.fn().mockName("$on"),
      $emit: jest.fn().mockName("$emit"),
      $broadcast: jest.fn().mockName("$broadcast")
    };
    jest.spyOn(Utilities, "timeout").mockImplementation((fn) => fn());
    jest.spyOn(element, "val").mockReturnValue(getNode(element, resizing, parent));
    jest.spyOn(resizing, "val").mockReturnValue(getNode(resizing, resizing, resizing));
    jest.spyOn(parent, "val").mockReturnValue(getNode(parent, parent, parent));
    jest.spyOn($.fn, "offset").mockReturnValue({top: 0, left: 0});
    jest.spyOn(Utilities, "animateCSS").mockImplementation((a, b, c, fn) => fn());
    const infoSpy = jest.spyOn(console, "info").mockImplementation(() => {});

    try {
      Maximize.initMaximize($scope, element);
      $scope.maximizePanel();

      expect($scope.$broadcast).toHaveBeenCalledWith("resize");
      expect(infoSpy).not.toHaveBeenCalled();
    } finally {
      infoSpy.mockRestore();
    }
  });
});
