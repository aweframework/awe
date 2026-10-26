import {getActionAddress} from "../../utilities";
import {acceptAction} from "../actions/actions";
import {getContextMenuAnchor, hasContextMenu, showContextMenuUnder} from "../../utilities/contextMenuRegistry";
import type {AppThunk} from "../types";
import {selectRows} from "./grid";
import type {ClientAction} from "./types";

/**
 * Action show-context-menu: open the context menu of a component under the button that launched the action. A button of
 * a grid row opens the menu of its grid, after selecting its row as a right click on the row does; any other
 * button opens its own menu. When there is nothing to open, the action only finishes.
 * @param {Action} action Action received
 */
export const showContextMenuAction = (action: ClientAction): AppThunk => {
  return (dispatch, getState) => {
    try {
      const address = getActionAddress(action);
      const anchor = getContextMenuAnchor(address);
      if (anchor && hasContextMenu(address)) {
        const {view, component, row} = address;
        if (row !== undefined && row !== null) {
          selectRows(dispatch, getState, {view, component}, [row]);
        }
        showContextMenuUnder(address, anchor);
      }
    } finally {
      dispatch(acceptAction(action));
    }
  };
};
