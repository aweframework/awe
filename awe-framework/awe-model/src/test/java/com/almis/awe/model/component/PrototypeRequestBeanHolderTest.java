package com.almis.awe.model.component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PrototypeRequestBeanHolder}.
 *
 * <p>These tests cover both the async-worker path (holder present on thread) and the
 * request-thread path (no holder), including the pending-propagation overlay introduced to
 * fix the first-hop parameter-propagation bug.</p>
 */
class PrototypeRequestBeanHolderTest {

  private PrototypeRequestBeanHolder holder;

  @BeforeEach
  void setUp() {
    holder = new PrototypeRequestBeanHolder();
    holder.clear();
    holder.clearPendingPropagation();
  }

  @AfterEach
  void tearDown() {
    holder.clear();
    holder.clearPendingPropagation();
  }

  // ─── getPrototypeBean / setPrototypeBean ───────────────────────────────────

  @Test
  void getPrototypeBean_returnsNullWhenNoneSet() {
    assertNull(holder.getPrototypeBean());
  }

  @Test
  void setAndGetPrototypeBean_roundtrips() {
    RequestDataHolder rdh = new RequestDataHolder();
    holder.setPrototypeBean(rdh);
    assertSame(rdh, holder.getPrototypeBean());
  }

  // ─── getRequestDataSnapshot ───────────────────────────────────────────────

  @Test
  void getRequestDataSnapshot_returnsNullWhenNoBeanSet() {
    assertNull(holder.getRequestDataSnapshot());
  }

  @Test
  void getRequestDataSnapshot_returnsNullWhenBeanHasNullData() {
    holder.setPrototypeBean(new RequestDataHolder());
    assertNull(holder.getRequestDataSnapshot());
  }

  @Test
  void getRequestDataSnapshot_returnsDeepCopy() {
    ObjectNode data = JsonNodeFactory.instance.objectNode().put("k", "v");
    RequestDataHolder rdh = new RequestDataHolder();
    rdh.setRequestData(data);
    holder.setPrototypeBean(rdh);

    ObjectNode snapshot = holder.getRequestDataSnapshot();

    assertNotNull(snapshot);
    assertEquals("v", snapshot.get("k").asText());
    assertNotSame(data, snapshot);   // deep copy, not the same reference
  }

  // ─── mergeRequestData: holder present (async worker thread) ───────────────

  @Test
  void mergeRequestData_withHolderPresent_mergesIntoHolder() {
    RequestDataHolder rdh = new RequestDataHolder();
    rdh.setRequestData(JsonNodeFactory.instance.objectNode().put("existing", "yes"));
    holder.setPrototypeBean(rdh);

    ObjectNode update = JsonNodeFactory.instance.objectNode().put("new", "value");
    holder.mergeRequestData(update);

    ObjectNode result = rdh.getRequestData();
    assertEquals("yes", result.get("existing").asText());
    assertEquals("value", result.get("new").asText());
  }

  @Test
  void mergeRequestData_withHolderPresentAndNullData_initializesAndMerges() {
    RequestDataHolder rdh = new RequestDataHolder();
    // requestData starts as null
    holder.setPrototypeBean(rdh);

    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("k", "v"));

    assertNotNull(rdh.getRequestData());
    assertEquals("v", rdh.getRequestData().get("k").asText());
  }

  @Test
  void mergeRequestData_withNullInput_doesNothing() {
    RequestDataHolder rdh = new RequestDataHolder();
    ObjectNode original = JsonNodeFactory.instance.objectNode().put("k", "v");
    rdh.setRequestData(original);
    holder.setPrototypeBean(rdh);

    holder.mergeRequestData(null);

    assertSame(original, rdh.getRequestData());
  }

  // ─── mergeRequestData: no holder (request thread) → pending propagation ──

  @Test
  void mergeRequestData_withNoHolder_accumulatesInPendingPropagation() {
    ObjectNode params = JsonNodeFactory.instance.objectNode().put("seed", "hello");

    holder.mergeRequestData(params);

    ObjectNode pending = holder.getPendingPropagationSnapshot();
    assertNotNull(pending);
    assertEquals("hello", pending.get("seed").asText());
    // getPendingPropagationSnapshot returns a deep copy — the overlay is still there
    assertNotNull(holder.getPendingPropagationSnapshot());
  }

  @Test
  void mergeRequestData_withNoHolder_accumulatesMultipleCalls() {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("a", "1"));
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("b", "2"));

    ObjectNode pending = holder.getPendingPropagationSnapshot();
    assertNotNull(pending);
    assertEquals("1", pending.get("a").asText());
    assertEquals("2", pending.get("b").asText());
  }

  @Test
  void mergeRequestData_withNoHolder_laterCallOverwritesEarlierValue() {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("x", "first"));
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("x", "second"));

    ObjectNode pending = holder.getPendingPropagationSnapshot();
    assertNotNull(pending);
    assertEquals("second", pending.get("x").asText());
  }

  // ─── getPendingPropagationSnapshot ────────────────────────────────────────

  @Test
  void getPendingPropagationSnapshot_returnsNullWhenNothingPending() {
    assertNull(holder.getPendingPropagationSnapshot());
  }

  @Test
  void getPendingPropagationSnapshot_returnsDeepCopy() {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("k", "v"));

    ObjectNode snap1 = holder.getPendingPropagationSnapshot();
    ObjectNode snap2 = holder.getPendingPropagationSnapshot();

    assertNotNull(snap1);
    assertNotNull(snap2);
    assertNotSame(snap1, snap2);   // each call returns a fresh deep copy
  }

  // ─── clearPendingPropagation ──────────────────────────────────────────────

  @Test
  void clearPendingPropagation_removesAccumulatedData() {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("k", "v"));
    assertNotNull(holder.getPendingPropagationSnapshot());

    holder.clearPendingPropagation();

    assertNull(holder.getPendingPropagationSnapshot());
  }

  // ─── clear ────────────────────────────────────────────────────────────────

  @Test
  void clear_removesPrototypeBean() {
    holder.setPrototypeBean(new RequestDataHolder());
    assertNotNull(holder.getPrototypeBean());

    holder.clear();

    assertNull(holder.getPrototypeBean());
  }

  @Test
  void clear_doesNotAffectPendingPropagation() {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("k", "v"));
    holder.clear();

    // Pending overlay must survive a clear() because clear() targets the async-worker
    // ThreadLocal, not the request-thread overlay.
    assertNotNull(holder.getPendingPropagationSnapshot());
  }
}
