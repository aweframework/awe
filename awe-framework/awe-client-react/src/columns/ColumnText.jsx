import React from "react";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import ColumnTextType from "./ColumnTextType";

function ColumnText(props) {
  const { address, t: tProp, updateModelWithDependencies: updProp, ...rest } = props;
  const { t: tHook } = useTranslation();
  const dispatch = useDispatch();
  const updateThunkDispatcher = (addr, payload) => dispatch(updateThunk(addr, payload));

  const t = tProp || tHook;
  const updateModelWithDependencies = updProp || updateThunkDispatcher;

  return (
    <ColumnTextType
      {...rest}
      address={address}
      t={t}
      inputType="text"
      updateModelWithDependencies={updateModelWithDependencies}
    />
  );
}

export default ColumnText;