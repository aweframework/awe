package com.almis.awe.model.entities.permissions;

import com.almis.awe.model.entities.XMLNode;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Resource of the permission catalog. Its permissions are {@code <resource>.<verb>} for each listed verb and
 * {@code <resource>.<action>} for each action
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("resource")
public class PermissionResource implements XMLNode {

  @Serial
  private static final long serialVersionUID = -6129400187215006514L;

  // Resource identifier
  @XStreamAsAttribute
  private String id;

  // Resource label (locale key)
  @XStreamAsAttribute
  private String label;

  // Comma separated verb list ('crud' means read, create, update and delete)
  @XStreamAsAttribute
  private String verbs;

  // 'always' grants the permissions of the resource to every authenticated user
  @XStreamAsAttribute
  private String granted;

  // Action list
  @XStreamImplicit(itemFieldName = "action")
  private List<PermissionAction> actionList;

  /**
   * Returns the declared actions
   *
   * @return Action list
   */
  public List<PermissionAction> getActionList() {
    return actionList == null ? new ArrayList<>() : actionList;
  }

  @Override
  public String getElementKey() {
    return getId();
  }
}
