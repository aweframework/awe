import {render} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnLinkTest.jsx', () => {

  it('renders Column Link component', () => {
    render(Columns({
      component: 'link',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'link', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // fails
    expect(document.querySelector("a")).not.toBeNull();
  });
});
