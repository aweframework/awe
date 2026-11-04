package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessSubjects.Subject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.service.access.AccessData.DESCRIPTION;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.ICON;
import static com.almis.awe.service.access.AccessData.ICON_TONE;
import static com.almis.awe.service.access.AccessData.INFO;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.SEPARATOR;
import static com.almis.awe.service.access.AccessData.TITLE;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.VISIBLE;
import static com.almis.awe.service.access.AccessData.asServiceData;

/**
 * View as of the access window (access-admin): the menu that a user or a profile would see in a module, from its
 * cascade with {@link AccessMenuPreview}, and the options it would not see with the reason. No session is opened as the
 * subject and nothing is stored.
 */
public class AccessMenuPreviewService {

  private final AccessSubjects subjects;
  private final AccessTexts texts;

  /**
   * Autowired constructor
   *
   * @param subjects Subjects of the access window
   * @param texts    Texts of the access window
   */
  public AccessMenuPreviewService(AccessSubjects subjects, AccessTexts texts) {
    this.subjects = subjects;
    this.texts = texts;
  }

  /**
   * Retrieve the menu that the subject would see in a module, as the real menu shows it: the allowed options whose
   * parents are all shown, in the order of the menu, with the section titles (not the divider lines). Nothing is stored
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @return Tree rows: preview-id, preview-parent, preview-is-leaf and preview-name (empty without subject)
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuPreview(String subjectType, Integer user, Integer profile, String module)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent()) {
      // A separator without label is a divider line of the menu, not a row of the preview
      List<AccessMenuPreview.Entry> visible = getEntries(subject.get(), module).stream()
        .filter(AccessMenuPreview.Entry::visible)
        .filter(entry -> !entry.access().option().isSeparator() || entry.access().option().getLabel() != null)
        .toList();
      Set<Option> parents = Collections.newSetFromMap(new IdentityHashMap<>());
      visible.forEach(entry -> parents.add(entry.access().parent()));
      for (AccessMenuPreview.Entry entry : visible) {
        Option option = entry.access().option();
        Map<String, CellData> row = new HashMap<>();
        row.put("preview-id", new CellData(option.getName()));
        row.put("preview-parent", new CellData(entry.access().parent() == null ? null
          : entry.access().parent().getName()));
        row.put("preview-is-leaf", new CellData(parents.contains(option) ? FALSE : TRUE));
        row.put("preview-name", texts.getOptionNameCell(option));
        rows.add(row);
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the number of options of the menu that the subject would see in a module, as a card: how many it sees of
   * all of them and, when an allowed option is hidden by a denied parent, an example of it
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @return One row (visible-count, total-count, hidden-count, title, description, icon and iconTone), none without
   * subject
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuPreviewCounts(String subjectType, Integer user, Integer profile, String module)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent()) {
      List<AccessMenuPreview.Entry> options = getEntries(subject.get(), module).stream()
        .filter(AccessMenuPreview.Entry::counted).toList();
      int visible = (int) options.stream().filter(AccessMenuPreview.Entry::visible).count();
      Map<String, CellData> row = new HashMap<>();
      row.put("visible-count", new CellData(visible));
      row.put("total-count", new CellData(options.size()));
      row.put("hidden-count", new CellData(options.size() - visible));
      row.put(TITLE, new CellData(texts.getLocale(PREFIX + "PREVIEW_COUNT", String.valueOf(visible),
        String.valueOf(options.size()))));
      row.put(DESCRIPTION, new CellData(options.stream().filter(entry -> entry.hiddenBy() != null).findFirst()
        .map(entry -> texts.getLocale(PREFIX + "PREVIEW_PARENT_HIDDEN", texts.getOptionLabel(entry.access().option()),
          texts.getOptionLabel(entry.hiddenBy())))
        .orElse(texts.getLocale(PREFIX + "PREVIEW_HIDDEN_TEXT"))));
      row.put(ICON, new CellData("lucide:list-tree"));
      row.put(ICON_TONE, new CellData(INFO));
      rows.add(row);
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the options of the menu that the subject would not see in a module, with the reason: the layer that denies
   * it, or the denied parent that hides it
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @return Rows: hidden-option, hidden-option-key and hidden-reason, in the order of the menu (empty without subject)
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuPreviewHidden(String subjectType, Integer user, Integer profile, String module)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent()) {
      for (AccessMenuPreview.Entry entry : getEntries(subject.get(), module)) {
        if (entry.counted() && !entry.visible()) {
          Map<String, CellData> row = new HashMap<>();
          row.put("hidden-option", new CellData(texts.getOptionLabel(entry.access().option())));
          row.put("hidden-option-key", new CellData(entry.access().option().getName()));
          row.put("hidden-reason", new CellData(entry.hiddenBy() != null
            ? texts.getLocale(PREFIX + "PREVIEW_HIDDEN_BY_PARENT", texts.getOptionLabel(entry.hiddenBy()))
            : texts.getReason(entry.access(), subject.get())));
          rows.add(row);
        }
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the banner of the menu preview, as a card: who it is seen as, in which module, and that it is only a
   * preview (no session is opened as the subject, none of its screens)
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param module      Selected module (optional)
   * @return One row (visible, title, description, icon and iconTone), not visible without subject
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuPreviewBanner(String subjectType, Integer user, Integer profile, String module)
    throws AWException {
    Map<String, CellData> row = new HashMap<>();
    row.put(VISIBLE, new CellData(FALSE));
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isPresent()) {
      String name = AccessTexts.getSubjectName(subject.get());
      List<String> title = new ArrayList<>();
      title.add(texts.getLocale(PREFIX + "PREVIEW_TITLE", name));
      if (subject.get().layer() == Layer.USER && subject.get().profileAcronym() != null) {
        title.add(texts.getLocale(PREFIX + "SUBJECT_PROFILE", subject.get().profileAcronym()));
      }
      title.add(StringUtils.isBlank(module) ? texts.getLocale(PREFIX + "PREVIEW_ALL_MODULES")
        : texts.getLocale(PREFIX + "PREVIEW_MODULE", module));
      row.put(VISIBLE, new CellData(TRUE));
      row.put(TITLE, new CellData(String.join(SEPARATOR, title)));
      row.put(DESCRIPTION, new CellData(texts.getLocale(PREFIX + "PREVIEW_TEXT", name)));
      row.put(ICON, new CellData("lucide:eye"));
      row.put(ICON_TONE, new CellData(INFO));
    }
    return asServiceData(List.of(row));
  }

  private List<AccessMenuPreview.Entry> getEntries(Subject subject, String module) throws AWException {
    return AccessMenuPreview.of(subjects.evaluate(subject, module));
  }
}
