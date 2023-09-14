package com.almis.awe.test.listener;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestSessionListener implements HttpSessionListener {
  private static final Map<String, HttpSession> sessionMap = new ConcurrentHashMap<>();

  public TestSessionListener() {
    super();
  }

  public static Map<String, HttpSession> getAllSessions() {
    return sessionMap;
  }

  public void sessionCreated(final HttpSessionEvent event) {
    sessionMap.put(event.getSession().getId(), event.getSession());
  }

  public void sessionDestroyed(final HttpSessionEvent event) {
    sessionMap.remove(event.getSession().getId());
  }
}
