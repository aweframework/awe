import React from "react";
import { act, cleanup, createEvent, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { ADD_ACTIONS_TOP } from "../../../src/redux/actions/actions";
import { updateModelWithDependencies } from "../../../src/redux/thunks/components";
import { renderWithProviders } from "../test-utils";
import AweButton from "../../../src/components/AweButton";
import AweChart from "../../../src/components/AweChart";
import AweInputText from "../../../src/criteria/AweInputText";
import AweSteps from "../../../src/components/AweSteps";
import AweTabs from "../../../src/components/AweTabs";
import { echarts } from "../../../src/utilities/echartsSetup";

jest.mock("../../../src/utilities/echartsSetup", () => ({
  echarts: { init: jest.fn() },
  getEChartsLocale: jest.requireActual("../../../src/utilities/echartsSetup").getEChartsLocale,
  isDarkTheme: jest.fn(() => false),
  renderSvg: jest.fn(() => "<svg>print</svg>")
}));

// The click event of the option goes through the thunk that stores it and checks the dependencies: it is spied
jest.mock("../../../src/redux/thunks/components", () => {
  const actual = jest.requireActual("../../../src/redux/thunks/components");
  return { ...actual, updateModelWithDependencies: jest.fn(actual.updateModelWithDependencies) };
});

const hook = (testId) => `[data-testid='${testId}']`;
const address = (component) => ({ component, view: "report" });
const OPTIONS = [{ id: "OptAdd" }, { id: "OptHidden" }];

// The component of the family plus the two options of its context menu (one of them hidden)
function stateOf(id, extra = {}, contextMenu = OPTIONS) {
  return {
    settings: DEFAULT_SETTINGS,
    actions: { running: false },
    screen: { size: { width: 1000, height: 1000 } },
    components: {
      [id]: {
        address: address(id),
        model: { values: [{ label: "one", value: "one", selected: true }, { label: "two", value: "two" }] },
        attributes: { label: "Label", visible: true, contextMenu, ...extra },
        elementList: [],
        actions: []
      },
      OptAdd: {
        address: address("OptAdd"),
        model: { values: [] },
        attributes: { label: "Add", visible: true, actions: [{ type: "do-add" }] }
      },
      OptHidden: {
        address: address("OptHidden"),
        model: { values: [] },
        attributes: { label: "Hidden", visible: false }
      }
    }
  };
}

const families = [
  ["criterion", "txt", () => <AweInputText id="txt" />, "[criterion-id='txt']"],
  ["button", "but", () => <AweButton id="but" />, hook("button")],
  ["tab", "tabs", () => <AweTabs id="tabs" />, ".p-tabmenu-container"],
  ["wizard", "steps", () => <AweSteps id="steps" />, ".p-steps-container"],
  ["chart", "chart", () => <AweChart id="chart" />, hook("chart")]
];

describe("awe-react-client/test/js/components/ContextMenuFamiliesTest.jsx", () => {
  beforeEach(() => {
    updateModelWithDependencies.mockClear();
    echarts.init.mockReset();
    echarts.init.mockImplementation(() => ({
      setOption: jest.fn(), resize: jest.fn(), dispatchAction: jest.fn(), getOption: jest.fn(() => ({})),
      dispose: jest.fn(), on: jest.fn()
    }));
  });
  afterEach(cleanup);

  const rightClick = async (element) => {
    const event = createEvent.contextMenu(element, { cancelable: true });
    await act(async () => {
      fireEvent(element, event);
    });
    return event;
  };

  describe.each(families)("%s", (family, id, render, selector) => {
    it("opens its options on right click and leaves out the hidden ones", async () => {
      const { container } = renderWithProviders(render(), { preloadedState: stateOf(id) });

      const event = await rightClick(container.querySelector(selector));

      expect(event.defaultPrevented).toBe(true);
      const menu = document.querySelector(hook("context-menu"));
      expect(menu).not.toBeNull();
      const links = Array.from(menu.querySelectorAll(hook("context-menu-link")));
      expect(links.map(link => link.getAttribute("option-id"))).toEqual(["OptAdd"]);
      expect(links[0].textContent).toBe("Add");
    });

    it("launches the actions of the option with the owner address and stores its click event", async () => {
      const { container, dispatchSpy } = renderWithProviders(render(), {
        preloadedState: stateOf(id), spyDispatch: true
      });
      await rightClick(container.querySelector(selector));

      await act(async () => {
        fireEvent.click(document.querySelector(`${hook("context-menu-link")}[option-id='OptAdd']`));
      });

      expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({
        type: ADD_ACTIONS_TOP, payload: [{ type: "do-add", address: address(id) }]
      }));
      expect(updateModelWithDependencies).toHaveBeenCalledWith(address("OptAdd"), { event: "click" });
    });

    it("keeps the browser menu when it declares no context menu", async () => {
      const { container } = renderWithProviders(render(), { preloadedState: stateOf(id, {}, []) });

      const event = await rightClick(container.querySelector(selector));

      expect(event.defaultPrevented).toBe(false);
      expect(document.querySelector(hook("context-menu"))).toBeNull();
    });

    it("keeps the browser menu when every option is hidden", async () => {
      const { container } = renderWithProviders(render(), {
        preloadedState: stateOf(id, {}, [{ id: "OptHidden" }])
      });

      const event = await rightClick(container.querySelector(selector));

      expect(event.defaultPrevented).toBe(false);
    });
  });
});
