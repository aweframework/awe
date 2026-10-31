import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {useDispatch, useStore} from "react-redux";
import {OverlayPanel} from "primereact/overlaypanel";
import {Dialog} from "primereact/dialog";
import {Dropdown} from "primereact/dropdown";
import {InputText} from "primereact/inputtext";
import PropTypes from "prop-types";
import {getIconNode, translateLabel} from "../utilities";
import {
  getSavedModelValues,
  getSearchValues,
  isSavedSearchInEffect,
  parseSearchValues
} from "../utilities/advancedSearch";
import {TestAttributes, TestIds, testHook} from "../utilities/testIds";
import {dropdownPassThrough, selectValueTemplate} from "../utilities/testPassThrough";
import {useMultipleComponents} from "../hooks/useComponentState";
import {updateMultipleModelsWithDependencies} from "../redux/thunks/components";
import {
  deleteWindowSearch,
  loadSavedSearchProfiles,
  loadWindowSavedSearches,
  renameWindowSearch,
  saveWindowSearch
} from "../redux/thunks/windowSavedSearches";
import {getAllComponents} from "../redux/selectors/componentSelectors";
import {useRestoreDefaults, useShowResults, useWindowScreen} from "./AweWindowAdvanced";
import "./AweWindowSavedSearches.less";

// Widths of the dialogs, relative to the text size (as the side panel of the advanced search)
const SAVE_DIALOG_STYLE = {width: "30em", maxWidth: "95vw"};
const MANAGE_DIALOG_STYLE = {width: "39em", maxWidth: "95vw"};
// Profile of a search that is only for the user who saves it (the select shows no option for an empty value)
const ONLY_ME = "@only-me";
const SAVE = "save";
const MANAGE = "manage";

/**
 * Check whether a saved search is an own search of the user (not one published for its profile)
 * @param {object} search Saved search
 * @return {boolean} Own search
 */
const isOwn = (search) => search.own === true || String(search.own) === "1";

/**
 * Label of the profile a search is published for: the name of the profile when the user may publish for it, its
 * acronym otherwise
 */
const getProfileLabel = (search, profiles) =>
  (profiles || []).find(profile => profile.value === search.profile)?.label || search.profile;

/**
 * Menu of the saved searches of a window: its own searches and the ones shared with its profile, then Save current
 * search and Manage searches. The arrows move between its options and Escape gives the focus back to the button.
 */
