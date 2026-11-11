import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import {buildPanel, previewHidden, previewPlan, toCatalog, toElements, toRules} from "../../../main/resources/js/awe/services/screenPersonalizer";

void DefaultSettings;

describe("screenPersonalizer loadScreen", () => {
  it("asks for the screen of the menu option that opens it, as the subject gets it", () => {
    angular.mock.module("aweApplication");
    inject(["$q", "$rootScope", "$httpBackend", "ScreenPersonalizer", "ServerData", ($q, $rootScope, $httpBackend, personalizer, serverData) => {
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      jest.spyOn(personalizer, "query").mockImplementation(() => $q.resolve([{option: "usr-opt"}]));
      const getScreenData = jest.spyOn(serverData, "getScreenData").mockImplementation(() => $q.resolve(null));

      personalizer.loadScreen("Usr", new Set(), {subjectType: "profile", subjectId: 7});
      $rootScope.$digest();

      expect(getScreenData).toHaveBeenCalledWith("usr-opt", expect.any(String), false, {subjectType: "profile", subjectId: 7});
    }]);
  });
});

describe("screenPersonalizer rows", () => {
  it("reads the rules of the screen from the rows of the list", () => {
    const rules = toRules([
      {IdeAweScrCnf: {value: 7}, IdeOpe: {value: ""}, IdePro: {value: 3}, Nam: {value: "name"}, Atr: {value: "label"}, Val: {value: "Full name"}, Act: {value: 1}},
      {IdeAweScrCnf: 8, IdeOpe: 5, NamVal: "age", AtrVal: "visible", Act: "1"}
    ]);

    expect(rules).toEqual([
      {id: 7, user: null, profile: 3, component: "name", attribute: "label", value: "Full name", active: 1},
      {id: 8, user: 5, profile: null, component: "age", attribute: "visible", value: "", active: 1}
    ]);
  });

  it("keeps the configurable elements of the structure and the grid of each column", () => {
    const elements = toElements([
      {path: "0", parent: "", id: "grid", label: "Users", elementType: "Grid", configurable: "true"},
      {path: "0.0", parent: "0", id: "name", label: "Name", elementType: "Column", configurable: "true"},
      {path: "1", parent: "", id: "", label: "", elementType: "Tag", configurable: "false"}
    ]);

    expect(Object.keys(elements)).toEqual(["grid", "name"]);
    expect(elements.name).toEqual({id: "name", label: "Name", elementType: "Column", grid: "grid"});
  });

  it("shows the real default of a switch, and a column Visible instead of its hidden", () => {
    const rows = [{attribute: "hidden", valueType: "BOOLEAN", value: "", allowedValues: ""}, {attribute: "label", valueType: "TEXT", value: "Name", allowedValues: "a,b"}];

    const {catalog, isColumn} = toCatalog(rows);

    expect(isColumn).toBe(true);
    expect(catalog.map(item => [item.attribute, item.value])).toEqual([["visible", "true"], ["label", "Name"]]);
    expect(catalog[1].allowedValues).toEqual(["a", "b"]);
  });
});

describe("screenPersonalizer preview", () => {
  const elements = {name: {id: "name", elementType: "Criteria"}, col: {id: "col", elementType: "Column", grid: "grid"}};
  const meta = {"name|label": {valueType: "TEXT", allowedValues: []}, "name|max": {valueType: "INTEGER", allowedValues: []}, "col|label": {valueType: "TEXT", allowedValues: []}};

  it("plans the valid pending values typed as the component reads them, apart from visible and the columns", () => {
    const plan = previewPlan({"name|label": "Full", "name|max": "12", "name|visible": "false", "col|label": "x"}, meta, elements);

    expect(plan).toEqual([{id: "name", attribute: "label", value: "Full"}, {id: "name", attribute: "max", value: 12}]);
  });

  it("previews a reset (null) with the default value of the attribute", () => {
    const withDefault = {"name|label": {valueType: "TEXT", allowedValues: [], value: "New user"}, "name|max": {valueType: "INTEGER", allowedValues: []}};

    expect(previewPlan({"name|label": null, "name|max": null}, withDefault, elements)).toEqual([{id: "name", attribute: "label", value: "New user"}]);
  });

  it("skips a value the server would reject", () => {
    expect(previewPlan({"name|max": "abc"}, meta, elements)).toEqual([]);
  });

  it("hides what a pending Visible hides and shows what it shows, apart from the stored state", () => {
    const base = {hidden: new Set(["a"]), hiddenColumns: new Set(["c1"])};
    const cols = {...elements, a: {id: "a", elementType: "Criteria"}, c1: {id: "c1", elementType: "Column"}, c2: {id: "c2", elementType: "Column"}};

    const result = previewHidden(base, {"a|visible": "true", "name|visible": "false", "c1|visible": "true", "c2|visible": "false"}, cols);

    expect([...result.hidden]).toEqual(["name"]);
    expect([...result.hiddenColumns]).toEqual(["c2"]);
    expect([...base.hidden]).toEqual(["a"]);
  });
});

