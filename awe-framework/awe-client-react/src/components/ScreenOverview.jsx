import React, {useMemo} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {translateLabel} from "../utilities";
import {describeOverview} from "../utilities/screenPersonalization";
import "./ScreenOverview.less";

/**
 * Overview of the personalization of a screen (#792): per element and option, what each subject sees, and the rules
 * that no longer work with one action each. A click on an element opens it in the editor
 * @returns {React.ReactElement} Overview
 */
export default function ScreenOverview({screen, rows, elements, onOpen, onDelete, onKeepOne}) {
  const {t} = useTranslation();
  const {title, warning, blocks, problems} = useMemo(
    () => describeOverview(screen, rows, elements, (key, ...args) => translateLabel(key, t, ...args), key => translateLabel(key, t)),
    [screen, rows, elements, t]);

  return (
    <div className="scr-per-overview">
      <h2 className="scr-per-overview-title">{title}</h2>
      {blocks.length === 0 && <p className="scr-per-empty">{t("SCR_PER_OVERVIEW_EMPTY")}</p>}
      {blocks.map(block => (
        <section className="scr-per-overview-block" key={block.key}>
          <Button type="button" link className="scr-per-overview-name" label={block.name} onClick={() => onOpen(block.component, null)}/>
          <ul className="scr-per-overview-rows">
            {block.entries.map(entry => (
              <li key={entry.ruleId}>
                <Button type="button" link className="scr-per-overview-subject" label={entry.text} onClick={() => onOpen(block.component, entry.entry)}/>
              </li>
            ))}
          </ul>
        </section>
      ))}
      {problems.length > 0 && (
        <section className="scr-per-overview-problems" role="region" aria-label={warning}>
          <h3 className="scr-per-overview-warning">{warning}</h3>
          <ul className="scr-per-overview-rows">
            {problems.map(problem => (
              <li key={problem.key} className="scr-per-overview-problem">
                <span>{problem.text}</span>
                {problem.kind === "duplicate" ? (
                  <span className="scr-per-overview-actions">
                    <span>{t("SCR_PER_KEEP_ONE")}</span>
                    {problem.keep.map(keep => <Button type="button" outlined size="small" key={keep.ruleId} label={keep.text}
                      onClick={() => onKeepOne(problem.rows, keep.ruleId)}/>)}
                  </span>
                ) : <Button type="button" outlined size="small" label={t("SCR_PER_DELETE")} onClick={() => onDelete(problem.rows)}/>}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

ScreenOverview.propTypes = {
  screen: PropTypes.string.isRequired,
  rows: PropTypes.array.isRequired,
  elements: PropTypes.object.isRequired,
  onOpen: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onKeepOne: PropTypes.func.isRequired
};
