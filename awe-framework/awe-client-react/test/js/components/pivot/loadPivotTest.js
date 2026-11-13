describe('loadPivot', () => {
  const load = () => {
    let loader;
    jest.isolateModules(() => {
      loader = require("../../../../src/components/pivot/loadPivot");
    });
    return loader.loadPivot;
  };

  it('loads the pivot table with the library and Table Barchart renderers, once', async () => {
    const loadPivot = load();

    const pivot = await loadPivot();

    expect(await loadPivot()).toBe(pivot);
    expect(Object.keys(pivot.renderers)).toEqual(expect.arrayContaining(["Table", "Table Heatmap", "Table Barchart"]));
    expect(pivot.PivotTableUI).toBeDefined();
    expect(pivot.utilities.aggregators).toBeDefined();
  });

  it('can load again after a failed load', async () => {
    let offline = true;
    jest.doMock("react-pivottable/Utilities", () => {
      if (offline) {
        throw new Error("offline");
      }
      return jest.requireActual("react-pivottable/Utilities");
    });
    const loadPivot = load();

    await expect(loadPivot()).rejects.toThrow("offline");
    offline = false;

    await expect(loadPivot()).resolves.toHaveProperty("PivotTableUI");
    jest.dontMock("react-pivottable/Utilities");
  });
});
