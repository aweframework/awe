package com.almis.awe.service.menu;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.DELETE_RESTRICTION;
import static com.almis.awe.model.constant.AweConstants.JSON_CELL_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.MENU_RESTRICTIONS;
import static com.almis.awe.model.constant.AweConstants.NEW_RESTRICTION;
import static com.almis.awe.model.constant.AweConstants.PRIVATE_MENU;
import static com.almis.awe.model.constant.AweConstants.UPDATE_RESTRICTION;

/**
 * Manages the menu restriction tree: profile restrictions and per user/profile option restrictions
 */
public class MenuRestrictionTreeService extends ServiceConfig {

  public static final String OPTION = "option";
  public static final String RESTRICTION = "restriction";
  public static final String ACCESS = "access";
  public static final String TEXT_SUCCESS = "text-success";
  public static final String PROFILE = "profile";
  private static final String RESTRICTIONS = "restrictions";
  private static final String UPDATE_CELL = "update-cell";
  private static final String MENU_OPTION_TREE = "menu-option-tree";
  private static final String OPTION_IS_LEAF = "option-is-leaf";
  private final MenuProvider menuProvider;
  private final MenuScreenResolver menuScreenResolver;
  private final QueryService queryService;
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Autowired constructor
   *
   * @param menuProvider             Menu provider
   * @param menuScreenResolver       Menu screen resolver
   * @param queryService             Query service
   * @param securityConfigProperties Security configuration properties
   */
  public MenuRestrictionTreeService(MenuProvider menuProvider, MenuScreenResolver menuScreenResolver,
                                    QueryService queryService, SecurityConfigProperties securityConfigProperties) {
    this.menuProvider = menuProvider;
    this.menuScreenResolver = menuScreenResolver;
    this.queryService = queryService;
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Retrieve the restrictions of a profile merged with the database restrictions
   *
   * @param restriction Restriction profile
   * @return Screen restriction list
   * @throws AWException Screen restriction retrieval failure
   */
  public ServiceData getScreenRestrictions(String restriction) throws AWException {
    // Step 1: Get profile restrictions
    String restrictionToSearch = restriction == null || restriction.isEmpty()
      ? securityConfigProperties.getDefaultRestriction() : restriction;
    Profile baseProfile = getElements().getProfile(restrictionToSearch).copy();

    // Step 2: Generate a datalist from base profile
    DataList baseRestrictions = new DataList();
    if (baseProfile.getRestrictions() != null) {
      for (Restriction baseRestriction : baseProfile.getRestrictions()) {
        Map<String, CellData> row = new HashMap<>();
        // Set option name
        row.put(AweConstants.JSON_OPTION, new CellData(baseRestriction.getOption()));

        // Store screen label
        row.put(AweConstants.JSON_RESTRICTED,
          new CellData(RestrictionType.R.equals(baseRestriction.getRestrictionType())));
        baseRestrictions.addRow(row);
      }
    }
    // Set records
    baseRestrictions.setRecords(baseRestrictions.getRows().size());

    // Step 3: Get database restrictions and apply profile restrictions if it doesn't exist
    ServiceData serviceData = queryService.launchPrivateQuery(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY, "1", "0");
    DataList databaseRestrictions = serviceData.getDataList();

    // Step 4: Merge datalists
    DataList result = getBean(DataListBuilder.class).addDataList(baseRestrictions).addDataList(databaseRestrictions)
      .build();

    // Set datalist to service
    serviceData.setDataList(result);
    return serviceData;
  }

  /**
   * Retrieve the private menu as a tree with the number of restrictions per option
   *
   * @return Full menu without restrictions
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuOptionTree() throws AWException {
    // Retrieve the full menu
    Menu menu = menuProvider.getMenu(PRIVATE_MENU);

    // Retrieve the number of restrictions per option and transform datalist into map
    DataList restrictionsDataList = queryService
      .launchPrivateQuery(AweConstants.RESTRICTIONS_PER_OPTION_QUERY, "1", "0").getDataList();
    Map<String, Integer> numberOfRestrictions = restrictionsDataList.getRows().stream()
      .collect(Collectors.toMap(row -> row.get(OPTION).getStringValue(), row -> row.get(RESTRICTIONS).getIntegerValue(),
        (a, b) -> a, HashMap::new));

    // Generate datalist with menu and restrictions
    List<Map<String, CellData>> menuTree = getMenuOptionsAsTree(menu.getChildrenByType(Option.class),
      numberOfRestrictions, null);

    // Return the menu with applied restrictions
    return new ServiceData().setDataList(new DataList().setRows(menuTree).setRecords(menuTree.size()));
  }

  /**
   * Retrieve the private menu of a module as a tree with the user/profile restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param module  Selected module
   * @return Full menu without restrictions
   * @throws AWException Error retrieving data
   */
  public ServiceData getMenuOptionTreeByModule(Integer user, Integer profile, String module) throws AWException {
    // Retrieve the full menu
    Menu menu = menuProvider.getMenu(PRIVATE_MENU);
    new ScreenRestrictionGenerator().applyModuleRestriction(module, menu);

    // Retrieve the number of restrictions per option and transform datalist into map
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("user", user);
    parameters.put(PROFILE, profile);
    DataList restrictionsDataList = queryService.launchPrivateQuery(MENU_RESTRICTIONS, parameters).getDataList();
    Map<String, String> restrictions = toRestrictionMap(restrictionsDataList);

    DataList previousRestrictions = new DataList();
    if (user != null) {
      previousRestrictions = queryService
        .launchPrivateQuery(AweConstants.MENU_RESTRICTIONS_WITH_USER_PROFILE, parameters).getDataList();
      parameters.set("user", null);
      previousRestrictions.getRows().addAll(queryService.launchPrivateQuery(MENU_RESTRICTIONS, parameters).getDataList()
        .getRows());
    } else if (profile != null) {
      parameters.set(PROFILE, null);
      previousRestrictions = queryService.launchPrivateQuery(MENU_RESTRICTIONS, parameters).getDataList();
    }

    Map<String, String> previousRestrictionsMap = toRestrictionMap(previousRestrictions);

    // Generate datalist with menu and restrictions
    List<Map<String, CellData>> menuTree = getMenuOptionsAsTreeWithIcons(menu.getChildrenByType(Option.class),
      restrictions, previousRestrictionsMap, null);

    // Return the menu with applied restrictions
    return new ServiceData().setDataList(new DataList().setRows(menuTree).setRecords(menuTree.size()));
  }

  /**
   * Update menu restriction tree
   *
   * @param option Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData updateMenuRestrictionTree(String option) throws AWException {
    // Retrieve restriction number
    DataList restrictionsDataList = queryService
      .launchPrivateQuery(AweConstants.RESTRICTIONS_PER_OPTION_QUERY, "1", "0").getDataList();
    CellData restrictions = restrictionsDataList.getRows().stream().findFirst().map(row -> row.get(RESTRICTIONS))
      .orElse(new CellData(0));

    // Store restrictions
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("data", JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, restrictions.getIntegerValue())
      .put(JSON_LABEL_PARAMETER, restrictions.getIntegerValue())
      .put(JSON_CELL_STYLE_PARAMETER, restrictions.getIntegerValue() > 0 ? TEXT_SUCCESS : "text-light-gray"));

    // Generate update-cell action
    return new ServiceData().addClientAction(new ClientAction()
      .setAddress(new ComponentAddress("report", MENU_OPTION_TREE, option, "number-of-restrictions"))
      .setType(UPDATE_CELL)
      .setParameterMap(parameters));
  }

  /**
   * Allow menu option with restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData allowMenuOption(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "A");
  }

  /**
   * Restrict menu option with restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData restrictMenuOption(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "R");
  }

  /**
   * Remove menu option restriction
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData removeRestriction(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "");
  }

  /**
   * Insert, update or delete the restriction of an option
   *
   * @param user             Selected user
   * @param profile          Selected profile
   * @param option           Selected option
   * @param restrictionValue Restriction value (A, R or empty to delete)
   * @return Service data
   * @throws AWException Error retrieving data
   */
  private ServiceData changeMenuOptionRestriction(Integer user, Integer profile, String option,
                                                  String restrictionValue) throws AWException {
    // Retrieve previous restriction (if exists)
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("user", user);
    parameters.put(PROFILE, profile);
    parameters.put(OPTION, option);
    DataList restrictionsDataList = queryService.launchPrivateQuery(MENU_RESTRICTIONS, parameters).getDataList();
    Optional<Map<String, CellData>> restriction = restrictionsDataList.getRows().stream().findFirst();

    // Update or delete
    parameters.put(RESTRICTION, restrictionValue);
    parameters.put("active", 1);

    // Get maintain service bean
    MaintainService maintainService = getBean(MaintainService.class);

    if (restriction.isPresent()) {
      if (restrictionValue.isEmpty()) {
        // Delete
        maintainService.launchPrivateMaintain(DELETE_RESTRICTION, parameters);
      } else {
        // Update
        maintainService.launchPrivateMaintain(UPDATE_RESTRICTION, parameters);
      }
    } else {
      // Insert
      maintainService.launchPrivateMaintain(NEW_RESTRICTION, parameters);
    }

    // Store restrictions
    Map<String, Object> restrictionIcon = new HashMap<>();
    restrictionIcon.put("data", JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, restrictionValue)
      .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(restrictionValue)))
      .put(JSON_ICON_PARAMETER, getRestrictionIcon(restrictionValue))
      .put(JSON_STYLE_PARAMETER, getRestrictionStyle(restrictionValue)));

    // Generate update-cell action
    return new ServiceData().addClientAction(new ClientAction()
      .setAddress(new ComponentAddress("report", MENU_OPTION_TREE, option, RESTRICTION))
      .setType(UPDATE_CELL)
      .setParameterMap(restrictionIcon));
  }

  private static Map<String, String> toRestrictionMap(DataList restrictions) {
    return restrictions.getRows().stream()
      .collect(Collectors.toMap(row -> row.get(OPTION).getStringValue(), row -> row.get(RESTRICTION).getStringValue(),
        (a, b) -> a, HashMap::new));
  }

  private List<Map<String, CellData>> getMenuOptionsAsTree(List<Option> options,
                                                           Map<String, Integer> numberOfRestrictions,
                                                           String parentId) {
    List<Map<String, CellData>> listOptions = options.stream()
      .map(option -> {
        Map<String, CellData> row = new HashMap<>();
        getMenuOptionTreeCellData(parentId, option, row);
        row.put("module-name", new CellData(option.getModule()));
        int restrictions = Optional.ofNullable(numberOfRestrictions.get(option.getName())).orElse(0);
        row.put("number-of-restrictions", new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, restrictions)
          .put(JSON_LABEL_PARAMETER, restrictions)
          .put(JSON_CELL_STYLE_PARAMETER, restrictions > 0 ? TEXT_SUCCESS : "text-light-gray")));
        row.put(OPTION_IS_LEAF, new CellData(option.getOptions().isEmpty() ? "true" : "false"));
        return row;
      })
      .collect(Collectors.toList());

    // Add children
    listOptions.addAll(options.stream()
      .filter(option -> !option.getOptions().isEmpty())
      .flatMap(option -> getMenuOptionsAsTree(option.getOptions(), numberOfRestrictions, option.getName()).stream())
      .toList()
    );

    return listOptions;
  }

  private void getMenuOptionTreeCellData(String parentId, Option option, Map<String, CellData> row) {
    ObjectNode optionNode = JsonNodeFactory.instance.objectNode();
    optionNode.put(JSON_VALUE_PARAMETER, option.getName());
    optionNode.put(JSON_ICON_PARAMETER, Optional.ofNullable(option.getIcon())
      .orElse(option.isSeparator() ? "minus fa-rotate-90" : null));
    optionNode.put(JSON_LABEL_PARAMETER, getLocale(Optional.ofNullable(option.getLabel())
      .orElse(Optional.ofNullable(option.getScreen())
        .map(screen -> {
          try {
            return menuScreenResolver.getScreen(screen);
          } catch (Exception exc) {
            return null;
          }
        })
        .map(Element::getLabel)
        .orElse(option.isSeparator() ? getLocale("ELEMENT_TYPE_CONTEXTSEPARATOR") : "NO LABEL")))
      + " (" + option.getName() + ")");

    row.put("option-id", new CellData(option.getName()));
    row.put("parent-option", new CellData(parentId));
    row.put("option-name", new CellData(optionNode));
  }

  private List<Map<String, CellData>> getMenuOptionsAsTreeWithIcons(List<Option> options,
                                                                    Map<String, String> restrictions,
                                                                    Map<String, String> previousRestrictions,
                                                                    String parentId) {
    List<Map<String, CellData>> listOptions = options.stream()
      .filter(option -> !option.isRestricted())
      .map(option -> {
        Map<String, CellData> row = new HashMap<>();
        getMenuOptionTreeCellData(parentId, option, row);
        String status = Optional.ofNullable(previousRestrictions.get(option.getName())).orElse("A");
        row.put(ACCESS, new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, status)
          .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(status)))
          .put(JSON_ICON_PARAMETER, getRestrictionIcon(status))
          .put(JSON_STYLE_PARAMETER, "")));
        String restriction = Optional.ofNullable(restrictions.get(option.getName())).orElse("");
        row.put(RESTRICTION, new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, restrictions.get(option.getName()))
          .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(restriction)))
          .put(JSON_ICON_PARAMETER, getRestrictionIcon(restriction))
          .put(JSON_STYLE_PARAMETER, getRestrictionStyle(restriction))));
        row.put(OPTION_IS_LEAF, new CellData(option.getOptions().isEmpty() ? "true" : "false"));
        return row;
      })
      .collect(Collectors.toList());

    // Add children
    listOptions.addAll(options.stream()
      .filter(option -> !option.getOptions().isEmpty())
      .flatMap(option -> getMenuOptionsAsTreeWithIcons(option.getOptions(), restrictions, previousRestrictions,
        option.getName()).stream())
      .toList()
    );

    return listOptions;
  }

  private String getRestrictionLabel(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "ENUM_REST_MODE_RESTRICTED";
      case "A" -> "ENUM_REST_MODE_ALLOW";
      default -> "";
    };
  }

  private String getRestrictionIcon(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "fa-minus-circle";
      case "A" -> "fa-check-circle-o";
      default -> "";
    };
  }

  private String getRestrictionStyle(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "text-danger";
      case "A" -> TEXT_SUCCESS;
      default -> "";
    };
  }
}
