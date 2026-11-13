import {buildActionRegistry} from "../../../src/utilities/actionRegistry";

const service = (actions) => ({getActions: () => actions});

describe('awe-react-client/test/js/utilities/actionRegistryTest.js', () => {
  it('merges the actions of every service into one table', () => {
    const reload = jest.fn();
    const message = jest.fn();

    const registry = buildActionRegistry({screen: service({reload}), message: service({message})});

    expect(registry).toEqual({reload, message});
  });

  it('fails naming both owners when two services register the same action', () => {
    const services = {screen: service({reload: jest.fn()}), grid: service({reload: jest.fn()})};

    expect(() => buildActionRegistry(services)).toThrow(/"reload".*"screen".*"grid"/);
  });

  it('returns an empty table when there are no services', () => {
    expect(buildActionRegistry({})).toEqual({});
  });
});
