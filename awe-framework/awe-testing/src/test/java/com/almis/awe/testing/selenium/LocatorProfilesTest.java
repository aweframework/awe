package com.almis.awe.testing.selenium;

import com.almis.awe.testing.driver.Locator;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every locator that the engine profiles build today ({@code By} and {@code List<By>}) must fit the tool-neutral
 * {@link Locator}, so that a later engine adapter does not need the tests rewritten. The calls come from the catalog of
 * the selector guards, which another guard keeps complete.
 */
class LocatorProfilesTest {

  private static final List<AbstractFrontEndSelectorGuardTest> GUARDS = List.of(
    new AngularAweInstructionsSelectorGuardTest(), new ReactAweInstructionsSelectorGuardTest());

  @Test
  void shouldCatalogEveryLocatorMethodOfTheProfiles() {
    for (AbstractFrontEndSelectorGuardTest guard : GUARDS) {
      Set<String> cataloged = guard.catalog().keySet().stream().map(call -> call.replaceAll("\\(.*", ""))
        .collect(Collectors.toSet());
      Set<String> missing = locatorMethods(guard.instructions().getClass()).stream().map(Method::getName)
        .filter(name -> !cataloged.contains(name)).collect(Collectors.toCollection(TreeSet::new));
      assertThat(missing).as("Locator methods of %s missing in the catalog", guard.instructions().getClass().getSimpleName())
        .isEmpty();
    }
  }

  @Test
  void shouldConvertEveryLocatorOfTheProfilesToANeutralLocatorAndBack() {
    List<String> failures = new ArrayList<>();
    int converted = 0;
    for (AbstractFrontEndSelectorGuardTest guard : GUARDS) {
      String profile = guard.instructions().getClass().getSimpleName();
      for (Map.Entry<String, Supplier<Object>> call : guard.catalog().entrySet()) {
        Object result = call.getValue().get();
        List<?> locators = result instanceof List<?> list ? list : result instanceof By ? List.of(result) : List.of();
        for (Object item : locators) {
          By by = (By) item;
          try {
            Locator locator = Locator.from(by);
            By back = locator.toBy();
            if (!(by instanceof By.ById)) {
              assertThat(back).isEqualTo(by);
              assertThat(back).hasToString(by.toString());
            }
            assertThat(Locator.from(back)).isEqualTo(locator);
            assertThat(locator.expression()).isNotBlank();
            converted++;
          } catch (RuntimeException | AssertionError exception) {
            failures.add(profile + "." + call.getKey() + " -> " + by + " [" + exception.getMessage() + "]");
          }
        }
      }
    }

    assertThat(failures).as("Locators that do not fit the neutral form").isEmpty();
    assertThat(converted).as("Converted locators").isGreaterThan(150);
  }

  @Test
  void shouldConvertTheRelativeRowOfACell() {
    Locator row = Locator.from(new ReactAweInstructions().getGridRowOfCell());

    assertThat(row.kind()).isEqualTo(Locator.Kind.XPATH);
    assertThat(row.isContextRelative()).isTrue();
    assertThat(row.expression()).startsWith("ancestor-or-self::");
  }

  @Test
  void shouldConvertTheShellControlLists() {
    for (AbstractFrontEndSelectorGuardTest guard : GUARDS) {
      IAweFrontEndInstructions profile = guard.instructions();
      assertThat(Locator.from(profile.getRequiredPostLoginShellControls()))
        .hasSameSizeAs(profile.getRequiredPostLoginShellControls()).doesNotContainNull();
      assertThat(Locator.from(profile.getOptionalPostLoginShellControls()))
        .hasSameSizeAs(profile.getOptionalPostLoginShellControls()).doesNotContainNull();
    }
  }

  private static List<Method> locatorMethods(Class<?> type) {
    List<Method> methods = new ArrayList<>();
    for (Method method : type.getMethods()) {
      boolean locator = By.class.equals(method.getReturnType()) || List.class.equals(method.getReturnType())
        && method.getGenericReturnType().getTypeName().contains(By.class.getName());
      if (locator && !Modifier.isStatic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass())) {
        methods.add(method);
      }
    }
    return methods;
  }
}
