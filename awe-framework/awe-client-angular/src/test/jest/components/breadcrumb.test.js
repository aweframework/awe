// Breadcrumb of the window header (#938): navigable levels, collapse of long paths and the screens the user came from
import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";

const hook = id => `[data-testid='${id}']`;

describe("awe-breadcrumb", () => {
  let refs;
  let breadcrumb;
  let $location;
  let $state;
  let $filter;
  let $settings;

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["Breadcrumb", "$location", "$state", "$filter", "AweSettings", (_Breadcrumb_, _$location_, _$state_, _$filter_, _AweSettings_) => {
      breadcrumb = _Breadcrumb_;
      $location = _$location_;
      $state = _$state_;
      $filter = _$filter_;
      $settings = _AweSettings_;
    }]);
    jest.spyOn($location, "url").mockReturnValue("/screen/private/current");
  });

  afterEach(cleanupHeavyComponentTest);

  const group = (name, label = name) => ({name, label, group: true});
  const option = (name, label = name) => ({name, label, group: false});

  // The menu of the application: its options and what it does to launch one and, the vertical one, to open at a group
  const attachMenu = (options, extra = {}) => {
    const menu = {options: () => options, launch: jest.fn(), ...extra};
    breadcrumb.attachMenu(menu);
    return menu;
  };

  const render = (levels, title = "PAGE", pageOption = "") => {
    const node = $("<awe-breadcrumb><i class='home-icon'></i></awe-breadcrumb>")
      .attr({levels: JSON.stringify(levels), "page-title": title, "page-option": pageOption});
    const element = refs.$compile(node)(refs.$rootScope.$new());
    refs.$rootScope.$digest();
    return element;
  };
  // The current page carries the hook too (the tests find it), but it is not a level the user follows
  const levelsOf = element => element.find(hook("breadcrumb-level")).not("[aria-current]").toArray().map(node => $(node));
  const textsOf = element => element.find("li").toArray().map(node => node.textContent.trim());

  describe("levels", () => {
    it("is a navigation landmark with the home level and the page as the current level", () => {
      const element = render([group("sales")], "Orders");

      expect(element.find("nav").attr("aria-label")).toBe($filter("translateMultiple")("SCREEN_TEXT_BREADCRUMB"));
      expect(element.find(hook("breadcrumb")).length).toBe(1);
      expect(element.find(hook("breadcrumb-home")).attr("aria-label")).toBe($filter("translateMultiple")("BUTTON_HOME"));
      // The window template draws the icon of the home level
      expect(element.find(hook("breadcrumb-home")).find(".home-icon").length).toBe(1);
      const page = element.find("[aria-current='page']");
      expect(page.length).toBe(1);
      expect(page.text().trim()).toBe("Orders");
      expect(page.is("a, button")).toBe(false);
      expect(page.attr("data-testid")).toBe("breadcrumb-level");
      expect(page.attr("data-active")).toBe("true");
    });

    it("shows a group as plain text when the menu cannot open at it (horizontal menu or no menu)", () => {
      attachMenu([{name: "sales", options: []}]);
      const element = render([group("sales")]);

      expect(levelsOf(element).length).toBe(0);
      expect(textsOf(element)).toEqual(["sales", "PAGE"]);
    });

    it("shows a group that the menu does not know as plain text", () => {
      attachMenu([{name: "other", options: []}], {expand: jest.fn()});
      const element = render([group("sales")]);

      expect(levelsOf(element).length).toBe(0);
    });

    it("opens the menu at the path of a group when the menu can", () => {
      const expand = jest.fn();
      attachMenu([{name: "root", options: [{name: "sales", options: []}]}], {expand});
      const element = render([group("root"), group("sales")]);

      const [root, sales] = levelsOf(element);
      expect(sales.is("button")).toBe(true);
      expect(sales.attr("name")).toBe("sales");
      sales.trigger("click");
      root.trigger("click");

      expect(expand.mock.calls).toEqual([[["root", "sales"]], [["root"]]]);
    });

    it("launches an option level as the menu does", () => {
      const sales = {name: "sales", actions: [{type: "screen"}]};
      const menu = attachMenu([{name: "root", options: [sales]}]);
      const element = render([option("sales")]);

      const [level] = levelsOf(element);
      expect(level.attr("name")).toBe("sales");
      level.trigger("click");

      expect(menu.launch).toHaveBeenCalledWith(sales);
    });

    it("shows an option level that the menu does not know as plain text", () => {
      attachMenu([{name: "root", options: []}]);
      const element = render([option("sales")]);

      expect(levelsOf(element).length).toBe(0);
    });

    it("goes to the start screen from the home level, as a menu launch", () => {
      jest.spyOn(refs.$utilities, "getState").mockReturnValue({to: "private.screen", parameters: {screenId: "home"}});
      jest.spyOn($state, "go").mockImplementation(jest.fn());
      jest.spyOn($settings, "get").mockReturnValue("screen/private/home");
      const element = render([group("sales")]);

      element.find(hook("breadcrumb-home")).trigger("click");

      expect($state.go).toHaveBeenCalledWith("private.screen", {screenId: "home", freshScreen: true}, expect.any(Object));
    });
  });

  describe("levels that are not well formed", () => {
    it("renders braces of a label or the title literally", () => {
      const element = render([group("sales", "{{1 + 1}} sales")], "{{2 + 2}} page");

      expect(textsOf(element)).toEqual(["{{1 + 1}} sales", "{{2 + 2}} page"]);
    });

    it("shows the home level and the page when the levels are not JSON", () => {
      const node = $("<awe-breadcrumb><i></i></awe-breadcrumb>").attr({levels: "[{broken", "page-title": "PAGE"});
      const element = refs.$compile(node)(refs.$rootScope.$new());
      refs.$rootScope.$digest();

      expect(textsOf(element)).toEqual(["PAGE"]);
      expect(element.find(hook("breadcrumb-home")).length).toBe(1);
    });
  });

  describe("long paths", () => {
    const fourGroups = [group("a"), group("b"), group("c"), group("d")];

    it("shows up to four levels as they are", () => {
      const element = render([group("a"), group("b"), group("c")]);

      expect(textsOf(element)).toEqual(["a", "b", "c", "PAGE"]);
      expect(element.find(hook("breadcrumb-ellipsis")).length).toBe(0);
    });

    it("collapses the middle of more than four levels into a button that expands them in place", () => {
      const element = render(fourGroups);
      const ellipsis = () => element.find(hook("breadcrumb-ellipsis"));

      expect(textsOf(element).filter(text => text !== "…")).toEqual(["a", "d", "PAGE"]);
      expect(ellipsis().attr("aria-expanded")).toBe("false");
      expect(ellipsis().attr("aria-label")).toBe($filter("translateMultiple")("SCREEN_TEXT_BREADCRUMB_EXPAND"));

      ellipsis().trigger("click");
      expect(ellipsis().attr("aria-expanded")).toBe("true");
      expect(textsOf(element).filter(text => text !== "…")).toEqual(["a", "b", "c", "d", "PAGE"]);

      ellipsis().trigger("click");
      expect(textsOf(element).filter(text => text !== "…")).toEqual(["a", "d", "PAGE"]);
    });

    it("collapses again on another screen", () => {
      render(fourGroups).find(hook("breadcrumb-ellipsis")).trigger("click");

      const next = render(fourGroups);

      expect(next.find(hook("breadcrumb-ellipsis")).attr("aria-expanded")).toBe("false");
    });
  });

  describe("screens the user came from", () => {
    const START = {name: "private.screen"};
    const params = (subScreenId, freshScreen = null) => ({subScreenId, freshScreen});
    // The user leaves the screen that is open for another one: the transition ends before the next screen renders
    const navigate = (to, from = params("a"), toState = START, fromState = START) => {
      refs.$rootScope.$broadcast("$stateChangeStart", toState, to, fromState, from);
      refs.$rootScope.$broadcast("$stateChangeSuccess", toState, to, fromState, from);
    };
    // Screen A is open at its address with its levels
    const openA = () => {
      $location.url.mockReturnValue("/screen/private/a");
      return render([group("sales")], "A");
    };
    const openB = (levels = [], title = "B") => {
      $location.url.mockReturnValue("/screen/private/b");
      return render(levels, title);
    };

    it("shows the origin screen as a level that goes back to it, after the levels it had", () => {
      openA();
      navigate(params("b"));
      const element = openB();

      expect(textsOf(element)).toEqual(["sales", "A", "B"]);
      const [origin] = levelsOf(element);
      expect(origin.attr("path")).toBe("/screen/private/a");
      origin.trigger("click");
      expect($location.url).toHaveBeenLastCalledWith("/screen/private/a");
    });

    it("keeps the whole trail through several screens", () => {
      openA();
      navigate(params("b"));
      openB();
      navigate(params("c"), params("b"));
      $location.url.mockReturnValue("/screen/private/c");

      expect(textsOf(render([], "C"))).toEqual(["sales", "A", "B", "C"]);
    });

    it("starts from the menu without the screens the user came from", () => {
      openA();
      navigate(params("b", true));

      expect(textsOf(openB([group("other")]))).toEqual(["other", "B"]);
    });

    it("forgets them when a level launches an option of the menu or goes home", () => {
      const sales = {name: "sales", actions: [{}]};
      attachMenu([sales]);
      openA();
      navigate(params("b"));
      breadcrumb.follow({name: "sales", group: false});

      expect(textsOf(openB())).toEqual(["B"]);

      openA();
      navigate(params("b"));
      jest.spyOn(refs.$utilities, "getState").mockReturnValue({to: "private.screen", parameters: {}});
      jest.spyOn($state, "go").mockImplementation(jest.fn());
      breadcrumb.goHome();

      expect(textsOf(openB())).toEqual(["B"]);
    });

    it("keeps them when the new screen shows its levels before the transition ends", () => {
      openA();
      refs.$rootScope.$broadcast("$stateChangeStart", START, params("b"), START, params("a"));
      // The view of the new screen is compiled before the service handles the end of the transition
      const element = openB();
      refs.$rootScope.$broadcast("$stateChangeSuccess", START, params("b"), START, params("a"));
      navigate(params("c"), params("b"));
      $location.url.mockReturnValue("/screen/private/c");

      expect(textsOf(element)).toEqual(["sales", "A", "B"]);
      expect(textsOf(render([], "C"))).toEqual(["sales", "A", "B", "C"]);
    });

    it("keeps them when the screen is reloaded", () => {
      openA();
      navigate(params("b"));
      openB();
      navigate(params("b"), params("b"));

      expect(textsOf(openB())).toEqual(["sales", "A", "B"]);
    });

    it("keeps them through a screen without breadcrumb", () => {
      openA();
      navigate(params("b"));
      // The screen B has no breadcrumb (window header none): it publishes no levels
      navigate(params("c"), params("b"));
      $location.url.mockReturnValue("/screen/private/c");

      expect(textsOf(render([], "C"))).toEqual(["sales", "A", "C"]);
    });

    it("heals itself when the user comes back to the origin screen without the levels", () => {
      openA();
      navigate(params("b"));
      openB();
      // Browser back: A again, at its address
      navigate(params("a"), params("b"));

      expect(textsOf(openA())).toEqual(["sales", "A"]);
    });

    it("does not repeat the menu levels of a screen it came from", () => {
      attachMenu([]);
      $location.url.mockReturnValue("/screen/private/a");
      render([group("sales")], "A");
      navigate(params("b"));
      openB([group("sales")], "B");
      navigate(params("a"), params("b"));

      expect(textsOf(openA())).toEqual(["sales", "A"]);
    });

    describe("launcher that is on the menu path of the screen it opens", () => {
      const launcher = {name: "launcher", label: "Launcher", group: false};
      const reports = [group("settings"), launcher, option("sales", "Sales"), option("monthly", "Monthly")];
      const openLauncher = () => {
        $location.url.mockReturnValue("/screen/private/launcher");
        return render([group("settings")], "Launcher", "launcher");
      };
      const openMonthly = () => {
        $location.url.mockReturnValue("/screen/private/monthly");
        return render(reports.slice(0, 3), "Monthly", "monthly");
      };

      it("shows only its own menu path", () => {
        openLauncher();
        navigate(params("monthly"), params("launcher"));

        expect(textsOf(openMonthly())).toEqual(["settings", "Launcher", "Sales", "Monthly"]);
      });

      it("keeps the trail when the screen it came from is not on its menu path", () => {
        openLauncher();
        navigate(params("monthly"), params("launcher"));
        $location.url.mockReturnValue("/screen/private/monthly");

        expect(textsOf(render([group("other")], "Monthly", "monthly"))).toEqual(["settings", "Launcher", "other", "Monthly"]);
      });
    });
  });
});
