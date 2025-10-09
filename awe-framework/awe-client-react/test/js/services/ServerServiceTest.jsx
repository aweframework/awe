import React from 'react';
import useServerService from "../../../src/services/ServerService";
import {renderWithProviders} from "../test-utils";
import {waitFor} from "@testing-library/react";

describe('awe-react-client/test/js/services/ServerServiceTest.jsx', function () {
  let service;
  let props;
  let store;
  let dispatchSpy;

  function TestHarness() {
    service = useServerService();
    return null;
  }

  beforeEach(function () {
    const rendered = renderWithProviders(<TestHarness />, {spyDispatch: true});
    store = rendered.store;
    props = {
      settings: {}
    };
    dispatchSpy = store.dispatch;
  });

  it('should launch a server call', async function () {
    spyOn(window, "fetch").and.returnValue(Promise.resolve({
      headers: {
        get: () => 'application/json;charset=UTF-8'
      },
      status: 200,
      ok: true,
      json: () => Promise.resolve([])
    }));

    await service.callServer({parameters: {}, target: "", address: {}}, [], props);
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalled();
    });
  });

});
