package com.almis.awe.service.user;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.user.PredefinedProfiles.PredefinedProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import tools.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Creation at startup of the predefined profiles Reader ({@code RDR}) and Contributor ({@code CTB}). Their permissions
 * are in their profile files (reader.xml and contributor.xml), so only the profile rows are created, and only when no
 * profile with their acronym exists. Switched off with {@code awe.security.permissions.predefined-profiles=false}
 */
@Slf4j
public class PredefinedProfileSeed {

  private static final String PROFILE_QUERY = "profileByAcronym";
  private static final String MAX_KEY_QUERY = "profileMaxKey";
  private static final String SEQUENCE_KEY_QUERY = "profileSequenceKey";
  private static final String CREATE_MAINTAIN = "PredefinedProNew";
  private static final String PROFILE_ID = "IdePro";

  private final SecurityConfigProperties securityConfigProperties;
  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   * @param queryService             Query service
   * @param maintainService          Maintain service
   * @param queryUtil                Query utilities
   */
  public PredefinedProfileSeed(SecurityConfigProperties securityConfigProperties, QueryService queryService,
                               MaintainService maintainService, QueryUtil queryUtil) {
    this.securityConfigProperties = securityConfigProperties;
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
  }

  /**
   * Create the predefined profiles that do not exist. A profile with the same acronym is kept and logged as a
   * warning; a failure is logged and never stops the startup
   */
  @EventListener(ApplicationReadyEvent.class)
  public void createPredefinedProfiles() {
    if (!securityConfigProperties.getPermissions().isPredefinedProfiles()) {
      return;
    }
    for (PredefinedProfile profile : PredefinedProfiles.PROFILES) {
      try {
        create(profile);
      } catch (AWException | RuntimeException exc) {
        log.warn("The predefined profile {} could not be created: {}", profile.acronym(), exc.getMessage());
      }
    }
  }

  private void create(PredefinedProfile profile) throws AWException {
    ObjectNode search = queryUtil.getParameters();
    search.put("Acr", profile.acronym());
    if (!queryService.launchPrivateQuery(PROFILE_QUERY, search).getDataList().getRows().isEmpty()) {
      log.warn("A profile with acronym {} already exists: the predefined profile {} is not created",
        profile.acronym(), profile.name());
      return;
    }

    // The next identifier of the profiles and of ProKey, so neither a profile inserted with its own identifier nor a
    // later ProNew collides with it
    int id = Math.max(readKey(MAX_KEY_QUERY, PROFILE_ID), readKey(SEQUENCE_KEY_QUERY, "KeyVal")) + 1;
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(PROFILE_ID, id);
    parameters.put("Acr", profile.acronym());
    parameters.put("Nam", profile.name());
    parameters.put("Res", profile.file());
    maintainService.launchPrivateMaintain(CREATE_MAINTAIN, parameters);
    log.info("Predefined profile {} ({}) created with id {}", profile.acronym(), profile.name(), id);
  }

  private int readKey(String query, String column) throws AWException {
    return queryService.launchPrivateQuery(query, queryUtil.getParameters()).getDataList().getRows().stream()
      .map(row -> row.get(column))
      .filter(Objects::nonNull)
      .map(CellData::getIntegerValue)
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(0);
  }
}
