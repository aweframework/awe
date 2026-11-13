package com.almis.awe.service.menu;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.StringUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the suggestion lists (screens, options and modules) used by selectors
 */
public class MenuSuggestionLists extends ServiceConfig {

  private final MenuOptionLookup menuOptionLookup;
  private final MenuScreenResolver menuScreenResolver;

  /**
   * Autowired constructor
   *
   * @param menuOptionLookup   Menu option lookup
   * @param menuScreenResolver Menu screen resolver
   */
  public MenuSuggestionLists(MenuOptionLookup menuOptionLookup, MenuScreenResolver menuScreenResolver) {
    this.menuOptionLookup = menuOptionLookup;
    this.menuScreenResolver = menuScreenResolver;
  }

  /**
   * Retrieve a list of available screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getAvailableScreenList(String suggest) throws AWException {
    // Get screen from option
    Set<String> addedScreens = new HashSet<>();
    List<Option> optionList = menuOptionLookup.getAllAvailableOptions();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String screenId = option.getScreen();
      if (screenId != null && !addedScreens.contains(screenId)) {
        // Add to list
        addOptionToList(suggest, screenId, option, dataList, addedScreens);
      }
    }

    return sortedServiceData(dataList);
  }

  /**
   * Retrieve a list of all screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   */
  public ServiceData getAllScreenList(String suggest) {
    // Get screen from option
    Set<String> addedScreens = new HashSet<>();
    Set<String> screenList = getElements().getScreenList();
    DataList dataList = new DataList();
    for (String screen : screenList) {
      // Add to list
      addScreenToList(suggest, screen, screen, dataList, addedScreens);
    }

    return sortedServiceData(dataList);
  }

  /**
   * Retrieve a list of available modules
   *
   * @return Module list retrieved
   * @throws AWException Error retrieving module list
   */
  public ServiceData getModuleList() throws AWException {
    // Get screen from option
    Set<String> modules = new HashSet<>();
    List<Option> optionList = menuOptionLookup.getAllOptions();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String module = option.getModule();
      if (module != null && !modules.contains(module)) {
        Map<String, CellData> row = new HashMap<>();
        // Set screen name
        row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(module));

        // Store screen label
        row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(module));

        // Store row
        dataList.addRow(row);
        modules.add(module);
      }
    }

    return sortedServiceData(dataList);
  }

  /**
   * Retrieve a list of available options
   *
   * @param suggest Option typed
   * @return Option list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getNameOptionList(String suggest) throws AWException {
    // Get screen from option
    Set<String> addedOptions = new HashSet<>();
    List<Option> optionList = menuOptionLookup.getAllOptions();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String optionName = option.getName();
      if (optionName != null && !addedOptions.contains(optionName)) {
        // Add the option to the list
        addOptionToList(suggest, optionName, option, dataList, addedOptions);
      }
    }

    return sortedServiceData(dataList);
  }

  /**
   * Set the records, sort the list by label and wrap it into a service data
   *
   * @param dataList Data list
   * @return Service data
   */
  private ServiceData sortedServiceData(DataList dataList) {
    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Add an option to the datalist
   *
   * @param suggest         Suggest to search
   * @param id              Option/screen identifier
   * @param option          Option
   * @param dataList        Datalist to add data
   * @param previouslyAdded Previously added identifiers
   * @throws AWException Error adding option to list
   */
  private void addOptionToList(String suggest, String id, Option option, DataList dataList,
                               Set<String> previouslyAdded) throws AWException {
    // Get screen label
    String label = option.getLabel();
    String screenId = option.getScreen();
    if (label == null && screenId != null) {
      Screen screen = menuScreenResolver.getScreen(screenId);
      label = screen.getLabel();
    }

    // Get option label locale
    label = label == null ? id : getLocale(label) + " (" + id + ")";

    // Add screen to list
    addScreenToList(suggest, id, label, dataList, previouslyAdded);
  }

  /**
   * Add a screen to the datalist
   *
   * @param suggest         Suggest to search
   * @param id              Option/screen identifier
   * @param label           Screen label
   * @param dataList        Datalist to add data
   * @param previouslyAdded Previously added identifiers
   */
  private void addScreenToList(String suggest, String id, String label, DataList dataList,
                               Set<String> previouslyAdded) {
    Map<String, CellData> row = new HashMap<>();

    // Add screen if matches with screen or locale
    if (StringUtil.containsIgnoreCase(label, suggest.trim())) {
      // Set screen name
      row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(id));

      // Store screen label
      row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(label));

      // Store row
      dataList.addRow(row);
      previouslyAdded.add(id);
    }
  }
}
