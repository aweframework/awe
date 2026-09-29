package com.almis.awe.testing.extensions;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SeleniumExtensionTest {

  @TempDir
  Path tempDir;

  private SeleniumExtension extension;
  private SeleniumModel model;
  private Path shots;

  @BeforeEach
  void setUp() throws IOException {
    WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    Path source = Files.createTempFile(tempDir, "source", ".png");
    Files.write(source, new byte[]{1, 2, 3});
    when(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)).thenReturn(source.toFile());

    shots = tempDir.resolve("shots");
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setScreenshotPath(shots.toString());
    properties.setAllowedRecording(false);

    model = new SeleniumModel().setDriver(driver).setProperties(properties).setCurrentOption("option");

    extension = new SeleniumExtension();
    ReflectionTestUtils.setField(extension, "seleniumModel", model);
    // A non-null driver makes beforeEach skip the real driver initialization
    ReflectionTestUtils.setField(extension, "driver", driver);
  }

  @Test
  void afterTestExecutionStoresExactlyOneErrorScreenshotOnFailure() throws Exception {
    extension.beforeEach(context("t010_login", new AssertionError("boom")));

    extension.afterTestExecution(context("t010_login", new AssertionError("boom")));

    assertThat(shots()).hasSize(1);
    assertThat(shots().get(0)).startsWith("LoginIT-").contains("-[ERROR]-option-t010_login").endsWith(".png");
  }

  @Test
  void afterTestExecutionStoresNothingOnPass() throws Exception {
    extension.beforeEach(context("t010_login", null));

    extension.afterTestExecution(context("t010_login", null));

    assertThat(shots()).isEmpty();
  }

  @Test
  void afterEachDoesNotTakeTheScreenshotAnymore() throws Exception {
    extension.beforeEach(context("t010_login", new AssertionError("boom")));

    extension.afterEach(context("t010_login", new AssertionError("boom")));

    assertThat(shots()).isEmpty();
  }

  @Test
  void beforeEachResetsTheScreenshotTakenFlag() throws Exception {
    model.setScreenshotTaken(true);

    extension.beforeEach(context("t020_next", new AssertionError("boom")));
    assertThat(model.isScreenshotTaken()).isFalse();

    extension.afterTestExecution(context("t020_next", new AssertionError("boom")));
    assertThat(shots()).hasSize(1);
  }

  private ExtensionContext context(String testName, Throwable failure) {
    ExtensionContext parent = mock(ExtensionContext.class);
    when(parent.getDisplayName()).thenReturn("LoginIT");
    ExtensionContext context = mock(ExtensionContext.class);
    when(context.getDisplayName()).thenReturn(testName);
    when(context.getParent()).thenReturn(Optional.of(parent));
    when(context.getExecutionException()).thenReturn(Optional.ofNullable(failure));
    return context;
  }

  private List<String> shots() throws IOException {
    if (!Files.exists(shots)) {
      return java.util.Collections.emptyList();
    }
    try (Stream<Path> files = Files.list(shots)) {
      return files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
    }
  }
}
