// Suggest option groups: rows with a "group" locale key render under one non-selectable header per group.
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {wrapSelect2FormatResult} from "../../../main/resources/js/awe/services/testIds";

const {createContractScope} = require("../contracts/shared/contractSharedTestUtils.js");

describe("Selector option groups", function () {
  let $rootScope;
  let $selector;
  let $translate;
  let $control;
  let $timeout;
  let currentModels;
  let currentControllers;

  const groupedRows = [
    {value: "profile-1", label: "ADM - Administrator", group: "PARAMETER_PROFILE"},
    {value: "user-1", label: "test - Manager", group: "PARAMETER_USER"},
    {value: "profile-2", label: "OPE - Operator", group: "PARAMETER_PROFILE"}
  ];

  beforeEach(function () {
    angular.mock.module("aweApplication");

    currentModels = {};
    currentControllers = {};

    inject(["$injector", function ($injector) {
      $rootScope = $injector.get("$rootScope");
      $selector = $injector.get("Selector");
      $translate = $injector.get("$translate");
      $control = $injector.get("Control");
      $timeout = $injector.get("$timeout");
      const $httpBackend = $injector.get("$httpBackend");
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      $httpBackend.when("POST", "./settings").respond(DefaultSettings);
    }]);

    jest.spyOn($control, "getAddressModel").mockImplementation(address => currentModels[address.component]);
    jest.spyOn($control, "setAddressModel").mockImplementation((address, model) => {
      currentModels[address.component] = model;
    });
    jest.spyOn($control, "getAddressController").mockImplementation(address => currentControllers[address.component]);
    jest.spyOn($control, "checkComponent").mockReturnValue(true);
    jest.spyOn($translate, "instant").mockImplementation(value => ({PARAMETER_PROFILE: "Profile", PARAMETER_USER: "User"}[value] || value));
  });

  afterEach(function () {
    $(".select2-drop, .select2-drop-mask, .select2-container, input.select2-test").remove();
  });

  /**
   * Launch a suggest search and answer it with the given rows
   * @param {Array} rows Rows returned by the server
   * @param {String} term Search term
   * @return {Object} select2 results passed to the query callback
   */
  function suggestResults(rows, term = "") {
    const id = "CrtCompare";
    currentModels[id] = {values: [], selected: null, storedValues: []};
    currentControllers[id] = {id, optional: true, strict: true, targetAction: "compareWith"};
    const scope = createContractScope($rootScope, {});
    const component = new $selector(scope, id, {});
    component.asSuggest();
    component.reload = jest.fn().mockName("reload");
    const callback = jest.fn().mockName("queryCallback");

    scope.aweSelectOptions.query({term, callback});
    $timeout.flush();
    currentModels[id].values = rows;
    component.onModelChangedValues();

    expect(component.reload).toHaveBeenCalledTimes(1);
    expect(callback).toHaveBeenCalledTimes(1);
    return callback.mock.calls[0][0];
  }

  /**
   * Render select2 results in its dropdown (with the formatter of the uiSelect2 directive) and open it
   * @param {Array} results select2 results
   * @return {jQuery} select2 dropdown
   */
  function openSelect2(results) {
    const input = $("<input type='hidden' class='select2-test'/>").appendTo(document.body);
    input.select2({query: query => query.callback({results}), formatResult: wrapSelect2FormatResult(null, "CrtCompare")});
    input.select2("open");
    return {dropdown: input.select2("dropdown")};
  }

  it("groups suggest results under one translated header per group, in order of first appearance", function () {
    const results = suggestResults(groupedRows);

    expect(results).toEqual({
      results: [
        {text: "Profile", children: [{id: "profile-1", text: "ADM - Administrator"}, {id: "profile-2", text: "OPE - Operator"}]},
        {text: "User", children: [{id: "user-1", text: "test - Manager"}]}
      ]
    });
  });

  it("keeps only the groups with matching options when searching", function () {
    expect(suggestResults(groupedRows, "manager")).toEqual({
      results: [{text: "User", children: [{id: "user-1", text: "test - Manager"}]}]
    });
  });

  it("renders group headers as non-selectable entries the keyboard skips", function () {
    const {dropdown} = openSelect2(suggestResults(groupedRows).results);

    const headers = dropdown.find(".select2-result-with-children > .select2-result-label");
    expect(headers.map((index, header) => $(header).text()).get()).toEqual(["Profile", "User"]);
    expect(headers.closest("li").hasClass("select2-result-unselectable")).toBe(true);
    expect(headers.attr("role")).toBe("presentation");
    expect(headers.attr("data-testid")).toBeUndefined();

    const highlighted = () => dropdown.find(".select2-highlighted > .select2-result-label").text();
    expect(highlighted()).toBe("ADM - Administrator");
    dropdown.find("input.select2-input").trigger($.Event("keydown", {which: 40}));
    expect(highlighted()).toBe("OPE - Operator");
    dropdown.find("input.select2-input").trigger($.Event("keydown", {which: 40}));
    expect(highlighted()).toBe("test - Manager");
  });

  it("returns a flat list when no row carries a group, as before", function () {
    const results = suggestResults([{value: "A", label: "Alpha"}, {value: "B", label: "Beta"}]);

    expect(results).toEqual({results: [{id: "A", text: "Alpha"}, {id: "B", text: "Beta"}]});
  });
});
