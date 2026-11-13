package com.almis.awe.testing.guard;

import com.almis.awe.testing.annotations.DependentChain;
import com.almis.awe.testing.annotations.Quarantine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps the declarations that organise the browser tests honest, so a test cannot be quarantined without a trace:
 * <ul>
 *   <li>{@link Quarantine} needs a reference to the issue that tracks the flakiness (its number, {@code #766}, or its URL)
 *   and a reason.</li>
 *   <li>The {@value Quarantine#TAG} tag cannot be given by hand with {@code @Tag}: the blocking jobs exclude the tag, so a
 *   hand-written one would hide a test without issue and without reason.</li>
 *   <li>{@link DependentChain} needs a reason and the {@code @TestMethodOrder} that makes it a chain, the
 *   {@value DependentChain#TAG} tag cannot be given by hand either (the build runs the tagged classes without the per-test
 *   rerun), and a step of a chain cannot be quarantined alone: the chain cannot run with a step missing.</li>
 * </ul>
 *
 * <p>The guard reads the classes that are already compiled, so it works on the declarations that JUnit sees. A test
 * application runs it in a unit test over its browser test package; a product can do the same:</p>
 *
 * <pre>{@code
 * List<String> violations = BrowserTestDeclarationsGuard.scan(Path.of("target/test-classes"), "com.acme.test.selenium");
 * assertThat(violations).isEmpty();
 * }</pre>
 */
public final class BrowserTestDeclarationsGuard {

  /**
   * An issue number such as {@code #766}
   */
  private static final Pattern ISSUE_NUMBER = Pattern.compile("#\\d++");
  /**
   * What precedes {@code /issues/<number>} in the URL of an issue of a GitLab or GitHub project: the scheme, the host and
   * the path of the project, without spaces
   */
  private static final Pattern ISSUE_URL_PREFIX = Pattern.compile("https?://\\S++");
  private static final String ISSUES_PATH = "/issues/";
  private static final String TEST_CLASS_SUFFIX = "IT";

  private BrowserTestDeclarationsGuard() {
  }

  /**
   * Check the browser test classes (the {@code *IT} classes) of a package, including its subpackages
   *
   * @param classesDirectory Directory with the compiled classes (the root of the packages, e.g. {@code target/test-classes})
   * @param packageName      Package to scan
   * @return Violations found, empty when the declarations are right
   * @throws IOException When the package cannot be read, which also happens when it does not exist: a renamed package
   *                     would otherwise leave the guard checking nothing
   */
  public static List<String> scan(Path classesDirectory, String packageName) throws IOException {
    Path packageDirectory = classesDirectory.resolve(packageName.replace('.', '/'));
    if (!Files.isDirectory(packageDirectory)) {
      throw new IOException("Package " + packageName + " has no compiled classes in " + classesDirectory);
    }

    List<Class<?>> classes = new ArrayList<>();
    try (Stream<Path> files = Files.walk(packageDirectory)) {
      for (Path file : files.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList()) {
        String name = classesDirectory.relativize(file).toString().replace('/', '.').replace('\\', '.');
        if (name.endsWith(TEST_CLASS_SUFFIX + ".class")) {
          classes.add(load(name.substring(0, name.length() - ".class".length())));
        }
      }
    }
    return violations(classes.toArray(new Class<?>[0]));
  }

  /**
   * Check the declarations of some classes and of their own methods
   *
   * @param classes Classes to check
   * @return Violations found, empty when the declarations are right
   */
  public static List<String> violations(Class<?>... classes) {
    List<String> violations = new ArrayList<>();
    for (Class<?> testClass : classes) {
      checkQuarantine(testClass, testClass.getName(), violations);
      checkChain(testClass, violations);
      Arrays.stream(testClass.getDeclaredMethods())
        .sorted(Comparator.comparing(Method::getName))
        .forEach(method -> {
          String where = testClass.getName() + "#" + method.getName();
          checkQuarantine(method, where, violations);
          if (method.isAnnotationPresent(Quarantine.class) && testClass.isAnnotationPresent(DependentChain.class)) {
            violations.add(where + " is a step of a @DependentChain: the chain cannot run with a step missing, so quarantine"
              + " the whole class");
          }
        });
    }
    return violations;
  }

  /**
   * Check if a text is a reference to an issue
   *
   * @param text Text to check
   * @return The text is an issue number ({@code #766}) or the URL of an issue
   */
  public static boolean isIssueReference(String text) {
    if (text == null) {
      return false;
    }
    if (ISSUE_NUMBER.matcher(text).matches()) {
      return true;
    }
    // The URL of an issue ends in /issues/<number>; the prefix is read apart so that no pattern has to backtrack over it
    int issues = text.lastIndexOf(ISSUES_PATH);
    return issues > 0 && ISSUE_URL_PREFIX.matcher(text.substring(0, issues)).matches()
      && ISSUE_NUMBER.matcher("#" + text.substring(issues + ISSUES_PATH.length())).matches();
  }

  private static void checkQuarantine(AnnotatedElement element, String where, List<String> violations) {
    Quarantine quarantine = element.getAnnotation(Quarantine.class);
    if (quarantine == null) {
      if (hasTagByHand(element, Quarantine.TAG)) {
        violations.add(where + " is tagged \"" + Quarantine.TAG + "\" with @Tag: use @Quarantine(issue = \"#123\", reason = \"...\"),"
          + " so the issue that tracks the flakiness is recorded");
      }
      return;
    }
    if (!isIssueReference(quarantine.issue())) {
      violations.add(where + ": @Quarantine issue \"" + quarantine.issue() + "\" is not an issue reference: give the number"
        + " (#123) or the URL of the issue that tracks the flakiness");
    }
    if (quarantine.reason().isBlank()) {
      violations.add(where + ": @Quarantine needs a reason that says why the test is flaky");
    }
  }

  private static void checkChain(Class<?> testClass, List<String> violations) {
    DependentChain chain = testClass.getAnnotation(DependentChain.class);
    if (chain == null) {
      if (hasTagByHand(testClass, DependentChain.TAG)) {
        violations.add(testClass.getName() + " is tagged \"" + DependentChain.TAG + "\" with @Tag: use @DependentChain(\"why the"
          + " tests depend on each other\"), so the build runs it without the per-test rerun and the reason is recorded");
      }
      return;
    }
    if (chain.value().isBlank()) {
      violations.add(testClass.getName() + ": @DependentChain needs a reason that says why the tests depend on each other");
    }
    if (!testClass.isAnnotationPresent(TestMethodOrder.class)) {
      violations.add(testClass.getName() + ": a @DependentChain runs its tests in order, so it needs @TestMethodOrder"
        + " (for example MethodOrderer.MethodName)");
    }
  }

  private static boolean hasTagByHand(AnnotatedElement element, String tag) {
    // Only the tags written on the element itself: the tag that @Quarantine brings is a meta-annotation and is not reported
    return Arrays.stream(element.getDeclaredAnnotationsByType(Tag.class)).anyMatch(declared -> tag.equals(declared.value()));
  }

  private static Class<?> load(String className) throws IOException {
    try {
      return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
    } catch (ClassNotFoundException | LinkageError exception) {
      throw new IOException("Class " + className + " cannot be loaded to check its declarations: " + exception, exception);
    }
  }
}
