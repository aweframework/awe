package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWESessionException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Message;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.TagList;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.model.event.ScreenChangeEvent;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.service.observation.ExecutionObserver;
import com.almis.awe.service.screen.DeferredComponents;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.NO_KEY;

/**
 * Manage AWE screen access
 */
@Slf4j
public class ScreenService extends ServiceConfig {

  // Parameters sent by the client when a deferred container is opened: components with data and with a selected value
  private static final String DEFERRED_LOADED_PARAMETER = "deferredLoaded";
  private static final String DEFERRED_VALUED_PARAMETER = "deferredValued";
  // Client action sent when the initial load of a deferred container failed: the client must ask for it again
  private static final String DEFERRED_FAILED_ACTION = "deferred-failed";

  // Autowired services
  private final MenuService menuService;
  private final MaintainService maintainService;
  private final ScreenComponentGenerator screenComponentGenerator;
  private final ApplicationEventPublisher eventPublisher;
  private final TemplateService templateService;

  // Observer of the screens built (metrics, traces...). Optional: without one the screen is just built
  private ExecutionObserver executionObserver = ExecutionObserver.NONE;

  /**
   * Autowired constructor
   *
   * @param menuService              Menu service
   * @param maintainService          Maintain service
   * @param templateService          Template service
   * @param screenComponentGenerator Screen component generator
   * @param eventPublisher           Event publisher
   */
  public ScreenService(MenuService menuService, MaintainService maintainService, TemplateService templateService, ScreenComponentGenerator screenComponentGenerator, ApplicationEventPublisher eventPublisher) {
    this.menuService = menuService;
    this.maintainService = maintainService;
    this.templateService = templateService;
    this.screenComponentGenerator = screenComponentGenerator;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Set the observer of the screens built
   *
   * @param executionObserver Execution observer (optional, defined by the application when it needs one)
   */
  @Autowired(required = false)
  public void setExecutionObserver(ExecutionObserver executionObserver) {
    this.executionObserver = executionObserver == null ? ExecutionObserver.NONE : executionObserver;
  }

  /**
   * Generates an screen data
   *
   * @return Screen data
   */
  public ServiceData getScreenDataAction() {
    String optionId = this.getRequest().getParameterAsString(AweConstants.PARAMETER_OPTION);
    ScreenData screenData;
    try {
      screenData = generateScreenData(optionId, true);
    } catch (AWESessionException exc) {
      screenData = generateScreenDataError(exc);
      screenData.addError(exc);
    } catch (AWException exc) {
      screenData = generateScreenDataError(exc);
    }

    // Store screen data
    ServiceData serviceData = new ServiceData();
    serviceData.addVariable(AweConstants.ACTION_SCREEN_DATA, new CellData(screenData));

    return serviceData;
  }

  /**
   * Generates the data of a deferred dialog or tab page when it is opened for the first time: it runs the initial
   * load of its components and returns their models as initial-model client actions.
   * The container is the target action, the screen is identified by the option parameter.
   *
   * @return Client actions which set the initial model of the components of the container
   * @throws AWException Container not found or error retrieving the data
   */
  public ServiceData getDeferredDataAction() throws AWException {
    String containerId = getRequest().getTargetAction();
    String optionId = getRequest().getParameterAsString(AweConstants.PARAMETER_OPTION);

    // Get the screen and the deferred container
    Menu menu = menuService.getMenu();
    Screen screen = getScreenFromOptionId(optionId, menu);
    Element container = DeferredComponents.findDeferredContainer(screen, containerId);
    if (container == null) {
      throw new AWENotFoundException(getLocale("ERROR_TITLE_SCREEN_GENERATION_ERROR"), getLocale("ERROR_MESSAGE_DEFERRED_CONTAINER_NOT_FOUND", containerId, screen.getId()));
    }

    // Components which got a value before the first opening keep it
    Set<String> loaded = getRequestIdentifiers(DEFERRED_LOADED_PARAMETER);
    Set<String> valued = getRequestIdentifiers(DEFERRED_VALUED_PARAMETER);

    // Run the initial load of the container components
    ScreenData data = new ScreenData();
    Map<String, ScreenComponent> componentMap = screenComponentGenerator.generateDeferredComponentMap(screen, container, data, menu, getScreenParameters(optionId), loaded);

    // Generate the actions which apply the loaded models
    ServiceData serviceData = new ServiceData();
    for (ScreenComponent component : componentMap.values()) {
      if (component.getController().getInitialLoad() != null) {
        addDeferredModelAction(serviceData, component, valued.contains(component.getId()));
      }
    }
    data.getActions().forEach(serviceData::addClientAction);

    // A failed initial load is shown as an error message, and the container is flagged as not loaded so the client tries again
    if (data.getActions().stream().anyMatch(ScreenService::isErrorMessage)) {
      serviceData.addClientAction(new ClientAction(DEFERRED_FAILED_ACTION).setTarget(containerId).setAsync(Boolean.TRUE).setSilent(Boolean.TRUE));
    }
    return serviceData;
  }

  /**
   * Check whether a client action is an error message. Only an explicit warning, information or ok message is not a
   * failure: a message without a known type counts as an error
   *
   * @param action Client action
   * @return The action is an error message
   */
  private static boolean isErrorMessage(ClientAction action) {
    if (!AweConstants.MESSAGE.equals(action.getType())) {
      return false;
    }
    Object type = action.getParameters().get(AweConstants.ACTION_MESSAGE_TYPE_ATTRIBUTE);
    String typeValue = switch (type) {
      case CellData cellData -> cellData.getStringValue();
      case String text -> text;
      case null, default -> null;
    };
    return Stream.of(AnswerType.WARNING, AnswerType.INFO, AnswerType.OK)
      .noneMatch(harmless -> harmless.toString().equalsIgnoreCase(typeValue));
  }

  /**
   * Retrieve a list of component identifiers sent in a request parameter
   *
   * @param parameter Parameter name
   * @return Identifiers
   */
  private Set<String> getRequestIdentifiers(String parameter) {
    Set<String> identifiers = new HashSet<>();
    JsonNode node = getRequest().getParameter(parameter);
    if (node != null && node.isArray()) {
      node.forEach(item -> identifiers.add(item.asText()));
    }
    return identifiers;
  }

  /**
   * Add the client action which applies the loaded model of a component, as the screen load would have done
   *
   * @param serviceData Service data
   * @param component   Loaded component
   * @param keepValue   The component already has a selected value, which must not be replaced
   */
  private void addDeferredModelAction(ServiceData serviceData, ScreenComponent component, boolean keepValue) {
    ComponentModel model = component.getModel();
    DataList dataList = new DataList();
    dataList.setRows(model.getValues());
    dataList.setPage(model.getPage());
    dataList.setTotal(model.getTotal());
    dataList.setRecords(model.getRecords());

    ClientAction action = new ClientAction("initial-model")
      .setTarget(component.getId())
      .addParameter("datalist", dataList)
      .setAsync(Boolean.TRUE);
    if (!keepValue) {
      action.addParameter("selected", new CellData(model.getSelected()))
        .addParameter("defaultValues", new CellData(model.getDefaultValues()));
    }
    serviceData.addClientAction(action);
  }

  /**
   * Generates an empty screen
   *
   * @return Empty screen
   */
  public String generateEmptyScreen() {
    // Get empty screen
    return AweConstants.EMPTY_TEMPLATE;
  }

  /**
   * Generates an screen data
   *
   * @param optionId Option identifier
   * @param generateTemplate Generate template
   * @return Screen data
   * @throws AWException Screen data generation failed
   */
  public ScreenData generateScreenData(String optionId, boolean generateTemplate) throws AWException {
    // Get menu once to avoid redundant loads
    Menu menu = menuService.getMenu();

    // Get screen
    Screen screen = getScreenFromOptionId(optionId, menu);

    // Observe the build once the option is known to be in the menu, so a forged option never becomes a tag
    String screenId = screen.getId();
    return executionObserver.observe(ExecutionObserver.ExecutionType.SCREEN, optionId == null ? screenId : optionId,
      Map.of("screen", screenId == null ? "undefined" : screenId), () -> {
        // Initialize screen
        initializeScreenNavigation(screen, optionId);

        // Generate map with model and controller data
        return getScreenData(screen, menu, optionId, generateTemplate);
      });
  }

  /**
   * Retrieve screen from optionId
   *
   * @param optionId Option id
   * @return Screen
   * @throws AWException Error retrieving screen
   */
  private Screen getScreenFromOptionId(String optionId, Menu menu) throws AWException {
    return optionId == null ? menuService.getDefaultScreen(menu) : menuService.getAvailableOptionScreen(optionId, menu);
  }

  /**
   * Initialize screen navigation
   *
   * @param screen   Screen navigation
   * @param optionId Option id
   * @throws AWException Error initializing screen
   */
  private void initializeScreenNavigation(Screen screen, String optionId) throws AWException {
    // Store current screen
    storeCurrentScreen(optionId == null ? screen.getId() : optionId, screen);

    // Store last screen criteria
    storeLastScreenCriteria();

    // Track navigation
    AweClientTracker clientTracker = getBean(AweClientTracker.class);
    clientTracker.navigateToScreen(optionId);

    // Publish screen change
    if (getSession() != null && getSession().getUser() != null) {
      eventPublisher.publishEvent(new ScreenChangeEvent(this, getSession().getUser(), getRequest().getToken(), optionId));
    }

    // Launch on load
    launchScreenOnLoadEvent(screen);
  }

  /**
   * Retrieve screen data
   * @param generateTemplate Generate template
   *
   * @return Screen data
   */
  public ScreenData getScreenData(boolean generateTemplate) throws AWException {
    return getScreenData(null, generateTemplate);
  }

  /**
   * Retrieve an option screen data
   *
   * @param optionId Option id
   * @param generateTemplate Generate template for awe-angular
   * @return Screen data
   */
  public ScreenData getScreenData(String optionId, boolean generateTemplate) throws AWException {
      return generateScreenData(optionId, generateTemplate);
  }

  /**
   * Retrieve error screen data
   *
   * @param errorOptionId Error option id
   * @param generateTemplate Generate template for awe-angular
   * @return Screen data
   */
  public ScreenData getErrorScreenData(String errorOptionId, boolean generateTemplate) {
    try {
      return getScreenData(errorOptionId, generateTemplate);
    } catch (Exception exc) {
      log.error("Error generating error screen data", exc);
      return null;
    }
  }

  /**
   * Generates screen data
   *
   * @param screen   Screen object
   * @param optionId Option identifier
   * @param generateTemplate Generate template
   * @return Screen data
   * @throws AWException Error retrieving menu
   */
  private ScreenData getScreenData(Screen screen, Menu menu, String optionId, boolean generateTemplate) throws AWException {
    ScreenData data = new ScreenData();

    // If criteria are cached, add them to parameter map
    ObjectNode storedParameters = getScreenParameters(optionId);

    // Important! Move first the parameter list over the stored parameters to avoid losing view, option and screen parameters
    updateParameterList(storedParameters, screen);

    // Add screen information to screen data
    addScreenInformationToScreenData(screen, data, optionId);

    // Add messages to screen data
    addMessagesToScreenData(screen, data);

    // Generate component map with initial load data
    Map<String, ScreenComponent> componentMap = screenComponentGenerator.generateComponentMap(screen, data, menu, storedParameters);

    // Add components to screen data
    addComponentsToScreenData(data, componentMap);

    if (generateTemplate) {
      // Add template to screen data
      addTemplateToScreenData(screen, menu, data, getRequest().getParameterAsString("view"), optionId);
    } else {
      data.setStructure(screen);
    }

    return data;
  }

  /**
   * Update parameter list with stored parameters
   *
   * @param storedParameters Stored parameters
   */
  private void updateParameterList(ObjectNode storedParameters, Screen screen) {
    // Add screen name
    getRequest().setParameter(AweConstants.JSON_SCREEN, screen.getId());

    ObjectNode requestParameters = getRequest().getParameterList();
    List<String> baseAttributes = Arrays.asList(AweConstants.PARAMETER_VIEW,
      AweConstants.PARAMETER_OPTION, AweConstants.JSON_SCREEN);

    // Keep basic values in stored parameters
    for (String attribute : baseAttributes) {
      if (requestParameters.has(attribute)) {
        storedParameters.set(attribute, requestParameters.get(attribute));
      }
    }

    // Update request parameters
    getRequest().setParameterList(storedParameters);
  }

  /**
   * Add components to Screen Data
   *
   * @param data         Screen data
   * @param componentMap Component map
   */
  private void addComponentsToScreenData(ScreenData data, Map<String, ScreenComponent> componentMap) {
    // Store component list
    for (ScreenComponent component : componentMap.values()) {
      // Add component
      data.addComponent(component);
    }
  }

  /**
   * Add messages to Screen Data
   *
   * @param screen Screen object
   * @param data   Screen data
   */
  private void addMessagesToScreenData(Screen screen, ScreenData data) {
    // Generate messages
    List<Message> messages = screen.getElementsByType(Message.class);
    for (Message message : messages) {
      // Add controller
      data.addMessage(message.getElementKey(), message);
    }
  }

  /**
   * Add screen info to Screen Data
   *
   * @param screen   Screen object
   * @param data     Screen data
   * @param optionId Option identifier
   */
  private void addScreenInformationToScreenData(Screen screen, ScreenData data, String optionId) {
    if (screen.getLabel() != null) {
      data
        .addScreenProperty("title", screen.getLabel())
        .addScreenProperty("name", screen.getId())
        .addScreenProperty("option", optionId);
      // Add Unload
      if (screen.getOnUnload() != null) {
        data.addScreenProperty("onunload", screen.getOnUnload());
      }
    }
  }

  /**
   * Add template to screen data
   *
   * @param screen   Screen object
   * @param menu     Menu
   * @param data     Screen data
   * @param view      View
   * @param optionId  Option identifier
   */
  private void addTemplateToScreenData(Screen screen, Menu menu, ScreenData data, String view, String optionId) throws AWException {
    data.setTemplate(templateService.generateScreenTemplate(screen, menu, view, optionId));
  }

  /**
   * Generates screen data
   *
   * @param exc AWException
   * @return data
   */
  private ScreenData generateScreenDataError(AWException exc) {
    ScreenData data = new ScreenData();

    // Log error
    log.error("{} - {}", exc.getTitle(), exc.getMessage(), exc);

    // Generate screen properties
    return data
      .addScreenProperty("title", exc.getTitle())
      .addScreenProperty("name", AweConstants.ERROR_SCREEN)
      .addScreenProperty("option", AweConstants.ERROR_OPTION);
  }

  /**
   * Launches screen on load event
   *
   * @param screen Screen
   * @throws AWException Error launching on load event
   */
  private void launchScreenOnLoadEvent(Screen screen) throws AWException {
    // Launch on load maintain if defined
    if (screen.getOnLoad() != null) {
      maintainService.launchPrivateMaintain(screen.getOnLoad());
    }
  }

  /**
   * Stores the last screen criteria if keep last screen criteria is true
   */
  private void storeLastScreenCriteria() {
    AweSession session = getSession();
    if (session.isAuthenticated()) {
      String lastScreen = session.getParameter(String.class, AweConstants.SESSION_LAST_SCREEN);

      // Get last screen if stored
      if (lastScreen != null) {
        // Set parameters to get'em cached
        setScreenParameters(lastScreen);
      }
    }
  }

  /**
   * Retrieve screen parameters and add them to cache
   *
   * @param optionName Option name
   * @return Option parameters
   */
  private ObjectNode getScreenParameters(String optionName) {
    ObjectNode screenParameters;
    AweSession session = getSession();
    String keepCriteriaKey = AweConstants.SESSION_KEEP_CRITERIA_HEADER + optionName;
    if (optionName != null) {
      try {
        screenParameters = (ObjectNode) session.getParameter(keepCriteriaKey);
        screenParameters = screenParameters == null ? JsonNodeFactory.instance.objectNode() : screenParameters;
      } catch (Exception exc) {
        screenParameters = JsonNodeFactory.instance.objectNode();
      }
    } else {
      screenParameters = JsonNodeFactory.instance.objectNode();
    }

    // Log screen parameters
    log.info("Screen parameters retrieved - {} - {}", keepCriteriaKey, screenParameters.toString());
    return screenParameters;
  }

  /**
   * Retrieve screen parameters and add them to cache
   *
   * @param optionName Option name
   */
  private void setScreenParameters(String optionName) {
    AweSession session = getSession();
    // Log screen parameters
    if (session.isAuthenticated()) {
      String keepCriteriaKey = AweConstants.SESSION_KEEP_CRITERIA_HEADER + optionName;
      ObjectNode screenParameters = getRequest().getParametersSafe();
      session.setParameter(keepCriteriaKey, screenParameters);
      log.info("Screen parameters set - {} - {}", keepCriteriaKey, screenParameters.toString());
    }
  }

  /**
   * Stores into session the current screen name
   *
   * @param option option name
   */
  private void storeCurrentScreen(String option, Screen screen) {
    AweSession session = getSession();

    if (session.isAuthenticated()) {
      // Store last screen in session
      session.setParameter(AweConstants.SESSION_LAST_SCREEN, session.getParameter(AweConstants.SESSION_STORE_SCREEN_CRITERIA));

      // Store option name in session
      session.setParameter(AweConstants.SESSION_CURRENT_SCREEN, option);

      // Store screen option if keep criteria
      session.setParameter(AweConstants.SESSION_STORE_SCREEN_CRITERIA, screen.isKeepCriteria() ? option : null);
    }
  }

  /**
   * Get screen component list names
   *
   * @param screenId Screen identifier
   * @param suggest  Written suggest
   * @return Screen component list
   * @throws AWException Error retrieving screen element list
   */
  public ServiceData getScreenElementList(@NonNull String screenId, String suggest) throws AWException {
    // Get screen from option
    Screen screen = menuService.getScreen(screenId);
    List<Component> componentList = screen.getElementsByType(Component.class);
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (Component component : componentList) {
      if (!NO_KEY.equalsIgnoreCase(component.getElementKey())) {
        Map<String, CellData> row = new HashMap<>();

        String componentLabel = component.getLabel() == null ? component.getElementKey() : getLocale(component.getLabel()) + " (" + component.getElementKey() + ")";

        // Add component if matches with component id or locale
        if (StringUtil.containsIgnoreCase(componentLabel, suggest.trim())) {
          // Set screen name
          row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(component.getElementKey()));

          // Store screen label
          row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(componentLabel));

          // Store row
          dataList.addRow(row);
        }
      }
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Get screen attribute list names
   *
   * @param suggest Written suggest
   * @return Screen component list
   * @throws AWException Error retrieving screen element list
   */
  public ServiceData getAttributeNameList(String suggest) throws AWException {
    // Get screen from option
    EnumeratedGroup attributeList = getElements().getEnumerated(AweConstants.ATTRIBUTE_LIST).copy();
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (Global option : attributeList.getOptionList()) {
      Map<String, CellData> row = new HashMap<>();

      String attributeLabel = getLocale(option.getLabel());

      // Add component if matches with component id or locale
      if (StringUtil.containsIgnoreCase(attributeLabel, suggest.trim())) {
        // Set screen name
        row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(option.getValue()));

        // Store screen label
        row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(attributeLabel));

        // Store row
        dataList.addRow(row);
      }
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Generate taglist data
   *
   * @param option    Option
   * @param tagListId TagList
   * @return Taglist data
   */
  public ServiceData getTaglistData(String option, String tagListId) throws AWException {
    TagList tagList;
    Menu menu = menuService.getMenu();
    if (option == null) {
      tagList = templateService.getTagList(menu, tagListId);
    } else {
      tagList = templateService.getTagList(menu, option, tagListId);
    }

    // Get taglist data
    ServiceData serviceData = templateService.loadTagListData(tagList);

    // Generate rendered taglist as object
    List<Element> elementList = screenComponentGenerator.generateTagListElements(templateService.generateTaglistTemplate(tagList, serviceData.getDataList()));

    // Generate component map
    Map<String, ScreenComponent> componentMap = screenComponentGenerator.generateTagListComponentMap(tagListId, elementList);

    // Generate service data
    return serviceData
      .addClientAction(new ClientAction("taglist-data")
        .addParameter("components", componentMap)
        .addParameter("html", templateService.renderTagList(elementList)));
  }
}
