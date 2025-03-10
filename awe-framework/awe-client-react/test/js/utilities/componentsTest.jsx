import {
  checkModelIsEmpty,
  checkModelIsUnchanged,
  checkModelIsUpdated,
  deleteFile,
  getComponentData,
  getFormValues,
  getFormValuesForPrinting,
  getInitialFileData,
  getSelectedValues,
  initialSuggest,
  parseRule,
  parseValidationRules,
  suggest,
  uploadFile
} from "../../../src/utilities/components";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

describe('awe-react-client/test/js/utilities/componentsTest.jsx', () => {
  const address = {component: "tutu", view: "lala"};
  const getDataListActions = () => ([{
    "type": "fill",
    "parameters": {
      "datalist": {
        "total": 1,
        "page": 1,
        "records": 1,
        "rows": [{"label": "test", "id": 1, "value": 1, "nom": "test"}]
      }
    }
  }, {"type": "end-load", "parameters": {}}]);
  let component;
  let abortController;
  let grid;
  let pivotTable;
  let criterion;
  let tab;
  let props;
  let numeric;
  let time;
  let checkbox;

  beforeEach(function () {
    abortController = {abort: jasmine.createSpy('abort'), signal: {aborted: false}};
    component = {
      suggesting: false,
      props: {
        serverAction: "serverAction", targetAction: "targetAction", settings: DEFAULT_SETTINGS, strict: true,
        keepModel: jasmine.createSpy("keepModel")
      },
      autocomplete: {
        hide: jasmine.createSpy("hide")
      },
      abortController: abortController
    };
    props = {
      updateSettings: jasmine.createSpy('updateSettings'),
      acceptAction: jasmine.createSpy('accept'),
      t: (v) => v,
      settings: {}
    };
    grid = {
      address: {component: "grid", view: "report"},
      model: {
        values: [
          {id: 1, Col1: "Value11", Col2: "Value21", selected: true},
          {id: 2, Col1: "Value12", Col2: "Value22", $row: {operation: "UPDATE"}}
        ]
      },
      storedModel: {
        values: [
          {id: 1, Col1: "Value11", Col2: "Value21", selected: true},
          {id: 2, Col1: "Value12", Col2: "Value22", $row: {operation: "UPDATE"}}
        ]
      },
      attributes: {
        id: "grid",
        component: "grid",
        columnModel: [{name: "Col1", label: "Col1", sendable: true}, {name: "Col2", label: "Col2"}]
      }
    };
    pivotTable = {
      address: {component: "pivotTable", view: "report"},
      model: {values: [{"Col1": "Value1"}]},
      storedModel: {values: [{"Col1": "Value1"}]},
      attributes: {id: "pivotTable", component: "grid", checkEmpty: true}
    };
    criterion = {
      address: {component: "criterion", view: "report"},
      model: {values: [{"value": 1, label: "Value1", selected: true}]},
      storedModel: {values: [{"value": 1, label: "Value1", selected: true}]},
      attributes: {id: "criterion", component: "text", checkEmpty: true}
    };
    numeric = {
      address: {component: "numeric", view: "report"},
      model: {values: [{"value": 11231, selected: true}]},
      storedModel: {values: [{"value": 11231, selected: true}]},
      attributes: {id: "numeric", component: "numeric", numberFormat: {vMin: '-999999', mDec: 1, aSign: '$', aSep: ',', pSign: 'p', aPad: true}, checkEmpty: true}
    };
    time = {
      address: {component: "time", view: "report"},
      model: {values: [{"value": "12:31:22", "label": "12:31:22", selected: true}]},
      storedModel: {values: [{"value": "12:31:22", "label": "12:31:22", selected: true}]},
      attributes: {id: "time", component: "time", checkEmpty: true}
    };
    checkbox = {
      address: {component: "checkbox", view: "report"},
      model: {values: [{"value": 1, selected: true}]},
      storedModel: {values: [{"value": 2, selected: true}]},
      attributes: {id: "checkbox", component: "checkbox"}
    };
    tab = {
      address: {component: "tab", view: "report"},
      model: {values: [{"value": 1, label: "Tab1", selected: false}, {"value": 2, label: "Tab2", selected: true}, {"value": 3, label: "Tab3", selected: false}]},
      storedModel: {values: [{"value": 1, label: "Tab1", selected: false}, {"value": 2, label: "Tab2", selected: true}, {"value": 3, label: "Tab3", selected: false}]},
      attributes: {id: "tab", component: "tab", checkEmpty: true}
    };
  });

  it('should parse a rule from object', () => {
    const rule = {required: true};
    expect(parseRule(rule, address)).toEqual(rule);
  });

  it('should parse a rule from string', () => {
    expect(parseRule("required", address)).toEqual({required: true});
  });

  it('should parse a rule from string as object', () => {
    expect(parseRule("{min: 0, max: 100, step: 0.01, precision: 2, aSign:' £', pSign:'s', aPad:true}", address))
      .toEqual({min: 0, max: 100, step: 0.01, precision: 2, aSign: ' £', pSign: 's', aPad: true});
  });

  it('should parse an invalid rule', () => {
    expect(parseRule("required {min: 0, max: 100, step: 0.01, precision: 2, aSign:' £', pSign:'s', aPad:true}", address))
      .toEqual({});
  });

  it('should parse a rule list from object', () => {
    const rules = {min: 0, max: 100, step: 0.01, precision: 2, aSign: ' £', pSign: 's', aPad: true};
    expect(parseValidationRules(rules, address)).toEqual(rules);
  });

  it('should parse a rule list from string', () => {
    expect(parseValidationRules("required number", address)).toEqual({required: true, number: true});
  });

  it('should parse a rule list from string as object', () => {
    expect(parseValidationRules("{min: 0, max: 100, step: 0.01, precision: 2, aSign:' £', pSign:'s', aPad:true}", address))
      .toEqual({min: 0, max: 100, step: 0.01, precision: 2, aSign: ' £', pSign: 's', aPad: true});
  });

  it('should suggest a text', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: () => getDataListActions()
    }));

    // Define setState
    component.setState = (params) => {
      expect(params).toEqual({suggestions: [{"label": "test", "id": 1, "value": 1, "nom": "test"}]});
      done();
    };
    suggest(component, {},"test");
  });

  it('should suggest a text non strict', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: getDataListActions
    }));

    // Define setState
    component.props.strict = false;
    component.setState = (params) => {
      expect(params).toEqual({
        suggestions: [
          {"label": "test", "id": 1, "value": 1, "nom": "test"},
          {"label": "te", "value": "te"}]
      });
      done();
    };
    suggest(component, {},"te");
  });

  it('should suggest a text non strict with the same label', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: getDataListActions
    }));

    // Define setState
    component.props.strict = false;
    component.setState = (params) => {
      expect(params).toEqual({suggestions: [{"label": "test", "id": 1, "value": 1, "nom": "test"}]});
      done();
    };
    suggest(component, {},"test");
  });

  it('should suggest a text aborting the previous one', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: getDataListActions
    }));

    // Define setState
    component.suggesting = true;
    component.setState = (params) => {
      expect(abortController.abort).toHaveBeenCalled();
      expect(params).toEqual({suggestions: [{"label": "test", "id": 1, "value": 1, "nom": "test"}]});
      done();
    };
    suggest(component, {},"test");
  });

  it('should suggest a text aborted', () => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: getDataListActions
    }));

    // Define setState
    component.abortController.signal.aborted = true;
    suggest(component, {},"test");
  });

  it('should suggest a text without data', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      ok: true,
      status: 200,
      statusText: 'HTTP/1.1 200 OK',
      json: () => [],
      text: () => ""
    }));

    // Define setState
    component.setState = (params) => {
      expect(params).toEqual({suggestions: []});
      done();
    };
    suggest(component, {},"test");
  });

  it('should suggest not strict a text with bad data', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: () => [{type: 'fill', parameters: {datalist: {}}}]
    }));

    // Define setState
    component.props.strict = false;
    component.setState = (params) => {
      expect(params).toEqual({suggestions: [{"label": "test", "value": "test"}]});
      done();
    };
    suggest(component, {}, "test");
  });

  it('should suggest a text with error in feedback', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'plain/text;charset=UTF-8'
      },
      status: 500,
      ok: false,
      statusText: 'HTTP/1.1 500 INTERNAL ERROR',
      text: () => ""
    }));
    spyOn(console, "debug").and.callFake(() => done());
    suggest(component, {}, "test");
  });


  it('should fill initial target', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: getDataListActions
    }));

    // Define setState
    component.props.strict = false;
    component.props.updateModelWithDependencies = (address, data) => {
      //console.info(data);
      expect(data).toEqual({
        values: [{"label": "test", "id": 1, "value": 1, "nom": "test", selected: true}]
      });
      done();
    };
    initialSuggest(component, "1");
  });

  it('should fill initial target with bad data', (done) => {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      statusText: 'HTTP/1.1 200 OK',
      json: () => [{type: 'fill', parameters: {datalist: {}}}]
    }));

    // Define setState
    component.props.updateModelWithDependencies = (address, data) => {
      expect(data).toEqual({
        values: []
      });
      done();
    };
    initialSuggest(component, "test");
  });

  it('should get initial file data with empty filedata', () => {
    const uploadComponent = {
      setState: jasmine.createSpy("setState"),
      props: {
        address: {},
        addActionsTop: jasmine.createSpy("addActionsTop"),
        destination: null,
        settings: {}
      }
    };

    getInitialFileData(uploadComponent, "");

    expect(uploadComponent.setState).toHaveBeenCalledTimes(1);
    expect(uploadComponent.props.addActionsTop).not.toHaveBeenCalled();
  });


  it('should upload a file', () => {
    const uploadComponent = {
      setState: jasmine.createSpy("setState"),
      props: {
        settings: {}
      }
    };

    const uploader = {
      xhr: {
        setRequestHeader: jasmine.createSpy("setRequestHeader")
      },
      formData: {
        set: jasmine.createSpy("setFormData")
      }
    };
    uploadFile(uploadComponent, uploader);

    expect(uploadComponent.setState).toHaveBeenCalledTimes(1);
  });

  it('should delete an uploaded file', () => {
    const uploadComponent = {
      setState: jasmine.createSpy("setState"),
      props: {
        address: {},
        addActionsTop: jasmine.createSpy("addActionsTop"),
        destination: null,
        settings: {}
      }
    };

    deleteFile(uploadComponent, "filename");

    expect(uploadComponent.setState).toHaveBeenCalledTimes(1);
    expect(uploadComponent.props.addActionsTop).toHaveBeenCalledTimes(1);
  });

  it('should get grid data', () => {
    let data = getComponentData(grid, props, false);
    //console.info(data);
    expect(data).toEqual({Col1: ["Value11"], "Col1.selected": "Value11", grid: [1]});
  });

  it('should get multioperation grid data', () => {
    let data = getComponentData({...grid, attributes: {...grid.attributes, multioperation: true}}, props, false);
    //console.info(data);
    expect(data).toEqual({Col1: ["Value12"], "Col1.selected": "Value11", grid: [2], "grid-RowTyp":["UPDATE"], "Col1.editing": null, "grid.editing": [ ]});
  });

  it('should get grid data for printing', () => {
    grid.attributes.sendAll = true;
    let data = getComponentData(grid, props, true);
    //console.info(data);
    expect(data).toEqual({
      Col1: [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
      "Col1.selected": "Value11",
      "grid.data": {
        visibleColumns: [
          {
            name: 'Col1',
            label: 'Col1',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          },
          {
            name: 'Col2',
            label: 'Col2',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          }
        ]
      },
      grid: [1, 2]
    });
  });

  it('should get grid data from pivot table', () => {
    let data = getComponentData(pivotTable, props, false);
    //console.info(data);
    expect(data).toEqual({pivotTable: []});
  });

  it('should get grid data for printing from pivot table', () => {
    let data = getComponentData(pivotTable, props, true);
    //console.info(data);
    expect(data).toEqual({pivotTable: [], "pivotTable.data": {visibleColumns: []}});
  });

  it('should get form values for printing', () => {
    let data = getFormValuesForPrinting({...props, components: {grid, pivotTable, numeric, checkbox, time}});
    //console.info(data);
    expect(data).toEqual({
      Col1: [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
      "Col1.selected": "Value11",
      "grid.data": {visibleColumns: [
          {
            name: 'Col1',
            label: 'Col1',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          },
          {
            name: 'Col2',
            label: 'Col2',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          }
        ]},
      grid: [1],
      pivotTable: [],
      "pivotTable.data": {visibleColumns: []},
      numeric: 11231,
      "numeric.data": { text: '$11,231.0' },
      checkbox: 1,
      "checkbox.data": { text: '1' },
      time: '12:31:22',
      "time.data": { text: '12:31:22' }
    });
  });

  it('should get form values with duplicates', () => {
    let data = getFormValues({...props, components: {grid, pivotTable, grid2: {...grid}}});
    //console.info(data);
    expect(data).toEqual({Col1: ["Value11"], "Col1.selected": "Value11", grid: [1], pivotTable: []});
  });

  it('should get form values for printing with duplicates', () => {
    let data = getFormValuesForPrinting({...props, components: {grid, pivotTable, grid2: {...grid}, numeric, checkbox, time}});
    //console.info(data);
    expect(data).toEqual({
      Col1: [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
      "Col1.selected": "Value11",
      "grid.data": {visibleColumns: [
          {
            name: 'Col1',
            label: 'Col1',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          },
          {
            name: 'Col2',
            label: 'Col2',
            type: undefined,
            component: undefined,
            width: undefined,
            charlength: undefined,
            align: undefined
          }
        ]},
      grid: [1],
      pivotTable: [],
      "pivotTable.data": {visibleColumns: []},
      numeric: 11231,
      "numeric.data": { text: '$11,231.0' },
      checkbox: 1,
      "checkbox.data": { text: '1' },
      time: '12:31:22',
      "time.data": { text: '12:31:22' }
    });
  });

  it('should get form values in empty components', () => {
    let data = getFormValues({});
    //console.info(data);
    expect(data).toEqual({});
  });

  it('should get selected values for an empty criterion', () => {
    let data = getSelectedValues({});
    //console.info(data);
    expect(data).toEqual([]);
  });

  it('should get criterion data', () => {
    let data = getComponentData(criterion, props, false);
    //console.info(data);
    expect(data).toEqual({criterion: 1});
  });

  it('should get criterion data for printing', () => {
    let data = getComponentData(criterion, props, true);
    //console.info(data);
    expect(data).toEqual({criterion: 1, "criterion.data": {text: "Value1"}});
  });

  it('should get criterion data for printing without label', () => {
    let data = getComponentData({
      ...criterion,
      model: {values: [{value: "eepa", selected: true}, {value: null, selected: true}, {value: 1, selected: false}]}
    }, props, true);
    //console.info(data);
    expect(data).toEqual({criterion: ["eepa", null], "criterion.data": {text: "eepa"}});
  });

  it('should get component data for other type', () => {
    let data = getComponentData({attributes: {component: "other"}}, props, true);
    //console.info(data);
    expect(data).toEqual({});
  });

  it('should get component data with empty component', () => {
    let data = getComponentData({}, props, true);
    //console.info(data);
    expect(data).toEqual({"undefined.data": {text: ""}});
  });

  it('should get tab data', () => {
    let data = getComponentData(tab, props, false);
    //console.info(data);
    expect(data).toEqual({tab: 2});
  });

  it('should get tab data for printing', () => {
    let data = getComponentData(tab, props, true);
    //console.info(data);
    expect(data).toEqual({tab: 2, "tab.data": {text: "Tab2", all:[{"value": 1, label: "Tab1", selected: false}, {"value": 2, label: "Tab2", selected: true}, {"value": 3, label: "Tab3", selected: false}]}});
  });

  it('should get tab data for printing without label', () => {
    let data = getComponentData({
      ...tab,
      model: {values: [{value: "eepa", selected: true}, {value: null, selected: false}, {value: 1, selected: false}]}
    }, props, true);
    //console.info(data);
    expect(data).toEqual({tab: "eepa", "tab.data": {text: "eepa", all:[{value: "eepa", selected: true}, {value: null, selected: false}, {value: 1, selected: false}]}});
  });

  it('should get tab data for empty tab', () => {
    let data = getComponentData({
      attributes: {component: "tab", id: "tab"}
    }, props, true);
    //console.info(data);
    expect(data).toEqual({tab: null, "tab.data": {text: "", all: []}});
  });

  it('should check if model is empty (true)', () => {
    expect(checkModelIsEmpty(props)).toEqual(true);
  });

  it('should check if model is empty (false)', () => {
    expect(checkModelIsEmpty({
      ...props,
      components: { grid, pivotTable, numeric, criterion, tab, checkbox }
    })).toEqual(false);
  });

  it('should check if model is updated (false)', () => {
    expect(checkModelIsUpdated(props)).toEqual(false);
  });

  it('should check if model is updated (true)', () => {
    expect(checkModelIsUpdated({
      ...props,
      components: { grid, pivotTable, numeric, criterion, tab, checkbox }
    })).toEqual(true);
  });

  it('should check if model is unchanged (true)', () => {
    expect(checkModelIsUnchanged(props)).toEqual(true);
  });

  it('should check if model is unchanged (false)', () => {
    expect(checkModelIsUnchanged({
      ...props,
      components: { grid, pivotTable, numeric, criterion, tab, checkbox }
    })).toEqual(false);
  });
});
