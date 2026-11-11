import fs from "fs";
import path from "path";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";

void DefaultSettings;

const TEMPLATE_URL = "test-screen-personalizer-template";
const TEMPLATE = fs.readFileSync(path.join(__dirname, "../../../main/resources/templates/angular/screenPersonalizer.html"), "utf8");
const SCREEN = "<div><div criterion-id=\"name\"><input></div></div>";

describe("aweScreenPersonalizer", () => {
  let $rootScope;
  let $compile;
  let $templateCache;
  let personalizer;
  let answers;

  beforeEach(() => {
    answers = {
      getScreenList: [{label: "Users", value: "Usr"}],
      ProSel: [],
      ScrCnfLst: [],
      getScreenStructure: [{path: "0", parent: "", id: "name", label: "Name", elementType: "Criteria", configurable: "true"}],
      getScreenElementAttributes: [{attribute: "label", valueType: "TEXT", value: "Name", allowedValues: ""}]
    };
    function ComponentMock(scope) {
      return {asComponent: () => true, scope};
    }
    angular.mock.module("aweApplication", {
      Component: ComponentMock,
      ServerData: {getAngularTemplateUrl: () => TEMPLATE_URL},
      ViewController: function ViewController() {}
    });
    inject(["$rootScope", "$compile", "$templateCache", "$q", "$httpBackend", "ScreenPersonalizer", (_$rootScope_, _$compile_, _$templateCache_, $q, $httpBackend, _ScreenPersonalizer_) => {
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $templateCache = _$templateCache_;
      personalizer = _ScreenPersonalizer_;
      jest.spyOn(personalizer, "query").mockImplementation(target => $q.resolve(answers[target]));
      jest.spyOn(personalizer, "loadScreen").mockImplementation(() => $q.resolve({template: SCREEN, hidden: new Set(), hiddenColumns: new Set()}));
      jest.spyOn(personalizer, "save").mockImplementation(() => $q.resolve(true));
    }]);
    $templateCache.put(TEMPLATE_URL, TEMPLATE);
  });

  function compile() {
    const element = $compile("<awe-screen-personalizer screen-personalizer-id='personalizer'></awe-screen-personalizer>")($rootScope);
    $rootScope.$digest();
    document.body.appendChild(element[0]);
    return element;
  }

  it("draws the chosen screen and opens the panel of the element that is clicked", () => {
    const element = compile();
    const scope = element.isolateScope();

    scope.chooseScreen({value: "Usr", label: "Users"});
    $rootScope.$digest();
    const input = element[0].querySelector(".scr-per-canvas input");
    input.dispatchEvent(new MouseEvent("click", {bubbles: true, cancelable: true}));
    $rootScope.$digest();

    expect(scope.selected).toBe("name");
    expect(element[0].querySelector(".scr-per-side-title").textContent).toBe("Name");
    expect(element[0].querySelector("[criterion-id=name]").classList.contains("awe-inspect-selected")).toBe(true);
    expect(scope.panel.common.map(row => row.attribute)).toEqual(["label"]);
  });

  it("saves the typed changes of the scope and draws the screen again", () => {
    const element = compile();
    const scope = element.isolateScope();
    scope.chooseScreen({value: "Usr", label: "Users"});
    $rootScope.$digest();
    scope.select("name");
    $rootScope.$digest();

    scope.edit("label", "Full name");
    expect(scope.canSave()).toBe(true);
    scope.save();
    $rootScope.$digest();

    expect(personalizer.save.mock.calls[0][0]).toEqual([expect.objectContaining({operation: "INSERT", component: "name", attribute: "label", value: "Full name"})]);
    expect(personalizer.loadScreen).toHaveBeenCalledTimes(2);
    expect(scope.pending).toEqual({});
  });

  it("blocks the save of an invalid value and discards the pending ones", () => {
    const element = compile();
    const scope = element.isolateScope();
    answers.getScreenElementAttributes = [{attribute: "max", valueType: "INTEGER", value: "", allowedValues: ""}];
    scope.chooseScreen({value: "Usr", label: "Users"});
    $rootScope.$digest();
    scope.select("name");
    $rootScope.$digest();

    scope.edit("max", "abc");
    expect(scope.canSave()).toBe(false);
    scope.discard();

    expect(scope.changes()).toEqual([]);
  });

  describe("screen search", () => {
    let resolvers;
    beforeEach(() => {
      resolvers = [];
      personalizer.query.mockImplementation((target, values) => {
        if (target !== "getScreenList") {
          return Promise.resolve(answers[target]);
        }
        return new Promise(resolve => resolvers.push({values, resolve}));
      });
    });

    it("asks the server for the typed text with a short page, and not for less than the minimum", () => {
      const scope = compile().isolateScope();

      scope.screenInput.text = " ";
      scope.searchScreens();
      scope.screenInput.text = "Us";
      scope.searchScreens();

      expect(resolvers).toHaveLength(1);
      expect(resolvers[0].values).toEqual({suggest: "Us", max: 50});
    });

    it("ignores an older answer that arrives after a newer one", async () => {
      const scope = compile().isolateScope();
      scope.screenInput.text = "U";
      scope.searchScreens();
      scope.screenInput.text = "Us";
      scope.searchScreens();

      resolvers[1].resolve([{label: "Users", value: "Usr"}]);
      await Promise.resolve();
      $rootScope.$digest();
      resolvers[0].resolve([{label: "Other", value: "Oth"}]);
      await Promise.resolve();
      $rootScope.$digest();

      expect(scope.screenInput.suggestions.map(item => item.label)).toEqual(["Users"]);
    });

    it("keeps the label of the chosen screen and closes the list", () => {
      const scope = compile().isolateScope();

      scope.chooseScreen({value: "Usr", label: "Users"});

      expect(scope.screenInput).toEqual({text: "Users", suggestions: []});
      expect(scope.screen).toBe("Usr");
    });
  });

  it("shows the scope of a user only once the user is chosen, not the half-typed search", () => {
    const scope = compile().isolateScope();
    scope.setScopeType("user");

    scope.user.text = "e";
    expect(scope.scopeLabel()).toBe("PARAMETER_USER");
    scope.chooseUser({label: "Ed Smith", value: 5});

    expect(scope.scopeLabel()).toBe("Ed Smith");
  });

  describe("the canvas shows the screen as a subject gets it", () => {
    const subjects = () => personalizer.loadScreen.mock.calls.map(call => call[2]);
    const open = () => {
      const element = compile();
      const scope = element.isolateScope();
      scope.chooseScreen({value: "Usr", label: "Users"});
      $rootScope.$digest();
      return scope;
    };

    it("renders it for everyone by default, as the edited scope is everyone", () => {
      open();
      expect(subjects()).toEqual([{subjectType: "everyone", subjectId: null}]);
    });

    it("loads it again for the profile of the edited scope once the profile is chosen, and keeps the pending edits and the selection", () => {
      const scope = open();
      scope.select("name");
      $rootScope.$digest();
      scope.edit("label", "Full name");

      scope.setScopeType("profile");
      $rootScope.$digest();
      expect(subjects()).toHaveLength(1);
      scope.chooseProfile({value: 7, label: "Admin"});
      $rootScope.$digest();

      expect(subjects()[1]).toEqual({subjectType: "profile", subjectId: 7});
      expect(scope.pending).toEqual({"name|label": "Full name"});
      expect(scope.selected).toBe("name");
    });

    it("keeps showing the subject chosen in View as when the edited scope changes", () => {
      const scope = open();
      scope.setViewAsType("everyone");
      $rootScope.$digest();

      scope.setScopeType("profile");
      scope.chooseProfile({value: 7, label: "Admin"});
      $rootScope.$digest();

      expect(subjects()).toHaveLength(1);
    });
  });

  describe("overview mode", () => {
    const stored = (ruleId, extra) => ({ruleId, component: "name", attribute: "label", value: "Add", scopeType: "everyone", profileId: "", profileName: "",
      userId: "", userName: "", problem: "", duplicateGroup: "", ...extra});
    const open = () => {
      answers.getScreenConfigurationOverview = [stored(1), stored(2, {value: "Create", scopeType: "user", userId: "5", userName: "ana"}),
        stored(3, {component: "Gone", problem: "missingElement"})];
      const element = compile();
      const scope = element.isolateScope();
      scope.chooseScreen({value: "Usr", label: "Users"});
      $rootScope.$digest();
      scope.setMode("overview");
      $rootScope.$digest();
      return {element, scope};
    };

    it("lists what each subject sees per element and option, and the rules that no longer work", () => {
      const {element} = open();

      expect(element[0].querySelector(".scr-per-overview-name").textContent).toBe("Criteria \"Name\" · label");
      expect([...element[0].querySelectorAll(".scr-per-overview-subject")].map(node => node.textContent))
        .toEqual(["SCR_PER_SCOPE_EVERYONE → Add", "SCR_PER_SCOPE_USER ana → Create"]);
      expect(element[0].querySelectorAll(".scr-per-overview-problem")).toHaveLength(1);
    });

    it("opens the element in the editor at the scope of the row that was clicked", () => {
      const {element, scope} = open();

      element[0].querySelectorAll(".scr-per-overview-subject")[1].click();
      $rootScope.$digest();

      expect(scope.mode).toBe("edit");
      expect(scope.applies).toEqual({type: "user", id: 5});
      expect(scope.user.text).toBe("ana");
      expect(scope.selected).toBe("name");
    });

    it("deletes a rule that no longer works through the update maintain and reads the overview again", () => {
      const {element, scope} = open();
      personalizer.query.mockClear();

      element[0].querySelector(".scr-per-overview-problem button").click();
      $rootScope.$digest();

      expect(personalizer.save.mock.calls[0][0]).toEqual([expect.objectContaining({operation: "DELETE", id: 3})]);
      expect(personalizer.query.mock.calls.map(call => call[0])).toContain("getScreenConfigurationOverview");
      expect(scope.mode).toBe("overview");
    });
  });

  describe("history, export and import", () => {
    const audit = (ruleId, action, valueBefore, valueAfter, extra) => ({date: "09/11/2026 10:30:00", operator: "ana", action, ruleId, component: "name", attribute: "label",
      screen: "Usr", scopeType: "everyone", profileId: "", profileName: "", userId: "", userName: "", value: valueAfter || valueBefore, valueBefore, valueAfter, undoable: true, ...extra});
    const open = () => {
      answers.ProSel = [{value: 7, label: "ADM - Admin", Acr: "ADM"}];
      answers.getScreenConfigurationHistory = [audit(2, "changed", "a", "b"), audit(1, "created", "", "x", {undoable: false})];
      answers.getScreenConfigurationExport = [{component: "name", attribute: "label", value: "b", scopeType: "everyone", profile: "", user: ""}];
      answers.getScreenImportUsers = [{value: 5, login: "ana"}];
      answers.getScreenConfigurationOverview = [];
      const element = compile();
      const scope = element.isolateScope();
      scope.chooseScreen({value: "Usr", label: "Users"});
      $rootScope.$digest();
      return {element, scope};
    };
    // The file is read by a FileReader: wait until the preview is there, not for a fixed time (a busy runner reads slower)
    const untilPreviewed = async scope => {
      for (let tries = 0; !scope.importPlan && tries < 200; tries++) {
        await new Promise(resolve => setTimeout(resolve, 10));
        $rootScope.$digest();
      }
    };
    const importFile = (element, scope, content) => {
      const input = element[0].querySelector(".scr-per-import-file");
      Object.defineProperty(input, "files", {value: [new File([content], "Usr.json")], configurable: true});
      input.dispatchEvent(new Event("change"));
      return untilPreviewed(scope);
    };

    // The Import link opens a popover (the dropdown of the house) with the drop zone
    const toggle = element => element[0].querySelector(".scr-per-import-dropdown > [data-toggle=dropdown]");
    const isOpen = element => element[0].querySelector(".scr-per-import-dropdown").classList.contains("open");

    it("opens a popover on Import and closes it with Esc, giving the focus back to the link", () => {
      const {element} = open();
      toggle(element).click();

      expect(isOpen(element)).toBe(true);
      expect(element[0].querySelector(".scr-per-import-menu").textContent).toContain("SCR_PER_IMPORT_TITLE");
      expect(element[0].querySelector(".scr-per-import-menu").textContent).toContain("SCR_PER_IMPORT_HINT");
      expect(document.activeElement).toBe(element[0].querySelector(".scr-per-choose"));
      element[0].querySelector(".scr-per-choose").dispatchEvent(new KeyboardEvent("keydown", {which: 27, keyCode: 27, bubbles: true}));

      expect(isOpen(element)).toBe(false);
      expect(document.activeElement).toBe(toggle(element));
    });

    it("closes the popover and previews the import when a file is chosen", async () => {
      const {element, scope} = open();
      toggle(element).click();
      await importFile(element, scope, JSON.stringify({screen: "Usr", version: 1, rules: [{component: "name", attribute: "label", value: "n", scope: "everyone"}]}));

      expect(isOpen(element)).toBe(false);
      expect(scope.importPlan.toAdd).toBe(1);
    });

    it("closes the popover and previews the import when a file is dropped on the drop zone", async () => {
      const {element, scope} = open();
      toggle(element).click();
      const file = new File([JSON.stringify({screen: "Usr", version: 1, rules: [{component: "name", attribute: "label", value: "n", scope: "everyone"}]})], "Usr.json");
      const drop = new Event("drop", {bubbles: true, cancelable: true});
      drop.dataTransfer = {files: [file]};
      element[0].querySelector(".scr-per-dropzone").dispatchEvent(drop);
      await untilPreviewed(scope);

      expect(isOpen(element)).toBe(false);
      expect(scope.importPlan.toAdd).toBe(1);
    });

    it("tells the drawn screen to resize when it is shown again in Edit, so its grids take their size back", () => {
      const {scope} = open();
      const resized = jest.fn();
      scope.$on("resize", resized);
      scope.setMode("history");
      $rootScope.$digest();
      scope.setMode("edit");
      $rootScope.$digest();
      inject(["$timeout", $timeout => $timeout.flush()]);

      expect(resized).toHaveBeenCalledTimes(1);
    });

    it("lists the changes newest first with Undo only where it is allowed, and undoes one through the update maintain", () => {
      const {element, scope} = open();
      scope.setMode("history");
      $rootScope.$digest();

      expect([...element[0].querySelectorAll(".scr-per-history-change")].map(node => node.textContent)).toEqual(["a → b", "SCR_PER_HISTORY_CREATED"]);
      expect(element[0].querySelectorAll(".scr-per-history-row .btn")).toHaveLength(1);
      expect(element[0].querySelector(".scr-per-history-later").textContent).toBe("SCR_PER_CHANGED_LATER");
      personalizer.query.mockClear();
      element[0].querySelector(".scr-per-history-row .btn").click();
      $rootScope.$digest();

      expect(personalizer.save.mock.calls[0][0]).toEqual([expect.objectContaining({operation: "UPDATE", id: 2, value: "a"})]);
      expect(personalizer.query.mock.calls.map(call => call[0])).toContain("getScreenConfigurationHistory");
      expect(scope.mode).toBe("history");
    });

    it("says Earlier value not recorded for the first change of a rule that existed before the history", () => {
      const {element, scope} = open();
      answers.getScreenConfigurationHistory = [audit(2, "changed", "", "b", {undoable: false})];
      scope.setMode("history");
      $rootScope.$digest();

      expect(element[0].querySelector(".scr-per-history-later").textContent).toBe("SCR_PER_HISTORY_UNRECORDED");
      expect(element[0].querySelector(".scr-per-history-change").textContent).toBe("SCR_PER_HISTORY_CHANGED_TO");
    });

    it("downloads the rules of the screen as a file named after it", async () => {
      global.URL.createObjectURL = jest.fn(() => "blob:x");
      global.URL.revokeObjectURL = jest.fn();
      const click = jest.spyOn(window.HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
      const {scope} = open();
      scope.exportRules();
      $rootScope.$digest();

      expect(click).toHaveBeenCalled();
      const text = await new Promise(resolve => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.readAsText(global.URL.createObjectURL.mock.calls[0][0]);
      });
      expect(JSON.parse(text)).toEqual({screen: "Usr", version: 1, rules: [{component: "name", attribute: "label", value: "b", scope: "everyone"}]});
      click.mockRestore();
    });

    it("previews an import and applies it, as inserts only here, on Apply import", async () => {
      const {element, scope} = open();
      await importFile(element, scope, JSON.stringify({screen: "Usr", version: 1, rules: [
        {component: "name", attribute: "label", value: "n", scope: "profile", profile: "ADM"}, {component: "Gone", attribute: "label", value: "n", scope: "everyone"}]}));

      expect(scope.importPlan.toAdd).toBe(1);
      expect(element[0].querySelector(".scr-per-import-title").textContent).toBe("SCR_PER_IMPORT_PREVIEW");
      expect(element[0].querySelectorAll(".scr-per-import-skipped li")).toHaveLength(1);
      expect(personalizer.save).not.toHaveBeenCalled();
      scope.applyImport();
      $rootScope.$digest();

      expect(personalizer.save.mock.calls[0][0]).toEqual([expect.objectContaining({operation: "INSERT", profile: 7, value: "n"})]);
      expect(scope.importPlan).toBeNull();
    });

    it("names the rules an import leaves out as the overview does, and reads the history again after Apply import", async () => {
      const {element, scope} = open();
      await importFile(element, scope, JSON.stringify({screen: "Usr", version: 1, rules: [
        {component: "name", attribute: "label", value: "n", scope: "profile", profile: "NOPE"}, {component: "name", attribute: "label", value: "n", scope: "everyone"}]}));

      expect(scope.importPlan.skippedLabels).toEqual([{component: 'Criteria "Name"', attribute: "label"}]);
      personalizer.query.mockClear();
      scope.applyImport();
      $rootScope.$digest();

      expect(personalizer.query.mock.calls.map(call => call[0])).toEqual(expect.arrayContaining(["getScreenConfigurationHistory", "getScreenConfigurationOverview"]));
    });

    it("discards the preview on Cancel without sending anything", async () => {
      const {element, scope} = open();
      await importFile(element, scope, JSON.stringify({screen: "Usr", version: 1, rules: [{component: "name", attribute: "label", value: "n", scope: "everyone"}]}));
      scope.cancelImport();
      $rootScope.$digest();

      expect(scope.importPlan).toBeNull();
      expect(personalizer.save).not.toHaveBeenCalled();
    });
  });
});
