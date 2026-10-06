package com.almis.awe.testing.guard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keeps the sources of browser tests (the {@code *IT} classes) free of selectors and automation-tool types, so the tests
 * only describe screen steps and the way the elements are located lives in the front-end instructions of
 * {@code awe-testing}. The rule is the same for every web engine and every automation tool, which is what allows a
 * test written once to run against another engine or another tool.
 *
 * <p>The guard is a plain text scan of Java sources (no automation library is needed to run it). By default it only scans
 * the integration test classes (files named {@code *IT.java}): the helper classes of a product that extend
 * {@code SeleniumUtilities} are where locators belong, so they are not checked. Use {@link #files(String)} to scan other
 * files. It reports three kinds of violations:</p>
 * <ul>
 *   <li>{@value #RULE_IMPORT}: imports of {@code org.openqa.selenium.*}, {@code TestIds} or {@code TestAttributes}.</li>
 *   <li>{@value #RULE_IDENTIFIER}: use of {@code By}, {@code WebDriver}, {@code WebElement}, {@code JavascriptExecutor},
 *   {@code Actions}, {@code new Select(...)}, {@code getDriver(...)}, {@code executeScript(...)}, {@code Locator},
 *   {@code getBrowser(...)}, {@code TestIds} or {@code TestAttributes}. Comments and string literals are ignored.</li>
 *   <li>{@value #RULE_SELECTOR}: a string literal that looks like a selector passed as the selector argument of a helper
 *   that takes a raw selector ({@code click}, {@code clearText}, {@code checkText}, {@code checkTextContains},
 *   {@code checkTextNotContains}, {@code checkPresence}, {@code checkVisible}, {@code checkNotVisible},
 *   {@code checkVisibleAndContains}, {@code checkTextInEmbeddedFrame}, {@code waitForCssSelector}, {@code waitForCssLocator},
 *   {@code waitForText} with a class, the four-argument {@code checkLogin} and the two-argument {@code checkLogout}).</li>
 * </ul>
 *
 * <p>The selector heuristic is deliberately simple. Only the <em>selector position</em> of those helpers is inspected
 * (the expected texts are never checked), only the string literals written directly in that argument count (a literal
 * inside a nested call such as {@code getCriterion("User")} does not), and a literal is considered a selector when it
 * contains any of {@code [ ] # . > + ~ * / :} or a space. A bare tag name such as {@code "button"} is therefore not
 * detected: the guard prevents the common cases, it does not prove the absence of locators. Any literal given to
 * {@code waitForText} is reported, because its first argument is always a CSS class.</p>
 *
 * <p>A violation that is justified is allowed explicitly with {@link #allow(String, String, String)}: the file (or the end
 * of its relative path), the exact snippet reported and a mandatory reason. An allowance that no longer matches anything
 * is reported as stale, so the allowlist cannot silently outlive the code it excused. A product can apply the same rule
 * to its own tests:</p>
 *
 * <pre>{@code
 * Report report = BrowserTestSourceGuard.create()
 *     .allow("FileManagerIT.java", "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"Files\")", "Third-party application in a frame")
 *     .scan(Path.of("src/test/java"));
 * assertThat(report.isClean()).as(report.describe()).isTrue();
 * }</pre>
 *
 * <p>To migrate suites step by step, {@link #allowBaseline(Path, String)} reads a baseline file with the violations that
 * exist today (generated with {@link Report#toBaseline()}); new violations fail the check, and so does a baselined
 * violation that disappeared, so the baseline can only shrink.</p>
 */
public final class BrowserTestSourceGuard {

  /** Rule name of the forbidden imports */
  public static final String RULE_IMPORT = "forbidden-import";
  /** Rule name prefix of the forbidden identifiers (followed by {@code :} and the identifier) */
  public static final String RULE_IDENTIFIER = "forbidden-identifier";
  /** Rule name of the selector literals */
  public static final String RULE_SELECTOR = "selector-literal";

  private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
    "^\\s*import\\s+(?:static\\s+)?(?:org\\.openqa\\.selenium\\b|com\\.almis\\.awe\\.testing\\.selenium\\.(?:TestIds|TestAttributes)\\b)");
  private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s");

  private static final Map<String, Pattern> FORBIDDEN_IDENTIFIERS = new LinkedHashMap<>();
  private static final Pattern SELECTOR_HELPER = Pattern.compile(
    "(?<![\\w$])(click|clearText|checkText|checkTextContains|checkTextNotContains|checkPresence|checkVisible|checkNotVisible"
      + "|checkVisibleAndContains|checkTextInEmbeddedFrame|waitForCssSelector|waitForCssLocator|waitForText|checkLogin|checkLogout)\\s*\\(");
  private static final String SELECTOR_CHARACTERS = "[]#.>+~*/:";

  static {
    FORBIDDEN_IDENTIFIERS.put("By", Pattern.compile("\\bBy\\b"));
    FORBIDDEN_IDENTIFIERS.put("WebDriver", Pattern.compile("\\bWebDriver\\b"));
    FORBIDDEN_IDENTIFIERS.put("WebElement", Pattern.compile("\\bWebElement\\b"));
    FORBIDDEN_IDENTIFIERS.put("JavascriptExecutor", Pattern.compile("\\bJavascriptExecutor\\b"));
    FORBIDDEN_IDENTIFIERS.put("Select", Pattern.compile("\\bnew\\s+Select\\s*\\("));
    FORBIDDEN_IDENTIFIERS.put("Actions", Pattern.compile("\\bActions\\b"));
    FORBIDDEN_IDENTIFIERS.put("getDriver", Pattern.compile("\\bgetDriver\\s*\\("));
    FORBIDDEN_IDENTIFIERS.put("Locator", Pattern.compile("\\bLocator\\b"));
    FORBIDDEN_IDENTIFIERS.put("getBrowser", Pattern.compile("\\bgetBrowser\\s*\\("));
    FORBIDDEN_IDENTIFIERS.put("executeScript", Pattern.compile("\\bexecuteScript\\s*\\("));
    FORBIDDEN_IDENTIFIERS.put("TestIds", Pattern.compile("\\bTestIds\\b"));
    FORBIDDEN_IDENTIFIERS.put("TestAttributes", Pattern.compile("\\bTestAttributes\\b"));
  }

  private static final String DEFAULT_FILES = "*IT.java";

  private final List<Allowance> allowances = new ArrayList<>();
  private PathMatcher fileFilter = fileMatcher(DEFAULT_FILES);

  private BrowserTestSourceGuard() {
  }

  /**
   * Create a guard with the default rules and no allowance
   *
   * @return Guard
   */
  public static BrowserTestSourceGuard create() {
    return new BrowserTestSourceGuard();
  }

  /**
   * Choose the files to scan, replacing the default ({@code *IT.java}). The glob is matched against the name of each file
   * (for instance {@code *IT.java}, {@code *Test.java} or {@code *.java} for every source)
   *
   * @param glob Glob of the file names to scan
   * @return This guard
   */
  public BrowserTestSourceGuard files(String glob) {
    if (isBlank(glob)) {
      throw new IllegalArgumentException("The files to scan need a glob, for instance " + DEFAULT_FILES);
    }
    this.fileFilter = fileMatcher(glob.trim());
    return this;
  }

  /**
   * Allow every occurrence of a violation, because it is justified
   *
   * @param file    Name of the file, or the end of its path relative to the scanned directory
   * @param snippet Snippet exactly as reported in the violation
   * @param reason  Why the violation is acceptable (mandatory)
   * @return This guard
   */
  public BrowserTestSourceGuard allow(String file, String snippet, String reason) {
    return allow(file, snippet, reason, Integer.MAX_VALUE);
  }

  /**
   * Allow an exact number of occurrences of a violation. More occurrences are reported as violations and fewer are
   * reported as a stale allowance
   *
   * @param file    Name of the file, or the end of its path relative to the scanned directory
   * @param snippet Snippet exactly as reported in the violation
   * @param reason  Why the violation is acceptable (mandatory)
   * @param times   Number of occurrences allowed
   * @return This guard
   */
  public BrowserTestSourceGuard allow(String file, String snippet, String reason, int times) {
    if (isBlank(file) || isBlank(snippet)) {
      throw new IllegalArgumentException("An allowance needs the file and the snippet it excuses");
    }
    if (isBlank(reason)) {
      throw new IllegalArgumentException("An allowance needs a reason: " + file + ": " + snippet);
    }
    if (times < 1) {
      throw new IllegalArgumentException("An allowance needs at least one occurrence: " + file + ": " + snippet);
    }
    allowances.add(new Allowance(file.trim(), snippet.trim(), reason.trim(), times));
    return this;
  }

  /**
   * Allow the violations listed in a baseline file. Each line is {@code file:snippet} (the first colon separates them),
   * a violation that appears several times is listed several times, and blank lines and lines starting with {@code #}
   * are ignored. The file is generated with {@link Report#toBaseline()}
   *
   * @param baseline Baseline file
   * @param reason   Why the violations are tolerated (mandatory)
   * @return This guard
   * @throws IOException When the file cannot be read
   */
  public BrowserTestSourceGuard allowBaseline(Path baseline, String reason) throws IOException {
    if (isBlank(reason)) {
      throw new IllegalArgumentException("A baseline needs a reason: " + baseline);
    }
    Map<String, Integer> entries = new LinkedHashMap<>();
    for (String line : Files.readAllLines(baseline, StandardCharsets.UTF_8)) {
      String entry = line.trim();
      if (entry.isEmpty() || entry.startsWith("#")) {
        continue;
      }
      int separator = entry.indexOf(':');
      if (separator < 1 || separator == entry.length() - 1) {
        throw new IllegalArgumentException("Wrong baseline entry, expected 'file:snippet': " + entry);
      }
      entries.merge(entry, 1, Integer::sum);
    }
    entries.forEach((entry, times) -> {
      int separator = entry.indexOf(':');
      allow(entry.substring(0, separator), entry.substring(separator + 1), reason, times);
    });
    return this;
  }

  /**
   * Scan the Java sources under a directory whose name matches the files to scan ({@code *IT.java} unless
   * {@link #files(String)} says otherwise)
   *
   * @param directory Directory with the test sources
   * @return Report
   * @throws IOException When a file cannot be read
   */
  public Report scan(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException("The directory to scan does not exist: " + directory);
    }

    List<Violation> found = new ArrayList<>();
    List<Path> files;
    try (Stream<Path> walk = Files.walk(directory)) {
      files = walk.filter(path -> Files.isRegularFile(path) && fileFilter.matches(path.getFileName())).sorted().collect(Collectors.toList());
    }
    for (Path file : files) {
      String relative = directory.relativize(file).toString().replace('\\', '/');
      scanSource(relative, Files.readString(file, StandardCharsets.UTF_8), found);
    }
    found.sort(Comparator.comparing(Violation::file).thenComparingInt(Violation::line).thenComparing(Violation::rule));

    return applyAllowances(found);
  }

  private Report applyAllowances(List<Violation> found) {
    int[] used = new int[allowances.size()];
    List<Violation> violations = new ArrayList<>();
    for (Violation violation : found) {
      boolean allowed = false;
      for (int index = 0; index < allowances.size() && !allowed; index++) {
        Allowance allowance = allowances.get(index);
        if (allowance.matches(violation) && used[index] < allowance.times()) {
          used[index]++;
          allowed = true;
        }
      }
      if (!allowed) {
        violations.add(violation);
      }
    }

    List<Allowance> stale = new ArrayList<>();
    List<Integer> matched = new ArrayList<>();
    for (int index = 0; index < allowances.size(); index++) {
      int expected = allowances.get(index).times() == Integer.MAX_VALUE ? 1 : allowances.get(index).times();
      if (used[index] < expected) {
        stale.add(allowances.get(index));
        matched.add(used[index]);
      }
    }
    return new Report(violations, stale, matched);
  }

  private void scanSource(String file, String source, List<Violation> found) {
    String code = mask(source);
    int[] lineStarts = lineStarts(source);
    String[] codeLines = code.split("\n", -1);
    String[] sourceLines = source.split("\n", -1);

    for (int index = 0; index < codeLines.length; index++) {
      String codeLine = codeLines[index];
      if (FORBIDDEN_IMPORT.matcher(codeLine).find()) {
        found.add(new Violation(file, index + 1, RULE_IMPORT, snippetOfLine(sourceLines[index], codeLine)));
      } else if (!IMPORT_LINE.matcher(codeLine).find()) {
        for (Map.Entry<String, Pattern> identifier : FORBIDDEN_IDENTIFIERS.entrySet()) {
          if (identifier.getValue().matcher(codeLine).find()) {
            found.add(new Violation(file, index + 1, RULE_IDENTIFIER + ":" + identifier.getKey(), snippetOfLine(sourceLines[index], codeLine)));
          }
        }
      }
    }

    Matcher helper = SELECTOR_HELPER.matcher(code);
    while (helper.find()) {
      scanHelperCall(file, source, code, lineStarts, helper, found);
    }
  }

  private void scanHelperCall(String file, String source, String code, int[] lineStarts, Matcher helper, List<Violation> found) {
    int open = helper.end() - 1;
    int close = closingParenthesis(code, open);
    if (close < 0) {
      return;
    }
    List<int[]> arguments = splitArguments(code, open + 1, close);
    int selectorIndex = selectorArgument(helper.group(1), arguments.size());
    if (selectorIndex < 0 || selectorIndex >= arguments.size()) {
      return;
    }
    int[] argument = arguments.get(selectorIndex);
    boolean always = "waitForText".equals(helper.group(1));
    for (String literal : topLevelLiterals(source, code, argument[0], argument[1])) {
      if (always || looksLikeSelector(literal)) {
        String snippet = source.substring(helper.start(), close + 1).replaceAll("\\s+", " ");
        found.add(new Violation(file, lineOf(lineStarts, helper.start()), RULE_SELECTOR, snippet));
        return;
      }
    }
  }

  /**
   * Position of the argument that holds the raw selector, or -1 when the call does not take one
   */
  private static int selectorArgument(String helper, int arguments) {
    switch (helper) {
      case "checkLogin":
        return arguments == 4 ? 2 : -1;
      case "checkLogout":
        return arguments == 2 ? 0 : -1;
      case "waitForText":
        return arguments == 2 ? 0 : -1;
      default:
        return 0;
    }
  }

  private static boolean looksLikeSelector(String literal) {
    String text = literal.trim();
    if (text.isEmpty()) {
      return false;
    }
    for (char character : text.toCharArray()) {
      if (SELECTOR_CHARACTERS.indexOf(character) >= 0 || Character.isWhitespace(character)) {
        return true;
      }
    }
    return false;
  }

  private static List<String> topLevelLiterals(String source, String code, int from, int to) {
    List<String> literals = new ArrayList<>();
    int depth = 0;
    int index = from;
    while (index < to) {
      char character = code.charAt(index);
      if (character == '(' || character == '[' || character == '{') {
        depth++;
        index++;
      } else if (character == ')' || character == ']' || character == '}') {
        depth--;
        index++;
      } else if (character == '"') {
        boolean block = code.startsWith("\"\"\"", index);
        int length = block ? 3 : 1;
        int end = code.indexOf(block ? "\"\"\"" : "\"", index + length);
        if (end < 0) {
          break;
        }
        if (depth == 0) {
          literals.add(source.substring(index + length, end));
        }
        index = end + length;
      } else {
        index++;
      }
    }
    return literals;
  }

  private static List<int[]> splitArguments(String code, int from, int to) {
    List<int[]> arguments = new ArrayList<>();
    if (code.substring(from, to).isBlank()) {
      return arguments;
    }
    int depth = 0;
    int start = from;
    for (int index = from; index < to; index++) {
      char character = code.charAt(index);
      if (character == '(' || character == '[' || character == '{') {
        depth++;
      } else if (character == ')' || character == ']' || character == '}') {
        depth--;
      } else if (character == ',' && depth == 0) {
        arguments.add(new int[]{start, index});
        start = index + 1;
      }
    }
    arguments.add(new int[]{start, to});
    return arguments;
  }

  private static int closingParenthesis(String code, int open) {
    int depth = 0;
    for (int index = open; index < code.length(); index++) {
      char character = code.charAt(index);
      if (character == '(') {
        depth++;
      } else if (character == ')') {
        depth--;
        if (depth == 0) {
          return index;
        }
      }
    }
    return -1;
  }

  private static String snippetOfLine(String sourceLine, String codeLine) {
    String code = codeLine.stripTrailing();
    return sourceLine.substring(0, Math.min(code.length(), sourceLine.length())).strip();
  }

  private static int[] lineStarts(String source) {
    List<Integer> starts = new ArrayList<>();
    starts.add(0);
    for (int index = 0; index < source.length(); index++) {
      if (source.charAt(index) == '\n') {
        starts.add(index + 1);
      }
    }
    return starts.stream().mapToInt(Integer::intValue).toArray();
  }

  private static int lineOf(int[] lineStarts, int offset) {
    int low = 0;
    int high = lineStarts.length - 1;
    while (low < high) {
      int middle = (low + high + 1) >>> 1;
      if (lineStarts[middle] <= offset) {
        low = middle;
      } else {
        high = middle - 1;
      }
    }
    return low + 1;
  }

  /**
   * Blank the comments and the content of the string and character literals (the delimiters and the line breaks are
   * kept), so the offsets of the result are the ones of the source
   */
  private static String mask(String source) {
    char[] masked = source.toCharArray();
    int length = masked.length;
    int index = 0;
    while (index < length) {
      char character = source.charAt(index);
      if (character == '/' && index + 1 < length && source.charAt(index + 1) == '/') {
        while (index < length && source.charAt(index) != '\n') {
          blank(masked, index++);
        }
      } else if (character == '/' && index + 1 < length && source.charAt(index + 1) == '*') {
        blank(masked, index++);
        blank(masked, index++);
        while (index < length && !(source.charAt(index) == '*' && index + 1 < length && source.charAt(index + 1) == '/')) {
          blank(masked, index++);
        }
        for (int closing = 0; closing < 2 && index < length; closing++) {
          blank(masked, index++);
        }
      } else if (character == '"' && source.startsWith("\"\"\"", index)) {
        index += 3;
        while (index < length && !source.startsWith("\"\"\"", index)) {
          if (source.charAt(index) == '\\') {
            blank(masked, index++);
          }
          if (index < length) {
            blank(masked, index++);
          }
        }
        index += 3;
      } else if (character == '"' || character == '\'') {
        index = maskDelimited(source, masked, index, character);
      } else {
        index++;
      }
    }
    return new String(masked);
  }

  private static int maskDelimited(String source, char[] masked, int opening, char delimiter) {
    int index = opening + 1;
    while (index < source.length() && source.charAt(index) != delimiter && source.charAt(index) != '\n') {
      if (source.charAt(index) == '\\') {
        blank(masked, index++);
      }
      if (index < source.length() && source.charAt(index) != '\n') {
        blank(masked, index++);
      }
    }
    return index + 1;
  }

  private static void blank(char[] masked, int index) {
    if (masked[index] != '\n' && masked[index] != '\r') {
      masked[index] = ' ';
    }
  }

  private static PathMatcher fileMatcher(String glob) {
    return FileSystems.getDefault().getPathMatcher("glob:" + glob);
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }

  /**
   * A violation of the rules
   *
   * @param file    Path of the file relative to the scanned directory, with {@code /} separators
   * @param line    Line number, starting at 1
   * @param rule    Rule name ({@value #RULE_IMPORT}, {@value #RULE_IDENTIFIER}{@code :<identifier>} or {@value #RULE_SELECTOR})
   * @param snippet Offending line, or the offending call for a selector literal; this is what an allowance must match
   */
  public record Violation(String file, int line, String rule, String snippet) {
    @Override
    public String toString() {
      return file + ":" + line + " [" + rule + "] " + snippet;
    }

    private String baselineEntry() {
      return file + ":" + snippet;
    }
  }

  /**
   * A justified violation
   *
   * @param file    File name, or the end of its relative path
   * @param snippet Snippet of the violation
   * @param reason  Why it is acceptable
   * @param times   Occurrences allowed ({@link Integer#MAX_VALUE} for any number of them)
   */
  public record Allowance(String file, String snippet, String reason, int times) {
    private boolean matches(Violation violation) {
      return snippet.equals(violation.snippet()) && (violation.file().equals(file) || violation.file().endsWith("/" + file));
    }

    @Override
    public String toString() {
      return file + ": " + snippet + " (" + reason + ")";
    }
  }

  /**
   * Result of a scan
   */
  public static final class Report {
    private final List<Violation> violations;
    private final List<Allowance> staleAllowances;
    private final List<Integer> staleMatches;

    private Report(List<Violation> violations, List<Allowance> staleAllowances, List<Integer> staleMatches) {
      this.violations = List.copyOf(violations);
      this.staleAllowances = List.copyOf(staleAllowances);
      this.staleMatches = List.copyOf(staleMatches);
    }

    /**
     * @return Violations that no allowance excuses
     */
    public List<Violation> violations() {
      return violations;
    }

    /**
     * @return Allowances that match fewer occurrences than they declare, to be removed or shrunk
     */
    public List<Allowance> staleAllowances() {
      return staleAllowances;
    }

    /**
     * @return True when there are no violations and no stale allowances
     */
    public boolean isClean() {
      return violations.isEmpty() && staleAllowances.isEmpty();
    }

    /**
     * Violations as baseline lines (see {@link BrowserTestSourceGuard#allowBaseline(Path, String)}), sorted by file and line
     *
     * @return Baseline text
     */
    public String toBaseline() {
      return violations.stream().map(Violation::baselineEntry).collect(Collectors.joining("\n", "", violations.isEmpty() ? "" : "\n"));
    }

    /**
     * @return Human-readable description of what has to be fixed, empty when the report is clean
     */
    public String describe() {
      StringBuilder description = new StringBuilder();
      if (!violations.isEmpty()) {
        description.append("Browser tests must only express screen steps, without selectors or automation-tool types. ")
          .append("Use the semantic steps of SeleniumUtilities, or allow the violation with a reason:\n");
        violations.forEach(violation -> description.append("  ").append(violation).append('\n'));
      }
      if (!staleAllowances.isEmpty()) {
        description.append("Shrink the baseline or the allowlist, these allowed violations do not exist (any more):\n");
        for (int index = 0; index < staleAllowances.size(); index++) {
          Allowance allowance = staleAllowances.get(index);
          description.append("  ").append(allowance).append(" - found ").append(staleMatches.get(index));
          if (allowance.times() != Integer.MAX_VALUE) {
            description.append(" of ").append(allowance.times());
          }
          description.append('\n');
        }
      }
      return description.toString();
    }
  }
}
