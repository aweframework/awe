package com.almis.awe.model.dto;

import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.Queue;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.services.Service;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * XML initialization data: holds the new element maps being built and the futures
 * that fill them, so callers can wait for termination and atomically swap the maps in
 */
@Data
@Accessors(chain = true)
public class XMLInitData {
  List<Future<String>> general = new ArrayList<>();
  List<Future<Map<String, Profile>>> profileResults = new ArrayList<>();
  List<Future<Map<String, Screen>>> screenResults = new ArrayList<>();
  List<Map<String, Future<Map<String, String>>>> localeResults = new ArrayList<>();

  // New element maps filled by the general futures
  Map<String, EnumeratedGroup> enumerated = new ConcurrentHashMap<>();
  Map<String, Query> queries = new ConcurrentHashMap<>();
  Map<String, Queue> queues = new ConcurrentHashMap<>();
  Map<String, Target> maintains = new ConcurrentHashMap<>();
  Map<String, Email> emails = new ConcurrentHashMap<>();
  Map<String, Service> services = Collections.synchronizedMap(new LinkedHashMap<>());
  Map<String, Action> actions = new ConcurrentHashMap<>();
}
