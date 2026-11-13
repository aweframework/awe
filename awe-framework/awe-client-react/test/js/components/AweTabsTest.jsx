import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {act, fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweTabs from "../../../src/components/AweTabs";

describe('awe-react-client/test/js/components/AweTabsTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      tabs: {
        address: {component: 'tabs', view: 'report'},
        model: {values: [{label: 'test', value: 'test', title: 'test',  icon: 'test',  unit: 'test', selected: true}]},
        elementList: [],
        attributes: {
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Tabs component', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState});

    // fails
    expect(document.querySelector("div.p-tabmenu")).not.toBeNull();
  });

  const withInvalidField = (error) => ({
    ...preloadedState,
    components: {
      tabs: {
        ...preloadedState.components.tabs,
        model: {values: [{label: 'one', value: 'one', selected: true}, {label: 'two', value: 'two'}]}
      },
      field: {
        address: {component: 'field', view: 'report'},
        context: {view: 'report', source: ['tabs', 'two', 'field']},
        model: {values: []},
        attributes: {error: error},
        validationRules: {}
      }
    }
  });

  it('marks the tab that contains an invalid field', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState: withInvalidField({message: 'Required'})});

    const tabs = [...document.querySelectorAll("[data-testid='tab']")];
    expect(tabs.map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "true"]);
    expect(tabs[1].classList.contains("p-tab-invalid")).toBe(true);
  });

  it('does not mark any tab when every field is valid', () => {
    renderWithProviders(<AweTabs id="tabs"/>, {preloadedState: withInvalidField(null)});

    const tabs = [...document.querySelectorAll("[data-testid='tab']")];
    expect(tabs.map(tab => tab.getAttribute("data-invalid"))).toEqual(["false", "false"]);
  });

  describe('page of the selected option (#906)', () => {
    // The containers are declared in another order than the options of the enum, and one has no option
    const containers = ['TabFirst', 'TabSecond', 'TabThird', 'TabHidden'].map(id => ({
      elementType: 'TabContainer', id, elementList: [{elementType: 'Tag', type: 'span', label: `Page of ${id}`, id: `Tag${id}`}]
    }));
    const withOptions = (options, selected) => ({
      ...preloadedState,
      components: {
        tabs: {
          ...preloadedState.components.tabs,
          model: {values: options.map(value => ({label: value, value, selected: value === selected}))}
        }
      }
    });
    const shownText = (container) => container.textContent;

    it.each([
      ['TabFirst', 'Page of TabFirst'],
      ['TabThird', 'Page of TabThird'],
      ['TabSecond', 'Page of TabSecond']
    ])('shows the container whose id is the selected value %s, whatever the order of the options', (selected, text) => {
      const {container} = renderWithProviders(<AweTabs id="tabs" elementList={containers}/>,
        {preloadedState: withOptions(['TabFirst', 'TabThird', 'TabSecond'], selected)});

      expect(shownText(container)).toContain(text);
      containers.filter(page => `Page of ${page.id}` !== text)
        .forEach(page => expect(shownText(container)).not.toContain(`Page of ${page.id}`));
    });

    it('shows no page for an option without container', () => {
      const {container} = renderWithProviders(<AweTabs id="tabs" elementList={containers}/>,
        {preloadedState: withOptions(['TabFirst', 'TabMissing'], 'TabMissing')});

      containers.forEach(page => expect(shownText(container)).not.toContain(`Page of ${page.id}`));
    });
  });

  describe('deferred loading (#824)', () => {
    const page = (id, attributes = {}) => ({
      address: {component: id, view: 'report'},
      context: {view: 'report', source: ['tabs', id]},
      model: {values: []},
      attributes,
      validationRules: {}
    });
    const pages = [{elementType: 'TabContainer', id: 'pageA', elementList: []}, {elementType: 'TabContainer', id: 'pageB', elementList: []}];
    const withPages = (selected, attributes) => ({
      ...preloadedState,
      components: {
        tabs: {
          ...preloadedState.components.tabs,
          model: {values: [{label: 'A', value: 'pageA', selected: selected === 'pageA'}, {label: 'B', value: 'pageB', selected: selected === 'pageB'}]}
        },
        pageA: page('pageA', attributes.pageA),
        pageB: page('pageB', attributes.pageB)
      }
    });
    const loaded = (store, id) => store.getState().components[id].attributes.deferredLoaded;

    it('asks for the data of the deferred page which is shown', () => {
      const {store} = renderWithProviders(<AweTabs id="tabs" elementList={pages}/>,
        {preloadedState: withPages('pageA', {pageA: {deferred: true}, pageB: {deferred: true}})});

      expect(loaded(store, 'pageA')).toBe(true);
    });

    it('waits for the pages which are not shown', () => {
      const {store} = renderWithProviders(<AweTabs id="tabs" elementList={pages}/>,
        {preloadedState: withPages('pageA', {pageA: {deferred: true}, pageB: {deferred: true}})});

      expect(loaded(store, 'pageB')).toBeUndefined();
    });

    it('does not ask for the data of a page which is not deferred', () => {
      const {store} = renderWithProviders(<AweTabs id="tabs" elementList={pages}/>,
        {preloadedState: withPages('pageA', {})});

      expect(loaded(store, 'pageA')).toBeUndefined();
    });
  });

  describe('maximize (#799)', () => {
    beforeEach(() => jest.useFakeTimers());
    afterEach(() => jest.useRealTimers());

    const settle = (time = 600) => act(() => {
      jest.advanceTimersByTime(time);
    });
    const button = () => document.querySelector("button.maximize-button");
    const container = () => document.querySelector("div.p-tabmenu-container");

    it('renders no maximize button when the tab is not maximizable', () => {
      renderWithProviders(<AweTabs id="tabs"/>, {preloadedState});

      expect(button()).toBeNull();
      expect(document.querySelector("[data-testid='tab-maximize-button']")).toBeNull();
    });

    it.each([true, "true"])('maximizes and restores the whole tab container with the button (maximize=%s)', (maximize) => {
      renderWithProviders(<AweTabs id="tabs" maximize={maximize}/>, {preloadedState});
      expect(container().classList).not.toContain("maximized");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_MAXIMIZE");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#maximize");

      fireEvent.click(button());
      settle();
      expect(container().classList).toContain("maximized");
      expect(container().style.position).toBe("fixed");
      expect(button().getAttribute("title")).toBe("SCREEN_TEXT_RESTORE");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#minimize");

      fireEvent.click(button());
      settle();
      expect(container().classList).not.toContain("maximized");
      expect(container().style.position).toBe("");
      expect(button().querySelector("i > svg.awe-icon > use").getAttribute("href")).toBe("/icons/lucide-sprite.svg#maximize");
    });

    it('maximizes over the maximize target and minimizes its siblings', () => {
      renderWithProviders(
        <div className="maximize-target" id="target">
          <AweTabs id="tabs" maximize={true}/>
          <div id="sibling">other</div>
        </div>, {preloadedState});
      jest.spyOn(document.getElementById("target"), "getBoundingClientRect")
        .mockReturnValue({top: 40, left: 10, width: 800, height: 500});
      const sibling = document.getElementById("sibling");

      fireEvent.click(button());
      settle();
      expect([container().style.top, container().style.left, container().style.width, container().style.height])
        .toEqual(["40px", "10px", "800px", "500px"]);
      expect(sibling.classList).toContain("minimized");

      fireEvent.click(button());
      settle();
      expect(sibling.classList).not.toContain("minimized");
    });

    it('exposes the tab container and its maximize button as test hooks that follow the state', () => {
      renderWithProviders(<AweTabs id="tabs" maximize={true}/>, {preloadedState});
      const hook = document.querySelector("[data-testid='tabs']");
      expect(hook).toBe(container());
      expect(hook.getAttribute("data-testid-owner")).toBe("tabs");
      expect(hook.getAttribute("data-maximized")).toBe("false");
      expect(button().getAttribute("data-testid")).toBe("tab-maximize-button");

      fireEvent.click(button());
      settle();
      expect(hook.getAttribute("data-maximized")).toBe("true");

      fireEvent.click(button());
      settle();
      expect(hook.getAttribute("data-maximized")).toBe("false");
    });
  });
});
