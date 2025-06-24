import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {launchScreenAction} from "../utils";

describe('awe-framework/awe-client-angular/src/test/js/components/menu.js', function() {
  let $injector, $rootScope, $compile, $httpBackend, $actionController, $storage, $control, $utilities;
  let model = {page:1, records:3, selected: "3", total:1, values:[{label: "Step 1", value: "1"}, {label: "Step 2", value: "2"}, {label: "Step 3", value: "3"}]};
  let controller = {id:"menuId", style:"horizontal"};

  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    // Inject controller
    inject(["$injector", "$rootScope", "$compile", "$httpBackend", "ActionController", "Storage", "Control", "AweUtilities",
      function(__$injector__, _$rootScope_, _$compile_, _$httpBackend_, __$actionController__, __$storage__, __$control__, __$utilities__){
        $injector = __$injector__;
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $actionController = __$actionController__;
        $storage = __$storage__;
        $control = __$control__;
        $utilities = __$utilities__;

        $rootScope.view = 'base';
        $rootScope.context = 'screen';

        // backend definition common for all tests
        $httpBackend.when('POST', 'settings').respond(DefaultSettings);
      }]);
  });

  it('replaces the element with the appropriate content', function() {
    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);
    // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
    $rootScope.$digest();

    expect(element.find("ul.awe-menu").length).toBe(1);
  });

  it('initializes a visible horizontal menu ', function(done) {
    $("body").removeClass("mmc");

    $rootScope.firstLoad = true;

    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressController").and.returnValue({style:"horizontal"});

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);

    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
            id: "menuId", controller: controller, model: model}], screen: {name: "TEST"}, messages: []}}}, () => {
      // Close all actions
      $actionController.closeAllActions();

      // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
      $rootScope.$digest();

      // Expect
      let scope = element.find("ul.awe-menu").scope();
      expect(scope.visible).toBe(true);
      expect(scope.status.minimized).toBe(false);

      done();
    });
  });

  it('initializes a minimized horizontal menu', function(done) {
    $rootScope.firstLoad = true;
    $("body").addClass("mmc");

    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressController").and.returnValue({style:"horizontal"});

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);

    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
            id: "menuId", controller: controller, model: model}], screen: {name: "TEST"}, messages: []}}}, () => {
      // Close all actions
      $actionController.closeAllActions();

      // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
      $rootScope.$digest();

      // Expect
      let scope = element.find("ul.awe-menu").scope();
      expect(scope.visible).toBe(false);
      expect(scope.status.minimized).toBe(true);

      done();
    });
  });

  it('initializes a visible vertical menu', function(done) {
    $rootScope.firstLoad = true;
    $("body").removeClass("mmc");

    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressController").and.returnValue({style:"vertical"});

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);

    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
            id: "menuId", controller: controller, model: model}], screen: {name: "TEST"}, messages: []}}}, () => {
      // Close all actions
      $actionController.closeAllActions();

      // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
      $rootScope.$digest();

      // Expect
      let scope = element.find("ul.awe-menu").scope();
      expect(scope.visible).toBe(true);
      expect(scope.status.minimized).toBe($("body").hasClass("mmc"));

      done();
    });
  });

  it('initializes a minimized vertical menu', function(done) {
    $rootScope.firstLoad = true;
    $("body").addClass("mmc");

    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressController").and.returnValue({style:"vertical"});

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);

    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [{type: "reload"}], components: [{
            id: "menuId", controller: controller, model: model}], screen: {name: "TEST"}, messages: []}}}, () => {
      // Close all actions
      $actionController.closeAllActions();

      // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
      $rootScope.$digest();

      // Expect
      let scope = element.find("ul.awe-menu").scope();
      expect(scope.visible).toBe(true);
      expect(scope.status.minimized).toBe($("body").hasClass("mmc"));

      done();
    });
  });

  it('initializes a menu with an expanded option', function(done) {
    $rootScope.firstLoad = true;
    // Remove minimize class from body
    $("body").removeClass("mmc");

    // Create a controller with options, one of which has expanded=true
    let controllerWithOptions = {
      id: "menuId",
      style: "vertical",
      options: [
        {
          id: "option1",
          name: "option1",
          label: "Option 1",
          visible: true,
          expanded: true,
          options: [
            {
              id: "suboption1",
              name: "suboption1",
              label: "Sub Option 1",
              visible: true
            }
          ]
        },
        {
          id: "option2",
          name: "option2",
          label: "Option 2",
          visible: true
        }
      ]
    };

    // Spy on storage
    spyOn($storage, "get").and.returnValue({'base': {}});
    spyOn($control, "checkComponent").and.returnValue(true);
    spyOn($control, "getAddressController").and.returnValue(controllerWithOptions);

    // Compile a piece of HTML containing the directive
    let element = $compile("<awe-menu menu-id='menuId'></awe-menu>")($rootScope);
    $rootScope.$digest();

    launchScreenAction($injector, "screen-data", "screenData", {parameters:{view: "base", screenData:{actions: [], components: [{
            id: "menuId", controller: controllerWithOptions, model: model}], screen: {name: "TEST"}, messages: []}}}, () => {
      // Close all actions
      $actionController.closeAllActions();

      // Usar setTimeout para asegurar que el DOM se actualice completamente
      setTimeout(() => {
        // Múltiples ciclos de digest para asegurar que todas las directivas se rendericen
        $rootScope.$digest();
        $rootScope.$digest();

        // Esperar un poco más para Chrome headless
        setTimeout(() => {
          // Buscar el elemento de manera más específica
          let optionElement = element.find("awe-option[option-name='option1']");

          // Si no se encuentra, intentar con otros selectores
          if (optionElement.length === 0) {
            optionElement = element.find("li.awe-option").filter(function() {
              return $(this).find("a[name='option1']").length > 0;
            });
          }

          // Fallback adicional
          if (optionElement.length === 0) {
            optionElement = element.find("li.mm-dropdown-root").first();
          }

          // Debug information más detallada para Chrome
          console.log("Full element HTML:", element[0].outerHTML);
          console.log("Found awe-option elements:", element.find("awe-option").length);
          console.log("Found li elements:", element.find("li").length);
          console.log("Body classes:", $("body").attr('class'));

          // Verificaciones
          expect(optionElement.length).toBeGreaterThan(0);
          if (optionElement.length > 0) {
            // Verificar que el elemento tiene la clase 'open' o 'mmc-dropdown-open'
            let hasOpenClass = optionElement.hasClass("open") || optionElement.hasClass("mmc-dropdown-open");
            console.log("Option element classes:", optionElement.attr('class'));
            console.log("Has open class:", hasOpenClass);
            expect(hasOpenClass).toBe(true);
          }

          done();
          }, 100); // Tiempo adicional para Chrome headless
        }, 50);
    });
  });
});