package com.almis.awe.testing.descriptor;

import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.actions.Actions;
import com.almis.awe.model.entities.email.Emails;
import com.almis.awe.model.entities.enumerated.Enumerated;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.entities.maintain.Maintain;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.permissions.Permissions;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queues.Queues;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.services.Services;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.oxm.xstream.XStreamMarshaller;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Descriptor test of an AWE application module. It checks the XML descriptors of the module without a Spring context
 * or a browser, so every rule runs in milliseconds:
 * <ol>
 *   <li>every XML file of the module parses into its AWE model class;</li>
 *   <li>every locale key that the screens, menus, enumerated groups and permissions use exists in every locale file
 *   of the module, unless a module of the AWE framework on the classpath provides it;</li>
 *   <li>every query, maintain target or enumerated group that a screen launches exists in the module or in an AWE
 *   module on the classpath;</li>
 *   <li>the queries and maintain targets marked <code>public="true"</code> are exactly the ones that
 *   {@link #publicTargets()} declares, so nothing becomes public by accident;</li>
 *   <li>the menus declare the options that the AWE shell opens by name ({@link #shellOptions()});</li>
 *   <li>a maintain operation of one row reads the selected row of a grid, never the whole column
 *   ({@link #rowFieldsReadTheSelectedRow()}).</li>
 * </ol>
 * A module extends this class and names its directory:
 * <pre>
 * class MyAppDescriptorTest extends AbstractApplicationDescriptorTest {
 *   protected Path moduleDirectory() {
 *     return Path.of("src/main/resources/application/myapp");
 *   }
 * }
 * </pre>
 * The AWE modules are read from the classpath (<code>application/&lt;module&gt;/...</code>), so the module under test
 * needs its AWE dependencies, as it does to run. Its own module is never taken as a framework module.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractApplicationDescriptorTest {

  /**
   * Attributes of the descriptors whose value is a locale key
   */
  private static final Set<String> KEY_ATTRIBUTES = Set.of("label", "title", "help", "placeholder", "subtitle",
    "description", "message", "empty-title", "chip", "caption", "footer");

  /**
   * A locale key is written in upper case with underscores. Any other value of a key attribute is a literal text
   */
  private static final Pattern KEY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

  private static final Pattern LOCALE_FILE = Pattern.compile("Locale-(.+)\\.xml");

  private static final Set<String> QUERY_SERVER_ACTIONS = Set.of("data", "data-silent");
  private static final Set<String> MAINTAIN_SERVER_ACTIONS = Set.of("maintain", "maintain-silent");
  private static final Set<String> QUERY_INITIAL_LOADS = Set.of("query", "value");
  private static final String SCREEN_DIRECTORY = "screen";
  private static final String TARGET_ACTION = "target-action";

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
  private XStreamMarshaller marshaller;
  private Map<String, Set<String>> frameworkLocaleKeys;
  private Map<String, Set<String>> frameworkTargets;

  /**
   * @return Directory of the application module, for instance <code>src/main/resources/application/myapp</code>
   */
  protected abstract Path moduleDirectory();

  /**
   * @return Queries and maintain targets of the module that are intentionally public (they run without a session)
   */
  protected Set<String> publicTargets() {
    return Set.of();
  }

  /**
   * Every XML of the module parses into its model class
   */
  @Test
  public void descriptorsParseIntoTheirModel() {
    List<String> failures = new ArrayList<>();
    List<Path> files = moduleFiles();
    assertThat(files).as("XML descriptors of " + moduleDirectory()).isNotEmpty();
    for (Path file : files) {
      Class<?> expected = modelOf(file);
      if (expected == null) {
        failures.add(relative(file) + ": it is not a descriptor of an AWE module");
        continue;
      }
      try (InputStream stream = Files.newInputStream(file)) {
        Object parsed = marshaller().unmarshal(new StreamSource(stream));
        if (!expected.isInstance(parsed)) {
          failures.add(relative(file) + ": expected " + expected.getSimpleName() + " but it is "
            + parsed.getClass().getSimpleName());
        }
      } catch (Exception exc) {
        failures.add(relative(file) + ": " + exc.getClass().getSimpleName() + " " + firstLine(exc.getMessage()));
      }
    }
    assertThat(failures).as("Descriptors that do not parse into their AWE model").isEmpty();
  }

  /**
   * Every locale key used by the module exists in every locale file of the module (or in the AWE framework)
   */
  @Test
  public void localeKeysExistInEveryLanguage() {
    Map<String, Set<String>> moduleKeys = new TreeMap<>();
    for (Path file : filesIn("locale")) {
      var matcher = LOCALE_FILE.matcher(file.getFileName().toString());
      assertThat(matcher.matches()).as(relative(file) + " is named Locale-<language>.xml").isTrue();
      moduleKeys.put(matcher.group(1), attributeValues(file, "locale", "name"));
    }
    assertThat(moduleKeys).as("Locale files of the module").isNotEmpty();

    Set<String> used = new TreeSet<>();
    for (String directory : List.of(SCREEN_DIRECTORY, "menu")) {
      filesIn(directory).forEach(file -> used.addAll(usedKeys(file)));
    }
    for (String file : List.of("global/Enumerated.xml", "global/Permissions.xml")) {
      Path path = moduleDirectory().resolve(file);
      if (Files.exists(path)) {
        used.addAll(usedKeys(path));
      }
    }

    List<String> missing = new ArrayList<>();
    moduleKeys.forEach((language, keys) -> {
      Set<String> framework = frameworkLocaleKeys().getOrDefault(language, Set.of());
      used.stream().filter(key -> !keys.contains(key) && !framework.contains(key))
        .forEach(key -> missing.add(language + ": " + key));
    });
    assertThat(missing).as("Locale keys used by the module and missing in a language").isEmpty();
  }

  /**
   * Every query, maintain target and enumerated group that a screen launches exists
   */
  @Test
  public void screenTargetsExist() {
    Set<String> queries = new TreeSet<>(frameworkTargets().get("queries"));
    Set<String> maintains = new TreeSet<>(frameworkTargets().get("maintain"));
    Set<String> enumerated = new TreeSet<>(frameworkTargets().get("enumerated"));
    queries.addAll(ownIds("global/Queries.xml", "query", "id"));
    maintains.addAll(ownIds("global/Maintain.xml", "target", "name"));
    enumerated.addAll(ownIds("global/Enumerated.xml", "group", "id"));
    // An enumerated group is requested with initial-load="enum"; asking for it as a query is reported as a missing query
    List<String> missing = new ArrayList<>();
    for (Path file : filesIn(SCREEN_DIRECTORY)) {
      for (Element element : elements(parse(file))) {
        String target = element.getAttribute(TARGET_ACTION);
        String serverAction = element.getAttribute("server-action");
        String initialLoad = element.getAttribute("initial-load");
        if (!target.isEmpty()) {
          if (QUERY_SERVER_ACTIONS.contains(serverAction) || QUERY_INITIAL_LOADS.contains(initialLoad)) {
            check(missing, file, "query", target, queries);
          } else if (MAINTAIN_SERVER_ACTIONS.contains(serverAction)) {
            check(missing, file, "maintain", target, maintains);
          } else if ("enum".equals(initialLoad)) {
            check(missing, file, "enumerated", target, enumerated);
          }
        }
        String checkTarget = element.getAttribute("check-target");
        if (!checkTarget.isEmpty()) {
          check(missing, file, "query", checkTarget, queries);
        }
      }
    }
    assertThat(missing).as("Targets that a screen launches and no descriptor declares").isEmpty();
  }

  /**
   * The public queries and maintain targets are the ones that the module declares as public on purpose
   */
  @Test
  public void publicTargetsAreDeclared() {
    Set<String> actual = new TreeSet<>();
    actual.addAll(publicIds("global/Queries.xml", "query", "id"));
    actual.addAll(publicIds("global/Maintain.xml", "target", "name"));
    Set<String> declared = new TreeSet<>(publicTargets());

    Set<String> undeclared = new TreeSet<>(actual);
    undeclared.removeAll(declared);
    Set<String> notPublic = new TreeSet<>(declared);
    notPublic.removeAll(actual);

    assertThat(undeclared).as("Public targets that publicTargets() does not declare").isEmpty();
    assertThat(notPublic).as("Targets declared by publicTargets() that are not public").isEmpty();
  }

  /**
   * @return Options that the AWE shell opens by name, per menu file of the module: the user menu opens
   * <code>user-settings</code> and the error pages open <code>error-4xx</code> and <code>error-5xx</code>
   */
  protected Map<String, Set<String>> shellOptions() {
    return Map.of("menu/private.xml", Set.of("user-settings"), "menu/public.xml", Set.of("error-4xx", "error-5xx"));
  }

  /**
   * The menus declare the options that the AWE shell opens by name, or the shell fails with "option not defined"
   */
  @Test
  public void shellOptionsAreDeclared() throws IOException {
    List<String> missing = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : new TreeMap<>(shellOptions()).entrySet()) {
      Path menu = moduleDirectory().resolve(entry.getKey());
      String content = Files.exists(menu) ? Files.readString(menu) : "";
      for (String option : new TreeSet<>(entry.getValue())) {
        if (!content.contains("name=\"" + option + "\"")) {
          missing.add(entry.getKey() + ": " + option);
        }
      }
    }
    assertThat(missing).as("Options that the AWE shell opens and the menus do not declare").isEmpty();
  }

  /**
   * A request carries every column of a grid as a list (<code>IdeTkt=[602]</code>) and the selected row as
   * <code>IdeTkt.selected=602</code>. A maintain operation that runs once (not <code>multiple="true"</code>) and stores
   * a field from a variable named like a column of the screen that calls it receives the list and fails at run time
   * with "Error in maintain operation". It has to read <code>&lt;column&gt;.selected</code>. A criterion of the screen
   * with the same name carries a single value, so it is not a defect
   */
  @Test
  public void rowFieldsReadTheSelectedRow() {
    Path file = moduleDirectory().resolve("global/Maintain.xml");
    if (!Files.exists(file)) {
      return;
    }
    Map<String, List<Element>> operations = new TreeMap<>();
    for (Element target : elements(parse(file))) {
      if ("target".equals(target.getTagName())) {
        operations.put(target.getAttribute("name"), singleRowOperations(target));
      }
    }
    List<String> defects = new ArrayList<>();
    for (Path screen : filesIn(SCREEN_DIRECTORY)) {
      List<Element> elements = elements(parse(screen));
      Set<String> columns = new TreeSet<>();
      Set<String> criteria = new TreeSet<>();
      Set<String> called = new TreeSet<>();
      for (Element element : elements) {
        if ("column".equals(element.getTagName()) && !element.getAttribute("name").isEmpty()) {
          columns.add(element.getAttribute("name"));
        } else if ("criteria".equals(element.getTagName())) {
          criteria.add(element.getAttribute("id"));
        }
        if (element.getAttribute("server-action").startsWith("maintain") && !element.getAttribute(TARGET_ACTION).isEmpty()) {
          called.add(element.getAttribute(TARGET_ACTION));
        }
      }
      for (String target : called) {
        for (Element operation : operations.getOrDefault(target, List.of())) {
          singleRowListFields(operation, columns, criteria).forEach(field ->
            defects.add(relative(screen) + ": maintain " + target + " stores field " + field[0] + " from variable "
              + field[1] + ", a column of the grid that arrives as a list; read " + field[1] + ".selected"));
        }
      }
    }
    assertThat(defects).as("Maintain fields of one row that read a whole grid column").isEmpty();
  }

  private List<Element> singleRowOperations(Element target) {
    List<Element> operations = new ArrayList<>();
    for (Node child = target.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element operation && Set.of("insert", "update").contains(operation.getTagName())
        && !"true".equals(operation.getAttribute("multiple"))) {
        operations.add(operation);
      }
    }
    return operations;
  }

  private List<String[]> singleRowListFields(Element operation, Set<String> columns, Set<String> criteria) {
    Map<String, String> variables = new TreeMap<>();
    for (Element element : childElements(operation, "variable")) {
      variables.put(element.getAttribute("id"), element.getAttribute("name"));
    }
    List<String[]> found = new ArrayList<>();
    for (Element field : childElements(operation, "field")) {
      String name = variables.get(field.getAttribute("variable"));
      if (name != null && columns.contains(name) && !criteria.contains(name)) {
        found.add(new String[]{field.getAttribute("id"), name});
      }
    }
    return found;
  }

  private List<Element> childElements(Element parent, String tag) {
    List<Element> children = new ArrayList<>();
    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element element && tag.equals(element.getTagName())) {
        children.add(element);
      }
    }
    return children;
  }

  /**
   * @return The application.properties of the module (two levels above the module directory, which is
   * src/main/resources/application/&lt;name&gt; in an application)
   */
  protected Path propertiesFile() {
    return moduleDirectory().resolve("../../application.properties").normalize();
  }

  /**
   * A sample application must not ship a literal key or secret: every property that ends with <code>master-key</code>
   * or <code>secret</code> has to read an environment variable, <code>${NAME:sample value}</code>, so a real deployment
   * can set it without touching the repository
   */
  @Test
  public void secretsAreExternalised() throws IOException {
    Path file = propertiesFile();
    Assumptions.assumeTrue(Files.exists(file), "No application.properties at " + file);
    Pattern externalised = Pattern.compile("^\\$\\{[A-Z][A-Z0-9_]*:.+}$");
    List<String> literal = new ArrayList<>();
    for (String line : Files.readAllLines(file)) {
      String trimmed = line.trim();
      int equals = trimmed.indexOf('=');
      if (trimmed.startsWith("#") || equals < 0) {
        continue;
      }
      String key = trimmed.substring(0, equals).trim();
      String value = trimmed.substring(equals + 1).trim();
      if ((key.endsWith("master-key") || key.endsWith("secret")) && !externalised.matcher(value).matches()) {
        literal.add(key);
      }
    }
    assertThat(literal).as("Keys and secrets of " + file + " that are not read from an environment variable").isEmpty();
  }

  /**
   * @return The Flyway scripts of the module (two levels above the module directory, db/migration)
   */
  protected Path seedDirectory() {
    return moduleDirectory().resolve("../../db/migration").normalize();
  }

  /**
   * The demo users that the landing page shows are the locale keys <code>*_DEMO_U&lt;n&gt;_USER</code> of the English
   * locale (read with the AWE locale model). Every one of them has to be inserted into the AWE user table
   * <code>ope</code>, in its login column <code>l1_nom</code>, by a Flyway script of the module, so the credentials of
   * the landing page cannot drift from the seed (the same name as an owner or a pet does not count). A module without
   * those keys has nothing to check
   */
  @Test
  public void demoUsersExistInTheSeed() throws IOException {
    Path locale = moduleDirectory().resolve("locale/Locale-en-GB.xml");
    Assumptions.assumeTrue(Files.exists(locale), "No English locale at " + locale);
    Locales locales;
    try (InputStream stream = Files.newInputStream(locale)) {
      locales = (Locales) marshaller().unmarshal(new StreamSource(stream));
    }
    Pattern demoKey = Pattern.compile("\\w+_DEMO_U\\d+_USER");
    List<String> users = locales.getBaseElementList().stream()
      .filter(entry -> entry.getName() != null && demoKey.matcher(entry.getName()).matches())
      .map(Global::getValue).toList();
    Assumptions.assumeFalse(users.isEmpty(), "The English locale of " + moduleDirectory() + " has no demo users (*_DEMO_U<n>_USER)");
    Path migrations = seedDirectory();
    assertThat(migrations).as("The migration directory of " + moduleDirectory()).isDirectory();
    Set<String> logins = new TreeSet<>();
    try (Stream<Path> scripts = Files.walk(migrations)) {
      for (Path script : scripts.filter(path -> path.toString().endsWith(".sql")).toList()) {
        logins.addAll(loginsInsertedIntoOpe(Files.readString(script)));
      }
    }
    assertThat(users).as("Demo users of the landing page that no Flyway script inserts into ope (l1_nom)")
      .allSatisfy(user -> assertThat(logins).contains(user));
  }

  /**
   * The values of the login column (l1_nom) of every <code>INSERT INTO ope (...) VALUES (...), (...)</code>
   */
  private static Set<String> loginsInsertedIntoOpe(String sql) {
    Set<String> logins = new TreeSet<>();
    Matcher insert = Pattern.compile("(?is)INSERT\\s+INTO\\s+ope\\s*\\(([^)]*)\\)\\s*VALUES\\s*(.*?);").matcher(sql);
    while (insert.find()) {
      List<String> columns = Arrays.stream(insert.group(1).split(",")).map(column -> column.trim().toLowerCase()).toList();
      int login = columns.indexOf("l1_nom");
      if (login < 0) {
        continue;
      }
      for (List<String> row : valueRows(insert.group(2))) {
        if (row.size() > login && row.get(login).startsWith("'")) {
          logins.add(row.get(login).substring(1, row.get(login).length() - 1).replace("''", "'"));
        }
      }
    }
    return logins;
  }

  /**
   * Splits <code>(a, 'b, c'), (d, e)</code> into rows of values, trimmed, without splitting inside quotes or parentheses
   */
  private static List<List<String>> valueRows(String values) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = null;
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    int depth = 0;
    for (char c : values.toCharArray()) {
      if (c == '\'') {
        quoted = !quoted;
      }
      if (!quoted && c == '(') {
        depth++;
        if (depth == 1) {
          row = new ArrayList<>();
          value.setLength(0);
          continue;
        }
      } else if (!quoted && c == ')') {
        depth--;
        if (depth == 0) {
          row.add(value.toString().trim());
          rows.add(row);
          continue;
        }
      } else if (!quoted && c == ',' && depth == 1) {
        row.add(value.toString().trim());
        value.setLength(0);
        continue;
      }
      if (depth >= 1) {
        value.append(c);
      }
    }
    return rows;
  }

  private void check(List<String> missing, Path file, String kind, String target, Set<String> known) {
    if (!known.contains(target)) {
      missing.add(relative(file) + ": " + kind + " " + target);
    }
  }

  // ---- Files ----

  private List<Path> moduleFiles() {
    try (Stream<Path> files = Files.walk(moduleDirectory())) {
      return files.filter(file -> file.toString().endsWith(".xml")).sorted().toList();
    } catch (IOException exc) {
      throw new AssertionError("The module directory " + moduleDirectory() + " cannot be read", exc);
    }
  }

  private List<Path> filesIn(String directory) {
    Path path = moduleDirectory().resolve(directory);
    if (!Files.isDirectory(path)) {
      return List.of();
    }
    try (Stream<Path> files = Files.walk(path)) {
      return files.filter(file -> file.toString().endsWith(".xml")).sorted().toList();
    } catch (IOException exc) {
      throw new AssertionError("The directory " + path + " cannot be read", exc);
    }
  }

  private String relative(Path file) {
    return moduleDirectory().relativize(file).toString().replace('\\', '/');
  }

  private Class<?> modelOf(Path file) {
    String relative = relative(file);
    String name = file.getFileName().toString();
    if (relative.startsWith("screen/")) {
      return Screen.class;
    } else if (relative.startsWith("menu/")) {
      return Menu.class;
    } else if (relative.startsWith("profile/")) {
      return Profile.class;
    } else if (relative.startsWith("locale/")) {
      return Locales.class;
    } else if (relative.startsWith("global/")) {
      return switch (name) {
        case "Queries.xml" -> Queries.class;
        case "Maintain.xml" -> Maintain.class;
        case "Enumerated.xml" -> Enumerated.class;
        case "Actions.xml" -> Actions.class;
        case "Services.xml" -> Services.class;
        case "Queues.xml" -> Queues.class;
        case "Email.xml" -> Emails.class;
        case "Permissions.xml" -> Permissions.class;
        default -> null;
      };
    }
    return null;
  }

  // ---- Model parsing ----

  /**
   * Same driver, annotations and security settings as the framework autoconfiguration
   */
  private XStreamMarshaller marshaller() {
    if (marshaller == null) {
      XStreamMarshaller created = new XStreamMarshaller();
      created.setStreamDriver(new DomDriver(null, new NoNameCoder()));
      created.getXStream().processAnnotations(new Class[]{
        com.almis.awe.model.entities.Element.class, Enumerated.class, Queries.class, Queues.class, Maintain.class,
        Emails.class, Services.class, Actions.class, Profile.class, Screen.class, Menu.class, Locales.class,
        Permissions.class
      });
      created.getXStream().allowTypesByWildcard(new String[]{"java.*", "com.almis.awe.model.entities.**"});
      marshaller = created;
    }
    return marshaller;
  }

  // ---- DOM helpers ----

  private Document parse(Path file) {
    try (InputStream stream = Files.newInputStream(file)) {
      return parse(stream);
    } catch (IOException exc) {
      throw new AssertionError("The file " + file + " cannot be read", exc);
    }
  }

  private Document parse(InputStream stream) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(stream);
    } catch (ParserConfigurationException | SAXException exc) {
      throw new IOException("The XML cannot be parsed: " + exc.getMessage(), exc);
    }
  }

  private List<Element> elements(Document document) {
    List<Element> result = new ArrayList<>();
    NodeList all = document.getElementsByTagName("*");
    for (int index = 0; index < all.getLength(); index++) {
      result.add((Element) all.item(index));
    }
    return result;
  }

  private Set<String> usedKeys(Path file) {
    Set<String> keys = new TreeSet<>();
    for (Element element : elements(parse(file))) {
      NamedNodeMap attributes = element.getAttributes();
      for (int index = 0; index < attributes.getLength(); index++) {
        Node attribute = attributes.item(index);
        if (KEY_ATTRIBUTES.contains(attribute.getNodeName()) && KEY_PATTERN.matcher(attribute.getNodeValue()).matches()) {
          keys.add(attribute.getNodeValue());
        }
      }
    }
    return keys;
  }

  private Set<String> attributeValues(Path file, String tag, String attribute) {
    Set<String> values = new TreeSet<>();
    for (Element element : elements(parse(file))) {
      if (tag.equals(element.getTagName()) && !element.getAttribute(attribute).isEmpty()) {
        values.add(element.getAttribute(attribute));
      }
    }
    return values;
  }

  private Set<String> ownIds(String file, String tag, String attribute) {
    Path path = moduleDirectory().resolve(file);
    return Files.exists(path) ? attributeValues(path, tag, attribute) : Set.of();
  }

  private Set<String> publicIds(String file, String tag, String attribute) {
    Path path = moduleDirectory().resolve(file);
    Set<String> ids = new TreeSet<>();
    if (Files.exists(path)) {
      for (Element element : elements(parse(path))) {
        if (tag.equals(element.getTagName()) && "true".equals(element.getAttribute("public"))) {
          ids.add(element.getAttribute(attribute));
        }
      }
    }
    return ids;
  }

  // ---- AWE modules on the classpath ----

  private String ownModule() {
    return moduleDirectory().toAbsolutePath().normalize().getFileName().toString();
  }

  private Map<String, Set<String>> frameworkLocaleKeys() {
    if (frameworkLocaleKeys == null) {
      Map<String, Set<String>> keys = new TreeMap<>();
      for (Resource resource : classpathResources("locale/Locale-*.xml")) {
        var matcher = LOCALE_FILE.matcher(String.valueOf(resource.getFilename()));
        if (matcher.matches()) {
          keys.computeIfAbsent(matcher.group(1), language -> new TreeSet<>())
            .addAll(attributeValues(resource, "locale", "name"));
        }
      }
      frameworkLocaleKeys = keys;
    }
    return frameworkLocaleKeys;
  }

  private Map<String, Set<String>> frameworkTargets() {
    if (frameworkTargets == null) {
      Map<String, Set<String>> targets = new TreeMap<>();
      targets.put("queries", frameworkIds("global/Queries.xml", "query", "id"));
      targets.put("maintain", frameworkIds("global/Maintain.xml", "target", "name"));
      targets.put("enumerated", frameworkIds("global/Enumerated.xml", "group", "id"));
      frameworkTargets = targets;
    }
    return frameworkTargets;
  }

  private Set<String> frameworkIds(String file, String tag, String attribute) {
    Set<String> ids = new TreeSet<>();
    for (Resource resource : classpathResources(file)) {
      ids.addAll(attributeValues(resource, tag, attribute));
    }
    return ids;
  }

  private List<Resource> classpathResources(String file) {
    try {
      Resource[] resources = resolver.getResources("classpath*:application/*/" + file);
      List<Resource> result = new ArrayList<>();
      for (Resource resource : resources) {
        String module = moduleOf(resource.getURL().toString());
        if (!module.equals(ownModule())) {
          result.add(resource);
        }
      }
      return result;
    } catch (IOException exc) {
      throw new AssertionError("The AWE modules of the classpath cannot be read", exc);
    }
  }

  /**
   * Module of a classpath resource: the segment after the last <code>/application/</code> folder of its URL
   *
   * @param url URL of the resource
   * @return Module name
   */
  static String moduleOf(String url) {
    String marker = "/application/";
    int start = url.lastIndexOf(marker) + marker.length();
    return url.substring(start, url.indexOf('/', start));
  }

  private Set<String> attributeValues(Resource resource, String tag, String attribute) {
    Set<String> values = new TreeSet<>();
    try (InputStream stream = resource.getInputStream()) {
      for (Element element : elements(parse(stream))) {
        if (tag.equals(element.getTagName()) && !element.getAttribute(attribute).isEmpty()) {
          values.add(element.getAttribute(attribute));
        }
      }
    } catch (IOException exc) {
      throw new AssertionError("The resource " + resource + " cannot be read", exc);
    }
    return values;
  }

  private static String firstLine(String message) {
    return message == null ? "" : message.lines().findFirst().orElse("");
  }
}
