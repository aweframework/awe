// Saved searches of a window (#790, delivery 3): the Saved searches menu of the row of the advanced block, with the
// searches of the user and the ones published for their profile, and its Save and Manage dialogs
import {cleanupHeavyComponentTest, initHeavyComponentTest, stubPanelComponent} from "./heavyComponentTestUtils";
import {TestIds} from "../../../main/resources/js/awe/data/testIds";
import {disposeModal} from "../../../main/resources/js/awe/services/modal";

const hook = id => `[data-testid='${id}']`;

describe("awe-framework/awe-client-angular/src/test/jest/components/windowSavedSearches.test.js", () => {
  let refs, controllers, models, restored, actionController, connection, $q, searches, profiles, answers;

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
    inject(["ActionController", "Connection", "$q", (_ActionController_, _Connection_, _$q_) => {
      actionController = _ActionController_;
      connection = _Connection_;
      $q = _$q_;
    }]);
    searches = [
      {id: 1, name: "Admins", values: JSON.stringify({profile: [{value: "ad", label: "Admin"}], state: [{value: "0", label: "Inactive"}]}), profile: "", own: 1},
      {id: 2, name: "Big customers", values: JSON.stringify({customer: [{value: "77", label: "Acme"}], tags: [{value: "a", label: "Alpha"}, {value: "z", label: "Zeta"}]}), profile: "", own: 1},
      {id: 3, name: "To review", values: JSON.stringify({locked: [{value: 1, label: 1}]}), profile: "OPE", own: 0}
    ];
    profiles = [];
    // Answers of the server that replace the rows of a query (failures)
    answers = {};
  });

  afterEach(() => {
    $(".modal").each((index, modal) => disposeModal(modal));
    $(document.body).removeClass("modal-open");
    cleanupHeavyComponentTest();
    document.body.innerHTML = "";
  });

  const BODY = `<div class='panel-body'>
      <div test-criterion criterion-id='user' class='criterion'><input id='user'></div>
      <div test-criterion criterion-id='profile' class='criterion'><input id='profile'></div>
      <div test-criterion criterion-id='customer' class='criterion'><input id='customer'></div>
      <awe-advanced advanced-id='adv1'>
        <div test-criterion criterion-id='state' class='criterion'><input id='state'></div>
        <div test-criterion criterion-id='tags' class='criterion'><input id='tags'></div>
        <div test-criterion criterion-id='locked' class='criterion'><input id='locked' type='checkbox'></div>
      </awe-advanced>
    </div><div class='panel-footer'></div>`;

  const compileWindow = (extra = {savedSearches: true}) => {
    controllers = {
      w1: {
        id: "w1", component: "window", label: "Users", advancedMode: "inline",
        advancedCriteria: ["state", "tags", "locked"],
        searchCriteria: ["user", "profile", "customer", "state", "tags", "locked"],
        ...extra
      },
      user: {id: "user", component: "text", label: "Name"},
      profile: {id: "profile", component: "select", label: "Profile"},
      customer: {id: "customer", component: "suggest", label: "Customer"},
      state: {id: "state", component: "select", label: "State"},
      tags: {id: "tags", component: "select-multiple", label: "Tags"},
      locked: {id: "locked", component: "checkbox", label: "Locked"}
    };
    models = {
      w1: {selected: null, values: []},
      user: {selected: "jo", defaultValues: null, values: []},
      profile: {selected: "op", defaultValues: null, values: [{value: "op", label: "Operator"}, {value: "ad", label: "Admin"}]},
      customer: {selected: null, defaultValues: null, values: []},
      state: {selected: "1", defaultValues: "1", values: [{value: "1", label: "Active"}, {value: "0", label: "Inactive"}]},
      tags: {selected: [], defaultValues: [], values: [{value: "a", label: "Alpha"}]},
      locked: {selected: 0, defaultValues: 0, values: []}
    };
    stubPanelComponent(refs, {}, controllers.w1);
    refs.$storage.get.mockReturnValue({base: {name: "UsrLst"}});
    jest.spyOn(actionController, "addActionList").mockImplementation(() => undefined);
    jest.spyOn(connection, "post").mockImplementation(url => {
      const target = url.split("/").pop();
      if (answers[target]) {
        return answers[target]();
      }
      return $q.resolve({
        data: [{type: "fill", parameters: {datalist: {rows: target === "getSavedSearchProfiles" ? profiles : searches}}}, {type: "end-load"}]
      });
    });
    refs.$control.getAddressController.mockImplementation(address => controllers[address.component]);
    refs.$control.getAddressModel.mockImplementation(address => models[address.component]);
    const form = $("<form></form>").appendTo(document.body);
    const element = refs.$compile(`<awe-window window-id='w1'>${BODY}</awe-window>`)(refs.$rootScope.$new());
    form.append(element);
    refs.$rootScope.$digest();
    return element;
  };

  const digest = () => refs.$rootScope.$digest();
  const button = element => element.find(hook(TestIds.windowSavedSearches));
  const menu = element => element.find(hook(TestIds.windowSavedSearchesMenu));
  const items = element => menu(element).find(hook(TestIds.windowSavedSearchItem));
  const menuItems = element => menu(element).find("[role='menuitem']");
  const saveDialog = () => $(hook(TestIds.windowSavedSearchDialog));
  const manageDialog = () => $(hook(TestIds.windowSavedSearchManageDialog));
  const rows = () => manageDialog().find(hook(TestIds.windowSavedSearchRow));
  const row = id => manageDialog().find(`${hook(TestIds.windowSavedSearchRow)}[data-saved-search-id='${id}']`);
  const text = node => $(node).text().replace(/\s+/g, " ").trim();
  const posts = () => connection.post.mock.calls.map(([url, data]) => ({target: url.split("/").pop(), data}));
  const actionLists = () => actionController.addActionList.mock.calls.map(([actions]) => actions);
  const keydown = (node, key) => {
    const event = $.Event("keydown", {key});
    $(node).trigger(event);
    digest();
    return event;
  };
  const open = element => {
    button(element)[0].focus();
    button(element).trigger("click");
    digest();
  };
  // A submit button of the window, with the onClick of the button directive, visible for jQuery
  const addSubmit = element => {
    const onClick = jest.fn();
    const submit = $("<button type='submit'>Search</button>").appendTo(element.find(".panel-footer"));
    submit[0].getClientRects = () => [{}];
    const buttonScope = refs.$rootScope.$new();
    buttonScope.onClick = onClick;
    submit.data("$scope", buttonScope);
    return onClick;
  };
  // The service of an action list launched by launchSilentService: its server action and the action that tells it worked
  const serviceCall = target => actionLists().find(actions => actions[0].attr && actions[0].attr("parameters")?.targetAction === target);
  const succeed = actions => {
    actions[1].launchAction();
    digest();
  };

  describe("opt-in (S6)", () => {
    it("draws no Saved searches button for a window that does not opt in", () => {
      const element = compileWindow({});

      expect(button(element).length).toBe(0);
      expect(menu(element).length).toBe(0);
      expect(saveDialog().length).toBe(0);
      expect(manageDialog().length).toBe(0);
      expect(connection.post).not.toHaveBeenCalled();
    });

    it("draws no button for savedSearches other than true", () => {
      const element = compileWindow({savedSearches: "true"});

      expect(button(element).length).toBe(0);
    });
  });

  describe("the Saved searches button (S8)", () => {
    it("is a text button with the bookmark icon and a chevron right after the Advanced filters toggle, before the chips", () => {
      const element = compileWindow();
      const rowChildren = element.find(".window-advanced-row").children();

      expect(rowChildren.eq(0).is(hook(TestIds.windowAdvancedToggle))).toBe(true);
      expect(rowChildren.eq(1).find(hook(TestIds.windowSavedSearches)).length).toBe(1);
      expect(rowChildren.eq(2).is(hook(TestIds.windowFilterChips))).toBe(true);
      expect(button(element).attr("data-testid-owner")).toBe("w1");
      expect(button(element).attr("type")).toBe("button");
      expect(button(element).attr("aria-haspopup")).toBe("menu");
      expect(button(element).attr("aria-expanded")).toBe("false");
      expect(button(element).attr("aria-controls")).toBe("w1-saved-searches-menu");
      expect(button(element).text()).toContain("SCREEN_TEXT_SAVED_SEARCHES");
      expect(button(element).find("svg use").first().attr("href")).toContain("#bookmark");
      expect(button(element).find("svg use").last().attr("href")).toContain("#chevron-down");
      expect(menu(element).attr("hidden")).toBe("hidden");
      expect(connection.post).not.toHaveBeenCalled();
    });
  });

  describe("the menu (S2, S3, S8)", () => {
    it("loads the searches of the screen and window and the profiles the user may publish for when it opens", () => {
      const element = compileWindow();

      open(element);

      expect(posts()).toEqual([
        {target: "getWindowSavedSearches", data: expect.objectContaining({
          serverAction: "data", targetAction: "getWindowSavedSearches", savedSearchScreen: "UsrLst", savedSearchWindow: "w1"})},
        {target: "getSavedSearchProfiles", data: expect.objectContaining({serverAction: "data", targetAction: "getSavedSearchProfiles"})}
      ]);
      expect(posts()[0].data.user).toBeUndefined();
      expect(button(element).attr("aria-expanded")).toBe("true");
      expect(menu(element).attr("hidden")).toBeUndefined();
      expect(menu(element).attr("role")).toBe("menu");
      expect(menu(element).attr("id")).toBe("w1-saved-searches-menu");
      expect(menu(element).attr("data-testid-owner")).toBe("w1");
    });

    it("reloads the searches every time it opens, and the profiles only once", () => {
      const element = compileWindow();
      open(element);
      keydown(menuItems(element)[0], "Escape");

      open(element);

      expect(posts().map(({target}) => target)).toEqual(["getWindowSavedSearches", "getSavedSearchProfiles", "getWindowSavedSearches"]);
    });

    it("lists My searches, then Shared with your profile with its profile, then Save current search and Manage searches", () => {
      const element = compileWindow();

      open(element);

      expect(menu(element).find(".window-saved-searches-head").map((i, head) => text(head)).get())
        .toEqual(["SCREEN_TEXT_MY_SEARCHES", "SCREEN_TEXT_SHARED_SEARCHES"]);
      expect(items(element).map((i, item) => [[item.getAttribute("data-saved-search-id"), item.getAttribute("data-type"), text(item)]]).get())
        .toEqual([["1", "own", "Admins"], ["2", "own", "Big customers"], ["3", "shared", "To review OPE"]]);
      expect(items(element).eq(2).find(".window-saved-searches-tag").text()).toBe("OPE");
      expect(items(element).eq(0).attr("data-testid-owner")).toBe("w1");
      expect(items(element).eq(0).find("svg use").attr("href")).toContain("#bookmark");
      expect(items(element).eq(2).find("svg use").attr("href")).toContain("#users");
      expect(menuItems(element).map((i, item) => item.getAttribute("data-testid")).get()).toEqual([
        TestIds.windowSavedSearchItem, TestIds.windowSavedSearchItem, TestIds.windowSavedSearchItem,
        TestIds.windowSavedSearchSave, TestIds.windowSavedSearchManage]);
      expect(text(menu(element).find(hook(TestIds.windowSavedSearchSave)))).toBe("SCREEN_TEXT_SAVE_CURRENT_SEARCH");
      expect(text(menu(element).find(hook(TestIds.windowSavedSearchManage)))).toBe("SCREEN_TEXT_MANAGE_SEARCHES");
      expect(menu(element).find("[role='separator']").length).toBe(2);
    });

    it("shows the name of the profile when the user may publish for it", () => {
      profiles = [{value: "ADM", label: "Administrator"}, {value: "OPE", label: "Operator"}];
      const element = compileWindow();

      open(element);

      expect(items(element).eq(2).find(".window-saved-searches-tag").text()).toBe("Operator");
    });

    it("tells that there are no searches of the user, and has no shared group without shared searches", () => {
      searches = [];
      const element = compileWindow();

      open(element);

      expect(items(element).length).toBe(0);
      expect(text(menu(element).find(".window-saved-searches-empty"))).toBe("SCREEN_TEXT_NO_SAVED_SEARCHES");
      expect(menu(element).find(".window-saved-searches-head").map((i, head) => text(head)).get()).toEqual(["SCREEN_TEXT_MY_SEARCHES"]);
      expect(menuItems(element).length).toBe(2);
    });

    it("moves the focus to the first option, and through the options with the arrows, Home and End", () => {
      const element = compileWindow();
      open(element);
      const options = menuItems(element);

      expect(document.activeElement).toBe(options[0]);
      expect(options.map((i, option) => option.getAttribute("tabindex")).get()).toEqual(["-1", "-1", "-1", "-1", "-1"]);
      keydown(options[0], "ArrowDown");
      expect(document.activeElement).toBe(options[1]);
      keydown(options[1], "ArrowUp");
      keydown(options[0], "ArrowUp");
      expect(document.activeElement).toBe(options[4]);
      keydown(options[4], "ArrowDown");
      expect(document.activeElement).toBe(options[0]);
      keydown(options[0], "End");
      expect(document.activeElement).toBe(options[4]);
      keydown(options[4], "Home");
      expect(document.activeElement).toBe(options[0]);
    });

    it("closes with Escape and gives the focus back to the button", () => {
      const element = compileWindow();
      open(element);

      const event = keydown(menuItems(element)[1], "Escape");

      expect(menu(element).attr("hidden")).toBe("hidden");
      expect(button(element).attr("aria-expanded")).toBe("false");
      expect(document.activeElement).toBe(button(element)[0]);
      expect(event.isDefaultPrevented()).toBe(true);
    });

    it("closes with a second click on the button and with a click outside", () => {
      const element = compileWindow();
      open(element);
      button(element).trigger("click");
      digest();
      expect(menu(element).attr("hidden")).toBe("hidden");

      open(element);
      $(document.body).trigger("mousedown");
      digest();
      expect(menu(element).attr("hidden")).toBe("hidden");
    });
  });

  describe("apply a search (S2, S3)", () => {
    it("restores every criterion to its default, sets the saved values and launches the submit of the window", () => {
      const element = compileWindow();
      const submit = addSubmit(element);
      models.tags.selected = ["a"];
      open(element);

      items(element).eq(0).trigger("click");
      digest();

      expect(restored.map(({id}) => id).sort()).toEqual(["customer", "locked", "profile", "state", "tags", "user"]);
      expect(models.user.selected).toBe(null);
      expect(models.profile.selected).toBe("ad");
      expect(models.state.selected).toBe("0");
      expect(models.tags.selected).toEqual([]);
      expect(submit).toHaveBeenCalledTimes(1);
      expect(menu(element).attr("hidden")).toBe("hidden");
      expect(document.activeElement).toBe(button(element)[0]);
    });

    it("keeps the labels of the saved values, so suggests and multiple selects show them", () => {
      const element = compileWindow();
      addSubmit(element);
      open(element);

      items(element).eq(1).trigger("click");
      digest();

      expect(models.customer.selected).toBe("77");
      expect(models.customer.values).toEqual([{value: "77", label: "Acme"}]);
      expect(models.tags.selected).toEqual(["a", "z"]);
      expect(models.tags.values).toEqual([{value: "a", label: "Alpha"}, {value: "z", label: "Zeta"}]);
      expect(models.profile.values.length).toBe(2);
    });

    it("applies a search shared with the profile", () => {
      const element = compileWindow();
      const submit = addSubmit(element);
      open(element);

      items(element).eq(2).trigger("click");
      digest();

      expect(models.locked.selected).toBe(1);
      expect(submit).toHaveBeenCalledTimes(1);
    });

    it("shows the name of the applied search on the button until a criterion value changes", () => {
      const element = compileWindow();
      addSubmit(element);
      open(element);
      items(element).eq(0).trigger("click");
      digest();

      expect(text(button(element))).toBe("Admins");
      expect(items(element).eq(0).attr("aria-current")).toBe("true");

      models.user.selected = "someone";
      digest();

      expect(text(button(element))).toBe("SCREEN_TEXT_SAVED_SEARCHES");
      expect(items(element).eq(0).attr("aria-current")).toBeUndefined();
    });
  });

  describe("save the current search (S2, S3, S8)", () => {
    const openSave = element => {
      open(element);
      menu(element).find(hook(TestIds.windowSavedSearchSave)).trigger("click");
      digest();
    };
    const nameInput = () => saveDialog().find(hook(TestIds.windowSavedSearchName));
    // The profile is the select of the engine (select2 on a hidden input, as the select criteria)
    const profileInput = () => saveDialog().find("input[type='hidden'][ui-select2]");
    const profileSelect = () => saveDialog().find(hook(TestIds.windowSavedSearchProfile));
    const profileOptions = () => {
      profileInput().select2("open");
      const labels = $(".select2-drop-active .select2-result-label").map((i, label) => text(label)).get();
      profileInput().select2("close");
      return labels;
    };
    const chooseProfile = value => {
      profileInput().select2("val", value, true);
      digest();
    };
    const accept = () => {
      saveDialog().find(hook(TestIds.windowSavedSearchAccept)).trigger("click");
      digest();
    };

    it("opens the Save search dialog from the menu, with the focus given back to the button", () => {
      const element = compileWindow();

      openSave(element);

      expect(saveDialog().attr("data-open")).toBe("true");
      expect(saveDialog().attr("data-testid-owner")).toBe("w1");
      expect(saveDialog().attr("role")).toBe("dialog");
      expect(saveDialog().find(".modal-title").text()).toContain("SCREEN_TEXT_SAVE_SEARCH");
      expect(saveDialog().find(".window-saved-search-subtitle").text()).toContain("SCREEN_TEXT_SAVE_SEARCH_SUBTITLE");
      expect(menu(element).attr("hidden")).toBe("hidden");
      expect(nameInput().val()).toBe("");
      expect(nameInput().attr("maxlength")).toBe("100");
      expect(saveDialog().find(`label[for='${nameInput().attr("id")}']`).text()).toContain("SCREEN_TEXT_SEARCH_NAME");
      expect(saveDialog().find(hook(TestIds.windowSavedSearchCancel)).attr("data-dismiss")).toBe("modal");
    });

    it("has no Publish for a profile select when the user may not publish", () => {
      const element = compileWindow();

      openSave(element);

      expect(profileSelect().length).toBe(0);
      expect(profileInput().length).toBe(0);
    });

    it("offers Only me first and the profiles the user may publish for", () => {
      profiles = [{value: "ADM", label: "Administrator"}, {value: "OPE", label: "Operator"}];
      const element = compileWindow();

      openSave(element);

      // No native select popup: the trigger of the select of the engine carries the test id
      expect(saveDialog().find("select").length).toBe(0);
      expect(profileSelect().length).toBe(1);
      expect(profileSelect().hasClass("select2-choice")).toBe(true);
      expect(profileSelect().attr("data-testid-owner")).toBe("w1");
      expect(text(profileSelect().find(".select2-chosen"))).toBe("SCREEN_TEXT_ONLY_ME");
      expect(profileOptions()).toEqual(["SCREEN_TEXT_ONLY_ME", "Administrator", "Operator"]);
      // Its label names the focusable control of the select (select2 moves it there)
      const focusser = saveDialog().find(".select2-focusser");
      expect(saveDialog().find(`label[for='${focusser.attr("id")}']`).text()).toContain("SCREEN_TEXT_PUBLISH_FOR_PROFILE");
      expect(saveDialog().find(".window-saved-search-help").text()).toContain("SCREEN_TEXT_PUBLISH_FOR_PROFILE_HELP");
    });

    it("requires a name: nothing is saved and the field tells it", () => {
      const element = compileWindow();
      openSave(element);
      nameInput().val("   ").trigger("input");

      accept();

      expect(serviceCall("saveWindowSearch")).toBeUndefined();
      expect(nameInput().attr("aria-invalid")).toBe("true");
      expect(saveDialog().find(".window-saved-search-error").text()).toContain("VALIDATOR_MESSAGE_REQUIRED");
      expect(saveDialog().attr("data-open")).toBe("true");
    });

    it("saves the selected values with their labels of every search criterion, for the user only", () => {
      const element = compileWindow();
      models.tags.selected = ["a"];
      openSave(element);
      nameInput().val("Mine").trigger("input");

      accept();

      const [save] = serviceCall("saveWindowSearch");
      const parameters = save.attr("parameters");
      expect(parameters).toEqual(expect.objectContaining({
        serverAction: "maintain-silent",
        targetAction: "saveWindowSearch",
        savedSearchScreen: "UsrLst",
        savedSearchWindow: "w1",
        savedSearchName: "Mine"
      }));
      expect(parameters.savedSearchProfile).toBe("");
      expect(parameters.user).toBeUndefined();
      expect(JSON.parse(parameters.savedSearchValues)).toEqual({
        user: [{value: "jo", label: "jo"}],
        profile: [{value: "op", label: "Operator"}],
        customer: [],
        state: [{value: "1", label: "Active"}],
        tags: [{value: "a", label: "Alpha"}],
        locked: [{value: 0, label: 0}]
      });
    });

    it("publishes for the chosen profile", () => {
      profiles = [{value: "ADM", label: "Administrator"}, {value: "OPE", label: "Operator"}];
      const element = compileWindow();
      openSave(element);
      nameInput().val("For operators").trigger("input");
      chooseProfile("OPE");

      expect(text(profileSelect().find(".select2-chosen"))).toBe("Operator");
      accept();

      expect(serviceCall("saveWindowSearch")[0].attr("parameters")).toEqual(expect.objectContaining({
        savedSearchName: "For operators", savedSearchProfile: "OPE"}));
    });

    it("saves for the user only when Only me is chosen again, and starts with Only me every time", () => {
      profiles = [{value: "OPE", label: "Operator"}];
      const element = compileWindow();
      openSave(element);
      chooseProfile("OPE");
      chooseProfile("@only-me");
      nameInput().val("Mine").trigger("input");
      accept();
      expect(serviceCall("saveWindowSearch")[0].attr("parameters").savedSearchProfile).toBe("");

      saveDialog().find(hook(TestIds.windowSavedSearchCancel)).trigger("click");
      digest();
      chooseProfile("OPE");
      openSave(element);

      expect(text(profileSelect().find(".select2-chosen"))).toBe("SCREEN_TEXT_ONLY_ME");
    });

    it("closes, reloads the searches and names the button after the saved search when the save works", () => {
      const element = compileWindow();
      openSave(element);
      nameInput().val("Mine").trigger("input");
      accept();
      expect(saveDialog().find(hook(TestIds.windowSavedSearchAccept)).attr("disabled")).toBe("disabled");

      succeed(serviceCall("saveWindowSearch"));

      expect(saveDialog().attr("data-open")).toBe("false");
      expect(posts().map(({target}) => target)).toEqual(["getWindowSavedSearches", "getSavedSearchProfiles", "getWindowSavedSearches"]);
      expect(text(button(element))).toBe("Mine");
    });

    it("stays open when the save fails (the server reports the error)", () => {
      const element = compileWindow();
      openSave(element);
      nameInput().val("Mine").trigger("input");
      accept();
      const [save] = serviceCall("saveWindowSearch");

      save.onError({status: 500});
      digest();

      expect(saveDialog().attr("data-open")).toBe("true");
      expect(saveDialog().find(hook(TestIds.windowSavedSearchAccept)).attr("disabled")).toBeUndefined();
      expect(posts().length).toBe(2);
    });

    it("prefills the name of the applied search", () => {
      const element = compileWindow();
      addSubmit(element);
      open(element);
      items(element).eq(1).trigger("click");
      digest();

      openSave(element);

      expect(nameInput().val()).toBe("Big customers");
    });
  });

  describe("manage the searches (S2, S3, S8)", () => {
    const openManage = element => {
      open(element);
      menu(element).find(hook(TestIds.windowSavedSearchManage)).trigger("click");
      digest();
    };

    it("lists the searches with who they are for, and the actions on the own ones only", () => {
      const element = compileWindow();

      openManage(element);

      expect(manageDialog().attr("data-open")).toBe("true");
      expect(manageDialog().attr("data-testid-owner")).toBe("w1");
      expect(text(manageDialog().find(".modal-title"))).toBe("SCREEN_TEXT_MANAGE_SEARCHES_TITLE");
      expect(text(manageDialog().find("thead th").eq(2))).toBe("SCREEN_TEXT_SEARCH_ACTIONS");
      expect(manageDialog().find("thead th").eq(2).find(".sr-only").length).toBe(1);
      expect(rows().map((i, item) => [[item.getAttribute("data-saved-search-id"), text($(item).children().eq(0)), text($(item).children().eq(1))]]).get())
        .toEqual([["1", "Admins", "SCREEN_TEXT_SEARCH_OWNER_ME"], ["2", "Big customers", "SCREEN_TEXT_SEARCH_OWNER_ME"], ["3", "To review", "OPE"]]);
      expect(row(1).find(hook(TestIds.windowSavedSearchRename)).attr("aria-label")).toBe("SCREEN_TEXT_RENAME_SEARCH Admins");
      expect(row(1).find(hook(TestIds.windowSavedSearchDelete)).attr("aria-label")).toBe("SCREEN_TEXT_DELETE_SEARCH Admins");
      expect(row(3).find(hook(TestIds.windowSavedSearchRename)).length).toBe(0);
      expect(row(3).find(hook(TestIds.windowSavedSearchDelete)).length).toBe(0);
      expect(manageDialog().find(hook(TestIds.windowSavedSearchManageClose)).attr("data-dismiss")).toBe("modal");
    });

    it("lets a user who may publish manage the shared searches too", () => {
      profiles = [{value: "OPE", label: "Operator"}];
      const element = compileWindow();

      openManage(element);

      expect(row(3).find(hook(TestIds.windowSavedSearchRename)).length).toBe(1);
      expect(row(3).find(hook(TestIds.windowSavedSearchDelete)).length).toBe(1);
      expect(text(row(3).children().eq(1))).toBe("Operator");
    });

    it("renames a search in its row and reloads the list", () => {
      const element = compileWindow();
      openManage(element);

      row(2).find(hook(TestIds.windowSavedSearchRename)).trigger("click");
      digest();
      const input = row(2).find(hook(TestIds.windowSavedSearchRenameInput));
      expect(input.val()).toBe("Big customers");
      expect(document.activeElement).toBe(input[0]);
      input.val("Bigger customers").trigger("input");
      row(2).find(hook(TestIds.windowSavedSearchRenameAccept)).trigger("click");
      digest();

      const rename = serviceCall("renameWindowSearch");
      expect(rename[0].attr("parameters")).toEqual(expect.objectContaining({
        serverAction: "maintain-silent", targetAction: "renameWindowSearch", savedSearchId: 2, savedSearchName: "Bigger customers"}));
      succeed(rename);
      expect(posts().map(({target}) => target)).toEqual(["getWindowSavedSearches", "getSavedSearchProfiles", "getWindowSavedSearches"]);
      expect(row(2).find(hook(TestIds.windowSavedSearchRenameInput)).length).toBe(0);
    });

    it("renames with Enter and cancels with Escape without closing the dialog", () => {
      const element = compileWindow();
      openManage(element);
      row(1).find(hook(TestIds.windowSavedSearchRename)).trigger("click");
      digest();

      const escape = keydown(row(1).find(hook(TestIds.windowSavedSearchRenameInput)), "Escape");
      expect(escape.isPropagationStopped()).toBe(true);
      expect(row(1).find(hook(TestIds.windowSavedSearchRenameInput)).length).toBe(0);
      expect(manageDialog().attr("data-open")).toBe("true");
      expect(serviceCall("renameWindowSearch")).toBeUndefined();

      row(1).find(hook(TestIds.windowSavedSearchRename)).trigger("click");
      digest();
      row(1).find(hook(TestIds.windowSavedSearchRenameInput)).val("Admins 2").trigger("input");
      keydown(row(1).find(hook(TestIds.windowSavedSearchRenameInput)), "Enter");
      expect(serviceCall("renameWindowSearch")[0].attr("parameters").savedSearchName).toBe("Admins 2");
    });

    it("does not rename to an empty name", () => {
      const element = compileWindow();
      openManage(element);
      row(1).find(hook(TestIds.windowSavedSearchRename)).trigger("click");
      digest();
      row(1).find(hook(TestIds.windowSavedSearchRenameInput)).val("").trigger("input");

      row(1).find(hook(TestIds.windowSavedSearchRenameAccept)).trigger("click");
      digest();

      expect(serviceCall("renameWindowSearch")).toBeUndefined();
      expect(row(1).find(hook(TestIds.windowSavedSearchRenameInput)).attr("aria-invalid")).toBe("true");
    });

    it("cancels the rename with its Cancel button", () => {
      const element = compileWindow();
      openManage(element);
      row(1).find(hook(TestIds.windowSavedSearchRename)).trigger("click");
      digest();

      row(1).find(hook(TestIds.windowSavedSearchRenameCancel)).trigger("click");
      digest();

      expect(row(1).find(hook(TestIds.windowSavedSearchRenameInput)).length).toBe(0);
      expect(text(row(1).children().eq(0))).toBe("Admins");
    });

    it("deletes a search after the confirmation and reloads the list", () => {
      const element = compileWindow();
      openManage(element);

      row(1).find(hook(TestIds.windowSavedSearchDelete)).trigger("click");
      digest();

      const [actions] = actionLists().slice(-1);
      expect(actions[0]).toEqual(expect.objectContaining({type: "confirm",
        parameters: {title: "SCREEN_TEXT_DELETE_SEARCH", message: "SCREEN_TEXT_DELETE_SEARCH_MESSAGE"}}));
      expect(serviceCall("deleteWindowSearch")).toBeUndefined();

      // The confirmation is accepted: the next action deletes the search
      actions[1].launchAction();
      const remove = serviceCall("deleteWindowSearch");
      expect(remove[0].attr("parameters")).toEqual(expect.objectContaining({
        serverAction: "maintain-silent", targetAction: "deleteWindowSearch", savedSearchId: 1}));
      succeed(remove);
      expect(posts().map(({target}) => target)).toEqual(["getWindowSavedSearches", "getSavedSearchProfiles", "getWindowSavedSearches"]);
    });

    it("forgets the applied search when it is deleted", () => {
      const element = compileWindow();
      addSubmit(element);
      open(element);
      items(element).eq(0).trigger("click");
      digest();
      openManage(element);

      row(1).find(hook(TestIds.windowSavedSearchDelete)).trigger("click");
      digest();
      actionLists().slice(-1)[0][1].launchAction();
      succeed(serviceCall("deleteWindowSearch"));

      expect(text(button(element))).toBe("SCREEN_TEXT_SAVED_SEARCHES");
    });
  });

  describe("a failed load (S2)", () => {
    const messages = () => actionLists().flat().filter(action => action && action.type === "message");
    const DEFAULT_ERROR = {type: "message", parameters: {type: "error",
      title: "SCREEN_TEXT_SAVED_SEARCH_ERROR_TITLE", message: "SCREEN_TEXT_SAVED_SEARCH_ERROR_MESSAGE"}};
    const reopen = element => {
      keydown(menuItems(element)[0], "Escape");
      open(element);
    };

    it("tells that the searches could not be loaded when the call fails, and keeps what the menu showed", () => {
      const element = compileWindow();
      open(element);
      answers.getWindowSavedSearches = () => $q.reject({status: 500});

      reopen(element);

      expect(messages()).toEqual([DEFAULT_ERROR]);
      expect(items(element).map((i, item) => item.getAttribute("data-saved-search-id")).get()).toEqual(["1", "2", "3"]);
      expect(menu(element).attr("aria-busy")).toBeUndefined();
    });

    it("shows the message the server answers instead of the searches", () => {
      const element = compileWindow();
      open(element);
      const error = {type: "message", parameters: {type: "error", title: "Query failed", message: "The query failed"}};
      answers.getWindowSavedSearches = () => $q.resolve({data: [{type: "end-load"}, error]});

      reopen(element);

      expect(messages()).toEqual([error]);
      expect(items(element).length).toBe(3);
    });

    it("tells that the profiles could not be loaded, and loads them again on the next opening", () => {
      answers.getSavedSearchProfiles = () => $q.reject({status: 500});
      const element = compileWindow();
      open(element);

      expect(messages()).toEqual([DEFAULT_ERROR]);
      delete answers.getSavedSearchProfiles;
      reopen(element);

      expect(posts().map(({target}) => target)).toEqual(["getWindowSavedSearches", "getSavedSearchProfiles",
        "getWindowSavedSearches", "getSavedSearchProfiles"]);
    });

    it("tells nothing when the loads work", () => {
      const element = compileWindow();

      open(element);

      expect(messages()).toEqual([]);
    });
  });

  it("removes its dialogs with the window", () => {
    const element = compileWindow();

    element.scope().$destroy();

    expect(saveDialog().length).toBe(0);
    expect(manageDialog().length).toBe(0);
  });
});
