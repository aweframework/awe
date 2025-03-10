import {render, screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnSuggestTest.jsx', () => {

  it('renders Column Suggest component', () => {
    render(Columns({
      component: 'suggest',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'suggest', view: 'report', column: 'column', row: 'row'},
      placeholder: "Suggest test",
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // check component
    expect(screen.getByPlaceholderText("Suggest test")).toBeDefined();
  });
});
