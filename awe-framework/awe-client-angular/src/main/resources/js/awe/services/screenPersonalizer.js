import {aweApplication} from "../awe";
import {
  attributeLabelKey, buildChanges, columnVisibility, isColumnCatalog, countInvalid, describeOverview, orderAttributes, ownRule, previewValue, resolveValue, toMaintainRequest,
  validateValue, withDefaults
} from "awe-client-react/utilities/screenPersonalization";
import {INSPECT_VIEW, revealHiddenElements} from "./screenInspect";

/** Server side of the visual screen personalization editor (#792): reads the rules, the structure and the attributes, saves the changes */

// A cell of a list row is a value or {value, label}
const cell = value => (value !== null && typeof value === "object" ? value.value : value);
const blankToNull = value => (value === "" || value === undefined ? null : value);

/**
 * Rules of a screen from the rows of the ScrCnfLst query
 * @param {Array} rows Rows
 * @returns {Array} Rules
 */
export const toRules = rows => rows.map(row => ({
  id: Number(cell(row.IdeAweScrCnf)),
  user: blankToNull(cell(row.IdeOpe) ?? cell(row.IdeOpeVal)),
  profile: blankToNull(cell(row.IdePro) ?? cell(row.IdeProVal)),
  component: cell(row.Nam) ?? cell(row.NamVal),
  attribute: cell(row.Atr) ?? cell(row.AtrVal),
  value: cell(row.Val) ?? "",
  active: Number(cell(row.Act))
}));

/**
 * Rules of the overview from the rows of getScreenConfigurationOverview
 * @param {Array} rows Rows
 * @returns {Array} Overview rows (ruleId as a number, the rest as text)
 */
export const toOverviewRows = rows => rows.map(row => ({
  ruleId: Number(cell(row.ruleId)), scopeType: cell(row.scopeType),
  ...Object.fromEntries(["component", "attribute", "profileId", "profileName", "userId", "userName", "value", "problem", "duplicateGroup"].map(key => [key, cell(row[key]) ?? ""]))
}));

/**
 * Elements that can be personalized from the rows of getScreenStructure, by identifier. A column knows its grid
 * @param {Array} rows Rows
 * @returns {Object} Elements by identifier
 */
export function toElements(rows) {
  const byPath = Object.fromEntries(rows.map(row => [cell(row.path), row]));
  const gridOf = row => {
    let parent = byPath[cell(row.parent)];
    while (parent && cell(parent.elementType) !== "Grid") {
      parent = byPath[cell(parent.parent)];
    }
    return parent ? cell(parent.id) : null;
  };
  return Object.fromEntries(rows.filter(row => String(cell(row.configurable)) === "true")
    .map(row => [cell(row.id), {id: cell(row.id), label: cell(row.label), elementType: cell(row.elementType), grid: gridOf(row)}]));
}

/**
 * Attributes of an element from the rows of getScreenElementAttributes, as the editor shows them
 * @param {Array} rows Rows
 * @returns {{catalog: Array, isColumn: boolean}} Attributes (a column shows Visible instead of hidden) and whether the element is a column
 */
export function toCatalog(rows) {
  const raw = rows.map(row => ({
    attribute: cell(row.attribute), valueType: cell(row.valueType), value: cell(row.value) ?? "",
    allowedValues: (cell(row.allowedValues) || "").split(",").filter(Boolean)
  }));
  const catalog = columnVisibility(withDefaults(raw));
  return {catalog, isColumn: isColumnCatalog(raw)};
}

/**
 * Valid pending values to show live in the components, typed as the component reads them. The Visible of an element is
 * drawn by the inspect mode and a column is not previewed
 * @param {Object} pending Pending edits ("component|attribute" -> value)
 * @param {Object} meta Value type and allowed values by "component|attribute"
 * @param {Object} elements Elements by identifier
 * @returns {Array<{id: string, attribute: string, value: *}>} Values to apply
 */
