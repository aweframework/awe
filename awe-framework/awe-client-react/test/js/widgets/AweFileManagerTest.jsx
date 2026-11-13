import React from 'react';
import {renderWithProviders} from "../test-utils";
import AweFileManager from "../../../src/widgets/AweFileManager";

describe('awe-react-client/test/js/widgets/AweFileManagerTest.jsx', () => {

  it('renders AWE File Manager widget', () => {
    renderWithProviders(<AweFileManager id="file-manager"/>, {});

    expect(document.querySelector("#file-manager")).not.toBeNull();
    expect(document.querySelector("iframe.expand")).not.toBeNull();
  });

  it('renders AWE File Manager widget without id', () => {
    renderWithProviders(<AweFileManager/>, {});

    expect(document.querySelector("iframe.expand")).not.toBeNull();
  });
});
