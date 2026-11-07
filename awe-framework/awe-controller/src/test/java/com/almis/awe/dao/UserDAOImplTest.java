package com.almis.awe.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDAOImplTest {

  private static final String USER = "jorgito";
  private static final String HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv";

  @Mock
  private QueryUtil queryUtil;
  @Mock
  private QueryService queryService;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweSession aweSession;
  @Mock
  private MaintainService maintainService;

  private UserDAOImpl userDAO;

  @BeforeEach
  void setUp() {
    userDAO = new UserDAOImpl(queryUtil, queryService);
    userDAO.setApplicationContext(context);
  }

  @Test
  void updatePassword_launchesTheRehashTargetWithTheValuesInTheSession() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);

    assertThat(userDAO.updatePassword(USER, HASH)).isTrue();

    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER, USER);
    order.verify(aweSession).setParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER, HASH);
    order.verify(maintainService).launchPrivateMaintain(eq(UserDAOImpl.REHASH_MAINTAIN), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER);
    order.verify(aweSession).removeParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER);
  }

  @Test
  void updatePassword_doesNotPassTheValuesAsRequestParameters() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);

    userDAO.updatePassword(USER, HASH);

    verify(maintainService).launchPrivateMaintain(eq("UsrRehashPwd"), org.mockito.ArgumentMatchers.<ObjectNode>argThat(ObjectNode::isEmpty));
  }

  @Test
  void updatePassword_whenTheMaintainFails_clearsTheSessionAndReturnsFalse() throws Exception {
    when(context.getBean(AweSession.class)).thenReturn(aweSession);
    when(context.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenThrow(new AWException("error", "value too long"));

    assertThat(userDAO.updatePassword(USER, HASH)).isFalse();

    verify(aweSession).removeParameter(UserDAOImpl.REHASH_USER_SESSION_PARAMETER);
    verify(aweSession).removeParameter(UserDAOImpl.REHASH_VALUE_SESSION_PARAMETER);
  }

  @Test
  void updatePassword_withoutSession_returnsFalseAndLaunchesNothing() throws Exception {
    when(context.getBean(AweSession.class)).thenThrow(new IllegalStateException("no session"));

    assertThat(userDAO.updatePassword(USER, HASH)).isFalse();

    verify(context, never()).getBean(MaintainService.class);
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @Test
  void findProfileId_returnsTheIdOfTheProfileWithThatName() throws Exception {
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    DataList profiles = new DataList();
    DataListUtil.addColumnWithOneRow(profiles, "IdePro", 7);
    when(queryService.launchPrivateQuery(eq(AweConstants.CHECK_EXIST_PROFILE),
      org.mockito.ArgumentMatchers.<ObjectNode>argThat(
        parameters -> "operator".equals(parameters.get("profile").asString()))))
      .thenReturn(new ServiceData().setDataList(profiles));

    assertThat(userDAO.findProfileId("operator")).contains(7);
  }

  @Test
  void findProfileId_withoutThatProfile_returnsEmpty() throws Exception {
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.CHECK_EXIST_PROFILE), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    assertThat(userDAO.findProfileId("ghost")).isEmpty();
  }

  @Test
  void findProfile_findsTheProfileByNameAsTheSingleSignOnAndReturnsItsAcronymNameAndRestriction() throws Exception {
    when(queryUtil.getParameters()).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    DataList ids = new DataList();
    DataListUtil.addColumnWithOneRow(ids, "IdePro", 7);
    when(queryService.launchPrivateQuery(eq(AweConstants.CHECK_EXIST_PROFILE),
      org.mockito.ArgumentMatchers.<ObjectNode>argThat(
        parameters -> "Operator".equals(parameters.get("profile").asString()))))
      .thenReturn(new ServiceData().setDataList(ids));
    DataList profile = new DataList();
    DataListUtil.addColumnWithOneRow(profile, "profile", "OPE");
    DataListUtil.addColumnWithOneRow(profile, "name", "operator");
    DataListUtil.addColumnWithOneRow(profile, "restriction", "general");
    when(queryService.launchPrivateQuery(eq("access-subject-profile"),
      org.mockito.ArgumentMatchers.<ObjectNode>argThat(parameters -> parameters.get("profile").asInt() == 7)))
      .thenReturn(new ServiceData().setDataList(profile));

    assertThat(userDAO.findProfile("Operator")).contains(new UserProfile("OPE", "operator", "general"));
  }

  @Test
  void findProfile_withoutAProfileWithThatName_returnsEmpty() throws Exception {
    when(queryUtil.getParameters()).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.CHECK_EXIST_PROFILE), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    assertThat(userDAO.findProfile("OPE")).isEmpty();
    verify(queryService, never()).launchPrivateQuery(eq("access-subject-profile"), any(ObjectNode.class));
  }

  @Test
  void findProfile_whenTheQueryFails_throws() throws Exception {
    when(queryUtil.getParameters()).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.CHECK_EXIST_PROFILE), any(ObjectNode.class)))
      .thenThrow(new AWException("error", "no table"));

    // A failed lookup is not a missing profile: the token must not fall back to the default profile
    assertThatThrownBy(() -> userDAO.findProfile("operator")).isInstanceOf(AWException.class);
  }

  @Test
  void findPermissions_readsThePermissionsOfTheProfilesOnce() throws Exception {
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(JsonNodeFactory.instance.objectNode());
    DataList permissions = new DataList();
    DataListUtil.addColumn(permissions, "permission", List.of("*", "users.read", "users.read"));
    when(queryService.launchPrivateQuery(eq(AweConstants.PROFILE_PERMISSIONS_QUERY),
      org.mockito.ArgumentMatchers.<ObjectNode>argThat(
        parameters -> "[\"ADM\",\"OPE\"]".equals(parameters.get("profiles").toString()))))
      .thenReturn(new ServiceData().setDataList(permissions));

    assertThat(userDAO.findPermissions(List.of("ADM", "OPE"))).containsExactly("*", "users.read");
  }

  @Test
  void findPermissions_withoutProfiles_launchesNothing() throws Exception {
    assertThat(userDAO.findPermissions(List.of())).isEmpty();

    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void findPermissions_whenTheQueryFails_grantsNothing() throws Exception {
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.PROFILE_PERMISSIONS_QUERY), any(ObjectNode.class)))
      .thenThrow(new AWException("error", "no table"));

    assertThat(userDAO.findPermissions(List.of("ADM"))).isEmpty();
  }

  @Test
  void findAllPermissions_groupsTheAssignmentsByProfile() throws Exception {
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(JsonNodeFactory.instance.objectNode());
    DataList permissions = new DataList();
    DataListUtil.addColumn(permissions, "profile", List.of("ADM", "OPE", "OPE"));
    DataListUtil.addColumn(permissions, "permission", List.of("*", "users.read", "reports.*"));
    when(queryService.launchPrivateQuery(eq(AweConstants.ALL_PROFILE_PERMISSIONS_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(permissions));

    assertThat(userDAO.findAllPermissions())
      .containsExactly(java.util.Map.entry("ADM", List.of("*")),
        java.util.Map.entry("OPE", List.of("users.read", "reports.*")));
  }

  @Test
  void findAllPermissions_whenTheQueryFails_findsNothing() throws Exception {
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.ALL_PROFILE_PERMISSIONS_QUERY), any(ObjectNode.class)))
      .thenThrow(new AWException("error", "no table"));

    assertThat(userDAO.findAllPermissions()).isEmpty();
  }

  @Test
  void findProfileRestrictions_readsTheRestrictionFileOfEachProfile() throws Exception {
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    DataList profiles = new DataList();
    DataListUtil.addColumn(profiles, "profile", List.of("ADM", "RDR"));
    DataListUtil.addColumn(profiles, "restriction", List.of("administrator", ""));
    when(queryService.launchPrivateQuery(eq(AweConstants.PROFILE_RESTRICTIONS_QUERY),
      org.mockito.ArgumentMatchers.<ObjectNode>argThat(
        parameters -> "[\"ADM\",\"RDR\"]".equals(parameters.get("profiles").toString()))))
      .thenReturn(new ServiceData().setDataList(profiles));

    java.util.Map<String, String> restrictions = userDAO.findProfileRestrictions(List.of("ADM", "RDR"));

    assertThat(restrictions).containsOnlyKeys("ADM", "RDR").containsEntry("ADM", "administrator");
    assertThat(restrictions.get("RDR")).isNull();
  }

  @Test
  void findProfileRestrictions_whenTheQueryFails_findsNothing() throws Exception {
    when(queryUtil.getParameters()).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.PROFILE_RESTRICTIONS_QUERY), any(ObjectNode.class)))
      .thenThrow(new AWException("error", "no table"));

    assertThat(userDAO.findProfileRestrictions(List.of("ADM"))).isEmpty();
    assertThat(userDAO.findProfileRestrictions(List.of())).isEmpty();
  }

  @Test
  void supportsAdditionalProfiles() {
    assertThat(userDAO.supportsAdditionalProfiles()).isTrue();
  }

  @Test
  void aCustomUserDAODoesNotSupportAdditionalProfilesNorPermissionsByDefault() throws Exception {
    UserDAO custom = new UserDAO() {
      @Override
      public com.almis.awe.model.dto.User findByUserName(String userName) {
        return null;
      }

      @Override
      public com.almis.awe.model.dto.User findByEmail(String email) {
        return null;
      }

      @Override
      public com.almis.awe.model.dto.User findByRole(String role) {
        return null;
      }

      @Override
      public boolean existRole(String role) {
        return false;
      }
    };

    assertThat(custom.supportsAdditionalProfiles()).isFalse();
    assertThat(custom.findPermissions(List.of("ADM"))).isEmpty();
    assertThat(custom.findAllPermissions()).isEmpty();
    assertThat(custom.findProfileRestrictions(List.of("ADM"))).isEmpty();
    assertThat(custom.findProfile("administrator")).isEmpty();
  }
}
