import {getRowDetailId, renderRowDetail} from "../../../../main/resources/js/awe/services/grid/rowDetail";

describe("awe-framework/awe-client-angular/src/main/resources/js/awe/services/grid/rowDetail.js", () => {
  it("replaces each placeholder with the field of the row", () => {
    expect(renderRowDetail("Name: {name} ({code})", {name: "Ann", code: 7})).toBe("Name: Ann (7)");
  });

  it("keeps the text of a template without placeholders", () => {
    expect(renderRowDetail("Plain text", {name: "Ann"})).toBe("Plain text");
  });

  it("uses the label of a cell object, else its value", () => {
    const row = {status: {value: "A", label: "Active"}, type: {value: "T1"}, zero: {value: 0, label: null}};
    expect(renderRowDetail("{status}|{type}|{zero}", row)).toBe("Active|T1|0");
  });

  it("renders an empty text for missing, null and undefined fields", () => {
    expect(renderRowDetail("[{missing}][{empty}][{undef}][{cell}]",
      {empty: null, undef: undefined, cell: {value: null}})).toBe("[][][][]");
  });

  it("renders the fields of hidden columns, since the row keeps every field of the datalist", () => {
    expect(renderRowDetail("Notes: {HiddenNotes}", {id: "1", HiddenNotes: "Secret"})).toBe("Notes: Secret");
  });

  it("keeps HTML-like text as literal text", () => {
    expect(renderRowDetail("<b>{name}</b>", {name: "<img src=x onerror=alert(1)>"}))
      .toBe("<b><img src=x onerror=alert(1)></b>");
  });

  it("keeps line breaks", () => {
    expect(renderRowDetail("Line 1: {a}\nLine 2: {b}", {a: "x", b: "y"})).toBe("Line 1: x\nLine 2: y");
  });

  it("keeps unclosed and invalid braces as text", () => {
    expect(renderRowDetail("open {name", {name: "Ann"})).toBe("open {name");
    expect(renderRowDetail("close name}", {name: "Ann"})).toBe("close name}");
    expect(renderRowDetail("empty {} and {two words}", {name: "Ann"})).toBe("empty {} and {two words}");
    expect(renderRowDetail("{{name}}", {name: "Ann"})).toBe("{Ann}");
    expect(renderRowDetail("{name} {", {name: "Ann"})).toBe("Ann {");
  });

  it("does not read inherited properties of the row", () => {
    expect(renderRowDetail("[{constructor}][{toString}]", {})).toBe("[][]");
  });

  it("renders an empty text for an empty template or row", () => {
    expect(renderRowDetail(undefined, {name: "Ann"})).toBe("");
    expect(renderRowDetail(null, null)).toBe("");
    expect(renderRowDetail("{name}", undefined)).toBe("");
  });

  it("reads a long template in linear time", () => {
    const template = "{".repeat(50000) + "x";
    expect(renderRowDetail(template, {})).toBe(template);
  });

  it("builds the identifier of the detail of a row from the grid and the row identifiers", () => {
    expect(getRowDetailId("GrdUsr", "r1")).toBe("GrdUsr-row-detail-r1");
    expect(getRowDetailId("GrdUsr", 7)).toBe("GrdUsr-row-detail-7");
  });
});
