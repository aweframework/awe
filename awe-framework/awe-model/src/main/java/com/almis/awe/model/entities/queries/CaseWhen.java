package com.almis.awe.model.entities.queries;

import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * CaseWhen Class
 * <p>
 * Used to parse the file Queries.xml with XStream
 * Generates a CASE WHEN condition in a query
 *
 * @author Isaac Serna - 13/JUN/2018
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Accessors(chain = true)
@XStreamAlias("when")
public class CaseWhen extends Filter {

  // Optional filter
  @XStreamAlias("then")
  private TransitionField thenOperand;

  // Optional AND filters
  @XStreamAlias("and")
  private FilterAnd filterAnd;

  @Override
  public CaseWhen copy() {
    return ((CaseWhen) super.copy())
      .setFilterAnd(ListUtil.copyElement(filterAnd))
      .setThenOperand(ListUtil.copyElement(thenOperand));
  }

  @Override
  public String toString() {
    String thenString = "";
    String conditionWhen = super.toString();
    if (getFilterAnd() != null) {
      conditionWhen = getFilterAnd().toString();
    }
    if (getThenOperand() != null) {
      thenString = getThenOperand().toString();
    }
    return "WHEN " + conditionWhen + " THEN " + thenString;
  }
}