function SavedSearchesMenu({id, menuRef, listRef, open, loading, searches, profiles, current, onApply, onSave, onManage,
                             onClose}) {
  const {t} = useTranslation();
  const own = (searches || []).filter(isOwn);
  const shared = (searches || []).filter(search => !isOwn(search));

  // The first option takes the focus once the searches are loaded
  useEffect(() => {
    if (open && !loading) {
      listRef.current?.querySelector("[role='menuitem']")?.focus();
    }
  }, [open, loading, listRef]);

  const onKeyDown = (event) => {
    const options = [...listRef.current.querySelectorAll("[role='menuitem']")];
    const index = options.indexOf(document.activeElement);
    const focus = (next) => {
      event.preventDefault();
      options[(next + options.length) % options.length]?.focus();
    };
    switch (event.key) {
      case "ArrowDown":
        focus(index + 1);
        break;
      case "ArrowUp":
        focus(index < 0 ? options.length - 1 : index - 1);
        break;
      case "Home":
        focus(0);
        break;
      case "End":
        focus(options.length - 1);
        break;
      case "Escape":
        event.preventDefault();
        event.stopPropagation();
        onClose(true);
        break;
      case "Tab":
        onClose(false);
        break;
      default:
    }
  };

  const renderSearch = (search) => {
    const applied = String(search.id) === String(current);
    return <button key={search.id} type="button" role="menuitem" className="window-saved-searches-option"
                   aria-current={applied ? "true" : undefined} onClick={() => onApply(search)}
                   {...testHook(TestIds.windowSavedSearchItem, {owner: id, type: isOwn(search) ? "own" : "shared",
                     attributes: {[TestAttributes.savedSearchId]: String(search.id)}})}>
      {getIconNode(isOwn(search) ? "lucide:bookmark" : "lucide:users",
        {className: "window-saved-searches-option-icon", "aria-hidden": "true"})}
      <span className="window-saved-searches-option-text">{search.name}</span>
      {!isOwn(search) && <span className="window-saved-search-profile">{getProfileLabel(search, profiles)}</span>}
    </button>;
  };

  const title = t("SCREEN_TEXT_SAVED_SEARCHES");
  return <OverlayPanel ref={menuRef} dismissable closeOnEscape={false} className="window-saved-searches-overlay"
                       onHide={() => onClose(false)}>
    <div ref={listRef} id={`${id}-saved-searches-menu`} role="menu" tabIndex={-1} aria-label={title}
         aria-busy={loading || undefined}
         className="window-saved-searches-menu" onKeyDown={onKeyDown}
         {...testHook(TestIds.windowSavedSearchesMenu, {owner: id})}>
      {!loading && searches?.length === 0 &&
        <p className="window-saved-searches-empty" role="none">{t("SCREEN_TEXT_NO_SAVED_SEARCHES")}</p>}
      {own.length > 0 && <div role="group" aria-label={t("SCREEN_TEXT_MY_SEARCHES")}>
        <p className="window-saved-searches-head" role="presentation">{t("SCREEN_TEXT_MY_SEARCHES")}</p>
        {own.map(renderSearch)}
      </div>}
      {shared.length > 0 && <div role="group" aria-label={t("SCREEN_TEXT_SHARED_SEARCHES")}>
        {own.length > 0 && <div className="window-saved-searches-separator" role="separator"/>}
        <p className="window-saved-searches-head" role="presentation">{t("SCREEN_TEXT_SHARED_SEARCHES")}</p>
        {shared.map(renderSearch)}
      </div>}
      <div className="window-saved-searches-separator" role="separator"/>
      <button type="button" role="menuitem" className="window-saved-searches-option" onClick={onSave}
              {...testHook(TestIds.windowSavedSearchSave, {owner: id})}>
        {getIconNode("lucide:save", {className: "window-saved-searches-option-icon", "aria-hidden": "true"})}
        <span className="window-saved-searches-option-text">{t("SCREEN_TEXT_SAVE_CURRENT_SEARCH")}</span>
      </button>
      <button type="button" role="menuitem" className="window-saved-searches-option" onClick={onManage}
              {...testHook(TestIds.windowSavedSearchManage, {owner: id})}>
        {getIconNode("lucide:settings", {className: "window-saved-searches-option-icon", "aria-hidden": "true"})}
        <span className="window-saved-searches-option-text">{t("SCREEN_TEXT_MANAGE_SEARCHES")}</span>
      </button>
    </div>
  </OverlayPanel>;
}

SavedSearchesMenu.propTypes = {
  id: PropTypes.string,
  menuRef: PropTypes.object,
  listRef: PropTypes.object,
  open: PropTypes.bool,
  loading: PropTypes.bool,
  searches: PropTypes.array,
  profiles: PropTypes.array,
  current: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  onApply: PropTypes.func,
  onSave: PropTypes.func,
  onManage: PropTypes.func,
  onClose: PropTypes.func
};

/**
 * Header of a dialog of the saved searches: its title, its subtitle and the close button
 */
function DialogHeader({titleId, title, subtitle, onClose}) {
  const {t} = useTranslation();
  return <div className="window-saved-search-dialog-header">
    <div className="window-saved-search-dialog-titles">
      <p id={titleId} className="window-saved-search-dialog-title">{title}</p>
      {subtitle && <p className="window-saved-search-dialog-subtitle">{subtitle}</p>}
    </div>
    <button type="button" className="p-button p-component p-button-text p-button-icon-only p-button-sm"
            title={t("BUTTON_CLOSE")} aria-label={t("BUTTON_CLOSE")} onClick={onClose}>
      {getIconNode("lucide:x", {tag: "span", "aria-hidden": "true"})}
    </button>
  </div>;
}

