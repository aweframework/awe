function createContractScope($rootScope, options = {}) {
  const scope = $rootScope.$new();
  scope.view = options.view || "report";
  scope.context = options.context || "contract";

  return Object.assign(scope, options.scope || {});
}

function buildComponentApiMap(entries = []) {
  return entries.reduce((apis, entry) => {
    apis[entry.id] = entry.component;
    return apis;
  }, {});
}

function createApiStorage(view = "report", entries = []) {
  return {
    [view]: buildComponentApiMap(entries)
  };
}

function mergeComponentData(entries = []) {
  return entries.reduce((data, entry) => Object.assign(data, entry.component.getData()), {});
}

function createServerAction(Action, callbackTarget, parameters) {
  const action = new Action();
  action.attr("callbackTarget", callbackTarget);
  action.attr("parameters", parameters);

  return action;
}

function buildLaunchRequestExpectation({action, target, values}) {
  return jasmine.objectContaining({
    action,
    target,
    values: jasmine.objectContaining(values)
  });
}

module.exports = {
  buildComponentApiMap,
  buildLaunchRequestExpectation,
  createApiStorage,
  createContractScope,
  createServerAction,
  mergeComponentData
};
