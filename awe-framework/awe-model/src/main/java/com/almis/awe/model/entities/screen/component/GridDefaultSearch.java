package com.almis.awe.model.entities.screen.component;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;

/**
 * Default saved search applied to a grid before its initial load (#945): the default search of the user, or the default
 * search of its profile, with its state checked against the current grid (the columns it no longer has are dropped)
 *
 * @param id                 Search identifier
 * @param name               Search name
 * @param columnFilters      Column filters, as the clients send them (an empty list when none)
 * @param columnFilterLabels Labels of the values of the column filters, by column and value (an empty object when none)
 * @param sort               Sort, a list of {@code {"id": ..., "direction": "asc|desc"}} (an empty list when none)
 * @param quickFilter        Value of the quick filter, not sent when the search does not set one
 * @param search             Text of the search, not sent when the search does not set one
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GridDefaultSearch(int id, String name, JsonNode columnFilters, JsonNode columnFilterLabels, JsonNode sort,
                                JsonNode quickFilter, JsonNode search) {
}