DialogHeader.propTypes = {
  titleId: PropTypes.string,
  title: PropTypes.string,
  subtitle: PropTypes.string,
  onClose: PropTypes.func
};

/**
 * Save dialog: the name of the search (required) and, for a user who may publish, the profile to publish it for
 */
function SaveSearchDialog({id, visible, name: initialName, profiles, onSave, onHide}) {
  const {t} = useTranslation();
  const [name, setName] = useState(initialName || "");
  const [profile, setProfile] = useState(ONLY_ME);
  const [invalid, setInvalid] = useState(false);
  const [saving, setSaving] = useState(false);
  const titleId = `${id}-saved-search-dialog-title`;
  const nameId = `${id}-saved-search-name`;
  const profileId = `${id}-saved-search-profile`;
  const errorId = `${nameId}-error`;
  const options = useMemo(() => [{value: ONLY_ME, label: t("SCREEN_TEXT_ONLY_ME")}, ...(profiles || [])],
    [profiles, t]);

  const save = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      setInvalid(true);
      return;
    }
    setSaving(true);
    try {
      await onSave({name: trimmed, profile: profile === ONLY_ME ? undefined : profile});
    } finally {
      setSaving(false);
    }
  };

  const footer = <div className="window-saved-search-dialog-footer">
    <button type="button" className="p-button p-component p-button-outlined p-button-sm" onClick={onHide}
            {...testHook(TestIds.windowSavedSearchCancel, {owner: id})}>{t("BUTTON_CANCEL")}</button>
    <button type="button" className="p-button p-component p-button-sm" onClick={save} disabled={saving}
            {...testHook(TestIds.windowSavedSearchAccept, {owner: id, loading: saving})}>
      {getIconNode(saving ? "lucide:loader-circle" : "lucide:save", {tag: "span", "aria-hidden": "true",
        className: `p-button-icon p-button-icon-left${saving ? " awe-icon-spin" : ""}`})}
      <span className="p-button-label">{t("SCREEN_TEXT_SAVE_SEARCH")}</span>
    </button>
  </div>;

  return <Dialog visible={visible} onHide={onHide} showCloseIcon={false} focusOnShow={false} footer={footer}
                 className="window-saved-search-dialog" style={SAVE_DIALOG_STYLE}
                 header={<DialogHeader titleId={titleId} title={t("SCREEN_TEXT_SAVE_SEARCH")}
                                       subtitle={t("SCREEN_TEXT_SAVE_SEARCH_SUBTITLE")} onClose={onHide}/>}
                 pt={{root: {...testHook(TestIds.windowSavedSearchDialog, {owner: id}), "aria-labelledby": titleId}}}>
    <div className="window-saved-search-fields">
      <div className="window-saved-search-field">
        <label htmlFor={nameId}>{t("SCREEN_TEXT_SEARCH_NAME")} <span className="window-saved-search-required"
                                                                      aria-hidden="true">*</span></label>
        <InputText id={nameId} value={name} autoFocus required aria-required="true" aria-invalid={invalid}
                   aria-describedby={invalid ? errorId : undefined} maxLength={100}
                   className={invalid ? "p-invalid" : undefined}
                   onChange={event => {
                     setName(event.target.value);
                     setInvalid(false);
                   }}
                   onKeyDown={event => {
                     if (event.key === "Enter") {
                       event.preventDefault();
                       // save reports its own errors
                       void save();
                     }
                   }}
                   {...testHook(TestIds.windowSavedSearchName, {owner: id})}/>
        {invalid && <small id={errorId} className="p-error" role="alert">{t("VALIDATOR_MESSAGE_REQUIRED")}</small>}
      </div>
      {(profiles || []).length > 0 && <div className="window-saved-search-field">
        <label id={`${profileId}-label`} htmlFor={profileId}>{t("SCREEN_TEXT_PUBLISH_FOR_PROFILE")}</label>
        {/* The trigger carries the hook and the window owns the list, as the select2 of the AngularJS client */}
        <Dropdown inputId={profileId} value={profile} options={options} onChange={event => setProfile(event.value)}
                  valueTemplate={selectValueTemplate}
                  pt={{...dropdownPassThrough(id), trigger: testHook(TestIds.windowSavedSearchProfile, {owner: id})}}
                  aria-describedby={`${profileId}-help`}/>
        <small id={`${profileId}-help`} className="window-saved-search-help">
          {t("SCREEN_TEXT_PUBLISH_FOR_PROFILE_HELP")}
        </small>
      </div>}
    </div>
  </Dialog>;
}

