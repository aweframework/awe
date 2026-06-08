describe("AngularJS Jest smoke", () => {
  beforeEach(() => {
    angular.module("awe.jest.smoke", [])
      .service("smokeService", function() {
        this.message = function() {
          return "angular-mocks-ready";
        };
      });

    angular.mock.module("awe.jest.smoke");
  });

  it("loads AngularJS modules through angular-mocks", angular.mock.inject((smokeService) => {
    expect(smokeService.message()).toBe("angular-mocks-ready");
  }));

  it("cleans DOM fixtures between Jest examples", () => {
    document.body.innerHTML = "<div id=\"awe-fixture\">temporary fixture</div>";

    expect(document.getElementById("awe-fixture").textContent).toBe("temporary fixture");
  });

  it("starts the next Jest example with an empty DOM fixture area", () => {
    expect(document.body.innerHTML).toBe("");
  });
});
