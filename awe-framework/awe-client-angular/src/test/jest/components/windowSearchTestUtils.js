// Fixtures of the saved searches of a window (#790, #915): a window with essential criteria and an advanced block with
// saved-searches, test criteria that restore their default value as the criteria do, and a server that answers the
// saved search queries
import {initHeavyComponentTest, stubPanelComponent} from "./heavyComponentTestUtils";

export const hook = id => `[data-testid='${id}']`;
export const text = node => $(node).text().replace(/\s+/g, " ").trim();

// Essential criteria (user, profile, customer) and the advanced block (state, tags, locked)
export const SAVED_SEARCHES_BODY = `<div class='panel-body'>
      <div test-criterion criterion-id='user' class='criterion'><input id='user'></div>
      <div test-criterion criterion-id='profile' class='criterion'><input id='profile'></div>
      <div test-criterion criterion-id='customer' class='criterion'><input id='customer'></div>
      <awe-advanced advanced-id='adv1'>
        <div test-criterion criterion-id='state' class='criterion'><input id='state'></div>
        <div test-criterion criterion-id='tags' class='criterion'><input id='tags'></div>
        <div test-criterion criterion-id='locked' class='criterion'><input id='locked' type='checkbox'></div>
      </awe-advanced>
    </div><div class='panel-footer'></div>`;

/**
 * Controllers of the window w1 and of its criteria
 * @param {Object} extra Attributes of the window controller
 * @returns {Object} Controllers by id
 */
export const savedSearchControllers = extra => ({
  w1: {
    id: "w1", component: "window", label: "Users", advancedMode: "inline",
    advancedCriteria: ["state", "tags", "locked"],
    searchCriteria: ["user", "profile", "customer", "state", "tags", "locked"],
    ...extra
  },
  user: {id: "user", component: "text", label: "Name"},
  profile: {id: "profile", component: "select", label: "Profile"},
  customer: {id: "customer", component: "suggest", label: "Customer"},
  state: {id: "state", component: "select", label: "State"},
  tags: {id: "tags", component: "select-multiple", label: "Tags"},
  locked: {id: "locked", component: "checkbox", label: "Locked"}
});

/**
 * Models of the window w1 and of its criteria
 * @returns {Object} Models by id
 */
export const savedSearchModels = () => ({
  w1: {selected: null, values: []},
  user: {selected: "jo", defaultValues: null, values: []},
  profile: {selected: "op", defaultValues: null, values: [{value: "op", label: "Operator"}, {value: "ad", label: "Admin"}]},
  customer: {selected: null, defaultValues: null, values: []},
  state: {selected: "1", defaultValues: "1", values: [{value: "1", label: "Active"}, {value: "0", label: "Inactive"}]},
  tags: {selected: [], defaultValues: [], values: [{value: "a", label: "Alpha"}]},
  locked: {selected: 0, defaultValues: 0, values: []}
});

/**
 * Start a saved searches test: a criterion of the test (an isolated scope that tells its restores) and the services
 * @param {function(string, string)} onRestore Called with the criterion id and the view of every restore it receives
 * @returns {Object} Heavy component refs, with actionController, connection and $q
 */
export function initSavedSearchTest(onRestore) {
  angular.mock.module(["$compileProvider", $compileProvider => {
    $compileProvider.directive("testCriterion", () => ({
      restrict: "A",
      scope: {},
      link: (scope, elem) => {
        const id = elem.attr("criterion-id");
        scope.$on("restore-scope", (event, view) => onRestore(id, view));
      }
    }));
  }]);
  const refs = initHeavyComponentTest();
  inject(["ActionController", "Connection", "$q", (ActionController, Connection, $q) => {
    refs.actionController = ActionController;
    refs.connection = Connection;
    refs.$q = $q;
  }]);
  return refs;
}

/**
 * Compile the window w1 in a form, with a server that answers the saved search queries
 * @param {Object} refs Refs of initSavedSearchTest
 * @param {{controllers: Object, models: Object, body: string, server: {searches: Array, profiles: Array, answers: Object}}} window
 * Controllers and models by id, body of the window and the server: the rows of the searches and of the profiles, and
 * the answers that replace them by target (failures)
 * @returns {Object} Window element
 */
export function compileSavedSearchWindow(refs, {controllers, models, body = SAVED_SEARCHES_BODY, server}) {
  stubPanelComponent(refs, {}, controllers.w1);
  refs.$storage.get.mockReturnValue({base: {name: "UsrLst"}});
  jest.spyOn(refs.actionController, "addActionList").mockImplementation(() => undefined);
  jest.spyOn(refs.connection, "post").mockImplementation(url => {
    const target = url.split("/").pop();
    if (server.answers[target]) {
      return server.answers[target]();
    }
    return refs.$q.resolve({
      data: [{type: "fill", parameters: {datalist: {rows: target === "getSavedSearchProfiles" ? server.profiles : server.searches}}}, {type: "end-load"}]
    });
  });
  refs.$control.getAddressController.mockImplementation(address => controllers[address.component]);
  refs.$control.getAddressModel.mockImplementation(address => models[address.component]);
  const form = $("<form></form>").appendTo(document.body);
  const element = refs.$compile(`<awe-window window-id='w1'>${body}</awe-window>`)(refs.$rootScope.$new());
  form.append(element);
  refs.$rootScope.$digest();
  return element;
}

/**
 * The service of an action list launched by launchSilentService: its server action and the action that tells it worked
 * @param {Object} actionController Action controller (addActionList mocked)
 * @param {string} target Maintain target
 * @param {number} [index] Which call, the first one by default
 * @returns {Array|undefined} Actions of the call
 */
export const serviceCall = (actionController, target, index = 0) => actionController.addActionList.mock.calls
  .map(([actions]) => actions)
  .filter(actions => actions[0].attr && actions[0].attr("parameters")?.targetAction === target)[index];