SaveSearchDialog.propTypes = {
  id: PropTypes.string,
  visible: PropTypes.bool,
  name: PropTypes.string,
  profiles: PropTypes.array,
  onSave: PropTypes.func,
  onHide: PropTypes.func
};

/**
 * Confirmation of the deletion of a saved search, as the confirm dialog of the messages
 */
function DeleteConfirm({id, search, onAccept, onCancel}) {
  const {t} = useTranslation();
  const footer = <div className="confirm-footer">
    <button type="button" className="p-button p-component p-button-secondary" onClick={onCancel}
            {...testHook(TestIds.confirmCancel, {owner: id})}>
      {getIconNode("lucide:x", {tag: "span", className: "p-button-icon p-button-icon-left", "aria-hidden": "true"})}
      <span className="p-button-label">{t("BUTTON_CANCEL")}</span>
    </button>
    <button type="button" className="p-button p-component" onClick={onAccept}
            {...testHook(TestIds.confirmAccept, {owner: id})}>
      {getIconNode("lucide:check", {tag: "span", className: "p-button-icon p-button-icon-left", "aria-hidden": "true"})}
      <span className="p-button-label">{t("BUTTON_ACCEPT")}</span>
    </button>
  </div>;
  return <Dialog visible={Boolean(search)} header={t("SCREEN_TEXT_DELETE_SEARCH")} footer={footer} focusOnShow={false}
                 modal closable={false} onHide={onCancel} className="awe-confirm-dialog"
                 pt={{root: {...testHook(TestIds.confirmDialog, {owner: id}), role: "alertdialog"}}}>
    {getIconNode("pi-exclamation-triangle", {className: "confirm-icon"})}
    <span><b>{search?.name}</b>: {t("SCREEN_TEXT_DELETE_SEARCH_MESSAGE")}</span>
  </Dialog>;
}

DeleteConfirm.propTypes = {
  id: PropTypes.string,
  search: PropTypes.object,
  onAccept: PropTypes.func,
  onCancel: PropTypes.func
};

/**
 * Manage dialog: a row per search with whom it is for; the own searches (and the shared ones for a user who may
 * publish) can be renamed and deleted (after a confirmation)
 */
