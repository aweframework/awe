import React, {forwardRef, useImperativeHandle, useRef, useState} from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {OverlayPanel} from "primereact/overlaypanel";
import {getIconCode} from "../utilities";
import "./ScreenImportPopover.less";

/**
 * Popover of the Import link of the screen personalizer (#792): a drop zone for the file of rules. Choosing or dropping
 * a file closes it and hands the file to onFile; Esc and a click outside close it and the focus goes back to the link.
 * @returns {React.ReactElement} Popover, with toggle(event) to open it from the link
 */
const ScreenImportPopover = forwardRef(function ScreenImportPopover({onFile}, ref) {
  const {t} = useTranslation();
  const panel = useRef(null);
  const body = useRef(null);
  const input = useRef(null);
  const link = useRef(null);
  const [over, setOver] = useState(false);

  useImperativeHandle(ref, () => ({
    toggle: event => {
      link.current = event.currentTarget;
      panel.current.toggle(event);
    }
  }));

  const take = file => {
    panel.current.hide();
    if (file) onFile(file);
  };

  return (
    <OverlayPanel ref={panel} dismissable className="scr-per-import-popover"
      onShow={() => body.current?.querySelector(".scr-per-choose")?.focus()} onHide={() => link.current?.focus()}>
      <div ref={body} role="dialog" aria-label={t("SCR_PER_IMPORT_TITLE")} className="scr-per-import-body">
        <h3 className="scr-per-import-popover-title">{t("SCR_PER_IMPORT_TITLE")}</h3>
        <div className={"scr-per-dropzone" + (over ? " scr-per-dropzone-over" : "")}
          onDragOver={event => {
            event.preventDefault();
            setOver(true);
          }}
          onDragLeave={() => setOver(false)}
          onDrop={event => {
            event.preventDefault();
            setOver(false);
            take(event.dataTransfer?.files?.[0]);
          }}>
          <i className={getIconCode("lucide:upload", "scr-per-dropzone-icon")} aria-hidden="true"/>
          <span>
            <span>{t("SCR_PER_IMPORT_DROP")}</span>{" "}
            <Button type="button" link className="scr-per-link scr-per-choose" label={t("SCR_PER_IMPORT_CHOOSE")}
              onClick={() => input.current?.click()}/>
          </span>
          <input ref={input} type="file" accept=".json,application/json" hidden aria-label={t("SCR_PER_IMPORT_FILE")}
            onChange={event => {
              const file = event.target.files?.[0];
              event.target.value = "";
              take(file);
            }}/>
        </div>
        <p className="scr-per-import-hint">{t("SCR_PER_IMPORT_HINT")}</p>
      </div>
    </OverlayPanel>
  );
});

ScreenImportPopover.propTypes = {
  onFile: PropTypes.func.isRequired
};

export default ScreenImportPopover;
