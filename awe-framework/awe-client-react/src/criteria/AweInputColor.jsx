import React, {useCallback, useRef} from "react";
import {ColorPicker} from "primereact/colorpicker";
import {translateLabel} from "../utilities";
import {InputText} from "primereact/inputtext";
import {OverlayPanel} from "primereact/overlaypanel";
import {classNames} from "../utilities/components";

import "./AweInputColor.less";
import {fromColor, toColor} from "../utilities/color";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import {useTranslation} from "react-i18next";

function AweInputColor(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes = {}, validationRules = {}, value, onChange, onBlur, onSubmit } = useText(id);
  const overlayRef = useRef(null);

  const { placeholder, required, readonly, size } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size });

  const onPickerChange = useCallback((e) => {
    onChange({ target: { value: toColor(e.target.value) } });
  }, [onChange]);

  const onToggleOverlay = useCallback((e) => {
    if (!readonly) {
      overlayRef.current?.toggle(e);
    }
  }, [readonly]);

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <InputText
        id={address?.component}
        value={toColor(value)}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        onBlur={onBlur}
        onKeyPress={e => e.key === "Enter" && onSubmit()}
        required={required}
        disabled={readonly}
      />
      <span className="p-inputgroup-addon">
        <button
          type="button"
          className={classNames("colorpicker", { "no-color": !value, [`colorpicker-${size}`]: size })}
          style={{ backgroundColor: toColor(value) }}
          onClick={onToggleOverlay}
          onKeyDown={onToggleOverlay}
        />
        <OverlayPanel ref={overlayRef} dismissable appendTo={document.body} onHide={onBlur}>
          <ColorPicker
            inline
            value={fromColor(value)}
            disabled={readonly}
            onChange={onPickerChange}
            format="hex"
          />
        </OverlayPanel>
      </span>
    </AweCriterion>
  );
}

export default AweInputColor;
