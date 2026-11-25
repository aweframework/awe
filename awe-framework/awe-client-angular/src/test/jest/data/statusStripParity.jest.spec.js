const fs = require("fs");
const path = require("path");

// The status strip rules are framework-agnostic and mirrored in the React client. Sonar does not count the AngularJS
// copy as duplicated code (sonar.cpd.exclusions in the root pom), so this test keeps both copies identical and, with
// that, both engines give the same items and the same summary for the same cell.
const ANGULAR_COPY = path.join(__dirname, "../../../main/resources/js/awe/services/grid/statusStrip.js");
const REACT_COPY = path.join(__dirname, "../../../../../awe-client-react/src/utilities/statusStrip.js");

describe("awe-client-angular/src/test/jest/data/statusStripParity.jest.spec.js", () => {
  it("keeps the status strip module identical to the React client copy", () => {
    expect(fs.readFileSync(ANGULAR_COPY, "utf8")).toBe(fs.readFileSync(REACT_COPY, "utf8"));
  });
});
