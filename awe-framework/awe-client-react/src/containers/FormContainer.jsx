import React, {useCallback} from "react";

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

export default FormContainer;