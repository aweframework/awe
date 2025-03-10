import React, {Component} from "react";

/**
 * Form container
 * @extends React.Component
 * @category Containers
 */
class FormContainer extends Component {

  /**
   * Create the container
   * @param {object} props Container properties
   */
  constructor(props) {
    super(props);

    this.handleSubmit = this.handleSubmit.bind(this);
  }

  /**
   * Handle form submit
   * @param {object} event Submit event
   */
  handleSubmit(event) {
    event.preventDefault();
    event.stopPropagation();
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    return <form className="expand expandible-vertical" onSubmit={this.handleSubmit}>{this.props.children}</form>;
  }
}

export default FormContainer;