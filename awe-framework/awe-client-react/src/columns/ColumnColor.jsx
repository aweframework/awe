import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {InputText} from "primereact/inputtext";
import {fromColor, toColor} from "../utilities/color";
import {OverlayPanel} from "primereact/overlaypanel";
import {ColorPicker} from "primereact/colorpicker";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import debounce from "lodash/debounce";

function ColumnColor(props) {
  const {placeholder, required, readonly, style, data, address} = props;
  const {t} = useTranslation();
  const dispatch = useDispatch();

  const [value, setValue] = useState(fromColor(data.value));
  const overlayRef = useRef(null);

  // Sync local state when external value changes
  useEffect(() => {
    setValue(fromColor(data.value));
  }, [data.value]);

  // Text input change: update local state; dispatch on blur for text edits
  const onChangeText = useCallback((e) => {
    const newVal = e?.target?.value ?? "";
    setValue(newVal);
  }, []);

  const dispatchIfChanged = useCallback((hexNoHash) => {
    const color = toColor(hexNoHash);
    if (data.value !== color) {
      dispatch(updateModelWithDependencies(address, {values: color}));
    }
  }, [data.value, address, dispatch]);

  const onBlur = useCallback(() => {
    dispatchIfChanged(value);
  }, [value, dispatchIfChanged]);

  const onShowColor = useCallback((e) => {
    if (!readonly && overlayRef.current) {
      overlayRef.current.toggle(e);
    }
  }, [readonly]);

  const onShowColorDebounced = useMemo(() => debounce((e) => onShowColor(e), 100), [onShowColor]);

  useEffect(() => () => { onShowColorDebounced.cancel && onShowColorDebounced.cancel(); }, [onShowColorDebounced]);

  const onShowColorKey = useCallback((e) => {
    if (e && e.persist) { e.persist(); }
    onShowColorDebounced(e);
  }, [onShowColorDebounced]);

  // ColorPicker change: update local state and dispatch immediately
  const onChangePicker = useCallback((e) => {
    const newVal = (e && (e.value ?? e.target?.value)) ?? "";
    setValue(newVal);
    dispatchIfChanged(newVal);
  }, [dispatchIfChanged]);

  const classes = classNames(style, data.style, "p-inputgroup", "column-editor", {"p-invalid": data?.error});

  return <div className={classes}>
    <InputText
      value={toColor(value)}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onChange={onChangeText}
      onBlur={onBlur}
      required={required}
      disabled={readonly}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    />
    <span className="p-inputgroup-addon">
        <button className={"colorpicker " + (value ? "" : "no-color")}
                style={{backgroundColor: toColor(value)}} onClick={onShowColor} onKeyDown={onShowColorKey}/>
        <OverlayPanel ref={overlayRef} dismissable appendTo={document.body} onHide={onBlur}>
          <ColorPicker
            inline
            value={fromColor(value)}
            disabled={readonly}
            onChange={onChangePicker}
            format="hex"
          />
        </OverlayPanel>
      </span>
  </div>;
}

ColumnColor.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  placeholder: PropTypes.string,
  style: PropTypes.string
};

export default ColumnColor;
