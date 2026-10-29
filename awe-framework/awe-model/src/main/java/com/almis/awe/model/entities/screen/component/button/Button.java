package com.almis.awe.model.entities.screen.component.button;

import com.almis.awe.model.util.data.ListUtil;
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
 * Button Class
 *
 * Used to parse the Button Component in a screen with XStream
 *
 *
 * Launches contained Button Actions when it is clicked
 *
 *
 * @author Pablo GARCIA - 24/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("button")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Button extends AbstractButton {

  @Serial
  private static final long serialVersionUID = 4537762712903982049L;

  // Busy state: a spinner while the action launched by the button runs (#771). Absent means true
  @XStreamAlias("show-loading")
  @XStreamAsAttribute
  private Boolean showLoading;

  // Selection button of a grid (#793): shown in the selection bar of the grid while it has selected rows, not in its footer
  @XStreamAlias("selection")
  @XStreamAsAttribute
  private Boolean selection;

  /**
   * Returns if the button shows its busy state while the action it launched runs. Not a bean getter: the clients only
   * receive showLoading when the screen sets it
   * @return The busy state is on (true unless show-loading is false)
   */
  public boolean showsLoading() {
    return getShowLoading() == null || getShowLoading();
  }

  @Override
  public Button copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }
}
