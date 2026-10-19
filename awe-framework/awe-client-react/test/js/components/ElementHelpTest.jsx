import React, {Suspense} from 'react';
import {act, cleanup, fireEvent, screen, waitFor} from '@testing-library/react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweInputCheckbox from "../../../src/criteria/AweInputCheckbox";
import AweInputRadio from "../../../src/criteria/AweInputRadio";
import AweButtonCheckbox from "../../../src/criteria/AweButtonCheckbox";
import AweButtonRadio from "../../../src/criteria/AweButtonRadio";
import AweInputDateRange from "../../../src/criteria/AweInputDateRange";
import AweInputUploader from "../../../src/criteria/AweInputUploader";

// help and help-image of the screen contract are shown, with a tooltip, in every element that accepts them
describe('awe-react-client/test/js/components/ElementHelpTest.jsx', () => {

  afterEach(cleanup);

  const criterion = (attributes = {}, values = []) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      item: {
        address: {component: 'item', view: 'report'},
        model: {values},
        attributes: {label: "Label", readonly: false, visible: true, ...attributes},
        validationRules: {}
      }
    }
  });

  const hover = async (target, text) => {
    expect(target).not.toBeNull();
    fireEvent.mouseEnter(target);
    return screen.findByText(text, {}, {timeout: 3000});
  };

  const options = [{value: "A", label: "Option A"}, {value: "B", label: "Option B"}];

  it('shows the help of a checkbox', async () => {
    renderWithProviders(<AweInputCheckbox id="item"/>, {preloadedState: criterion({help: "Checkbox help"})});
    expect(document.querySelector('.help-icon')).not.toBeNull();
    expect(await hover(document.querySelector('.help-target-item'), "Checkbox help")).not.toBeNull();
  });

  it('shows the help of a radio button', async () => {
    renderWithProviders(<AweInputRadio id="item"/>, {preloadedState: criterion({help: "Radio help", group: "g"})});
    expect(document.querySelector('.help-icon')).not.toBeNull();
    expect(await hover(document.querySelector('.help-target-item'), "Radio help")).not.toBeNull();
  });

  it('shows the help image of a checkbox', async () => {
    renderWithProviders(<AweInputCheckbox id="item"/>, {preloadedState: criterion({helpImage: "img/help.png"})});
    fireEvent.mouseEnter(document.querySelector('.help-target-item'));
    await waitFor(() => expect(document.querySelector('.p-tooltip img')).not.toBeNull(), {timeout: 3000});
  });

  it('shows the help of a button checkbox', async () => {
    renderWithProviders(<AweButtonCheckbox id="item"/>, {preloadedState: criterion({help: "Button checkbox help"}, options)});
    expect(await hover(document.querySelector('.help-target-item'), "Button checkbox help")).not.toBeNull();
  });

  it('shows the help of a button radio', async () => {
    renderWithProviders(<AweButtonRadio id="item"/>, {preloadedState: criterion({help: "Button radio help"}, options)});
    expect(await hover(document.querySelector('.help-target-item'), "Button radio help")).not.toBeNull();
  });

  it('shows the help of a date range', async () => {
    renderWithProviders(<AweInputDateRange id="item"/>, {preloadedState: criterion({help: "Range help"})});
    expect(await hover(document.querySelector('.help-target-item'), "Range help")).not.toBeNull();
  });

  it('shows the help of an uploader', async () => {
    renderWithProviders(<AweInputUploader id="item"/>, {preloadedState: criterion({help: "Uploader help"})});
    expect(await hover(document.querySelector('.help-target-item'), "Uploader help")).not.toBeNull();
  });

  describe('grid columns', () => {
    const renderGrid = (columnModel) => renderWithProviders(<Suspense fallback={<div>Loading...</div>}>
      <AweGrid id="grid"/>
    </Suspense>, {
      preloadedState: {
        settings: DEFAULT_SETTINGS,
        screen: {size: {width: 800, height: 600}},
        components: {
          grid: {
            address: {component: 'grid', view: 'report'},
            screen: {breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
            model: {values: [{id: "1", name: "n1", other: "o1"}]},
            attributes: {columnModel, headerModel: [], buttonModel: []},
            specificAttributes: {sort: []}
          }
        }
      }
    });

    it('shows the help of a column in its header', async () => {
      renderGrid([
        {name: "name", label: "Name", help: "Name help"},
        {name: "other", label: "Other"}
      ]);
      expect(await hover(document.querySelector('.help-target-grid-name'), "Name help")).not.toBeNull();
      expect(document.querySelector('.help-target-grid-other')).toBeNull();
    });

    it('shows the help image of a column in its header', async () => {
      renderGrid([{name: "name", label: "Name", helpImage: "img/help.png"}]);
      await act(async () => {
        fireEvent.mouseEnter(document.querySelector('.help-target-grid-name'));
      });
      await waitFor(() => expect(document.querySelector('.p-tooltip img')).not.toBeNull(), {timeout: 3000});
    });
  });
});
