package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweSession;
import com.almis.awe.scheduler.bean.file.ServerTestResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Last connection test of each server in the session of the user (configuration screen, «Última prueba»). It is never
 * stored in the database: a new session starts with every server untested. The map in the session is replaced on each
 * record, never changed in place, so a session store that serializes its attributes keeps the new value too
 */
public class ServerTestResultStore {

  static final String SESSION_PARAMETER = "awe-scheduler.server-test-results";

  private final Supplier<AweSession> session;

  /**
   * Constructor
   *
   * @param session Session of the current user, null outside a request
   */
  public ServerTestResultStore(Supplier<AweSession> session) {
    this.session = session;
  }

  /**
   * Keep a result as the last test of a server; nothing is kept without a session
   *
   * @param serverId Server identifier
   * @param result   Result of the test
   */
  public void record(Integer serverId, ServerTestResult result) {
    AweSession current = session.get();
    if (current != null && serverId != null) {
      synchronized (current) {
        Map<Integer, ServerTestResult> results = new HashMap<>(read(current));
        results.put(serverId, result);
        current.setParameter(SESSION_PARAMETER, results);
      }
    }
  }

  /**
   * Last test of each server tested in the session
   *
   * @return Result by server identifier, empty without a session
   */
  public Map<Integer, ServerTestResult> getResults() {
    AweSession current = session.get();
    return current == null ? Map.of() : Map.copyOf(read(current));
  }

  @SuppressWarnings("unchecked")
  private static Map<Integer, ServerTestResult> read(AweSession session) {
    Object stored = session.getParameter(SESSION_PARAMETER);
    return stored instanceof Map<?, ?> map ? (Map<Integer, ServerTestResult>) map : Map.of();
  }
}
