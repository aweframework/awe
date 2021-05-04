package com.almis.awe.test.unit.developer;

import com.almis.awe.developer.service.LocaleFileService;
import com.almis.awe.developer.service.PathService;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.test.unit.TestUtil;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Class used for testing translation service
 *
 * @author pvidal
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Log4j2
@RunWith(MockitoJUnitRunner.class)
public class LocaleFileServiceTest extends TestUtil {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @InjectMocks
  private LocaleFileService localeFileService;
  @Mock
  private PathService pathService;
  @Mock
  private XStreamSerializer serializer;

  /**
   * Initializes beans for tests
   */
  @Before
  public void initBeans() throws Exception {
    ReflectionTestUtils.setField(localeFileService, "xmlExtension", ".xml");
    ReflectionTestUtils.setField(localeFileService, "localeFile", "Locale-");
  }

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  public void readLocalesFromFile() throws Exception {
    when(pathService.getPath()).thenReturn(folder.getRoot().getAbsolutePath() + "/");
    when(serializer.getObjectFromXml(any(Class.class), any(InputStream.class))).thenReturn(new Locales());
    folder.newFile("Locale-EN.xml").createNewFile();

    // Launch
    Locales locales = localeFileService.readLocalesFromFile("en");
    assertNotNull(locales);
  }

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  public void readLocalesFromFileNotFound() throws Exception {
    // Launch
    assertNull(localeFileService.readLocalesFromFile("en"));
  }
}