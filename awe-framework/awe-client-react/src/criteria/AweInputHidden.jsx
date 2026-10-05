import React from "react";
import PropTypes from "prop-types";
import useComponent from "../hooks/useComponent";
import {useComponentState} from "../hooks/useComponentState";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Hidden criterion: it shows nothing, but it keeps its value in a hidden input (as the AngularJS client does), so the
 * value of the criterion can be read from the page
 */
function AweInputHidden(props) {
  const { id } = props;
  const { address } = useComponent(id);
  const { model = { values: [] } } = useComponentState(id);
  const value = (model.values || []).filter(item => item.selected).map(item => item.value).join(", ");

  if (!address) {
    return null;
  }

  return (
    <div className="hidden" criterion-id={address.component}>
      <input type="hidden" value={value} readOnly {...testHook(TestIds.criterionInput)} />
    </div>
  );
}

AweInputHidden.propTypes = {
  id: PropTypes.string,
};

export default AweInputHidden;
