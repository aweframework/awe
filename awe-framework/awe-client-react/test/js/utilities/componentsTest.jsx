import {
  checkModelIsEmpty,
  checkModelIsUnchanged,
  checkModelIsUpdated,
  classNames, clickDropdown,
  fixController,
  fixModel,
  fixSelectedModel,
  getAddressType,
  getCheckboxData,
  getComponentData,
  getComponentId,
  getCriterionData,
  getCriterionDataAsList,
  getCriterionPrintData,
  getDependencyComponentId,
  getSelectedValues,
  getSpecificAttributes,
  getTabPrintData,
  getTriggerId,
  inspectComponentStructure,
  parseRule,
  parseValidationRules,
} from "../../../src/utilities/components";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {getDataDependingOnList, getVisibleTextData} from "../../../src/utilities";

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
  let picklist1;
  let picklist2;
  let picklist3;
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
        keepModel: jasmine.createSpy("keepModel"),
        address: {},
        addActionsTop: jasmine.createSpy("addActionsTop"),
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
    picklist1 = {
      address: {component: "picklist1", view: "report"},
      model: {values: [{"value": 1, label: "Value1", selected: true}]},
      storedModel: {values: [{"value": 1, label: "Value1", selected: true}]},
      attributes: {id: "picklist1", component: "picklist", checkEmpty: true}
    };
    picklist2 = {
      address: {component: "picklist2", view: "report"},
      model: {values: [{"value": 1, label: "Value1", selected: false}]},
      storedModel: {values: [{"value": 1, label: "Value1"}]},
      attributes: {id: "picklist2", component: "picklist", checkEmpty: true}
    };
    picklist3 = {
      address: {component: "picklist3", view: "report"},
      model: {values: [{"value": 1, label: "Value1", selected: true}, {"value": 2, label: "Value2", selected: true}, {"value": 3, label: "Value3"}, {"value": 4, label: "Value4", selected: true}]},
      storedModel: {values: [{"value": 1, label: "Value1", selected: true}, {"value": 2, label: "Value2", selected: true}, {"value": 3, label: "Value3"}, {"value": 4, label: "Value4", selected: true}]},
      attributes: {id: "picklist3", component: "picklist", checkEmpty: true}
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

  /*it('should suggest a text', (done) => {
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
      json: () => [{type: 'message'}]
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
  });*/

  it('should get grid data', () => {
    let data = getComponentData(grid, props, false);
    expect(data).toEqual({Col1: ["Value11"], "Col1.selected": "Value11", grid: [1], "grid.selected": 1, "grid.selectedRowAddress": {component: "grid", view: "report", row: 1}});
  });

  it('should get multioperation grid data', () => {
    let data = getComponentData({...grid, attributes: {...grid.attributes, multioperation: true}}, props, false);
    expect(data).toEqual({Col1: ["Value12"], "Col1.selected": "Value11", grid: [2], "grid-RowTyp":["UPDATE"], "Col1.editing": null, "grid.editing": null, "grid.selected": 1, "grid.selectedRowAddress": {component: "grid", view: "report", row: 1}});
  });

  it('should get grid data for printing', () => {
    grid.attributes.sendAll = true;
    let data = getComponentData(grid, props, true);
    expect(data).toEqual({
      Col1: ["Value11","Value12"],
      "Col1.selected": "Value11",
      "Col1.data": [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
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
      grid: [1, 2],
      "grid.selected": 1,
      "grid.selectedRowAddress": {component: "grid", view: "report", row: 1}
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

  /*it('should get form values for printing', () => {
    let data = getFormValuesForPrinting({...props, components: {grid, pivotTable, numeric, checkbox, time}});
    //console.info("LO QUE VALE:", data);
    console.info("LO QUE DEBERIA VALER", {
      Col1: ["Value11", "Value12"],
      "Col1.data": [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
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
    expect(data).toEqual({
      Col1: ["Value11", "Value12"],
      "Col1.data": [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
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
    console.info(data);
    expect(data).toEqual({
      Col1: ["Value11", "Value12"],
      "Col1.data": [{value: "Value11", label: "Value11"}, {value: "Value12", label: "Value12"}],
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
  });*/

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

  it('should get picklist data as list', () => {
    let data = getComponentData(picklist1, props, false);
    //console.info(data);
    expect(data).toEqual({ picklist1: [ 1 ] });

    data = getComponentData(picklist2, props, false);
    //console.info(data);
    expect(data).toEqual({ picklist2: [] });

    data = getComponentData(picklist3, props, false);
    //console.info(data);
    expect(data).toEqual({ picklist3: [ 1, 2, 4 ] });
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

  // Tests for getDataDependingOnList
  it('should get data depending on list length - empty list', () => {
    expect(getDataDependingOnList([])).toEqual(null);
  });

  it('should get data depending on list length - single item', () => {
    expect(getDataDependingOnList([5])).toEqual(5);
  });

  it('should get data depending on list length - multiple items', () => {
    expect(getDataDependingOnList([1, 2, 3])).toEqual([1, 2, 3]);
  });

  // Tests for classNames
  it('should join class names from strings', () => {
    expect(classNames('class1', 'class2')).toEqual('class1 class2');
  });

  it('should join class names from numbers', () => {
    expect(classNames(1, 2)).toEqual('1 2');
  });

  it('should join class names from arrays', () => {
    expect(classNames(['class1', 'class2'])).toEqual('class1 class2');
  });

  it('should join class names from objects', () => {
    expect(classNames({ class1: true, class2: false, class3: true })).toEqual('class1 class3');
  });

  it('should join class names from mixed types', () => {
    expect(classNames('class1', 2, ['class3', 'class4'], { class5: true, class6: false })).toEqual('class1 2 class3 class4 class5');
  });

  it('should handle empty or falsy values in classNames', () => {
    expect(classNames('', null, undefined, false, 0)).toEqual('0');
  });

  // Tests for getAddressType
  it('should identify cell address type', () => {
    expect(getAddressType({ view: 'view1', component: 'comp1', column: 'col1', row: 'row1' })).toEqual('cell');
  });

  it('should identify column address type', () => {
    expect(getAddressType({ view: 'view1', component: 'comp1', column: 'col1' })).toEqual('column');
  });

  it('should identify component address type', () => {
    expect(getAddressType({ view: 'view1', component: 'comp1' })).toEqual('component');
  });

  it('should identify view address type', () => {
    expect(getAddressType({ view: 'view1' })).toEqual('view');
  });

  it('should identify invalid address type', () => {
    expect(getAddressType({})).toEqual('invalid');
    expect(getAddressType(null)).toEqual('invalid');
    expect(getAddressType('string')).toEqual('invalid');
  });

  // Tests for getComponentId
  it('should get component id for cell address', () => {
    expect(getComponentId({view: "report", component: 'comp1', row: 'row1', column: 'col1' })).toEqual('comp1-row1-col1');
  });

  it('should get component id for column address', () => {
    expect(getComponentId({view: "report",  component: 'comp1', column: 'col1' })).toEqual('comp1-col1');
  });

  it('should get component id for component address', () => {
    expect(getComponentId({view: "report",  component: 'comp1' })).toEqual('comp1');
  });

  it('should return null for invalid address', () => {
    expect(getComponentId({})).toEqual(null);
  });

  // Tests for getDependencyComponentId
  it('should get dependency component id with all parameters', () => {
    expect(getDependencyComponentId(
      {view: "report",  component: 'comp1' },
      {view: "report",  column: 'col1', row: 'row1', index: 'idx1' }
    )).toEqual('comp1-col1-row1-idx1');
  });

  it('should get dependency component id with some parameters', () => {
    expect(getDependencyComponentId(
      {view: "report",  component: 'comp1' },
      {view: "report",  column: 'col1' }
    )).toEqual('comp1-col1');
  });

  it('should get dependency component id with no parameters', () => {
    expect(getDependencyComponentId(
      {view: "report",  component: 'comp1' },
      {}
    )).toEqual('comp1');
  });

  // Tests for getTriggerId
  it('should get trigger id with alias', () => {
    const trigger = { id: 'trigger1', alias: 'aliasName' };
    const dependency = { address: { view: 'view1' } };
    expect(getTriggerId(trigger, dependency)).toEqual('aliasName');
  });

  it('should get trigger id without alias', () => {
    const trigger = { id: 'trigger1', event: 'click' };
    const dependency = { address: { view: 'view1' } };
    expect(getTriggerId(trigger, dependency)).toEqual('trigger1-click');
  });

  it('should get trigger id with column', () => {
    const trigger = { id: 'trigger1', column1: 'col1' };
    const dependency = { address: { view: 'view1' } };
    expect(getTriggerId(trigger, dependency)).toEqual('trigger1-col1');
  });

  it('should get trigger id with row', () => {
    const trigger = { id: 'trigger1' };
    const dependency = { address: { view: 'view1', row: 'row1' } };
    expect(getTriggerId(trigger, dependency)).toEqual('trigger1');
  });

  // Tests for fixSelectedModel
  it('should fix selected model with object', () => {
    const selected = { value: 'test' };
    expect(fixSelectedModel(selected)).toEqual(selected);
  });

  it('should fix selected model with string', () => {
    expect(fixSelectedModel('test')).toEqual({ value: 'test' });
  });

  it('should fix selected model with number', () => {
    expect(fixSelectedModel(123)).toEqual({ value: '123' });
  });

  // Tests for fixModel
  it('should fix model for grid', () => {
    const model = {
      selected: [1, 2],
      values: [
        { id: '1', name: 'Item 1' },
        { id: '2', name: 'Item 2' },
        { id: '3', name: 'Item 3' }
      ]
    };

    const result = fixModel(model, true);
    expect(result.values[0].selected).toBe(true);
    expect(result.values[1].selected).toBe(true);
    expect(result.values[2].selected).toBe(false);
  });

  it('should fix model for non-grid', () => {
    const model = {
      selected: [{ value: '1' }, '2'],
      values: [
        { value: '1', label: 'Item 1' },
        { value: '2', label: 'Item 2' },
        { value: '3', label: 'Item 3' }
      ]
    };

    const result = fixModel(model, false);
    expect(result.values[0].selected).toBe(true);
    expect(result.values[1].selected).toBe(true);
    expect(result.values[2].selected).toBe(false);
  });

  it('should fix model with empty values but selected items', () => {
    const model = {
      selected: [{ value: '1', label: 'Item 1' }],
      values: []
    };

    const result = fixModel(model, false);
    expect(result.values.length).toBe(1);
    expect(result.values[0]).toEqual({ value: '1', label: 'Item 1', selected: true });
  });

  // Tests for getSpecificAttributes
  it('should get specific attributes with default values', () => {
    const controller = { max: 10 };
    const settings = { recordsPerPage: 20 };

    expect(getSpecificAttributes(controller, false, settings)).toEqual({
      max: 10,
      rows: 10,
      sort: [],
      page: 1,
      first: 0
    });
  });

  it('should get specific attributes with loadAll', () => {
    const controller = { max: 10, loadAll: true };
    const settings = { recordsPerPage: 20 };

    expect(getSpecificAttributes(controller, false, settings)).toEqual({
      max: 0,
      rows: 10,
      sort: [],
      page: 1,
      first: 0
    });
  });

  it('should get specific attributes with settings fallback', () => {
    const controller = {};
    const settings = { recordsPerPage: 20 };

    expect(getSpecificAttributes(controller, false, settings)).toEqual({
      max: 20,
      rows: 20,
      sort: [],
      page: 1,
      first: 0
    });
  });

  // Tests for fixController
  it('should fix controller for non-grid', () => {
    const controller = {
      numberFormat: "{ vMin: '-999999', mDec: 1 }",
      size: 'medium'
    };
    const settings = {
      numericOptions: { aSep: ',' },
      defaultComponentSize: 'small'
    };

    const result = fixController(controller, false, settings);
    expect(result.numberFormat).toEqual({ aSep: ',', vMin: '-999999', mDec: 1 });
    expect(result.size).toEqual('medium');
  });

  // Tests for inspectComponentStructure
  it('should inspect component structure', () => {
    const element = {
      id: 'parent',
      elementType: 'Container',
      elementList: [
        { id: 'child1', elementType: 'Button' },
        { id: 'child2', elementType: 'Input' }
      ]
    };

    const result = inspectComponentStructure(element, [], {});
    expect(Object.keys(result).length).toBe(3);
    expect(result.parent).toEqual(['parent']);
    expect(result.child1).toEqual(['parent', 'child1']);
    expect(result.child2).toEqual(['parent', 'child2']);
  });

  it('should handle Dialog in component structure', () => {
    const element = {
      id: 'parent',
      elementType: 'Container',
      elementList: [
        { 
          id: 'dialog', 
          elementType: 'Dialog',
          elementList: [
            { id: 'dialogChild', elementType: 'Button' }
          ]
        }
      ]
    };

    const result = inspectComponentStructure(element, ['context'], {});
    expect(result.parent).toEqual(['context', 'parent']);
    expect(result.dialog).toEqual(['dialog']);
    expect(result.dialogChild).toEqual(['dialog', 'dialogChild']);
  });

  it('should handle TreeGrid in component structure', () => {
    const element = {
      id: 'grid',
      elementType: 'Grid',
      treegrid: true
    };

    const result = inspectComponentStructure(element, [], {});
    expect(element.elementType).toEqual('TreeGrid');
    expect(result.grid).toEqual(['grid']);
  });

  // Tests for getVisibleTextData
  it('should get visible text data', () => {
    const t = (text) => `Translated: ${text}`;
    expect(getVisibleTextData('Hello', t)).toEqual('Translated: Hello');
  });

  // Tests for getCriterionPrintData
  it('should get criterion print data for numeric', () => {
    const criterion = {
      attributes: {
        id: 'numCriterion',
        component: 'numeric',
        numberFormat: { mDec: 2 }
      }
    };
    const model = {
      values: [
        { value: 123.45, selected: true }
      ]
    };
    const props = { t: (text) => text };

    const result = getCriterionPrintData(criterion, model, props);
    expect('numCriterion.data' in result).toBeTrue();
    expect('text' in result['numCriterion.data']).toBeTrue();
  });

  it('should get criterion print data for time', () => {
    const criterion = {
      attributes: {
        id: 'timeCriterion',
        component: 'time'
      }
    };
    const model = {
      values: [
        { value: '12:34:56', selected: true }
      ]
    };
    const props = { t: (text) => text };

    const result = getCriterionPrintData(criterion, model, props);
    expect('timeCriterion.data' in result).toBeTrue();
    expect(result['timeCriterion.data'].text).toEqual('12:34:56');
  });

  it('should get criterion print data for default component', () => {
    const criterion = {
      attributes: {
        id: 'defaultCriterion',
        component: 'text'
      }
    };
    const model = {
      values: [
        { value: 'value1', label: 'Label 1', selected: true },
        { value: 'value2', label: 'Label 2', selected: true }
      ]
    };
    const props = { t: (text) => `T: ${text}` };

    const result = getCriterionPrintData(criterion, model, props);
    expect('defaultCriterion.data' in result).toBeTrue();
    expect('text' in result['defaultCriterion.data']).toBeTrue();
  });

  // Tests for getCheckboxData
  it('should get checkbox data', () => {
    const checkbox = {
      attributes: {
        id: 'checkboxId',
        component: 'checkbox'
      }
    };
    const model = {
      values: [
        { value: 1, selected: true }
      ]
    };

    const result = getCheckboxData(checkbox, model, {}, false);
    expect(result).toEqual({ checkboxId: 1 });
  });

  it('should get checkbox data with multiple selected values', () => {
    const checkbox = {
      attributes: {
        id: 'checkboxId',
        component: 'checkbox'
      }
    };
    const model = {
      values: [
        { value: 1, selected: true },
        { value: 2, selected: true }
      ]
    };

    const result = getCheckboxData(checkbox, model, {}, false);
    expect(result).toEqual({ checkboxId: [1, 2] });
  });

  it('should get checkbox data with no selected values', () => {
    const checkbox = {
      attributes: {
        id: 'checkboxId',
        component: 'checkbox'
      }
    };
    const model = {
      values: [
        { value: 1, selected: false }
      ]
    };

    const result = getCheckboxData(checkbox, model, {}, false);
    expect(result).toEqual({ checkboxId: 0 });
  });

  // Tests for getCriterionData
  it('should get criterion data', () => {
    const criterion = {
      attributes: {
        id: 'criterionId',
        component: 'text'
      }
    };
    const model = {
      values: [
        { value: 'value1', selected: true }
      ]
    };

    const result = getCriterionData(criterion, model, {}, false);
    expect(result).toEqual({ criterionId: 'value1' });
  });

  it('should get criterion data with multiple selected values', () => {
    const criterion = {
      attributes: {
        id: 'criterionId',
        component: 'text'
      }
    };
    const model = {
      values: [
        { value: 'value1', selected: true },
        { value: 'value2', selected: true }
      ]
    };

    const result = getCriterionData(criterion, model, {}, false);
    expect(result).toEqual({ criterionId: ['value1', 'value2'] });
  });

  it('should get criterion data with no selected values', () => {
    const criterion = {
      attributes: {
        id: 'criterionId',
        component: 'text'
      }
    };
    const model = {
      values: [
        { value: 'value1', selected: false }
      ]
    };

    const result = getCriterionData(criterion, model, {}, false);
    expect(result).toEqual({});
  });

  // Tests for getCriterionDataAsList
  it('should get criterion data as list', () => {
    const criterion = {
      attributes: {
        id: 'criterionId',
        component: 'picklist'
      }
    };
    const model = {
      values: [
        { value: 'value1', selected: true },
        { value: 'value2', selected: false },
        { value: 'value3', selected: true }
      ]
    };

    const result = getCriterionDataAsList(criterion, model, {}, false);
    expect(result).toEqual({ criterionId: ['value1', 'value3'] });
  });

  it('should get criterion data as list with no selected values', () => {
    const criterion = {
      attributes: {
        id: 'criterionId',
        component: 'picklist'
      }
    };
    const model = {
      values: [
        { value: 'value1', selected: false },
        { value: 'value2', selected: false }
      ]
    };

    const result = getCriterionDataAsList(criterion, model, {}, false);
    expect(result).toEqual({ criterionId: [] });
  });

  // Tests for clickDropdown
  it('should handle click on dropdown', () => {
    const dropdown = {
      toggle: jasmine.createSpy('toggle')
    };

    const dropdownProps = {
      address: { component: 'dropdown1', view: 'view1' },
      addActionsTop: jasmine.createSpy('addActionsTop'),
      updateModelWithDependencies: jasmine.createSpy('updateModelWithDependencies'),
      dispatch: jasmine.createSpy('dispatch'),
      actions: [
        { type: 'action1', parameters: {} },
        { type: 'action2', parameters: {} }
      ]
    };

    const event = { target: {} };

    clickDropdown(event, dropdownProps, dropdown);

    expect(dropdown.toggle).toHaveBeenCalledWith(event);
    expect(dropdownProps.updateModelWithDependencies).toHaveBeenCalledWith(
      dropdownProps.address, 
      { event: 'click' }
    );
    expect(dropdownProps.addActionsTop).toHaveBeenCalledWith([
      { ...dropdownProps.actions[0], address: dropdownProps.address },
      { ...dropdownProps.actions[1], address: dropdownProps.address }
    ]);
  });

  // Test for getTabPrintData
  it('should get tab print data', () => {
    const tab = {
      attributes: {
        id: 'tabId'
      }
    };
    const model = {
      values: [
        { value: 'tab1', label: 'Tab 1', selected: true },
        { value: 'tab2', label: 'Tab 2', selected: false }
      ]
    };
    const props = {
      t: (text) => `Translated: ${text}`
    };

    const result = getTabPrintData(tab, model, props);
    expect('tabId.data' in result).toBeTrue();
    expect(result['tabId.data'].text).toBe('Translated: Tab Translated: 1');
    expect('all' in result['tabId.data']).toBeTrue();
    expect(result['tabId.data'].all).toEqual(model.values);
  }); //X
});
