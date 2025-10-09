import React from "react";
import { InputTextarea } from "primereact/inputtextarea";
import { classNames } from "../utilities/components";
import { translateLabel } from "../utilities";
import { useTranslation } from "react-i18next";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";

function AweInputTextarea(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, attributes, validationRules, value, onChange, onBlur } = useText(id);

  const { placeholder, required, readonly, size, areaRows, error } = attributes;
  const classes = classNames("w-full", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}
                  groupClass="" generateIcon={false} generateUnit={false}>
      <InputTextarea
        id={address?.component}
        value={value}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        onBlur={onBlur}
        required={required}
        disabled={readonly}
        rows={areaRows}
      />
    </AweCriterion>
  );
}

export default AweInputTextarea;
