const {buildStubPackage} = require('../../../scripts/sync-test-app-lock.cjs');

describe('sync-test-app-lock', () => {
  const lock = {
    packages: {
      'node_modules/awe-react-client': {
        version: '5.0.0-SNAPSHOT',
        license: 'Apache-2.0',
        dependencies: {'i18next-http-backend': '4.0.4', react: '19.3.1'},
        engines: {node: '>= 16'}
      }
    }
  };

  it('builds the client stub with the dependencies of the lock at the versions the client declares', () => {
    const client = {dependencies: {'i18next-http-backend': '4.0.7', react: '19.3.1', 'not-in-lock': '1.0.0'}};

    expect(buildStubPackage(lock, client)).toEqual({
      name: 'awe-react-client',
      version: '5.0.0-SNAPSHOT',
      license: 'Apache-2.0',
      dependencies: {'i18next-http-backend': '4.0.7', react: '19.3.1'},
      engines: {node: '>= 16'}
    });
    expect(() => buildStubPackage(lock, {dependencies: {react: '19.3.1'}}))
      .toThrow('The client package.json does not declare the dependencies of the lock: i18next-http-backend');
  });
});
