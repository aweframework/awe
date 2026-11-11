import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import PropTypes from "prop-types";
import {useDispatch, useSelector, useStore} from "react-redux";
import {useTranslation} from "react-i18next";
import {AutoComplete} from "primereact/autocomplete";
import {Button} from "primereact/button";
import {Dropdown} from "primereact/dropdown";
import {SelectButton} from "primereact/selectbutton";
import {Sidebar} from "primereact/sidebar";
import {fetchAction, getIconCode, translateLabel} from "../utilities";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {addActionsTop} from "../redux/actions/actions";
import {clearComponents, updateAttributes} from "../redux/actions/components";
import {loadScreen} from "../redux/thunks/screen";
import ViewRegistry from "../redux/registry/ViewRegistry";
import {useView} from "../hooks/useViewRegistry";
import Templates from "../templates";
import ErrorBoundary from "../components/ErrorBoundary";
import {InspectContext, inspectWrapper} from "../components/ScreenInspect";
import GridColumnOverlays from "../components/GridColumnOverlays";
import ScreenOverview from "../components/ScreenOverview";
import ScreenHistory, {ScreenImportPreview} from "../components/ScreenHistory";
import ScreenImportPopover from "../components/ScreenImportPopover";
import ScreenElementPanel, {ScreenElementPanelHeader} from "../components/ScreenElementPanel";
import {setInspectWrapper} from "../utilities/structure";
import {getMergedComponent} from "../redux/reducers/components/stateHelpers";
import {SCREEN_SEARCH, buildChanges, buildExport, canvasSubject, chosenUserText, columnVisibility, countInvalid, isColumnCatalog, deleteChanges, importLogins, keepOneChanges, latestOnly, limitMatches, planImport, previewValue, revealHiddenColumns, subjectParams, toMaintainRequest, toStoragePending, visibleRules, withDefaults} from "../utilities/screenPersonalization";
import "./AweScreenPersonalizer.less";

// The screen under edit has a view of its own: the editor itself is the screen of the "report" view, inside the shell of "base"
const VIEW = "personalizer";
const cell = value => (value !== null && typeof value === "object" ? value.value : value);
const blankToNull = value => (value === "" || value === undefined ? null : value);
const toRules = rows => rows.map(row => ({
  id: Number(cell(row.IdeAweScrCnf)),
  user: blankToNull(cell(row.IdeOpe) ?? cell(row.IdeOpeVal)),
  profile: blankToNull(cell(row.IdePro) ?? cell(row.IdeProVal)),
  component: cell(row.Nam) ?? cell(row.NamVal),
  attribute: cell(row.Atr) ?? cell(row.AtrVal),
  value: cell(row.Val) ?? "",
  active: Number(cell(row.Act))
}));

const toOverviewRows = rows => rows.map(row => ({
  ruleId: Number(cell(row.ruleId)), scopeType: cell(row.scopeType),
  ...Object.fromEntries(["component", "attribute", "profileId", "profileName", "userId", "userName", "value", "problem", "duplicateGroup"].map(key => [key, cell(row[key]) ?? ""]))
}));

const toHistoryRows = rows => rows.map(row => ({
  ruleId: Number(cell(row.ruleId)),
  ...Object.fromEntries(["date", "operator", "action", "component", "attribute", "screen", "scopeType", "profileId", "profileName", "userId", "userName", "value", "valueBefore", "valueAfter", "undoable"]
    .map(key => [key, cell(row[key]) ?? ""]))
}));

// Text of a chosen file
const readText = file => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(String(reader.result));
  reader.onerror = reject;
  reader.readAsText(file);
});

// Download a text file named by the caller
const download = (name, text) => {
  const url = URL.createObjectURL(new Blob([text], {type: "application/json"}));
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
};

const MODES = ["edit", "overview", "history"];
const SCOPE_TYPES = ["everyone", "profile", "user"];

