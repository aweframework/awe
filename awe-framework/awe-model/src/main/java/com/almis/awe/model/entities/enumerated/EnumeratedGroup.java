package com.almis.awe.model.entities.enumerated;

import com.almis.awe.model.entities.Copyable;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.XMLNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;

/*
 * File Imports
 */

/**
 * EnumeratedGroup Class
 *
 * Used to parse the file Enumerated.xml with XStream
 * Generates an enumerated group
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("group")
public class EnumeratedGroup implements XMLNode, Copyable {

  private static final long serialVersionUID = 405249052409598721L;

  // Group identifier
  @XStreamAlias("id")
  @XStreamAsAttribute
  private String id;

  // Group option list
  @XStreamImplicit(itemFieldName = "option")
  private List<Global> optionList;

  /**
   * Returns the label of the selected value (for translate purpose in queries)
   *
   * @param value Value to find
   * @return Label of the value
   */
  public String findLabel(String value) {
    String notNullValue = Optional.ofNullable(value).orElse("");
    return this.getOptionList().stream()
      .filter(option -> option.getValue().equalsIgnoreCase(notNullValue))
      .map(Global::getLabel)
      .findFirst()
      .orElse(notNullValue);
  }

  @JsonIgnore
  @Override
  public String getElementKey() {
    return this.getId();
  }

  @Override
  public EnumeratedGroup copy() {
    return this.toBuilder().build();
  }
}
