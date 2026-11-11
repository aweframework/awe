import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";
import {submitForm} from "./criterion";
import {iconMarkup} from "./icon";
import {launchSilentService} from "./silentService";
import "../directives/plugins/uiSelect";

// Saved searches of a window (#790, delivery 3): a window with an advanced block and saved-searches="true" has a Saved
// searches menu in the row of the block, with the searches of the user and the ones published for their profile.
// Applying one restores every search criterion of the window to its default, sets the saved values and launches the
// submit of the window. The user is never sent: the server takes it (and its profile) from the session. A grid with
// saved-searches="true" (#912) has the same menu in its toolbar, with a source that reads and restores the grid state.
// A search of a window may also keep its layout (#915), the advanced criteria it shows in the main row, under the
// reserved key $layout, and be the default search of the user or of a profile: the server applies it before the initial
// load, the window names it in a chip and its restores return to it. A grid search has no layout, but it may be the
// default search of the grid (#945): the server applies it before the initial load and the grid names it in a chip.

// Lists whose selected values are an array
const MULTIPLE = ["select-multiple", "suggest-multiple"];
// Criteria whose values are options with a label
const LISTS = ["select", "suggest", ...MULTIPLE];
const NAME_MAX_LENGTH = 100;
// Profile of a search that is only for the user who saves it (the select shows no option for an empty value)
export const ONLY_ME = "@only-me";
// Message of a load that failed without an answer of the server
const LOAD_ERROR = Object.freeze({type: "message", parameters: Object.freeze({type: "error",
  title: "SCREEN_TEXT_SAVED_SEARCH_ERROR_TITLE", message: "SCREEN_TEXT_SAVED_SEARCH_ERROR_MESSAGE"})});

const owner = `ng-attr-data-testid-owner="{{::saved.windowId}}"`;
const savedSearchId = `ng-attr-${TestAttributes.savedSearchId}="{{search.id}}"`;
// Reserved key of the values of a window search with the advanced criteria it shows in the main row (#915)
const LAYOUT = "$layout";

// Default search of the user and default of its profile (#915), as a text tag
const defaultTagsTemplate = `<span class="window-saved-searches-tag window-saved-searches-tag-default" ng-if="saved.isDefault(search)"
            data-testid="${TestIds.windowSavedSearchDefaultTag}" ${owner}>{{'SCREEN_TEXT_SAVED_SEARCH_DEFAULT' | translateMultiple}}</span>
      <span class="window-saved-searches-tag" ng-if="saved.isProfileDefault(search)"
            data-testid="${TestIds.windowSavedSearchProfileDefaultTag}" ${owner}>{{'SCREEN_TEXT_SAVED_SEARCH_PROFILE_DEFAULT' | translateMultiple}}</span>`;

const optionTemplate = (type, icon) => `<button type="button" role="menuitem" tabindex="-1" class="window-saved-searches-option"
          ng-repeat="search in saved.${type} track by search.id" data-testid="${TestIds.windowSavedSearchItem}" ${owner} ${savedSearchId}
          data-type="${type}" ng-attr-aria-current="{{saved.isCurrent(search) ? 'true' : undefined}}" ng-click="saved.apply(search)">
      ${iconMarkup(icon)}
      <span class="window-saved-searches-text">{{search.name}}</span>
      ${defaultTagsTemplate}
      ${type === "shared" ? `<span class="window-saved-searches-tag">{{saved.profileLabel(search.profile)}}</span>` : ""}
    </button>`;

/**
 * The Saved searches button (with the name of the search applied while its values do not change) and its menu
 * @param {string} [buttonClasses] Classes of the button: a text button, or the look of the toolbar it is in
 * @param {string} [labels] Expression that says whether the button shows its text (the grid toolbar): when it is false,
 * an icon button with the text as its tooltip and accessible name (the name of the search applied after the text). The
 * button always shows its text when no expression is given (the window)
 * @returns {string} Template
 */
