package com.almis.awe.service.data.builder.sql;

import com.querydsl.core.types.dsl.PathBuilder;

/**
 * Builds the QueryDSL path of a single node. Used by {@code SQLBuilder#buildPath(String)}; it is not an extension
 * point: the {@code buildPath} overloads of {@code SQLBuilder} keep calling each other so that overriding one of them
 * still applies to the others.
 */
public final class SqlPaths {

  private SqlPaths() {
  }

  /**
   * Build the path of a node
   *
   * @param node Node (a table, a field or an alias)
   * @return Path, or null when the node is null
   */
  public static PathBuilder<Object> path(String node) {
    return node == null ? null : new PathBuilder<>(Object.class, node);
  }
}
