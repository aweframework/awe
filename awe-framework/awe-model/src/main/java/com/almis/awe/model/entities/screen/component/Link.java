package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * A Tag Class
 * Used to parse a tag with XStream
 * Default HTML A tag
 *
 * @author Pablo García
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@XStreamAlias("link")
@Accessors(chain = true)
public class Link extends Component {

  private static final long serialVersionUID = 8799204817655283841L;

  // href attribute
  @XStreamAlias("url")
  @XStreamAsAttribute
  private String url;

  @Override
  public Link copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve component tag
   *
   * @return <code>link</code> tag
   */
  @Override
  public String getComponentTag() {
    return "link";
  }
}