export const getSavedSearchesTemplate = (buttonClasses = "", labels = null) => {
  const text = "('SCREEN_TEXT_SAVED_SEARCHES' | translateMultiple) + (saved.current ? ': ' + saved.current.name : '')";
  const iconOnly = labels ? ` ng-class="{'grid-toolbar-button-icon': !(${labels})}" ng-attr-title="{{!(${labels}) ? ${text} : undefined}}" ng-attr-aria-label="{{!(${labels}) ? ${text} : undefined}}"` : "";
  const shown = labels ? `(${labels}) && ` : "";
  return `<div class="window-saved-searches" ng-if="::saved">
  <button type="button" class="window-saved-searches-toggle${buttonClasses ? ` ${buttonClasses}` : ""}" data-testid="${TestIds.windowSavedSearches}" ${owner}${iconOnly}
          aria-haspopup="menu" ng-attr-aria-expanded="{{saved.open}}" aria-controls="{{::saved.menuId}}" ng-click="saved.toggleMenu()">
    ${iconMarkup("lucide:bookmark")}
    <span class="window-saved-searches-label" ng-if="${shown}!saved.current">{{'SCREEN_TEXT_SAVED_SEARCHES' | translateMultiple}}</span>
    <span class="window-saved-searches-label" ng-if="${shown}saved.current">{{saved.current.name}}</span>
    ${iconMarkup("lucide:chevron-down window-saved-searches-chevron", labels ? `ng-if="${labels}"` : "")}
  </button>
  <div class="window-saved-searches-menu" role="menu" id="{{::saved.menuId}}" aria-label="{{'SCREEN_TEXT_SAVED_SEARCHES' | translateMultiple}}"
       data-testid="${TestIds.windowSavedSearchesMenu}" ${owner} ng-attr-hidden="{{saved.open ? undefined : 'hidden'}}"
       ng-attr-aria-busy="{{saved.loading ? 'true' : undefined}}" ng-keydown="saved.menuKeydown($event)">
    <div role="group" aria-labelledby="{{::saved.menuId}}-own">
      <p class="window-saved-searches-head" id="{{::saved.menuId}}-own" role="presentation">{{'SCREEN_TEXT_MY_SEARCHES' | translateMultiple}}</p>
      ${optionTemplate("own", "lucide:bookmark")}
      <p class="window-saved-searches-empty" role="presentation" ng-if="saved.own.length === 0">{{'SCREEN_TEXT_NO_SAVED_SEARCHES' | translateMultiple}}</p>
    </div>
    <div role="separator" class="window-saved-searches-separator" ng-if="saved.shared.length > 0"></div>
    <div role="group" aria-labelledby="{{::saved.menuId}}-shared" ng-if="saved.shared.length > 0">
      <p class="window-saved-searches-head" id="{{::saved.menuId}}-shared" role="presentation">{{'SCREEN_TEXT_SHARED_SEARCHES' | translateMultiple}}</p>
      ${optionTemplate("shared", "lucide:users")}
    </div>
    <div role="separator" class="window-saved-searches-separator"></div>
    <button type="button" role="menuitem" tabindex="-1" class="window-saved-searches-option" data-testid="${TestIds.windowSavedSearchSave}" ${owner}
            ng-click="saved.openSave()">${iconMarkup("lucide:save")}<span class="window-saved-searches-text">{{'SCREEN_TEXT_SAVE_CURRENT_SEARCH' | translateMultiple}}</span></button>
    <button type="button" role="menuitem" tabindex="-1" class="window-saved-searches-option" data-testid="${TestIds.windowSavedSearchManage}" ${owner}
            ng-click="saved.openManage()">${iconMarkup("lucide:settings")}<span class="window-saved-searches-text">{{'SCREEN_TEXT_MANAGE_SEARCHES' | translateMultiple}}</span></button>
  </div>
</div>`;
};

// The Saved searches of the advanced block of a window: a text button
export const savedSearchesTemplate = getSavedSearchesTemplate();

// The chip next to the Saved searches button that names the default search applied when the window opened (#915)
export const appliedDefaultTemplate = `<span class="window-saved-search-applied" ng-if="saved.applied"
      data-testid="${TestIds.windowSavedSearchAppliedDefault}" ${owner} title="{{'SCREEN_TEXT_SAVED_SEARCH_APPLIED_DEFAULT' | translateMultiple}}: {{saved.applied.name}}">
    ${iconMarkup("lucide:bookmark")}<span class="sr-only">{{'SCREEN_TEXT_SAVED_SEARCH_APPLIED_DEFAULT' | translateMultiple}}: </span><span class="window-saved-search-applied-name">{{saved.applied.name}}</span>
  </span>`;

/**
 * An option of the Save search dialog of a window (#915): a checkbox with its help
 * @param {string} field Field of the form
 * @param {string} testId Test id of the checkbox
 * @param {string} label Text of the option (its help is the text with _HELP)
 * @param {string} [before] Markup before the help
 * @param {string} [attributes] Attributes of the option
 * @returns {string} Template
 */
const optionCheckTemplate = (field, testId, label, before = "", attributes = "") => `<div class="window-saved-search-check"${attributes}>
          <label>
            <input type="checkbox" ng-model="saved.form.${field}" data-testid="${testId}" ${owner}
                   awe-saved-search-selected="saved.form.${field}" aria-describedby="{{::saved.dialogId}}-${field}-help">
            <span>{{'${label}' | translateMultiple}}</span>
          </label>
          <p class="window-saved-search-help" id="{{::saved.dialogId}}-${field}-help">${before}{{'${label}_HELP' | translateMultiple}}</p>
        </div>`;

// Options of the Save search dialog that only a window search has: a grid search keeps neither a layout nor values to
// leave out (#945)
const WINDOW_ONLY = ` ng-if="::saved.windowSearch"`;

