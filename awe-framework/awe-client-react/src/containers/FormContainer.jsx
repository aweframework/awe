import React, {useCallback} from "react";
import PropTypes from "prop-types";

/**
 * Form container
 * @category Containers
 */
function FormContainer(props) {
  /**
   * Handle form submit
   * @param {object} event Submit event
   */
  const handleSubmit = useCallback((event) => {
    event.preventDefault();
    event.stopPropagation();
  }, []);

  return <form className="expand expandible-vertical" onSubmit={handleSubmit}>{props.children}</form>;
}

FormContainer.propTypes = {
  children: PropTypes.node
};

export default FormContainer;
