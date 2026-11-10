package com.almis.awe.model.entities.screen.component;

import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Default saved search applied to a window before the initial load of its screen (#915): the default search of the user,
 * or the default search of its profile
 *
 * @param id     Search identifier
 * @param name   Search name
 * @param values Values of the search (a JSON object), as saved, for the current search criteria of the window:
 *               {@code {"<criterion>": [{"value": ..., "label": ...}]}}
 * @param layout Advanced criteria of the window the search shows in the main filters, in order (empty when none)
 */
public record DefaultSearch(int id, String name, JsonNode values, List<String> layout) {
}
