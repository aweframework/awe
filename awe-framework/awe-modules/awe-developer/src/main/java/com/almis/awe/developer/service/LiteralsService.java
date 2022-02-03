package com.almis.awe.developer.service;

import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.client.grid.UpdateCellActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.developer.comparator.CompareLocal;
import com.almis.awe.developer.model.ITranslationResult;
import com.almis.awe.developer.model.Literal;
import com.almis.awe.developer.type.FormatType;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class LiteralsService extends ServiceConfig {

  // Autowired services
  private final TranslationService translationService;
  private final LocaleFileService localeFileService;

  @Value("${language.default:en}")
  private String defaultLanguage;

  /**
   * Autowired constructor
   *
   * @param translationService Translation service
   * @param localeFileService  Locale service
   */
  public LiteralsService(TranslationService translationService, LocaleFileService localeFileService) {
    this.translationService = translationService;
    this.localeFileService = localeFileService;
  }

  /**
   * Translate from one language to other
   *
   * @param text         Text to translate
   * @param fromLanguage Source language
   * @param toLanguage   Target language
   * @return Translation
   * @throws AWException Error translating text
   */
  public ServiceData translate(String text, String fromLanguage, String toLanguage) throws AWException {
    String result = text;

    if (!Optional.ofNullable(fromLanguage).orElse("").equalsIgnoreCase(toLanguage)) {
      // Call translation API
      result = translationService.getTranslation(text, fromLanguage, toLanguage).getTranslation();
    }

    return new ServiceData()
      .setDataList(DataListUtil
        .fromBeanList(Collections.singletonList(new Literal()
          .setValue(result))));
  }

  /**
   * Returns the locale match list for a string given the language
   *
   * @param literal  Code
   * @param codeLang Language
   * @return Service
   * @throws AWException Error retrieving locale matches
   */
  public ServiceData getLocaleMatches(String literal, String codeLang) throws AWException {
    return new ServiceData()
      .setDataList(findStringInFile(codeLang.toUpperCase(), literal));
  }

  /**
   * Returns existing translations corresponding to a code
   *
   * @param code Code
   * @return Translation list
   * @throws AWException Error retrieving translation list
   */
  public ServiceData getTranslationList(String code) throws AWException {
    // List of loaded languages
    DataList translations = new DataList();

    // Iterate by language
    for (String codeLang : localeFileService.getLanguageList()) {
      // Get language
      DataList languageData = retrieveLocaleFromFile(code, codeLang.toUpperCase());
      translations.getRows().addAll(languageData.getRows());
    }

    return new ServiceData().setDataList(translations.setRecords(translations.getRows().size()));
  }

  /**
   * Save translation
   *
   * @param formatSelector Format selector
   * @param text           Text
   * @param markdown       Markdown
   * @param codeLang       Language
   * @param searchLang     Language (search)
   * @param code           Code
   * @return Translation stored
   * @throws AWException Error storing translation
   */
  public ServiceData saveTranslation(String formatSelector, String text, String markdown, String codeLang, String searchLang, String code) throws AWException {
    // Access to the memory
    ServiceData serviceData = new ServiceData();

    try {
      storeUpdatedLocale(codeLang.toUpperCase(), code, text, markdown, formatSelector);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_STORING_TRANSLATION"), getLocale("ERROR_MESSAGE_STORING_TRANSLATION", code, text), exc);
    }

    String value;
    if (FormatType.TEXT.toString().equalsIgnoreCase(formatSelector)) {
      value = text;
    } else {
      value = markdown;
    }

    serviceData.setTitle(getLocale("OK_TITLE_LOCAL_UPDATED"));
    serviceData.setMessage(getLocale("OK_MESSAGE_LOCAL_UPDATED", code));

    // Build address of cell
    ComponentAddress address = new ComponentAddress("report", "GrdTraLit", codeLang, "lite");

    // Add action to list
    serviceData.addClientAction(new UpdateCellActionBuilder(address, new CellData(value)).build());

    // If saved translation's language is the same as from searched, update locale list grid
    if (codeLang.equalsIgnoreCase(searchLang)) {
      // Build address of cell
      address = new ComponentAddress("report", "GrdStrLit", code, "lit");

      // Add action to list
      serviceData.addClientAction(new UpdateCellActionBuilder(address, new CellData(value)).build());
    }

    return serviceData;
  }

  /**
   * Save new literal
   *
   * @param codeLang Language
   * @param code     Code
   * @param literal  Text
   * @return New literal created
   * @throws AWException Error creating new locale
   */
  public ServiceData newLiteral(String codeLang, String code, String literal) throws AWException {
    // Iterate by language (excluding the base codeLang)
    List<ServiceData> resultList = localeFileService.getLanguageList().stream()
      .map(language -> addNewLocale(language, codeLang, code, literal))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    // Retrieve service data
    Comparator<ServiceData> comparator = Comparator.comparingInt(serviceData -> Integer.parseInt(Optional.ofNullable((String) serviceData.getData()).orElse(String.valueOf(Integer.MAX_VALUE))));
    return resultList.stream()
      .filter(serviceData -> AnswerType.ERROR.equals(serviceData.getType()))
      .findAny()
      .orElse(resultList.stream()
        .filter(serviceData -> AnswerType.OK.equals(serviceData.getType()))
        .min(comparator)
        .orElse(new ServiceData()
          .setTitle(getLocale("WARNING_TITLE_NEW_LOCAL"))
          .setMessage(getLocale("WARNING_MESSAGE_LOCAL_ALREADY_EXISTS", code))
          .setType(AnswerType.WARNING)));
  }

  private ServiceData addNewLocale(String language, String oldLanguage, String code, String literal) {
    try {
      // Get locals of one language
      Locales localesFromFile = Optional.ofNullable(localeFileService.readLocalesFromFile(language)).orElse(new Locales());

      // Check if local already exists
      if (localesFromFile.getLocale(code) == null) {
        String newLiteral = literal;
        String remaining = null;
        if (!oldLanguage.equalsIgnoreCase(language)) {
          ITranslationResult result = translationService.getTranslation(literal, oldLanguage.toUpperCase(), language);
          newLiteral = result.getTranslation();
          remaining = result.getRemaining();
          if (remaining != null) {
            log.info("There are still {} words remaining on translate service", remaining);
          }
        }

        // Create new local in XML file
        storeNewLocale(language, code, newLiteral);

        return new ServiceData()
          .setData(remaining)
          .setTitle(getLocale("OK_TITLE_NEW_LOCAL"))
          .setMessage(remaining == null ? getLocale("OK_MESSAGE_NEW_LOCAL", code) : getLocale("OK_MESSAGE_NEW_LOCAL_REMAINING", code, remaining));
      }
    } catch (AWException exc) {
      log.error("Error trying to add a new locale ({}) on language {}", code, language, exc);
      return new ServiceData()
        .setType(AnswerType.ERROR)
        .setTitle(exc.getTitle())
        .setMessage(exc.getMessage());
    }

    return null;
  }

  /**
   * Delete literal
   *
   * @param code Code
   * @return Deletion message
   * @throws AWException Error deleting locale
   */
  public ServiceData deleteLiteral(String code) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Iterate by language
    for (String codeLang : localeFileService.getLanguageList()) {
      // Get language
      storeDeletedLocale(codeLang, code);
    }

    return serviceData
      .setTitle(getLocale("OK_TITLE_REMOVED_LOCAL"))
      .setMessage(getLocale("OK_MESSAGE_REMOVED_LOCAL", code));
  }

  /**
   * Returns using language
   *
   * @return Used language
   */
  public ServiceData getUsingLanguage() {
    log.debug("Retrieving default language");
    return new ServiceData()
      .setDataList(DataListUtil.fromBeanList(Collections.singletonList(
        new Global()
          .setValue(defaultLanguage.toLowerCase())
          .setLabel("ENUM_LAN_" + defaultLanguage.toUpperCase()))));
  }

  /**
   * Get markdown text of literal from file
   *
   * @param codeLang Language
   * @param code     code
   * @return markdown text
   * @throws AWException Error retrieving markdown
   */
  public ServiceData getSelectedLocale(String codeLang, String code) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Read Locale File List for a LANGUAGE
    Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()))
      .getLocales().stream()
      .filter(global -> code.equalsIgnoreCase(global.getName()))
      .forEach(global -> {
        // Add actions to list
        FormatType format = StringUtils.isBlank(global.getMarkdown()) ? FormatType.TEXT : FormatType.MARKDOWN;
        String value = FormatType.TEXT.equals(format) ? global.getValue() : global.getMarkdown();
        serviceData
          .addClientAction(new SelectActionBuilder("litTxt", value).setAsync(true).build())
          .addClientAction(new SelectActionBuilder("litMrk", value).setAsync(true).build())
          .addClientAction(new SelectActionBuilder("FormatSelector", format.toString()).setAsync(true).build())
          .addClientAction(new SelectActionBuilder("FlgStoLit", format.toString()).setAsync(true).build());
      });

    return serviceData;
  }

  /**
   * Switch the languages
   *
   * @param fromLanguage Source language
   * @param toLanguage   Target language
   * @param fromTarget   Source target
   * @param toTarget     Target target
   * @return Languages changed
   */
  public ServiceData switchLanguages(String fromLanguage, String toLanguage, String fromTarget, String toTarget) {
    return new ServiceData()
      .addClientAction(new SelectActionBuilder(fromTarget, Collections.singletonList(toLanguage)).build())
      .addClientAction(new SelectActionBuilder(toTarget, Collections.singletonList(fromLanguage)).build());
  }

  /**
   * Search literal in file
   *
   * @param codeLang Language
   * @param search   Search string
   * @return matches
   * @throws AWException Error finding string in file
   */
  private DataList findStringInFile(String codeLang, String search) throws AWException {
    DataList dataList = new DataList();

    // Read Locale File List for a LANGUAGE
    List<Global> found = Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()))
      .getLocales().stream()
      .filter(global -> matchesSearch(search, global.getName(), Optional.ofNullable(global.getValue()).orElse(global.getMarkdown())))
      .collect(Collectors.toList());

    // Store in datalist
    DataListUtil.addColumn(dataList, "key", found.stream()
      .map(Global::getName)
      .collect(Collectors.toList()));
    DataListUtil.addColumn(dataList, AweConstants.JSON_VALUE_PARAMETER, found.stream()
      .map(global -> Optional.ofNullable(global.getValue()).orElse(global.getMarkdown()))
      .collect(Collectors.toList()));
    dataList.setRecords(dataList.getRows().size());

    return dataList;
  }

  /**
   * Check if search string matches values
   *
   * @param search Search string
   * @param values Values
   * @return Search string matches values
   */
  private boolean matchesSearch(String search, String... values) {
    return Arrays.stream(values)
      .anyMatch(value -> value.toLowerCase().contains(Optional.ofNullable(search).orElse("").toLowerCase()));
  }

  /**
   * Returns locale by the locale key and language code
   *
   * @param code     Code
   * @param codeLang Language
   * @return result Array of translations
   * @throws AWException Error retrieving locale
   */
  private DataList retrieveLocaleFromFile(String code, String codeLang) throws AWException {
    Locales locales = Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()));

    return DataListUtil.fromBeanList(locales.getLocales().stream()
      .filter(global -> code.equalsIgnoreCase(global.getName()))
      .map(global -> new Literal()
        .setValue(StringUtil.parseLocale(global))
        .setKey(codeLang)
        .setCode(code))
      .collect(Collectors.toList()));
  }

  /**
   * Save modifications in file
   *
   * @param codeLang       Language
   * @param code           Code
   * @param text           Text
   * @param markdown       Markdown
   * @param formatSelector Format
   * @throws AWException Error stroring locale
   */
  private void storeUpdatedLocale(String codeLang, String code, String text, String markdown, String formatSelector) throws AWException {

    String valueToText = FormatType.TEXT.toString().equalsIgnoreCase(formatSelector) ? text : null;
    String valueToMarkdown = FormatType.TEXT.toString().equalsIgnoreCase(formatSelector) ? null : markdown;

    // Read locale File List for a LANGUAGE
    Locales locales = Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()));

    // Update locale
    locales.getLocales().stream()
      .filter(global -> code.equalsIgnoreCase(global.getName()))
      .forEach(global -> global.setValue(valueToText).setMarkdown(valueToMarkdown));

    // Fix markdown attribute
    fixMarkdown(locales.getLocales());

    // Store updated locale file
    localeFileService.storeLocaleListFile(codeLang, locales);
  }

  /**
   * Save new local in XML file
   *
   * @param codeLang Language
   * @param code     Code
   * @param text     Text
   * @throws AWException Error storing locale
   */
  private void storeNewLocale(String codeLang, String code, String text) throws AWException {

    Global locale = new Global();
    locale.setName(code);
    locale.setValue(text);
    locale.setMarkdown(null);

    // Read Local File List for a LANGUAGE
    Locales locales = Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()));

    // Add locale
    locales.getLocales().add(locale);

    // Fix markdown attribute
    fixMarkdown(locales.getLocales());

    // Sort
    locales.getLocales().sort(new CompareLocal());

    // Store updated local file
    localeFileService.storeLocaleListFile(codeLang, locales);
  }

  /**
   * Delete literal from file
   *
   * @param codeLang Language
   * @param code     Code
   * @throws AWException Error deleting locale
   */
  private void storeDeletedLocale(String codeLang, String code) throws AWException {

    // Read locale File List for a LANGUAGE
    Locales locales = Optional.ofNullable(localeFileService.readLocalesFromFile(codeLang))
      .orElse(new Locales().setLocales(new ArrayList<>()));

    locales.setLocales(locales.getLocales().stream()
      .filter(global -> !code.equalsIgnoreCase(global.getName()))
      .collect(Collectors.toList()));

    // Fix markdown attribute
    fixMarkdown(locales.getLocales());

    // Store updated local file
    localeFileService.storeLocaleListFile(codeLang, locales);
  }

  /**
   * Fix markdown attribute
   *
   * @param globals Globals
   */
  private void fixMarkdown(List<Global> globals) {
    // Fix markdown attribute
    globals.stream()
      .filter(global -> StringUtils.isBlank(global.getMarkdown()))
      .forEach(global -> global.setMarkdown(null));
  }
}
