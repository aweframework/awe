import useFormService from "../../../src/services/FormService";
import {renderWithProviders} from "../test-utils";
import React from "react";

describe('awe-react-client/test/js/services/FormServiceTest.jsx', () => {
  let service;
  let actions;

  function TestHarness() {
    service = useFormService();
    actions = service.getActions();
    return null;
  }

  beforeEach(function () {
    renderWithProviders(<TestHarness />);
  });

  it('should get all form actions', () => {
    expect(Object.keys(actions).length).toBe(23);
  });

});
