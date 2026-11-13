import React from "react";
import {render, cleanup} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";

const address = {view: "report", component: "grid", column: "col", row: "1"};

const renderBar = (node, data, row) => render(<div>{Columns({component: "progress", address, ...node}, data, {}, false, row)}</div>);
const meter = (container) => container.querySelector("[role=meter]");
const fill = (container) => container.querySelector(".awe-data-bar-fill");

describe("awe-react-client/test/js/columns/ColumnProgressBarTest.jsx", () => {
  afterEach(cleanup);

  it("keeps the PrimeReact progress bar without bar attributes", () => {
    const {container} = renderBar({}, {value: 40});
    expect(container.querySelector(".p-progressbar")).not.toBeNull();
    expect(container.querySelector("[role=meter]")).toBeNull();
    expect(container.querySelector(".awe-data-bar")).toBeNull();
  });

  it("keeps the PrimeReact progress bar when the attributes are empty", () => {
    const {container} = renderBar({barSecondField: "", barStackFields: "", barLeafOnly: false}, {value: 40});
    expect(container.querySelector(".p-progressbar")).not.toBeNull();
  });

  it("draws the value on the scale from the zero", () => {
    const {container} = renderBar({barMax: 40}, {value: 4.13});
    expect(fill(container).style.left).toBe("0%");
    expect(fill(container).style.right).toBe("89.675%");
    expect(fill(container).className).not.toContain("awe-data-bar-negative");
    expect(container.querySelector(".awe-data-bar-axis")).toBeNull();
    expect(container.querySelector(".p-progressbar")).toBeNull();
  });

  it("reads the scale and the value from numeric strings", () => {
    const {container} = renderBar({barMin: "0", barMax: "40"}, {value: "4.13"});
    expect(fill(container).style.right).toBe("89.675%");
  });

  it("draws a negative value to the left of the zero mark", () => {
    const {container} = renderBar({barMin: -40, barMax: 40}, {value: -10});
    expect(fill(container).style.left).toBe("37.5%");
    // Placed by both edges, so the end on the zero is exactly where the axis is (no gap of a pixel between them)
    expect(fill(container).style.right).toBe("50%");
    expect(fill(container).className).toContain("awe-data-bar-negative");
    expect(container.querySelector(".awe-data-bar-axis").style.left).toBe("50%");
  });

  it("marks the fills of a signed scale so that the end on the zero is square", () => {
    const signed = renderBar({barMin: -40, barMax: 40, barSecondField: "t"}, {value: 10}, {t: -5}).container;
    signed.querySelectorAll(".awe-data-bar-fill").forEach(item => expect(item.className).toContain("awe-data-bar-signed"));
    cleanup();
    const plain = renderBar({barMax: 40, barSecondField: "t"}, {value: 10}, {t: 5}).container;
    plain.querySelectorAll(".awe-data-bar-fill").forEach(item => expect(item.className).not.toContain("awe-data-bar-signed"));
  });

  it("clamps the value to the scale", () => {
    const {container} = renderBar({barMax: 40}, {value: 90});
    expect([fill(container).style.left, fill(container).style.right]).toEqual(["0%", "0%"]);
  });

  it("falls back to 0..100 when the scale is not valid", () => {
    const {container} = renderBar({barMin: 50, barMax: 10}, {value: 25});
    expect(fill(container).style.right).toBe("75%");
    expect(meter(container).getAttribute("aria-valuemin")).toBe("0");
    expect(meter(container).getAttribute("aria-valuemax")).toBe("100");
  });

  it("draws nothing for an empty or non-numeric value", () => {
    [{value: ""}, {value: null}, {value: "abc"}, undefined].forEach(data => {
      const {container} = renderBar({barMax: 40}, data);
      expect(container.querySelector("[role=meter]")).toBeNull();
      expect(container.querySelector(".p-progressbar")).toBeNull();
      cleanup();
    });
  });

  it("tells the value as a meter", () => {
    const {container} = renderBar({barMin: -40, barMax: 40}, {value: -10, label: "-10,0 %"});
    const element = meter(container);
    expect(element.getAttribute("aria-valuemin")).toBe("-40");
    expect(element.getAttribute("aria-valuemax")).toBe("40");
    expect(element.getAttribute("aria-valuenow")).toBe("-10");
    expect(element.getAttribute("aria-valuetext")).toBe("-10,0 %");
    expect(element.getAttribute("title")).toBe("-10,0 %");
  });

  it("clamps the value of the meter to the scale", () => {
    const {container} = renderBar({barMax: 40}, {value: 90});
    expect(meter(container).getAttribute("aria-valuenow")).toBe("40");
    expect(meter(container).getAttribute("aria-valuetext")).toBe("90");
  });

  describe("number on top of the bar", () => {
    const value = (container) => container.querySelector(".awe-data-bar-value");

    it("shows the label of the value, or the value when there is no label", () => {
      expect(value(renderBar({barMax: 40}, {value: 4.13, label: "4,13 %"}).container).textContent).toBe("4,13 %");
      cleanup();
      expect(value(renderBar({barMax: 40}, {value: 4.13}).container).textContent).toBe("4.13");
    });

    it("keeps the number after the lanes so that it is drawn over them", () => {
      const {container} = renderBar({barMax: 40}, {value: 4});
      expect(meter(container).lastElementChild).toBe(value(container));
    });

    it("marks the number of a negative value", () => {
      expect(value(renderBar({barMin: -40, barMax: 40}, {value: -10}).container).className)
        .toContain("awe-data-bar-value-negative");
      cleanup();
      expect(value(renderBar({barMin: -40, barMax: 40}, {value: 10}).container).className)
        .not.toContain("awe-data-bar-value-negative");
    });

    it("shows only the main number when there is a second value or segments", () => {
      const second = renderBar({barSecondField: "target"}, {value: 30}, {target: 50}).container;
      expect(value(second).textContent).toBe("30");
      cleanup();
      const stacked = renderBar({barStackFields: "a"}, {value: 30}, {a: 10}).container;
      expect(value(stacked).textContent).toBe("30");
    });
  });

  describe("second value", () => {
    it("draws a second lane under the first one with the same scale and signed rules", () => {
      const {container} = renderBar({barMin: -40, barMax: 40, barSecondField: "target"}, {value: 20}, {target: -20});
      const second = container.querySelector(".awe-data-bar-lane-second .awe-data-bar-fill");
      expect(second.style.left).toBe("25%");
      expect(second.style.right).toBe("50%");
      expect(second.className).toContain("awe-data-bar-negative");
      expect(container.querySelectorAll(".awe-data-bar-lane")).toHaveLength(2);
      expect(container.querySelector(".awe-data-bar").className).toContain("awe-data-bar-two");
      expect(container.querySelectorAll(".awe-data-bar-axis")).toHaveLength(1);
    });

    it("tells both values", () => {
      const {container} = renderBar({barSecondField: "target"}, {value: 30, label: "30 %"}, {target: {value: 50, label: "50 %"}});
      expect(meter(container).getAttribute("aria-valuetext")).toBe("30 % / 50 %");
      expect(meter(container).getAttribute("title")).toBe("30 % / 50 %");
    });

    it("draws only the main bar when the second value is empty or not numeric", () => {
      ["", null, "abc", undefined].forEach(target => {
        const {container} = renderBar({barSecondField: "target"}, {value: 30}, {target});
        expect(container.querySelectorAll(".awe-data-bar-lane")).toHaveLength(1);
        expect(container.querySelector(".awe-data-bar").className).not.toContain("awe-data-bar-two");
        expect(meter(container).getAttribute("aria-valuetext")).toBe("30");
        cleanup();
      });
    });
  });

  describe("stacked segments", () => {
    const row = {a: 10, b: "20", c: -5, d: "x", e: 30};

    it("draws one segment per field from the zero", () => {
      const {container} = renderBar({barStackFields: "a, b"}, {value: 30}, row);
      const segments = container.querySelectorAll(".awe-data-bar-segment");
      expect(segments).toHaveLength(2);
      expect([segments[0].style.left, segments[0].style.right]).toEqual(["0%", "90%"]);
      expect([segments[1].style.left, segments[1].style.right]).toEqual(["10%", "70%"]);
      expect(segments[0].className).toContain("awe-data-bar-segment-0");
      expect(segments[1].className).toContain("awe-data-bar-segment-1");
      expect(fill(container)).toBeNull();
    });

    it("ignores negative and non-numeric fields and keeps the colour of the position", () => {
      const {container} = renderBar({barStackFields: "a,c,d,e"}, {value: 40}, row);
      const segments = container.querySelectorAll(".awe-data-bar-segment");
      expect(Array.from(segments).map(segment => segment.className)).toEqual([
        "awe-data-bar-segment awe-data-bar-segment-0", "awe-data-bar-segment awe-data-bar-segment-0"
      ]);
    });

    it("cuts the segments at the end of the scale", () => {
      const {container} = renderBar({barStackFields: "a,b,e", barMax: 40}, {value: 60}, row);
      const segments = container.querySelectorAll(".awe-data-bar-segment");
      expect(segments).toHaveLength(3);
      expect([segments[2].style.left, segments[2].style.right]).toEqual(["75%", "0%"]);
    });

    it("cycles the colours every three segments", () => {
      const fields = {f0: 1, f1: 1, f2: 1, f3: 1, f4: 1, f5: 1};
      const {container} = renderBar({barStackFields: Object.keys(fields).join(",")}, {value: 6}, fields);
      const segments = container.querySelectorAll(".awe-data-bar-segment");
      expect(segments[3].className).toContain("awe-data-bar-segment-0");
      expect(segments[5].className).toContain("awe-data-bar-segment-2");
    });

    it("tells the value with its parts", () => {
      const parts = {a: {value: 10, label: "10 u"}, b: {value: 20, label: "20 u"}};
      const {container} = renderBar({barStackFields: "a,b"}, {value: 30, label: "30 u"}, parts);
      expect(meter(container).getAttribute("aria-valuetext")).toBe("30 u (10 u + 20 u)");
    });
  });

  describe("leaf only", () => {
    it("renders an empty cell in a row that is not a leaf", () => {
      const {container} = renderBar({barLeafOnly: true}, {value: 30}, {$leaf: false});
      expect(container.firstChild.innerHTML).toBe("");
    });

    it("draws the bar of a leaf and of a flat grid row", () => {
      expect(meter(renderBar({barLeafOnly: true}, {value: 30}, {$leaf: true}).container)).not.toBeNull();
      cleanup();
      expect(meter(renderBar({barLeafOnly: true}, {value: 30}, {name: "flat"}).container)).not.toBeNull();
      cleanup();
      expect(meter(renderBar({barLeafOnly: true}, {value: 30}).container)).not.toBeNull();
    });

    it("draws any row when leaf only is not asked", () => {
      expect(meter(renderBar({barMax: 40}, {value: 30}, {$leaf: false}).container)).not.toBeNull();
    });
  });
});
