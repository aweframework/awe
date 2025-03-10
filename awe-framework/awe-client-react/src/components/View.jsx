import React, {Component} from "react";
import {Redirect, Route, Switch} from "react-router-dom";
import {ConnectedRouter} from "connected-react-router";
import {history} from "../redux/store";
import ViewContainer from "../containers/ViewContainer";

const routes = [
  {path: "/screen/public/:screenId"},
  {path: "/screen/private/:screenId"},
  {path: "/screen/:screenId"},
  {path: ""},
];

/**
 * View component
 * @extends React.Component
 * @category Components
 */
export class View extends Component {

  render() {
    return (
      <ConnectedRouter history={history}>
        <Switch>
          {routes.map((route, i) => (
            <RouteWithSubRoutes key={"View" + i} {...route} reloadCurrentScreen={this.props.reloadCurrentScreen} initialURL={this.props.initialURL} props={this.props}/>
          ))}
        </Switch>
      </ConnectedRouter>);
  }
}

function RouteWithSubRoutes(route) {
  return (
    <Route path={route.path} render={props => {
      if (route.reloadCurrentScreen) {
        route.props.updateSettings({reloadCurrentScreen: false});
        return <Redirect to={route.initialURL} />
      } else {
        return <ViewContainer {...props}/>
      }
    }}/>
  );
}
