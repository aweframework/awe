// Default search and layout of the saved searches of a window (#915): a saved search may keep the advanced criteria in
// use in the main row and may be the default search of the user or of a profile, which the server applies before the
// initial load; the restores of the window return to it, Clear all to the values of the XML
import {cleanupHeavyComponentTest} from "./heavyComponentTestUtils";
import {compileSavedSearchWindow, hook, initSavedSearchTest, SAVED_SEARCHES_BODY, savedSearchControllers, savedSearchModels,
  serviceCall, text} from "./windowSearchTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";

const criterion = id => `[criterion-id='${id}']`;
// The criterion locked inside a row of the block: an advanced criterion that is not a direct child of the block
const NESTED_BODY = SAVED_SEARCHES_BODY.replace(/(<div test-criterion criterion-id='locked'.*?<\/div>)/, "<div class='row'>$1</div>");
// The default search the server applied on load: its values are already in the models
const DEFAULT_SEARCH = Object.freeze({id: 1, name: "Inactive here", values: {state: [{value: "0", label: "Inactive"}], tags: [{value: "z", label: "Zeta"}]},
  layout: ["state"]});

describe("awe-framework/awe-client-angular/src/test/jest/components/windowDefaultSearch.test.js", () => {
  let refs, controllers, models, restored, searches, profiles;

  beforeEach(() => {
    restored = [];
    refs = initSavedSearchTest((id, view) => {
      restored.push({id, view});
      models[id].selected = models[id].defaultValues;
    });
    searches = [
      {id: 1, name: "Inactive here", values: JSON.stringify({state: [{value: "0", label: "Inactive"}], $layout: ["state"]}), profile: "", own: 1, default: 1, profileDefault: 0},
      {id: 2, name: "Admins", values: JSON.stringify({profile: [{value: "ad", label: "Admin"}]}), profile: "", own: 1, default: 0, profileDefault: 0},
      {id: 3, name: "To review", values: JSON.stringify({locked: [{value: 1, label: 1}]}), profile: "OPE", own: 0, default: 0, profileDefault: 1}
    ];
    profiles = [];
  });

  afterEach(() => {
    $(".modal").each((index, modal) => disposeModal(modal));
    $(document.body).removeClass("modal-open");
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  /**
   * Compile the window
   * @param {Object} extra Attributes of the window controller (saved searches on)
   * @param {Object} [options] Body of the window and changes of the models before it is compiled
   * @returns {Object} Window element
   */
  const compileWindow = (extra = {}, {body = NESTED_BODY, change = () => undefined} = {}) => {
    controllers = savedSearchControllers({savedSearches: true, ...extra});
    models = savedSearchModels();
    change(models);
    return compileSavedSearchWindow(refs, {controllers, models, body, server: {searches, profiles, answers: {}}});
  };
  // The window opened with its default search: the server already gave its values to the criteria
  const compileWithDefault = (extra = {}) => compileWindow({defaultSearch: _.cloneDeep(DEFAULT_SEARCH), ...extra}, {change: changed => {
    changed.state.selected = "0";
  }});

  const digest = () => refs.$rootScope.$digest();
  const block = element => element.find(".window-advanced");
  const region = () => $(".window-advanced-region");
  const button = element => element.find(hook(TestIds.windowSavedSearches));
  const menu = element => element.find(hook(TestIds.windowSavedSearchesMenu));
  const item = (element, id) => menu(element).find(`${hook(TestIds.windowSavedSearchItem)}[data-saved-search-id='${id}']`);
  const badge = element => element.find(hook(TestIds.windowAdvancedCount));
  const chip = (element, id) => element.find(`${hook(TestIds.windowFilterChip)}[data-filter-id='${id}']`);
  const applied = element => element.find(hook(TestIds.windowSavedSearchAppliedDefault));
  const saveDialog = () => $(hook(TestIds.windowSavedSearchDialog));
  const manageDialog = () => $(hook(TestIds.windowSavedSearchManageDialog));
  const row = id => manageDialog().find(`${hook(TestIds.windowSavedSearchRow)}[data-saved-search-id='${id}']`);
  const posts = () => refs.connection.post.mock.calls.map(([url]) => url.split("/").pop());
  const call = (target, index) => serviceCall(refs.actionController, target, index);
  const parametersOf = (target, index) => call(target, index)[0].attr("parameters");
  const succeed = actions => {
    actions[1].launchAction();
    digest();
  };
  const open = element => {
    button(element).trigger("click");
    digest();
  };
  const choose = (element, id) => {
    open(element);
    item(element, id).trigger("click");
    digest();
  };
  const addSubmit = element => {
    const onClick = jest.fn();
    const submit = $("<button type='submit'>Search</button>").appendTo(element.find(".panel-footer"));
    submit[0].getClientRects = () => [{}];
    const buttonScope = refs.$rootScope.$new();
    buttonScope.onClick = onClick;
    submit.data("$scope", buttonScope);
    return onClick;
  };
  // The criterion is in the main row: after the essential criteria, out of the block
  const inMainRow = (element, id) => {
    const node = element.find(criterion(id));
    return node.length === 1 && node.closest(".window-advanced").length === 0 && node.nextAll(".window-advanced").length === 1;
  };
  const inBlock = id => region().find(criterion(id)).length === 1;

  describe("the Save search dialog (S1, S2)", () => {
    const openSave = element => {
      open(element);
      menu(element).find(hook(TestIds.windowSavedSearchSave)).trigger("click");
      digest();
    };
    const option = testId => saveDialog().find(hook(testId));
    const accept = name => {
      saveDialog().find(hook(TestIds.windowSavedSearchName)).val(name).trigger("input");
      saveDialog().find(hook(TestIds.windowSavedSearchAccept)).trigger("click");
      digest();
    };
    const savedValues = () => JSON.parse(parametersOf("saveWindowSearch").savedSearchValues);

    it("keeps the layout only when asked: the direct children in use and the ones in the main row", () => {
      const element = compileWindow({defaultSearch: {id: 9, name: "Tags", values: {}, layout: ["tags"]}});
      models.state.selected = "0";
      models.locked.selected = 1;
      openSave(element);

      // Opt-in: a plain save keeps the values only, as before #915
      expect(option(TestIds.windowSavedSearchKeepLayout).prop("checked")).toBe(false);
      expect(option(TestIds.windowSavedSearchKeepLayout).attr("data-selected")).toBe("false");
      expect(option(TestIds.windowSavedSearchKeepLayout).attr("data-testid-owner")).toBe("w1");
      // Its help names the criteria it keeps
      expect(text(option(TestIds.windowSavedSearchKeepLayout).closest(".window-saved-search-check").find(".window-saved-search-help")))
        .toBe("Tags, State. SCREEN_TEXT_SAVED_SEARCH_KEEP_LAYOUT_HELP");
      expect(text(option(TestIds.windowSavedSearchKeepLayout).closest("label"))).toContain("SCREEN_TEXT_SAVED_SEARCH_KEEP_LAYOUT");
      expect(option(TestIds.windowSavedSearchKeepValues).prop("checked")).toBe(true);
      expect(option(TestIds.windowSavedSearchOpenWith).prop("checked")).toBe(false);
      option(TestIds.windowSavedSearchKeepLayout).trigger("click");
      digest();
      expect(option(TestIds.windowSavedSearchKeepLayout).attr("data-selected")).toBe("true");
      accept("Mine");

      // locked is in use but inside a row of the block: it cannot move to the main row
      expect(savedValues().$layout).toEqual(["tags", "state"]);
      expect(savedValues().state).toEqual([{value: "0", label: "Inactive"}]);
      succeed(call("saveWindowSearch"));
      expect(call("setWindowDefaultSearch")).toBeUndefined();
    });

    it("starts without the layout when no advanced criterion is in use, and then saves no layout", () => {
      const element = compileWindow();
      openSave(element);

      expect(option(TestIds.windowSavedSearchKeepLayout).prop("checked")).toBe(false);
      accept("Mine");

      expect(savedValues()).not.toHaveProperty("$layout");
      expect(savedValues().user).toEqual([{value: "jo", label: "jo"}]);
    });

    it("saves no value of the criteria when Save the values is unchecked", () => {
      const element = compileWindow();
      models.state.selected = "0";
      openSave(element);

      option(TestIds.windowSavedSearchKeepLayout).trigger("click");
      option(TestIds.windowSavedSearchKeepValues).trigger("click");
      digest();
      expect(option(TestIds.windowSavedSearchKeepValues).attr("data-selected")).toBe("false");
      accept("Only the layout");

      expect(savedValues()).toEqual({$layout: ["state"]});
    });

    it("makes the saved search the default of the user with Open the screen with this search", () => {
      const element = compileWindow();
      openSave(element);
      option(TestIds.windowSavedSearchOpenWith).trigger("click");
      digest();
      accept("Mine");
      searches.push({id: 7, name: "Mine", values: "{}", profile: "", own: 1, default: 0, profileDefault: 0});

      succeed(call("saveWindowSearch"));

      expect(parametersOf("setWindowDefaultSearch")).toEqual(expect.objectContaining({
        serverAction: "maintain-silent", savedSearchScreen: "UsrLst", savedSearchWindow: "w1", savedSearchId: 7}));
      succeed(call("setWindowDefaultSearch"));
      expect(posts().filter(target => target === "getWindowSavedSearches").length).toBe(3);
    });
  });

  describe("the menu and the Manage searches dialog (S2, S3)", () => {
    const openManage = element => {
      open(element);
      menu(element).find(hook(TestIds.windowSavedSearchManage)).trigger("click");
      digest();
    };

    it("tags the default search of the user and the default of a profile with a text", () => {
      const element = compileWindow();

      open(element);

      expect(text(item(element, 1).find(hook(TestIds.windowSavedSearchDefaultTag)))).toBe("SCREEN_TEXT_SAVED_SEARCH_DEFAULT");
      expect(text(item(element, 3).find(hook(TestIds.windowSavedSearchProfileDefaultTag)))).toBe("SCREEN_TEXT_SAVED_SEARCH_PROFILE_DEFAULT");
      expect(item(element, 2).find(hook(TestIds.windowSavedSearchDefaultTag)).length).toBe(0);
      expect(item(element, 1).find(hook(TestIds.windowSavedSearchProfileDefaultTag)).length).toBe(0);
    });

    it("sets a search as the default of the user and stops using it, and reloads the searches", () => {
      const element = compileWindow();
      openManage(element);
      const setDefault = id => row(id).find(hook(TestIds.windowSavedSearchSetDefault));

      // A user who may not publish has no default of a profile to change
      expect(row(3).find(hook(TestIds.windowSavedSearchSetProfileDefault)).length).toBe(0);
      expect(setDefault(2).attr("aria-label")).toBe("SCREEN_TEXT_SAVED_SEARCH_SET_DEFAULT Admins");
      expect(setDefault(2).attr("data-selected")).toBe("false");
      expect(setDefault(1).attr("data-selected")).toBe("true");
      expect(setDefault(1).attr("aria-label")).toBe("SCREEN_TEXT_SAVED_SEARCH_UNSET_DEFAULT Inactive here");
      setDefault(2).trigger("click");
      digest();
      expect(parametersOf("setWindowDefaultSearch")).toEqual(expect.objectContaining({
        savedSearchScreen: "UsrLst", savedSearchWindow: "w1", savedSearchId: 2}));
      succeed(call("setWindowDefaultSearch"));
      expect(posts().filter(target => target === "getWindowSavedSearches").length).toBe(2);

      setDefault(1).trigger("click");
      digest();
      expect(parametersOf("setWindowDefaultSearch", 1).savedSearchId).toBe("");
    });

    it("lets a user who may publish set and unset the default of a profile, on the published searches", () => {
      profiles = [{value: "OPE", label: "Operator"}];
      const element = compileWindow();
      openManage(element);
      const setProfileDefault = id => row(id).find(hook(TestIds.windowSavedSearchSetProfileDefault));

      expect(setProfileDefault(1).length).toBe(0);
      expect(setProfileDefault(3).attr("aria-label")).toBe("SCREEN_TEXT_SAVED_SEARCH_UNSET_PROFILE_DEFAULT To review");
      expect(setProfileDefault(3).attr("data-selected")).toBe("true");
      setProfileDefault(3).trigger("click");
      digest();
      expect(parametersOf("setProfileDefaultSearch")).toEqual(expect.objectContaining({
        savedSearchScreen: "UsrLst", savedSearchWindow: "w1", savedSearchId: 3, savedSearchDefault: "0"}));
    });
  });

  describe("the layout of a saved search (S1, S5)", () => {
    it("shows its criteria in the main row, after the essential criteria, where they count as essential", () => {
      const element = compileWindow();
      addSubmit(element);

      choose(element, 1);

      expect(models.state.selected).toBe("0");
      expect(inBlock("state")).toBe(false);
      expect(element.find(criterion("state")).prev().is(criterion("customer"))).toBe(true);
      expect(element.find(criterion("state")).next().is(block(element))).toBe(true);
      // Its value differs from the default, but it is not counted as an advanced filter nor marked as hidden
      expect(badge(element).hasClass("ng-hide")).toBe(true);
      expect(chip(element, "state").hasClass("window-filter-chip-hidden")).toBe(false);
    });

    it("goes back to the layout of the XML with a search without a layout", () => {
      const element = compileWindow();
      addSubmit(element);
      choose(element, 1);

      choose(element, 2);

      expect(region().children().first().is(criterion("state"))).toBe(true);
      expect(inMainRow(element, "state")).toBe(false);
    });

    it("moves only the direct children of the block and ignores the criteria the window no longer has", () => {
      searches.push({id: 4, name: "Old", values: JSON.stringify({ghost: [{value: "1", label: "1"}], $layout: ["ghost", "locked", "tags"]}), profile: "", own: 1});
      const element = compileWindow();
      addSubmit(element);

      choose(element, 4);

      expect(inMainRow(element, "tags")).toBe(true);
      expect(inBlock("locked")).toBe(true);
      expect(models.ghost).toBeUndefined();
    });

    it("keeps the criteria of the main row out of the side panel", () => {
      const element = compileWindow({advancedMode: "side-panel"});
      addSubmit(element);

      choose(element, 1);

      expect($(hook(TestIds.windowAdvancedPanel)).find(criterion("state")).length).toBe(0);
      expect(element.find(criterion("state")).length).toBe(1);
    });

    it("does not open the block for an invalid criterion of the main row", () => {
      const element = compileWindow();
      addSubmit(element);
      choose(element, 1);

      element.find(criterion("state")).isolateScope().$emit("criterion-validity", {id: "state", invalid: true});
      digest();

      expect(region().hasClass("window-advanced-collapsed")).toBe(true);
    });
  });

  describe("the default search applied on load (S2, S7)", () => {
    it("applies its layout and names it in a chip, without applying its values again nor launching the search", () => {
      const element = compileWithDefault();
      const submit = addSubmit(element);
      digest();

      expect(inMainRow(element, "state")).toBe(true);
      expect(applied(element).attr("data-testid-owner")).toBe("w1");
      expect(text(applied(element))).toBe("SCREEN_TEXT_SAVED_SEARCH_APPLIED_DEFAULT: Inactive here");
      expect(applied(element).find(".sr-only").length).toBe(1);
      expect(applied(element).find("svg use").attr("href")).toContain("#bookmark");
      // Next to the Saved searches button
      expect(applied(element).prev().find(hook(TestIds.windowSavedSearches)).length).toBe(1);
      // The models keep the values the server gave them (tags of the default search lost to a context variable)
      expect(models.tags.selected).toEqual([]);
      expect(restored).toEqual([]);
      expect(submit).not.toHaveBeenCalled();
      expect(refs.connection.post).not.toHaveBeenCalled();
    });

    it("forgets the chip when another search is applied", () => {
      const element = compileWithDefault();
      addSubmit(element);

      choose(element, 2);

      expect(applied(element).length).toBe(0);
    });

    it("is where the restore buttons of the window return: its values and its layout", () => {
      const element = compileWithDefault();
      addSubmit(element);
      choose(element, 2);

      // The restore action of a button of the window
      refs.$rootScope.$broadcast("restore-scope", "base");
      digest();

      expect(models.profile.selected).toBe(null);
      expect(models.state.selected).toBe("0");
      expect(models.tags.selected).toEqual(["z"]);
      expect(inMainRow(element, "state")).toBe(true);
      expect(text(applied(element))).toContain("Inactive here");
    });

    it("is where Reset to defaults of the side panel returns the advanced criteria", () => {
      const element = compileWithDefault({advancedMode: "side-panel"});
      models.locked.selected = 1;

      $(hook(TestIds.windowAdvancedReset)).trigger("click");
      digest();

      expect(models.locked.selected).toBe(0);
      expect(models.state.selected).toBe("0");
      expect(models.tags.selected).toEqual(["z"]);
      expect(models.user.selected).toBe("jo");
      expect(inMainRow(element, "state")).toBe(true);
    });

    it("is left by Clear all: the values of the XML, the layout as it is and no chip", () => {
      const element = compileWithDefault();

      element.find(hook(TestIds.windowFilterClearAll)).trigger("click");
      digest();

      expect(models.state.selected).toBe("1");
      expect(inMainRow(element, "state")).toBe(true);
      expect(applied(element).length).toBe(0);
    });

    it("ignores the criteria of its layout the window no longer has", () => {
      const element = compileWindow({defaultSearch: {id: 1, name: "Old", values: {}, layout: ["ghost", "state"]}});

      expect(inMainRow(element, "state")).toBe(true);
      expect(text(applied(element))).toContain("Old");
    });
  });

  it("does nothing with a default search in a window without saved searches (S6)", () => {
    const element = compileWindow({savedSearches: undefined, defaultSearch: _.cloneDeep(DEFAULT_SEARCH)});

    expect(inBlock("state")).toBe(true);
    expect(applied(element).length).toBe(0);
    expect(button(element).length).toBe(0);
  });
});
