package com.almis.awe.component;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import org.springframework.expression.EvaluationException;

import java.util.Map;

/**
 * <p>This class was generated when migrating a logback-classic configuration
 * using JaninoEventEvaluator.</p>
 *
 * <p>JaninoEventEvaluator has been removed due to identified vulnerabilities.</p>which
 *
 * <p>Note that the generated code in the {@link #evaluate(ILoggingEvent)} method will
 * depend on the boolean expression, more specifically on the variables referenced
 * in the original boolean  expression.</p>
 */
public class AweUserLogEvaluator extends EventEvaluatorBase<ILoggingEvent> {

  public boolean evaluate(ILoggingEvent event) throws NullPointerException, EvaluationException {
    Map<String, String> mdc = event.getMDCPropertyMap();
    return mdc.get("logUserName") != null;
  }
}