/** Profile list or user search of a subject, shared by the edited scope and View as */
function SubjectFields({subject, onChange, profiles, query, text, onText}) {
  const {t} = useTranslation();
  const [suggestions, setSuggestions] = useState([]);
  if (subject.type === "profile") {
    return <Dropdown value={subject.id} options={profiles} placeholder={t("PARAMETER_PROFILE")} aria-label={t("PARAMETER_PROFILE")}
      onChange={event => onChange({type: "profile", id: event.value})}/>;
  }
  return subject.type === "user" ? <AutoComplete value={text} suggestions={suggestions} field="label" placeholder={t("PARAMETER_USER")} aria-label={t("PARAMETER_USER")}
    completeMethod={event => query("UsrSug", {suggest: event.query})
      .then(rows => setSuggestions(rows.map(row => ({label: cell(row.label), value: Number(cell(row.value))}))))}
    onChange={event => {
      onText(event.value?.label ?? event.value);
      onChange({type: "user", id: event.value?.value ?? null});
    }}/> : null;
}

/** The elements of the screen that can be configured, by identifier; a column is previewed through the column model of its grid */
const toElements = rows => {
  const byPath = Object.fromEntries(rows.map(row => [cell(row.path), row]));
  const gridOf = row => {
    let parent = byPath[cell(row.parent)];
    while (parent && cell(parent.elementType) !== "Grid") parent = byPath[cell(parent.parent)];
    return parent ? cell(parent.id) : null;
  };
  return Object.fromEntries(rows.filter(row => String(cell(row.configurable)) === "true")
    .map(row => [cell(row.id), {id: cell(row.id), label: cell(row.label), elementType: cell(row.elementType), grid: gridOf(row)}]));
};

/** Show a pending edit on the canvas component (or give it back its value), remembering the value it had in originals */
const previewEdit = (key, {pending, elements, meta, components, originals, dispatch}) => {
  const [id, attribute] = key.split("|");
  const element = elements[id];
  const info = meta[key];
  const isColumn = element?.elementType === "Column";
  // The Visible of a column is previewed by dimming it (hidden columns are drawn anyway)
  if (!element || (isColumn && attribute === "visible")) return;
  const typed = typeof pending[key] === "string" && info ? previewValue(info.valueType, pending[key], info.allowedValues) : undefined;
  // The store keeps only the deltas of a component: read it merged with its registered base
  const ownerId = isColumn ? element.grid : id;
  const owner = getMergedComponent(ownerId, components);
  if (!owner) return;
  const model = owner.attributes?.columnModel;
  const current = isColumn ? model?.find(column => column.name === id)?.[attribute] : owner.attributes?.[attribute];
  if (!(key in originals)) originals[key] = current;
  const value = typed === undefined ? originals[key] : typed;
  if (typed === undefined) delete originals[key];
  if (value === current) return;
  dispatch(updateAttributes(owner.address ?? {view: VIEW, component: ownerId}, isColumn
    ? {columnModel: model.map(column => column.name === id ? {...column, [attribute]: value} : column)} : {[attribute]: value}));
};

// The scope the side panel edits: everyone, the name of the profile or the chosen user
const scopeLabelOf = (scope, profiles, userText, t) => {
  if (scope.type === "everyone") return t("SCR_PER_SCOPE_EVERYONE");
  if (scope.type === "profile") return profiles.find(profile => profile.value === scope.id)?.label ?? t("PARAMETER_PROFILE");
  return chosenUserText(scope, userText) || t("PARAMETER_USER");
};

// The toolbar status: the invalid edits first, then the changes waiting to be saved
const statusOf = (invalid, changes, t) => {
  if (invalid > 0) return translateLabel("SCR_PER_INVALID", t, invalid);
  return changes > 0 ? translateLabel("SCR_PER_CHANGES", t, changes) : "";
};

/**
 * Visual screen personalization editor (#792): the chosen screen is rendered with its real components in inspect mode,
 * a click selects an element and a non-modal side panel edits its attributes for the chosen scope
 * @category Widgets
 */
