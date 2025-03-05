package com.almis.awe.model.dao;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.XMLFile;
import com.almis.awe.model.entities.XMLNode;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.util.data.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class AweElementsDao {

  public static final String LOG_SECONDS_FORMAT = "s.SSS";
  private static final String OK = " - \u001B[1m\u001B[32mOK\u001B[0m";
  private static final String KO = " - \u001B[1m\u001B[90mNOT FOUND\u001B[0m";
  private static final String READING = "Reading ''{0}''{1} - elapsed time: {2}s";
  private static final String READING_FILES_FROM = "Reading files from '{}'{}";
  private static final String ERROR_PARSING_XML = "\u001B[31mError parsing XML - '{}'\u001B[0m";
  private static final String ERROR_READING_XML = "\u001B[31mError reading XML - '{}'\u001B[0m";
  private static final String WARNING_FILE_TOO_BIG = "\u001B[93mWARNING! This file is very big and takes too much time to load: {}\u001B[0m";
  private static final int LONG_FILE_TIME_TO_LOAD = 5000;
  // Autowired services
  private final XStreamSerializer serializer;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param serializer           Serializer
   * @param baseConfigProperties Base config properties
   */
  public AweElementsDao(XStreamSerializer serializer, BaseConfigProperties baseConfigProperties) {
    this.serializer = serializer;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param rootClass Root class
   * @param storage   Storage list
   * @param filePath  File path
   */
  @Async("contextLessTaskExecutor")
  public <T extends XMLFile, N extends XMLNode> Future<String> readXmlFilesAsync(Class<T> rootClass, Map<String, N> storage, String filePath) {
    return CompletableFuture.completedFuture(readXmlFiles(rootClass, storage, filePath));
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param rootClass Root class
   * @param storage   Storage list
   * @param filePath  File path
   */
  public <T extends XMLFile, N extends XMLNode> String readXmlFiles(Class<T> rootClass, Map<String, N> storage, String filePath) {
    // For each module read XML files
    Arrays.stream(baseConfigProperties.getModuleList())
      .map(module -> readModuleFile(rootClass, storage, baseConfigProperties.getPaths().getApplication() + module + filePath))
      .forEach(log::info);
    return "OK";
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param rootClass Root class
   * @param storage   Storage list
   * @param filePath  File path
   */
  public <T extends XMLFile, N extends XMLNode> String readModuleFile(Class<T> rootClass, Map<String, N> storage, String filePath) {
    Path logPath = Paths.get(filePath);
    long startTime = System.currentTimeMillis();
    // Unmarshall XML (if it exists)
    Resource resource = new ClassPathResource(filePath);
    if (resource.exists()) {
      try (InputStream inputStream = resource.getInputStream()) {
        XMLFile fullXml = fromXML(rootClass, inputStream);

        // Read all XML elements
        readXmlElements(fullXml, storage);
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(WARNING_FILE_TOO_BIG, logPath);
        }
        return MessageFormat.format(READING, logPath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, LOG_SECONDS_FORMAT, false));
      } catch (IOException exc) {
        log.error(ERROR_PARSING_XML, logPath, exc);
      }
    }

    return MessageFormat.format(READING, logPath, KO,
      DurationFormatUtils.formatDuration(System.currentTimeMillis() - startTime, LOG_SECONDS_FORMAT, false));
  }

  /**
   * Read all XML elements and store them in the component
   *
   * @param fullXml Full xml object
   * @param storage Storage map
   */
  private <N extends XMLNode> void readXmlElements(XMLFile fullXml, Map<String, N> storage) {
    // Read XML Elements and store them
    List<N> elementList = fullXml.getBaseElementList();
    elementList.stream()
      .filter(Objects::nonNull)
      .filter(element -> !storage.containsKey(element.getElementKey()))
      .forEach(element -> storage.put(element.getElementKey(), element));
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param clazz    File class
   * @param basePath File path
   * @param fileName File name
   * @param <T>      Class type
   * @return Xml file object
   */
  public <T> T readXmlFile(Class<T> clazz, String basePath, String fileName) {
    List<String> messageList = new ArrayList<>();
    // For each module read XML files
    T file = Arrays.stream(baseConfigProperties.getModuleList())
      .map(module -> findXmlModulePath(baseConfigProperties.getPaths().getApplication() + module + basePath, fileName))
      .filter(Strings::isNotEmpty)
      .map(path -> readModuleXmlFile(clazz, path + fileName, messageList))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);

    // Log results
    messageList.forEach(log::info);

    return file;
  }

  private String findXmlModulePath(String basePath, String fileName) {
    // Read files from path
    try {
      PathMatchingResourcePatternResolver loader = new PathMatchingResourcePatternResolver();
      Resource[] resources = loader.getResources("classpath:" + basePath + "**/" + fileName);
      return Arrays.stream(resources)
        .filter(resource -> fileName.equalsIgnoreCase(resource.getFilename()))
        .map(resource -> basePath)
        .findFirst()
        .orElse(null);
    } catch (Exception exc) {
      log.warn(ERROR_READING_XML, fileName, exc);
    }

    return null;
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param clazz    File class
   * @param filePath File path
   * @param <T>      Class type
   * @return Xml file object
   */
  private <T> T readModuleXmlFile(Class<T> clazz, String filePath, List<String> messageList) {
    T file = null;
    long startTime = System.currentTimeMillis();
    Path logFilePath = Paths.get(filePath);

    // Unmarshall XML
    Resource resource = new ClassPathResource(filePath);
    if (resource.exists()) {
      try (InputStream resourceInputStream = resource.getInputStream()) {
        file = fromXML(clazz, resourceInputStream);
        long elapsedTime = System.currentTimeMillis() - startTime;
        messageList.add(MessageFormat.format(READING, logFilePath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, LOG_SECONDS_FORMAT, false)));
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(WARNING_FILE_TOO_BIG, logFilePath);
        }
      } catch (IOException exc) {
        log.error(ERROR_PARSING_XML, logFilePath, exc);
      }
    }

    return file;
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param clazz File class
   * @param path  Base directory path
   * @return Xml file object
   */
  @Async("contextLessTaskExecutor")
  public <T> Future<Map<String, T>> readModuleFolderXmlFile(Class<T> clazz, String path) {
    Path logPath = Paths.get(path);
    Map<String, T> storage = new ConcurrentHashMap<>();
    try {
      // Check resource path
      Resource basePathResource = new ClassPathResource(path);
      if (basePathResource.exists()) {
        // Read files from path
        PathMatchingResourcePatternResolver loader = new PathMatchingResourcePatternResolver();
        Resource[] resources = loader.getResources("classpath:" + path + "**/*" + baseConfigProperties.getExtensionXml());
        if (resources.length > 0) {
          log.info(READING_FILES_FROM, logPath, OK);
          storage = readXmlFileFolder(resources, clazz, path);
        }
      }
    } catch (Exception exc) {
      log.error(ERROR_READING_XML, logPath, exc);
    }
    return CompletableFuture.completedFuture(storage);
  }

  /**
   * Read XML Files from subfolder
   *
   * @param resources XML File resources
   * @param clazz     Target class to map
   * @param path      XML File path
   * @return Message list
   */
  private <T> Map<String, T> readXmlFileFolder(Resource[] resources, Class<T> clazz, String path) {
    return Arrays.stream(resources)
      .map(resource -> readXmlResourceFile(resource, clazz, path))
      .flatMap(m -> m.entrySet().stream())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Read XML file from a resource
   *
   * @param resource Resource
   * @param clazz    Resource class
   * @param basePath Base path
   * @return Resource file read
   */
  private <T> Map<String, T> readXmlResourceFile(Resource resource, Class<T> clazz, String basePath) {
    long startTime = System.currentTimeMillis();
    Map<String, T> storage = new HashMap<>();
    if (resource.exists()) {
      try (InputStream inputStream = resource.getInputStream()) {
        T file = fromXML(clazz, inputStream);
        String fileName = Objects.requireNonNull(resource.getFilename()).replace(baseConfigProperties.getExtensionXml(), "");
        storage.put(fileName, file);
        String logFilePath = Paths.get(basePath, Optional.ofNullable(resource.getURL().getPath())
          .orElse(basePath + resource.getFilename()).split(basePath)[1]).toString();
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(WARNING_FILE_TOO_BIG, logFilePath);
        }
        log.info(MessageFormat.format(READING, logFilePath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, LOG_SECONDS_FORMAT, false)));
        return storage;
      } catch (Exception exc) {
        log.error(ERROR_READING_XML, resource.getFilename(), exc);
      }
    }
    return Collections.emptyMap();
  }

  /**
   * Read a locale file asynchronously
   *
   * @param basePath base path
   */
  @Async("contextLessTaskExecutor")
  public Future<Map<String, String>> readLocaleModuleAsync(String basePath) {
    // Create a local storage and read the locales from all modules
    Map<String, Global> localeLanguage = new ConcurrentHashMap<>();
    log.info(readModuleFile(Locales.class, localeLanguage, basePath));

    // Parse the read locales and store them on the final storage
    return CompletableFuture.completedFuture(localeLanguage.values().stream()
      .collect(Collectors.toMap(Global::getName, StringUtil::parseLocale, (f, s) -> f, ConcurrentHashMap::new)));
  }

  /**
   * Deserialize string template
   *
   * @param template String template
   * @return Object deserialized
   */
  public <T> String toXMLString(T template) {
    return serializer.writeStringFromObject(template);
  }

  /**
   * Deserialize string template
   *
   * @param template String template
   * @return Object deserialized
   */
  public <T> T parseTemplate(Class<T> clazz, String template) {
    return serializer.getObjectFromTemplate(clazz, template);
  }

  /**
   * Deserialize XML
   *
   * @param clazz  Object class
   * @param stream XML Stream
   * @return Object deserialized
   */
  private <T> T fromXML(Class<T> clazz, InputStream stream) {
    return serializer.getObjectFromXml(clazz, stream);
  }
}
