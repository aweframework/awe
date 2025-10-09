import React from 'react';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import useComponentService from "../../../src/services/ComponentService";
import {renderWithProviders} from "../test-utils";
import {waitFor} from "@testing-library/react";

let service;
let actions;

const preloadedState = {
  settings: DEFAULT_SETTINGS,
  components: {
    chart1: {
      address: {component: 'chart1', view: 'report'},
      model: {values: []},
      attributes: {
        style: "estilo-especifico",
        chartModel: {
          series: [
            {id: "tutu", data: []},
            {id: "lala", data: []},
            {id: "test", data: []}
          ]
        }
      },
      specificAttributes: {sort: []}
    }
  }
};

function TestHarness() {
  service = useComponentService();
  actions = service.getActions();
  return null;
}

describe('awe-react-client/test/js/services/ComponentServiceTest.jsx', () => {
  let props;
  let store;
  let dispatchSpy;


  beforeEach(function () {
    props = {...preloadedState};
    const rendered = renderWithProviders(<TestHarness />, { preloadedState, spyDispatch: true });
    store = rendered.store;
    dispatchSpy = store.dispatch;
  });

  it('should get all component actions', () => {
    expect(Object.keys(actions).length).toBe(21);
  });

  it('should add points to the chart serie', async () => {
    // Prepare
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {values: [{tutu1: 1, tutu2: 2}]}
    };

    // Test
    actions["add-points"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should add some series to a chart', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };

    // Test
    actions["add-chart-series"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should remove some series from a chart', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };

    // Test
    actions["remove-chart-series"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should replace all series from a chart', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };

    // Test
    actions["replace-chart-series"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should set pivot sorters', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {sorters: {}}
    };

    // Test
    actions["set-pivot-sorters"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should set pivot rows', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {rows: "tuut,lala"}
    };

    // Test
    actions["set-pivot-group-rows"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should set pivot columns', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {cols: "tuut,lala"}
    };

    // Test
    actions["set-pivot-group-cols"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should toggle menu', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {}
    };

    // Test
    actions["toggle-menu"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should toggle navbar', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {}
    };

    // Test
    actions["toggle-navbar"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

  it('should change menu', async () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {options: []}
    };

    // Test
    actions["change-menu"](action, props);

    // Verify
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

});
