package com.almis.awe.observability.instrumentation;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounds the number of series of the AWE meters: for the meters named {@code awe.*}, each of the {@code id} and
 * {@code screen} tags admits at most a fixed number of distinct values per meter. Later new values are reported as
 * {@value #OTHER}, while the values already admitted keep their own series.
 *
 * <p>The long task timer {@code <name>.active} that an observation creates shares the values of its timer, so both
 * show the same identifiers.</p>
 */
public class AweTagValueLimitFilter implements MeterFilter {

  /** Value of the tags beyond the cap */
  public static final String OTHER = "other";

  private static final String PREFIX = "awe.";
  private static final String ACTIVE_SUFFIX = ".active";
  private static final Set<String> LIMITED_KEYS = Set.of("id", "screen");

  private final int maxValues;
  private final Map<String, Set<String>> admitted = new ConcurrentHashMap<>();

  /**
   * Constructor
   *
   * @param maxValues Maximum distinct values of each limited tag of a meter
   */
  public AweTagValueLimitFilter(int maxValues) {
    this.maxValues = maxValues;
  }

  @Override
  public Meter.Id map(Meter.Id id) {
    if (!id.getName().startsWith(PREFIX)) {
      return id;
    }
    String meter = id.getName().endsWith(ACTIVE_SUFFIX)
      ? id.getName().substring(0, id.getName().length() - ACTIVE_SUFFIX.length())
      : id.getName();

    boolean changed = false;
    List<Tag> tags = new ArrayList<>();
    for (Tag tag : id.getTagsAsIterable()) {
      if (LIMITED_KEYS.contains(tag.getKey()) && !admit(meter + "|" + tag.getKey(), tag.getValue())) {
        tags.add(Tag.of(tag.getKey(), OTHER));
        changed = true;
      } else {
        tags.add(tag);
      }
    }
    return changed ? id.replaceTags(tags) : id;
  }

  /**
   * Whether a value keeps its own series: it was admitted before, or there is still room for it
   */
  private boolean admit(String key, String value) {
    Set<String> values = admitted.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
    if (values.contains(value)) {
      return true;
    }
    synchronized (values) {
      if (values.size() < maxValues) {
        values.add(value);
        return true;
      }
      return values.contains(value);
    }
  }
}
