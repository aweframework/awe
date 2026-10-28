package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Card: a metric tile (title, value, icon, change with its trend, footer...) filled from a query, whose children are
 * shown in its body (#899). The attributes are static values; the fields of the first row of the query override them.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("card")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Card extends Component {

  @Serial
  private static final long serialVersionUID = 2618417032845169734L;

  private static final String DEFAULT_GOOD_TREND = "up";

  // Secondary text below the title
  @XStreamAlias("subtitle")
  @XStreamAsAttribute
  private String subtitle;

  // Colour of the icon tile (success, warning, danger, info or neutral)
  @XStreamAlias("icon-tone")
  @XStreamAsAttribute
  private String iconTone;

  // Status chip at the top right, before the icon or the delta (#836)
  @XStreamAlias("chip")
  @XStreamAsAttribute
  private String chip;

  // Tone of the status chip (success, warning, danger, info or neutral)
  @XStreamAlias("chip-tone")
  @XStreamAsAttribute
  private String chipTone;

  // Main value
  @XStreamAlias("value")
  @XStreamAsAttribute
  private String value;

  // Unit of the value and of the target
  @XStreamAlias("unit")
  @XStreamAsAttribute
  private String unit;

  // Decimals of the numbers of the card
  @XStreamAlias("number-decimals")
  @XStreamAsAttribute
  private Integer numberDecimals;

  // Change of the value
  @XStreamAlias("delta")
  @XStreamAsAttribute
  private String delta;

  // Unit of the change
  @XStreamAlias("delta-unit")
  @XStreamAsAttribute
  private String deltaUnit;

  // Direction of the change (up, down or flat)
  @XStreamAlias("trend")
  @XStreamAsAttribute
  private String trend;

  // Direction of the change that is good news (up or down)
  @XStreamAlias("good-trend")
  @XStreamAsAttribute
  private String goodTrend;

  // Tone of the change, used instead of the one derived from the trend
  @XStreamAlias("tone")
  @XStreamAsAttribute
  private String tone;

  // Text next to the change
  @XStreamAlias("caption")
  @XStreamAsAttribute
  private String caption;

  // Target of the value (progress bar)
  @XStreamAlias("target")
  @XStreamAsAttribute
  private String target;

  // Series of numbers drawn as a sparkline below the value (JSON array)
  @XStreamAlias("sparkline")
  @XStreamAsAttribute
  private String sparkline;

  // Type of the sparkline (line, area or bar)
  @XStreamAlias("sparkline-type")
  @XStreamAsAttribute
  private String sparklineType;

  // How the progress towards the target is drawn (bar or gauge)
  @XStreamAlias("progress-style")
  @XStreamAsAttribute
  private String progressStyle;

  // Paragraph below the value
  @XStreamAlias("description")
  @XStreamAsAttribute
  private String description;

  // Muted text at the bottom of the card
  @XStreamAlias("footer")
  @XStreamAsAttribute
  private String footer;

  @Override
  public Card copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Returns the direction of the change that is good news
   *
   * @return Good trend (up by default)
   */
  @JsonGetter("goodTrend")
  public String getGoodTrend() {
    return goodTrend == null ? DEFAULT_GOOD_TREND : goodTrend;
  }

  /**
   * Retrieve component tag
   *
   * @return Component tag
   */
  @Override
  public String getComponentTag() {
    return "card";
  }
}