export function previewPlan(pending, meta, elements) {
  return Object.entries(pending).flatMap(([key, value]) => {
    const [id, attribute] = key.split("|");
    const info = meta[key];
    // A reset (null) shows the default value of the attribute
    const shown = value === null && info ? info.value ?? "" : value;
    const typed = typeof shown === "string" && info ? previewValue(info.valueType, shown, info.allowedValues) : undefined;
    return typed === undefined || attribute === "visible" || !elements[id] || elements[id].elementType === "Column" ? [] : [{id, attribute, value: typed}];
  });
}

/**
 * What the canvas draws as hidden once the pending Visible edits are applied
 * @param {{hidden: Set<string>, hiddenColumns: Set<string>}} base What the screen hides
 * @param {Object} pending Pending edits
 * @param {Object} elements Elements by identifier
 * @returns {{hidden: Set<string>, hiddenColumns: Set<string>}} Hidden elements and columns
 */
export function previewHidden(base, pending, elements) {
  const hidden = new Set(base.hidden);
  const hiddenColumns = new Set(base.hiddenColumns);
  Object.entries(pending).forEach(([key, value]) => {
    const [id, attribute] = key.split("|");
    const typed = attribute === "visible" && elements[id] ? previewValue("BOOLEAN", value ?? "", []) : undefined;
    if (typed !== undefined) {
      const target = elements[id].elementType === "Column" ? hiddenColumns : hidden;
      target[typed ? "delete" : "add"](id);
    }
  });
  return {hidden, hiddenColumns};
}

// Attributes whose value can be a locale key, shown in the current language
const TRANSLATABLE = ["label", "title", "placeholder"];
const ORIGIN_KEYS = {default: "SCR_PER_ORIGIN_DEFAULT", everyone: "SCR_PER_ORIGIN_EVERYONE", profile: "SCR_PER_ORIGIN_PROFILE", user: "SCR_PER_ORIGIN_USER"};
const MAX_VALUE_LENGTH = 60;

/**
 * Rows of the side panel of an element: the attributes with the value to show, where it comes from and the error of an invalid value
 * @param {{element: Object, catalog: Array, rules: Array, pending: Object, scope: Object, t: function(string, ...*): string}} data
 *   Element, its attributes, the rules as the editor reads them, the pending edits, the scope and the translation function
 * @returns {{common: Array, advanced: Array}} Rows
 */
export function buildPanel({element, catalog, rules, pending, scope, t}) {
  const mine = rules.filter(rule => rule.component === element.id);
  const row = item => {
    const {attribute} = item;
    const resolved = resolveValue(rules, pending, element.id, attribute, scope, item.value);
    const edit = pending[`${element.id}|${attribute}`];
    const overridden = typeof edit === "string" || (edit === undefined && !!ownRule(mine, attribute, scope));
    const error = typeof edit === "string" ? validateValue(item.valueType, edit, item.allowedValues) : null;
    // A label that is a locale key shows its text (the key stays as helper text); a typed one is kept as typed
    const text = typeof edit === "string" || !TRANSLATABLE.includes(attribute) ? resolved.value : t(resolved.value);
    const translated = t(attributeLabelKey(attribute));
    const label = translated === attributeLabelKey(attribute) ? attribute : translated;
    return {
      attribute, label, valueType: item.valueType, allowedValues: item.allowedValues, value: resolved.value, text,
      key: text !== resolved.value ? resolved.value : "",
      origin: overridden ? t("SCR_PER_ORIGIN_PERSONALIZED") : t(ORIGIN_KEYS[resolved.origin]), overridden,
      resetLabel: `${t("SCR_PER_RESET_ATTRIBUTE")} ${label}`,
      error: error && t(`ERROR_MESSAGE_SCREEN_CONFIGURATION_${error}`, ...(error === "VALUE_TOO_LONG" ? [attribute, MAX_VALUE_LENGTH] : [attribute, edit, item.allowedValues.join(", ")]))
    };
  };
  const {common, advanced} = orderAttributes(catalog, element.elementType);
  return {common: common.map(row), advanced: advanced.map(row)};
}

