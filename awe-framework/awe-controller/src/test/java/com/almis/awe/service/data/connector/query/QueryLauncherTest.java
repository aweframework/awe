package com.almis.awe.service.data.connector.query;

import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.Variable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryLauncherTest {

  @InjectMocks
  private QueryLauncher queryLauncher;

  @Test
  void givenNullQuery_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> queryLauncher.launchQuery(null, null));
  }

  @Test
  void sessionQueryIsCachedOnlyWhenTheSessionValuesExist() {
    Query sessionUser = cacheableQuery(Variable.builder().id("Usr").type("STRING").session("user").build());
    AweSession session = mock(AweSession.class);
    QueryLauncher launcher = spy(queryLauncher);
    doReturn(session).when(launcher).getSession();

    when(session.getParameter("user")).thenReturn("test");
    assertThat(launcher.isCacheable(sessionUser)).isTrue();

    when(session.getParameter("user")).thenReturn(null);
    assertThat(launcher.isCacheable(sessionUser)).isFalse();

    doReturn(null).when(launcher).getSession();
    assertThat(launcher.isCacheable(sessionUser)).isFalse();
  }

  @Test
  void queryWithoutSessionValuesKeepsItsCacheableFlag() {
    QueryLauncher launcher = spy(queryLauncher);
    doReturn(null).when(launcher).getSession();

    assertThat(launcher.isCacheable(cacheableQuery(Variable.builder().id("Par").type("STRING").name("par").build()))).isTrue();
    assertThat(launcher.isCacheable(new Query().setCacheable(false))).isFalse();
  }

  private Query cacheableQuery(Variable variable) {
    List<Variable> variables = new ArrayList<>();
    variables.add(variable);
    Query query = new Query().setCacheable(true);
    query.setVariableDefinitionList(variables);
    return query;
  }
}
