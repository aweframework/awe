package com.almis.awe.test.selenium;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Keeps the React browser jobs of {@code .gitlab-ci.yml} honest: they run the IT classes listed in {@code TEST_CLASSES}
 * ({@code -Dit.test=}), and a name that is misspelled or that no longer exists would make the job run fewer tests (or none)
 * without failing. Every class listed there must have its source in this application.
 *
 * <p>Only the React jobs (the ones that extend {@code .react-testing}) list classes today: the AngularJS jobs select by tag
 * ({@code TEST_TAGS}). A job that lists {@code TEST_CLASSES} without being a React job fails the check with its own
 * message, so it is not mistaken for a missing class.</p>
 *
 * <p>It is a unit test over the sources, so it runs with the unit tests and does not need a browser. When the module is
 * built on its own and the pipeline file is not reachable, the test is skipped with an explicit message.</p>
 */
// The surefire run of this module (All UT) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class CiBrowserClassesGuardTest {

  private static final Path BASEDIR = Path.of(System.getProperty("basedir", System.getProperty("user.dir"))).toAbsolutePath();
  private static final Path PIPELINE = BASEDIR.resolve("../../.gitlab-ci.yml").normalize();
  private static final Path SELENIUM_TESTS = BASEDIR.resolve("src/test/java/com/almis/awe/test/selenium");
  private static final String REACT_TEMPLATE = ".react-testing";
  private static final String BROWSER_TEMPLATE = ".browser-testing";
  private static final String QUARANTINE_TEMPLATE = ".quarantine-testing";
  private static final String QUARANTINE_JOBS = "Quarantine ";
  private static final Path ANGULAR_TESTS = BASEDIR.resolve("../awe-boot/src/test/java/com/almis/awe/test/selenium").normalize();
  private static final String CHAIN_RETRY_TEMPLATE = ".chain-retry";
  private static final String SONAR_JOB = "Launch Sonar";
  private static final Pattern BROWSER_JOB_NAME = Pattern.compile("(Selenium|Playwright) IT (\\d++)/(\\d++)");
  private static final Pattern TAG = Pattern.compile("@Tag\\(\"([^\"]+)\"\\)");
  private static final Pattern TEST_TAGS = Pattern.compile("^[ \\t]*+(?:-[ \\t]*+)?TEST_TAGS:[ \\t]*+([^\\s#]++)", Pattern.MULTILINE);
  private static final Pattern EMPTY_EXCLUDED_GROUPS = Pattern.compile("-Dit\\.excluded-groups=(?:\\s|$)");
  private static final Pattern EXTENDS_KEY = Pattern.compile("^ {2}extends:[ \\t]*+(.*+)$");
  private static final Pattern LIST_ITEM = Pattern.compile("^ {4}-[ \\t]++(\\S.*+)$");
  private static final Pattern QUARANTINE_GREP = Pattern.compile("grep -rqE --include='([^']++)' \"\\$QUARANTINE_ANNOTATION\"");
  private static final Pattern MATRIX_ENTRY = Pattern.compile("^[ \\t]*+-[ \\t]*+(\\w++):", Pattern.MULTILINE);
  private static final Pattern QUARANTINE_ANNOTATION = Pattern.compile("QUARANTINE_ANNOTATION:[ \\t]*+'([^']++)'");
  private static final Pattern JOB =Pattern.compile("^([^\\s#][^\\n]*):\\s*$", Pattern.MULTILINE);
  private static final Pattern TEST_CLASSES = Pattern.compile("^[ \\t]*+(?:-[ \\t]*+)?TEST_CLASSES:[ \\t]*+([^\\s#]++)", Pattern.MULTILINE);

  @Test
  void shouldListClassesOnlyInReactJobs() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI classes check skipped");

    Map<String, String> jobsWithClasses = jobs(Files.readString(PIPELINE)).entrySet().stream()
      .filter(job -> TEST_CLASSES.matcher(job.getValue()).find())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    assertThat(jobsWithClasses).as("Jobs of %s that list TEST_CLASSES", PIPELINE).isNotEmpty();

    List<String> notReact = jobsWithClasses.entrySet().stream()
      .filter(job -> !extendsOf(job.getValue()).contains(REACT_TEMPLATE))
      .map(Map.Entry::getKey)
      .toList();
    assertThat(notReact)
      .as("Jobs that list TEST_CLASSES without extending %s: only the React jobs run classes of this application,"
        + " the AngularJS jobs select by tag. Extend the template or extend this guard", REACT_TEMPLATE)
      .isEmpty();
  }

  @Test
  void shouldHaveASourceForEveryClassThatTheReactBrowserJobsRun() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI classes check skipped");

    Set<String> listed = listedClasses(Files.readString(PIPELINE));
    assertThat(listed).as("Classes listed in the TEST_CLASSES of %s", PIPELINE).isNotEmpty();

    List<String> missing = listed.stream()
      .filter(name -> !Files.isRegularFile(SELENIUM_TESTS.resolve(name + ".java")))
      .toList();
    assertThat(missing)
      .as("Classes of TEST_CLASSES without a source in %s (a misspelled or removed class is silently not run)", SELENIUM_TESTS)
      .isEmpty();
  }

  @Test
  void shouldKeepTheQuarantineJobsFromBlockingThePipeline() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI quarantine check skipped");

    String pipeline = Files.readString(PIPELINE);

    assertThat(quarantineJobs(pipeline)).as("Quarantine jobs of %s", PIPELINE).isNotEmpty();
    assertThat(quarantineProblems(pipeline)).isEmpty();
  }

  @Test
  void shouldRetryTheBrowserJobsOnlyWhenTheRunnerFails() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI retry check skipped");

    String pipeline = Files.readString(PIPELINE);

    assertThat(retriesOnTestFailure(pipeline))
      .as("%s retries the whole job when a test fails: a flaky test is rerun alone by failsafe (it.rerun-count), and a job"
        + " retry would run every test again and hide the flaky ones", BROWSER_TEMPLATE)
      .isFalse();
    assertThat(jobs(pipeline).get(BROWSER_TEMPLATE)).contains("runner_system_failure");
  }

  @Test
  void shouldDetectAJobRetryOnTestFailure() {
    String retrying = """
      .browser-testing:
        retry:
          max: 1
          when:
            - script_failure
            - runner_system_failure
      """;
    String runnerOnly = """
      .browser-testing:
        # script_failure is not retried
        retry:
          max: 1
          when:
            - runner_system_failure
      """;

    assertThat(retriesOnTestFailure(retrying)).isTrue();
    assertThat(retriesOnTestFailure(runnerOnly)).isFalse();
  }

  @Test
  void shouldReportQuarantineJobsThatCouldBlockThePipeline() {
    String pipeline = """
      .quarantine-testing:
        retry: 0
      Quarantine Selenium IT:
        extends:
          - .chrome-testing
          - .quarantine-testing
      Quarantine Playwright IT:
        extends: .playwright-chromium-testing
        allow_failure: false
      """;

    assertThat(quarantineProblems(pipeline)).hasSize(3)
      .anyMatch(problem -> problem.contains(QUARANTINE_TEMPLATE) && problem.contains("allow_failure: true"))
      .anyMatch(problem -> problem.contains("Quarantine Playwright IT") && problem.contains(QUARANTINE_TEMPLATE))
      .anyMatch(problem -> problem.contains("Quarantine Playwright IT") && problem.contains("allow_failure"));
  }

  @Test
  void shouldRetryTheWholeJobOnlyWhereADependentChainRuns() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI chain retry check skipped");
    assumeTrue(Files.isDirectory(ANGULAR_TESTS), ANGULAR_TESTS + " is not reachable from this module: CI chain retry check skipped");

    String pipeline = Files.readString(PIPELINE);

    assertThat(chainRetryProblems(pipeline, chainTags(ANGULAR_TESTS), chainClasses(SELENIUM_TESTS)))
      .as("The jobs that run a @DependentChain class retry the whole job on a test failure (a chain is never rerun test by"
        + " test), the rest only when the runner fails")
      .isEmpty();
  }

  @Test
  void shouldReportAChainJobWithoutRetryAndAJobWithRetryWithoutChain() {
    String pipeline = """
      .chain-retry:
        retry:
          max: 1
          when:
            - script_failure
            - runner_system_failure
      Chrome IT:
        extends:
          - .chrome-testing
        parallel:
          matrix:
            - TEST_TAGS: CrudIT
      Firefox IT:
        extends:
          - .firefox-testing
          - .chain-retry
        parallel:
          matrix:
            - TEST_TAGS: PlainIT
      Chrome IT React:
        extends:
          - .chrome-testing
          - .chain-retry
          - .react-testing
        parallel:
          matrix:
            - TEST_CLASSES: CRUDTestsIT,OtherIT
      """;

    assertThat(chainRetryProblems(pipeline, Set.of("CrudIT"), Set.of("CRUDTestsIT"))).hasSize(2)
      .anyMatch(problem -> problem.contains("Chrome IT:") && problem.contains("must extend"))
      .anyMatch(problem -> problem.contains("Firefox IT") && problem.contains("must not extend"));
    assertThat(chainRetryProblems(".chain-retry:\n  retry: 1\n", Set.of(), Set.of()))
      .singleElement().asString().contains(CHAIN_RETRY_TEMPLATE).contains("script_failure");
  }

  @Test
  void shouldReadTheTemplatesThatAJobExtendsFromItsOwnKey() {
    assertThat(extendsOf("  extends: .a")).containsExactly(".a");
    assertThat(extendsOf("  extends: [.a, .b]")).containsExactly(".a", ".b");
    assertThat(extendsOf("  extends:\n    - .a\n    - .b # not .chain-retry\n  rules: *rules")).containsExactly(".a", ".b");
    assertThat(extendsOf("  variables:\n    NOTE: .chain-retry\n  extends:\n    - .a")).containsExactly(".a");
  }

  @Test
  void shouldNotTakeACommentOrAnotherKeyForTheRetryTemplate() {
    String pipeline = """
      .chain-retry:
        retry:
          max: 1
          when:
            - script_failure
            - runner_system_failure
      Chrome IT:
        extends:
          - .chrome-testing # .chain-retry would be wrong here
        variables:
          NOTE: .chain-retry
        parallel:
          matrix:
            - TEST_TAGS: CrudIT
      Firefox IT:
        extends:
          - .firefox-testing
          - .chain-retry
        parallel:
          matrix:
            - TEST_TAGS: PlainIT
      """;

    assertThat(chainRetryProblems(pipeline, Set.of("CrudIT"), Set.of())).hasSize(2)
      .anyMatch(problem -> problem.contains("Chrome IT") && problem.contains("must extend"))
      .anyMatch(problem -> problem.contains("Firefox IT") && problem.contains("must not extend"));
  }

  @Test
  void shouldRunTheQuarantineGroupThroughTheSelectionOfTheBrowserJobs() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI quarantine check skipped");

    String pipeline = Files.readString(PIPELINE);

    assertThat(quarantineSelectionProblems(pipeline)).isEmpty();
    for (Path pom : List.of(BASEDIR.resolve("pom.xml"), BASEDIR.resolve("../awe-boot/pom.xml").normalize())) {
      assumeTrue(Files.isRegularFile(pom), pom + " is not reachable from this module: pom check skipped");
      assertThat(Files.readString(pom)).as("%s defines the properties that the quarantine job sets", pom)
        .contains("<it.excluded-groups>").contains("<it.rerun-count>");
    }
  }

  @Test
  void shouldReportAQuarantineJobThatDoesNotSelectTheQuarantineGroup() {
    String pipeline = """
      .browser-testing:
        script:
          - mvn verify -Dawe.test.browser=${BROWSER}
      .quarantine-testing:
        allow_failure: true
        variables:
          IT_SELECTION: -Dgroups=quarantine -Dit.excluded-groups=quarantine
        script:
          - mvn verify
      """;

    assertThat(quarantineSelectionProblems(pipeline)).hasSize(4)
      .anyMatch(problem -> problem.contains(BROWSER_TEMPLATE) && problem.contains("IT_SELECTION"))
      .anyMatch(problem -> problem.contains("-Dit.excluded-groups="))
      .anyMatch(problem -> problem.contains("-Dit.rerun-count=0"))
      .anyMatch(problem -> problem.contains("!reference"));
  }

  @Test
  void shouldDetectEveryFormOfTheQuarantineAnnotationInTheSources(@TempDir Path sources) throws IOException, InterruptedException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI quarantine check skipped");
    assumeTrue(grepExists(), "grep is not available: quarantine detection check skipped");

    String[] command = quarantineSearch(Files.readString(PIPELINE));
    Map<String, String> forms = Map.of(
      "OwnLineIT.java", "  @Quarantine(issue = \"#1\", reason = \"r\")\n  void test() {}\n",
      "WithOtherAnnotationsIT.java", "  @Test @Quarantine(issue = \"#1\", reason = \"r\") void test() {}\n",
      "SpaceBeforeParenthesisIT.java", "  @Quarantine (issue = \"#1\", reason = \"r\")\n  void test() {}\n",
      "QualifiedIT.java", "  @com.almis.awe.testing.annotations.Quarantine(issue = \"#1\", reason = \"r\")\n  void test() {}\n",
      "ParenthesisOnTheNextLineIT.java", "  @Quarantine\n  (issue = \"#1\", reason = \"r\")\n  void test() {}\n");

    for (Map.Entry<String, String> form : forms.entrySet()) {
      Path directory = Files.createDirectory(sources.resolve(form.getKey().replace(".java", "")));
      Files.writeString(directory.resolve(form.getKey()), form.getValue());
      assertThat(grep(command, directory)).as("The form in %s is detected", form.getKey()).isTrue();
    }
    Path none = Files.createDirectory(sources.resolve("none"));
    Files.writeString(none.resolve("PlainIT.java"), "  @Test\n  void test() {}\n");
    assertThat(grep(command, none)).as("A source without the annotation is not detected").isFalse();
  }

  @Test
  void shouldNotStartTheQuarantineJobsBecauseOfTheClassesThatMentionTheAnnotation(@TempDir Path sources)
    throws IOException, InterruptedException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI quarantine check skipped");
    assumeTrue(grepExists(), "grep is not available: quarantine detection check skipped");
    String[] command = quarantineSearch(Files.readString(PIPELINE));

    // This very class, the guard of the declarations and a helper mention the annotation in strings and comments
    Path guard = BASEDIR.resolve("src/test/java/com/almis/awe/test/selenium/CiBrowserClassesGuardTest.java");
    assumeTrue(Files.isRegularFile(guard), guard + " is not reachable: quarantine detection check skipped");
    Files.copy(guard, sources.resolve("CiBrowserClassesGuardTest.java"));
    Files.writeString(sources.resolve("AbstractHelper.java"), "// the browser tests use @Quarantine(issue = \"#1\", reason = \"r\")\n");

    assertThat(Files.readString(sources.resolve("CiBrowserClassesGuardTest.java"))).contains("@Quarantine");
    assertThat(grep(command, sources)).as("Only the browser test classes (*IT.java) are searched").isFalse();
  }

  @Test
  void shouldNameTheBrowserJobsSoThatEachToolIsOneGroupAndGateSonarWithEveryOne() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI job names check skipped");

    String pipeline = Files.readString(PIPELINE);

    assertThat(browserJobs(pipeline)).as("Browser jobs of %s", PIPELINE).hasSize(8);
    assertThat(browserJobProblems(pipeline)).isEmpty();
  }

  @Test
  void shouldReportBrowserJobsThatDoNotGroupOrDoNotGateSonar() {
    String pipeline = """
      Selenium IT 1/2:
        extends: .chrome-testing
        parallel:
          matrix:
            - TARGET: chrome-angularjs
              TEST_TAGS: A
      Chrome IT:
        extends: .firefox-testing
        parallel:
          matrix:
            - TARGET: chrome-angularjs
              TEST_TAGS: B
      Playwright IT 1/1:
        extends: .playwright-chromium-testing
        parallel:
          matrix:
            - TARGET: chrome-angularjs
              TEST_TAGS: C
      Launch Sonar:
        needs:
          - job: Selenium IT 1/2
      """;

    assertThat(browserJobProblems(pipeline)).hasSize(3)
      .anyMatch(problem -> problem.contains("Chrome IT") && problem.contains("must be named"))
      .anyMatch(problem -> problem.contains("Selenium IT") && problem.contains("1/2"))
      .anyMatch(problem -> problem.contains("Playwright IT 1/1") && problem.contains(SONAR_JOB));
  }

  @Test
  void shouldReportAMatrixEntryThatDoesNotStartWithTheTarget() {
    String pipeline = """
      Selenium IT 1/1:
        extends: .chrome-testing
        parallel:
          matrix:
            - TARGET: chrome-angularjs
              TEST_TAGS: A
            - TEST_TAGS: B
      Launch Sonar:
        needs:
          - job: Selenium IT 1/1
      """;

    assertThat(browserJobProblems(pipeline)).singleElement().asString()
      .contains("Selenium IT 1/1").contains("TARGET");
  }

  @Test
  void shouldReadTheClassesOfEveryMatrixEntryWithoutQuotes() {
    String pipeline = """
      parallel:
        matrix:
          - TEST_CLASSES: ApplicationSettingsTestsIT,IntegrationTestsIT
            TEST_PORT: "6309"
          - TEST_CLASSES: SchedulerTestsIT # comment
          - TEST_CLASSES: "QuotedTestsIT,'SingleQuotedTestsIT'"
          - TEST_CLASSES: 'OtherQuotedTestsIT'
      """;

    assertThat(listedClasses(pipeline)).containsExactlyInAnyOrder("ApplicationSettingsTestsIT", "IntegrationTestsIT",
      "SchedulerTestsIT", "QuotedTestsIT", "SingleQuotedTestsIT", "OtherQuotedTestsIT");
  }

  @Test
  void shouldSplitThePipelineInJobs() {
    String pipeline = """
      # comment
      Chrome IT:
        extends:
          - .chrome-testing
        variables:
          TEST_TAGS: A
      Chrome IT React:
        extends:
          - .react-testing
        parallel:
          matrix:
            - TEST_CLASSES: AIT
      # the comment of the next job mentions .react-testing
      Other IT:
        variables:
          TEST_TAGS: B
      """;

    Map<String, String> jobs = jobs(pipeline);

    assertThat(jobs).containsOnlyKeys("Chrome IT", "Chrome IT React", "Other IT");
    assertThat(jobs.get("Chrome IT React")).contains("TEST_CLASSES").contains(REACT_TEMPLATE);
    assertThat(jobs.get("Chrome IT")).doesNotContain("TEST_CLASSES");
    assertThat(jobs.get("Chrome IT React")).doesNotContain("comment");
  }

  /**
   * The browser suites of the pipeline: the jobs that select tests with TEST_TAGS or TEST_CLASSES, except the quarantine jobs
   */
  private static Map<String, String> browserJobs(String pipeline) {
    return jobs(pipeline).entrySet().stream()
      .filter(job -> !job.getKey().startsWith(QUARANTINE_JOBS))
      .filter(job -> TEST_TAGS.matcher(job.getValue()).find() || TEST_CLASSES.matcher(job.getValue()).find())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
  }

  /**
   * GitLab shows jobs whose names end in a number over a total, or in the variables of a matrix, as one group named by the rest
   * of the name. The browser jobs of a tool are named "Selenium IT n/m" or "Playwright IT n/m" (n from 1 to m, m the number of
   * jobs of the tool), so each tool is one group in the pipeline graph, and "Launch Sonar" waits for each one of them.
   */
  private static List<String> browserJobProblems(String pipeline) {
    List<String> problems = new ArrayList<>();
    Map<String, String> jobs = jobs(pipeline);
    String sonar = jobs.getOrDefault(SONAR_JOB, "");
    Map<String, List<Matcher>> byTool = new LinkedHashMap<>();
    browserJobs(pipeline).forEach((name, job) -> {
      Matcher entries = MATRIX_ENTRY.matcher(job);
      while (entries.find()) {
        if (!"TARGET".equals(entries.group(1))) {
          problems.add(name + ": every matrix entry must start with TARGET (browser and engine), so that a failed job reads \"" + name + ": [chromium-angularjs, ...]\"");
          break;
        }
      }
      String tool = job.contains("playwright") ? "Playwright" : "Selenium";
      Matcher matcher = BROWSER_JOB_NAME.matcher(name);
      if (!matcher.matches() || !matcher.group(1).equals(tool)) {
        problems.add(name + " must be named \"" + tool + " IT n/m\", so that the " + tool + " jobs are one group of the pipeline graph");
        return;
      }
      byTool.computeIfAbsent(tool, key -> new ArrayList<>()).add(matcher);
      if (!sonar.contains("- job: " + name)) {
        problems.add(name + " must be listed in the needs of " + SONAR_JOB + ": it gates Sonar and the release jobs");
      }
    });
    byTool.forEach((tool, names) -> {
      Set<String> numbers = names.stream().map(matcher -> matcher.group(2)).collect(Collectors.toCollection(TreeSet::new));
      Set<String> totals = names.stream().map(matcher -> matcher.group(3)).collect(Collectors.toCollection(TreeSet::new));
      Set<String> expected = IntStream.rangeClosed(1, names.size()).mapToObj(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
      if (!numbers.equals(expected) || !totals.equals(Set.of(String.valueOf(names.size())))) {
        problems.add(tool + " IT jobs must be numbered 1/" + names.size() + " to " + names.size() + "/" + names.size()
          + " (found " + names.stream().map(matcher -> matcher.group(0)).toList() + ")");
      }
    });
    return problems;
  }

  /**
   * A test failure is a script failure, so a job retry on script_failure (or on any failure) runs the whole job again
   */
  private static boolean retriesOnTestFailure(String pipeline) {
    String template = jobs(pipeline).getOrDefault(BROWSER_TEMPLATE, "");
    return template.contains("script_failure") || template.contains("- always");
  }

  /**
   * The templates that a job extends, read from its own "extends" key: a single name, a flow list or a block list (a comment
   * that mentions a template, or another key that does, does not count)
   */
  private static List<String> extendsOf(String job) {
    List<String> templates = new ArrayList<>();
    List<String> lines = job.lines().toList();
    for (int index = 0; index < lines.size(); index++) {
      Matcher key = EXTENDS_KEY.matcher(lines.get(index));
      if (!key.matches()) {
        continue;
      }
      String inline = withoutTrailingComment(key.group(1)).replaceAll("[\\[\\]]", "");
      for (String name : inline.split(",")) {
        if (!name.isBlank()) {
          templates.add(name.trim());
        }
      }
      for (int next = index + 1; next < lines.size(); next++) {
        Matcher item = LIST_ITEM.matcher(lines.get(next));
        if (!item.matches()) {
          break;
        }
        templates.add(withoutTrailingComment(item.group(1)).trim());
      }
    }
    return templates;
  }

  private static String withoutTrailingComment(String value) {
    int comment = value.indexOf(" #");
    return comment < 0 ? value : value.substring(0, comment);
  }

  /**
   * The jobs that run the quarantined tests
   */
  private static Map<String, String> quarantineJobs(String pipeline) {
    return jobs(pipeline).entrySet().stream()
      .filter(job -> job.getKey().startsWith(QUARANTINE_JOBS))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
  }

  /**
   * The quarantine jobs run tests that fail by definition, so they must never block the pipeline: the shared template
   * sets allow_failure and no job of the family may skip the template or set it back
   */
  private static List<String> quarantineProblems(String pipeline) {
    List<String> problems = new ArrayList<>();
    String template = jobs(pipeline).get(QUARANTINE_TEMPLATE);
    if (template == null || !template.contains("allow_failure: true")) {
      problems.add(QUARANTINE_TEMPLATE + " must set allow_failure: true so that a quarantined test never blocks the pipeline");
    }
    quarantineJobs(pipeline).forEach((name, job) -> {
      if (!extendsOf(job).contains(QUARANTINE_TEMPLATE)) {
        problems.add(name + " must extend " + QUARANTINE_TEMPLATE);
      }
      if (job.contains("allow_failure: false")) {
        problems.add(name + " sets allow_failure: false: a quarantine job never blocks the pipeline");
      }
    });
    return problems;
  }

  /**
   * The classes of a test application that are a {@code @DependentChain}
   */
  private static Set<String> chainClasses(Path sources) throws IOException {
    try (Stream<Path> files = Files.list(sources)) {
      return files.filter(file -> file.getFileName().toString().endsWith("IT.java"))
        .filter(file -> read(file).contains("@DependentChain("))
        .map(file -> file.getFileName().toString().replace(".java", ""))
        .collect(Collectors.toCollection(TreeSet::new));
    }
  }

  /**
   * The JUnit tags of the {@code @DependentChain} classes of a test application: the jobs of the AngularJS application select
   * their classes by tag
   */
  private static Set<String> chainTags(Path sources) throws IOException {
    Set<String> tags = new TreeSet<>();
    for (String chain : chainClasses(sources)) {
      Matcher matcher = TAG.matcher(read(sources.resolve(chain + ".java")));
      while (matcher.find()) {
        tags.add(matcher.group(1));
      }
    }
    return tags;
  }

  private static String read(Path file) {
    try {
      return Files.readString(file);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  /**
   * Whole-job retry is the way to run a chain again (a chain is never rerun test by test), and GitLab retries a job, not an
   * entry of its matrix: the jobs that run a chain class extend the retry template and the others must not
   */
  private static List<String> chainRetryProblems(String pipeline, Set<String> chainTags, Set<String> chainClasses) {
    List<String> problems = new ArrayList<>();
    Map<String, String> jobs = jobs(pipeline);
    String template = jobs.get(CHAIN_RETRY_TEMPLATE);
    if (template == null || !template.contains("script_failure") || !template.contains("runner_system_failure")) {
      problems.add(CHAIN_RETRY_TEMPLATE + " must retry the job on script_failure and runner_system_failure");
    }
    jobs.forEach((name, job) -> {
      Matcher tags = TEST_TAGS.matcher(job);
      boolean runsChain = listedClasses(job).stream().anyMatch(chainClasses::contains);
      while (tags.find()) {
        runsChain |= chainTags.contains(tags.group(1));
      }
      boolean selects = TEST_CLASSES.matcher(job).find() || TEST_TAGS.matcher(job).find();
      if (selects && !name.startsWith(QUARANTINE_JOBS)) {
        boolean retries = extendsOf(job).contains(CHAIN_RETRY_TEMPLATE);
        if (runsChain && !retries) {
          problems.add(name + ": runs a @DependentChain class, so it must extend " + CHAIN_RETRY_TEMPLATE);
        } else if (!runsChain && retries) {
          problems.add(name + ": runs no @DependentChain class, so it must not extend " + CHAIN_RETRY_TEMPLATE);
        }
      }
    });
    return problems;
  }

  /**
   * The quarantine job selects the quarantined tests through the same command as every browser job: the command consumes
   * IT_SELECTION and the quarantine template sets it, so a template that stops setting it would run the whole suite
   */
  private static List<String> quarantineSelectionProblems(String pipeline) {
    List<String> problems = new ArrayList<>();
    Map<String, String> jobs = jobs(pipeline);
    String browser = jobs.getOrDefault(BROWSER_TEMPLATE, "");
    if (!browser.contains("${IT_SELECTION}") && !browser.contains("$IT_SELECTION")) {
      problems.add(BROWSER_TEMPLATE + " must consume IT_SELECTION in its script: the quarantine job selects its tests with it");
    }
    String quarantine = jobs.getOrDefault(QUARANTINE_TEMPLATE, "");
    if (!quarantine.contains("-Dgroups=quarantine")) {
      problems.add(QUARANTINE_TEMPLATE + " must select the quarantine group with -Dgroups=quarantine");
    }
    if (!EMPTY_EXCLUDED_GROUPS.matcher(quarantine).find()) {
      problems.add(QUARANTINE_TEMPLATE + " must empty the excluded groups with -Dit.excluded-groups=, or it excludes what it runs");
    }
    if (!quarantine.contains("-Dit.rerun-count=0")) {
      problems.add(QUARANTINE_TEMPLATE + " must run without per-test rerun with -Dit.rerun-count=0");
    }
    if (!quarantine.contains("!reference [.browser-testing, script]")) {
      problems.add(QUARANTINE_TEMPLATE + " must run the script of " + BROWSER_TEMPLATE + " with !reference [.browser-testing, script]");
    }
    return problems;
  }

  /**
   * The search that the quarantine job makes in the sources, read from the template that defines it: the grep options and the
   * pattern of QUARANTINE_ANNOTATION (the directory is added by the caller)
   */
  private static String[] quarantineSearch(String pipeline) {
    String template = jobs(pipeline).getOrDefault(QUARANTINE_TEMPLATE, "");
    Matcher pattern = QUARANTINE_ANNOTATION.matcher(template);
    assertThat(pattern.find()).as("%s defines QUARANTINE_ANNOTATION between single quotes", QUARANTINE_TEMPLATE).isTrue();
    Matcher search = QUARANTINE_GREP.matcher(template);
    assertThat(search.find())
      .as("%s searches the sources with grep -rqE --include='*IT.java' \"$QUARANTINE_ANNOTATION\": only the browser test classes", QUARANTINE_TEMPLATE)
      .isTrue();
    return new String[]{"grep", "-rqE", "--include=" + search.group(1), pattern.group(1)};
  }

  private static boolean grepExists() throws InterruptedException {
    try {
      return new ProcessBuilder("grep", "-V").redirectErrorStream(true).start().waitFor() == 0;
    } catch (IOException exception) {
      return false;
    }
  }

  /**
   * The same command as the job: grep succeeds when the pattern is found
   */
  private static boolean grep(String[] command, Path directory) throws IOException, InterruptedException {
    List<String> arguments = new ArrayList<>(List.of(command));
    arguments.add(directory.toString());
    return new ProcessBuilder(arguments).start().waitFor() == 0;
  }

  /**
   * Split the pipeline in its top level blocks: the jobs and the templates
   */
  private static Map<String, String> jobs(String pipeline) {
    Map<String, String> jobs = new LinkedHashMap<>();
    Matcher matcher = JOB.matcher(pipeline);
    String name = null;
    int start = 0;
    while (matcher.find()) {
      if (name != null) {
        jobs.put(name, withoutComments(pipeline.substring(start, matcher.start())));
      }
      name = matcher.group(1);
      start = matcher.end();
    }
    if (name != null) {
      jobs.put(name, withoutComments(pipeline.substring(start)));
    }
    return jobs;
  }

  /**
   * Drop the comment lines: the comments that precede a job mention the templates of the next one
   */
  private static String withoutComments(String block) {
    return block.lines().filter(line -> !line.stripLeading().startsWith("#")).collect(Collectors.joining("\n"));
  }

  private static Set<String> listedClasses(String pipeline) {
    Set<String> classes = new TreeSet<>();
    Matcher matcher = TEST_CLASSES.matcher(pipeline);
    while (matcher.find()) {
      for (String name : matcher.group(1).split(",")) {
        String clean = name.replaceAll("[\"']", "").trim();
        if (!clean.isEmpty()) {
          classes.add(clean);
        }
      }
    }
    return classes;
  }
}
