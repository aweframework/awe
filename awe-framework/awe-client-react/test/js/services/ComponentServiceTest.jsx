import ComponentService from "../../../src/services/ComponentService";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

describe('awe-react-client/test/js/services/ComponentServiceTest.jsx', () => {
  const service = new ComponentService();

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


  beforeEach(function () {
  });

  it('should get all component actions', () => {
    let actions = service.getActions();
    expect(Object.keys(actions).length).toBe(21);
  });

  it('should add points to the chart serie', () => {
    // Prepare
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {values: [{tutu1: 1, tutu2: 2}]}
    };
    const props = {...preloadedState,
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onAddPoints(action, props);

    // Verify
    expect(props.updateModelWithDependencies).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should add some series to a chart', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onAddSeries(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should remove some series from a chart', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onRemoveSeries(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).not.toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should replace all series from a chart', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {series: [{id: "tutu", tutu2: 2, data}]}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onReplaceSeries(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should set pivot sorters', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {sorters: {}}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onSetPivotSorters(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should set pivot rows', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {rows: "tuut,lala"}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onSetPivotGroupRows(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should set pivot columns', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {cols: "tuut,lala"}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.onSetPivotGroupCols(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should toggle menu', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {}
    };
    const props = {...preloadedState,
      updateAttributes: jasmine.createSpy("updateAttributes"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.toggleMenu(action, props);

    // Verify
    expect(props.updateAttributes).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should toggle navbar', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {}
    };
    const props = {...preloadedState,
      acceptAction: jasmine.createSpy("acceptAction")
    };

    // Test
    service.toggleNavbar(action, props);

    // Verify
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should change menu', () => {
    // Prepare
    const data = [["a", 0], ["b", 1], ["c", 12]];
    const action = {
      address: {component: 'chart1', view: 'report'},
      parameters: {options: []}
    };
    const props = {...preloadedState,
      updateMenu: jasmine.createSpy("updateMenu"),
      acceptAction: jasmine.createSpy("acceptAction") };

    // Test
    service.changeMenu(action, props);

    // Verify
    expect(props.updateMenu).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

});
