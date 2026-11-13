import React from "react";
import {act, fireEvent, screen, waitFor} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweScreenPersonalizer from "../../../src/widgets/AweScreenPersonalizer";
import ViewRegistry from "../../../src/redux/registry/ViewRegistry";

const fill = rows => Promise.resolve({headers: {get: () => "application/json;charset=UTF-8"}, status: 200, ok: true,
  json: () => Promise.resolve([{type: "fill", parameters: {datalist: {rows}}}])});

describe("awe-react-client/test/js/widgets/AweScreenPersonalizerTest.jsx", () => {
  const preloadedState = {settings: DEFAULT_SETTINGS, components: {}};
  const answers = {getScreenList: [{value: "Scr", label: "Scr"}], ProSel: [], getScreenOption: []};

  beforeEach(() => {
    ViewRegistry.clearAll();
    global.fetch = jest.fn(url => fill(answers[Object.keys(answers).find(key => String(url).includes(key))] || []));
  });

  it("leaves the screen that holds the editor (the report view) and the current view alone", () => {
    const editorScreen = {loading: false, name: "ScrPer", structure: {}};
    ViewRegistry.setView("report", editorScreen);
    ViewRegistry.setCurrentView("report");
    const {unmount} = renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
    unmount();
    expect(ViewRegistry.get("report")).toBe(editorScreen);
    expect(ViewRegistry.getCurrentView()).toBe("report");
  });

  it("says so when no menu option opens the chosen screen, instead of loading it", async () => {
    renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
    await act(async () => {});
    fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Sc"}});
    fireEvent.click(await screen.findByText("Scr", {selector: "li *, li"}));
    expect(await screen.findByText("SCR_PER_NO_OPTION")).toBeInTheDocument();
    await waitFor(() => expect(global.fetch.mock.calls.some(([url]) => String(url).includes("screen-data"))).toBe(false));
  });

  it("searches the screens on the server with what is typed, a short page, and not before anything is typed", async () => {
    renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
    await act(async () => {});
    expect(global.fetch.mock.calls.some(([url]) => String(url).includes("getScreenList"))).toBe(false);

    fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Us"}});
    await waitFor(() => expect(global.fetch.mock.calls.some(([url]) => String(url).includes("getScreenList"))).toBe(true));

    const [, options] = global.fetch.mock.calls.find(([url]) => String(url).includes("getScreenList"));
    expect(JSON.parse(options.body)).toMatchObject({suggest: "Us", max: 50});
  });

  it("keeps the label of the chosen screen in the picker", async () => {
    renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
    await act(async () => {});
    fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Sc"}});
    fireEvent.click(await screen.findByText("Scr", {selector: "li *, li"}));

    await waitFor(() => expect(screen.getByLabelText("PARAMETER_SCREEN")).toHaveValue("Scr"));
  });

  describe("the canvas shows the screen as a subject gets it", () => {
    const loads = () => global.fetch.mock.calls.filter(([url]) => String(url).includes("screen-data")).map(([, options]) => JSON.parse(options.body));
    const open = async () => {
      answers.getScreenOption = [{option: "scr-opt"}];
      answers.ProSel = [{value: 7, label: "Admin"}];
      renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
      await act(async () => {});
      fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Sc"}});
      fireEvent.click(await screen.findByText("Scr", {selector: "li *, li"}));
      await waitFor(() => expect(loads()).toHaveLength(1));
    };
    const choose = async (label, text) => {
      fireEvent.click(screen.getAllByLabelText(label)[0].parentElement);
      fireEvent.click(await screen.findByText(text, {selector: "li *, li"}));
    };
    afterEach(() => {
      answers.getScreenOption = [];
      answers.ProSel = [];
    });

    it("renders it for everyone by default, as the edited scope is everyone", async () => {
      await open();
      expect(loads()[0]).toMatchObject({subjectType: "everyone", subjectId: null});
    });

    it("reloads it for the profile of the edited scope once the profile is chosen", async () => {
      await open();
      fireEvent.click(screen.getByText("SCR_PER_SCOPE_PROFILE"));
      expect(loads()).toHaveLength(1);
      await choose("PARAMETER_PROFILE", "Admin");
      await waitFor(() => expect(loads()).toHaveLength(2));
      expect(loads()[1]).toMatchObject({subjectType: "profile", subjectId: 7});
    });

    it("keeps showing the subject chosen in View as when the edited scope changes", async () => {
      await open();
      fireEvent.click(screen.getByText("SCR_PER_SCOPE_PROFILE"));
      await choose("SCR_PER_VIEW_AS", "SCR_PER_SCOPE_EVERYONE");
      await choose("PARAMETER_PROFILE", "Admin");
      await act(async () => {});
      expect(loads()).toHaveLength(1);
    });
  });

  describe("overview mode", () => {
    const text = value => ({value});
    const stored = (ruleId, extra) => ({ruleId: text(ruleId), component: text("Btn"), attribute: text("label"), value: text("Add"), scopeType: text("everyone"),
      profileId: text(""), profileName: text(""), userId: text(""), userName: text(""), problem: text(""), duplicateGroup: text(""), ...extra});
    const calls = target => global.fetch.mock.calls.filter(([url]) => String(url).includes(target)).map(([, options]) => JSON.parse(options.body));
    const open = async () => {
      answers.getScreenStructure = [{path: "0", parent: "", position: 0, elementType: "Button", id: "Btn", label: "New user", configurable: "true"}];
      answers.getScreenConfigurationOverview = [
        stored(1, {}), stored(2, {value: text("Create"), scopeType: text("user"), userId: text("5"), userName: text("ana")}),
        stored(3, {component: text("Gone"), problem: text("missingElement")})];
      renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
      await act(async () => {});
      fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Sc"}});
      fireEvent.click(await screen.findByText("Scr", {selector: "li *, li"}));
      fireEvent.click(await screen.findByText("SCR_PER_MODE_OVERVIEW"));
    };
    afterEach(() => {
      delete answers.getScreenStructure;
      delete answers.getScreenConfigurationOverview;
    });

    it("lists what each subject sees per element and option, and the rules that no longer work", async () => {
      await open();
      expect(await screen.findByText('Button "New user" · label')).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_SCOPE_EVERYONE → Add")).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_SCOPE_USER ana → Create")).toBeInTheDocument();
      expect(screen.getAllByText("SCR_PER_PROBLEMS")).not.toHaveLength(0);
      expect(screen.getByText("SCR_PER_DELETE")).toBeInTheDocument();
    });

    it("opens the element in the editor at the scope of the row that was clicked", async () => {
      await open();
      fireEvent.click(await screen.findByText("SCR_PER_SCOPE_USER ana → Create"));

      await waitFor(() => expect(calls("getScreenElementAttributes")).toEqual([expect.objectContaining({screen: "Scr", component: "Btn"})]));
      expect(screen.getByLabelText("PARAMETER_USER")).toHaveValue("ana");
      expect(screen.queryByText("SCR_PER_OVERVIEW_TITLE")).not.toBeInTheDocument();
    });

    it("deletes a rule that no longer works through the update maintain", async () => {
      await open();
      fireEvent.click(await screen.findByText("SCR_PER_DELETE"));

      await waitFor(() => expect(calls("updateScreenConfiguration")).toHaveLength(1));
      expect(calls("updateScreenConfiguration")[0]).toMatchObject({"GrdScrCnf-RowTyp": ["DELETE"], IdeAweScrCnf: [3]});
    });
  });

  describe("history, export and import", () => {
    const text = value => ({value});
    const audit = (ruleId, action, valueBefore, valueAfter, extra) => ({date: text("09/11/2026 10:30:00"), operator: text("ana"), action: text(action), ruleId: text(ruleId),
      component: text("Btn"), attribute: text("label"), screen: text("Scr"), scopeType: text("everyone"), profileId: text(""), profileName: text(""), userId: text(""),
      userName: text(""), value: text(valueAfter || valueBefore), valueBefore: text(valueBefore), valueAfter: text(valueAfter), undoable: text(true), ...extra});
    const calls = target => global.fetch.mock.calls.filter(([url]) => String(url).includes(target)).map(([, options]) => JSON.parse(options.body));
    const open = async () => {
      answers.getScreenStructure = [{path: "0", parent: "", position: 0, elementType: "Button", id: "Btn", label: "New user", configurable: "true"}];
      answers.ProSel = [{value: 7, label: "ADM - Admin", Acr: "ADM"}];
      answers.getScreenConfigurationHistory = [audit(2, "changed", "a", "b"), audit(1, "created", "", "x", {undoable: text(false)})];
      answers.getScreenConfigurationExport = [{component: text("Btn"), attribute: text("label"), value: text("b"), scopeType: text("everyone"), profile: text(""), user: text("")}];
      answers.getScreenImportUsers = [{value: text(5), login: text("ana")}];
      renderWithProviders(<AweScreenPersonalizer/>, {preloadedState});
      await act(async () => {});
      fireEvent.change(screen.getByLabelText("PARAMETER_SCREEN"), {target: {value: "Sc"}});
      fireEvent.click(await screen.findByText("Scr", {selector: "li *, li"}));
      // The elements of the screen are what an import is compared with
      await waitFor(() => expect(calls("getScreenStructure")).toHaveLength(1));
      await act(async () => {});
    };
    afterEach(() => ["getScreenStructure", "ProSel", "getScreenConfigurationHistory", "getScreenConfigurationExport", "getScreenImportUsers"].forEach(key => delete answers[key]));

    it("lists the changes newest first with Undo only where it is allowed, and undoes one through the update maintain", async () => {
      await open();
      fireEvent.click(await screen.findByText("SCR_PER_MODE_HISTORY"));

      expect(await screen.findByText("a → b")).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_HISTORY_CREATED")).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_CHANGED_LATER")).toBeInTheDocument();
      fireEvent.click(screen.getByText("SCR_PER_UNDO"));

      await waitFor(() => expect(calls("updateScreenConfiguration")).toHaveLength(1));
      expect(calls("updateScreenConfiguration")[0]).toMatchObject({"GrdScrCnf-RowTyp": ["UPDATE"], IdeAweScrCnf: [2], Val: ["a"]});
    });

    it("says Earlier value not recorded for the first change of a rule that existed before the history", async () => {
      await open();
      answers.getScreenConfigurationHistory = [audit(2, "changed", "", "b", {undoable: text(false)})];
      fireEvent.click(await screen.findByText("SCR_PER_MODE_HISTORY"));

      expect(await screen.findByText("SCR_PER_HISTORY_UNRECORDED")).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_HISTORY_CHANGED_TO")).toBeInTheDocument();
      expect(screen.queryByText("SCR_PER_CHANGED_LATER")).not.toBeInTheDocument();
    });

    it("downloads the rules of the screen as a file named after it", async () => {
      global.URL.createObjectURL = jest.fn(() => "blob:x");
      global.URL.revokeObjectURL = jest.fn();
      const click = jest.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
      await open();
      fireEvent.click(await screen.findByText("SCR_PER_EXPORT"));

      await waitFor(() => expect(click).toHaveBeenCalled());
      const blob = global.URL.createObjectURL.mock.calls[0][0];
      expect(JSON.parse(await new Promise(resolve => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.readAsText(blob);
      }))).toEqual({screen: "Scr", version: 1, rules: [{component: "Btn", attribute: "label", value: "b", scope: "everyone"}]});
      click.mockRestore();
    });

    // The Import link opens a popover with the drop zone; the file is chosen there
    const openImport = async () => {
      fireEvent.click(await screen.findByText("SCR_PER_IMPORT"));
      return screen.findByText("SCR_PER_IMPORT_TITLE");
    };
    const chooseFile = async file => {
      await openImport();
      fireEvent.change(screen.getByLabelText("SCR_PER_IMPORT_FILE"), {target: {files: [file]}});
    };
    const validFile = () => new File([JSON.stringify({screen: "Scr", version: 1, rules: [{component: "Btn", attribute: "label", value: "n", scope: "everyone"}]})], "Scr.json");

    it("opens a popover on Import and closes it with Esc, giving the focus back to the link", async () => {
      await open();
      await openImport();
      expect(screen.getByText("SCR_PER_IMPORT_HINT")).toBeInTheDocument();
      fireEvent.keyDown(document, {key: "Escape", code: "Escape"});
      await waitFor(() => expect(screen.queryByText("SCR_PER_IMPORT_TITLE")).not.toBeInTheDocument());
      expect(screen.getByText("SCR_PER_IMPORT").closest("button")).toHaveFocus();
    });

    it("closes the popover and previews the import when a file is chosen", async () => {
      await open();
      await chooseFile(validFile());
      expect(await screen.findByText("SCR_PER_IMPORT_PREVIEW")).toBeInTheDocument();
      await waitFor(() => expect(screen.queryByText("SCR_PER_IMPORT_TITLE")).not.toBeInTheDocument());
    });

    it("closes the popover and previews the import when a file is dropped on the drop zone", async () => {
      await open();
      await openImport();
      fireEvent.drop(screen.getByText("SCR_PER_IMPORT_DROP").closest(".scr-per-dropzone"), {dataTransfer: {files: [validFile()]}});
      expect(await screen.findByText("SCR_PER_IMPORT_PREVIEW")).toBeInTheDocument();
      await waitFor(() => expect(screen.queryByText("SCR_PER_IMPORT_TITLE")).not.toBeInTheDocument());
    });

    it("previews an import and applies it, as inserts and updates only, on Apply import", async () => {
      await open();
      const file = new File([JSON.stringify({screen: "Scr", version: 1, rules: [
        {component: "Btn", attribute: "label", value: "n", scope: "profile", profile: "ADM"}, {component: "Gone", attribute: "label", value: "n", scope: "everyone"}]})], "Scr.json");
      await chooseFile(file);

      expect(await screen.findByText("SCR_PER_IMPORT_PREVIEW")).toBeInTheDocument();
      expect(screen.getByText("SCR_PER_IMPORT_SKIPPED")).toBeInTheDocument();
      expect(calls("updateScreenConfiguration")).toHaveLength(0);
      fireEvent.click(screen.getByText("SCR_PER_IMPORT_APPLY"));

      await waitFor(() => expect(calls("updateScreenConfiguration")).toHaveLength(1));
      expect(calls("updateScreenConfiguration")[0]).toMatchObject({"GrdScrCnf-RowTyp": ["INSERT"], IdePro: [7], Val: ["n"]});
      await waitFor(() => expect(screen.queryByText("SCR_PER_IMPORT_APPLY")).not.toBeInTheDocument());
    });

    it("reads the history again after Apply import so the imported changes show without changing mode", async () => {
      await open();
      fireEvent.click(await screen.findByText("SCR_PER_MODE_HISTORY"));
      await screen.findByText("a → b");
      const before = calls("getScreenConfigurationHistory").length;
      const file = new File([JSON.stringify({screen: "Scr", version: 1, rules: [{component: "Btn", attribute: "label", value: "n", scope: "everyone"}]})], "Scr.json");
      await chooseFile(file);
      fireEvent.click(await screen.findByText("SCR_PER_IMPORT_APPLY"));

      await waitFor(() => expect(calls("getScreenConfigurationHistory").length).toBeGreaterThan(before));
    });

    it("discards the preview on Cancel without sending anything", async () => {
      await open();
      const file = new File([JSON.stringify({screen: "Scr", version: 1, rules: [{component: "Btn", attribute: "label", value: "n", scope: "everyone"}]})], "Scr.json");
      await chooseFile(file);
      fireEvent.click(await screen.findByText("SCR_PER_IMPORT_CANCEL"));

      expect(screen.queryByText("SCR_PER_IMPORT_APPLY")).not.toBeInTheDocument();
      expect(calls("updateScreenConfiguration")).toHaveLength(0);
    });
  });
});
