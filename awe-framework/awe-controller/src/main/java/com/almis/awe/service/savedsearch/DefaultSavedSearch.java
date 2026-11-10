package com.almis.awe.service.savedsearch;

/**
 * Default saved search of a window for the session user (#915): its own default, or the default of its profile
 *
 * @param id     Search identifier
 * @param name   Search name
 * @param values Values of the search as stored (JSON text)
 */
public record DefaultSavedSearch(int id, String name, String values) {
}
