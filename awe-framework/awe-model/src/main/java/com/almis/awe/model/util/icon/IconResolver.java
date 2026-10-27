package com.almis.awe.model.util.icon;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Icon value of the screen XML resolved to a Lucide icon of the AWE 5 sprite or, during the transition, to its current
 * font (#885). Server port of the resolver of the clients (awe-client-react/src/utilities/iconResolver.js) over the
 * same tables of awe-design-tokens/icons: the equivalences of the Font Awesome 4, Google Material Icons (mdi:) and
 * PrimeIcons names, and the Lucide names the pinned lucide-static provides.
 */
@Slf4j
public final class IconResolver {

  /**
   * Family of the Lucide icons
   */
  public static final String LUCIDE = "lucide";

  /**
   * The Lucide sprite of the clients, relative to the base of the page ({@code <base href>} is the context path)
   */
  public static final String SPRITE = "icons/lucide-sprite.svg";

  private static final String FONT_AWESOME = "fa";
  private static final String PRIME_ICONS = "pi";
  // Family written in the icon value -> family of the equivalence table
  private static final String MATERIAL = "material";
  private static final Map<String, String> FAMILIES = Map.of(FONT_AWESOME, FONT_AWESOME, "mdi", MATERIAL,
    PRIME_ICONS, PRIME_ICONS);
  // Prefix of a bare name -> family (a bare name without prefix is Font Awesome)
  private static final Map<String, String> PREFIXES = Map.of(FONT_AWESOME, "fa-", PRIME_ICONS, "pi-");

  private static final Map<String, Map<String, String>> EQUIVALENCES = readEquivalences();
  private static final Set<String> LUCIDE_ICONS;
  private static final Map<String, String> LUCIDE_ALIASES;
  private static final String LUCIDE_VERSION;
  private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

  static {
    Map<String, Object> lucideNames = read("lucide-names.json");
    LUCIDE_ICONS = Collections.unmodifiableSet(new HashSet<>(stringList(lucideNames.get("icons"))));
    LUCIDE_ALIASES = stringMap(lucideNames.get("aliases"));
    LUCIDE_VERSION = String.valueOf(lucideNames.get("version"));
  }

  private IconResolver() {
  }

  /**
   * Resolved icon
   *
   * @param family  Family: {@code lucide}, or the family of its current font ({@code fa}, {@code material},
   *                {@code pi} or another family)
   * @param name    Icon name
   * @param classes Extra classes written after the icon, separated by a space
   */
  public record Icon(String family, String name, String classes) {

    /**
     * @return Whether it is a Lucide icon of the sprite
     */
    public boolean isLucide() {
      return LUCIDE.equals(family);
    }
  }

  /**
   * Resolve the icon value of the screen XML
   *
   * @param icon Icon value: the icon ("name", "fa-name", "fa:name", "mdi:name", "lucide:name", "pi-name", "pi:name")
   *             and optionally extra classes after it ("level-up icon-rotate-90")
   * @return The Lucide icon, or the icon in its current font for a name without Lucide equivalence; empty without icon
   * or for an unknown Lucide name (warned once per name)
   */
  public static Optional<Icon> resolve(String icon) {
    String[] tokens = icon == null ? new String[0] : icon.trim().split("\\s+");
    if (tokens.length == 0 || tokens[0].isEmpty()) {
      return Optional.empty();
    }
    String[] familyAndName = parse(tokens[0]);
    String family = familyAndName[0];
    String name = familyAndName[1];
    if (name.isEmpty()) {
      return Optional.empty();
    }
    String classes = String.join(" ", Arrays.asList(tokens).subList(1, tokens.length));
    if (LUCIDE.equals(family)) {
      return resolveLucide(name).map(lucideName -> new Icon(LUCIDE, lucideName, classes));
    }
    String lucideName = EQUIVALENCES.getOrDefault(family, Map.of()).get(name);
    return Optional.of(lucideName != null ? new Icon(LUCIDE, lucideName, classes) : new Icon(family, name, classes));
  }

  // Family and name of an icon token: "fa:name", "mdi:name", "lucide:name", "pi:name", "fa-name", "pi-name" or "name"
  private static String[] parse(String token) {
    int separator = token.indexOf(':');
    if (separator > -1) {
      String family = token.substring(0, separator);
      String tableFamily = FAMILIES.getOrDefault(family, family);
      return new String[]{tableFamily, stripPrefix(tableFamily, token.substring(separator + 1))};
    }
    return PREFIXES.entrySet().stream()
      .filter(prefix -> token.startsWith(prefix.getValue()))
      .findFirst()
      .map(prefix -> new String[]{prefix.getKey(), token.substring(prefix.getValue().length())})
      .orElse(new String[]{FONT_AWESOME, token});
  }

  private static String stripPrefix(String family, String name) {
    String prefix = PREFIXES.get(family);
    return prefix != null && name.startsWith(prefix) ? name.substring(prefix.length()) : name;
  }

  private static Optional<String> resolveLucide(String name) {
    if (LUCIDE_ICONS.contains(name)) {
      return Optional.of(name);
    }
    Optional<String> current = Optional.ofNullable(LUCIDE_ALIASES.get(name));
    if (current.isEmpty() && WARNED.add(name)) {
      log.warn("Unknown Lucide icon \"lucide:{}\" (lucide-static {}): it is not shown", name, LUCIDE_VERSION);
    }
    return current;
  }

  private static Map<String, Map<String, String>> readEquivalences() {
    Map<String, Object> equivalences = read("icon-equivalences.json");
    return Map.of(FONT_AWESOME, stringMap(equivalences.get(FONT_AWESOME)),
      MATERIAL, stringMap(equivalences.get(MATERIAL)),
      PRIME_ICONS, stringMap(equivalences.get(PRIME_ICONS)));
  }

  private static Map<String, Object> read(String file) {
    try (InputStream input = IconResolver.class.getResourceAsStream(file)) {
      if (input == null) {
        throw new IllegalStateException("Icon table not found in the classpath: " + file);
      }
      return JsonMapper.builder().build().readValue(input, new TypeReference<>() {
      });
    } catch (IOException exc) {
      throw new UncheckedIOException("Icon table could not be read: " + file, exc);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> stringMap(Object value) {
    return value instanceof Map<?, ?> map ? Map.copyOf((Map<String, String>) map) : Map.of();
  }

  @SuppressWarnings("unchecked")
  private static List<String> stringList(Object value) {
    return value instanceof List<?> list ? (List<String>) list : List.of();
  }
}
