import React, {Profiler} from "react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGridContainer from "../../../src/components/AweGridContainer";

describe("awe-react-client/test/js/performance/GridContainerMeasureTest.jsx", () => {
  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    screen: {size: {width: 100, height: 100}}
  };

  afterEach(() => jest.restoreAllMocks());

  it("mounts in one commit without measuring its own height", () => {
    const measured = [];
    jest.spyOn(Element.prototype, "clientHeight", "get").mockImplementation(function () {
      measured.push(this);
      return 0;
    });
    let commits = 0;

    const {container} = renderWithProviders(
      <Profiler id="grid-container" onRender={() => commits++}>
        <AweGridContainer><div>rows</div></AweGridContainer>
      </Profiler>, {preloadedState});

    const gridContainer = container.querySelector(".grid-container");
    expect(gridContainer).not.toBeNull();
    expect(measured).not.toContain(gridContainer);
    expect(commits).toBe(1);
  });
});
