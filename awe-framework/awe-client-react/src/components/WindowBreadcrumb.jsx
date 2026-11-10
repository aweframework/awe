import React, {cloneElement, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {BreadCrumb} from "primereact/breadcrumb";
import {deleteStack, addActionsTop} from "../redux/actions/actions";
import {navigationActions} from "../redux/actions/navigation";
import MenuRegistry from "../redux/registry/MenuRegistry";
import {useMenuBreadcrumbs, useMenuHasExpander, useMenuLocation, useMenuOptions, useMenuOrigin} from "../hooks/useMenuRegistry";
import {useView} from "../hooks/useViewRegistry";
import {TestIds, testHook} from "../utilities/testIds";
import {getIconCode, translateLabel} from "../utilities";
import {collapseLevels, getBreadcrumbLevels} from "../utilities/breadcrumbs";
import {findOptionPath, stampMenuOrigin} from "../utilities/menuLaunch";

/**
 * Breadcrumb of a window: the home icon and the levels down to the open page. Every level but the page is a link: a
 * menu option launches as the menu does, a menu group opens the menu at it, a screen the user came from goes back to it
 * @category Components
 */
function WindowBreadcrumb() {
  const {t} = useTranslation();
  const dispatch = useDispatch();
  const initialURL = useSelector(state => state.settings?.initialURL);
  const breadcrumbs = useMenuBreadcrumbs();
  const origin = useMenuOrigin();
  const report = useView("report");
  const location = useMenuLocation();
  const menuOptions = useMenuOptions();
  const canExpandMenu = useMenuHasExpander();

  const levels = getBreadcrumbLevels(origin, breadcrumbs, report, location);
  // The hidden levels are expanded for these levels only: another screen collapses them again
  const levelsKey = levels.map(level => level.name ?? level.path ?? level.label).join("/");
  const [expandedKey, setExpandedKey] = useState(null);
  const expanded = expandedKey === levelsKey;

  const goHome = () => {
    MenuRegistry.setOrigin([]);
    dispatch(navigationActions.navigateTo(initialURL));
  };
  const goBack = (index, path) => {
    MenuRegistry.setOrigin(origin.slice(0, index));
    dispatch(navigationActions.navigateTo(path));
  };
  const launch = (actions) => {
    MenuRegistry.setOrigin([]);
    dispatch(deleteStack());
    dispatch(addActionsTop(stampMenuOrigin(actions)));
  };

  // Command of a level that is not the page, and the attribute that identifies it for tests
  const levelBehavior = (level, index) => {
    if (level.path) {
      return {command: () => goBack(index, level.path), attributes: {path: level.path}};
    } else if (level.group) {
      // A group opens the side menu at it: where no menu can (the horizontal menu) or it does not know the group, it is text
      if (!canExpandMenu || !findOptionPath(menuOptions, level.name)) {
        return {};
      }
      return {command: () => MenuRegistry.requestExpand(level.name), attributes: {name: level.name}};
    } else if (level.actions?.length) {
      return {command: () => launch(level.actions), attributes: {name: level.name}};
    }
    return {};
  };

  const toItem = (level, shown) => {
    const label = translateLabel(level.label, t);
    if (level.collapsed) {
      return {
        template: () => <button type="button" className="p-menuitem-link window-breadcrumb-ellipsis"
          aria-label={t("SCREEN_TEXT_BREADCRUMB_EXPAND")} aria-expanded={expanded}
          onClick={() => setExpandedKey(expanded ? null : levelsKey)} {...testHook(TestIds.breadcrumbEllipsis)}>…</button>
      };
    }
    const {command, attributes} = levelBehavior(level, levels.indexOf(level));
    if (shown[shown.length - 1] === level || !command) {
      const current = shown[shown.length - 1] === level;
      return {
        template: () => <span className="p-menuitem-text" {...(current ? {"aria-current": "page"} : {})}
          {...testHook(TestIds.breadcrumbLevel, {attributes, active: current})}>{label}</span>
      };
    }
    return {
      label, command,
      template: (item, options) => cloneElement(options.element, testHook(TestIds.breadcrumbLevel, {attributes}))
    };
  };

  const shown = collapseLevels(levels, expanded);
  const home = {
    icon: getIconCode("pi-home", "p-menuitem-icon"),
    command: goHome,
    template: (item, options) => cloneElement(options.element, {
      "aria-label": t("BUTTON_HOME"), ...testHook(TestIds.breadcrumbHome)
    })
  };
  return <BreadCrumb model={shown.map(level => toItem(level, shown))} home={home}
    pt={{root: {...testHook(TestIds.breadcrumb), "aria-label": t("SCREEN_TEXT_BREADCRUMB")}}}/>;
}

export default WindowBreadcrumb;
