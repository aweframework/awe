package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.type.LoadType;
import com.almis.awe.service.QueryService;
import org.springframework.scheduling.annotation.Async;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.almis.awe.model.type.LoadType.ENUM;
import static com.almis.awe.model.type.LoadType.MENU;

/*
 * File Imports
 */

/**
 * Initial load runner
 * Launches initial load values
 *
 * @author Pablo GARCIA - 20/MAR/2017
 */
public class InitialLoadDao {

  // Load types that launch no query of the screen: enumerated values and the menu restrictions of the framework.
  // Every other load (also one without type) launches a query for the client, so it is checked
  private static final Set<LoadType> UNCHECKED_LOAD_TYPES = EnumSet.of(ENUM, MENU);

  // Autowired services
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public InitialLoadDao(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Launch initial load runner
   *
   * @param initializationData Initialization data
   * @return Future service data
   * @throws AWException AWE exception
   */
  @Async("threadPoolTaskExecutor")
  public Future<ServiceData> launchInitialLoad(AweThreadInitialization initializationData) throws AWException {
    return launch(initializationData, true);
  }

  /**
   * Launch an initial load that the server runs for itself, not for the client of the request, as the values a screen
   * definition shared by every user stores once. It is not checked, as a private query launched from Java
   *
   * @param initializationData Initialization data
   * @return Future service data
   * @throws AWException AWE exception
   */
  @Async("threadPoolTaskExecutor")
  public Future<ServiceData> launchServerInitialLoad(AweThreadInitialization initializationData) throws AWException {
    return launch(initializationData, false);
  }

  /**
   * Launch an initial load
   *
   * @param initializationData Initialization data
   * @param clientLaunch       The load is launched for the client of the request, so its query is checked
   * @return Future service data
   * @throws AWException AWE exception
   */
  private Future<ServiceData> launch(AweThreadInitialization initializationData, boolean clientLaunch) throws AWException {
    try {
      if (ENUM.equals(initializationData.getInitialLoadType())) {
        return CompletableFuture.completedFuture(queryService.launchEnumQuery(initializationData.getTarget()));
      } else {
        // The queries of the screen are launched for the client, so they are checked as its launches
        LoadType loadType = initializationData.getInitialLoadType();
        if (clientLaunch && (loadType == null || !UNCHECKED_LOAD_TYPES.contains(loadType))) {
          queryService.authorizeClientLaunch(initializationData.getTarget());
        }
        return CompletableFuture.completedFuture(queryService.launchPrivateQuery(initializationData.getTarget(), initializationData.getParameters()));
      }
    } catch (AWException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new AWException("Error launching initial load", exc.getMessage(), exc);
    }
  }
}
