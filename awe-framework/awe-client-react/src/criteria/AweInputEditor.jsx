import React, {useCallback} from "react";
import {Editor} from "primereact/editor";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {useTranslation} from "react-i18next";

function AweInputEditor(props) {

  const { id } = props;
  const {address, attributes, validationRules, value} = useText( id );
  const {placeholder, required, readonly, size, error} = attributes;
  const dispatch = useDispatch();
  const { t } = useTranslation();

  const onChange = useCallback( (e) => {
    if (value !== e.htmlValue) {
      dispatch(updateModelWithDependencies(address, {values: [{value: e.htmlValue, selected: true}]}));
    }
  }, [value]);

  const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error});

  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}
                       generateLabel={false} generateIcon={false} generateUnit={false} groupClass={"col-12"}>
      <Editor
      id={address.component}
      style={{height: "100%", minHeight:"4rem"}}
      value={value}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onTextChange={onChange}
      required={required}
      disabled={readonly}
    />
  </AweCriterion>;
}

export default AweInputEditor;
