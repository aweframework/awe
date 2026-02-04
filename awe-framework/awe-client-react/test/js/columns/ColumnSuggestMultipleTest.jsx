import {screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnSuggestMultipleTest.jsx', () => {
  it('renders Column Suggest Multiple component (editor)', () => {
    renderWithProviders(Columns({
      component: 'suggest-multiple',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'suggest-multiple', view: 'report', column: 'column', row: 'row'},
      placeholder: "Suggest multiple test",
      t: jasmine.createSpy("t"),
      settings: {}
    }, [
      {value: "test-a", label: "Test A", selected: true},
      {value: "test-b", label: "Test B", selected: true}
    ], {}, true));

    expect(screen.getByPlaceholderText("Suggest multiple test")).toBeDefined();
  });
});
