import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/js/components/loader.js', function () {
    let  $injector, $rootScope, $templateCache, $compile, $httpBackend, Utilities, ServerData, $storage;

    // Mock module
    beforeEach(function () {
        angular.mock.module('aweApplication');

        // Inject controller
        inject(["$injector", "$rootScope", "$compile", "$templateCache", "$httpBackend", "Storage", "ServerData",  "AweUtilities",
            function (__$injector__, _$rootScope_, _$compile_, _$templateCache_, _$httpBackend_, _$storage_,  _ServerData_, _Utilities_ ) {
                $injector = __$injector__;
                $rootScope = _$rootScope_;
                $compile = _$compile_;
                $httpBackend = _$httpBackend_;
                $templateCache = _$templateCache_;
                Utilities = _Utilities_;
                ServerData = _ServerData_;
                $storage = _$storage_;

                $rootScope.view = 'base';
                $rootScope.context = 'screen';

                // backend definition common for all tests
                $httpBackend.when('POST', 'settings').respond({...DefaultSettings, language: "en"});
                $httpBackend.when('GET', 'http://server/template/angular/loader/circle').respond("TEST");

            }]);
    });

    it('try to initialize as component', function () {
        // Spy on storage
        spyOn($storage, "get").and.returnValue({'base': {}});
        spyOn(ServerData, "preloadAngularTemplate").and.callFake((a, fn) => fn());

        // Compile a piece of HTML containing the directive
        let  element = $compile("<awe-loader icon-loader='circle'/>")($rootScope);
        // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
        $rootScope.$digest();
        expect(element[0].tagName).toBe("AWE-LOADER");
    });

    it('try to initialize with a loading template', function () {
        // Spy on storage
        spyOn($storage, "get").and.returnValue({'base': {}});
        spyOn(ServerData, "preloadAngularTemplate").and.callFake((a, fn) => fn());
        spyOn($templateCache, "get").and.returnValue("--LOADING--");
        spyOn($rootScope, "$on").and.callFake((a, fn) => {
            setTimeout(fn, 0);
            return function() {};
        });

        // Compile a piece of HTML containing the directive
        let  element = $compile("<awe-loader icon-loader='square'/>")($rootScope);
        // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
        $rootScope.$digest();
        expect(element[0].tagName).toBe("AWE-LOADER");
    });

    it('try to initialize with a loaded template', function () {
        // Spy on storage
        spyOn($storage, "get").and.returnValue({'base': {}});
        spyOn(ServerData, "preloadAngularTemplate").and.callFake((a, fn) => fn());
        spyOn($templateCache, "get").and.returnValue("Loaded template");


        // Compile a piece of HTML containing the directive
        let  element = $compile("<awe-loader icon-loader='spinner'/>")($rootScope);
        // fire all the watches, so the scope expression {{1 + 1}} will be evaluated
        $rootScope.$digest();
        expect(element[0].tagName).toBe("AWE-LOADER");
    });
});