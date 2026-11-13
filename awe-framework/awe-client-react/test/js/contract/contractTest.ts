import fixture from './fixtures/screenData.json';
import type { ClientAction, ComponentController, ScreenColumn, ScreenData, TypedScreenComponent } from '../../../src/contract';

// Ground truth: a trimmed copy of a real /screen-data response (serialised from the Java model by the server).
// Assigning it (no cast) to the generated types makes `npm run typecheck` fail when a key of the fixture changes its
// type in the contract. A key the contract drops fails only where the tests below read it: the generated properties are
// optional and a JSON module is not checked for excess properties.
const data: ScreenData = fixture;

const components = (data.components ?? []) as TypedScreenComponent[];
const byId = (id: string): TypedScreenComponent => {
  const component = components.find((item) => item.id === id);
  if (!component) {
    throw new Error(`Component ${id} not in the fixture`);
  }
  return component;
};

describe('screen contract types', () => {
  it('reads the screen components with their id and model', () => {
    expect(components.map((component) => component.id)).toEqual(['Txt', 'Sel', 'Grid', 'Dia']);
    expect(byId('Grid').model?.total).toBe(0);
    expect(byId('Txt').model?.page).toBe(1);
  });

  it('reads the controller of a component through the loose controller type', () => {
    const grid: ComponentController = byId('Grid').controller as ComponentController;
    expect(grid.component).toBe('grid');
    expect(grid.id).toBe('Grid');
    expect(Array.isArray(grid.columnModel)).toBe(true);
    expect((grid.columnModel as ComponentController[])[0].id).toBe('Col');
  });

  it('reads the flat column properties of a grid column', () => {
    const grid = byId('Grid').controller as ComponentController;
    const column: ScreenColumn = (grid.columnModel as ScreenColumn[])[0];
    expect(column.gridId).toBe('Grid');
    expect(column.id).toBe('Col');
    expect(column.model?.page).toBe(1);
    expect(column.frozen).toBe(true);
    expect(column.sortable).toBe(true);
    expect(column.width).toBe('120');
    expect(column.name).toBe('Col');
  });

  it('reads the client actions with their type', () => {
    const actions: ClientAction[] = data.actions ?? [];
    expect(actions.map((action) => action.type)).toEqual(['fill', 'message']);
    expect(actions[0].target).toBe('Grid');
    expect(actions[0].async).toBe(true);
  });

  it('reads the screen properties and the structure', () => {
    expect(data.screenProperties?.title).toBe('T');
    expect(data.structure?.id).toBe('gt');
  });
});
