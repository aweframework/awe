package com.almis.awe.service.menu;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.builder.DataListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The restrictions read to authorize a request must not inherit the grid sort of that request
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MenuRestrictionTreeServiceTest {

  @Mock
  private MenuProvider menuProvider;
  @Mock
  private MenuScreenResolver menuScreenResolver;
  @Mock
  private QueryService queryService;
  @Mock
  private SecurityConfigProperties securityConfigProperties;
  @Mock
  private ApplicationContext context;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private AweElements elements;

  private MenuRestrictionTreeService service;

  @BeforeEach
  void setUp() {
    service = new MenuRestrictionTreeService(menuProvider, menuScreenResolver, queryService, securityConfigProperties);
    service.setApplicationContext(context);
    when(context.getBean(QueryUtil.class)).thenReturn(queryUtil);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(context.getBean(DataListBuilder.class)).thenReturn(mock(DataListBuilder.class, RETURNS_SELF));
  }

  @Test
  void databaseRestrictionsAreReadWithAnEmptyClientSort() throws AWException {
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.putArray(AweConstants.COMPONENT_SORT).addObject().put("id", "TypIco");
    requestParameters.put(AweConstants.COMPONENT_PAGE, 1).put(AweConstants.COMPONENT_MAX, 0);
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("general");
    when(elements.getProfile("general")).thenReturn(new Profile());
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(requestParameters);
    when(queryService.launchPrivateQuery(eq(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData());

    service.getScreenRestrictions(null);

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY), captor.capture());
    assertThat(captor.getValue().get(AweConstants.COMPONENT_SORT).isEmpty()).isTrue();
    assertThat(captor.getValue().get(AweConstants.COMPONENT_PAGE).asInt()).isEqualTo(1);
    assertThat(captor.getValue().get(AweConstants.COMPONENT_MAX).asInt()).isZero();
  }

  @Test
  void anOpenPublicOptionDoesNotOpenAPrivateOptionThatEveryProfileCloses() throws AWException {
    // X is in the public menu and, in the private menu, under P, which the restriction file of every profile closes
    AweSession session = mock(AweSession.class);
    when(context.getBean(AweSession.class)).thenReturn(session);
    when(session.getParameter(String.class, AweConstants.SESSION_PROFILES)).thenReturn("AAA,BBB");
    when(queryUtil.getParameters(null, "1", "0")).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(eq(AweConstants.SCREEN_DATABASE_RESTRICTION_BY_PROFILES_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(queryService.launchPrivateQuery(eq(AweConstants.PROFILES_RESTRICTION_QUERY), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("general");
    Restriction restrictParent = new Restriction();
    restrictParent.setOption("P");
    restrictParent.setRestrictionType(RestrictionType.R);
    when(elements.getProfile(anyString())).thenReturn(new Profile().setRestrictions(List.of(restrictParent)));
    when(menuProvider.getMenu(AweConstants.PRIVATE_MENU)).thenAnswer(invocation -> menu(option("P", option("X"))));
    when(menuProvider.getMenu(AweConstants.PUBLIC_MENU)).thenAnswer(invocation -> menu(option("X")));

    DataList restrictions = service.getScreenRestrictions("general").getDataList();

    assertThat(restrictions.getRows())
      .filteredOn(row -> "X".equals(row.get(AweConstants.JSON_OPTION).getStringValue()))
      .extracting(row -> row.get(AweConstants.JSON_RESTRICTED).getValue())
      .containsOnly(true);
  }

  private static Menu menu(Option... options) {
    Menu menu = new Menu();
    for (Option option : options) {
      menu.addElement(option);
    }
    return menu;
  }

  private static Option option(String name, Option... children) {
    Option option = new Option();
    option.setName(name);
    for (Option child : children) {
      option.addElement(child);
    }
    return option;
  }
}
