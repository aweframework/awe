import {routeMethods} from "../../../main/resources/js/awe/data/routes";

describe('awe-framework/awe-client-angular/src/test/js/data/routes.js', function() {

  it('should get the public screen url', function() {
    expect(routeMethods.public[1]({screenId:"tutu"})).toBe("screen/public/tutu");
  });

  it('should get the private screen url', function() {
    expect(routeMethods.private[1]({screenId:"tutu"})).toBe("screen/private/tutu");
  });

  it('should retrieve the view on a full page', function() {
    expect(routeMethods.view({screenId:"tutu?lalasdsad=12313"})).toBe("base");
  });

  it('should retrieve the view on a report page', function() {
    expect(routeMethods.view({subScreenId: "lala", screenId:"tutu"})).toBe("report");
  });

  it('should retrieve the screen on a full page', function() {
    expect(routeMethods.screen({screenId:"tutu?lalasdsad=12313"})).toBe("tutu");
  });

  it('should retrieve the screen on a report page', function() {
    expect(routeMethods.screen({subScreenId: "lala", screenId:"tutu"})).toBe("lala");
  });

  it('should retrieve screen data view', function() {
    spyOn(routeMethods, "view").and.returnValue({then: () => null});
    routeMethods.screenData[1]({});
    expect(routeMethods.view).toHaveBeenCalled();
  });

  it('should retrieve template on index', function() {
    let $injector = {get: () => jasmine.createSpy("get")};
    spyOn(angular, 'element').and.returnValue({injector: () => $injector});
    spyOn($injector, "get").and.returnValue({getScreenData: () => {}});
    routeMethods.template({});
    expect($injector.get).toHaveBeenCalled();
  });

  it('should retrieve template on a full page', function() {
    let $injector = {get: () => jasmine.createSpy("get")};
    spyOn(angular, 'element').and.returnValue({injector: () => $injector});
    spyOn($injector, "get").and.returnValue({getScreenData: () => {}});
    routeMethods.template({screenId:"tutu?lalasdsad=12313"});
    expect($injector.get).toHaveBeenCalled();
  });

  it('should retrieve template on a report page', function() {
    let $injector = {get: () => jasmine.createSpy("get")};
    spyOn(angular, 'element').and.returnValue({injector: () => $injector});
    spyOn($injector, "get").and.returnValue({getScreenData: () => {}});
    routeMethods.template({subScreenId: "lala", screenId:"tutu"});
    expect($injector.get).toHaveBeenCalled();
  });
});