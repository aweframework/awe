import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/components/grid.js', function () {
  let  $injector, $rootScope, $compile, $httpBackend, $actionController, $screen, $control, $storage, $sce, $utilities;
  let model = {
    page: 1,
    records: 1,
    selected: "23/10/1978",
    total: 1,
    values: [{label: "23/10/1978", value: "23/10/1978"}]
  };
  let emptyModel = {page: 1, records: 0, selected: null, total: 0, values: []};
  let controller = {
    checkInitial: true,
    checkTarget: false,
    checked: false,
    component: "grid",
    url: "http://localhost",
    contextMenu: [],
    dependencies: [],
    icon: "search",
    id: "Grid",
    loadAll: false,
    optional: false,
    placeholder: "SCREEN_TEXT_DATE",
    printable: true,
    readonly: false,
    required: true,
    size: "lg",
    strict: true,
    style: "no-label",
    validation: "required",
    visible: true,
    headerModel: [],
    columnModel: [],
    buttonModel: []
  };

  // Mock module
  beforeEach(function () {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Screen", "Control", "Storage", "$sce", "AweUtilities",
      function (__$injector__, _$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Screen_, _Control_, __Storage__, _$sce_, _$utilities_) {
        $injector = __$injector__;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = _ActionController_;
        $screen = _Screen_;
        $control = _Control_;
        $storage = __Storage__;
        $sce = _$sce_;
        $utilities = _$utilities_;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en"});
        $httpBackend.when('GET', 'grid/row').respond("<div></div>");
        $httpBackend.when('GET', 'grid/pagination').respond("<div></div>");

      }]);
  });

  it('replaces the element with the appropriate content', function () {
    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkComponent').and.returnValue(true);
    spyOn($utilities, "timeout").and.callFake(fn => fn());
    spyOn($control, "getAddressController").and.returnValue(controller);

    // Compile a piece of HTML containing the directive
    let  element = $compile("<awe-grid grid-id='Grid'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();
    expect(element.attr("grid-id")).toEqual("Grid");
    expect(element[0].tagName).toBe("DIV");
  });
});