import React, {useMemo} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {translateLabel} from "../utilities";
import {describeHistory, describeSkipped} from "../utilities/screenPersonalization";
import "./ScreenHistory.less";

/**
 * History of the personalization of a screen (#792): who changed what and when, newest first, with Undo on the latest change of each rule
 * @returns {React.ReactElement} History
 */
export default function ScreenHistory({screen, rows, elements, onUndo}) {
  const {t} = useTranslation();
  const items = useMemo(
    () => describeHistory(screen, rows, elements, (key, ...args) => translateLabel(key, t, ...args), key => translateLabel(key, t)),
    [screen, rows, elements, t]);

  return (
    <div className="scr-per-history">
      {items.length === 0 && <p className="scr-per-empty">{t("SCR_PER_HISTORY_EMPTY")}</p>}
      <ul className="scr-per-history-rows">
        {items.map(item => (
          <li key={item.key} className="scr-per-history-row">
            <span className="scr-per-history-when">{item.when}</span>
            <span className="scr-per-history-operator">{item.operator}</span>
            <span className="scr-per-history-what">
              <span className="scr-per-history-name">{item.name}</span>
              <span className="scr-per-history-change">{item.change}</span>
            </span>
            {item.undoable
              ? <Button type="button" outlined size="small" label={t("SCR_PER_UNDO")} onClick={() => onUndo(item.undo)}/>
              : <span className="scr-per-history-later">{t(item.unrecorded ? "SCR_PER_HISTORY_UNRECORDED" : "SCR_PER_CHANGED_LATER")}</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}

ScreenHistory.propTypes = {
  screen: PropTypes.string.isRequired,
  rows: PropTypes.array.isRequired,
  elements: PropTypes.object.isRequired,
  onUndo: PropTypes.func.isRequired
};

/**
 * Preview of an import (#792): what it would add and change and the rules left out; nothing is saved until Apply import
 * @returns {React.ReactElement} Preview
 */
export function ScreenImportPreview({plan, elements, onApply, onCancel}) {
  const {t} = useTranslation();
  const labels = useMemo(() => describeSkipped(plan.skipped, elements, key => translateLabel(key, t)), [plan.skipped, elements, t]);
  const msg = (key, ...args) => translateLabel(key, t, ...args);
  const {error} = plan;
  return (
    <section className="scr-per-import" role="region" aria-label={t("SCR_PER_IMPORT")}>
      {error ? <p className="scr-per-import-title">{error.code === "OTHER_SCREEN" ? msg("SCR_PER_IMPORT_OTHER_SCREEN", error.screen) : t("SCR_PER_IMPORT_INVALID_FILE")}</p>
        : <h3 className="scr-per-import-title">{msg("SCR_PER_IMPORT_PREVIEW", plan.toAdd, plan.toChange)}</h3>}
      {plan.skipped.length > 0 && <>
        <span>{msg("SCR_PER_IMPORT_SKIPPED", plan.skipped.length)}</span>
        <ul className="scr-per-import-skipped">
          {plan.skipped.map((skip, index) => <li key={index}>{msg("SCR_PER_IMPORT_SKIPPED_ITEM", labels[index].component, labels[index].attribute, t(`SCR_PER_IMPORT_REASON_${skip.reason.toUpperCase()}`), skip.name)}</li>)}
        </ul>
      </>}
      <span className="scr-per-import-actions">
        {!error && <Button type="button" disabled={plan.changes.length === 0} label={t("SCR_PER_IMPORT_APPLY")} onClick={onApply}/>}
        <Button type="button" outlined label={t("SCR_PER_IMPORT_CANCEL")} onClick={onCancel}/>
      </span>
    </section>
  );
}

ScreenImportPreview.propTypes = {
  plan: PropTypes.object.isRequired,
  elements: PropTypes.object.isRequired,
  onApply: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired
};
