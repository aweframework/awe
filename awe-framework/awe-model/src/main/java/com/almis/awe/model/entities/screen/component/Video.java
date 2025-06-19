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
 * Video Tag Class
 *
 * Used to parse video tag with XStream
 *
 *
 * Default HTML video tag
 *
 *
 * @author David FUENTES - 09/MAR/2016
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@XStreamAlias("video")
@Accessors(chain = true)
public class Video extends Component {

  private static final long serialVersionUID = 8799204817655283841L;

  // Tag loop attribute
  @XStreamAlias("loop")
  @XStreamAsAttribute
  private String loop;

  // Tag preload attribute
  @XStreamAlias("preload")
  @XStreamAsAttribute
  private String preload;

  // Tag autoplay attribute
  @XStreamAlias("autoplay")
  @XStreamAsAttribute
  private String autoplay;

  // Tag poster attribute
  @XStreamAlias("poster")
  @XStreamAsAttribute
  private String poster;

  // Tag poster attribute
  @XStreamAlias("controls")
  @XStreamAsAttribute
  private String controls;

  // Tag source attribute
  @XStreamAlias("src")
  @XStreamAsAttribute
  private String src;

  @Override
  public Video copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Returns controls value for the video
   *
   * @return controls
   */
  public String getControls() {
    return controls != null && !controls.equals("") ? controls : "true";
  }

  /**
   * Retrieve component tag
   *
   * @return <code>video</code> tag
   */
  @Override
  public String getComponentTag() {
    return "video";
  }
}
