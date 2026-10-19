package com.almis.awe.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Column layout of a grid chosen by a user: the order of the columns and the columns the user hid.
 *
 * <p>Only the columns the user can manage take part (the ones the XML definition does not hide). Columns never leave
 * their group header: groups keep the positions they have in the XML definition and only the columns of a group
 * are reordered among the positions of that group.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GridColumnLayout implements Serializable {

  private static final long serialVersionUID = 1L;

  // Column identifiers in display order
  private List<String> order = new ArrayList<>();

  // Identifiers of the columns hidden by the user
  private List<String> hidden = new ArrayList<>();

  /**
   * Merge a stored layout with the columns of the current XML definition.
   *
   * <ul>
   *   <li>Identifiers that are not manageable columns anymore are ignored</li>
   *   <li>New columns are visible and keep their XML position among the columns of their group</li>
   *   <li>A column never leaves its group, whatever the stored layout says</li>
   *   <li>A layout that would hide every column hides none</li>
   * </ul>
   *
   * @param stored  Stored layout (can be null)
   * @param columns Manageable columns in XML order, with the key of their group header (null if they have none)
   * @return Merged layout, always complete
   */
  public static GridColumnLayout merge(GridColumnLayout stored, Map<String, String> columns) {
    List<String> storedOrder = stored == null || stored.getOrder() == null ? List.of() : stored.getOrder();
    List<String> storedHidden = stored == null || stored.getHidden() == null ? List.of() : stored.getHidden();

    // Stored position of each known column (the first occurrence wins)
    Map<String, Integer> storedPosition = new HashMap<>();
    for (String id : storedOrder) {
      if (columns.containsKey(id)) {
        storedPosition.putIfAbsent(id, storedPosition.size());
      }
    }

    // Slots: the XML positions of each group (null group: columns without header)
    Map<String, List<Integer>> slotsByGroup = new LinkedHashMap<>();
    Map<String, List<String>> membersByGroup = new LinkedHashMap<>();
    List<String> xmlOrder = new ArrayList<>(columns.keySet());
    for (int position = 0; position < xmlOrder.size(); position++) {
      String id = xmlOrder.get(position);
      String group = columns.get(id);
      slotsByGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(position);
      membersByGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(id);
    }

    String[] merged = new String[xmlOrder.size()];
    for (Map.Entry<String, List<String>> entry : membersByGroup.entrySet()) {
      List<String> ordered = orderMembers(entry.getValue(), storedPosition);
      List<Integer> slots = slotsByGroup.get(entry.getKey());
      for (int index = 0; index < slots.size(); index++) {
        merged[slots.get(index)] = ordered.get(index);
      }
    }

    Set<String> hidden = new LinkedHashSet<>();
    for (String id : storedHidden) {
      if (columns.containsKey(id)) {
        hidden.add(id);
      }
    }
    if (hidden.size() >= columns.size()) {
      hidden.clear();
    }

    return new GridColumnLayout(new ArrayList<>(List.of(merged)), new ArrayList<>(hidden));
  }

  /**
   * Order the members of a group: known columns by their stored position, new columns at their own position
   *
   * @param members        Members in XML order
   * @param storedPosition Stored position by known column
   * @return Members in the merged order
   */
  private static List<String> orderMembers(List<String> members, Map<String, Integer> storedPosition) {
    List<String> ordered = new ArrayList<>();
    members.stream().filter(storedPosition::containsKey).forEach(ordered::add);
    ordered.sort((first, second) -> Integer.compare(storedPosition.get(first), storedPosition.get(second)));

    Set<String> known = new HashSet<>(ordered);
    for (int index = 0; index < members.size(); index++) {
      if (!known.contains(members.get(index))) {
        ordered.add(Math.min(index, ordered.size()), members.get(index));
      }
    }
    return ordered;
  }
}