// The Save search dialog: the name (required) and, only for a user who may publish, the profile to publish it for
const saveDialogTemplate = `<div class="modal fade window-saved-search-modal" ui-modal trap-tab role="dialog" aria-modal="true"
     aria-labelledby="{{::saved.dialogId}}-save-title" data-testid="${TestIds.windowSavedSearchDialog}" ${owner}>
  <div class="modal-dialog window-saved-search-save-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="{{'BUTTON_CLOSE' | translateMultiple}}">&times;</button>
        <h4 class="modal-title" id="{{::saved.dialogId}}-save-title">{{'SCREEN_TEXT_SAVE_SEARCH' | translateMultiple}}</h4>
        <p class="window-saved-search-subtitle">{{'SCREEN_TEXT_SAVE_SEARCH_SUBTITLE' | translateMultiple}}</p>
      </div>
      <div class="modal-body">
        <div class="window-saved-search-field">
          <label for="{{::saved.dialogId}}-name">{{'SCREEN_TEXT_SEARCH_NAME' | translateMultiple}} <span class="window-saved-search-required" aria-hidden="true">*</span></label>
          <input type="text" class="form-control" id="{{::saved.dialogId}}-name" maxlength="${NAME_MAX_LENGTH}" required aria-required="true"
                 data-testid="${TestIds.windowSavedSearchName}" ${owner} ng-model="saved.form.name"
                 ng-attr-aria-invalid="{{saved.form.invalid ? 'true' : undefined}}"
                 ng-attr-aria-describedby="{{saved.form.invalid ? saved.dialogId + '-name-error' : undefined}}" ng-keydown="saved.nameKeydown($event)">
          <p class="window-saved-search-error" id="{{::saved.dialogId}}-name-error" ng-if="saved.form.invalid">{{'VALIDATOR_MESSAGE_REQUIRED' | translateMultiple}}</p>
        </div>
        <div class="window-saved-search-field form-group" ng-if="saved.profiles.length > 0">
          <label for="{{::saved.dialogId}}-profile">{{'SCREEN_TEXT_PUBLISH_FOR_PROFILE' | translateMultiple}}</label>
          <awe-saved-search-profile></awe-saved-search-profile>
          <p class="window-saved-search-help" id="{{::saved.dialogId}}-profile-help">{{'SCREEN_TEXT_PUBLISH_FOR_PROFILE_HELP' | translateMultiple}}</p>
        </div>
        <div class="window-saved-search-field window-saved-search-options">
          ${optionCheckTemplate("keepLayout", TestIds.windowSavedSearchKeepLayout, "SCREEN_TEXT_SAVED_SEARCH_KEEP_LAYOUT",
    `<span ng-repeat="label in saved.form.layoutLabels">{{label | translateMultiple}}{{$last ? '. ' : ', '}}</span>`, WINDOW_ONLY)}
          ${optionCheckTemplate("keepValues", TestIds.windowSavedSearchKeepValues, "SCREEN_TEXT_SAVED_SEARCH_KEEP_VALUES", "", WINDOW_ONLY)}
          ${optionCheckTemplate("openWith", TestIds.windowSavedSearchOpenWith, "SCREEN_TEXT_SAVED_SEARCH_OPEN_WITH")}
        </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-awe" data-dismiss="modal" data-testid="${TestIds.windowSavedSearchCancel}" ${owner}>{{'BUTTON_CANCEL' | translateMultiple}}</button>
        <button type="button" class="btn btn-awe btn-primary" data-testid="${TestIds.windowSavedSearchAccept}" ${owner}
                ng-disabled="saved.form.saving" ng-click="saved.save()">${iconMarkup("lucide:save")}<span>{{'SCREEN_TEXT_SAVE_SEARCH' | translateMultiple}}</span></button>
      </div>
    </div>
  </div>
</div>`;

