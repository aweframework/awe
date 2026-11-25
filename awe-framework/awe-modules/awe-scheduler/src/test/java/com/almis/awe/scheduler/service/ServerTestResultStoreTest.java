package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweSession;
import com.almis.awe.scheduler.bean.file.ServerTestResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Last connection test of each server in the session of the user (#993): it is kept in the HTTP session, never in the
 * database, so a new store over the same session reads it and a request without a session keeps nothing
 */
class ServerTestResultStoreTest {

  private static final LocalDateTime TESTED_AT = LocalDateTime.of(2026, 11, 24, 10, 44);

  @Test
  void theLastResultOfEachServerIsKeptInTheSessionOfTheUser() {
    AweSession session = new AweSession();
    ServerTestResultStore store = new ServerTestResultStore(() -> session);

    store.record(1, ServerTestResult.success(420, TESTED_AT));
    store.record(2, ServerTestResult.failure("CONNECTION_ERROR", 3000, TESTED_AT));
    store.record(1, ServerTestResult.failure("AUTHENTICATION_ERROR", 180, TESTED_AT.plusMinutes(1)));

    assertThat(new ServerTestResultStore(() -> session).getResults())
      .containsOnlyKeys(1, 2)
      .containsEntry(1, ServerTestResult.failure("AUTHENTICATION_ERROR", 180, TESTED_AT.plusMinutes(1)))
      .containsEntry(2, ServerTestResult.failure("CONNECTION_ERROR", 3000, TESTED_AT));
  }

  @Test
  void withoutASessionNothingIsKeptAndNothingFails() {
    ServerTestResultStore store = new ServerTestResultStore(() -> null);

    store.record(1, ServerTestResult.success(420, TESTED_AT));

    assertThat(store.getResults()).isEmpty();
  }
}
