import {
  buildChanges, columnVisibility, isColumnCatalog, countInvalid, revealHiddenColumns, withDefaults, toStoragePending, visibleRules, effectiveValue, orderAttributes, ownRule, previewValue, toMaintainRequest, validateValue, SCREEN_SEARCH, latestOnly, limitMatches, chosenUserText, canvasSubject, subjectParams, buildOverview, deleteChanges, keepOneChanges, formatOverviewValue, describeOverview, describeHistory, describeSkipped, undoChanges, buildExport, importLogins, planImport
} from "../../../src/utilities/screenPersonalization";
import type {Scope} from "../../../src/utilities/screenPersonalization";

const rule = (id: number, attribute: string, value: string, user: number | null = null, profile: number | null = null, active = 1) =>
  ({id, user, profile, component: "Btn", attribute, value, active});

describe("screenPersonalization", () => {
  describe("effectiveValue: everyone < profile < user", () => {
    const rules = [rule(1, "label", "E"), rule(2, "label", "P", null, 7), rule(3, "label", "U", 5)];

    it("matches the scope of a rule whose profile and user ids arrive as text (a computed list column)", () => {
      const asText = [{...rule(2, "label", "P", null, 7), profile: "7"}, {...rule(3, "label", "U", 5), user: "5"}] as unknown as ReturnType<typeof rule>[];
      expect(effectiveValue(asText, "label", {type: "profile", id: 7}, "xml")).toMatchObject({value: "P", origin: "profile"});
      expect(effectiveValue(asText, "label", {type: "user", id: 5}, "xml")).toMatchObject({value: "U", origin: "user"});
      expect(ownRule(asText, "label", {type: "profile", id: 7})).not.toBeNull();
    });

    it("falls back to the XML value when no rule applies", () => {
      expect(effectiveValue([], "label", {type: "everyone", id: null}, "xml")).toEqual({value: "xml", origin: "default", rule: null});
    });
    it("reads everyone at the everyone scope", () => {
      expect(effectiveValue(rules, "label", {type: "everyone", id: null}, "xml")).toMatchObject({value: "E", origin: "everyone"});
    });
    it("profile beats everyone and ignores other profiles", () => {
      expect(effectiveValue(rules, "label", {type: "profile", id: 7}, "xml")).toMatchObject({value: "P", origin: "profile"});
      expect(effectiveValue(rules, "label", {type: "profile", id: 8}, "xml")).toMatchObject({value: "E", origin: "everyone"});
    });
    it("user beats profile and everyone, and an inactive rule is ignored", () => {
      expect(effectiveValue(rules, "label", {type: "user", id: 5}, "xml")).toMatchObject({value: "U", origin: "user"});
      expect(effectiveValue([rule(3, "label", "U", 5, null, 0)], "label", {type: "user", id: 5}, "xml").origin).toBe("default");
    });
  });

  it("ownRule finds only the rule of the exact scope", () => {
    const rules = [rule(1, "label", "E"), rule(2, "label", "P", null, 7)];
    expect(ownRule(rules, "label", {type: "profile", id: 7})?.id).toBe(2);
    expect(ownRule(rules, "label", {type: "user", id: 7})).toBeNull();
  });

  describe("validateValue mirrors the server", () => {
    it("boolean literal", () => {
      expect(validateValue("BOOLEAN", "True", [])).toBeNull();
      expect(validateValue("BOOLEAN", "yes", [])).toBe("INVALID_BOOLEAN");
    });
    it("integer", () => {
      expect(validateValue("INTEGER", "-12", [])).toBeNull();
      expect(validateValue("INTEGER", "1.5", [])).toBe("INVALID_INTEGER");
      expect(validateValue("INTEGER", "1234567890", [])).toBe("INVALID_INTEGER");
    });
    it("enum member", () => {
      expect(validateValue("ENUM", "left", ["left", "right"])).toBeNull();
      expect(validateValue("ENUM", "up", ["left", "right"])).toBe("INVALID_ENUM");
    });
    it("text of at most 60 characters", () => {
      expect(validateValue("TEXT", "a".repeat(60), [])).toBeNull();
      expect(validateValue("TEXT", "a".repeat(61), [])).toBe("VALUE_TOO_LONG");
    });
  });

  it("orderAttributes puts the common ones first and the rest under advanced", () => {
    const list = ["zeta", "required", "label", "visible", "alpha"].map(attribute => ({attribute}));
    const {common, advanced} = orderAttributes(list);
    expect(common.map(item => item.attribute)).toEqual(["visible", "required", "label"]);
    expect(advanced.map(item => item.attribute)).toEqual(["alpha", "zeta"]);
  });

  describe("buildChanges", () => {
    const scope = {type: "profile" as const, id: 7};
    const rules = [rule(2, "label", "P", null, 7), rule(9, "style", "x", null, 7)];

    it("inserts, updates and deletes against the rules of the scope", () => {
      const changes = buildChanges(rules, {"Btn|label": "New", "Btn|visible": "false", "Btn|style": null}, scope, "Scr");
      expect(changes).toEqual([
        expect.objectContaining({operation: "UPDATE", id: 2, attribute: "label", value: "New"}),
        expect.objectContaining({operation: "INSERT", id: null, attribute: "visible", value: "false", profile: 7, user: null, screen: "Scr", component: "Btn"}),
        expect.objectContaining({operation: "DELETE", id: 9, attribute: "style"})
      ]);
    });
    it("skips a reset of an attribute with no rule and a value equal to the current rule", () => {
      expect(buildChanges(rules, {"Btn|align": null, "Btn|label": "P"}, scope, "Scr")).toEqual([]);
    });
    it("toMaintainRequest sends the lists of the screen configuration maintain", () => {
      const request = toMaintainRequest(buildChanges(rules, {"Btn|visible": "false"}, scope, "Scr"));
      expect(request).toMatchObject({"GrdScrCnf-RowTyp": ["INSERT"], Scr: ["Scr"], Nam: ["Btn"], Atr: ["visible"], Val: ["false"], IdePro: [7], IdeOpe: [null], Act: [1], IdeAweScrCnf: [null]});
    });
  });

  it("countInvalid counts the pending values that the server would reject, and ignores resets", () => {
    const meta = {"Btn|max": {valueType: "INTEGER", allowedValues: []}, "Btn|align": {valueType: "ENUM", allowedValues: ["left"]}};
    expect(countInvalid({"Btn|max": "x", "Btn|align": "left"}, meta)).toBe(1);
    expect(countInvalid({"Btn|max": null}, meta)).toBe(0);
    expect(countInvalid({}, meta)).toBe(0);
  });

  describe("column visibility is the inverse of its hidden attribute", () => {
    const column = [{attribute: "hidden", valueType: "BOOLEAN", allowedValues: [], value: "true"}, {attribute: "label", valueType: "TEXT", allowedValues: [], value: "x"}];

    it("shows one Visible row instead of Hidden, with the inverted value", () => {
      const shown = columnVisibility(column);
      expect(shown.map(item => item.attribute)).toEqual(["visible", "label"]);
      expect(shown[0]).toMatchObject({valueType: "BOOLEAN", value: "false"});
    });
    it("tells a column (hidden, no visible) from any other element, so only a column stores Visible as hidden", () => {
      const button = [{attribute: "visible", valueType: "BOOLEAN", allowedValues: [], value: ""}, {attribute: "label", valueType: "TEXT", allowedValues: [], value: "x"}];
      expect(isColumnCatalog(column)).toBe(true);
      expect(isColumnCatalog(button)).toBe(false);
      expect(isColumnCatalog([column[1]])).toBe(false);
    });
    it("leaves a component that has visible untouched", () => {
      const other = [{attribute: "visible", valueType: "BOOLEAN", allowedValues: [], value: "true"}];
      expect(columnVisibility(other)).toBe(other);
    });
    it("reads the hidden rules as inverted visible rules, so origin and reset work on them", () => {
      expect(visibleRules([rule(1, "hidden", "true"), rule(2, "label", "L")])).toEqual([rule(1, "visible", "false"), rule(2, "label", "L")]);
    });
    it("writes a Visible edit back as the inverted hidden value", () => {
      expect(toStoragePending({"Btn|visible": "true", "Btn|label": "L", "Btn|visible2": null}, new Set(["Btn"])))
        .toEqual({"Btn|hidden": "false", "Btn|label": "L", "Btn|visible2": null});
      expect(toStoragePending({"Btn|visible": null}, new Set(["Btn"]))).toEqual({"Btn|hidden": null});
      expect(toStoragePending({"Other|visible": "true"}, new Set(["Btn"]))).toEqual({"Other|visible": "true"});
    });
  });

  describe("revealHiddenColumns (inspect mode only)", () => {
    it("draws the hidden columns narrow and remembers which ones are really hidden", () => {
      const model = [{name: "a", hidden: false, width: 200}, {name: "b", hidden: true, width: 300}];
      const {columnModel, hidden} = revealHiddenColumns(model);
      expect(hidden).toEqual(["b"]);
      expect(columnModel[1]).toMatchObject({name: "b", hidden: false, width: 80});
      expect(columnModel[0]).toBe(model[0]);
      expect(model[1].hidden).toBe(true);
    });
    it("gives back the same model when nothing is hidden", () => {
      const model = [{name: "a", hidden: false}];
      expect(revealHiddenColumns(model).columnModel).toBe(model);
    });
  });

  describe("what an unset attribute really is", () => {
    const bool = (attribute: string, value = "") => ({attribute, valueType: "BOOLEAN", allowedValues: [] as string[], value});

    it("unset booleans take the real default: visible-like true, readonly and required false", () => {
      const shown = withDefaults([bool("visible"), bool("printable"), bool("readonly"), bool("required"), bool("hidden"), bool("visible", "false"),
        {attribute: "label", valueType: "TEXT", allowedValues: [] as string[], value: ""}]);
      expect(shown.map(item => item.value)).toEqual(["true", "true", "false", "false", "false", "false", ""]);
    });
    it("a column whose hidden is unset is visible", () => {
      expect(columnVisibility(withDefaults([bool("hidden")]))[0]).toMatchObject({attribute: "visible", value: "true"});
    });
  });

  describe("the options at the top depend on the type of element", () => {
    const names = (type: string) => orderAttributes(["readonly", "required", "label", "placeholder", "defaultValue", "style", "width", "align", "sortable", "visible", "title", "zeta"]
      .map(attribute => ({attribute})), type).common.map(item => item.attribute);

    it("criterion, column and button each get their own list", () => {
      expect(names("Criteria")).toEqual(["visible", "readonly", "required", "label", "placeholder", "defaultValue"]);
      expect(names("Column")).toEqual(["visible", "label", "width", "align", "sortable"]);
      expect(names("Button")).toEqual(["visible", "label", "style", "title"]);
    });
  });

  describe("previewValue: a pending value as the canvas component reads it", () => {
    it("types booleans and integers", () => {
      expect(previewValue("BOOLEAN", "False", [])).toBe(false);
      expect(previewValue("INTEGER", "12", [])).toBe(12);
      expect(previewValue("TEXT", "Add user", [])).toBe("Add user");
    });
    it("gives nothing for a value the server would reject, so the canvas keeps the original", () => {
      expect(previewValue("INTEGER", "1x", [])).toBeUndefined();
      expect(previewValue("ENUM", "c", ["a", "b"])).toBeUndefined();
    });
  });

  describe("screen search", () => {
    it("asks for a page of at most 50 matches after one character", () => {
      expect(SCREEN_SEARCH).toEqual({minChars: 1, delay: 250, limit: 50});
    });

    it("keeps at most the limit of matches, in order, when the server sent more", () => {
      const rows = Array.from({length: 60}, (_, index) => index);

      expect(limitMatches(rows, 50)).toHaveLength(50);
      expect(limitMatches(rows, 50)[49]).toBe(49);
      expect(limitMatches([1, 2], 50)).toEqual([1, 2]);
    });

    it("lets only the latest search apply its answer", () => {
      const next = latestOnly();
      const first = next();
      const second = next();

      expect(first()).toBe(false);
      expect(second()).toBe(true);
    });
  });

  describe("chosenUserText", () => {
    it("shows the user text only once a user is chosen, not the half-typed search", () => {
      expect(chosenUserText({type: "user", id: null}, "e")).toBe("");
      expect(chosenUserText({type: "user", id: 5}, "Ed Smith")).toBe("Ed Smith");
    });
  });

  describe("canvasSubject: who the canvas shows the screen to", () => {
    const everyone: Scope = {type: "everyone", id: null};

    it("follows the edited scope while View as is linked", () => {
      expect(canvasSubject({type: "profile", id: 7}, {type: "scope", id: null}, everyone)).toEqual({type: "profile", id: 7});
    });

    it("shows the chosen subject when View as is not linked, whatever the edited scope is", () => {
      expect(canvasSubject({type: "user", id: 5}, {type: "profile", id: 7}, everyone)).toEqual({type: "profile", id: 7});
      expect(canvasSubject({type: "user", id: 5}, {type: "everyone", id: null}, everyone)).toEqual({type: "everyone", id: null});
    });

    it("keeps the last complete subject while a profile or user is not chosen yet", () => {
      const last: Scope = {type: "user", id: 5};
      expect(canvasSubject({type: "user", id: null}, {type: "scope", id: null}, last)).toBe(last);
      expect(canvasSubject({type: "everyone", id: null}, {type: "profile", id: null}, last)).toBe(last);
    });
  });

  describe("subjectParams", () => {
    it("names the subject type and its id for the screen data request", () => {
      expect(subjectParams({type: "user", id: 5})).toEqual({subjectType: "user", subjectId: 5});
      expect(subjectParams({type: "everyone", id: null})).toEqual({subjectType: "everyone", subjectId: null});
    });
  });

  describe("overview", () => {
    const stored = (ruleId: number, component: string, attribute: string, value: string, extra: Record<string, unknown> = {}) =>
      ({ruleId, component, attribute, value, scopeType: "everyone", profileId: "", profileName: "", userId: "", userName: "", problem: "", duplicateGroup: "", ...extra}) as never;
    const t = (key: string) => key;

    it("groups the working rules by element and option, ordered everyone, profiles, users", () => {
      const {blocks} = buildOverview([
        stored(3, "Btn", "label", "Ana's", {scopeType: "user", userId: "5", userName: "ana"}),
        stored(2, "Btn", "label", "Admins", {scopeType: "profile", profileId: "7", profileName: "Admin"}),
        stored(1, "Btn", "label", "All"), stored(4, "Btn", "visible", "false")], new Set());

      expect(blocks.map(block => [block.component, block.attribute])).toEqual([["Btn", "label"], ["Btn", "visible"]]);
      expect(blocks[0].entries.map(entry => [entry.scope, entry.id, entry.name, entry.value])).toEqual([
        ["everyone", null, "", "All"], ["profile", 7, "Admin", "Admins"], ["user", 5, "ana", "Ana's"]]);
    });

    it("shows the hidden rule of a column as the inverted Visible", () => {
      const {blocks} = buildOverview([stored(1, "Col", "hidden", "true")], new Set(["Col"]));

      expect(blocks[0]).toMatchObject({attribute: "visible", entries: [{value: "false"}]});
    });

    it("counts the elements changed and leaves the rules that no longer work out of the blocks", () => {
      const overview = buildOverview([stored(1, "Btn", "label", "x"), stored(2, "Btn", "visible", "false"), stored(3, "Gone", "label", "x", {problem: "missingElement"})], new Set());

      expect(overview.changedElements).toBe(1);
      expect(overview.blocks).toHaveLength(2);
    });

    it("lists one problem per rule and one per group of duplicates, with the rows to choose from", () => {
      const {problems} = buildOverview([
        stored(1, "Gone", "label", "x", {problem: "missingElement"}), stored(2, "Btn", "align", "x", {problem: "invalidAttribute"}),
        stored(3, "Btn", "label", "x", {problem: "missingSubject", scopeType: "profile", profileId: "7"}),
        stored(4, "Btn", "visible", "true", {problem: "duplicate", duplicateGroup: "g"}), stored(5, "Btn", "visible", "false", {problem: "duplicate", duplicateGroup: "g"})], new Set());

      expect(problems.map(problem => [problem.kind, problem.rows.map(row => row.ruleId)])).toEqual([
        ["missingElement", [1]], ["invalidAttribute", [2]], ["missingSubject", [3]], ["duplicate", [4, 5]]]);
    });

    it("formats a value as the editor shows it: Visible/Hidden, Yes/No, a label key translated", () => {
      expect(formatOverviewValue("visible", "false", t)).toBe("SCR_PER_VALUE_HIDDEN");
      expect(formatOverviewValue("visible", "true", t)).toBe("SCR_PER_VALUE_VISIBLE");
      expect(formatOverviewValue("readonly", "TRUE", t)).toBe("SCR_PER_VALUE_YES");
      expect(formatOverviewValue("label", "BUTTON_NEW", () => "New")).toBe("New");
      expect(formatOverviewValue("width", "80", t)).toBe("80");
    });

    it("deletes a rule as the update maintain reads it, and keeps one of a group by deleting the others", () => {
      const rows = buildOverview([stored(4, "Btn", "visible", "true", {problem: "duplicate", duplicateGroup: "g", profileId: "7", scopeType: "profile"}),
        stored(5, "Btn", "visible", "false", {problem: "duplicate", duplicateGroup: "g", profileId: "7", scopeType: "profile"})], new Set()).problems[0].rows;

      expect(deleteChanges([rows[0]], "Scr")).toEqual([{operation: "DELETE", id: 4, screen: "Scr", component: "Btn", attribute: "visible", value: "true", user: null, profile: 7}]);
      expect(keepOneChanges(rows, 5, "Scr").map(change => change.id)).toEqual([4]);
    });

    it("describes the overview in plain words for both engines", () => {
      const msg = (key: string, ...args: Array<string | number>) => `${key}(${args.join(",")})`;
      const view = describeOverview("Users", [
        stored(1, "Btn", "label", "Add"), stored(2, "Btn", "label", "Create", {scopeType: "user", userId: "5", userName: "ana", profileName: "Admin"}),
        stored(3, "Gone", "label", "x", {problem: "missingElement"}),
        stored(4, "Btn", "visible", "true", {problem: "duplicate", duplicateGroup: "g"}), stored(5, "Btn", "visible", "false", {problem: "duplicate", duplicateGroup: "g"})],
      {Btn: {id: "Btn", label: "New user", elementType: "Button"}}, msg, t);

      expect(view.title).toBe("SCR_PER_OVERVIEW_TITLE(Users,1)");
      expect(view.warning).toBe("SCR_PER_PROBLEMS(2)");
      expect(view.blocks[0].name).toBe('Button "New user" · label');
      expect(view.blocks[0].entries.map(entry => entry.text)).toEqual(["SCR_PER_SCOPE_EVERYONE → Add", "SCR_PER_SCOPE_USER ana (Admin) → Create"]);
      expect(view.problems.map(problem => problem.text)).toEqual([
        "SCR_PER_PROBLEM_MISSING_ELEMENT(Gone,label)", 'SCR_PER_PROBLEM_DUPLICATE(Button "New user",visible,SCR_PER_SCOPE_EVERYONE,2)']);
      expect(view.problems[1].keep.map(keep => keep.text)).toEqual(["SCR_PER_KEEP_VALUE(SCR_PER_VALUE_VISIBLE)", "SCR_PER_KEEP_VALUE(SCR_PER_VALUE_HIDDEN)"]);
    });
  });

  describe("history, undo, export and import", () => {
    const t = (key: string) => key;
    const msg = (key: string, ...args: Array<string | number>) => `${key}(${args.join(",")})`;
    const audit = (ruleId: number, action: "created" | "changed" | "deleted", valueBefore: string, valueAfter: string, extra = {}) => ({
      date: "09/11/2026 10:30:00", operator: "ana", action, ruleId, component: "Btn", attribute: "label", screen: "Scr", scopeType: "everyone" as const,
      profileId: "", profileName: "", userId: "", userName: "", value: valueAfter || valueBefore, valueBefore, valueAfter, undoable: true, ...extra
    });

    it("undoes a created rule by deleting it, a changed one by writing the value before, a deleted one by inserting it again", () => {
      expect(undoChanges(audit(4, "created", "", "a"), "Scr")).toEqual([{operation: "DELETE", id: 4, screen: "Scr", component: "Btn", attribute: "label", value: "a", user: null, profile: null}]);
      expect(undoChanges(audit(4, "changed", "a", "b", {profileId: "7", scopeType: "profile"}), "Scr")).toEqual([
        {operation: "UPDATE", id: 4, screen: "Scr", component: "Btn", attribute: "label", value: "a", user: null, profile: 7}]);
      expect(undoChanges(audit(4, "deleted", "a", "", {userId: "5", scopeType: "user"}), "Scr")).toEqual([
        {operation: "INSERT", id: null, screen: "Scr", component: "Btn", attribute: "label", value: "a", user: 5, profile: null}]);
    });

    it("describes the history as the clients draw it, with Undo only where the server allows it", () => {
      const view = describeHistory("Scr", [audit(2, "changed", "a", "b"), audit(1, "created", "", "x", {undoable: "false", scopeType: "profile", profileName: "Admin", profileId: "7"}),
        audit(3, "deleted", "z", "", {attribute: "visible", component: "Col", valueBefore: "true"})],
      {Btn: {id: "Btn", label: "New", elementType: "Button"}, Col: {id: "Col", label: "Name", elementType: "Column"}}, msg, t);

      expect(view.map(item => item.when)).toEqual(["09/11 10:30", "09/11 10:30", "09/11 10:30"]);
      expect(view[0]).toMatchObject({operator: "ana", name: 'Button "New" · label · SCR_PER_SCOPE_EVERYONE', change: "a → b", undoable: true});
      expect(view[1]).toMatchObject({name: 'Button "New" · label · SCR_PER_SCOPE_PROFILE Admin', change: "SCR_PER_HISTORY_CREATED(x)", undoable: false});
      expect(view[2].change).toBe("SCR_PER_HISTORY_DELETED(SCR_PER_VALUE_VISIBLE)");
    });

    it("tells the first recorded change of a rule that existed before the history from a change that was replaced later", () => {
      const rows = [audit(5, "changed", "b", "c", {undoable: false}), audit(5, "changed", "a", "b", {undoable: false}),
        audit(6, "changed", "", "x", {undoable: "false"}), audit(7, "changed", "y", "z"), audit(7, "changed", "", "y", {undoable: false})];
      const view = describeHistory("Scr", rows, {Btn: {id: "Btn", label: "New", elementType: "Button"}}, msg, t);

      expect(view.map(item => [item.undoable, item.unrecorded])).toEqual([[false, false], [false, false], [false, true], [true, false], [false, false]]);
      expect(view[2].change).toBe("SCR_PER_HISTORY_CHANGED_TO(x)");
    });

    it("names the rules an import leaves out as the history does, and keeps the id of an element that is not here", () => {
      const labels = describeSkipped([{component: "Btn", attribute: "label", scope: "profile", name: "SUP", reason: "profile"},
        {component: "Gone", attribute: "zzz", scope: "everyone", name: "Gone", reason: "element"}], {Btn: {id: "Btn", label: "New", elementType: "Button"}}, t);

      expect(labels).toEqual([{component: 'Button "New"', attribute: "label"}, {component: "Gone", attribute: "zzz"}]);
    });

    it("shows the hidden of a column as Visible, inverted, in the history", () => {
      const [item] = describeHistory("Scr", [audit(3, "changed", "false", "true", {attribute: "hidden", component: "Col"})], {Col: {id: "Col", label: "Name", elementType: "Column"}}, msg, t);

      expect(item.change).toBe("SCR_PER_VALUE_VISIBLE → SCR_PER_VALUE_HIDDEN");
    });

    it("exports every rule with the profile acronym and the user login", () => {
      const rows = [{component: "Btn", attribute: "label", value: "a", scopeType: "everyone", profile: "", user: ""},
        {component: "Btn", attribute: "label", value: "b", scopeType: "profile", profile: "ADM", user: ""},
        {component: "Btn", attribute: "label", value: "c", scopeType: "user", profile: "ADM", user: "ana"}] as Parameters<typeof buildExport>[1];

      expect(buildExport("Scr", rows)).toEqual({screen: "Scr", version: 1, rules: [
        {component: "Btn", attribute: "label", value: "a", scope: "everyone"}, {component: "Btn", attribute: "label", value: "b", scope: "profile", profile: "ADM"},
        {component: "Btn", attribute: "label", value: "c", scope: "user", user: "ana"}]});
    });

    describe("import", () => {
      const file = (rules: unknown[], extra = {}) => JSON.stringify({screen: "Scr", version: 1, rules, ...extra});
      const elements = {Btn: {id: "Btn"}};
      const profiles = [{id: 7, acronym: "ADM"}];
      const users = [{id: 5, login: "ana"}];
      const plan = (text: string, rules: ReturnType<typeof rule>[] = []) => planImport(text, "Scr", rules, elements, profiles, users);

      it("lists the logins of the file to look them up", () => {
        expect(importLogins(file([{component: "Btn", attribute: "label", value: "a", scope: "user", user: "ana"}, {component: "Btn", attribute: "visible", value: "a", scope: "user", user: "ana"}]))).toEqual(["ana"]);
        expect(importLogins("not json")).toEqual([]);
      });

      it("rejects a file that is not an export, of another screen or of another version", () => {
        expect(plan("not json").error).toEqual({code: "INVALID_FILE"});
        expect(plan(JSON.stringify({screen: "Scr", version: 2, rules: []})).error).toEqual({code: "INVALID_FILE"});
        expect(plan(JSON.stringify({screen: "Other", version: 1, rules: []})).error).toEqual({code: "OTHER_SCREEN", screen: "Other"});
      });

      it("adds the rules that do not exist, changes the ones with another value and leaves the same ones", () => {
        const result = plan(file([
          {component: "Btn", attribute: "label", value: "a", scope: "everyone"}, {component: "Btn", attribute: "visible", value: "false", scope: "profile", profile: "ADM"},
          {component: "Btn", attribute: "placeholder", value: "p", scope: "user", user: "ana"}, {component: "Btn", attribute: "title", value: "same", scope: "everyone"}]),
        [rule(1, "label", "old"), rule(2, "title", "same")]);

        expect(result.error).toBeNull();
        expect([result.toAdd, result.toChange, result.unchanged]).toEqual([2, 1, 1]);
        expect(result.changes).toEqual([
          {operation: "UPDATE", id: 1, screen: "Scr", component: "Btn", attribute: "label", value: "a", user: null, profile: null},
          {operation: "INSERT", id: null, screen: "Scr", component: "Btn", attribute: "visible", value: "false", user: null, profile: 7},
          {operation: "INSERT", id: null, screen: "Scr", component: "Btn", attribute: "placeholder", value: "p", user: 5, profile: null}]);
      });

      it("skips, and lists, the rules of a profile, a user or an element that does not exist here, and never deletes", () => {
        const result = plan(file([
          {component: "Btn", attribute: "label", value: "a", scope: "profile", profile: "NOPE"}, {component: "Btn", attribute: "label", value: "a", scope: "user", user: "bob"},
          {component: "Gone", attribute: "label", value: "a", scope: "everyone"}]), [rule(9, "label", "kept")]);

        expect(result.changes).toEqual([]);
        expect(result.skipped.map(skip => [skip.reason, skip.name])).toEqual([["profile", "NOPE"], ["user", "bob"], ["element", "Gone"]]);
      });
    });
  });
});
