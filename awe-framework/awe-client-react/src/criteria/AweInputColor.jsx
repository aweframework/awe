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
import PropTypes from "prop-types";
import useEnterKey from "../hooks/useEnterKey";

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

  // Enter is read on key down, the event the ancestors (grid containers, global shortcuts) listen to
  const onKeyDown = useEnterKey(onSubmit);

  const onToggleOverlay = useCallback((e) => {
    if (!readonly) {
      overlayRef.current?.toggle(e);
    }
  }, [readonly]);

  // The panel is placed when it starts to open, before the inline picker has its width: placed again once shown, so it
  // is shifted to the left of the swatch when it does not fit on the right instead of widening the page
  const onShowOverlay = useCallback(() => overlayRef.current?.align(), []);

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <InputText
        id={address?.component}
        value={toColor(value)}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        onBlur={onBlur}
        onKeyDown={onKeyDown}
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
        <OverlayPanel ref={overlayRef} dismissable appendTo={document.body} onShow={onShowOverlay} onHide={onBlur}>
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

AweInputColor.propTypes = {
  id: PropTypes.string,
};

export default AweInputColor;
