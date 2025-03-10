import React, {Component} from "react";
import {Route, Switch} from "react-router-dom";
import SubViewContainer from "../containers/SubViewContainer";

const routes = [
  {path: "/screen/public/:screenId/:subScreenId"},
  {path: "/screen/private/:screenId/:subScreenId"}
]

export default class SubView extends Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <Switch>
        {routes.map((route, i) => (
          <RouteWithSubRoutes key={"SubView-" + i} {...route}/>
        ))}
      </Switch>
    );
  }
}

function RouteWithSubRoutes(route) {
  return (
    <Route exact
           path={route.path}
           render={props => (
             // pass the sub-routes down to keep nesting
             <SubViewContainer {...props}/>
           )}
    />
  );
}