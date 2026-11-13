import {cleanup} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

const editor = component => renderWithProviders(Columns({
  component,
  model: {values: []},
  numberFormat: {},
  address: {component: "Grd", view: "report", column: "col", row: "row"},
  settings: {language: "en"},
  placeholder: "placeholder"
}, {value: "secret"}, {}, true));

describe("awe-react-client/test/js/columns/ColumnPasswordTest.jsx", () => {
  afterEach(cleanup);

  // A password cell of a grid is never a login: the browser must not fill a saved credential in it
  it("asks the browser not to autofill saved credentials in the password editor of a cell", () => {
    const {container} = editor("password");

    expect(container.querySelector("input[type='password']").getAttribute("autocomplete")).toBe("new-password");
  });

  it("keeps the text editor of a cell without autocomplete", () => {
    const {container} = editor("text");

    expect(container.querySelector("input[type='text']").hasAttribute("autocomplete")).toBe(false);
  });
});
