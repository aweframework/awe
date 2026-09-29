import React from "react";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import ColumnTextType from "./ColumnTextType";
import PropTypes from "prop-types";

function ColumnPassword(props) {
  const { address, ...rest } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  return (
    <ColumnTextType
      {...rest}
      address={address}
      t={t}
      inputType="password"
      updateModelWithDependencies={updateModelWithDependencies}
    />
  );
}

ColumnPassword.propTypes = {
  address: PropTypes.object,
};

export default ColumnPassword;