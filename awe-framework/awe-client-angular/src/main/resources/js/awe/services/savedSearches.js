import {aweApplication} from "../awe";
import {TestAttributes, TestIds} from "../data/testIds";
import {submitForm} from "./criterion";
import {iconMarkup} from "./icon";
import {launchSilentService} from "./silentService";
import "../directives/plugins/uiSelect";

// Saved searches of a window (#790, delivery 3): a window with an advanced block and saved-searches="true" has a Saved
// searches menu in the row of the block, with the searches of the user and the ones published for their profile.
// Applying one restores every search criterion of the window to its default, sets the saved values and launches the
// submit of the window. The user is never sent: the server takes it (and its profile) from the session.

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

const optionTemplate = (type, icon) => `<button type="button" role="menuitem" tabindex="-1" class="window-saved-searches-option"
          ng-repeat="search in saved.${type} track by search.id" data-testid="${TestIds.windowSavedSearchItem}" ${owner} ${savedSearchId}
          data-type="${type}" ng-attr-aria-current="{{saved.isCurrent(search) ? 'true' : undefined}}" ng-click="saved.apply(search)">
      ${iconMarkup(icon)}
      <span class="window-saved-searches-text">{{search.name}}</span>
      ${type === "shared" ? `<span class="window-saved-searches-tag">{{saved.profileLabel(search.profile)}}</span>` : ""}
    </button>`;

// The Saved searches button (with the name of the search applied while its values do not change) and its menu
export const savedSearchesTemplate = `<div class="window-saved-searches" ng-if="::saved">
  <button type="button" class="window-saved-searches-toggle" data-testid="${TestIds.windowSavedSearches}" ${owner}
          aria-haspopup="menu" ng-attr-aria-expanded="{{saved.open}}" aria-controls="{{::saved.menuId}}" ng-click="saved.toggleMenu()">
    ${iconMarkup("lucide:bookmark")}
    <span class="window-saved-searches-label" ng-if="!saved.current">{{'SCREEN_TEXT_SAVED_SEARCHES' | translateMultiple}}</span>
    <span class="window-saved-searches-label" ng-if="saved.current">{{saved.current.name}}</span>
    ${iconMarkup("lucide:chevron-down window-saved-searches-chevron")}
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

// The Save search dialog: the name (required) and, only for a user who may publish, the profile to publish it for
const saveDialogTemplate = `<div class="modal fade window-saved-search-modal" ui-modal trap-tab role="dialog" aria-modal="true"
     aria-labelledby="{{::saved.dialogId}}-save-title" data-testid="${TestIds.windowSavedSearchDialog}" ${owner}>
  <div class="modal-dialog modal-sm">
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
 * Initialize the saved searches of the advanced block of a window: only for a window that opts in
 * @param {Object} scope Block scope
 * @param {Object} elem Block element
 * @param {Object} block What the block offers: windowId, title, searchIds, view, screenName(), controllerOf(id),
 * modelOf(id), modelAddressOf(id), valuesOf(value) and restore(ids)
 * @param {Object} services $compile, Control, Utilities, $actionController, $settings and Connection services
 * @returns {Object} Saved searches of the block
 */
export function initSavedSearches(scope, elem, block, {$compile, Control, Utilities, $actionController, $settings, Connection}) {
  const {windowId, searchIds, view, controllerOf, modelOf, modelAddressOf, valuesOf, restore} = block;
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

  // Values of the search criteria as the criteria show them, to know whether they changed
  const snapshot = () => searchIds.map(id => valuesOf(modelOf(id).selected));

  /**
   * Values to save: the selected values of every search criterion, with their labels
   * @returns {Object} Values by criterion id
   */
  const currentValues = () => searchIds.reduce((all, id) => {
    const model = modelOf(id);
    all[id] = Utilities.asArray(model.selected).filter(value => !Utilities.isEmpty(value)).map(value => {
      const option = (model.values || []).find(item => String(item.value) === String(value));
      return {value, label: option && !Utilities.isEmpty(option.label) ? option.label : value};
    });
    return all;
  }, {});

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
    }
    if (focus) {
      toggleButton().focus();
    }
  };

  // The first option of the open menu takes the focus (when it opens, and when its searches arrive)
  const focusFirst = () => afterDigest(() => {
    const active = document.activeElement;
    if (saved.open && (active === toggleButton()[0] || menu()[0].contains(active))) {
      menuItems().first().focus();
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

  const saved = {
    windowId,
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
    form: {name: "", profile: "", invalid: false, saving: false},
    editing: null,
    isOwn(search) {
      return Number(search.own) === 1;
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
      }
      focusFirst();
      load(focusFirst);
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
    // Restore every search criterion to its default (as Clear all), set the saved values and launch the submit
    apply(search) {
      closeMenu(true);
      const values = parseValues(search.values);
      restore(null);
      Object.keys(values).filter(id => searchIds.includes(id)).forEach(id => setValues(id, values[id]));
      saved.current = {id: search.id, name: search.name, values: null};
      const applied = saved.current;
      // Once the criteria have taken the values
      Utilities.timeout(() => {
        applied.values = snapshot();
        submitForm(elem);
      });
    },
    openSave() {
      closeMenu(true);
      saved.form = {name: saved.current ? saved.current.name : "", profile: "", invalid: false, saving: false};
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
      const parameters = {
        savedSearchScreen: block.screenName(),
        savedSearchWindow: windowId,
        savedSearchName: name,
        savedSearchValues: JSON.stringify(currentValues()),
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
          const stored = saved.own.find(search => search.name === name);
          if (saved.current && saved.current.name === name && stored && !form.profile) {
            saved.current.id = stored.id;
          }
        });
      });
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

  return saved;
}

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