// The Manage searches dialog: who each search is for, and rename and delete for the searches the user may change
const manageDialogTemplate = `<div class="modal fade window-saved-search-modal" ui-modal trap-tab role="dialog" aria-modal="true"
     aria-labelledby="{{::saved.dialogId}}-manage-title" data-testid="${TestIds.windowSavedSearchManageDialog}" ${owner}>
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="{{'BUTTON_CLOSE' | translateMultiple}}">&times;</button>
        <h4 class="modal-title" id="{{::saved.dialogId}}-manage-title">{{'SCREEN_TEXT_MANAGE_SEARCHES_TITLE' | translateMultiple}}</h4>
        <p class="window-saved-search-subtitle">{{::saved.title | translateMultiple}}</p>
      </div>
      <div class="modal-body">
        <p class="window-saved-searches-empty" ng-if="saved.searches.length === 0">{{'SCREEN_TEXT_NO_SAVED_SEARCHES' | translateMultiple}}</p>
        <table class="window-saved-search-table" ng-if="saved.searches.length > 0">
          <thead><tr>
            <th scope="col">{{'SCREEN_TEXT_SEARCH_NAME' | translateMultiple}}</th>
            <th scope="col">{{'SCREEN_TEXT_SEARCH_FOR' | translateMultiple}}</th>
            <th scope="col"><span class="sr-only">{{'SCREEN_TEXT_SEARCH_ACTIONS' | translateMultiple}}</span></th>
          </tr></thead>
          <tbody>
            <tr ng-repeat="search in saved.searches track by search.id" data-testid="${TestIds.windowSavedSearchRow}" ${owner} ${savedSearchId}>
              <td class="window-saved-search-name">
                <span ng-if="saved.editing.id !== search.id">{{search.name}}</span>
                <span class="window-saved-search-rename" ng-if="saved.editing.id === search.id">
                  <input type="text" class="form-control" maxlength="${NAME_MAX_LENGTH}" aria-label="{{'SCREEN_TEXT_SEARCH_NAME' | translateMultiple}}"
                         data-testid="${TestIds.windowSavedSearchRenameInput}" ${owner} ng-model="saved.editing.name"
                         ng-attr-aria-invalid="{{saved.editing.invalid ? 'true' : undefined}}" ng-keydown="saved.renameKeydown($event)">
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchRenameAccept}" ${owner}
                          aria-label="{{'BUTTON_ACCEPT' | translateMultiple}}" title="{{'BUTTON_ACCEPT' | translateMultiple}}" ng-click="saved.rename()">${iconMarkup("lucide:check")}</button>
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchRenameCancel}" ${owner}
                          aria-label="{{'BUTTON_CANCEL' | translateMultiple}}" title="{{'BUTTON_CANCEL' | translateMultiple}}" ng-click="saved.cancelRename()">${iconMarkup("lucide:x")}</button>
                </span>
              </td>
              <td>
                <span ng-if="saved.isOwn(search)">{{'SCREEN_TEXT_SEARCH_OWNER_ME' | translateMultiple}}</span>
                <span class="window-saved-searches-tag" ng-if="!saved.isOwn(search)">{{saved.profileLabel(search.profile)}}</span>
              </td>
              <td class="window-saved-search-actions">
                <span ng-show="saved.editing.id !== search.id">
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchSetDefault}" ${owner}
                          awe-saved-search-selected="saved.isDefault(search)"
                          aria-label="{{(saved.isDefault(search) ? 'SCREEN_TEXT_SAVED_SEARCH_UNSET_DEFAULT' : 'SCREEN_TEXT_SAVED_SEARCH_SET_DEFAULT') | translateMultiple}} {{search.name}}"
                          title="{{(saved.isDefault(search) ? 'SCREEN_TEXT_SAVED_SEARCH_UNSET_DEFAULT' : 'SCREEN_TEXT_SAVED_SEARCH_SET_DEFAULT') | translateMultiple}}"
                          ng-click="saved.toggleDefault(search)">${iconMarkup("lucide:star", 'ng-if="!saved.isDefault(search)"')}${iconMarkup("lucide:star-off", 'ng-if="saved.isDefault(search)"')}</button>
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchSetProfileDefault}" ${owner}
                          ng-if="saved.profiles.length > 0 && !saved.isOwn(search)"
                          awe-saved-search-selected="saved.isProfileDefault(search)"
                          aria-label="{{(saved.isProfileDefault(search) ? 'SCREEN_TEXT_SAVED_SEARCH_UNSET_PROFILE_DEFAULT' : 'SCREEN_TEXT_SAVED_SEARCH_SET_PROFILE_DEFAULT') | translateMultiple}} {{search.name}}"
                          title="{{(saved.isProfileDefault(search) ? 'SCREEN_TEXT_SAVED_SEARCH_UNSET_PROFILE_DEFAULT' : 'SCREEN_TEXT_SAVED_SEARCH_SET_PROFILE_DEFAULT') | translateMultiple}}"
                          ng-click="saved.toggleProfileDefault(search)">${iconMarkup("lucide:user-check", 'ng-if="!saved.isProfileDefault(search)"')}${iconMarkup("lucide:user-x", 'ng-if="saved.isProfileDefault(search)"')}</button>
                </span>
                <span ng-if="saved.canManage(search) && saved.editing.id !== search.id">
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchRename}" ${owner}
                          aria-label="{{'SCREEN_TEXT_RENAME_SEARCH' | translateMultiple}} {{search.name}}" title="{{'SCREEN_TEXT_RENAME_SEARCH' | translateMultiple}}"
                          ng-click="saved.startRename(search)">${iconMarkup("lucide:pencil")}</button>
                  <button type="button" class="window-saved-search-action" data-testid="${TestIds.windowSavedSearchDelete}" ${owner}
                          aria-label="{{'SCREEN_TEXT_DELETE_SEARCH' | translateMultiple}} {{search.name}}" title="{{'SCREEN_TEXT_DELETE_SEARCH' | translateMultiple}}"
                          ng-click="saved.remove(search)">${iconMarkup("lucide:trash-2")}</button>
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-awe" data-dismiss="modal" data-testid="${TestIds.windowSavedSearchManageClose}" ${owner}>{{'BUTTON_CLOSE' | translateMultiple}}</button>
      </div>
    </div>
  </div>
</div>`;

const dialogsTemplate = `<div class="window-saved-search-dialogs">${saveDialogTemplate}${manageDialogTemplate}</div>`;

/**
 * Parse the values of a saved search
 * @param {string} values Values (JSON)
 * @returns {Object} Values by criterion id
 */
