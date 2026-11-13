import {changeClasses, clearClassChanges, rememberClassChange} from "../../../src/utilities/classChanges";

describe('awe-react-client/test/js/utilities/classChangesTest.js', () => {
  const mount = async (html, parent = document.body) => {
    const node = document.createElement("div");
    node.innerHTML = html;
    const element = node.firstElementChild;
    parent.appendChild(element);
    await Promise.resolve();
    return element;
  };

  beforeEach(() => {
    jest.spyOn(console, "warn").mockImplementation(() => {});
    document.body.innerHTML = "";
  });

  afterEach(() => {
    clearClassChanges();
    jest.restoreAllMocks();
  });

  it('applies a remembered change to a node mounted later', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    const node = await mount('<div id="lazy" class="row hidden"></div>');
    expect(node.className).toBe("row");
  });

  it('applies a remembered change to a descendant of a mounted node', async () => {
    rememberClassChange("#inner", "hidden", "remove");
    const node = await mount('<section><div id="inner" class="hidden"></div></section>');
    expect(node.querySelector("#inner").classList.contains("hidden")).toBe(false);
  });

  it('does not touch nodes that were already mounted when other nodes are added', async () => {
    const mounted = await mount('<div id="lazy" class="hidden"></div>');
    rememberClassChange("#lazy", "hidden", "remove");
    await mount('<div id="other"></div>');
    expect(mounted.classList.contains("hidden")).toBe(true);
  });


  it('keeps only the last add/remove change of the same selector and class', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    rememberClassChange("#lazy", "hidden", "add");
    const node = await mount('<div id="lazy"></div>');
    expect(node.classList.contains("hidden")).toBe(true);
  });

  it('drops the remembered change when the class is toggled', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    rememberClassChange("#lazy", "hidden", "toggle");
    const node = await mount('<div id="lazy" class="hidden"></div>');
    expect(node.classList.contains("hidden")).toBe(true);
  });

  it('skips an invalid selector, warns once and keeps applying the others', async () => {
    rememberClassChange("#(invalid", "hidden", "remove");
    rememberClassChange("#lazy", "hidden", "remove");
    const first = await mount('<div id="lazy" class="hidden"></div>');
    await mount('<div id="lazy2"></div>');
    expect(first.classList.contains("hidden")).toBe(false);
    expect(console.warn).toHaveBeenCalledTimes(1);
  });

  describe('when the component renders its classes again (#905)', () => {
    // A render writes the whole class attribute of the node, as React does with className
    const render = async (node, className) => {
      node.className = className;
      await Promise.resolve();
    };

    it('keeps a class added to a mounted node', async () => {
      const node = await mount('<button id="ButToggle" class="btn"></button>');
      changeClasses(node, "#ButToggle", ["is-active"], "add");
      await render(node, "btn btn-default");
      expect(node.className).toBe("btn btn-default is-active");
    });

    it('keeps a class removed from a mounted node', async () => {
      const node = await mount('<div id="Panel" class="panel highlight"></div>');
      changeClasses(node, "#Panel", ["highlight"], "remove");
      await render(node, "panel highlight expand");
      expect(node.className).toBe("panel expand");
    });

    it('keeps the state that a toggle left, added or removed', async () => {
      const node = await mount('<div id="Panel" class="panel on"></div>');
      changeClasses(node, "#Panel", ["on", "off"], "toggle");
      await render(node, "panel on wide");
      expect(node.className).toBe("panel wide off");
    });

    it('toggles back the class that a previous toggle added', async () => {
      const node = await mount('<div id="Panel" class="panel"></div>');
      changeClasses(node, "#Panel", ["on"], "toggle");
      changeClasses(node, "#Panel", ["on"], "toggle");
      await render(node, "panel wide");
      expect(node.className).toBe("panel wide");
    });

    it('lets a render that changes the same class win, as ng-class does in the AngularJS client', async () => {
      const node = await mount('<div id="Panel" class="panel hidden"></div>');
      changeClasses(node, "#Panel", ["hidden"], "remove");
      await render(node, "panel hidden wide");
      expect(node.classList.contains("hidden")).toBe(false);
      // The component is shown and then hidden again (for instance by a dependency): the render adds the class on purpose
      await render(node, "panel wide");
      await render(node, "panel hidden wide");
      expect(node.classList.contains("hidden")).toBe(true);
    });

    it('applies the changes to a node mounted again with its classes', async () => {
      const node = await mount('<button id="ButToggle" class="btn"></button>');
      changeClasses(node, "#ButToggle", ["is-active"], "add");
      node.remove();
      const again = await mount('<button id="ButToggle" class="btn"></button>');
      expect(again.classList.contains("is-active")).toBe(true);
    });

    it('remembers a toggle requested while the node is not mounted as the opposite of the remembered change', async () => {
      changeClasses(null, "#Lazy", ["on"], "add");
      changeClasses(null, "#Lazy", ["on"], "toggle");
      const node = await mount('<div id="Lazy" class="on"></div>');
      expect(node.classList.contains("on")).toBe(false);
    });

    it('does not touch the other nodes when one renders again', async () => {
      const node = await mount('<button id="ButToggle" class="btn"></button>');
      const other = await mount('<button id="ButOther" class="btn"></button>');
      changeClasses(node, "#ButToggle", ["is-active"], "add");
      await render(other, "btn btn-primary");
      expect(other.className).toBe("btn btn-primary");
    });
  });

  it('forgets the remembered changes when cleared', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    clearClassChanges();
    const node = await mount('<div id="lazy" class="hidden"></div>');
    expect(node.classList.contains("hidden")).toBe(true);
  });
});
