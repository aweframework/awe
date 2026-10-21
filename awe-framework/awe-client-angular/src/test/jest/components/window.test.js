// Window directive: maximize and the test hooks shared with the React client
import {
  cleanupHeavyComponentTest,
  createPanelController,
  createPanelModel,
  initHeavyComponentTest,
  stubPanelComponent
} from "./heavyComponentTestUtils";

const hook = id => `[data-testid='${id}']`;

describe("window directive", () => {
  let refs;

  beforeEach(() => {
    refs = initHeavyComponentTest();
  });

  afterEach(cleanupHeavyComponentTest);

  const compileWindow = (extra = {}) => {
    stubPanelComponent(refs, createPanelModel(), createPanelController("w1", "window", {maximize: true, ...extra}));
    const element = refs.$compile("<awe-window window-id='w1'></awe-window>")(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return {element, scope: element.isolateScope() || element.scope()};
  };

  it("renders the window and its maximize button as test hooks that follow the maximized state", () => {
    const target = $("<div class='maximize-target'><div class='sibling'>other</div></div>").appendTo(document.body);
    const {element, scope} = compileWindow();
    target.append(element);
    target.find(".sibling")[0].getClientRects = () => [{}];
    const panel = element.is(hook("window")) ? element : element.find(hook("window")).first();
    const button = () => element.find(hook("window-maximize-button"));
    jest.spyOn(refs.$utilities, "animateCSS").mockImplementation((node, size, time, end) => end());

    expect(panel.length).toBe(1);
    expect(panel.attr("data-testid-owner")).toBe("w1");
    expect(panel.attr("data-maximized")).toBe("false");
    expect(button().find("i").hasClass("fa-expand")).toBe(true);

    scope.togglePanel();
    refs.$rootScope.$digest();
    expect(panel.attr("data-maximized")).toBe("true");
    expect(panel.hasClass("maximized")).toBe(true);
    expect(target.find(".sibling").hasClass("minimized")).toBe(true);
    expect(button().find("i").hasClass("fa-compress")).toBe(true);

    scope.togglePanel();
    refs.$rootScope.$digest();
    expect(panel.attr("data-maximized")).toBe("false");
    expect(target.find(".sibling").hasClass("minimized")).toBe(false);
    target.remove();
  });

  it("renders no maximize button when the window is not maximizable", () => {
    const {element} = compileWindow({maximize: false});

    expect(element.find(hook("window-maximize-button")).length).toBe(0);
    expect(element.is(hook("window")) || element.find(hook("window")).length === 1).toBe(true);
  });
});
