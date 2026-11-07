package com.almis.awe.service.user;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Creation at startup of the predefined profiles Reader (RDR) and Contributor (CTB)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredefinedProfileSeedTest {

  @Mock
  private QueryService queryService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;

  private final SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
  private final Logger logger = (Logger) LoggerFactory.getLogger(PredefinedProfileSeed.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private PredefinedProfileSeed seed;

  @BeforeEach
  void setUp() throws Exception {
    seed = new PredefinedProfileSeed(securityConfigProperties, queryService, maintainService, queryUtil);
    when(queryUtil.getParameters()).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    existing();
    keys(5, 9);
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  private void existing(String... acronyms) throws AWException {
    when(queryService.launchPrivateQuery(eq("profileByAcronym"), any(ObjectNode.class))).thenAnswer(invocation -> {
      String acronym = invocation.<ObjectNode>getArgument(1).get("Acr").asString();
      DataList list = new DataList();
      if (List.of(acronyms).contains(acronym)) {
        DataListUtil.addColumnWithOneRow(list, "IdePro", 3);
      }
      return new ServiceData().setDataList(list);
    });
  }

  private void keys(int maxProfile, int sequence) throws AWException {
    DataList max = new DataList();
    DataListUtil.addColumnWithOneRow(max, "IdePro", maxProfile);
    DataList key = new DataList();
    DataListUtil.addColumnWithOneRow(key, "KeyVal", sequence);
    when(queryService.launchPrivateQuery(eq("profileMaxKey"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(max));
    when(queryService.launchPrivateQuery(eq("profileSequenceKey"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(key));
  }

  private List<ObjectNode> createdProfiles() throws AWException {
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(2)).launchPrivateMaintain(eq("PredefinedProNew"), parameters.capture());
    return parameters.getAllValues();
  }

  private List<String> messages(Level level) {
    return appender.list.stream().filter(event -> event.getLevel() == level)
      .map(ILoggingEvent::getFormattedMessage).toList();
  }

  /**
   * An absent profile is created with the next identifier of the highest profile and of ProKey (the target moves
   * ProKey to it), named after the predefined profile and with its profile file
   */
  @Test
  void absentProfilesAreCreatedWithTheNextIdentifierOfTheProfilesAndTheSequence() throws Exception {
    seed.createPredefinedProfiles();

    List<ObjectNode> created = createdProfiles();
    assertThat(created).extracting(node -> node.get("Acr").asString()).containsExactly("RDR", "CTB");
    assertThat(created).extracting(node -> node.get("Nam").asString()).containsExactly("Reader", "Contributor");
    assertThat(created).extracting(node -> node.get("Res").asString()).containsExactly("reader", "contributor");
    assertThat(created.getFirst().get("IdePro").asInt()).isEqualTo(10);
    assertThat(messages(Level.INFO)).anyMatch(message -> message.contains("RDR") && message.contains("10"));
  }

  /**
   * The identifier is the next of the highest profile when profiles have been inserted past the sequence
   */
  @Test
  void theIdentifierFollowsTheHighestProfileWhenItIsPastTheSequence() throws Exception {
    keys(40, 2);

    seed.createPredefinedProfiles();

    assertThat(createdProfiles().getFirst().get("IdePro").asInt()).isEqualTo(41);
  }

  /**
   * A profile with the acronym of a predefined profile is kept: the predefined profile is not created and a warning is
   * logged
   */
  @Test
  void anExistingAcronymSkipsTheProfileWithAWarning() throws Exception {
    existing("RDR");

    seed.createPredefinedProfiles();

    verify(maintainService).launchPrivateMaintain(eq("PredefinedProNew"),
      argThat((ObjectNode node) -> "CTB".equals(node.get("Acr").asString())));
    verify(maintainService, never()).launchPrivateMaintain(eq("PredefinedProNew"),
      argThat((ObjectNode node) -> "RDR".equals(node.get("Acr").asString())));
    assertThat(messages(Level.WARN)).singleElement().asString().contains("RDR").contains("already exists");
  }

  /**
   * A failure is logged and never stops the startup; the next predefined profile is still created
   */
  @Test
  void aFailureIsLoggedAndNeverStopsTheStartup() throws Exception {
    when(maintainService.launchPrivateMaintain(eq("PredefinedProNew"),
      argThat((ObjectNode node) -> node != null && "RDR".equals(node.get("Acr").asString()))))
      .thenThrow(new AWException("error", "duplicated name"));

    assertThatCode(seed::createPredefinedProfiles).doesNotThrowAnyException();

    assertThat(createdProfiles()).extracting(node -> node.get("Acr").asString()).containsExactly("RDR", "CTB");
    assertThat(messages(Level.WARN)).singleElement().asString().contains("RDR").contains("duplicated name");
  }

  /**
   * The seed is switched off with awe.security.permissions.predefined-profiles=false
   */
  @Test
  void theSeedCanBeSwitchedOff() throws Exception {
    securityConfigProperties.getPermissions().setPredefinedProfiles(false);

    seed.createPredefinedProfiles();

    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }
}