function ManageSearchesDialog({id, label, visible, searches, profiles, onRename, onDelete, onHide}) {
  const {t} = useTranslation();
  const [renaming, setRenaming] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const canPublish = (profiles || []).length > 0;
  const titleId = `${id}-saved-search-manage-title`;

  const rename = async () => {
    const name = renaming.name.trim();
    if (name && await onRename(renaming.search, name)) {
      setRenaming(null);
    }
  };

  const remove = async () => {
    const search = deleting;
    setDeleting(null);
    await onDelete(search);
  };

  const renderName = (search) => {
    if (String(renaming?.search.id) !== String(search.id)) {
      return <span className="window-saved-search-name">{search.name}</span>;
    }
    return <span className="window-saved-search-rename">
      <InputText value={renaming.name} autoFocus maxLength={100} aria-label={t("SCREEN_TEXT_SEARCH_NAME")}
                 aria-invalid={!renaming.name.trim()} className="p-inputtext-sm"
                 onChange={event => setRenaming({...renaming, name: event.target.value})}
                 onKeyDown={event => {
                   if (event.key === "Enter") {
                     event.preventDefault();
                     // rename reports its own errors
                     void rename();
                   } else if (event.key === "Escape") {
                     event.preventDefault();
                     event.stopPropagation();
                     setRenaming(null);
                   }
                 }}
                 {...testHook(TestIds.windowSavedSearchRenameInput, {owner: id})}/>
      <button type="button" className="p-button p-component p-button-text p-button-icon-only p-button-sm"
              title={t("BUTTON_ACCEPT")} aria-label={t("BUTTON_ACCEPT")} onClick={rename}
              {...testHook(TestIds.windowSavedSearchRenameAccept, {owner: id})}>
        {getIconNode("lucide:check", {tag: "span", "aria-hidden": "true"})}
      </button>
      <button type="button" className="p-button p-component p-button-text p-button-icon-only p-button-sm"
              title={t("BUTTON_CANCEL")} aria-label={t("BUTTON_CANCEL")} onClick={() => setRenaming(null)}
              {...testHook(TestIds.windowSavedSearchRenameCancel, {owner: id})}>
        {getIconNode("lucide:x", {tag: "span", "aria-hidden": "true"})}
      </button>
    </span>;
  };

  const renderActions = (search) => {
    if (!isOwn(search) && !canPublish) {
      return null;
    }
    const renameLabel = `${t("SCREEN_TEXT_RENAME_SEARCH")} ${search.name}`;
    const deleteLabel = `${t("SCREEN_TEXT_DELETE_SEARCH")} ${search.name}`;
    return <span className="window-saved-search-actions">
      <button type="button" className="p-button p-component p-button-text p-button-icon-only p-button-sm"
              title={t("SCREEN_TEXT_RENAME_SEARCH")} aria-label={renameLabel}
              onClick={() => setRenaming({search, name: search.name})}
              {...testHook(TestIds.windowSavedSearchRename, {owner: id})}>
        {getIconNode("lucide:pencil", {tag: "span", "aria-hidden": "true"})}
      </button>
      <button type="button" className="p-button p-component p-button-text p-button-icon-only p-button-sm"
              title={t("SCREEN_TEXT_DELETE_SEARCH")} aria-label={deleteLabel} onClick={() => setDeleting(search)}
              {...testHook(TestIds.windowSavedSearchDelete, {owner: id})}>
        {getIconNode("lucide:trash-2", {tag: "span", "aria-hidden": "true"})}
      </button>
    </span>;
  };

  const footer = <div className="window-saved-search-dialog-footer">
    <button type="button" className="p-button p-component p-button-outlined p-button-sm" onClick={onHide}
            {...testHook(TestIds.windowSavedSearchManageClose, {owner: id})}>{t("BUTTON_CLOSE")}</button>
  </div>;

  return <>
    <Dialog visible={visible} onHide={onHide} showCloseIcon={false} footer={footer} focusOnShow
            className="window-saved-search-dialog window-saved-search-manage" style={MANAGE_DIALOG_STYLE}
            header={<DialogHeader titleId={titleId} title={t("SCREEN_TEXT_MANAGE_SEARCHES_TITLE")}
                                  subtitle={translateLabel(label, t)}
                                  onClose={onHide}/>}
            pt={{root: {...testHook(TestIds.windowSavedSearchManageDialog, {owner: id}), "aria-labelledby": titleId}}}>
      {(searches || []).length === 0 ?
        <p className="window-saved-searches-empty">{t("SCREEN_TEXT_NO_SAVED_SEARCHES")}</p> :
        <table className="window-saved-search-table">
          <thead>
          <tr>
            <th scope="col">{t("SCREEN_TEXT_SEARCH_NAME")}</th>
            <th scope="col">{t("SCREEN_TEXT_SEARCH_FOR")}</th>
            <th scope="col"><span className="p-hidden-accessible">{t("SCREEN_TEXT_SEARCH_ACTIONS")}</span></th>
          </tr>
          </thead>
          <tbody>
          {searches.map(search => <tr key={search.id}
                                      {...testHook(TestIds.windowSavedSearchRow, {owner: id, type: isOwn(search) ? "own" : "shared",
                                        attributes: {[TestAttributes.savedSearchId]: String(search.id)}})}>
            <td>{renderName(search)}</td>
            <td>{isOwn(search) ? t("SCREEN_TEXT_SEARCH_OWNER_ME") :
              <span className="window-saved-search-profile">{getProfileLabel(search, profiles)}</span>}</td>
            <td className="window-saved-search-actions-cell">{renderActions(search)}</td>
          </tr>)}
          </tbody>
        </table>}
    </Dialog>
    <DeleteConfirm id={id} search={deleting} onAccept={remove} onCancel={() => setDeleting(null)}/>
  </>;
}

