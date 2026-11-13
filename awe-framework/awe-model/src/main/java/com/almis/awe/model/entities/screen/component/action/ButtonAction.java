package com.almis.awe.model.entities.screen.component.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * ButtonAction Class
 *
 * Used to parse button actions with XStream
 *
 *
 * ButtonAction class extends from AbstractAction
 *
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("button-action")
public class ButtonAction extends AbstractAction {

  @Serial
  private static final long serialVersionUID = -7768601748285062450L;

  // Permissions required to edit in the dialog opened by the action, separated by spaces (all of them are required).
  // Read by the server only
  @JsonIgnore
  @XStreamAlias("edit-permission")
  @XStreamAsAttribute
  private String editPermission;

  /**
   * View mode of the dialog opened by the action: set to true by the server when the user does not hold the
   * {@code edit-permission} of the action, so the client opens the dialog with its criteria read-only. Sent to both
   * engines as {@code "viewMode": true} in the actions of the button, and never sent when it is not set
   */
  @XStreamOmitField
  private Boolean viewMode;

  @Override
  public ButtonAction copy() {
    return this.toBuilder().build();
  }
}
