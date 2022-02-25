package com.almis.awe.model.entities.screen.component;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Image Tag Class
 * Used to parse a tag with XStream
 * Default HTML Image tag
 *
 * @author Pablo García
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@XStreamAlias("image")
public class Image extends Component {

  private static final long serialVersionUID = 8799204817655283841L;

  // Tag src attribute
  @XStreamAlias("url")
  @XStreamAsAttribute
  private String url;

  // Tag alt attribute
  @XStreamAlias("alternate-url")
  @XStreamAsAttribute
  private String alternateUrl;

  @Override
  public Image copy() throws AWException {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve component tag
   *
   * @return <code>image</code> tag
   */
  @Override
  public String getComponentTag() {
    return "image";
  }
}
