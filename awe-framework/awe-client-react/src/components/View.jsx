import React, {useEffect} from "react";
import ViewContainer from "../containers/ViewContainer";
import { Routes, Route } from 'react-router';
import SubViewContainer from "../containers/SubViewContainer";
import {useDispatch, useSelector} from "react-redux";
import {updateSettings} from "../redux/actions/settings";
import { navigationActions } from "../redux/actions/navigation";

const routes = [
  {path: "/screen/public/:screenId", subroutes: [{path: ":subScreenId"}]},
  {path: "/screen/private/:screenId", subroutes: [{path: ":subScreenId"}]},
  {path: "/screen/:screenId"},
  {path: ""},
];

function View(props) {
  const dispatch = useDispatch();
  const {settings} = useSelector((state) => ({settings: state.settings}));
  const {initialURL, reloadCurrentScreen} = settings;

  useEffect(() => {
    if (reloadCurrentScreen) {
      dispatch(updateSettings({reloadCurrentScreen: false}));
      dispatch(navigationActions.navigateTo(initialURL));
    }
  }, [reloadCurrentScreen]);

  return (
      <Routes>
        {routes.map((route, i) => (
          <Route key={"View" + i} path={route.path} element={<ViewContainer {...props} />}>
          {(route.subroutes || []).map((subRoute, j) => (
            <Route key={"SubView" + j} path={subRoute.path} element={<SubViewContainer {...props} />}/>
          ))}
          </Route>
        ))}
      </Routes>
  );
}

export default View;
