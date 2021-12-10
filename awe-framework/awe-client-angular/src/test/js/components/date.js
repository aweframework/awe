import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {launchScreenAction} from "../utils";

describe('awe-framework/awe-client-angular/src/test/js/components/date.js', function() {
  var $injector, $rootScope, $compile, $httpBackend, $actionController, $screen, $control, $storage, $utilities, $settings;
  let model = {page:1, records:1, selected: "23/10/1978", total:1, values:[{label: "23/10/1978", value: "23/10/1978"}]};
  let emptyModel = {page:1, records:0, selected: null, total:0, values:[]};
  let controller = {checkInitial: true, checkTarget:false, checked:false, component:"date", contextMenu:[], dependencies:[], icon:"search", id:"Date", loadAll:false, optional:false, placeholder:"SCREEN_TEXT_DATE", printable:true, readonly:false, required:true, size:"lg", strict:true, style:"no-label", validation:"required", visible:true};

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Screen", "Control", "Storage", "AweUtilities", "AweSettings",
      function(__$injector__, _$rootScope_, _$compile_, _$httpBackend_, _ActionController_, _Screen_, _Control_, __Storage__, __AweUtilities__, __AweSettings__){
      $injector = __$injector__;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $httpBackend = _$httpBackend_;
      $actionController = _ActionController_;
      $screen = _Screen_;
      $control = _Control_;
      $storage = __Storage__;
      $utilities = __AweUtilities__;
      $settings = __AweSettings__;

      $rootScope.view = 'base';
      $rootScope.context = 'screen';

      // backend definition common for all tests
      $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language:"en"});
    }]);
  });

  it('replaces the element with the appropriate content', function() {
    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, 'checkOnlyComponent').and.returnValue(true);

    // Compile a piece of HTML containing the directive
    var element = $compile("<awe-input-date input-date-id='Date'/>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();

    expect(element.find(".validator").length).toBe(1);
    expect(element.attr("input-date-id")).toEqual("Date");
    expect(element.find(".fa-calendar").length).toBe(1);
  });

  it('initializes a date', function(done) {
    $rootScope.firstLoad = true;

    // Spy on storage
    spyOn($settings, "getLanguage").and.returnValue("en");
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressModel").and.returnValue(model);
    spyOn($utilities, "timeout").and.callFake(fn => {
      fn();
      done();
    });

    // Compile a piece of HTML containing the directive
    var element = $compile("<awe-input-date input-date-id='Date'/>")($rootScope);
    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
            id: "Date", controller, model: emptyModel}], screen: {name: "TEST"}, messages: []}}}, () => {
      $actionController.closeAllActions();

      // fire all the watches
      $rootScope.$digest();

      expect(element.attr("criterion-id")).toBe("Date");
      expect(element.find(".form-group.group-md").length).toBe(1);
      expect(element.find(".validator > span.add-on .fa.fa-calendar").length).toBe(1);
      expect(element.find(".validator > input[id='Date']").length).toBe(1);
      done();
    });
  });
});