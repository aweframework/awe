import {render} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnColorTest.jsx', () => {

  it('renders Column Color component', () => {
    render(Columns({
      component: 'color',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'color', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // fails
    expect(document.querySelector("button")).not.toBeNull();
  });
});
