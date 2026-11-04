import "./lightComponentTestUtils";

import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

// A button-radio criterion with options (initial-load enum or query) is one joined group of buttons, one per option,
// as the React engine draws it (#791 S2), so one XML works in both engines. Without options each criterion is still
// one button of its own value, grouped with the others by their group
describe("button-radio criterion", () => {
  let $rootScope, $compile, $httpBackend, $control, $storage;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$rootScope", "$compile", "$httpBackend", "Control", "Storage",
      (_$rootScope_, _$compile_, _$httpBackend_, _Control_, _Storage_) => {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
        $control = _Control_;
        $storage = _Storage_;
        $rootScope.view = "base";
        $rootScope.context = "screen";
        $rootScope.firstLoad = true;
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
        $httpBackend.when("POST", "./settings").respond(DefaultSettings);
        $storage.put("controller", {base: {}});
        $storage.put("model", {base: {}});
        $storage.put("api", {base: {}});
        $storage.put("status", {base: "loaded"});
      }]);
  });

  const compile = (ids) => {
    const html = ids.map(id => `<awe-input-button-radio input-button-radio-id="${id}"></awe-input-button-radio>`).join("");
    const container = $compile(`<div>${html}</div>`)($rootScope);
    $rootScope.$digest();
    return container[0];
  };

  describe("with options", () => {
    const address = {view: "base", component: "subject-type"};

    // The model the server sends for <criteria component="button-radio" initial-load="enum" value="user" checked="true">:
    // the options of the enum, and the value as the selected one only with checked="true"
    const render = ({readonly = false, checked = true, validation = "", visible = true} = {}) => {
      $control.setAddressController(address, {
        component: "button-radio", contextMenu: [], dependencies: [], id: "subject-type", label: "Subject",
        initialLoad: "enum", targetAction: "access-subject-types", value: "user", checked, optional: false, readonly,
        required: validation === "required", size: "md", style: "col-xs-12", visible, validation
      });
      $control.setAddressModel(address, {
        defaultValues: [], page: 1, records: 2, selected: checked ? ["user"] : [], total: 1,
        values: [{value: "profile", label: "Profile"}, {value: "user", label: "User"}]
      });
      return compile(["subject-type"]);
    };

    const group = element => element.querySelector("[criterion-id='subject-type'] [role=radiogroup]");
    const buttons = element => Array.from(group(element).querySelectorAll("label.btn"));
    const input = (element, value) => group(element).querySelector(`input[value='${value}']`);
    const activeValues = element => buttons(element).filter(button => button.classList.contains("active"))
      .map(button => button.getAttribute("option-id"));
    const choose = (element, value) => {
      input(element, value).click();
      $rootScope.$digest();
    };

    it("draws one group with one button per option, labelled by the option", () => {
      const element = render();

      expect(element.querySelectorAll("[role=radiogroup]").length).toBe(1);
      expect(buttons(element).map(button => button.textContent.trim())).toEqual(["Profile", "User"]);
      expect(buttons(element).map(button => button.getAttribute("option-id"))).toEqual(["profile", "user"]);
      expect(group(element).getAttribute("aria-label")).toBe("Subject");
    });

    it("starts on the selected value of the model", () => {
      const element = render();

      expect(activeValues(element)).toEqual(["user"]);
      expect(input(element, "user").checked).toBe(true);
      expect(input(element, "profile").checked).toBe(false);
    });

    it("starts with no option chosen when the model has no selected value", () => {
      const element = render({checked: false});

      expect(activeValues(element)).toEqual([]);
    });

    it("chooses the clicked option: the value of the criterion, sent and published as a change", () => {
      const element = render();
      const published = jest.spyOn($control, "publishModelChanged");

      choose(element, "profile");

      expect(String($control.getAddressModel(address).selected)).toBe("profile");
      expect(activeValues(element)).toEqual(["profile"]);
      expect($control.getAddressApi(address).getData()).toEqual({"subject-type": "profile"});
      expect(published).toHaveBeenCalledWith(expect.objectContaining({component: "subject-type"}),
        expect.objectContaining({selected: "profile"}));
    });

    it("moves the choice with the arrow keys", () => {
      const element = render();

      input(element, "user").dispatchEvent(new window.KeyboardEvent("keydown", {key: "ArrowLeft", bubbles: true}));
      $rootScope.$digest();

      expect(String($control.getAddressModel(address).selected)).toBe("profile");
    });

    it("does not change while readonly", () => {
      const element = render({readonly: true});

      expect(input(element, "profile").disabled).toBe(true);
      choose(element, "profile");

      expect(String($control.getAddressModel(address).selected)).toBe("user");
    });

    it("is required: invalid with no option chosen, valid once one is chosen", () => {
      const element = render({checked: false, validation: "required"});

      expect($control.getAddressApi(address).validate()).not.toBeNull();

      choose(element, "user");

      expect($control.getAddressApi(address).validate()).toBeNull();
    });

    it("follows its visibility", () => {
      const element = render({visible: false});
      const criterion = element.querySelector("[criterion-id='subject-type']");

      expect(criterion.classList.contains("ng-hide")).toBe(true);

      $control.changeControllerAttribute(address, {visible: true});
      $rootScope.$digest();

      expect(criterion.classList.contains("ng-hide")).toBe(false);
    });
  });

  describe("without options", () => {
    // The model the server sends for <criteria component="button-radio" group="RadBoxGrp2" value="Radio1">: its own
    // value as its only value, labelled with its id
    const radio = (id, value, checked) => {
      const address = {view: "base", component: id};
      $control.setAddressController(address, {
        component: "button-radio", contextMenu: [], dependencies: [], id, label: id, group: "RadBoxGrp2",
        value, checked, optional: false, readonly: false, required: false, size: "md", style: "col-xs-2",
        visible: true, validation: ""
      });
      $control.setAddressModel(address, {
        defaultValues: [], page: 1, records: 0, selected: checked ? [value] : [], total: 0,
        values: [{label: id, value}]
      });
    };

    it("draws each criterion as one button of its own value, grouped by its group", () => {
      radio("RadBox21", "Radio1", false);
      radio("RadBox22", "Radio2", true);
      const element = compile(["RadBox21", "RadBox22"]);

      expect(element.querySelectorAll("[role=radiogroup]").length).toBe(0);
      const criteria = Array.from(element.children);
      expect(criteria.map(criterion => criterion.tagName)).toEqual(["LABEL", "LABEL"]);
      expect(criteria.map(criterion => criterion.getAttribute("criterion-id"))).toEqual(["RadBox21", "RadBox22"]);
      expect(criteria.map(criterion => criterion.querySelectorAll("input[type=radio]").length)).toEqual([1, 1]);
      expect(criteria.map(criterion => criterion.classList.contains("active"))).toEqual([false, true]);

      criteria[0].querySelector("input").click();
      $rootScope.$digest();

      expect($control.getAddressViewApi({view: "base"}).RadBoxGrp2.getData()).toEqual({RadBoxGrp2: "Radio1"});
      expect(criteria.map(criterion => criterion.classList.contains("active"))).toEqual([true, false]);
    });
  });
});
