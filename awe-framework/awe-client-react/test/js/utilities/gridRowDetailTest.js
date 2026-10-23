import { getRowDetailInstanceId, getRowDetailParameters, renderRowDetail } from "../../../src/utilities/gridRowDetail";

describe("awe-react-client/test/js/utilities/gridRowDetailTest.js", () => {
  it("replaces each placeholder with the field of the row", () => {
    expect(renderRowDetail("Name: {name} ({code})", { name: "Ann", code: 7 })).toBe("Name: Ann (7)");
  });

  it("keeps the text of a template without placeholders", () => {
    expect(renderRowDetail("Plain text", { name: "Ann" })).toBe("Plain text");
  });

  it("uses the label of a cell object, else its value", () => {
    const row = { status: { value: "A", label: "Active" }, type: { value: "T1" }, zero: { value: 0, label: null } };
    expect(renderRowDetail("{status}|{type}|{zero}", row)).toBe("Active|T1|0");
  });

  it("renders an empty text for missing, null and undefined fields", () => {
    expect(renderRowDetail("[{missing}][{empty}][{undef}][{cell}]",
      { empty: null, undef: undefined, cell: { value: null } })).toBe("[][][][]");
  });

  it("renders the fields of hidden columns, since the row keeps every field of the datalist", () => {
    expect(renderRowDetail("Notes: {HiddenNotes}", { id: "1", HiddenNotes: "Secret" })).toBe("Notes: Secret");
  });

  it("keeps HTML-like text as literal text", () => {
    expect(renderRowDetail("<b>{name}</b>", { name: "<img src=x onerror=alert(1)>" }))
      .toBe("<b><img src=x onerror=alert(1)></b>");
  });

  it("keeps line breaks", () => {
    expect(renderRowDetail("Line 1: {a}\nLine 2: {b}", { a: "x", b: "y" })).toBe("Line 1: x\nLine 2: y");
  });

  it("keeps unclosed and invalid braces as text", () => {
    expect(renderRowDetail("open {name", { name: "Ann" })).toBe("open {name");
    expect(renderRowDetail("close name}", { name: "Ann" })).toBe("close name}");
    expect(renderRowDetail("empty {} and {two words}", { name: "Ann" })).toBe("empty {} and {two words}");
    expect(renderRowDetail("{{name}}", { name: "Ann" })).toBe("{Ann}");
    expect(renderRowDetail("{name} {", { name: "Ann" })).toBe("Ann {");
  });

  it("does not read inherited properties of the row", () => {
    expect(renderRowDetail("[{constructor}][{toString}]", {})).toBe("[][]");
  });

  it("renders an empty text for an empty template or row", () => {
    expect(renderRowDetail(undefined, { name: "Ann" })).toBe("");
    expect(renderRowDetail(null, null)).toBe("");
    expect(renderRowDetail("{name}", undefined)).toBe("");
  });

  it("reads a long template in linear time", () => {
    const template = "{".repeat(50000) + "x";
    expect(renderRowDetail(template, {})).toBe(template);
  });

  it("names the instance of a row detail component after the component and the parent row", () => {
    expect(getRowDetailInstanceId("GrdOrders", 7)).toBe("GrdOrders-7");
    expect(getRowDetailInstanceId("ButOrdSee", "r1")).toBe("ButOrdSee-r1");
  });

  it("sends every field of the parent row as a single value, cell objects as their value, without the internal ones", () => {
    const row = { id: 7, name: "Chair", hidden: null, code: { value: "C7", label: "Chair code" }, selected: true, $row: { editing: false } };

    expect(getRowDetailParameters("GrdProducts", row)).toEqual({
      "GrdProducts.row.id": 7,
      "GrdProducts.row.name": "Chair",
      "GrdProducts.row.hidden": null,
      "GrdProducts.row.code": "C7"
    });
  });
});
