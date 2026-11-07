package com.almis.awe.model.entities.permissions;

import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.services.Service;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code permission} attribute of queries, maintain targets and services: the permissions required to launch them
 * from a client, separated by spaces
 */
class PermissionAttributeTest {

  private static XStream xStream;

  @BeforeAll
  static void beforeAll() {
    xStream = new XStream();
    xStream.processAnnotations(new Class[]{Query.class, Target.class, Service.class});
    xStream.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
  }

  @Test
  void queryReadsAndCopiesThePermission() {
    Query query = (Query) xStream.fromXML("<query id=\"UsrLst\" permission=\"users.read users.export\"/>");

    assertThat(query.getPermission()).isEqualTo("users.read users.export");
    assertThat(query.copy().getPermission()).isEqualTo("users.read users.export");
  }

  @Test
  void targetReadsAndCopiesThePermission() {
    Target target = (Target) xStream.fromXML("<target name=\"UsrDel\" permission=\"users.delete\"/>");

    assertThat(target.getPermission()).isEqualTo("users.delete");
    assertThat(target.copy().getPermission()).isEqualTo("users.delete");
  }

  @Test
  void withoutTheAttributeNoPermissionIsRequired() {
    assertThat(((Query) xStream.fromXML("<query id=\"UsrLst\"/>")).getPermission()).isNull();
    assertThat(((Target) xStream.fromXML("<target name=\"UsrDel\"/>")).getPermission()).isNull();
  }

  @Test
  void serviceReadsAndCopiesThePermission() {
    Service service = (Service) xStream.fromXML("<service id=\"deleteFile\" permission=\"files.delete\"/>");

    assertThat(service.getPermission()).isEqualTo("files.delete");
    assertThat(service.copy().getPermission()).isEqualTo("files.delete");
    assertThat(service.isAuthorizedByTarget()).isFalse();
  }

  @Test
  void serviceReadsTheAuthorizationByItsTarget() {
    Service service = (Service) xStream.fromXML("<service id=\"launchQuery\" authorization=\"target\"/>");

    assertThat(service.isAuthorizedByTarget()).isTrue();
    assertThat(service.copy().isAuthorizedByTarget()).isTrue();
    assertThat(service.getPermission()).isNull();
  }
}