aweApplication.factory("ScreenPersonalizer",
  ['Connection', 'AweSettings', 'AweUtilities', 'Control', 'ActionController', 'ServerData', 'Storage',
    /**
     * Screen personalization service
     * @param {Object} Connection Connection service
     * @param {Object} $settings AWE settings
     * @param {Object} Utilities AWE utilities
     * @param {Object} Control Control service
     * @param {Object} $actionController Action controller
     * @param {Object} ServerData Server data service
     * @param {Object} Storage Storage service
     */
    function (Connection, $settings, Utilities, Control, $actionController, ServerData, Storage) {
      const $personalizer = {
        buildChanges,
        countInvalid,
        describeOverview,
        /**
         * Call an action of the server and decode its answer
         * @param {string} action Action (data or maintain)
         * @param {string} target Target
         * @param {Object} values Parameters
         * @returns {Promise<Array>} Actions of the answer
         */
        call: function (action, target, values) {
          const parameters = {...values};
          parameters[$settings.get("serverActionKey")] = action;
          parameters[$settings.get("targetActionKey")] = target;
          return Connection.post(Connection.getActionUrl(action, target), parameters)
            .then(response => Utilities.asArray(Utilities.decodeData(response.data, $settings.get("encodeTransmission"))).filter(item => item && typeof item === "object"));
        },
        /**
         * Rows of a query of the server
         * @param {string} target Query
         * @param {Object} values Parameters
         * @returns {Promise<Array>} Rows
         */
        query: function (target, values) {
          return $personalizer.call("data", target, {max: 0, ...values})
            .then(actions => actions.find(action => action.type === "fill")?.parameters?.datalist?.rows || []);
        },
        /**
         * Save the changes through the update maintain; the answer messages are shown
         * @param {Array} changes Changes to save
         * @returns {Promise<boolean>} Whether the server accepted them
         */
        save: function (changes) {
          return $personalizer.call("maintain", "updateScreenConfiguration", toMaintainRequest(changes)).then(actions => {
            $actionController.addActionList(actions, false, {address: {view: INSPECT_VIEW}, context: ""});
            return !actions.some(action => action.type === "message" && action.parameters?.type === "error");
          }).catch(() => false);
        },
        /**
         * Load the screen under edit in its own view and show what it hides. A screen is loaded through the menu option that opens it
         * @param {string} screen Screen identifier
         * @param {Set<string>} configurable Elements that can be personalized
         * @param {Object} subject Request parameters that render the screen as a subject gets it
         * @returns {Promise<{template: string, hidden: Set<string>, hiddenColumns: Set<string>}|null>} Screen template, or null when no option opens it
         */
        loadScreen: function (screen, configurable, subject) {
          return $personalizer.query("getScreenOption", {screen}).then(rows => {
            const option = cell(rows[0]?.option);
            return option ? ServerData.getScreenData(option, INSPECT_VIEW, false, subject) : null;
          }).then(template => {
            if (!template) {
              return null;
            }
            return {template, ...revealHiddenElements(Storage.get("screenData-" + INSPECT_VIEW).components, configurable)};
          });
        },
        /**
         * Put the values in the components of the canvas and give back the original of what is no longer in the plan
         * @param {Array<{id: string, attribute: string, value: *}>} plan Values to show
         * @param {Object} originals Values the components had before ("component|attribute" -> value), kept between calls
         */
        applyPreview: function (plan, originals) {
          const planned = new Set(plan.map(({id, attribute}) => `${id}|${attribute}`));
          const change = (id, attribute, value) => Control.changeControllerAttribute({view: INSPECT_VIEW, component: id}, {[attribute]: value});
          Object.keys(originals).filter(key => !planned.has(key)).forEach(key => {
            const [id, attribute] = key.split("|");
            change(id, attribute, originals[key]);
            delete originals[key];
          });
          plan.forEach(({id, attribute, value}) => {
            const key = `${id}|${attribute}`;
            const current = Control.getAddressController({view: INSPECT_VIEW, component: id})?.[attribute];
            if (!(key in originals)) {
              originals[key] = current;
            }
            if (current !== value) {
              change(id, attribute, value);
            }
          });
        }
      };
      return $personalizer;
    }
  ]);
