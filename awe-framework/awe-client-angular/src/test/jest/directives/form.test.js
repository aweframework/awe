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

  describe("update-controller", () => {
    let Control;
    let api;

    beforeEach(() => {
      inject(["$injector", ($injector) => {
        Control = $injector.get("Control");
      }]);
      api = {toggleAutorefresh: jest.fn()};
      jest.spyOn(Control, "getAddressApi").mockReturnValue(api);
      jest.spyOn(Control, "changeControllerAttribute").mockReturnValue(undefined);
    });

    function update(parameters) {
      const target = {view: "base", component: "logViewer"};
      const scope = $rootScope.$new();
      $compile("<form awe-form></form>")(scope);
      const action = {attr: (key) => ({callbackTarget: target, parameters}[key]), accept: jest.fn()};
      scope.$broadcast("/action/update-controller", action);
      return {target, action};
    }

    it("stops the autorefresh of the component when it sets it to 0, and starts it with a period", () => {
      const {target, action} = update({attribute: "autorefresh", value: 0});
      expect(Control.changeControllerAttribute).toHaveBeenCalledWith(target, {autorefresh: 0});
      expect(api.toggleAutorefresh).toHaveBeenCalledWith(0, false);
      expect(action.accept).toHaveBeenCalled();

      update({attribute: "autorefresh", value: 5});
      expect(api.toggleAutorefresh).toHaveBeenLastCalledWith(5, true);
    });

    it("only changes the controller attribute for any other attribute", () => {
      const {target} = update({attribute: "visible", value: false});
      expect(Control.changeControllerAttribute).toHaveBeenCalledWith(target, {visible: false});
      expect(api.toggleAutorefresh).not.toHaveBeenCalled();
    });
  });
});
