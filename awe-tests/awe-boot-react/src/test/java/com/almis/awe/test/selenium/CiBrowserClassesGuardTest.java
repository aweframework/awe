package com.almis.awe.test.selenium;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
  private static final Pattern JOB = Pattern.compile("^([^\\s#][^\\n]*):\\s*$", Pattern.MULTILINE);
  private static final Pattern TEST_CLASSES = Pattern.compile("^[ \\t]*+(?:-[ \\t]*+)?TEST_CLASSES:[ \\t]*+([^\\s#]++)", Pattern.MULTILINE);

  @Test
  void shouldListClassesOnlyInReactJobs() throws IOException {
    assumeTrue(Files.isRegularFile(PIPELINE), PIPELINE + " is not reachable from this module: CI classes check skipped");

    Map<String, String> jobsWithClasses = jobs(Files.readString(PIPELINE)).entrySet().stream()
      .filter(job -> TEST_CLASSES.matcher(job.getValue()).find())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    assertThat(jobsWithClasses).as("Jobs of %s that list TEST_CLASSES", PIPELINE).isNotEmpty();

    List<String> notReact = jobsWithClasses.entrySet().stream()
      .filter(job -> !job.getValue().contains(REACT_TEMPLATE))
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
