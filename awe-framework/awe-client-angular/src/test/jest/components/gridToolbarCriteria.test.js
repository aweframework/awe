// Toolbar criteria of a grid (toolbar-criteria): criteria of any component that the grid shows at the end of its
// toolbar, just before its Columns button, as the directive of their component
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates} from "./gridTestUtils";
import {toolbarCriterionMarkup} from "../../../main/resources/js/awe/directives/gridToolbar";

const hook = id => `[data-testid='${id}']`;

describe("grid toolbar criteria", () => {
  let refs;
  let element;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(() => {
    element?.remove();
    element = null;
    cleanupHeavyComponentTest();
  });

  // A grid whose controller lists the toolbar criteria, and the switch registered as a criterion of the screen
  const mount = (grid = {}) => {
    const controllers = {
      Grid: {id: "Grid", visible: true, loading: false, targetAction: "getRows", max: 10, pagerValues: [10],
        columnModel: [{name: "name", label: "Name", sendable: true}], buttonModel: [], headerModel: [],
        toolbarCriteria: ["CrtGrp"], ...grid},
      CrtGrp: {id: "CrtGrp", component: "checkbox", style: "switch no-label", label: "Group by flow", visible: true,
        dependencies: [], contextMenu: []}
    };
    const models = {Grid: {page: 1, records: 0, total: 1, selected: [], values: []}, CrtGrp: {selected: 0, values: []}};
    inject(["$templateCache", cacheGridTemplates]);
    jest.spyOn(refs.$storage, "get").mockReturnValue({base: {}});
    jest.spyOn(refs.$control, "checkComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "checkOnlyComponent").mockReturnValue(true);
    jest.spyOn(refs.$control, "getAddressModel").mockImplementation(address => models[address.component]);
    jest.spyOn(refs.$control, "getAddressController").mockImplementation(address => controllers[address.component]);
    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    jest.spyOn(console, "error").mockImplementation(() => undefined);
    element = refs.$compile("<awe-grid grid-id='Grid'></awe-grid>")(refs.$rootScope.$new());
    $(document.body).append(element);
    refs.$rootScope.$digest();
    return element;
  };

  it("shows each toolbar criterion as the directive of its component, at the end of the toolbar before Columns", () => {
    const toolbar = mount().find(hook("grid-toolbar"));

    const criteria = toolbar.children(".grid-toolbar-criteria");
    expect(criteria.length).toBe(1);
    const input = criteria.find("input[type='checkbox'][role='switch']");
    expect(input.attr("id")).toBe("CrtGrp");
    expect(criteria.find(".lbl").text().trim()).toBe("Group by flow");
    expect(criteria.next()[0]).toBe(toolbar.find(hook("grid-columns-button"))[0]);
  });

  it("shows the toolbar for its criteria when the grid hides its buttons", () => {
    const toolbar = mount({showToolbar: false}).find(hook("grid-toolbar"));

    expect(toolbar.length).toBe(1);
    expect(toolbar.find(hook("grid-columns-button")).length).toBe(0);
    expect(toolbar.find(".grid-toolbar-criteria #CrtGrp").length).toBe(1);
  });

  it("draws nothing for a criterion that is not registered or has an unexpected component", () => {
    expect(toolbarCriterionMarkup("CrtGrp", undefined)).toBeNull();
    expect(toolbarCriterionMarkup("CrtGrp", {component: "checkbox\" onclick=\"x"})).toBeNull();
    expect(toolbarCriterionMarkup("CrtSel", {component: "select"}))
      .toBe("<awe-input-select input-select-id=\"CrtSel\"></awe-input-select>");
  });
});
