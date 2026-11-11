import $ from "jquery";
import {aweApplication} from "../awe";
import {SCREEN_SEARCH, buildExport, canvasSubject, chosenUserText, deleteChanges, describeHistory, describeSkipped, importLogins, keepOneChanges, latestOnly, limitMatches, planImport, subjectParams, toStoragePending, visibleRules} from "awe-client-react/utilities/screenPersonalization";
import {buildPanel, previewHidden, previewPlan, toCatalog, toElements, toOverviewRows, toRules} from "../services/screenPersonalizer";
import {INSPECT_VIEW, markInspect, swallowInspectEvents} from "../services/screenInspect";

const cell = value => (value !== null && typeof value === "object" ? value.value : value);
const toHistoryRows = rows => rows.map(row => ({...Object.fromEntries(Object.entries(row).map(([key, value]) => [key, cell(value)])),
  ruleId: Number(cell(row.ruleId))}));
const EXPORT_KEYS = ["component", "attribute", "value", "scopeType", "profile", "user"];
const toExportRows = rows => rows.map(row => Object.fromEntries(EXPORT_KEYS.map(key => [key, cell(row[key]) ?? ""])));

// Visual screen personalization editor (#792): the chosen screen is drawn with its real components in inspect mode,
// a click selects an element and a non-modal side panel edits its attributes for the chosen scope
aweApplication.directive('aweScreenPersonalizer',
  ['ServerData', 'Component', 'ScreenPersonalizer', '$controller', '$compile', '$translate', '$timeout',
    /**
     * Screen personalizer directive
     * @param {Service} serverData Server call service
     * @param {Function} Component Component constructor
     * @param {Service} $personalizer Screen personalization service
     * @param {Function} $controller Controller service
     * @param {Function} $compile Compile service
     * @param {Service} $translate Translate service
     * @param {Function} $timeout Timeout service
     */
    function (serverData, Component, $personalizer, $controller, $compile, $translate, $timeout) {
      return {
        restrict: 'E',
        replace: true,
        templateUrl: function () {
          return serverData.getAngularTemplateUrl('screenPersonalizer');
        },
        scope: {
          'widgetId': '@screenPersonalizerId'
        },
        /**
         * Link function
         * @param {Object} scope Directive scope
         * @param {Object} elem Directive node
         */
        link: function (scope, elem) {
          // Init as component
          const component = new Component(scope, scope.widgetId);
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }

          const canvas = elem[0].querySelector(".scr-per-canvas");
          // Values the canvas components had before a pending edit was previewed on them ("component|attribute" -> value)
          let originals = {};
          // Columns whose Visible is the inverse of their hidden attribute, what the screen hides, and the scope of the canvas
          let columns = new Set();
          let base = {hidden: new Set(), hiddenColumns: new Set()};
          let canvasScope = null;
          let observer = null;
          const meta = {};

          const t = (key, ...args) => args.reduce((text, arg, index) => text.replace(`{${index}}`, arg), $translate.instant(key));
          Object.assign(scope, {
            scopeTypes: ["everyone", "profile", "user"], screenInput: {text: "", suggestions: []}, user: {text: "", suggestions: []}, profiles: [],
            screen: null, applies: {type: "everyone", id: null}, viewAs: {type: "scope", id: null}, viewAsUser: {text: "", suggestions: []}, rules: [], elements: {}, selected: null, catalog: [], pending: {},
            noOption: false, panel: {common: [], advanced: []}, mode: "edit", modes: ["edit", "overview", "history"], overview: null, history: null, importPlan: null
          });

          const personalized = () => new Set(scope.rules.filter(rule => rule.active === 1).map(rule => rule.component));
          const paint = () => {
            markInspect(canvas, {configurable: new Set(Object.keys(scope.elements)), selected: scope.selected, personalized: personalized(),
              ...previewHidden(base, scope.pending, scope.elements)});
          };
          const refreshPanel = () => {
            const element = scope.elements[scope.selected];
            scope.panel = element ? buildPanel({element, catalog: scope.catalog, rules: visibleRules(scope.rules, columns), pending: scope.pending, scope: scope.applies, t})
              : {common: [], advanced: []};
          };
          const update = () => {
            refreshPanel();
            $personalizer.applyPreview(previewPlan(scope.pending, meta, scope.elements), originals);
            paint();
          };
          const reset = () => {
            scope.selected = null;
            scope.pending = {};
            scope.noOption = false;
            originals = {};
          };

          /** Remove the screen of the canvas and its components */
          const clearCanvas = () => {
            observer?.disconnect();
            if (canvasScope) {
              scope.$root.$broadcast("unload", INSPECT_VIEW);
              canvasScope.$destroy();
              canvasScope = null;
            }
            canvas.innerHTML = "";
          };

          /**
           * Draw the screen in the canvas, in a view of its own
           * @param {{template: string, hidden: Set<string>, hiddenColumns: Set<string>}} result Screen and what it hides
           */
          const render = result => {
            clearCanvas();
            base = {hidden: result.hidden, hiddenColumns: result.hiddenColumns};
            canvasScope = scope.$new();
            // The view controller replaces the screen of the page: the editor keeps its own
            const screen = scope.$root.screen;
            $controller("ViewController", {$scope: canvasScope, screenData: INSPECT_VIEW, context: ""});
            scope.$root.screen = screen;
            canvas.innerHTML = result.template;
            $compile(canvas.childNodes)(canvasScope);
            // The screen draws itself as its components load: mark them as they appear
            observer = new MutationObserver(paint);
            observer.observe(canvas, {childList: true, subtree: true});
            paint();
          };

          // Overview mode: what each subject sees and the rules that no longer work, read from the server whenever it is opened
          let overviewRows = [];
          const refreshOverview = () => {
            scope.overview = $personalizer.describeOverview(scope.screenInput.text || scope.screen, overviewRows, scope.elements, t, key => $translate.instant(key));
          };
          const loadOverview = () => $personalizer.query("getScreenConfigurationOverview", {screen: scope.screen}).then(rows => {
            overviewRows = toOverviewRows(rows);
            refreshOverview();
          });

          // History mode: who changed what and when, read from the server whenever it is opened
          const loadHistory = () => $personalizer.query("getScreenConfigurationHistory", {screen: scope.screen}).then(rows => {
            scope.history = describeHistory(scope.screen, toHistoryRows(rows), scope.elements, t, key => $translate.instant(key));
          });

          const loadRules = id => $personalizer.query("ScrCnfLst", {CrtScr: id}).then(rows => {
            if (scope.screen === id) {
              scope.rules = toRules(rows);
              update();
            }
          });
          // Who the canvas shows the screen to; a profile or user not chosen yet keeps the last complete subject
          let lastSubject = {type: "everyone", id: null};
          const subject = () => {
            lastSubject = canvasSubject(scope.applies, scope.viewAs, lastSubject);
            return lastSubject;
          };
          /** Draw the screen of the canvas as the subject gets it; the pending edits keep previewing on top */
          const loadCanvas = id => {
            clearCanvas();
            originals = {};
            scope.noOption = false;
            return $personalizer.loadScreen(id, new Set(Object.keys(scope.elements)), subjectParams(subject())).then(result => {
              if (scope.screen !== id) {
                return;
              }
              if (result) {
                render(result);
                update();
              } else {
                scope.noOption = true;
              }
            });
          };
          const loadScreen = id => {
            reset();
            clearCanvas();
            loadRules(id);
            return $personalizer.query("getScreenStructure", {screen: id}).then(rows => {
              scope.elements = toElements(rows);
              if (scope.mode === "overview") {
                loadOverview();
              }
              return scope.screen === id ? loadCanvas(id) : null;
            });
          };
          // The canvas is loaded again only when the subject changes
          const subjectChanged = () => {
            const before = JSON.stringify(lastSubject);
            if (scope.screen && JSON.stringify(subject()) !== before) {
              loadCanvas(scope.screen);
            }
          };

          $personalizer.query("ProSel", {}).then(rows => {
            scope.profiles = rows.map(row => ({label: cell(row.label), value: Number(cell(row.value)), acronym: cell(row.Acr)}));
          });

          // The badge of a hidden element shows the word in the current language
          canvas.style.setProperty("--awe-inspect-hidden-text", JSON.stringify(t("SCR_PER_HIDDEN")));

          scope.t = t;
          scope.profileLabel = () => scope.profiles.find(item => item.value === scope.applies.id)?.label;
          scope.viewAsProfileLabel = () => scope.profiles.find(item => item.value === scope.viewAs.id)?.label;
          scope.scopeLabel = () => scope.applies.type === "everyone" ? t("SCR_PER_SCOPE_EVERYONE")
            : scope.applies.type === "profile" ? (scope.profileLabel() || t("PARAMETER_PROFILE")) : (chosenUserText(scope.applies, scope.user.text) || t("PARAMETER_USER"));

          // The screens are searched on the server as the user types; an older answer never replaces a newer one
          const nextScreenSearch = latestOnly();
          scope.searchScreens = () => {
            const text = scope.screenInput.text.trim();
            const current = nextScreenSearch();
            if (text.length < SCREEN_SEARCH.minChars) {
              scope.screenInput.suggestions = [];
              return;
            }
            $personalizer.query("getScreenList", {suggest: text, max: SCREEN_SEARCH.limit}).then(rows => {
              if (current()) {
                scope.screenInput.suggestions = limitMatches(rows.map(row => ({label: cell(row.label), value: cell(row.value)})), SCREEN_SEARCH.limit);
              }
            });
          };
          scope.chooseScreen = item => {
            scope.screenInput = {text: item.label, suggestions: []};
            scope.screen = item.value;
            loadScreen(item.value);
          };
          // The grids of the drawn screen size themselves while they are shown: back in Edit they are told to resize
          const showEditor = () => $timeout(() => scope.$broadcast("resize"));
          scope.setMode = mode => {
            const leavingOther = scope.mode !== "edit";
            scope.mode = mode;
            if (mode === "edit" && leavingOther) {
              showEditor();
            }
            if (mode === "overview" && scope.screen) {
              loadOverview();
            }
            if (mode === "history" && scope.screen) {
              loadHistory();
            }
          };
          // A click in the overview opens the element in the editor at the scope of the row clicked (everyone for the title)
          scope.openFromOverview = (component, entry) => {
            scope.applies = entry ? {type: entry.scope, id: entry.id} : {type: "everyone", id: null};
            if (entry?.scope === "user") {
              scope.user = {text: entry.name, suggestions: []};
            }
            scope.mode = "edit";
            showEditor();
            subjectChanged();
            scope.select(component);
          };
          // Changes go to the update maintain; the rules and the canvas are read again when the server accepts them
          const saveChanges = (changes, reload) => $personalizer.save(changes).then(worked => {
            if (worked) {
              loadRules(scope.screen);
              loadCanvas(scope.screen);
              reload();
            }
            return worked;
          });
          const removeRules = changes => saveChanges(changes, loadOverview);
          scope.undo = changes => saveChanges(changes, loadHistory);
          // The rules of the screen go to a file, by profile acronym and user login so they can be imported elsewhere
          scope.exportRules = () => $personalizer.query("getScreenConfigurationExport", {screen: scope.screen}).then(rows => {
            const url = URL.createObjectURL(new Blob([JSON.stringify(buildExport(scope.screen, toExportRows(rows)), null, 2)], {type: "application/json"}));
            const link = document.createElement("a");
            link.href = url;
            link.download = `${scope.screen}.json`;
            link.click();
            URL.revokeObjectURL(url);
          });
          // A chosen file is compared with the rules here; nothing is saved until Apply import
          const importFile = elem[0].querySelector(".scr-per-import-file");
          const compare = content => {
            const logins = importLogins(content);
            const plan = found => {
              scope.importPlan = planImport(content, scope.screen, scope.rules, scope.elements, scope.profiles.map(profile => ({id: profile.value, acronym: profile.acronym})),
                found.map(row => ({id: Number(cell(row.value)), login: String(cell(row.login)).trim()})));
              scope.importPlan.skippedLabels = describeSkipped(scope.importPlan.skipped, scope.elements, key => $translate.instant(key));
            };
            if (logins.length > 0) {
              $personalizer.query("getScreenImportUsers", {logins}).then(plan);
            } else {
              plan([]);
            }
          };
          scope.chooseImportFile = () => importFile.click();
          // The popover of Import is the dropdown of the house: it opens with the choose button focused and closes with Esc,
          // a click outside, or when a file is chosen or dropped (the focus goes back to the Import link)
          const importDropdown = elem[0].querySelector(".scr-per-import-dropdown");
          const importToggle = importDropdown.querySelector("[data-toggle=dropdown]");
          const takeFile = file => {
            if (importDropdown.classList.contains("open")) {
              importToggle.click();
            }
            importToggle.focus();
            if (file) {
              const reader = new FileReader();
              reader.onload = () => scope.$apply(() => compare(String(reader.result)));
              reader.readAsText(file);
            }
          };
          $(importDropdown).on("shown.bs.dropdown", () => importDropdown.querySelector(".scr-per-choose").focus());
          $(importDropdown).on("hidden.bs.dropdown", () => importToggle.focus());
          importFile.addEventListener("change", () => {
            const file = importFile.files?.[0];
            importFile.value = "";
            takeFile(file);
          });
          const dropzone = importDropdown.querySelector(".scr-per-dropzone");
          dropzone.addEventListener("dragover", event => {
            event.preventDefault();
            dropzone.classList.add("scr-per-dropzone-over");
          });
          dropzone.addEventListener("dragleave", () => dropzone.classList.remove("scr-per-dropzone-over"));
          dropzone.addEventListener("drop", event => {
            event.preventDefault();
            dropzone.classList.remove("scr-per-dropzone-over");
            takeFile(event.dataTransfer?.files?.[0]);
          });
          scope.applyImport = () => saveChanges(scope.importPlan.changes, () => Promise.all([loadOverview(), loadHistory()])).then(worked => {
            if (worked) {
              scope.importPlan = null;
            }
          });
          scope.cancelImport = () => {
            scope.importPlan = null;
          };
          scope.deleteRules = rows => removeRules(deleteChanges(rows, scope.screen));
          scope.keepOne = (rows, keepId) => removeRules(keepOneChanges(rows, keepId, scope.screen));

          scope.setScopeType = type => {
            scope.applies = {type, id: null};
            update();
            subjectChanged();
          };
          scope.chooseProfile = item => {
            scope.applies = {type: "profile", id: item.value};
            update();
            subjectChanged();
          };
          const searchUsers = user => $personalizer.query("UsrSug", {suggest: user.text}).then(rows => {
            user.suggestions = rows.map(row => ({label: cell(row.label), value: Number(cell(row.value))}));
          });
          scope.searchUsers = () => {
            scope.applies = {type: "user", id: null};
            searchUsers(scope.user);
          };
          scope.chooseUser = item => {
            scope.user = {text: item.label, suggestions: []};
            scope.applies = {type: "user", id: item.value};
            update();
            subjectChanged();
          };
          // View as: the edited scope (linked) or a subject of its own, with the same profile list and user search
          scope.viewAsTypes = ["scope", ...scope.scopeTypes];
          scope.setViewAsType = type => {
            scope.viewAs = {type, id: null};
            subjectChanged();
          };
          scope.chooseViewAsProfile = item => {
            scope.viewAs = {type: "profile", id: item.value};
            subjectChanged();
          };
          scope.searchViewAsUsers = () => {
            scope.viewAs = {type: "user", id: null};
            searchUsers(scope.viewAsUser);
          };
          scope.chooseViewAsUser = item => {
            scope.viewAsUser = {text: item.label, suggestions: []};
            scope.viewAs = {type: "user", id: item.value};
            subjectChanged();
          };

          scope.select = id => {
            scope.selected = id;
            update();
            $personalizer.query("getScreenElementAttributes", {screen: scope.screen, component: id}).then(rows => {
              const {catalog, isColumn} = toCatalog(rows);
              if (isColumn) {
                columns = new Set(columns).add(id);
              }
              scope.catalog = catalog;
              catalog.forEach(item => {
                meta[`${id}|${item.attribute}`] = item;
              });
              update();
            });
          };
          scope.edit = (attribute, value) => {
            scope.pending = {...scope.pending, [`${scope.selected}|${attribute}`]: value};
            update();
          };
          scope.resetElement = () => {
            scope.pending = {...scope.pending, ...Object.fromEntries(scope.catalog.map(item => [`${scope.selected}|${item.attribute}`, null]))};
            update();
          };
          scope.closePanel = () => {
            scope.selected = null;
            update();
          };
          scope.discard = () => {
            scope.pending = {};
            update();
          };

          scope.changes = () => $personalizer.buildChanges(scope.rules, toStoragePending(scope.pending, columns), scope.applies, scope.screen);
          scope.invalid = () => $personalizer.countInvalid(scope.pending, meta);
          scope.canSave = () => scope.changes().length > 0 && scope.invalid() === 0 && (scope.applies.type === "everyone" || scope.applies.id != null);
          scope.status = () => scope.invalid() > 0 ? t("SCR_PER_INVALID", scope.invalid()) : scope.changes().length > 0 ? t("SCR_PER_CHANGES", scope.changes().length) : "";
          scope.save = () => $personalizer.save(scope.changes()).then(worked => {
            if (worked) {
              // The saved values come with the screen: draw it again
              scope.pending = {};
              loadRules(scope.screen);
              loadCanvas(scope.screen);
            }
          });

          // The canvas is inert: a click on an element selects it instead of acting
          const stopSwallowing = swallowInspectEvents(canvas, () => new Set(Object.keys(scope.elements)), id => scope.$apply(() => scope.select(id)));
          scope.$on("$destroy", () => {
            stopSwallowing();
            clearCanvas();
          });
        }
      };
    }
  ]);
