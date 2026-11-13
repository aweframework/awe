package com.almis.awe.service.access;

import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu that a subject would see, from its cascade, as the real menu shows it: an option is shown when it is allowed and
 * every parent is shown. The options the menu never shows (invisible, opened from a screen) and the ones under them are
 * left out; section titles (separators) are shown but not counted as options.
 */
final class AccessMenuPreview {

  private AccessMenuPreview() {
  }

  /**
   * An option of the menu
   *
   * @param access   Access of the option
   * @param visible  The menu shows it
   * @param hiddenBy Hidden parent that hides this allowed option (null when it is visible or denied itself)
   */
  record Entry(OptionAccess access, boolean visible, Option hiddenBy) {

    /**
     * The option counts as an option of the menu (a section title does not)
     *
     * @return Counted
     */
    boolean counted() {
      return !access.option().isSeparator();
    }
  }

  /**
   * Options of the menu, parents before their children, without the invisible ones and the ones under them
   *
   * @param result Cascade of the subject
   * @return Entries
   */
  static List<Entry> of(Result result) {
    Map<Option, Entry> entries = new IdentityHashMap<>();
    List<Entry> list = new ArrayList<>();
    for (OptionAccess access : result.options()) {
      Option parent = access.parent();
      boolean underInvisible = parent != null && !entries.containsKey(parent);
      if (access.option().isInvisible() || underInvisible) {
        continue;
      }
      Entry parentEntry = parent == null ? null : entries.get(parent);
      boolean parentVisible = parentEntry == null || parentEntry.visible();
      Option hiddenBy = null;
      if (access.allowed() && !parentVisible) {
        hiddenBy = parentEntry.hiddenBy() != null ? parentEntry.hiddenBy() : parent;
      }
      Entry entry = new Entry(access, access.allowed() && parentVisible, hiddenBy);
      entries.put(access.option(), entry);
      list.add(entry);
    }
    return list;
  }
}