describe("ScreenPersonalizer service", () => {
  let personalizer;
  let connection;
  let control;
  let actionController;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$injector", $injector => {
      personalizer = $injector.get("ScreenPersonalizer");
      connection = $injector.get("Connection");
      control = $injector.get("Control");
      actionController = $injector.get("ActionController");
    }]);
    jest.spyOn(connection, "getActionUrl").mockImplementation((action, target) => `/action/${action}/${target}`);
  });

  it("asks a query of the server and returns the rows of its answer", async () => {
    jest.spyOn(connection, "post").mockResolvedValue({data: [{type: "fill", parameters: {datalist: {rows: [{a: 1}]}}}]});

    const rows = await personalizer.query("ScrCnfLst", {CrtScr: "Usr"});

    expect(rows).toEqual([{a: 1}]);
    expect(connection.post.mock.calls[0][0]).toBe("/action/data/ScrCnfLst");
    expect(connection.post.mock.calls[0][1]).toMatchObject({CrtScr: "Usr", max: 0});
  });

  it("saves the changes through the update maintain and tells whether it worked", async () => {
    jest.spyOn(actionController, "addActionList").mockReturnValue(undefined);
    jest.spyOn(connection, "post").mockResolvedValue({data: [{type: "message", parameters: {type: "ok"}}]});
    const change = {operation: "INSERT", id: null, screen: "Usr", component: "name", attribute: "label", value: "X", user: null, profile: null};

    const worked = await personalizer.save([change]);

    expect(worked).toBe(true);
    expect(connection.post.mock.calls[0][0]).toBe("/action/maintain/updateScreenConfiguration");
    expect(connection.post.mock.calls[0][1]).toMatchObject({"GrdScrCnf-RowTyp": ["INSERT"], Nam: ["name"], Val: ["X"]});
    expect(actionController.addActionList).toHaveBeenCalled();
  });

  it("does not report a save that the server rejected", async () => {
    jest.spyOn(actionController, "addActionList").mockReturnValue(undefined);
    jest.spyOn(connection, "post").mockResolvedValue({data: [{type: "message", parameters: {type: "error"}}]});

    expect(await personalizer.save([])).toBe(false);
  });

  it("puts the pending values in the components and gives back the original of what is no longer pending", () => {
    const controller = {label: "Name"};
    jest.spyOn(control, "getAddressController").mockReturnValue(controller);
    jest.spyOn(control, "changeControllerAttribute").mockImplementation((address, attributes) => Object.assign(controller, attributes));
    const originals = {};

    personalizer.applyPreview([{id: "name", attribute: "label", value: "Full"}], originals);
    expect(controller.label).toBe("Full");
    expect(control.changeControllerAttribute).toHaveBeenCalledWith({view: "personalizer", component: "name"}, {label: "Full"});

    personalizer.applyPreview([], originals);
    expect(controller.label).toBe("Name");
  });
});

describe("screenPersonalizer panel", () => {
  // Translation of the test: a known key gets its text, any other key stays as it is; {0} takes the first argument
  const texts = {SCR_PER_ORIGIN_DEFAULT: "default", SCR_PER_ORIGIN_EVERYONE: "everyone", SCR_PER_ORIGIN_PERSONALIZED: "personalized", ENUM_SCR_ATR_LABEL: "Label", GREETING: "Hello"};
  const t = (key, ...args) => (texts[key] ?? key).replace("{0}", args[0]);
  const element = {id: "name", elementType: "Window"};
  const catalog = [
    {attribute: "label", valueType: "TEXT", value: "GREETING", allowedValues: []},
    {attribute: "max", valueType: "INTEGER", value: "", allowedValues: []},
    {attribute: "zIndex", valueType: "INTEGER", value: "", allowedValues: []}
  ];
  const everyone = {type: "everyone", id: null};
  const rule = {id: 1, user: null, profile: null, component: "name", attribute: "max", value: "5", active: 1};

  it("lists the common attributes first and the rest as advanced, with their labels", () => {
    const panel = buildPanel({element, catalog, rules: [], pending: {}, scope: everyone, t});

    expect(panel.common.map(row => row.attribute)).toEqual(["label", "max"]);
    expect(panel.advanced.map(row => row.attribute)).toEqual(["zIndex"]);
    expect(panel.common[0]).toMatchObject({label: "Label", text: "Hello", key: "GREETING", origin: "default", overridden: false});
    expect(panel.common[1].label).toBe("max");
  });

  it("shows the value of a rule of the scope as personalized, and the broader one as inherited", () => {
    const own = buildPanel({element, catalog, rules: [rule], pending: {}, scope: everyone, t}).common[1];
    const inherited = buildPanel({element, catalog, rules: [rule], pending: {}, scope: {type: "user", id: 9}, t}).common[1];

    expect(own).toMatchObject({value: "5", origin: "personalized", overridden: true});
    expect(inherited).toMatchObject({value: "5", origin: "everyone", overridden: false});
  });

  it("shows a pending edit as typed, with the error the server would give", () => {
    const row = buildPanel({element, catalog, rules: [], pending: {"name|max": "abc"}, scope: everyone, t}).common[1];

    expect(row).toMatchObject({value: "abc", text: "abc", overridden: true});
    expect(row.error).toContain("ERROR_MESSAGE_SCREEN_CONFIGURATION_INVALID_INTEGER");
  });

  it("shows the inherited value again for a reset attribute", () => {
    const row = buildPanel({element, catalog, rules: [rule], pending: {"name|max": null}, scope: everyone, t}).common[1];

    expect(row).toMatchObject({value: "", origin: "default", overridden: false});
  });
});
