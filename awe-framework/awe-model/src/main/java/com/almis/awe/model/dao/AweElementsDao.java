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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
public class AweElementsDao {

  private static final String OK = " - OK";
  private static final String KO = " - NOT FOUND";
  private static final String READING = "Reading ''{0}''{1} - elapsed time: {2}s";
  private static final String READING_FILES_FROM = "Reading files from ''{0}''{1}";
  private static final String ERROR_PARSING_XML = "Error parsing XML - '{}'";
  private static final String ERROR_READING_XML = "Error reading XML - '{}'";
  private static final String WARNING_FILE_TOO_BIG = "WARNING! This file is very big and takes too much time to load: {}";
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
  @Async("contextlessTaskExecutor")
  public <T extends XMLFile, N extends XMLNode> Future<String> readXmlFilesAsync(Class<T> rootClass, Map<String, N> storage, String filePath) {
    readXmlFiles(rootClass, storage, filePath);
    return new AsyncResult<>(null);
  }

  /**
   * Read all XML files and store them in the component
   *
   * @param rootClass Root class
   * @param storage   Storage list
   * @param filePath  File path
   */
  public <T extends XMLFile, N extends XMLNode> void readXmlFiles(Class<T> rootClass, Map<String, N> storage, String filePath) {
    // For each module read XML files
    Arrays.stream(baseConfigProperties.getModuleList())
      .map(module -> readModuleFile(rootClass, storage, baseConfigProperties.getPaths().getApplication() + module + filePath))
      .forEach(log::info);
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
    try {
      // Unmarshall XML (if it exists)
      Resource resource = new ClassPathResource(filePath);
      if (resource.exists()) {
        XMLFile fullXml = fromXML(rootClass, resource.getInputStream());

        // Read all XML elements
        readXmlElements(fullXml, storage);
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(WARNING_FILE_TOO_BIG, logPath);
        }
        return MessageFormat.format(READING, logPath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, "s.SSS", false));
      }
    } catch (IOException exc) {
      log.error(ERROR_PARSING_XML, logPath, exc);
    }

    return MessageFormat.format(READING, logPath, KO,
      DurationFormatUtils.formatDuration(System.currentTimeMillis() - startTime, "s.SSS", false));
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
  @Cacheable(value = "xml", key = "{ #p0.toString(), #p1 }")
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
    try {
      // Unmarshall XML
      Resource resource = new ClassPathResource(filePath);
      if (resource.exists()) {
        InputStream resourceInputStream = resource.getInputStream();
        file = fromXML(clazz, resourceInputStream);
        long elapsedTime = System.currentTimeMillis() - startTime;
        messageList.add(MessageFormat.format(READING, logFilePath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, "s.SSS", false)));
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(MessageFormat.format(WARNING_FILE_TOO_BIG, logFilePath));
        }
      }
    } catch (IOException exc) {
      log.error(ERROR_PARSING_XML, logFilePath, exc);
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
  @Async("contextlessTaskExecutor")
  public <T> Future<String> readModuleFolderXmlFile(Class<T> clazz, String path, Map<String, T> storage) {
    List<String> resultList = new ArrayList<>();
    Path logPath = Paths.get(path);
    try {
      // Check resource path
      Resource basePathResource = new ClassPathResource(path);
      if (basePathResource.exists()) {
        // Read files from path
        PathMatchingResourcePatternResolver loader = new PathMatchingResourcePatternResolver();
        Resource[] resources = loader.getResources("classpath:" + path + "**/*" + baseConfigProperties.getExtensionXml());
        if (resources.length > 0) {
          log.info(MessageFormat.format(READING_FILES_FROM, logPath, OK));
          resultList.addAll(readXmlFileFolder(resources, clazz, path, storage));
          resultList.forEach(log::info);
        }
      }
    } catch (IOException exc) {
      log.error(ERROR_READING_XML, logPath, exc);
    }
    return new AsyncResult<>(null);
  }

  /**
   * Read XML Files from subfolder
   *
   * @param resources XML File resources
   * @param clazz     Target class to map
   * @param path      XML File path
   * @param storage   Storage map
   * @return Message list
   */
  private <T> List<String> readXmlFileFolder(Resource[] resources, Class<T> clazz, String path, Map<String, T> storage) {
    return Arrays.stream(resources)
      .map(resource -> {
        try {
          return readXmlResourceFile(resource, clazz, path, storage);
        } catch (IOException exc) {
          log.warn(ERROR_READING_XML, resource.getFilename(), exc);
        }
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  /**
   * Read XML file from a resource
   *
   * @param resource Resource
   * @param clazz    Resource class
   * @param basePath Base path
   * @param storage  Storage
   * @throws IOException Error reading resource
   */
  private <T> String readXmlResourceFile(Resource resource, Class<T> clazz, String basePath, Map<String, T> storage) throws IOException {
    long startTime = System.currentTimeMillis();
    if (resource.exists()) {
      String fileName = Objects.requireNonNull(resource.getFilename()).replace(baseConfigProperties.getExtensionXml(), "");
      if (!storage.containsKey(fileName)) {
        storage.put(fileName, fromXML(clazz, resource.getInputStream()));
        String logFilePath = Paths.get(basePath, Optional.ofNullable(resource.getURL().getPath())
          .orElse(basePath + resource.getFilename()).split(basePath)[1]).toString();
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > LONG_FILE_TIME_TO_LOAD) {
          log.warn(MessageFormat.format(WARNING_FILE_TOO_BIG, logFilePath));
        }
        return MessageFormat.format(READING, logFilePath, OK,
          DurationFormatUtils.formatDuration(elapsedTime, "s.SSS", false));
      }
    }
    return null;
  }

  /**
   * Read a locale file asynchronously
   *
   * @param basePath   base path
   * @param language   language
   * @param localeList locale list
   */
  @Async("contextlessTaskExecutor")
  public Future<String> readLocaleModuleAsync(String basePath, String language, Map<String, Map<String, String>> localeList) {
    // Create a local storage and read the locales from all modules
    Map<String, Global> localeLanguage = new ConcurrentHashMap<>();
    log.info(readModuleFile(Locales.class, localeLanguage, basePath));

    // Parse the read locales and store them on the final storage
    Map<String, String> newLanguageMap = localeLanguage.values().stream()
      .collect(Collectors.toMap(Global::getName, StringUtil::parseLocale, (f, s) -> f, ConcurrentHashMap::new));
    Map<String, String> languageLocaleMap = Optional.ofNullable(localeList.get(language)).orElse(new ConcurrentHashMap<>());
    newLanguageMap.putAll(languageLocaleMap);
    localeList.put(language, newLanguageMap);

    return new AsyncResult<>(null);
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