ManageSearchesDialog.propTypes = {
  id: PropTypes.string,
  label: PropTypes.string,
  visible: PropTypes.bool,
  searches: PropTypes.array,
  profiles: PropTypes.array,
  onRename: PropTypes.func,
  onDelete: PropTypes.func,
  onHide: PropTypes.func
};

/**
 * Saved searches of a window (#790, saved-searches="true"), in the row of its advanced block right after the Advanced
 * filters toggle: a text button that opens the menu of the searches and shows the name of the search last applied
 * while the criteria keep its values. Applying a search brings every search criterion to its default, sets the values of
 * the search and shows the results. The searches are loaded each time the menu opens; the profiles the user may publish
 * for, once.
 */
export function SavedSearches({id, label, advancedCriteria, searchCriteria}) {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const store = useStore();
  const buttonRef = useRef(null);
  const menuRef = useRef(null);
  const listRef = useRef(null);
  const profilesRequested = useRef(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [searches, setSearches] = useState(null);
  const [profiles, setProfiles] = useState([]);
  const [applied, setApplied] = useState(null);
  const [dialog, setDialog] = useState(null);
  // A new save dialog each time it opens, so it starts with the name of the search in effect
  const [saveKey, setSaveKey] = useState(0);
  const getScreen = useWindowScreen(searchCriteria.length ? searchCriteria : advancedCriteria);
  const restore = useRestoreDefaults();
  const showResults = useShowResults(id, advancedCriteria, searchCriteria);
  const components = useMultipleComponents(searchCriteria);
  const errorMessage = useMemo(() => ({
    title: t("SCREEN_TEXT_SAVED_SEARCH_ERROR_TITLE"),
    message: t("SCREEN_TEXT_SAVED_SEARCH_ERROR_MESSAGE")
  }), [t]);

  const inEffect = applied && isSavedSearchInEffect(components, searchCriteria, applied.values) ? applied : null;

  // Load the searches (and the profiles the first time); the search in effect follows its new name, or is forgotten
  // when it is gone
  const reload = useCallback(async () => {
    const screen = getScreen();
    if (!screen) {
      setSearches([]);
      return null;
    }
    const address = {view: screen.view, component: id};
    if (!profilesRequested.current) {
      profilesRequested.current = true;
      dispatch(loadSavedSearchProfiles(address, errorMessage)).then(rows => {
        profilesRequested.current = rows !== null;
        setProfiles(rows || []);
      });
    }
    const rows = await dispatch(loadWindowSavedSearches(address, screen.screenId, errorMessage));
    if (rows !== null) {
      setSearches(rows);
      setApplied(search => {
        const row = search && rows.find(item => String(item.id) === String(search.id));
        return row ? {...search, name: row.name} : null;
      });
    }
    return rows;
  }, [dispatch, getScreen, id, errorMessage]);

  const closeMenu = useCallback((focus) => {
    menuRef.current?.hide();
    setMenuOpen(false);
    if (focus) {
      buttonRef.current?.focus();
    }
  }, []);

  const toggleMenu = async (event) => {
    if (menuOpen) {
      closeMenu(false);
      return;
    }
    menuRef.current?.show(event, buttonRef.current);
    setMenuOpen(true);
    setLoading(true);
    await reload();
    setLoading(false);
  };

  const apply = (search) => {
    closeMenu(true);
    const values = parseSearchValues(search.values);
    restore(searchCriteria);
    const restored = getAllComponents(store.getState());
    const updates = searchCriteria
      .filter(criterion => criterion in values && restored[criterion]?.address)
      .map(criterion => ({
        address: restored[criterion].address,
        data: {values: getSavedModelValues(restored[criterion], values[criterion])}
      }));
    if (updates.length) {
      dispatch(updateMultipleModelsWithDependencies(updates));
    }
    setApplied({id: search.id, name: search.name, values});
    showResults();
  };

  const openDialog = (type) => {
    closeMenu(false);
    if (type === SAVE) {
      setSaveKey(key => key + 1);
    }
    setDialog(type);
  };

  const closeDialog = () => {
    setDialog(null);
    buttonRef.current?.focus();
  };

  const save = async ({name, profile}) => {
    const screen = getScreen();
    if (!screen) {
      return false;
    }
    const values = getSearchValues(getAllComponents(store.getState()), searchCriteria);
    const worked = await dispatch(saveWindowSearch({view: screen.view, component: id}, screen.screenId,
      {name, profile, values: JSON.stringify(values)}, errorMessage));
    if (worked) {
      closeDialog();
      const rows = await reload();
      // The saved search is the one in effect
      const saved = (rows || []).find(row => row.name === name && (profile ? row.profile === profile : isOwn(row)));
      if (saved) {
        setApplied({id: saved.id, name: saved.name, values});
      }
    }
    return worked;
  };

  const rename = async (search, name) => {
    const screen = getScreen();
    const worked = screen && await dispatch(renameWindowSearch({view: screen.view, component: id}, search.id, name,
      errorMessage));
    if (worked) {
      await reload();
    }
    return Boolean(worked);
  };

  const remove = async (search) => {
    const screen = getScreen();
    const worked = screen && await dispatch(deleteWindowSearch({view: screen.view, component: id}, search.id,
      errorMessage));
    if (worked) {
      await reload();
    }
    return Boolean(worked);
  };

  const text = inEffect ? inEffect.name : t("SCREEN_TEXT_SAVED_SEARCHES");
  return <>
    <button ref={buttonRef} type="button" onClick={toggleMenu}
            className="window-saved-searches p-button p-component p-button-text p-button-sm"
            aria-haspopup="menu" aria-expanded={menuOpen}
            aria-controls={menuOpen ? `${id}-saved-searches-menu` : undefined}
            title={inEffect ? `${t("SCREEN_TEXT_SAVED_SEARCHES")}: ${inEffect.name}` : undefined}
            {...testHook(TestIds.windowSavedSearches, {owner: id})}>
      {getIconNode("lucide:bookmark", {className: "window-saved-searches-icon", "aria-hidden": "true"})}
      <span className="window-saved-searches-text">{text}</span>
      {getIconNode("lucide:chevron-down", {className: "window-saved-searches-chevron", "aria-hidden": "true"})}
    </button>
    <SavedSearchesMenu id={id} menuRef={menuRef} listRef={listRef} open={menuOpen} loading={loading}
                       searches={searches} profiles={profiles} current={inEffect?.id} onApply={apply}
                       onSave={() => openDialog(SAVE)} onManage={() => openDialog(MANAGE)} onClose={closeMenu}/>
    {saveKey > 0 && <SaveSearchDialog key={saveKey} id={id} visible={dialog === SAVE} name={inEffect?.name}
                                      profiles={profiles} onSave={save} onHide={closeDialog}/>}
    <ManageSearchesDialog id={id} label={label} visible={dialog === MANAGE} searches={searches} profiles={profiles}
                          onRename={rename} onDelete={remove} onHide={closeDialog}/>
  </>;
}

SavedSearches.propTypes = {
  id: PropTypes.string,
  label: PropTypes.string,
  advancedCriteria: PropTypes.array,
  searchCriteria: PropTypes.array
};