function AweScreenPersonalizer() {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const store = useStore();
  const {token} = useSelector(selectSettings);
  const view = useView(VIEW);
  const [screenSuggestions, setScreenSuggestions] = useState([]);
  // What the screen picker shows: the text being typed, or the chosen screen ({label, value})
  const [screenInput, setScreenInput] = useState(null);
  const [profiles, setProfiles] = useState([]);
  const [screen, setScreen] = useState(null);
  const [scope, setScope] = useState({type: "everyone", id: null});
  // Who the canvas shows the screen to: the edited scope ("scope") or a subject of its own
  const [viewAs, setViewAs] = useState({type: "scope", id: null});
  const [viewAsText, setViewAsText] = useState("");
  // Bumped when the canvas has to be loaded again (after a save) and when it was loaded
  const [reloads, setReloads] = useState(0);
  const [canvasVersion, setCanvasVersion] = useState(0);
  const lastSubject = useRef({type: "everyone", id: null});
  // Edit (the editor) or Overview (what each subject sees and the rules that no longer work)
  const [mode, setMode] = useState("edit");
  const [overview, setOverview] = useState([]);
  const [history, setHistory] = useState([]);
  // The import of a file, compared with the rules of the screen, waiting for Apply import
  const [importPlan, setImportPlan] = useState(null);
  const importPopover = useRef(null);
  const [rules, setRules] = useState([]);
  const [elements, setElements] = useState({});
  const [selected, setSelected] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [meta, setMeta] = useState({});
  const [pending, setPending] = useState({});
  const [userText, setUserText] = useState("");
  const [columns, setColumns] = useState(() => new Set());
  const [revealed, setRevealed] = useState(null);
  const [noOption, setNoOption] = useState(false);
  const [hiddenColumns, setHiddenColumns] = useState(() => new Set());
  const [grids, setGrids] = useState([]);
  const subject = canvasSubject(scope, viewAs, lastSubject.current);
  lastSubject.current = subject;
  const canvas = useRef(null);
  const nextScreenSearch = useRef(latestOnly());
  // Values the canvas components had before a pending edit was previewed on them ("component|attribute" -> value)
  const originals = useRef({});

  const query = useCallback(async (target, parameters) => {
    const response = await fetchAction("data", target, {max: 0, ...parameters}, token);
    return (Array.isArray(response) ? response.find(action => action.type === "fill")?.parameters?.datalist?.rows : null) || [];
  }, [token]);

  useEffect(() => {
    query("ProSel", {}).then(rows => setProfiles(rows.map(row => ({label: cell(row.label), value: Number(cell(row.value)), acronym: cell(row.Acr)}))));
  }, [query]);

  // The screens are searched on the server as the user types; an older answer never replaces a newer one
  const searchScreens = event => {
    const current = nextScreenSearch.current();
    return query("getScreenList", {suggest: event.query, max: SCREEN_SEARCH.limit}).then(rows => {
      if (current()) setScreenSuggestions(limitMatches(rows.map(row => ({label: cell(row.label), value: cell(row.value)})), SCREEN_SEARCH.limit));
    });
  };

  const loadRules = useCallback(id => query("ScrCnfLst", {CrtScr: id}).then(rows => setRules(toRules(rows))), [query]);

  const loadOverview = useCallback(id => query("getScreenConfigurationOverview", {screen: id}).then(rows => setOverview(toOverviewRows(rows))), [query]);
  // The overview is read again whenever it is opened, so it never shows what a save or a delete has changed
  useEffect(() => {
    if (screen && mode === "overview") loadOverview(screen);
  }, [screen, mode, loadOverview]);

  const loadHistory = useCallback(id => query("getScreenConfigurationHistory", {screen: id}).then(rows => setHistory(toHistoryRows(rows))), [query]);
  useEffect(() => {
    if (screen && mode === "history") loadHistory(screen);
  }, [screen, mode, loadHistory]);

  // The inspect hook of the structure builder lives while the editor is open
  useEffect(() => {
    setInspectWrapper(inspectWrapper);
    return () => {
      setInspectWrapper(null);
      dispatch(clearComponents(VIEW));
      ViewRegistry.clearView(VIEW);
    };
  }, [dispatch]);

  // Load the rules and the elements that can be configured of the chosen screen
  useEffect(() => {
    setSelected(null);
    setPending({});
    originals.current = {};
    if (screen) {
      loadRules(screen);
      query("getScreenStructure", {screen}).then(rows => setElements(toElements(rows)));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps -- reload only when another screen is chosen
  }, [screen]);

  // The canvas is the screen with its real components as the subject gets it: loaded again when the screen, the subject or the stored rules change
  const subjectKey = JSON.stringify(subject);
  useEffect(() => {
    setNoOption(false);
    originals.current = {};
    if (!screen) return;
    // A screen is loaded through the menu option that opens it
    query("getScreenOption", {screen}).then(rows => {
      const option = cell(rows[0]?.option);
      if (!option) {
        setNoOption(true);
        return;
      }
      const current = ViewRegistry.getCurrentView();
      void Promise.resolve(dispatch(loadScreen(VIEW, option, t, false, subjectParams(subject)))).finally(() => {
        ViewRegistry.setCurrentView(current);
        setCanvasVersion(version => version + 1);
      });
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps -- reload only when the screen, the subject or the stored rules change
  }, [screen, subjectKey, reloads]);

  // Hidden columns are drawn (narrow) in the canvas so they can be selected; the screen is shown once its grids are ready
  useEffect(() => {
    setRevealed(null);
  }, [screen]);
  useEffect(() => {
    if (!screen || ViewRegistry.get(VIEW)?.loading !== false) return;
    const names = [];
    Object.values(store.getState().components).forEach(component => {
      const model = component.attributes?.columnModel;
      if (component.address?.view === VIEW && model) {
        const {columnModel, hidden} = revealHiddenColumns(model);
        if (hidden.length > 0) {
          dispatch(updateAttributes(component.address, {columnModel}));
          names.push(...hidden);
        }
      }
    });
    setHiddenColumns(new Set(names));
    setRevealed(screen);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs when the screen or its load state changes
  }, [screen, view?.loading]);

  // The grids of the canvas get column overlays; they are looked up again when the canvas changes
  useEffect(() => {
    const node = canvas.current;
    if (!node || revealed !== screen) return undefined;
    // The overlays are children of the grids: keep the same list when it did not change, or the observer would loop
    const find = () => {
      const found = Object.values(elements).filter(element => element.elementType === "Grid")
        .map(element => ({id: element.id, node: node.querySelector(`[id="${element.id}"]`)})).filter(grid => grid.node);
      found.forEach(grid => grid.node.classList.add("scr-per-grid"));
      setGrids(previous => previous.length === found.length && previous.every((grid, index) => grid.node === found[index].node) ? previous : found);
    };
    find();
    const observer = new MutationObserver(find);
    observer.observe(node, {childList: true, subtree: true});
    return () => observer.disconnect();
  }, [revealed, screen, elements]);

  const select = useCallback(id => {
    setSelected(id);
    query("getScreenElementAttributes", {screen, component: id}).then(rows => {
      const raw = rows.map(row => ({
        attribute: cell(row.attribute), valueType: cell(row.valueType), value: cell(row.value) ?? "",
        allowedValues: (cell(row.allowedValues) || "").split(",").filter(Boolean)
      }));
      // A column shows Visible (the inverse of its hidden attribute)
      const list = columnVisibility(withDefaults(raw));
      if (isColumnCatalog(raw)) setColumns(previous => new Set(previous).add(id));
      setCatalog(list);
      setMeta(previous => ({...previous, ...Object.fromEntries(list.map(item => [`${id}|${item.attribute}`, item]))}));
    });
  }, [query, screen]);

  const edit = (attribute, value) => setPending(previous => ({...previous, [`${selected}|${attribute}`]: value}));
  const resetElement = () => setPending(previous => ({
    ...previous, ...Object.fromEntries(catalog.map(item => [`${selected}|${item.attribute}`, null]))
  }));

  // The canvas shows the pending edits at once; a discarded, reset or invalid edit gives the component back its value
  useEffect(() => {
    const components = store.getState().components;
    new Set([...Object.keys(originals.current), ...Object.keys(pending)]).forEach(key => previewEdit(key, {pending, elements, meta, components, originals: originals.current, dispatch}));
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs when the pending edits change
  }, [pending, canvasVersion]);
  const dimmedColumns = useMemo(() => {
    const dimmed = new Set(hiddenColumns);
    Object.entries(pending).forEach(([key, value]) => {
      const [id, attribute] = key.split("|");
      if (attribute !== "visible" || elements[id]?.elementType !== "Column") return;
      const typed = previewValue("BOOLEAN", value ?? "", []);
      if (typed === true) dimmed.delete(id);
      else if (typed === false) dimmed.add(id);
    });
    return dimmed;
  }, [hiddenColumns, pending, elements]);

  const changes = useMemo(() => buildChanges(rules, toStoragePending(pending, columns), scope, screen), [rules, pending, scope, screen, columns]);
  const editorRules = useMemo(() => visibleRules(rules, columns), [rules, columns]);
  const invalid = countInvalid(pending, meta);
  const canSave = changes.length > 0 && invalid === 0 && (scope.type === "everyone" || scope.id != null);

  // Send changes to the update maintain; the rules and the canvas are read again when the server accepts them
  const send = async list => {
    const response = await fetchAction("maintain", "updateScreenConfiguration", toMaintainRequest(list), token);
    const actions = Array.isArray(response) ? response : [];
    dispatch(addActionsTop(actions));
    if (actions.some(action => action.type === "message" && action.parameters?.type === "error")) return false;
    loadRules(screen);
    setReloads(count => count + 1);
    return true;
  };
  const save = async () => {
    // The saved values stay in the canvas
    if (await send(changes)) {
      originals.current = {};
      setPending({});
    }
  };
  const removeRules = list => send(list).then(worked => worked && loadOverview(screen));
  const undo = list => send(list).then(worked => worked && loadHistory(screen));
  // The rules of the screen go to a file, by profile acronym and user login so they can be imported elsewhere
  const exportRules = () => query("getScreenConfigurationExport", {screen}).then(rows =>
    download(`${screen}.json`, JSON.stringify(buildExport(screen, rows.map(row => Object.fromEntries(["component", "attribute", "value", "scopeType", "profile", "user"]
      .map(key => [key, cell(row[key]) ?? ""])))), null, 2)));
  // A chosen file is compared with the rules here; nothing is saved until Apply import
  const chooseImport = async file => {
    const content = await readText(file);
    const logins = importLogins(content);
    const found = logins.length > 0 ? await query("getScreenImportUsers", {logins}) : [];
    setImportPlan(planImport(content, screen, rules, elements, profiles.map(profile => ({id: profile.value, acronym: profile.acronym})),
      found.map(row => ({id: Number(cell(row.value)), login: String(cell(row.login)).trim()}))));
  };
  const applyImport = async () => {
    if (await send(importPlan.changes)) {
      setImportPlan(null);
      loadOverview(screen);
      loadHistory(screen);
    }
  };
  // A click in the overview opens the element in the editor, at the scope of the row clicked (everyone for the title)
  const open = (component, entry) => {
    setScope(entry ? {type: entry.scope, id: entry.id} : {type: "everyone", id: null});
    if (entry?.scope === "user") setUserText(entry.name);
    setMode("edit");
    select(component);
  };

  const scopeLabel = scopeLabelOf(scope, profiles, userText, t);
  const personalized = useMemo(() => new Set(rules.filter(rule => rule.active === 1).map(rule => rule.component)), [rules]);
  const inspect = useMemo(() => ({configurable: new Set(Object.keys(elements)), selected, personalized, onSelect: select}),
    [elements, selected, personalized, select]);
  const scopes = SCOPE_TYPES.map(value => ({value, label: t(`SCR_PER_SCOPE_${value.toUpperCase()}`)}));
  const viewAsOptions = [{value: "scope", label: t("SCR_PER_VIEW_AS_EDITED")}, ...scopes];
  const status = statusOf(invalid, changes.length, t);

  return (
    <div className={"expand expandible-vertical scr-per" + (selected && mode === "edit" ? " scr-per-with-panel" : "")}>
      <div className="scr-per-toolbar">
        <AutoComplete value={screenInput} suggestions={screenSuggestions} field="label" minLength={SCREEN_SEARCH.minChars} delay={SCREEN_SEARCH.delay}
          placeholder={t("PARAMETER_SCREEN")} aria-label={t("PARAMETER_SCREEN")} completeMethod={searchScreens}
          onChange={event => setScreenInput(event.value)}
          onSelect={event => {
            setScreenInput(event.value);
            setScreen(event.value.value);
          }}/>
        {screen && <SelectButton value={mode} options={MODES.map(value => ({value, label: t(`SCR_PER_MODE_${value.toUpperCase()}`)}))} allowEmpty={false}
          aria-label={t("SCR_PER_MODE")} onChange={event => event.value && setMode(event.value)}/>}
        {screen && <>
          <span className="scr-per-links">
            <Button type="button" link className="scr-per-link" icon={getIconCode("lucide:download", "p-button-icon p-c p-button-icon-left")} label={t("SCR_PER_EXPORT")} onClick={exportRules}/>
            <Button type="button" link className="scr-per-link" icon={getIconCode("lucide:upload", "p-button-icon p-c p-button-icon-left")} label={t("SCR_PER_IMPORT")} onClick={event => importPopover.current?.toggle(event)}/>
            <ScreenImportPopover ref={importPopover} onFile={chooseImport}/>
          </span>
          <span className="scr-per-break"/>
        </>}
        {mode === "edit" && <>
        <span className="scr-per-applies">{t("SCR_PER_VIEW_AS")}</span>
        <Dropdown value={viewAs.type} options={viewAsOptions} aria-label={t("SCR_PER_VIEW_AS")}
          onChange={event => setViewAs({type: event.value, id: null})}/>
        <SubjectFields subject={viewAs} onChange={setViewAs} profiles={profiles} query={query} text={viewAsText} onText={setViewAsText}/>
        <span className="scr-per-applies">{t("SCR_PER_APPLIES_TO")}</span>
        <SelectButton value={scope.type} options={scopes} allowEmpty={false} onChange={event => event.value && setScope({type: event.value, id: null})}/>
        <SubjectFields subject={scope} onChange={setScope} profiles={profiles} query={query} text={userText} onText={setUserText}/>
        <span className="scr-per-hint">{t("SCR_PER_PRECEDENCE")}</span>
        <span className="scr-per-actions">
          <span className="scr-per-status" role="status">{status}</span>
          <Button type="button" outlined disabled={changes.length === 0} label={t("SCR_PER_DISCARD")} onClick={() => setPending({})}/>
          <Button type="button" disabled={!canSave} label={t("BUTTON_SAVE_CHANGES")} onClick={save}/>
        </span>
        </>}
      </div>
      {importPlan && <ScreenImportPreview plan={importPlan} elements={elements} onApply={applyImport} onCancel={() => setImportPlan(null)}/>}
      {mode === "history" && screen && <div className="scr-per-canvas">
        <ScreenHistory screen={screen} rows={history} elements={elements} onUndo={undo}/>
      </div>}
      {mode === "overview" && screen && <div className="scr-per-canvas">
        <ScreenOverview screen={screenInput?.label ?? screen} rows={overview} elements={elements} onOpen={open} onDelete={list => removeRules(deleteChanges(list, screen))}
          onKeepOne={(list, keepId) => removeRules(keepOneChanges(list, keepId, screen))}/>
      </div>}
      <div ref={canvas} className="scr-per-canvas" hidden={mode !== "edit"}>
        {!screen ? <p className="scr-per-empty">{t("SCR_PER_PICK_SCREEN")}</p> : noOption ? <p className="scr-per-empty">{t("SCR_PER_NO_OPTION")}</p> : view?.loading || revealed !== screen ? null : (
          <InspectContext.Provider value={inspect}>
            <ErrorBoundary scope="view" resetKey={screen}>
              <div className="expand expandible-vertical" view-id={VIEW}>{Templates(view.structure)}</div>
            </ErrorBoundary>
            {grids.map(grid => <GridColumnOverlays key={grid.id} grid={grid.node} columns={inspect.configurable} hidden={dimmedColumns}
              selected={selected} onSelect={select}/>)}
          </InspectContext.Provider>
        )}
      </div>
      <Sidebar className="side-panel scr-per-side" position="right" modal={false} dismissable={false} visible={!!selected && mode === "edit"}
        onHide={() => setSelected(null)}
        header={selected && elements[selected] ? <ScreenElementPanelHeader element={elements[selected]} scopeLabel={scopeLabel}/> : null}>
        {selected && elements[selected] && <ScreenElementPanel element={elements[selected]} attributes={catalog} rules={editorRules} scope={scope}
          pending={pending} onChange={edit} onResetElement={resetElement} onClose={() => setSelected(null)}/>}
      </Sidebar>
    </div>
  );
}

AweScreenPersonalizer.propTypes = {id: PropTypes.string};

export default AweScreenPersonalizer;
