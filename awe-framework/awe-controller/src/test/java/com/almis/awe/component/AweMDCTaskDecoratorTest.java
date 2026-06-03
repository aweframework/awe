package com.almis.awe.component;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AweMDCTaskDecoratorTest {

  @Mock
  private ObjectProvider<RequestDataHolder> requestDataHolderProvider;

  @Mock
  private RequestAttributes requestAttributes;

  @Mock
  private AweRequest aweRequest;

  private PrototypeRequestBeanHolder prototypeRequestBeanHolder;
  private AweMDCTaskDecorator decorator;

  @BeforeEach
  void setUp() {
    prototypeRequestBeanHolder = new PrototypeRequestBeanHolder();
    when(requestDataHolderProvider.getObject()).thenAnswer(invocation -> new RequestDataHolder());
    decorator = new AweMDCTaskDecorator(requestDataHolderProvider, prototypeRequestBeanHolder);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
    prototypeRequestBeanHolder.clear();
    MDC.clear();
  }

  @Test
  void decorate_withInactiveRequestScopeAndNoAncestorSnapshot_usesEmptySnapshotAndDoesNotThrow() {
    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();

    Runnable decoratedRunnable = assertDoesNotThrow(() -> decorator.decorate(() -> {
      RequestDataHolder requestDataHolder = prototypeRequestBeanHolder.getPrototypeBean();
      assertNotNull(requestDataHolder);
      seenSnapshot.set(requestDataHolder.getRequestData());
    }));

    assertDoesNotThrow(decoratedRunnable::run);

    assertNotNull(seenSnapshot.get());
    assertTrue(seenSnapshot.get().isEmpty());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());
  }

  @Test
  void decorate_withInactiveRequestScopeAndAncestorSnapshot_reusesAncestorSnapshot() {
    ObjectNode ancestorSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "bar");
    RequestDataHolder ancestorHolder = new RequestDataHolder();
    ancestorHolder.setRequestData(ancestorSnapshot);
    prototypeRequestBeanHolder.setPrototypeBean(ancestorHolder);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();
    AtomicReference<RequestDataHolder> seenHolder = new AtomicReference<>();

    Runnable decoratedRunnable = assertDoesNotThrow(() -> decorator.decorate(() -> {
      RequestDataHolder requestDataHolder = prototypeRequestBeanHolder.getPrototypeBean();
      seenHolder.set(requestDataHolder);
      seenSnapshot.set(requestDataHolder.getRequestData());
    }));

    assertDoesNotThrow(decoratedRunnable::run);

    assertNotNull(seenSnapshot.get());
    assertEquals("bar", seenSnapshot.get().get("foo").asText());
    assertTrue(seenSnapshot.get().size() > 0);
    assertNotNull(seenHolder.get());
    assertTrue(seenHolder.get() != ancestorHolder);
  }

  @Test
  void decorate_withActiveRequestScope_prefersLiveRequestSnapshotOverAncestorSnapshot() {
    ObjectNode ancestorSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "ancestor");
    RequestDataHolder ancestorHolder = new RequestDataHolder();
    ancestorHolder.setRequestData(ancestorSnapshot);
    prototypeRequestBeanHolder.setPrototypeBean(ancestorHolder);

    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "live");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    AtomicReference<ObjectNode> seenSnapshot = new AtomicReference<>();

    Runnable decoratedRunnable = decorator.decorate(() -> seenSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData()));
    decoratedRunnable.run();

    assertNotNull(seenSnapshot.get());
    assertEquals("live", seenSnapshot.get().get("foo").asText());
  }

  @Test
  void decorate_propagatesResolvedSnapshot_fromParentToChildToGrandchild() throws Exception {
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "live");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    AtomicReference<ObjectNode> parentSnapshot = new AtomicReference<>();
    AtomicReference<ObjectNode> childSnapshot = new AtomicReference<>();
    AtomicReference<ObjectNode> grandchildSnapshot = new AtomicReference<>();
    AtomicReference<Runnable> childDecorated = new AtomicReference<>();
    AtomicReference<Runnable> grandchildDecorated = new AtomicReference<>();

    Runnable parentDecorated = decorator.decorate(() -> {
      parentSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy());
      childDecorated.set(decorator.decorate(() -> {
        childSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy());
        grandchildDecorated.set(decorator.decorate(() ->
          grandchildSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData().deepCopy())));
      }));
    });

    RequestContextHolder.resetRequestAttributes();

    executeInSingleThread(parentDecorated);
    executeInSingleThread(childDecorated.get());
    executeInSingleThread(grandchildDecorated.get());

    assertNotNull(parentSnapshot.get());
    assertEquals(parentSnapshot.get(), childSnapshot.get());
    assertEquals(childSnapshot.get(), grandchildSnapshot.get());
    assertEquals("live", grandchildSnapshot.get().get("foo").asText());
  }

  @Test
  void decorate_clearsPrototypeHolderAndMdc_betweenSequentialRuns() {
    ObjectNode liveSnapshot = JsonNodeFactory.instance.objectNode().put("foo", "first");
    when(aweRequest.getParametersSafe()).thenReturn(liveSnapshot);

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).thenReturn(aweRequest);

    MDC.put("trace", "first-run");
    AtomicReference<ObjectNode> firstSnapshot = new AtomicReference<>();
    AtomicReference<String> firstMdc = new AtomicReference<>();

    Runnable firstDecorated = decorator.decorate(() -> {
      firstSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData());
      firstMdc.set(MDC.get("trace"));
    });
    firstDecorated.run();

    assertNotNull(firstSnapshot.get());
    assertEquals("first", firstSnapshot.get().get("foo").asText());
    assertEquals("first-run", firstMdc.get());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());

    RequestContextHolder.setRequestAttributes(requestAttributes);
    when(requestAttributes.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST))
      .thenThrow(new IllegalStateException("request is not active anymore"));

    AtomicReference<ObjectNode> secondSnapshot = new AtomicReference<>();
    AtomicReference<String> secondMdc = new AtomicReference<>();

    Runnable secondDecorated = decorator.decorate(() -> {
      secondSnapshot.set(prototypeRequestBeanHolder.getPrototypeBean().getRequestData());
      secondMdc.set(MDC.get("trace"));
    });
    secondDecorated.run();

    assertNotNull(secondSnapshot.get());
    assertTrue(secondSnapshot.get().isEmpty());
    assertNull(secondMdc.get());
    assertNull(prototypeRequestBeanHolder.getPrototypeBean());
    assertNull(MDC.getCopyOfContextMap());
  }

  private void executeInSingleThread(Runnable runnable) throws Exception {
    assertNotNull(runnable);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    try {
      Future<?> task = executorService.submit(runnable);
      task.get(10, TimeUnit.SECONDS);
    } finally {
      executorService.shutdownNow();
    }
  }
}
