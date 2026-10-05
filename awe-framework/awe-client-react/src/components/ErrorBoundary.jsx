import React from "react";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {TestIds, testHook} from "../utilities/testIds";
import {translateLabel} from "../utilities";

/**
 * Panel shown instead of a tree that failed to render. The message and the component stack stay in the DOM, so a
 * user can report them and a test can read them from the page source
 * @param {object} props Properties
 * @param {string} props.scope Part of the application the boundary protects
 * @param {Error} props.error Error caught
 * @param {string} props.componentStack Component stack of the error
 * @param {function} props.onRetry Render the children again
 * @param {function} props.onReload Reload the page
 * @category Components
 */
function ErrorPanel({scope, error, componentStack, onRetry, onReload}) {
  const {t} = useTranslation();
  const message = (error && error.message) || String(error);
  const details = componentStack || (error && error.stack) || "";

  return (
    <div className="error-boundary" role="alert" {...testHook(TestIds.errorBoundary, {attributes: {"data-scope": scope}})}>
      <div className="error-boundary-title" {...testHook(TestIds.errorBoundaryTitle)}>
        {translateLabel("SCREEN_TEXT_ERROR_TITLE", t)}
      </div>
      <div className="error-boundary-message" {...testHook(TestIds.errorBoundaryMessage)}>{message}</div>
      <pre className="error-boundary-details" {...testHook(TestIds.errorBoundaryDetails)}>{details}</pre>
      <div className="error-boundary-actions">
        <Button type="button" label={translateLabel("BUTTON_RETRY", t)} icon="pi pi-refresh" onClick={onRetry}
                pt={{root: testHook(TestIds.errorBoundaryRetry)}}/>
        <Button type="button" label={translateLabel("BUTTON_RELOAD", t)} icon="pi pi-sync" outlined onClick={onReload}
                pt={{root: testHook(TestIds.errorBoundaryReload)}}/>
      </div>
    </div>
  );
}

ErrorPanel.propTypes = {
  scope: PropTypes.string,
  error: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  componentStack: PropTypes.string,
  onRetry: PropTypes.func,
  onReload: PropTypes.func
};

/**
 * Error boundary: catches the errors thrown while rendering its children, logs them and shows an error panel, so a
 * failing part of the application does not unmount the whole tree (a white screen)
 * @category Components
 */
class ErrorBoundary extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    // Part of the application the boundary protects ("app", "view"...), written in the log and in the panel
    scope: PropTypes.string,
    // The boundary renders its children again when this value changes (for example, the screen that is shown)
    resetKey: PropTypes.any,
    // Reload the page (default: the page is reloaded)
    onReload: PropTypes.func
  };

  static defaultProps = {
    scope: "app"
  };

  state = {error: null, componentStack: null};

  static getDerivedStateFromError(error) {
    return {error};
  }

  componentDidCatch(error, info) {
    const componentStack = (info && info.componentStack) || null;
    console.error(`[ErrorBoundary] Render error in scope "${this.props.scope}"`, error, componentStack);
    this.setState({componentStack});
  }

  componentDidUpdate(prevProps) {
    if (this.state.error && prevProps.resetKey !== this.props.resetKey) {
      this.retry();
    }
  }

  retry = () => this.setState({error: null, componentStack: null});

  reload = () => {
    if (this.props.onReload) {
      this.props.onReload();
    } else {
      window.location.reload();
    }
  };

  render() {
    const {error, componentStack} = this.state;
    if (error) {
      return <ErrorPanel scope={this.props.scope} error={error} componentStack={componentStack}
                         onRetry={this.retry} onReload={this.reload}/>;
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
