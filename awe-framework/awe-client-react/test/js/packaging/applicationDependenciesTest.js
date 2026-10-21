const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '../../..');
const REPOSITORY = path.join(ROOT, '../..');
const readJson = (file) => JSON.parse(fs.readFileSync(file, 'utf8'));
const clientPackage = readJson(path.join(ROOT, 'package.json'));
const testApplication = path.join(REPOSITORY, 'awe-tests/awe-boot-react');
const archetype = path.join(REPOSITORY, 'awe-framework/awe-boot-react-archetype/src/main/resources/archetype-resources');

describe('Dependencies of the applications that use the React client', () => {
  it('does not use @stomp/stompjs 7.4.0, whose require entry exports nothing in a browser bundle', () => {
    // 7.4.0 dropped the "browser" condition: webpack resolves require() to the UMD bundle of a "type": "module"
    // package, so the client gets an empty module and the websocket fails with "Client is not a constructor"
    expect(clientPackage.dependencies['@stomp/stompjs']).not.toBe('7.4.0');
  });

  it('installs in the test application the versions the client declares', () => {
    // npm ci installs the dependencies of the file: client from the lock of the test application, so a lock that is not
    // refreshed after a client upgrade builds and tests the client with other versions
    const lock = readJson(path.join(testApplication, 'package-lock.json'));
    const installed = lock.packages['node_modules/awe-react-client'].dependencies;
    const differences = Object.entries(installed)
      .filter(([name, version]) => clientPackage.dependencies[name] !== version)
      .map(([name, version]) => `${name}: lock ${version}, client ${clientPackage.dependencies[name]}`);

    expect(differences).toEqual([]);
  });

  it.each([
    ['the test application', testApplication],
    ['the archetype', archetype]
  ])('does not declare @types/react-redux in %s (react-redux 9 has its own types; v7 drags redux 4)', (name, folder) => {
    // The package.json of the archetype is a Velocity template (not valid JSON): look for the declared key in the text
    const packageText = fs.readFileSync(path.join(folder, 'package.json'), 'utf8');

    expect(packageText).not.toMatch(/"@types\/react-redux"\s*:/);
  });
});
