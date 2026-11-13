import React from "react";
import PropTypes from "prop-types";
import { useComponentState } from "../hooks/useComponentState";
import { useDeferredLoad } from "../hooks/useDeferredLoad";
import Empty from "./Empty";

/**
 * Page of a tab. Only the shown page is rendered, so a deferred page asks for its data when it is rendered.
 * @param {object} props Node of the page
 * @returns {JSX.Element} Content of the page
 */
function AweTabContainer(props) {
  const { address, attributes } = useComponentState(props.id) || {};
  useDeferredLoad(address, attributes, true);

  return <Empty {...props} />;
}

AweTabContainer.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweTabContainer;
