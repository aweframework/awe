import { mapContextMenu } from '../../../src/hooks/useContextMenu';

describe('awe-react-client/test/js/hooks/useGridContextMenuTest.js', () => {
  const address = { view: 'base', component: 'grid1' };

  it('stores the click event on the context option when it is selected, like AngularJS', () => {
    const addActionsTop = jest.fn();
    const raiseEvent = jest.fn();
    const [item] = mapContextMenu([{ id: 'opt1' }], {
      t: (key) => key,
      addActionsTop,
      raiseEvent,
      address,
      optionAttributes: { opt1: { label: 'Opt', actions: [{ type: 'do' }] } }
    });

    item.command();

    expect(addActionsTop).toHaveBeenCalledWith([{ type: 'do', address }]);
    expect(raiseEvent).toHaveBeenCalledWith({ view: 'base', component: 'opt1' }, 'click');
  });
});
