const fs = require("fs");
const path = require("path");

// The sparkline geometry is framework-agnostic and mirrored in the React client. Sonar does not count the AngularJS
// copy as duplicated code (sonar.cpd.exclusions in the root pom), so this test keeps both copies identical.
const ANGULAR_COPY = path.join(__dirname, "../../../main/resources/js/awe/services/chart/sparkline.js");
const REACT_COPY = path.join(__dirname, "../../../../../awe-client-react/src/utilities/sparkline.js");

describe("awe-client-angular/src/test/jest/data/sparklineParity.jest.spec.js", () => {
  it("keeps the sparkline module identical to the React client copy", () => {
    expect(fs.readFileSync(ANGULAR_COPY, "utf8")).toBe(fs.readFileSync(REACT_COPY, "utf8"));
  });
});
