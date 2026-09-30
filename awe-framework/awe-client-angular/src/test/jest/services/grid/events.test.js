import "./gridTestUtils";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/events.js', function () {
  let GridEvents, $log, writeText;

  function flushPromises() {
    return new Promise(resolve => setTimeout(resolve, 0));
  }

  function buildScope() {
    return {
      component: {
        constants: {SELECTED_TAIL: "_selected"},
        hideContextMenu: jest.fn(),
        controller: {columnModel: [{id: "name", label: "NAME"}]},
        getColumnPrintData: jest.fn().mockReturnValue({name_selected: ["Alice", "Bob"]})
      }
    };
  }

  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["GridEvents", "$log", function (_GridEvents_, _$log_) {
      GridEvents = _GridEvents_;
      $log = _$log_;
    }]);

    writeText = jest.fn();
    Object.defineProperty(navigator, "clipboard", {value: {writeText}, configurable: true, writable: true});
    jest.spyOn($log, "error").mockImplementation(() => null);
  });

  afterEach(function () {
    delete navigator.clipboard;
    jest.restoreAllMocks();
  });

  it('copies the selected rows to the clipboard and logs nothing on success', async function () {
    writeText.mockResolvedValue(undefined);

    GridEvents.onCopySelectedRowsToClipboard({}, buildScope());
    await flushPromises();

    expect(writeText).toHaveBeenCalledWith("NAME\nAlice\nBob");
    expect($log.error).not.toHaveBeenCalled();
  });

  it('logs the rejection when the clipboard write fails', async function () {
    const error = new Error("Write permission denied");
    writeText.mockRejectedValue(error);

    GridEvents.onCopySelectedRowsToClipboard({}, buildScope());
    await flushPromises();

    expect($log.error).toHaveBeenCalledTimes(1);
    expect($log.error).toHaveBeenCalledWith(expect.stringContaining("clipboard"), error);
  });
});
