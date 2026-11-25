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

  describe("as choice cards (style awe-choice-cards)", () => {
    const address = {view: "base", component: "when"};

    it("draws one card per option with its title and description and chooses it by click and by the arrow keys", () => {
      $control.setAddressController(address, {
        component: "button-radio", contextMenu: [], dependencies: [], id: "when", label: "When", initialLoad: "query",
        targetAction: "options", optional: false, readonly: false, required: false, size: "md",
        style: "col-xs-12 awe-choice-cards", visible: true, validation: ""
      });
      $control.setAddressModel(address, {
        defaultValues: [], page: 1, records: 3, selected: ["0"], total: 1,
        values: [{value: "0", label: "Manual", icon: "lucide:hand", description: "Only by hand"},
          {value: "1", label: "Scheduled", icon: "lucide:clock", description: "At fixed days and times"},
          {value: "2", label: "On a file"}]
      });
      const element = compile(["when"]);
      const group = element.querySelector("[criterion-id='when'] [role=radiogroup]");
      const cards = Array.from(group.querySelectorAll("label.awe-choice"));
      const checked = () => cards.filter(card => card.classList.contains("awe-choice-checked"))
        .map(card => card.getAttribute("option-id"));
      const input = value => group.querySelector(`input[value='${value}']`);

      expect(group.getAttribute("aria-label")).toBe("When");
      expect(group.querySelectorAll(".btn").length).toBe(0);
      expect(cards.map(card => card.querySelector(".awe-choice-title").textContent.trim()))
        .toEqual(["Manual", "Scheduled", "On a file"]);
      const description = card => card.querySelector(".awe-choice-description");
      expect(cards.map(card => description(card) ? description(card).textContent.trim() : null))
        .toEqual(["Only by hand", "At fixed days and times", null]);
      expect(input("1").getAttribute("aria-describedby"))
        .toBe(description(cards[1]).getAttribute("id"));
      expect(checked()).toEqual(["0"]);

      input("2").click();
      $rootScope.$digest();
      expect(String($control.getAddressModel(address).selected)).toBe("2");
      expect(checked()).toEqual(["2"]);

      input("2").dispatchEvent(new window.KeyboardEvent("keydown", {key: "ArrowRight", bubbles: true}));
      $rootScope.$digest();
      expect(String($control.getAddressModel(address).selected)).toBe("0");
      expect(checked()).toEqual(["0"]);
    });
  });

  describe("as chips (style awe-chips) and as toggles (select-multiple display toggles, #993 T37)", () => {
    it("draws one pill per option, the chosen one with a tick, chosen by click and by the arrow keys", () => {
      const address = {view: "base", component: "preset"};
      $control.setAddressController(address, {
        component: "button-radio", contextMenu: [], dependencies: [], id: "preset", label: "Repetition", initialLoad: "enum",
        targetAction: "presets", optional: false, readonly: false, required: false, size: "md",
        style: "col-xs-12 awe-chips", visible: true, validation: ""
      });
      $control.setAddressModel(address, {
        defaultValues: [], page: 1, records: 3, selected: ["WRK"], total: 1,
        values: [{value: "DAY", label: "Daily"}, {value: "WRK", label: "Workdays"}, {value: "WEK", label: "Weekly"}]
      });
      const element = compile(["preset"]);
      const group = element.querySelector("[criterion-id='preset'] [role=radiogroup]");
      const chips = Array.from(group.querySelectorAll("label.awe-chip"));
      const checked = () => chips.filter(chip => chip.classList.contains("awe-chip-checked")).map(chip => chip.getAttribute("option-id"));
      const ticks = () => chips.map(chip => chip.querySelector(".awe-chip-tick") !== null);
      const input = value => group.querySelector(`input[value='${value}']`);

      expect(group.getAttribute("aria-label")).toBe("Repetition");
      expect(chips.map(chip => chip.querySelector(".awe-chip-label").textContent.trim())).toEqual(["Daily", "Workdays", "Weekly"]);
      expect(checked()).toEqual(["WRK"]);
      expect(ticks()).toEqual([false, true, false]);

      input("WEK").click();
      $rootScope.$digest();
      expect(String($control.getAddressModel(address).selected)).toBe("WEK");
      expect(ticks()).toEqual([false, false, true]);

      input("WEK").dispatchEvent(new window.KeyboardEvent("keydown", {key: "ArrowRight", bubbles: true}));
      $rootScope.$digest();
      expect(String($control.getAddressModel(address).selected)).toBe("DAY");
    });

    it("draws a select multiple of display toggles as toggle pills named by their title, pressed and released", () => {
      const address = {view: "base", component: "days"};
      $control.setAddressController(address, {
        component: "select-multiple", contextMenu: [], dependencies: [], id: "days", label: "Days", initialLoad: "query",
        targetAction: "days", optional: true, readonly: false, required: false, size: "md", style: "col-xs-12",
        display: "toggles", visible: true, validation: ""
      });
      $control.setAddressModel(address, {
        defaultValues: [], page: 1, records: 3, selected: ["1", "0"], total: 1,
        values: [{value: "1", label: "M", title: "Monday"}, {value: "2", label: "T", title: "Tuesday"},
          {value: "0", label: "S", title: "Sunday"}]
      });
      const container = $compile(`<div><awe-input-select-multiple input-select-multiple-id="days"></awe-input-select-multiple></div>`)($rootScope);
      $rootScope.$digest();
      const group = container[0].querySelector("[criterion-id='days'] [role=group]");
      const inputs = Array.from(group.querySelectorAll("input[type=checkbox]"));
      const pressed = () => Array.from(group.querySelectorAll("label.awe-chip.awe-chip-checked")).map(chip => chip.getAttribute("option-id"));

      expect(group.getAttribute("aria-label")).toBe("Days");
      expect(container[0].querySelector("[ui-select2]")).toBeNull();
      expect(inputs.map(input => input.getAttribute("aria-label"))).toEqual(["Monday", "Tuesday", "Sunday"]);
      expect(pressed()).toEqual(["1", "0"]);

      inputs[1].click();
      $rootScope.$digest();
      expect($control.getAddressModel(address).selected).toEqual(["1", "2", "0"]);

      inputs[0].click();
      $rootScope.$digest();
      expect($control.getAddressModel(address).selected).toEqual(["2", "0"]);
      expect(pressed()).toEqual(["2", "0"]);
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
