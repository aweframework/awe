import {clearClassChanges, rememberClassChange} from "../../../src/utilities/classChanges";

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

  it('does not revert later class changes done on an applied node', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    const node = await mount('<div id="lazy" class="hidden"></div>');
    node.classList.add("hidden");
    await mount('<div id="other"></div>');
    expect(node.classList.contains("hidden")).toBe(true);
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

  it('forgets the remembered changes when cleared', async () => {
    rememberClassChange("#lazy", "hidden", "remove");
    clearClassChanges();
    const node = await mount('<div id="lazy" class="hidden"></div>');
    expect(node.classList.contains("hidden")).toBe(true);
  });
});
