import {render} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnImageTest.jsx', () => {

  it('renders Column Image component', () => {
    render(Columns({
      component: 'image',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'image', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // fails
    expect(document.querySelector("img")).not.toBeNull();
  });
});