const parseValues = values => {
  try {
    const parsed = JSON.parse(values);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
};

/**
 * Source of the values of the saved searches of a window: its search criteria. Applying a search restores every search
 * criterion to its default (as Clear all), sets the saved values, shows its layout (#915) and launches the submit of
 * the window
 * @param {Object} block What the advanced block offers (searchIds, controllerOf, modelOf, modelAddressOf, valuesOf,
 * restore, layout)
 * @param {Object} elem Block element
 * @param {Object} services Control and Utilities services
 * @returns {Object} Source: snapshot(), currentValues(), parseValues(json), apply(values, onApplied) and
 * setValues(values, ids)
 */
function getCriteriaSource({searchIds, controllerOf, modelOf, modelAddressOf, valuesOf, restore, layout}, elem, {Control, Utilities}) {
  /**
   * Set the saved values of a criterion. The options of a list that it does not have are added with their labels, so
   * a suggest or a multiple select shows them
   * @param {string} id Criterion id
   * @param {Array} items Saved values with their labels
   */
  const setValues = (id, items) => {
    const component = controllerOf(id).component;
    const list = Utilities.asArray(items).filter(item => item && !Utilities.isEmpty(item.value));
    const values = list.map(item => item.value);
    const attributes = {selected: MULTIPLE.includes(component) ? values : (values.length > 0 ? values[0] : null)};
    if (LISTS.includes(component)) {
      const options = modelOf(id).values || [];
      const missing = list.filter(item => !options.some(option => String(option.value) === String(item.value)))
        .map(({value, label}) => ({value, label: Utilities.isEmpty(label) ? value : label}));
      if (missing.length > 0) {
        attributes.values = [...options, ...missing];
      }
    }
    Control.changeModelAttribute(modelAddressOf(id), attributes, true);
  };

  /**
   * Set the saved values of some search criteria: the ids the window no longer has are ignored
   * @param {Object} values Saved values by criterion id
   * @param {string[]} ids Criteria to set
   */
  const setCriteriaValues = (values, ids) => Object.keys(values)
    .filter(id => searchIds.includes(id) && ids.includes(id))
    .forEach(id => setValues(id, values[id]));

  return {
    // Values of the search criteria as the criteria show them
    snapshot: () => searchIds.map(id => valuesOf(modelOf(id).selected)),
    // Values to save: the selected values of every search criterion, with their labels
    currentValues: () => searchIds.reduce((all, id) => {
      const model = modelOf(id);
      all[id] = Utilities.asArray(model.selected).filter(value => !Utilities.isEmpty(value)).map(value => {
        const option = (model.values || []).find(item => String(item.value) === String(value));
        return {value, label: option && !Utilities.isEmpty(option.label) ? option.label : value};
      });
      return all;
    }, {}),
    parseValues,
    setValues: setCriteriaValues,
    apply(values, onApplied) {
      restore(null);
      setCriteriaValues(values, searchIds);
      // A search without layout shows the layout of the XML
      layout.set(values[LAYOUT] || []);
      // Once the criteria have taken the values
      Utilities.timeout(() => {
        onApplied();
        submitForm(elem);
      });
    }
  };
}

/**
 * Initialize the saved searches of a component: the advanced block of a window, or a grid (#912), that opts in
 * @param {Object} scope Block scope
 * @param {Object} elem Block element
 * @param {Object} block What the block offers: windowId (the component id), title, view, screenName() and either the
 * search criteria of a window (searchIds, controllerOf(id), modelOf(id), modelAddressOf(id), valuesOf(value), restore(ids),
 * layout: get(), set(ids), inUse(), labelOf(id) and listenRestores(onRestore), and the defaultSearch applied on load) or a
 * source of values (source: snapshot(), currentValues(), parseValues(json) and apply(values, onApplied))
 * @param {Object} services $compile, Control, Utilities, $actionController, $settings and Connection services
 * @returns {Object} Saved searches of the block
 */
export function initSavedSearches(scope, elem, block, {$compile, Control, Utilities, $actionController, $settings, Connection}) {
  const {windowId, view} = block;
  let profilesLoaded = false;
  let listening = false;

  // Run a scope change from a server answer or a DOM event, which may come inside or outside a digest
  const applyScope = fn => (scope.$root.$$phase ? fn() : scope.$apply(fn));
  const afterDigest = fn => scope.$$postDigest(fn);
  const toggleButton = () => elem.find(`[data-testid="${TestIds.windowSavedSearches}"]`);
  const menu = () => elem.find(`[data-testid="${TestIds.windowSavedSearchesMenu}"]`);
  const menuItems = () => menu().find("[role='menuitem']");

  // Show messages as the answers of the server are shown
  const report = messages => $actionController.addActionList(messages, false, {address: {view}, context: ""});
  // A failure already told to the user
  const REPORTED = {};

  /**
   * Rows of a query of the server. The messages of its answer are shown (an error answer has no rows, only its message)
   * and a call that fails shows an error message: the promise is then rejected
   * @param {string} target Query
   * @param {Object} values Parameters
   * @returns {Promise<Array>} Rows
   */
  const query = (target, values) => {
    const parameters = {...values};
    parameters[$settings.get("serverActionKey")] = "data";
    parameters[$settings.get("targetActionKey")] = target;
    return Connection.post(Connection.getActionUrl("data", target), parameters).then(response => {
      const actions = Utilities.asArray(Utilities.decodeData(response.data, $settings.get("encodeTransmission")))
        .filter(action => action && typeof action === "object");
      const fill = actions.find(action => action.type === "fill");
      const messages = actions.filter(action => action.type === "message");
      if (messages.length > 0) {
        report(messages);
      }
      // ($q promises reject with a returned rejection: a thrown error goes to the exception handler)
      if (!fill) {
        return Utilities.q.reject(messages.length > 0 ? REPORTED : null);
      }
      return fill.parameters?.datalist?.rows || [];
    }).catch(error => {
      if (error !== REPORTED) {
        report([_.cloneDeep(LOAD_ERROR)]);
      }
      return Utilities.q.reject(REPORTED);
    });
  };

  const source = block.source || getCriteriaSource(block, elem, {Control, Utilities});
  // A window search has a layout (#915), a grid search has not; both may be the default search (#915, #945)
  const windowSearch = !block.source;
  const defaultSearch = block.defaultSearch || null;
  // Values as the component shows them, to know whether they changed
  const snapshot = () => source.snapshot();

  /**
   * Call one of the saved search services (see launchSilentService)
   * @param {string} target Maintain target
   * @param {Object} values Service parameters
   * @param {function} onEnd Called once when the call ends
   * @param {function} onSuccess Called when the service worked
   */
  const launch = (target, values, onEnd, onSuccess) => launchSilentService({$actionController, $settings}, target, values,
    () => applyScope(onEnd), () => applyScope(onSuccess));

  /**
   * Show or hide one of the dialogs
   * @param {string} testId Test id of the dialog
   * @param {boolean} show Show it
   */
  const showDialog = (testId, show) => dialogs.find(`[data-testid="${testId}"]`).isolateScope().$broadcast("modalChange", show);

  /**
   * Place the menu under its button, in the coordinates of the viewport: the panel of the window clips its content, so
   * the menu is fixed and flips up when there is no room below
   */
  const positionMenu = () => afterDigest(() => {
    const button = toggleButton()[0];
    const menuElement = menu()[0];
    if (!button || !menuElement || !saved.open) {
      return;
    }
    const gap = parseFloat(getComputedStyle(document.documentElement).getPropertyValue("--awe-space-1")) || 4;
    const buttonBox = button.getBoundingClientRect();
    const menuBox = menuElement.getBoundingClientRect();
    const fitsBelow = buttonBox.bottom + gap + menuBox.height <= window.innerHeight;
    const top = fitsBelow ? buttonBox.bottom + gap : Math.max(gap, buttonBox.top - menuBox.height - gap);
    const left = Math.max(gap, Math.min(buttonBox.left, window.innerWidth - menuBox.width - gap));
    menuElement.style.top = `${Math.round(top)}px`;
    menuElement.style.left = `${Math.round(left)}px`;
  });

  // The menu is placed in viewport coordinates: a scroll or a resize would leave it away from its button
  const onViewportChange = () => applyScope(() => closeMenu(false));

  const onDocumentMouseDown = event => {
    if ($(event.target).closest(elem.find(".window-saved-searches")).length === 0) {
      applyScope(() => closeMenu(false));
    }
  };

  /**
   * Close the menu
   * @param {boolean} focus Give the focus back to the button
   */
  const closeMenu = focus => {
    saved.open = false;
    if (listening) {
      listening = false;
      $(document).off("mousedown", onDocumentMouseDown);
      $(window).off("resize scroll", onViewportChange);
    }
    if (focus) {
      // preventScroll: the window panel scrolls its content to reveal the element that takes the focus, and the menu
      // of the searches hangs from the body: focusing it moved the panel and hid the footer
      toggleButton()[0]?.focus({preventScroll: true});
    }
  };

  // The first option of the open menu takes the focus (when it opens, and when its searches arrive)
  const focusFirst = () => afterDigest(() => {
    const active = document.activeElement;
    if (saved.open && (active === toggleButton()[0] || menu()[0].contains(active))) {
      menuItems().first()[0]?.focus({preventScroll: true});
    }
  });

  /**
   * Load the searches of the window (and, once, the profiles the user may publish for)
   * @param {function} [onLoad] Called when the searches have arrived
   */
  const load = onLoad => {
    saved.loading = true;
    query("getWindowSavedSearches", {savedSearchScreen: block.screenName(), savedSearchWindow: windowId})
      .then(rows => {
        saved.searches = rows;
        saved.own = rows.filter(search => saved.isOwn(search));
        saved.shared = rows.filter(search => !saved.isOwn(search));
      })
      .catch(() => undefined)
      .finally(() => {
        saved.loading = false;
        if (onLoad) {
          onLoad();
        }
      });
    if (!profilesLoaded) {
      profilesLoaded = true;
      query("getSavedSearchProfiles", {})
        .then(rows => {
          saved.profiles = rows;
        })
        .catch(() => {
          profilesLoaded = false;
        });
    }
  };

  /**
   * Mark the default search of the user in the window, or clear it, and load the searches again
   * @param {*} id Search id, empty to clear it
   * @param {function} [onDone] Called once the searches are loaded again
   */
  const setDefault = (id, onDone) => launch("setWindowDefaultSearch", {
    savedSearchScreen: block.screenName(),
    savedSearchWindow: windowId,
    savedSearchId: id
  }, () => undefined, () => load(onDone));

  /**
   * The criteria back to the default search that opened the window: its values and its layout, and its chip
   * @param {string[]} ids Criteria to give their values
   */
  const toDefault = ids => {
    source.setValues(defaultSearch.values || {}, ids);
    block.layout.set(defaultSearch.layout || []);
    saved.applied = {id: defaultSearch.id, name: defaultSearch.name};
  };

  const saved = {
    windowId,
    windowSearch,
    title: block.title,
    menuId: `${windowId}-saved-searches-menu`,
    dialogId: `${windowId}-saved-search`,
    open: false,
    loading: false,
    searches: [],
    own: [],
    shared: [],
    profiles: [],
    current: null,
    // The default search applied when the window opened, while it is in effect
    applied: defaultSearch ? {id: defaultSearch.id, name: defaultSearch.name} : null,
    form: {name: "", profile: "", invalid: false, saving: false},
    editing: null,
    isOwn(search) {
      return Number(search.own) === 1;
    },
    isDefault(search) {
      return Number(search.default) === 1;
    },
    isProfileDefault(search) {
      return Number(search.profileDefault) === 1;
    },
    isCurrent(search) {
      return saved.current !== null && saved.current.id === search.id;
    },
    // A user who may publish manages the searches published for their profile too
    canManage(search) {
      return saved.isOwn(search) || saved.profiles.length > 0;
    },
    profileLabel(profile) {
      const known = saved.profiles.find(item => item.value === profile);
      return known ? known.label : profile;
    },
    toggleMenu() {
      if (saved.open) {
        closeMenu(true);
        return;
      }
      saved.open = true;
      if (!listening) {
        listening = true;
        $(document).on("mousedown", onDocumentMouseDown);
        $(window).on("resize scroll", onViewportChange);
      }
      focusFirst();
      positionMenu();
      load(() => {
        focusFirst();
        positionMenu();
      });
    },
    menuKeydown(event) {
      const options = menuItems().toArray();
      const index = options.indexOf(document.activeElement);
      const moves = {
        ArrowDown: (index + 1) % options.length,
        ArrowUp: (index - 1 + options.length) % options.length,
        Home: 0,
        End: options.length - 1
      };
      if (event.key in moves) {
        event.preventDefault();
        options[moves[event.key]].focus();
      } else if (event.key === "Escape") {
        event.preventDefault();
        closeMenu(true);
      } else if (event.key === "Tab") {
        closeMenu(false);
      }
    },
    // Apply the values of the search (a window restores its criteria and launches its submit, a grid restores its state
    // and reloads); the button names it once the component has taken them. A grid applying the default search it opened
    // with names it in the chip again (#945)
    apply(search) {
      closeMenu(true);
      saved.applied = !windowSearch && defaultSearch && String(search.id) === String(defaultSearch.id) ?
        {id: defaultSearch.id, name: search.name} : null;
      saved.current = {id: search.id, name: search.name, values: null};
      const applied = saved.current;
      source.apply(source.parseValues(search.values), () => {
        applied.values = snapshot();
      });
    },
    openSave() {
      closeMenu(true);
      // The layout to keep: the criteria of the main row and the advanced criteria in use that can move there (#915)
      const layout = windowSearch ? _.uniq([...block.layout.get(), ...block.layout.inUse()]) : [];
      // Keep the layout is opt-in: a plain save keeps the values only, as before #915
      saved.form = {name: saved.current ? saved.current.name : "", profile: "", invalid: false, saving: false,
        layout, layoutLabels: layout.map(id => block.layout.labelOf(id)), keepLayout: false, keepValues: true, openWith: false};
      showDialog(TestIds.windowSavedSearchDialog, true);
    },
    nameKeydown(event) {
      // Enter saves: it does not submit the form of the screen
      if (event.key === "Enter") {
        event.preventDefault();
        saved.save();
      }
    },
    save() {
      const name = (saved.form.name || "").trim();
      if (name === "") {
        saved.form.invalid = true;
        afterDigest(() => dialogs.find(`[data-testid="${TestIds.windowSavedSearchName}"]`).focus());
        return;
      }
      const form = saved.form;
      form.invalid = false;
      form.saving = true;
      const searchValues = form.keepValues === false ? {} : source.currentValues();
      if (windowSearch && form.keepLayout && form.layout.length > 0) {
        searchValues[LAYOUT] = form.layout;
      }
      const parameters = {
        savedSearchScreen: block.screenName(),
        savedSearchWindow: windowId,
        savedSearchName: name,
        savedSearchValues: JSON.stringify(searchValues),
        // Empty: a search of the user only
        savedSearchProfile: form.profile && form.profile !== ONLY_ME ? form.profile : ""
      };
      const values = snapshot();
      launch("saveWindowSearch", parameters, () => {
        form.saving = false;
      }, () => {
        form.saving = false;
        showDialog(TestIds.windowSavedSearchDialog, false);
        saved.current = {id: null, name, values};
        load(() => {
          const stored = form.profile ? saved.shared.find(search => search.name === name && search.profile === form.profile)
            : saved.own.find(search => search.name === name);
          if (saved.current && saved.current.name === name && stored && !form.profile) {
            saved.current.id = stored.id;
          }
          // Open the screen with this search: the default search of the user
          if (form.openWith && stored) {
            setDefault(stored.id);
          }
        });
      });
    },
    // Mark a search as the default of the user, or stop using it
    toggleDefault(search) {
      setDefault(saved.isDefault(search) ? "" : search.id, () => afterDigest(() => focusRow(search.id, TestIds.windowSavedSearchSetDefault)));
    },
    // Mark a published search as the default of its profile, or stop using it (a user who may publish)
    toggleProfileDefault(search) {
      launch("setProfileDefaultSearch", {
        savedSearchScreen: block.screenName(),
        savedSearchWindow: windowId,
        savedSearchId: search.id,
        savedSearchDefault: saved.isProfileDefault(search) ? "0" : "1"
      }, () => undefined, () => reloadAndFocus(() => focusRow(search.id, TestIds.windowSavedSearchSetProfileDefault)));
    },
    // Clear all: the default search is no longer in effect
    cleared() {
      saved.applied = null;
    },
    /**
     * Return criteria to the default search that opened the window (D7)
     * @param {string[]} ids Criteria
     * @returns {boolean} The window opened with a default search
     */
    restoreDefault(ids) {
      if (!windowSearch || !defaultSearch) {
        return false;
      }
      block.restore(ids);
      toDefault(ids);
      return true;
    },
    openManage() {
      closeMenu(true);
      saved.editing = null;
      showDialog(TestIds.windowSavedSearchManageDialog, true);
    },
    startRename(search) {
      saved.editing = {id: search.id, name: search.name, invalid: false};
      afterDigest(() => dialogs.find(`[data-testid="${TestIds.windowSavedSearchRenameInput}"]`).focus());
    },
    cancelRename() {
      const id = saved.editing ? saved.editing.id : null;
      saved.editing = null;
      afterDigest(() => focusRow(id, TestIds.windowSavedSearchRename));
    },
    renameKeydown(event) {
      if (event.key === "Enter") {
        event.preventDefault();
        saved.rename();
      } else if (event.key === "Escape") {
        // Escape leaves the rename, not the dialog
        event.preventDefault();
        event.stopPropagation();
        saved.cancelRename();
      }
    },
    rename() {
      const editing = saved.editing;
      const name = (editing.name || "").trim();
      if (name === "") {
        editing.invalid = true;
        return;
      }
      editing.invalid = false;
      launch("renameWindowSearch", {savedSearchId: editing.id, savedSearchName: name}, () => undefined, () => {
        if (saved.editing === editing) {
          saved.editing = null;
        }
        if (saved.current && saved.current.id === editing.id) {
          saved.current.name = name;
        }
        reloadAndFocus(() => focusRow(editing.id, TestIds.windowSavedSearchRename));
      });
    },
    // Delete a search once the user confirms it
    remove(search) {
      const addressData = {address: {view}, context: ""};
      const confirm = {type: "confirm", parameters: {title: "SCREEN_TEXT_DELETE_SEARCH", message: "SCREEN_TEXT_DELETE_SEARCH_MESSAGE"}};
      const deletion = $actionController.generateAction({type: "saved-search-delete", silent: true}, addressData, true, false);
      deletion.launchAction = function () {
        launch("deleteWindowSearch", {savedSearchId: search.id}, () => undefined, () => {
          if (saved.current && saved.current.id === search.id) {
            saved.current = null;
          }
          reloadAndFocus(focusManageClose);
        });
        this.accept();
      };
      $actionController.addActionList([confirm, deletion], false, addressData);
    }
  };

  /**
   * Load the searches again and give the focus to an element once they are drawn
   * @param {Function} focus Gives the focus
   */
  function reloadAndFocus(focus) {
    load(() => afterDigest(focus));
  }

  /**
   * Give the focus to the close button of the manage dialog
   */
  function focusManageClose() {
    dialogs.find(`[data-testid="${TestIds.windowSavedSearchManageClose}"]`).focus();
  }

  /**
   * Give the focus to a button of the row of a search
   * @param {*} id Search id
   * @param {string} testId Test id of the button
   */
  function focusRow(id, testId) {
    dialogs.find(`[data-testid="${TestIds.windowSavedSearchRow}"]`)
      .filter((index, row) => row.getAttribute(TestAttributes.savedSearchId) === String(id))
      .find(`[data-testid="${testId}"]`).focus();
  }

  scope.saved = saved;

  // The dialogs live with the dialogs of the screen (in its form), out of the window
  const dialogs = $compile(dialogsTemplate)(scope);
  const form = elem.closest("form");
  (form.length > 0 ? form : $(document.body)).append(dialogs);

  // The button names the search applied until a value of the criteria changes
  scope.$watch(snapshot, values => {
    if (saved.current && saved.current.values && !_.isEqual(values, saved.current.values)) {
      saved.current = null;
    }
  }, true);

  scope.$on("$destroy", () => {
    closeMenu(false);
    dialogs.remove();
  });

  if (windowSearch && defaultSearch) {
    // The server gave its values to the criteria before the initial load: only its layout is shown, once they are drawn
    afterDigest(() => block.layout.set(defaultSearch.layout || []));
    // The restore buttons of the window return the criteria they restore to it (D7)
    let restored = [];
    block.layout.listenRestores(id => {
      restored.push(id);
      if (restored.length === 1) {
        Utilities.timeout(() => {
          const ids = restored;
          restored = [];
          applyScope(() => toDefault(ids));
        });
      }
    });
  }

  return saved;
}

/**
 * State of an option of the saved searches for the tests (#915): "data-selected" true or false as the expression. An
 * interpolated data-selected would be taken for the boolean selected of a checkbox or a button
 */
aweApplication.directive("aweSavedSearchSelected", [() => ({
  restrict: "A",
  link: (scope, elem, attrs) => scope.$watch(attrs.aweSavedSearchSelected, selected => elem.attr(TestAttributes.selected, selected ? "true" : "false"))
})]);

/**
 * Profile to publish a search for, in the Save search dialog: the select of the engine (select2, as the select
 * criteria) with Only me and the profiles the user may publish for. Its trigger carries the test hook
 */
aweApplication.directive("aweSavedSearchProfile",
  ["$translate",
    function ($translate) {
      return {
        restrict: "E",
        // Its own scope: the select plugin works with the component of its scope
        scope: true,
        template: `<input type="hidden" class="form-control" ui-select2="profileOptions" initialized="profileReady" autocomplete="off"
                          id="{{::saved.dialogId}}-profile"/>`,
        link: function (scope, elem) {
          const saved = scope.saved;
          let initialized = false;
          const options = () => [{id: ONLY_ME, text: $translate.instant("SCREEN_TEXT_ONLY_ME")},
            ...saved.profiles.map(profile => ({id: profile.value, text: profile.label}))];
          const selected = () => saved.form.profile || ONLY_ME;

          scope.profileReady = true;
          scope.profileOptions = {
            minimumResultsForSearch: 5,
            query: query => {
              const term = String(query.term || "").toUpperCase();
              query.callback({results: options().filter(option => option.text.toUpperCase().includes(term))});
            },
            initSelection: (node, callback) => callback(options().find(option => option.id === node.val()) || options()[0])
          };
          scope.component = {
            id: saved.windowId,
            onPluginInit() {
              initialized = true;
              elem.find(".select2-choice")
                .attr(TestAttributes.testId, TestIds.windowSavedSearchProfile)
                .attr(TestAttributes.owner, saved.windowId);
              elem.find(".select2-focusser").attr("aria-describedby", `${saved.dialogId}-profile-help`);
              scope.component.select(selected());
            },
            onPluginChange(event) {
              saved.form.profile = event.val === ONLY_ME ? "" : event.val;
            },
            changeFocus: () => undefined
          };

          // Every Save search starts with Only me; the texts follow the language
          scope.$watch(() => saved.form, () => {
            if (initialized) {
              scope.component.select(selected());
            }
          });
          scope.$on("languageChanged", () => {
            if (initialized) {
              scope.component.select(selected());
            }
          });
        }
      };
    }
  ]);
