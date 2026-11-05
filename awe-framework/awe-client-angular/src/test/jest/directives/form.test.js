import "../../../main/resources/js/awe/app";

describe("Form server action", () => {
  let $compile;
  let $rootScope;
  let $serverData;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$injector", ($injector) => {
      $compile = $injector.get("$compile");
      $rootScope = $injector.get("$rootScope");
      $serverData = $injector.get("ServerData");
    }]);
    jest.spyOn($serverData, "getFormValues").mockReturnValue({});
    jest.spyOn($serverData, "launchServerAction").mockReturnValue(undefined);
  });

  function launch(origin) {
    const target = {view: "base", component: "button"};
    const scope = $rootScope.$new();
    $compile("<form awe-form></form>")(scope);
    scope.$broadcast("/action/server", {attr: (key) => ({callbackTarget: target, origin}[key])});
    return target;
  }

  it("asks for the base values only when the action comes from the menu", () => {
    const target = launch("menu");
    expect($serverData.getFormValues).toHaveBeenCalledWith(target, "base");
  });

  it("asks for all the values when the action does not come from the menu", () => {
    const target = launch(undefined);
    expect($serverData.getFormValues).toHaveBeenCalledWith(target, undefined);
  });
});
