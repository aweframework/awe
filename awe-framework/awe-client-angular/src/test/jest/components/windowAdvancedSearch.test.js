// Advanced search of a window (#790): the advanced block of the window, with its row (Advanced filters,
// the counter badge and the filters in effect) and its criteria, below the row or in a side panel
import {cleanupHeavyComponentTest, initHeavyComponentTest, stubPanelComponent} from "./heavyComponentTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {submitForm} from "../../../main/resources/js/awe/services/criterion";

const hook = id => `[data-testid='${id}']`;
const criterion = id => `[criterion-id='${id}']`;
const COLLAPSED = "window-advanced-collapsed";

describe("awe-framework/awe-client-angular/src/test/jest/components/windowAdvancedSearch.test.js", () => {
  let refs, controllers, models, restored;

  // A criterion of the test: an isolated scope that restores its default value as the criteria do
  beforeEach(() => {
    restored = [];
    angular.mock.module(["$compileProvider", $compileProvider => {
      $compileProvider.directive("testCriterion", () => ({
        restrict: "A",
        scope: {},
        link: (scope, elem) => {
          const id = elem.attr("criterion-id");
          scope.$on("restore-scope", (event, view) => {
            restored.push({id, view});
            models[id].selected = models[id].defaultValues;
          });
        }
      }));
    }]);
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    $(".modal").modal("hide");
    $(document.body).removeClass("modal-open");
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  const windowController = extra => ({
    id: "w1",
    component: "window",
    label: "Users",
    maximize: true,
    advancedMode: "inline",
    advancedCriteria: ["state", "locked"],
    searchCriteria: ["user", "profile", "state", "locked"],
    ...extra
  });

  const ESSENTIAL = `<div class='panel-body'>
      <div test-criterion criterion-id='user' class='criterion'><input id='user'></div>
      <div test-criterion criterion-id='profile' class='criterion'><input id='profile'></div>`;
  const BLOCK = `<awe-advanced advanced-id='adv1'>
        <div class='row'><div test-criterion criterion-id='state' class='criterion'><input id='state'></div></div>
        <div test-criterion criterion-id='locked' class='criterion'><input id='locked' type='checkbox'></div>
      </awe-advanced>`;

  const compileWindow = (extra = {}, body = `${ESSENTIAL}${BLOCK}</div>`) => {
    controllers = {
      w1: windowController(extra),
      user: {id: "user", component: "text", label: "Name"},
      profile: {id: "profile", component: "select", label: "Profile"},
      state: {id: "state", component: "select", label: "State"},
      locked: {id: "locked", component: "checkbox", label: "Locked"}
    };
    models = {
      w1: {selected: null, values: []},
      user: {selected: null, defaultValues: null, values: []},
      profile: {selected: "op", defaultValues: null, values: [{value: "op", label: "Operator"}, {value: "ad", label: "Admin"}]},
      state: {selected: "1", defaultValues: "1", values: [{value: "1", label: "Active"}, {value: "0", label: "Inactive"}]},
      locked: {selected: 0, defaultValues: null, values: []}
    };
    stubPanelComponent(refs, {}, controllers.w1);
    refs.$control.getAddressController.mockImplementation(address => controllers[address.component]);
    refs.$control.getAddressModel.mockImplementation(address => models[address.component]);
    const form = $("<form></form>").appendTo(document.body);
    const element = refs.$compile(`<awe-window window-id='w1'>${body}</awe-window>`)(refs.$rootScope.$new());
    form.append(element);
    refs.$rootScope.$digest();
    return element;
  };

  const digest = () => refs.$rootScope.$digest();
  const block = element => element.find(".window-advanced");
  const row = element => block(element).children(".window-advanced-row");
  const region = () => $(".window-advanced-region");
  const toggle = element => element.find(hook(TestIds.windowAdvancedToggle));
  const badge = element => element.find(hook(TestIds.windowAdvancedCount));
  const chipsGroup = element => element.find(hook(TestIds.windowFilterChips));
  const chips = element => element.find(hook(TestIds.windowFilterChip));
  const chipTexts = element => chips(element).map((i, chip) => $(chip).text().replace(/\s+/g, " ").trim()).get();
  const panel = () => $(hook(TestIds.windowAdvancedPanel));

  // A submit button with the onClick of the button directive, visible for jQuery
  const addSubmit = place => {
    const onClick = jest.fn();
    const button = $("<button type='submit'>Search</button>");
    place(button);
    button[0].getClientRects = () => [{}];
    const buttonScope = refs.$rootScope.$new();
    buttonScope.onClick = onClick;
    button.data("$scope", buttonScope);
    return onClick;
  };

  describe("a window without an advanced block (S9)", () => {
    it("renders as before: no row, no filters in effect and no panel", () => {
      const element = compileWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined},
        `${ESSENTIAL}</div>`);

      expect(element.children().map((i, child) => child.className.split(" ")[0]).get())
        .toEqual(["awe-panel-heading", "awe-panel-content"]);
      expect(toggle(element).length).toBe(0);
      expect(chipsGroup(element).length).toBe(0);
      expect(panel().length).toBe(0);
    });

    it("launches the first submit button of the form with Enter, as before", () => {
      const element = compileWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined},
        `${ESSENTIAL}</div>`);
      const first = addSubmit(button => button.prependTo($("form").first()));
      const own = addSubmit(button => button.appendTo(element.children(".awe-panel-content")));

      submitForm(element.find(criterion("user")));

      expect(first).toHaveBeenCalledTimes(1);
      expect(own).not.toHaveBeenCalled();
    });
  });

  describe("the advanced block (S17)", () => {
    it("draws nothing outside the block: the header and the body of the window are as the developer wrote them", () => {
      const element = compileWindow();

      expect(element.children().map((i, child) => child.className.split(" ")[0]).get())
        .toEqual(["awe-panel-heading", "awe-panel-content"]);
      expect(element.find(".panel-heading button").map((i, button) => button.getAttribute("data-testid")).get())
        .toEqual([TestIds.windowMaximizeButton]);
      expect(element.find(".window-advanced-bar").length).toBe(0);
      // Where the developer put it: after the essential criteria
      expect(block(element).parent().hasClass("panel-body")).toBe(true);
      expect(block(element).prev().is(criterion("profile"))).toBe(true);
    });

    it("is headed by its row, and the region with its children opens below it", () => {
      const element = compileWindow();

      expect(block(element).children().map((i, child) => child.className.split(" ")[0]).get())
        .toEqual(["window-advanced-row", "window-advanced-region"]);
      expect(row(element).children().first().is(hook(TestIds.windowAdvancedToggle))).toBe(true);
      expect(region().find(criterion("state")).length).toBe(1);
      expect(region().find(criterion("locked")).length).toBe(1);
      expect(region().find(criterion("user")).length).toBe(0);
    });
  });

  describe("inline mode (S2)", () => {
    it("is collapsed at load, with its children compiled, and the toggle shows and hides them", () => {
      const element = compileWindow();

      expect(toggle(element).attr("aria-expanded")).toBe("false");
      expect(toggle(element).attr("aria-controls")).toBe("w1-advanced-region");
      expect(region().attr("id")).toBe("w1-advanced-region");
      expect(region().hasClass(COLLAPSED)).toBe(true);
      expect(region().find(criterion("state")).isolateScope()).toBeDefined();
      expect(toggle(element).attr("data-testid-owner")).toBe("w1");
      expect(toggle(element).text()).toContain("SCREEN_TEXT_ADVANCED_FILTERS");
      expect(toggle(element).find("svg use").first().attr("href")).toContain("#sliders-horizontal");
      expect(toggle(element).find(".window-advanced-chevron use").attr("href")).toContain("#chevron-down");
      expect(toggle(element).attr("aria-haspopup")).toBeUndefined();

      toggle(element).trigger("click");
      expect(toggle(element).attr("aria-expanded")).toBe("true");
      expect(region().hasClass(COLLAPSED)).toBe(false);
      expect(toggle(element).text()).toContain("SCREEN_TEXT_HIDE_ADVANCED_FILTERS");
      // The row does not move: the region opens below it
      expect(row(element).next().is(region())).toBe(true);

      toggle(element).trigger("click");
      expect(toggle(element).attr("aria-expanded")).toBe("false");
      expect(region().hasClass(COLLAPSED)).toBe(true);
    });

    it("leaves the essential criteria as they are", () => {
      const element = compileWindow();

      expect(element.find(criterion("user")).closest(".window-advanced").length).toBe(0);
      expect(element.find(`.${COLLAPSED} ${criterion("user")}`).length).toBe(0);
    });
  });

  describe("counter badge (S4)", () => {
    it("counts only the advanced criteria whose value differs from its default, and follows the values", () => {
      const element = compileWindow();

      // The profile (essential) differs from its default: it is not counted
      expect(badge(element).hasClass("ng-hide")).toBe(true);
      expect(badge(element).closest(hook(TestIds.windowAdvancedToggle)).length).toBe(1);

      models.state.selected = "0";
      digest();
      expect(badge(element).hasClass("ng-hide")).toBe(false);
      expect(badge(element).text().trim()).toBe("1");

      models.locked.selected = 1;
      digest();
      expect(badge(element).text().trim()).toBe("2");

      models.state.selected = "1";
      models.locked.selected = 0;
      digest();
      expect(badge(element).hasClass("ng-hide")).toBe(true);
    });
  });

  describe("filters in effect (S5, S6, S7)", () => {
    it("lists every search criterion that differs from its default in the row, after the toggle", () => {
      const element = compileWindow();

      expect(chipsGroup(element).parent().is(row(element))).toBe(true);
      expect(chipsGroup(element).prev().is(hook(TestIds.windowAdvancedToggle))).toBe(true);
      expect(chipsGroup(element).hasClass("ng-hide")).toBe(false);
      expect(chipsGroup(element).attr("data-testid-owner")).toBe("w1");
      expect(chipsGroup(element).find(".window-filter-chips-title").text()).toContain("SCREEN_TEXT_FILTERED_BY");
      expect(chipsGroup(element).find("button").last().is(hook(TestIds.windowFilterClearAll))).toBe(true);
      expect(chipTexts(element)).toEqual([expect.stringContaining("Profile: Operator")]);
      expect(chips(element).attr("data-filter-id")).toBe("profile");

      models.state.selected = "0";
      models.locked.selected = 1;
      models.profile.selected = ["op", "ad"];
      digest();

      const texts = chipTexts(element);
      expect(texts[0]).toContain("Profile: Operator +1");
      expect(texts[1]).toContain("State: Inactive");
      // A flag is shown by its label only
      expect(texts[2]).toContain("Locked");
      expect(texts[2]).not.toContain(":");
    });

    it("marks an advanced filter that is hidden, for the eye and for screen readers", () => {
      const element = compileWindow();
      models.state.selected = "0";
      digest();
      const stateChip = () => chips(element).eq(1);

      expect(stateChip().hasClass("window-filter-chip-hidden")).toBe(true);
      expect(stateChip().find("svg use").first().attr("href")).toContain("#eye-off");
      expect(stateChip().find(".sr-only").text()).toContain("SCREEN_TEXT_HIDDEN_ADVANCED_FILTER");
      expect(chips(element).eq(0).hasClass("window-filter-chip-hidden")).toBe(false);

      toggle(element).trigger("click");
      expect(stateChip().hasClass("window-filter-chip-hidden")).toBe(false);
      expect(stateChip().find(".sr-only").length).toBe(0);
    });

    it("shows the filters in effect only when there are some, and keeps the toggle", () => {
      const element = compileWindow();
      models.profile.selected = null;
      digest();

      expect(chipsGroup(element).hasClass("ng-hide")).toBe(true);
      expect(chips(element).length).toBe(0);
      expect(toggle(element).closest(".ng-hide").length).toBe(0);
    });

    it("restores the criterion of a chip to its default when the chip is removed", () => {
      const element = compileWindow();
      models.state.selected = "0";
      digest();
      const remove = chips(element).eq(1).find(hook(TestIds.windowFilterChipRemove));

      expect(remove.attr("aria-label")).toBe("SCREEN_TEXT_REMOVE_FILTER State");
      remove.trigger("click");

      expect(restored).toEqual([{id: "state", view: "base"}]);
      expect(chipTexts(element)).toEqual([expect.stringContaining("Profile: Operator")]);
    });

    it("restores every criterion of the window with Clear all, and the badge goes back to 0", () => {
      const element = compileWindow();
      models.state.selected = "0";
      digest();

      element.find(hook(TestIds.windowFilterClearAll)).trigger("click");

      expect(restored.map(({id}) => id).sort()).toEqual(["locked", "profile", "state", "user"]);
      expect(chips(element).length).toBe(0);
      expect(badge(element).hasClass("ng-hide")).toBe(true);
    });
  });

  describe("side panel mode (S2, S6)", () => {
    let own, other;

    const compilePanelWindow = () => {
      const element = compileWindow({advancedMode: "side-panel"},
        `${ESSENTIAL}${BLOCK}</div><div class='panel-footer'></div>`);
      other = addSubmit(button => button.prependTo($("form").first()));
      own = addSubmit(button => button.appendTo(element.find(".panel-footer")));
      return element;
    };
    const open = element => {
      toggle(element)[0].focus();
      toggle(element).trigger("click");
      digest();
    };

    it("keeps the row in the window and moves the region, compiled, into a side panel titled with the window label", () => {
      const element = compilePanelWindow();

      expect(row(element).length).toBe(1);
      expect(block(element).children(".window-advanced-region").length).toBe(0);
      expect(panel().length).toBe(1);
      expect(panel().closest(".awe-panel-content").length).toBe(0);
      expect(panel().attr("data-testid-owner")).toBe("w1");
      expect(panel().attr("id")).toBe("w1-advanced-panel");
      expect(panel().find(".modal-title").text()).toContain("Users");
      expect(panel().find(".modal-dialog")[0].style.maxWidth).toBe("39em");
      expect(panel().find(`.window-advanced-region ${criterion("state")}`).length).toBe(1);
      expect(panel().find(criterion("locked")).length).toBe(1);
      expect(panel().find(criterion("user")).length).toBe(0);
      expect(panel().find(criterion("state")).isolateScope()).toBeDefined();
      expect(region().hasClass(COLLAPSED)).toBe(false);
      expect(toggle(element).text()).toContain("SCREEN_TEXT_ADVANCED_FILTERS");
      expect(toggle(element).text()).not.toContain("SCREEN_TEXT_ALL_FILTERS");
      expect(toggle(element).attr("aria-expanded")).toBeUndefined();
      expect(toggle(element).attr("aria-haspopup")).toBe("dialog");
      expect(toggle(element).attr("aria-controls")).toBe("w1-advanced-panel");
      expect(toggle(element).find(".window-advanced-chevron").length).toBe(0);
    });

    it("opens the panel with the Advanced filters button", () => {
      const element = compilePanelWindow();

      open(element);

      expect(panel().attr("data-open")).toBe("true");
    });

    it("restores the values of the advanced criteria of when it was opened with Cancel, and gives the focus back", () => {
      const element = compilePanelWindow();
      open(element);
      models.state.selected = "0";
      digest();

      panel().find(hook(TestIds.windowAdvancedCancel)).trigger("click");
      digest();

      expect(models.state.selected).toBe("1");
      expect(panel().attr("data-open")).toBe("false");
      expect(document.activeElement).toBe(toggle(element)[0]);
      expect(own).not.toHaveBeenCalled();
    });

    it("restores the values of when it was opened with its close button, as Cancel", () => {
      const element = compilePanelWindow();
      open(element);
      models.locked.selected = 1;
      digest();

      panel().find(".modal-header .close").trigger("click");
      digest();

      expect(models.locked.selected).toBe(0);
      expect(panel().attr("data-open")).toBe("false");
      expect(document.activeElement).toBe(toggle(element)[0]);
    });

    it("restores the advanced criteria (only them) to their defaults with Reset to defaults", () => {
      const element = compilePanelWindow();
      open(element);
      models.state.selected = "0";
      digest();

      panel().find(hook(TestIds.windowAdvancedReset)).trigger("click");
      digest();

      expect(restored.map(({id}) => id).sort()).toEqual(["locked", "state"]);
      expect(badge(element).hasClass("ng-hide")).toBe(true);
    });

    it("closes and launches the submit button of its own window with Show results, keeping the values", () => {
      const element = compilePanelWindow();
      open(element);
      models.state.selected = "0";
      digest();

      panel().find(hook(TestIds.windowAdvancedApply)).trigger("click");
      digest();

      expect(panel().attr("data-open")).toBe("false");
      expect(own).toHaveBeenCalledTimes(1);
      expect(other).not.toHaveBeenCalled();
      expect(models.state.selected).toBe("0");
    });

    it("launches the submit button of its own window with Enter in a criterion of the panel or of the window", () => {
      const element = compilePanelWindow();

      submitForm(panel().find(criterion("state")));
      submitForm(element.find(criterion("user")));

      expect(own).toHaveBeenCalledTimes(2);
      expect(other).not.toHaveBeenCalled();
    });

    it("removes the panel with the window", () => {
      const element = compilePanelWindow();

      element.scope().$destroy();

      expect(panel().length).toBe(0);
    });
  });

  describe("screen load (S8)", () => {
    // The screen sends the block as a component: its view waits for it before it starts the dependencies of its criteria
    const initializedBlocks = () => refs.$utilities.emitFromScope.mock.calls
      .filter(([channel]) => channel.startsWith("component-initialized-"))
      .map(([, parameters]) => parameters.address.component);

    it("tells its view that the block is initialized, so the dependencies of the screen start", () => {
      jest.spyOn(refs.$utilities, "emitFromScope");

      compileWindow();

      expect(initializedBlocks()).toContain("adv1");
    });

    it("tells it too out of a window with an advanced search", () => {
      jest.spyOn(refs.$utilities, "emitFromScope");

      compileWindow({advancedMode: undefined, advancedCriteria: undefined, searchCriteria: undefined});

      expect(initializedBlocks()).toContain("adv1");
    });
  });

  describe("validation (S8)", () => {
    const invalidate = (container, id) => {
      container.find(criterion(id)).isolateScope().$emit("criterion-validity", {id, invalid: true});
      digest();
    };

    it("shows the advanced criteria when a hidden one is invalid (inline)", () => {
      const element = compileWindow();

      invalidate(element, "state");

      expect(toggle(element).attr("aria-expanded")).toBe("true");
      expect(region().hasClass(COLLAPSED)).toBe(false);
    });

    it("does not show them for an invalid essential criterion", () => {
      const element = compileWindow();

      invalidate(element, "user");

      expect(toggle(element).attr("aria-expanded")).toBe("false");
    });

    it("opens the side panel when a criterion in it is invalid", () => {
      compileWindow({advancedMode: "side-panel"});

      invalidate(panel(), "state");

      expect(panel().attr("data-open")).toBe("true");
    });
  });
});
