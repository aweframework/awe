const fs = require('fs');
const path = require('path');

const REPOSITORY = path.join(__dirname, '../../../../..');
const renovate = JSON.parse(fs.readFileSync(path.join(REPOSITORY, 'renovate.json'), 'utf8'));
const ci = fs.readFileSync(path.join(REPOSITORY, '.gitlab-ci.yml'), 'utf8');
const rule = renovate.packageRules.find((packageRule) => packageRule.postUpgradeTasks);

describe('Renovate refresh of the React test application lock', () => {
  it('runs for every update of the client, also in branches shared with the AngularJS client', () => {
    // In "branch" mode Renovate only runs the tasks of the first upgrade of the branch: a dependency of both clients
    // (lodash, leaflet, echarts) starts with the AngularJS package.json, and the lock would never be refreshed
    expect(rule.matchFileNames).toEqual(['awe-framework/awe-client-react/package.json']);
    expect(rule.postUpgradeTasks.executionMode ?? 'update').toBe('update');
    expect(rule.postUpgradeTasks.fileFilters).toContain('awe-tests/awe-boot-react/package-lock.json');
  });

  it('only runs commands that the Renovate job allows', () => {
    const allowed = JSON.parse(ci.match(/RENOVATE_ALLOWED_COMMANDS:\s*'(.+)'/)[1]).map((pattern) => new RegExp(pattern));

    rule.postUpgradeTasks.commands.forEach((command) => expect(allowed.some((pattern) => pattern.test(command))).toBe(true));
  });
});
