package com.almis.awe.listener;

import com.almis.awe.model.util.data.StringUtil;
import com.querydsl.sql.SQLBaseListener;
import com.querydsl.sql.SQLListenerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code SpringSQLCloseListener} closes the JDBC connection at the end of the query
 * or clause execution
 */
@Slf4j
public final class SpringSQLCloseListener extends SQLBaseListener {

  @Override
  public void end(SQLListenerContext context) {
    log.debug("Connection finished: {}", StringUtil.toUnilineText(context.getSQL()));
  }
}
